//
//  TransportLayer.scala
//  over-layer
//
//  Created by d-exclaimation on 3:26 PM.
//

package io.github.dexclaimation.overlayer

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.handleWebSocketMessagesForProtocol
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.util.Timeout
import io.github.dexclaimation.overlayer.model.Hooks._
import io.github.dexclaimation.overlayer.model.Subtypes.{PID, Ref}
import io.github.dexclaimation.overlayer.model.{PoisonPill, SchemaConfig}
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage._
import io.github.dexclaimation.overlayer.protocol.common.{GraphMessage, ProtoMessage}
import io.github.dexclaimation.overlayer.proxy.ProxyActions.{Connect, Disconnect, StartOp, StopOp}
import io.github.dexclaimation.overlayer.proxy.{ProxyActions, ProxyStore}
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{DeprecationTracker, ExceptionHandler, Middleware, QueryReducer}
import sangria.schema.Schema
import sangria.validation.QueryValidator
import spray.json.{JsString, JsonParser}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * GraphQL Transport Layer for handling distributed websocket based subscription.
 *
 * @param protocol        The GraphQL Over Websocket Sub-Protocol.
 * @param timeoutDuration The idle timeout duration.
 * @param bufferSize      The Buffer size of each publishers.
 * @param system          ''Implicit'' Actor System that allow for spawning child Actors.
 * @todo Actor Behaviour for client distribution
 */
class OverTransportLayer[Ctx, Val](
  schema: Schema[Ctx, Val],
  root: Val,
  val protocol: OverWebsocket = OverWebsocket.subscriptionsTransportWs,
  queryValidator: QueryValidator = QueryValidator.default,
  deferredResolver: DeferredResolver[Ctx] = DeferredResolver.empty,
  exceptionHandler: ExceptionHandler = ExceptionHandler.empty,
  deprecationTracker: DeprecationTracker = DeprecationTracker.empty,
  middleware: List[Middleware[Ctx]] = Nil,
  maxQueryDepth: Option[Int] = None,
  queryReducers: List[QueryReducer[Ctx, _]] = Nil,
  timeoutDuration: FiniteDuration = 30.seconds,
  bufferSize: Int = 16
)(implicit system: ActorSystem[SpawnProtocol.Command]) extends OverComposite {
  implicit private val keepAlive: Timeout = Timeout(timeoutDuration)
  implicit private val ex: ExecutionContext = system.executionContext

  private val proxy = system.ask[ActorRef[ProxyActions]] { rep =>
    val config = SchemaConfig(
      schema, root, queryValidator,
      deferredResolver, exceptionHandler,
      deprecationTracker, middleware,
      maxQueryDepth, queryReducers
    )
    SpawnProtocol.Spawn(
      behavior = ProxyStore.behavior[Ctx, Val](protocol, config),
      name = "ProxyStore",
      props = Props.empty,
      replyTo = rep
    )
  }

  private val FaultFunction: PartialFunction[String, Throwable] = {
    case PoisonPill.Pattern => new Error("Websocket connection is being shut down due to a PoisonPill Message")
  }

  /**
   * Websocket Flow with the proper types for Akka Websocket.
   *
   * @param ctx Given the required context.
   * @return A Flow that takes in and return a Message of type TextMessage.Strict.
   */
  def flow(ctx: Ctx): Flow[Message, TextMessage.Strict, _] = {
    val pid = PID()

    val (actorRef, publisher) =
      ActorSource
        .actorRef[String](
          completionMatcher = PartialFunction.empty,
          failureMatcher = FaultFunction,
          bufferSize = bufferSize,
          overflowStrategy = OverflowStrategy.dropHead
        )
        .map(TextMessage.Strict)
        .toMat(Sink.asPublisher(false))(Keep.both)
        .run()

    onInit(pid, actorRef)

    val sink: Sink[Message, Any] = Flow[Message]
      .map(onMessage(ctx, pid, actorRef))
      .to(Sink.onComplete(onEnd(pid)))

    Flow.fromSinkAndSource(sink, Source.fromPublisher(publisher))
  }

  def ws(ctx: Ctx): Route = {
    handleWebSocketMessagesForProtocol(flow(ctx), protocol.name)
  }

  /** onInit Hook */
  private def onInit(pid: String, ref: Ref): InitHook = {
    proxy !! Connect(pid, ref)
  }


  /** onMessage Hook */
  private def onMessage(ctx: Any, pid: String, ref: Ref): MessageHook = {
    case TextMessage.Strict(msg) => JsonParser(msg).convertTo[GraphMessage] match {
      case GraphInit() => protocol.init(ref)

      case GraphStart(oid, ast, op, vars) => proxy !! StartOp(pid, oid, ast, ctx, op, vars)

      case GraphStop(oid) => proxy !! StopOp(pid, oid)

      case GraphError(msg) => ref.!(ProtoMessage.NoID(protocol.error, JsString(msg)).json)

      case GraphPing() => ref.!(ProtoMessage.Empty("pong").json)

      case GraphTerminate() => ref ! PoisonPill()
    }
    case _ => ()
  }


  /** onEnd Hook */
  private def onEnd(pid: String): EndHook = { _ =>
    proxy !! Disconnect(pid)
  }

}


object OverTransportLayer {

  /**
   * Spawn Behaviour for main actor system
   *
   * @return Behaviour for Spawning
   */
  def behavior: Behavior[SpawnProtocol.Command] = Behaviors.setup(_ => SpawnProtocol())
}
