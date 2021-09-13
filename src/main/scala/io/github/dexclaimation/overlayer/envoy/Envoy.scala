//
//  Envoy.scala
//  over-layer
//
//  Created by d-exclaimation on 11:41 PM.
//

package io.github.dexclaimation.overlayer.envoy

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.Materializer
import akka.stream.Materializer.createMaterializer
import io.github.dexclaimation.overlayer.envoy.EnvoyMessage._
import io.github.dexclaimation.overlayer.model.Subtypes.{PID, Ref}
import io.github.dexclaimation.overlayer.utils.ExceptionUtil.tolerate
import sangria.execution.ExecutionScheme.Stream
import sangria.execution.Executor
import sangria.marshalling.sprayJson._
import sangria.schema.Schema
import sangria.streaming.akkaStreams._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class Envoy[Ctx, Val](
  val pid: PID,
  val ref: Ref,
  val userContext: Any,
  val root: Val,
  val schema: Schema[Ctx, Val],
  override val context: ActorContext[EnvoyMessage]
) extends AbstractBehavior[EnvoyMessage](context) {

  implicit private val ex: ExecutionContext = context.executionContext
  implicit private val mat: Materializer = createMaterializer(context)

  def onMessage(msg: EnvoyMessage): Behavior[EnvoyMessage] = receive(msg) {
    case Subscribe(oid, ast, op, vars) => tolerate {
      val fut = Executor
        .execute(schema, ast, userContext.asInstanceOf[Ctx], root, op, vars)
        .map(_.compactPrint)
        .runForeach(ref.!(_)) // TODO: Format result

      context.pipeToSelf(fut) {
        case Success(_) => Ended(oid)
        case Failure(_) => Ignore()
      }
    }

    case Ended(oid) => // TODO: Send and Complete Message
    case _ => ()
  }


  private def receive(msg: EnvoyMessage)(handler: EnvoyMessage => Unit): Behavior[EnvoyMessage] = msg match {
    case Acid() => Behaviors.stopped[EnvoyMessage]
    case notStop =>
      handler(notStop)
      this
  }
}
