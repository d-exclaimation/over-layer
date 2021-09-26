//
//  EnvoyMessage.scala
//  over-layer
//
//  Created by d-exclaimation on 11:42 PM.
//

package io.github.dexclaimation.overlayer.envoy

import io.github.dexclaimation.overlayer.model.Subtypes.OID
import io.github.dexclaimation.overlayer.protocol.common.OpMsg
import sangria.ast.Document
import spray.json.JsObject

/** Envoy Message Intent */
sealed trait EnvoyMessage

object EnvoyMessage {
  /** Envoy Intent to start a Subscription Stream */
  case class Subscribe(oid: OID, ast: Document, op: Option[String], vars: JsObject) extends EnvoyMessage

  /** Envoy Intent to kill a Subscription Stream using a Kill Switch */
  case class Unsubscribe(oid: OID) extends EnvoyMessage

  /** Envoy ''self-used'' Intent to pipe finished Stream's Future back into the mailbox */
  case class Ended(oid: OID) extends EnvoyMessage

  /** Envoy Outgoing data concurrent safe handler by sending back to the mailbox */
  case class Output(oid: OID, data: OpMsg) extends EnvoyMessage

  /** Envoy Kill / PoisonPill Intent */
  case class Acid() extends EnvoyMessage
}
