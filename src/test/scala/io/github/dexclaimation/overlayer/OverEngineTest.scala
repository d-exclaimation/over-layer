//
//  OverEngineTest.scala
//  over-layer
//
//  Created by d-exclaimation on 10:36 PM.
//

package io.github.dexclaimation.overlayer

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import io.github.dexclaimation.overlayer.engine.{OverActions, OverEngine}
import io.github.dexclaimation.overlayer.model.SchemaConfig
import io.github.dexclaimation.overlayer.model.Subtypes.PID
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.OperationMessage
import io.github.dexclaimation.overlayer.utils.CustomTestInbox
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import sangria.macros.LiteralGraphQLStringContext
import sangria.schema.{Action, Field, ObjectType, Schema, StringType, fields}
import sangria.streaming.akkaStreams._
import spray.json.{JsNull, JsObject, JsString}

import scala.concurrent.duration.DurationInt

class OverEngineTest extends AnyWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val base = ActorTestKit()
  implicit val system: ActorSystem[_] = base.system

  val schema = {
    val q = ObjectType("Query", fields[Unit, Unit](Field("test", StringType, resolve = _ => "Test")))
    val s = ObjectType("Query",
      fields[Unit, Unit](
        Field.subs("test", StringType, resolve = _ => Source.single("Hello").map(Action(_)))
      )
    )
    Schema(q, subscription = Some(s))
  }

  var inbox = new CustomTestInbox(msg => msg.contains("complete") || msg.contains("error"))
  var testKit = base.spawn(OverEngine.behavior(OverWebsocket.graphqlWs, SchemaConfig(schema, ())))

  "OverEngine" when {

    "given internal message" should {
      val en = OperationMessage("next", "1", JsObject("data" -> JsNull))
      val err = OperationMessage("error", "1", JsObject("data" -> JsNull))

      "return the outgoing data and completion" in {
        testKit ! OverActions.Outgoing("1", inbox.ref, en)
        inbox.await()
        inbox
          .expectMessage(en.json)
          .expectMessage(OperationMessage("complete", "1").json)
      }

      "return the error data" in {
        testKit ! OverActions.OutError(inbox.ref, err)
        inbox.await()
        inbox.expectMessage(err.json)
      }

    }


    "given stateless request" should {
      val pid = PID()
      val req = graphql"query { test }"
      val reqResult = OperationMessage("next", "2", JsObject("data" -> JsObject("test" -> JsString("Test"))))
      val invalid = graphql"query { idk }"

      "return the outgoing data and completion" in {
        testKit ! OverActions.Connect(pid, inbox.ref)
        testKit ! OverActions.StatelessOp(pid, "2", req, (), None, JsObject.empty)
        inbox.await()
        inbox
          .expectMessage(reqResult.json)
          .expectMessage(OperationMessage("complete", "2").json)
      }

      "return an error response" in {
        testKit ! OverActions.Connect(pid, inbox.ref)
        testKit ! OverActions.StatelessOp(pid, "2", invalid, (), None, JsObject.empty)
        inbox.await()
        val results = inbox.receiveAll()
        assert(results.head.contains("error"))
      }
    }


    "given stateful request" should {
      val pid = PID()
      val req = graphql"subscription { test }"
      val reqResult = OperationMessage("next", "3", JsObject("data" -> JsObject("test" -> JsString("Hello"))))

      "return the streaming data and completion" in {
        testKit ! OverActions.Connect(pid, inbox.ref)
        testKit ! OverActions.StartOp(pid, "3", req, (), None, JsObject.empty)
        inbox.await()
        inbox
          .expectMessage(reqResult.json)
          .expectMessage(OperationMessage("complete", "3").json)
      }
    }
  }

  override def beforeEach() = {
    super.beforeEach()
    inbox = new CustomTestInbox(msg => msg.contains("complete") || msg.contains("error"))
    testKit = base.spawn(OverEngine.behavior(OverWebsocket.graphqlWs, SchemaConfig(schema, ())))
  }

  override def afterEach() = {
    super.afterEach()
    inbox.shutdown()
    base.stop(testKit, 20.seconds)
  }

  override def afterAll() = {
    super.afterAll()
    base.shutdownTestKit()
  }
}
