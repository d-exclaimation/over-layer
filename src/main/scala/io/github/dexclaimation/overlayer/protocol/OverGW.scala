//
//  OverGW.scala
//  over-layer
//
//  Created by d-exclaimation on 5:27 PM.
//

package io.github.dexclaimation.overlayer.protocol

import io.github.dexclaimation.overlayer.protocol.common.GraphMessage
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage.GraphInit
import spray.json.JsValue

object OverGW extends OverWebsocket {
  def name = "graphql-transport-ws"

  // TODO: Handle Parser / Decoder
  def decoder(json: JsValue): GraphMessage = GraphInit()
}
