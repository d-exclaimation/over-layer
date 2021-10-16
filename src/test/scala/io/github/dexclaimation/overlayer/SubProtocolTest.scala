//
//  SubProtocolTest.scala
//  over-layer
//
//  Created by d-exclaimation on 5:36 PM.
//

package io.github.dexclaimation.overlayer

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.{GraphMessage, OperationMessage}
import io.github.dexclaimation.overlayer.utils.CustomTestInbox
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class SubProtocolTest extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system

  "subscriptions-transport-ws" when {
    val protocol = OverWebsocket.subscriptionsTransportWs

    val GQL_START = "start"
    val GQL_STOP = "stop"
    val GQL_DATA = "data"
    val GQL_ERROR = "error"
    val GQL_COMPLETE = "complete"

    val GQL_CONNECTION_TERMINATE = "connection_terminate"
    val GQL_CONNECTION_KEEP_ALIVE = "ka"
    val GQL_CONNECTION_INIT = "connection_init"

    "accessed" should {
      "have a sub-protocol name of `graphql-ws`" in {
        assertResult("graphql-ws") {
          protocol.name
        }
      }

      "have the proper operation message `type`s" in {
        assertResult(GQL_DATA)(protocol.next)
        assertResult(GQL_COMPLETE)(protocol.complete)
        assertResult(GQL_ERROR)(protocol.error)
      }

      "have a keepAlive TextMessage Strict with the proper GQL_CONNECTION_KEEP_ALIVE" in {
        assert(protocol.keepAlive.text.contains(GQL_CONNECTION_KEEP_ALIVE))
      }
    }

    "given GQL_CONNECTION_INIT" should {
      "return GraphInit" in {
        val initReq = JsonParser(OperationMessage.just(GQL_CONNECTION_INIT).json)
        protocol.decoder(initReq) match {
          case GraphMessage.Init() =>
            val inbox = new CustomTestInbox(_.contains("stop"))

            protocol.init(inbox.ref)
            inbox.ref ! "stop"

            inbox.await()
            val res = inbox.receiveAll().slice(0, 2)

            assertResult(2)(res.length)
            assert(res.map(_.contains("type")).reduce(_ && _))

          case r => fail(s"Incorrect parsing into GraphMessage returned $r")
        }
      }
    }

    "given  GQL_START, GQL_STOP, GQL_CONNECTION_TERMINATE" should {
      "return the proper GraphMessage" in {
        val start = OperationMessage(GQL_START, "1", JsObject("query" -> JsString("subscription { fakeField }")))
        val immediate = OperationMessage(GQL_START, "2", JsObject("query" -> JsString("query { fakeField }")))
        val stop = OperationMessage(GQL_STOP, "1")
        val terminate = OperationMessage.just(GQL_CONNECTION_TERMINATE)

        decode(protocol, start.json) {
          case GraphMessage.Start(oid, _, None, JsObject.empty) => assertResult("1")(oid)
          case _ => fail("GQL_START does not return GraphStart or GraphImmediate")
        }

        decode(protocol, stop.json) {
          case GraphMessage.Stop(oid) => assertResult("1")(oid)
          case _ => fail("GQL_STOP does not return GraphStop")
        }

        decode(protocol, immediate.json) {
          case GraphMessage.Req(oid, _, None, JsObject.empty) => assertResult("2")(oid)
          case _ => fail("GQL_START does not return GraphStart or GraphImmediate")
        }

        decode(protocol, terminate.json) {
          case GraphMessage.Terminate() => assertResult(true)(true)
          case _ => fail("GQL_CONNECTION_TERMINATE does not return GraphTerminate")
        }
      }
    }
  }

  "graphql-ws" when {
    val protocol = OverWebsocket.graphqlWs

    val Subscribe = "subscribe"
    val Next = "next"
    val Error = "error"
    val Complete = "complete"

    val Ping = "ping"
    val Pong = "pong"
    val ConnectionInit = "connection_init"

    "accessed" should {
      "have a sub-protocol name of `graphql-transport-ws`" in {
        assertResult("graphql-transport-ws")(protocol.name)
      }

      "have the proper operation message `type`s" in {
        assertResult(Next)(protocol.next)
        assertResult(Complete)(protocol.complete)
        assertResult(Error)(protocol.error)
      }

      "have a keepAlive TextMessage Strict with the proper Ping" in {
        assert(protocol.keepAlive.text.contains(Ping))
      }
    }

    "given ConnectionInit" should {
      "return GraphInit" in {
        decode(protocol, OperationMessage.just(ConnectionInit).json) {
          case GraphMessage.Init() =>
            val inbox = new CustomTestInbox(_.contains("stop"))

            protocol.init(inbox.ref)
            inbox.ref ! "stop"
            inbox.await()

            val res = inbox.receiveAll().head

            assert(res.contains("type"))

          case r => fail(s"Incorrect parsing into GraphMessage returned $r")
        }
      }
    }

    "given Subscribe, Complete, Pong, Ping" should {
      "return the proper GraphMessage" in {
        val start = OperationMessage(Subscribe, "1", JsObject("query" -> JsString("subscription { fakeField }")))
        val immediate = OperationMessage(Subscribe, "2", JsObject("query" -> JsString("query { fakeField }")))
        val stop = OperationMessage(Complete, "1")
        val pong = OperationMessage.just(Pong)
        val ping = OperationMessage.just(Ping)

        decode(protocol, start.json) {
          case GraphMessage.Start(oid, _, None, JsObject.empty) => assertResult("1")(oid)
          case _ => fail("Subscribe does not return GraphStart or GraphImmediate")
        }

        decode(protocol, stop.json) {
          case GraphMessage.Stop(oid) => assertResult("1")(oid)
          case _ => fail("Complete does not return GraphStop")
        }

        decode(protocol, immediate.json) {
          case GraphMessage.Req(oid, _, None, JsObject.empty) => assertResult("2")(oid)
          case _ => fail("Subscribe does not return GraphStart or GraphImmediate")
        }

        decode(protocol, pong.json) {
          case GraphMessage.Ignore() => assertResult(true)(true)
          case _ => fail("Pong does not return GraphIgnored")
        }

        decode(protocol, ping.json) {
          case GraphMessage.Ping() => assertResult(true)(true)
          case _ => fail("Pong does not return GraphIgnored")
        }
      }
    }
  }

  override def afterAll() = {
    super.afterAll()
    testKit.shutdownTestKit()
  }

  def decode(protocol: OverWebsocket, req: String)(fn: GraphMessage => Unit): Unit =
    fn(protocol.decoder(JsonParser(req)))

}
