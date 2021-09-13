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

trait OverComposite extends DefaultJsonProtocol {
  def protocol: OverWebsocket

  implicit val graphDecoder: RootJsonReader[GraphMessage] = protocol.decoder

  implicit class FutureRef[T](actorRef: Future[ActorRef[T]]) {
    def !!(msg: T)(implicit ex: ExecutionContext) = actorRef.foreach(_.!(msg))
  }
}
