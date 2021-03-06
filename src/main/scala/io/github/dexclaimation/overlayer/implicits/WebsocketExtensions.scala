//
//  WebsocketExtensions.scala
//  over-layer
//
//  Created by d-exclaimation on 3:35 AM.
//

package io.github.dexclaimation.overlayer.implicits

import akka.http.scaladsl.model.ws.TextMessage
import io.github.dexclaimation.overlayer.model.Subtypes.Ref
import io.github.dexclaimation.overlayer.model.json.Encodable

/**
 * Websocket related extensions
 */
object WebsocketExtensions {

  /** Websocket Ref Extensions */
  implicit final class ExtendRef(ref: Ref) {

    /**
     * Send a encodable message to the Websocket connection referenced by this ActorRef using *at-most-once* messaging semantics.
     *
     * @param encodable Incoming json encodable data
     */
    def send(encodable: Encodable): Unit = ref.tell(encodable.json)

    /**
     * Send a encodable message to the Websocket connection referenced by this ActorRef using *at-most-once* messaging semantics.
     *
     * @param encodable Incoming json encodable data
     */
    def <~(encodable: Encodable): Unit = ref.tell(encodable.json)
  }


  /** Encodable Message Extensions */
  implicit final class ExtendEncodable(en: Encodable) {
    /** Encode into text message strict */
    def textMessage: TextMessage.Strict = TextMessage.Strict(en.json)
  }
}
