//
//  OverWebsocket.scala
//  over-layer
//
//  Created by d-exclaimation on 5:20 PM.
//


package io.github.dexclaimation.overlayer.protocol

import io.github.dexclaimation.overlayer.protocol.common.GraphMessage
import spray.json.JsValue

trait OverWebsocket {
  def name: String

  def decoder(json: JsValue): GraphMessage
}


object OverWebsocket {
  val subscriptionsTransportWs: OverWebsocket = OverSTW

  val graphqlWs: OverWebsocket = OverGW
}