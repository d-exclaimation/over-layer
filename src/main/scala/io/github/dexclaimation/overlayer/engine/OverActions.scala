//
//  ProxyActions.scala
//  over-layer
//
//  Created by d-exclaimation on 9:40 PM.
//


package io.github.dexclaimation.overlayer.engine

import io.github.dexclaimation.overlayer.model.Subtypes.{OID, PID, Ref}
import io.github.dexclaimation.overlayer.model.json.Encodable
import sangria.ast.Document
import spray.json.JsObject

/** Engine Message Action */
sealed trait OverActions

object OverActions {
  /** A websocket client Connected event action */
  case class Connect(pid: PID, ref: Ref) extends OverActions

  /** A websocket client Disconnected event action */
  case class Disconnect(pid: PID) extends OverActions

  /** A Start Subscription Operation action */
  case class StartOp(
    pid: PID, oid: OID,
    ast: Document, ctx: Any,
    op: Option[String], vars: JsObject
  ) extends OverActions


  /** A Request Stateless Operation action */
  case class StatelessOp(
    pid: PID, oid: OID,
    ast: Document, ctx: Any,
    op: Option[String], vars: JsObject
  ) extends OverActions

  /** A Stop Subscription Operation action */
  case class StopOp(
    pid: PID, oid: OID
  ) extends OverActions

  /** A Finished Stateless Operation */
  case class Outgoing(
    oid: OID,
    ref: Ref,
    en: Encodable,
  ) extends OverActions

  /** A Failed Stateless Operation */
  case class OutError(
    ref: Ref,
    en: Encodable,
  ) extends OverActions
}