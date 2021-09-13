//
//  ProxyActions.scala
//  over-layer
//
//  Created by d-exclaimation on 9:40 PM.
//


package io.github.dexclaimation.overlayer.proxy

import io.github.dexclaimation.overlayer.model.Subtypes.{OID, PID, Ref}
import sangria.ast.Document
import spray.json.JsObject

sealed trait ProxyActions

object ProxyActions {
  case class Connect(pid: PID, ref: Ref) extends ProxyActions

  case class Disconnect(pid: PID) extends ProxyActions

  case class StartOp(
    pid: PID, oid: OID,
    ast: Document, ctx: Any,
    op: Option[String], vars: JsObject
  ) extends ProxyActions

  case class StopOp(
    pid: PID, oid: OID
  ) extends ProxyActions
}