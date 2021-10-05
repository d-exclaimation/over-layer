//
//  RefTestProbe.scala
//  over-layer
//
//  Created by d-exclaimation on 10:08 PM.
//

package io.github.dexclaimation.overlayer.utils

import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{KillSwitches, OverflowStrategy}
import io.github.dexclaimation.overlayer.model.Subtypes.Ref

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

class CustomTestInbox(completeOn: String => Boolean = _ => false)(implicit system: ActorSystem[_]) {

  private val inbox = TestInbox[String]()
  private val prom = Promise[Unit]()
  private val consumer = Sink.foreach[String] { msg =>
    inbox.ref ! msg
    if (completeOn(msg)) {
      prom.success(())
    }
  }
  private val (actorRef, kill) = ActorSource
    .actorRef[String](PartialFunction.empty, PartialFunction.empty, 10, OverflowStrategy.dropHead)
    .viaMat(KillSwitches.single)(Keep.both)
    .to(consumer)
    .run()

  val ref: Ref = actorRef

  def receiveAll(): Seq[String] = inbox.receiveAll()

  def expectMessage(msg: String): TestInbox[String] = inbox.expectMessage(msg)

  def shutdown(): Unit = kill.shutdown()

  def await(dur: Duration = Duration.Inf): Unit = Await.result(prom.future, dur)
}