//
//  EnvoyTest.scala
//  over-layer
//
//  Created by d-exclaimation on 9:11 PM.
//

package io.github.dexclaimation.overlayer

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import io.github.dexclaimation.overlayer.envoy.{Envoy, EnvoyMessage}
import io.github.dexclaimation.overlayer.model.SchemaConfig
import io.github.dexclaimation.overlayer.model.Subtypes.{PID, Ref}
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.utils.CustomTestInbox
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import sangria.macros.LiteralGraphQLStringContext
import sangria.schema.{Action, Field, ObjectType, Schema, StringType, fields}
import sangria.streaming.akkaStreams._
import spray.json.JsObject

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.util.Try

class EnvoyTest extends AnyWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val base = ActorTestKit()
  implicit val system: ActorSystem[Nothing] = base.system
  implicit val ex: ExecutionContext = system.executionContext

  val schema = {
    def delayed(): Future[String] = Future(Thread.sleep(500)).map(_ => "Hello")

    val q = ObjectType("Query", fields[Unit, Unit](Field("test", StringType, resolve = _ => "Test")))
    val s = ObjectType("Query",
      fields[Unit, Unit](
        Field.subs("test", StringType, resolve = _ => Source.single("Hello").map(Action(_))),
        Field.subs("delayedTest", StringType, resolve = _ => Source.future(delayed()).map(Action(_))),
      )
    )
    Schema(q, subscription = Some(s))
  }

  def makeEnvoy(ref: Ref) = Behaviors
    .setup[EnvoyMessage](
      new Envoy[Unit, Unit](PID(), ref, (), SchemaConfig(schema, ()), OverWebsocket.subscriptionsTransportWs, _)
    )

  var mailbox = new CustomTestInbox(_.contains("complete"))
  var envoy = base.spawn(makeEnvoy(mailbox.ref))

  "Envoy" when {
    "given just Subscribe message" should {
      "send stream data and complete message" in {
        val subAst = graphql"subscription { test }"
        envoy ! EnvoyMessage.Subscribe("1", subAst, None, JsObject.empty)

        mailbox.await()
        val Seq(data, complete) = mailbox.receiveAll()

        assert(data.contains("payload") && data.contains("Hello") && data.contains("1"))
        assert(complete.contains("1"))
      }
    }

    "given Stop after Subscribe" should {
      "not send stream data if Stop was immediately send before completion" in {
        val subAst = graphql"subscription { delayedTest }"

        envoy ! EnvoyMessage.Subscribe("2", subAst, None, JsObject.empty)
        envoy ! EnvoyMessage.Unsubscribe("2")

        assertThrows[TimeoutException] {
          mailbox.await(1.seconds)
        }
      }
    }

    "given Acid" should {
      "can be killed using Acid and cancel all subscription" in {
        val subAst = graphql"subscription { delayedTest }"

        envoy ! EnvoyMessage.Subscribe("3", subAst, None, JsObject.empty)
        envoy ! EnvoyMessage.Acid()

        assertThrows[TimeoutException] {
          mailbox.await(1.seconds)
        }
      }
    }
  }

  override def beforeEach() = {
    super.beforeEach()
    mailbox = new CustomTestInbox(_.contains("complete"))
    envoy = base.spawn(makeEnvoy(mailbox.ref))
  }

  override def afterEach() = {
    super.afterEach()
    Try {
      base.stop(envoy, 10.seconds)
    }
    mailbox.shutdown()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    base.shutdownTestKit()
  }
}
