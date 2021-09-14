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

/** Proxy Message Action */
sealed trait ProxyActions

object ProxyActions {
  /** A websocket client Connected event action */
  case class Connect(pid: PID, ref: Ref) extends ProxyActions

  /** A websocket client Disconnected event action */
  case class Disconnect(pid: PID) extends ProxyActions

  /** A Start Subscription Operation action */
  case class StartOp(
    pid: PID, oid: OID,
    ast: Document, ctx: Any,
    op: Option[String], vars: JsObject
  ) extends ProxyActions

  /** A Stop Subscription Operation action */
  case class StopOp(
    pid: PID, oid: OID
  ) extends ProxyActions
}