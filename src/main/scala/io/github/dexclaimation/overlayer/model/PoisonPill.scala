//
//  Constants.scala
//  over-layer
//
//  Created by d-exclaimation on 4:40 PM.
//

package io.github.dexclaimation.overlayer.model

/**
 * Poison Pill String pattern.
 */
object PoisonPill {
  val Pattern = "@Websocket.Message.PoisonPill"

  /**
   * Create a new Poison Pill using the proper pattern.
   *
   * @return A PoisonPill String Pattern
   */
  def apply(): String = Pattern
}

