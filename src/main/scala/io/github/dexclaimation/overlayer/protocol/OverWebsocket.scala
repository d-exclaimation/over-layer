//
//  OverWebsocket.scala
//  over-layer
//
//  Created by d-exclaimation on 5:20 PM.
//


package io.github.dexclaimation.overlayer.protocol

trait OverWebsocket {}


object OverWebsocket {
  val subscriptionsTransportWs: OverWebsocket = OverSTW

  val graphqlWs: OverWebsocket = OverGW
}