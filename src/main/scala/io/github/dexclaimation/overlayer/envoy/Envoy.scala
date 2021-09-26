//
//  Envoy.scala
//  over-layer
//
//  Created by d-exclaimation on 11:41 PM.
//

package io.github.dexclaimation.overlayer.envoy

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.Materializer.createMaterializer
import akka.stream.scaladsl.{Flow, Keep}
import akka.stream.typed.scaladsl.ActorSink
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import io.github.dexclaimation.overlayer.envoy.EnvoyMessage._
import io.github.dexclaimation.overlayer.implicits.WebsocketExtensions._
import io.github.dexclaimation.overlayer.model.SchemaConfig
import io.github.dexclaimation.overlayer.model.Subtypes.{OID, PID, Ref}
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.OpMsg
import io.github.dexclaimation.overlayer.utils.ExceptionUtil.tolerate
import sangria.ast.Document
import sangria.execution.ExecutionScheme.Stream
import sangria.execution.Executor
import sangria.marshalling.sprayJson._
import sangria.streaming.akkaStreams._
import spray.json.{JsObject, JsValue}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

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

  private val ops = mutable.Map.empty[OID, KillSwitch]

  def onMessage(msg: EnvoyMessage): Behavior[EnvoyMessage] = receive(msg) {
    case Subscribe(oid, ast, op, vars) => tolerate {
      val sink = ActorSink
        .actorRef[EnvoyMessage](
          context.self,
          onCompleteMessage = Ended(oid),
          onFailureMessage = _ => Subscribe(oid, ast, op, vars)
        )

      val flow = Flow[JsValue]
        .map(OpMsg(protocol.next, oid, _))
        .map(Output(oid, _))

      val kill = executeSchema(ast, op, vars)
        .via(flow)
        .viaMat(KillSwitches.single)(Keep.right)
        .to(sink)
        .run()

      ops.update(oid, kill)
    }

    case Unsubscribe(oid) => tolerate {
      ops.remove(oid)
    }

    case Ended(oid) => ops.get(oid)
      .foreach { _ =>
        ref ? OpMsg.NoPayload(protocol.complete, oid)
        ops.remove(oid)
      }

    case Output(oid, data) => tolerate {
      if (ops.contains(oid)) ref ? data
    }

    case _ => ()
  }


  private def receive(msg: EnvoyMessage)(handler: EnvoyMessage => Unit): Behavior[EnvoyMessage] = msg match {
    case Acid() =>
      ops.values.foreach(_.shutdown())
      ops.clear()
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
}

