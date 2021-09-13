//
//  LayerSubtypes.scala
//  over-layer
//
//  Created by d-exclaimation on 4:33 PM.
//

package io.github.dexclaimation.overlayer.model

import java.util.UUID

object Subtypes {
  /** Websocket Client Process ID */
  type PID = String

  /** Websocket Client Process ID */
  def PID(): PID = UUID.randomUUID().toString

  /** GraphQL Operation ID */
  type OID = String

  /** Compound ID from PID & OID */
  type CID = String
}
