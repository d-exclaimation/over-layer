//
//  ActorExtensions.scala
//  over-layer
//
//  Created by d-exclaimation on 3:00 PM.
//

package io.github.dexclaimation.overlayer.implicits

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout

import scala.concurrent.{Await, Future}

/** Actor Related Extensions */
object ActorExtensions {

  /** Ask Pattern Extensions */
  implicit final class AskableExtend[Req](ref: RecipientRef[Req]) {

    /**
     * Ask and await a request response from an Askable
     *
     * @param replyTo ActorRef for the Actor to complete the request.
     * @param to      Timeout for the ask pattern.
     * @param sch     Scheduler for ask pattern.
     * @tparam Res Result type
     */
    def call[Res](replyTo: ActorRef[Res] => Req)
      (implicit to: Timeout, sch: Scheduler): Res = Await.result(ref.ask[Res](replyTo), to.duration)

    /**
     * Elvis operation for ask and await a request response from an Askable
     *
     * @param replyTo ActorRef for the Actor to complete the request.
     * @param to      Timeout for the ask pattern.
     * @param sch     Scheduler for ask pattern.
     * @tparam Res Result type
     */
    def ?:[Res](replyTo: ActorRef[Res] => Req)
      (implicit to: Timeout, sch: Scheduler): Res = call[Res](replyTo)
  }

  /** Spawn protocol Extensions */
  implicit final class SpawnExtend(ref: RecipientRef[SpawnProtocol.Command]) {

    /**
     * Utilising ask-pattern and spawn-protocol to spawn Actor outside the Actor tree
     *
     * @param behavior Behavior for the Actor.
     * @param name     Name of the Actor.
     * @param props    Props for the Actor.
     * @param to       Timeout for the ask pattern.
     * @param sch      Scheduler for ask pattern.
     * @tparam T Message type for the behavior
     */
    def spawn[T](
      behavior: Behavior[T],
      name: String,
      props: Props = Props.empty
    )(implicit to: Timeout, sch: Scheduler): ActorRef[T] =
      ref.call[ActorRef[T]](replyTo => SpawnProtocol.Spawn(behavior, name, props, replyTo))


    /**
     * Asynchronously utilising ask-pattern and spawn-protocol to spawn Actor outside the Actor tree
     *
     * @param behavior Behavior for the Actor.
     * @param name     Name of the Actor.
     * @param props    Props for the Actor.
     * @param to       Timeout for the ask pattern.
     * @param sch      Scheduler for ask pattern.
     * @tparam T Message type for the behavior
     */
    def launch[T](
      behavior: Behavior[T],
      name: String,
      props: Props = Props.empty
    )(implicit to: Timeout, sch: Scheduler): Future[ActorRef[T]] =
      ref.ask[ActorRef[T]](replyTo => SpawnProtocol.Spawn(behavior, name, props, replyTo))
  }

}
