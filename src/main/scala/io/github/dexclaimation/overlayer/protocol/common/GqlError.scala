//
//  GqlError.scala
//  over-layer
//
//  Created by d-exclaimation on 3:13 PM.
//

package io.github.dexclaimation.overlayer.protocol.common

import io.github.dexclaimation.overlayer.model.json.Encodable
import spray.json.{JsArray, JsObject, JsString, JsValue}

/**
 * GraphQL Error with only Message
 *
 * @param message Error Message as String
 */
case class GqlError(message: String) extends Encodable {
  def jsObject: JsObject = JsObject(
    "message" -> JsString(message)
  )
}

object GqlError {

  /**
   * Multiple GraphQL Error with only messages
   *
   * @param messages Error messages
   */
  def of(messages: String*): JsArray = {
    val jsValues: Seq[JsValue] = messages
      .map(apply)
      .map(_.jsObject)
    
    JsArray(jsValues: _*)
  }
}
