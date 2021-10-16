//
//  ProxyStore.scala
//  over-layer
//
//  Created by d-exclaimation on 9:20 PM.
//

package io.github.dexclaimation.overlayer.engine

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import io.github.dexclaimation.overlayer.engine.OverActions._
import io.github.dexclaimation.overlayer.envoy.EnvoyMessage.{Acid, Subscribe, Unsubscribe}
import io.github.dexclaimation.overlayer.envoy.{Envoy, EnvoyMessage}
import io.github.dexclaimation.overlayer.implicits.WebsocketExtensions._
import io.github.dexclaimation.overlayer.model.SchemaConfig
import io.github.dexclaimation.overlayer.model.Subtypes._
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.{GqlError, OperationMessage}
import io.github.dexclaimation.overlayer.utils.ExceptionUtil.safe
import sangria.ast
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import spray.json.JsObject

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * A Engine Actor managing distribution of operations and top-level state
 *
 * @param protocol The GraphQL Subscription Sub Protocol used.
 * @param config   The Schema Configuration.
 * @param context  The Actor Context for the Abstract Behavior.
 */
class OverEngine[Ctx, Val](
  val protocol: OverWebsocket,
  val config: SchemaConfig[Ctx, Val],
  override val context: ActorContext[OverActions]
) extends AbstractBehavior(context) {

  implicit private val ex: ExecutionContext = context.executionContext

  /** Client State mapped to the PID */
  private val refs: mutable.Map[PID, Ref] = mutable.Map.empty[PID, Ref]

  /** Envoy State mapped to the PID */
  private val envoys: mutable.Map[PID, ActorRef[EnvoyMessage]] = mutable.Map.empty[PID, ActorRef[EnvoyMessage]]

  def onMessage(msg: OverActions): Behavior[OverActions] = receive(msg) {
    case Connect(pid, ref) => refs.update(pid, ref)

    case Disconnect(pid) => safe {
      refs.remove(pid)
      envoys.get(pid).foreach(_ ! Acid())
      envoys.remove(pid)
    }

    case StartOp(pid, oid, ast, ctx, op, vars) => refs.get(pid).foreach { ref =>
      val envoy = envoys.getOrElse(pid,
        context.spawn(
          behavior = Behaviors.setup(new Envoy[Ctx, Val](pid, ref, ctx, config, protocol, _)),
          name = pid,
        )
      )
      envoy ! Subscribe(oid, ast, op, vars)

      envoys.update(pid, envoy)
    }

    case StatelessOp(pid, oid, ast, ctx, op, vars) => refs.get(pid).foreach { ref =>
      context.pipeToSelf(execute(oid, ast, ctx, op, vars)) {
        case Failure(e) => OutError(ref, OperationMessage(protocol.error, oid, GqlError.of(e.getMessage)))
        case Success(value) => Outgoing(oid, ref, value)
      }
    }

    case StopOp(pid, oid) => envoys
      .get(pid)
      .foreach(_ ! Unsubscribe(oid))

    case Outgoing(oid, ref, en) => safe {
      ref <~ en
      ref <~ OperationMessage(protocol.complete, oid)
    }

    case OutError(ref, en) => safe {
      ref <~ en
    }
  }

  private def receive(msg: OverActions)(handler: OverActions => Unit): Behavior[OverActions] = {
    handler(msg)
    this
  }

  private def execute(
    oid: OID,
    queryAst: ast.Document,
    ctx: Any,
    operation: Option[String],
    vars: JsObject
  ): Future[OperationMessage] = Executor
    .execute(
      schema = config.schema,
      queryAst = queryAst,
      userContext = ctx.asInstanceOf[Ctx],
      root = config.root,
      operationName = operation,
      variables = vars,
      queryValidator = config.queryValidator,
      deferredResolver = config.deferredResolver,
      exceptionHandler = config.exceptionHandler,
      deprecationTracker = config.deprecationTracker,
      middleware = config.middleware,
      maxQueryDepth = config.maxQueryDepth,
      queryReducers = config.queryReducers,
    )
    .map(result => OperationMessage(protocol.next, oid, result))
    .recover {
      case error: QueryAnalysisError => OperationMessage(protocol.next, oid, error.resolveError)
      case error: ErrorWithResolver => OperationMessage(protocol.next, oid, error.resolveError)
    }

}

object OverEngine {

  /**
   * Setup a full Actor behavior using the ProxyStore
   */
  def behavior[Ctx, Val](
    protocol: OverWebsocket,
    config: SchemaConfig[Ctx, Val]
  ): Behavior[OverActions] = {
    Behaviors.setup(new OverEngine[Ctx, Val](protocol, config, _))
  }
}