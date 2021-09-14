//
//  OverComposite.scala
//  over-layer
//
//  Created by d-exclaimation on 8:28 PM.
//


package io.github.dexclaimation.overlayer

import akka.actor.typed.ActorRef
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage
import spray.json.{DefaultJsonProtocol, RootJsonReader}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Over Composite Material for the json decoder.
 */
trait OverComposite extends DefaultJsonProtocol {
  def protocol: OverWebsocket

  implicit val graphDecoder: RootJsonReader[GraphMessage] = protocol.decoder

  implicit class FutureRef[T](actorRef: Future[ActorRef[T]]) {

    /**
     * Send a message to the Actor referenced by a Future of ActorRef using *at-most-once* messaging semantics.
     *
     * @param msg Message of the same time
     */
    def !!(msg: T)(implicit ex: ExecutionContext) = actorRef.foreach(_.!(msg))
  }
}
