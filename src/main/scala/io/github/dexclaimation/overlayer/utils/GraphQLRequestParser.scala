//
//  GraphQLRequestParser.scala
//  over-layer
//
//  Created by d-exclaimation on 9:32 AM.
//

package io.github.dexclaimation.overlayer.utils

import spray.json.{JsObject, JsString, JsValue}

object GraphQLRequestParser {
  /** Get Variables from the request as Object */
  def getVariables(fields: Map[String, JsValue]): JsObject = fields.get("variables") match {
    case Some(o: JsObject) => o
    case _ => JsObject.empty
  }

  /** Get Operation Name from request */
  def getOperationName(fields: Map[String, JsValue]): Option[String] = fields
    .get("operationName")
    .collect {
      case JsString(value) => value
    }

}
