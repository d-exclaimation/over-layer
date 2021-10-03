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
import io.github.dexclaimation.overlayer.engine.OverActions._
import io.github.dexclaimation.overlayer.engine.{OverActions, OverEngine}
import io.github.dexclaimation.overlayer.implicits.StreamExtensions._
import io.github.dexclaimation.overlayer.implicits.WebsocketExtensions._
import io.github.dexclaimation.overlayer.model.Hooks._
import io.github.dexclaimation.overlayer.model.Subtypes.{PID, Ref}
import io.github.dexclaimation.overlayer.model.{PoisonPill, SchemaConfig}
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage._
import io.github.dexclaimation.overlayer.protocol.common.{GqlError, GraphMessage, OperationMessage}
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{DeprecationTracker, ExceptionHandler, Middleware, QueryReducer}
import sangria.schema.Schema
import sangria.validation.QueryValidator
import spray.json.JsonParser

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}

/**
 * GraphQL Transport Layer for handling distributed websocket based subscription.
 *
 * @param protocol        The GraphQL Over Websocket Sub-Protocol.
 * @param timeoutDuration The idle timeout duration.
 * @param bufferSize      The Buffer size of each publishers.
 * @param keepAlive       The periodical time to send a keep-alive message from server.
 * @param system          ''Implicit'' Actor System that allow for spawning child Actors.
 */
class OverTransportLayer[Ctx, Val](
  val config: SchemaConfig[Ctx, Val],
  val protocol: OverWebsocket = OverWebsocket.subscriptionsTransportWs,
  val timeoutDuration: Duration = Duration.Inf,
  val bufferSize: Int = 128,
  val keepAlive: FiniteDuration = 12.seconds,
)(implicit system: ActorSystem[SpawnProtocol.Command]) extends OverComposite {

  // --- Implicits ---
  implicit private val timed: Timeout = Timeout(10.seconds)
  implicit private val ex: ExecutionContext = system.executionContext


  private val engine = {
    val spawn = (rep: ActorRef[ActorRef[OverActions]]) => SpawnProtocol.Spawn(
      behavior = OverEngine.behavior[Ctx, Val](protocol, config),
      name = "ProxyStore",
      props = Props.empty,
      replyTo = rep
    )
    Await.result(system.ask(spawn), timeoutDuration)
  }

  private val FaultFunction: PartialFunction[String, Unit] = {
    case PoisonPill.Pattern => ()
  }

  /**
   * Websocket Handler Shorthand with the proper sub protocol and flow.
   *
   * ''Does not include a path, add this inside your path directives''
   *
   * @param ctx Context object used in the request.
   * @return A Route
   */
  def ws(ctx: Ctx): Route = handleWebSocketMessagesForProtocol(flow(ctx), protocol.name)

  /**
   * Websocket Flow with the proper types for Akka Websocket.
   *
   * @param ctx Given the required context.
   * @return A Flow that takes in and return a Message of type TextMessage.Strict.
   */
  def flow(ctx: Ctx): Flow[Message, TextMessage.Strict, _] = {
    val pid = PID()

    val (actorRef, publisher) = ActorSource
      .actorRef[String](
        completionMatcher = FaultFunction,
        failureMatcher = PartialFunction.empty,
        bufferSize = bufferSize,
        overflowStrategy = OverflowStrategy.dropHead
      )
      .map(TextMessage.Strict)
      .idleIfFinite(timeoutDuration)
      .toMat(Sink.asPublisher(false))(Keep.both)
      .run()

    val sink: Sink[Message, Any] = Sink
      .onComplete[Unit](onEnd(pid))
      .withBefore(onMessage(ctx, pid, actorRef))

    val source = Source
      .fromPublisher(publisher)
      .also(Source.tick(keepAlive, keepAlive, protocol.keepAlive))

    Flow
      .fromSinkAndSourceCoupled(sink, source)
  }


  /** onInit Hook */
  private def onInit(pid: String, ref: Ref): InitHook = {
    engine ! Connect(pid, ref)
    protocol.init(ref)
  }


  /** onMessage Hook */
  private def onMessage(ctx: Any, pid: String, ref: Ref): MessageHook = {
    case TextMessage.Strict(msg) => JsonParser(msg).convertTo[GraphMessage] match {
      case GraphInit() => onInit(pid, ref)

      case GraphStart(oid, ast, op, vars) => engine ! StartOp(pid, oid, ast, ctx, op, vars)

      case GraphImmediate(oid, ast, op, vars) => engine ! StatelessOp(pid, oid, ast, ctx, op, vars)

      case GraphStop(oid) => engine ! StopOp(pid, oid)

      case GraphError(oid, message) => ref <~ OperationMessage(protocol.error, oid, GqlError.of(message))

      case GraphException(message) => ref <~ OperationMessage(protocol.error, "4400", GqlError.of(message))

      case GraphPing() => ref <~ OperationMessage.just("pong")

      case GraphTerminate() => ref ! PoisonPill()

      case GraphIgnore() => ()
    }
    case _ => ()
  }

  /** onEnd Hook */
  private def onEnd(pid: String): EndHook = { _ =>
    engine ! Disconnect(pid)
  }

}


