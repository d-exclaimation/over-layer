//
//  GqlError.scala
//  over-layer
//
//  Created by d-exclaimation on 3:13 PM.
//

package io.github.dexclaimation.overlayer.protocol.common

import spray.json.{JsArray, JsObject, JsString}

object GqlError {
  /**
   * GraphQL Error with only Message
   *
   * @param message Error Message as String
   */
  def apply(message: String): JsObject = JsObject(
    "message" -> JsString(message)
  )

  /**
   * Multiple GraphQL Error with only messages
   *
   * @param messages Error messages
   */
  def of(messages: String*): JsArray = JsArray(
    messages.map(apply): _*
  )
}
