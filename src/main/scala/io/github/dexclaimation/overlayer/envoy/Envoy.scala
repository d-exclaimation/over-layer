//
//  Envoy.scala
//  over-layer
//
//  Created by d-exclaimation on 11:41 PM.
//

package io.github.dexclaimation.overlayer.envoy

import akka.Done
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.Materializer.createMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import io.github.dexclaimation.overlayer.envoy.EnvoyMessage._
import io.github.dexclaimation.overlayer.model.SchemaConfig
import io.github.dexclaimation.overlayer.model.Subtypes.{OID, PID, Ref}
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.ProtoMessage
import io.github.dexclaimation.overlayer.utils.ExceptionUtil.tolerate
import sangria.ast.Document
import sangria.execution.ExecutionScheme.Stream
import sangria.execution.Executor
import sangria.marshalling.sprayJson._
import sangria.streaming.akkaStreams._
import spray.json.{JsObject, JsValue}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Envoy Actor for handling specific operation stream for one websocket client.
 *
 * @param pid         Websocket Client PID.
 * @param ref         User client ActorRef.
 * @param userContext Request Defined Schema Context.
 * @param config      Schema Config for setting up Executor.
 * @param protocol    GraphQL Over Websocket sub protocol.
 * @param context     ActorSystem Behavior Context.
 */
class Envoy[Ctx, Val](
  val pid: PID,
  val ref: Ref,
  val userContext: Any,
  val config: SchemaConfig[Ctx, Val],
  val protocol: OverWebsocket,
  override val context: ActorContext[EnvoyMessage]
) extends AbstractBehavior[EnvoyMessage](context) {

  implicit private val ex: ExecutionContext = context.executionContext
  implicit private val mat: Materializer = createMaterializer(context)

  private val switches: mutable.Map[OID, KillSwitch] =
    mutable.Map.empty[OID, KillSwitch]

  def onMessage(msg: EnvoyMessage): Behavior[EnvoyMessage] = receive(msg) {
    case Subscribe(oid, ast, op, vars) => tolerate {
      val switch = KillSwitches.shared(oid)

      val sink = Flow[String]
        .map(ref ! _)
        .via(switch.flow)
        .to(Sink.onComplete(onEnded(oid)))

      executeSchema(ast, op, vars)
        .map(ProtoMessage.Operation(protocol.name, oid, _))
        .map(_.json)
        .to(sink)
        .run()

      switches.update(oid, switch)
    }

    case Unsubscribe(oid) => tolerate {
      switches.get(oid)
        .foreach(_.shutdown())
    }

    case Ended(oid) => ref.tell(ProtoMessage.NoPayload(protocol.complete, oid).json)

    case _ => ()
  }


  private def receive(msg: EnvoyMessage)(handler: EnvoyMessage => Unit): Behavior[EnvoyMessage] = msg match {
    case Acid() =>
      switches.values.foreach(_.shutdown())
      Behaviors.stopped[EnvoyMessage]
    case notStop =>
      handler(notStop)
      this
  }

  private def executeSchema(ast: Document, op: Option[String], vars: JsObject): AkkaSource[JsValue] = Executor
    .execute(
      schema = config.schema,
      queryAst = ast,
      userContext = userContext.asInstanceOf[Ctx],
      root = config.root,
      operationName = op,
      variables = vars,
      queryValidator = config.queryValidator,
      deferredResolver = config.deferredResolver,
      exceptionHandler = config.exceptionHandler,
      deprecationTracker = config.deprecationTracker,
      middleware = config.middleware,
      maxQueryDepth = config.maxQueryDepth,
      queryReducers = config.queryReducers
    )

  private def onEnded(oid: OID): Try[Done] => Unit = { _ =>
    context.self ! Ended(oid)
  }
}