object OverTransportLayer {

  /**
   * Create a new ActorSystem that can be used for [[OverTransportLayer]]
   *
   * @param name Name of the ActorSystem.
   * @return A new ActorSystem
   */
  def makeSystem(name: String = s"OverSystemLayer-${PID()}"): ActorSystem[SpawnProtocol.Command] = {
    ActorSystem(behavior, name)
  }

  /**
   * Spawn Behaviour for main actor system
   *
   * @return Behaviour for Spawning
   */
  def behavior: Behavior[SpawnProtocol.Command] = Behaviors.setup(_ => SpawnProtocol())

  /**
   * Create a new instance of [[OverTransportLayer]] using direct configuration.
   *
   * @param schema             GraphQl Scheme used to execute subscriptions.
   * @param root               Root value object.
   * @param protocol           GraphQL over Websocket Transport Sub-Protocol.
   * @param queryValidator     Executor queryValidator.
   * @param deferredResolver   Any deferred resolver used by the executor.
   * @param exceptionHandler   Query Exception Handlers.
   * @param deprecationTracker Deprecation Trackers used by the executor.
   * @param middleware         Resolver middleware.
   * @param maxQueryDepth      Limit of the query depth can be resolved.
   * @param queryReducers      Query reducers for resolvers.
   * @param timeoutDuration    Idle timeout duration for websocket.
   * @param bufferSize         The websocket client buffer size.
   * @param keepAlive          The periodical time to send a keep-alive message from server.
   * @param sys                Implicit Actor System with he proper Behavior.
   * @tparam Ctx Context type of the Schema.
   * @tparam Val Root Value type.
   */
  def apply[Ctx, Val](
    schema: Schema[Ctx, Val],
    root: Val,
    protocol: OverWebsocket = OverWebsocket.subscriptionsTransportWs,
    queryValidator: QueryValidator = QueryValidator.default,
    deferredResolver: DeferredResolver[Ctx] = DeferredResolver.empty,
    exceptionHandler: ExceptionHandler = ExceptionHandler.empty,
    deprecationTracker: DeprecationTracker = DeprecationTracker.empty,
    middleware: List[Middleware[Ctx]] = Nil,
    maxQueryDepth: Option[Int] = None,
    queryReducers: List[QueryReducer[Ctx, _]] = Nil,
    timeoutDuration: Duration = Duration.Inf,
    keepAlive: FiniteDuration = 12.seconds,
    bufferSize: Int = 128
  )(implicit sys: ActorSystem[SpawnProtocol.Command]): OverTransportLayer[Ctx, Val] = {
    val config = SchemaConfig(
      schema, root, queryValidator,
      deferredResolver, exceptionHandler,
      deprecationTracker, middleware,
      maxQueryDepth, queryReducers
    )
    new OverTransportLayer[Ctx, Val](config, protocol, timeoutDuration, bufferSize, keepAlive)
  }
}
