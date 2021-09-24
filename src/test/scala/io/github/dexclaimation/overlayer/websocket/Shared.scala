package io.github.dexclaimation.overlayer.websocket

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{Materializer, OverflowStrategy}
import io.github.dexclaimation.overlayer.OverTransportLayer
import sangria.schema.{Action, Field, IntType, ObjectType, Schema, fields}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext

object Shared {
  implicit val testSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(OverTransportLayer.behavior,
    "TestActorSystem"
  )

  implicit val ex: ExecutionContext = testSystem.executionContext

  implicit val mat: Materializer = Materializer.createMaterializer(testSystem)

  case class Ctx() {
    val atomic = new AtomicInteger()
    val (actorRef, publisher) = ActorSource
      .actorRef[Int](
        completionMatcher = {
          case 100 => ()
        },
        failureMatcher = PartialFunction.empty,
        bufferSize = 100,
        overflowStrategy = OverflowStrategy.dropHead
      )
      .toMat(Sink.asPublisher(true))(Keep.both)
      .run()

    val source = Source.fromPublisher(publisher)
  }

  val ctx = Ctx()

  val demoSchema = {
    import sangria.streaming.akkaStreams._

    val Query = ObjectType("Query",
      fields[Ctx, Unit](
        Field("state", IntType, resolve = _.ctx.atomic.get())
      )
    )

    val Mutation = ObjectType("Mutation",
      fields[Ctx, Unit](
        Field("increment", IntType,
          resolve = c => {
            val res = c.ctx.atomic.incrementAndGet()
            c.ctx.actorRef ! res
            res
          }
        )
      )
    )

    val Subscription = ObjectType("Subscription",
      fields[Ctx, Unit](
        Field.subs("state", IntType, resolve = _.ctx.source.map(Action(_)))
      )
    )

    Schema(Query, Some(Mutation), Some(Subscription))
  }

}
