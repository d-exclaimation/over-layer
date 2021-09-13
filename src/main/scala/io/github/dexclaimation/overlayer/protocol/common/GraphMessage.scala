//
//  OverMessage.scala
//  over-layer
//
//  Created by d-exclaimation on 8:04 PM.
//


package io.github.dexclaimation.overlayer.protocol.common

import io.github.dexclaimation.overlayer.model.Subtypes.OID
import sangria.ast.Document
import spray.json.JsObject

sealed trait GraphMessage

object GraphMessage {
  case class GraphInit() extends GraphMessage

  case class GraphStart(oid: OID, ast: Document, op: Option[String], vars: JsObject) extends GraphMessage

  case class GraphStop(oid: OID) extends GraphMessage

  case class GraphError(message: String) extends GraphMessage

  case class GraphPing() extends GraphMessage

  case class GraphTerminate() extends GraphMessage
}
