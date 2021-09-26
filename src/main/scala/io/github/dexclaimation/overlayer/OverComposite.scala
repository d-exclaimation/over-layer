//
//  OverComposite.scala
//  over-layer
//
//  Created by d-exclaimation on 8:28 PM.
//


package io.github.dexclaimation.overlayer

import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage
import spray.json.{DefaultJsonProtocol, RootJsonReader}

/**
 * Over Composite Material for the json decoder.
 */
trait OverComposite extends DefaultJsonProtocol {
  def protocol: OverWebsocket

  implicit val graphDecoder: RootJsonReader[GraphMessage] = protocol.decoder
}
