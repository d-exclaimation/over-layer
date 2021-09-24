//
//  SubscriptionsTransportWsTest.scala
//  over-layer
//
//  Created by d-exclaimation on 11:07 PM.
//

package io.github.dexclaimation.overlayer.websocket

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Inlet, KillSwitches, Outlet}
import io.github.dexclaimation.overlayer.OverTransportLayer
import org.junit.Test
import sangria.execution.Executor
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SubscriptionsTransportWsTest {

  import Shared._

  private val transport = OverTransportLayer(demoSchema, ())

  private def wait(millis: Int)(action: => Unit): Unit = {
    Thread.sleep(millis)
    action
  }

  /**
   * '''Websocket'''
   *
   * 1. Properly Setup Flow Test
   */
  @Test def properlySetupFlow(): Unit = {

    val flow = transport.flow(ctx)

    val inMatch: PartialFunction[Any, Unit] = {
      case _: Inlet[Message] => ()
    }

    val outMatch: PartialFunction[Any, Unit] = {
      case _: Outlet[Message] => ()
    }

    inMatch(flow.shape.in)
    outMatch(flow.shape.out)
  }

  @Test def initializeConnection(): Unit = {
    val source =
      Source(
        Seq(
          JsObject(
            "type" -> JsString("connection_init"),
            "payload" -> JsObject.empty
          )
        )
      )
        .map(_.compactPrint)
        .map(TextMessage.Strict.apply)

    val sink = Sink.takeLast[TextMessage.Strict](2)
    val kill = KillSwitches.shared("ok")

    val (_, fut) = transport.flow(ctx)
      .via(kill.flow)
      .runWith(source, sink)

    wait(100) {
      kill.shutdown()
    }

    val store = Await.result(fut, Duration.Inf)
    assert(store.nonEmpty)
    assert(store.length == 2)
    assert(store.last.text == JsObject("type" -> JsString("ka")).compactPrint)
    assert(store.head.text == JsObject("type" -> JsString("connection_ack")).compactPrint)
  }

  @Test def startOperation(): Unit = {
    val source =
      Source(
        Seq(
          JsObject(
            "type" -> JsString("connection_init"),
            "payload" -> JsObject.empty
          ),
          JsObject(
            "type" -> JsString("start"),
            "id" -> JsString("1"),
            "payload" -> JsObject(
              "query" -> JsString("subscription { state }")
            )
          )
        )
      )
        .map(_.compactPrint)
        .map(TextMessage.Strict.apply)

    val kill = KillSwitches.shared("good")
    val sink = Sink.takeLast[TextMessage.Strict](1)
    val (_, fut) = transport.flow(ctx)
      .via(kill.flow)
      .runWith(source, sink)

    QueryParser.parse("mutation { increment }")
      .map { doc => Executor.execute(demoSchema, doc, ctx, ()) }

    wait(100) {
      kill.shutdown()
    }

    val Seq(TextMessage.Strict(_)) = Await.result(fut, Duration.Inf)

  }


  @Test def faultyStartOperation(): Unit = {
    val source = Source(
      Seq(
        JsObject(
          "type" -> JsString("connection_init"),
          "payload" -> JsObject.empty
        ),
        JsObject(
          "type" -> JsString("start"),
          "id" -> JsString("1"),
          "payload" -> JsObject(
            "query" -> JsString("query { state }")
          )
        )
      )
    )
      .map(_.compactPrint)
      .map(TextMessage.Strict.apply)


    val sink = Sink.takeLast[TextMessage.Strict](1)

    val kill = KillSwitches.shared("ok")

    val (_, future) = transport.flow(ctx)
      .via(kill.flow)
      .runWith(source, sink)

    wait(100) {
      kill.shutdown()
    }

    val messages = Await.result(future, Duration.Inf)
    assert(messages.last.text
      .equals(
        JsObject(
          "type" -> JsString("error"),
          "payload" -> JsString("Cannot perform operation other than subscriptions through websocket")
        ).compactPrint
      )
    )
  }
}
