//
//  TransportLayer.scala
//  over-layer
//
//  Created by d-exclaimation on 3:26 PM.
//

package io.github.dexclaimation.overlayer

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.util.Timeout
import org.reactivestreams.Publisher

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Try

class TransportLayer[TContext](
  val timeoutDuration: FiniteDuration = 30.seconds,
  val bufferSize: Int = 16
)(implicit system: ActorSystem[SpawnProtocol.Command]) {
  implicit private val keepAlive: Timeout = Timeout(timeoutDuration)
  implicit private val ex: ExecutionContext = system.executionContext


  private val PoisonPill: String = "@Websocket.PoisonPill"

  private val FaultFunction: PartialFunction[String, Throwable] = {
    case PoisonPill => new Error("Websocket connection is being shut down")
  }

  def flow(ctx: TContext): Flow[Message, TextMessage.Strict, _] = {
    val pid = UUID.randomUUID().toString

    val (actorRef: ActorRef[String], publisher: Publisher[TextMessage.Strict]) =
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


    // TODO: Add Connect Hook

    val sink: Sink[Message, Any] = Flow[Message]
      .map(onMessage(ctx, pid, actorRef))
      .to(Sink.onComplete(onEnd(ctx, pid, actorRef)))

    Flow.fromSinkAndSource(sink, Source.fromPublisher(publisher))
  }


  type IncomingFunction = Message => Unit

  private def onMessage(ctx: Any, pid: String, actorRef: ActorRef[String]): IncomingFunction = {
    _ => ()
  }

  type EndFunction = Try[Done] => Unit

  private def onEnd(ctx: Any, pid: String, actorRef: ActorRef[String]): EndFunction = {
    _ => ()
  }
}

