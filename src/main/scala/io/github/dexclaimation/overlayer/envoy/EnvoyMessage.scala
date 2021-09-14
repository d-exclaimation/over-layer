//
//  EnvoyMessage.scala
//  over-layer
//
//  Created by d-exclaimation on 11:42 PM.
//

package io.github.dexclaimation.overlayer.envoy

import io.github.dexclaimation.overlayer.model.Subtypes.OID
import sangria.ast.Document
import spray.json.JsObject

sealed trait EnvoyMessage

object EnvoyMessage {
  case class Subscribe(oid: OID, ast: Document, op: Option[String], vars: JsObject) extends EnvoyMessage

  case class Unsubscribe(oid: OID) extends EnvoyMessage

  case class Ended(oid: OID) extends EnvoyMessage

  case class Ignore() extends EnvoyMessage

  case class Acid() extends EnvoyMessage
}
