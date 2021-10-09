//
//  ProtoMessage.scala
//  over-layer
//
//  Created by d-exclaimation on 9:01 AM.
//


package io.github.dexclaimation.overlayer.protocol.common

import io.github.dexclaimation.overlayer.model.Subtypes.OID
import io.github.dexclaimation.overlayer.model.json.Encodable
import spray.json.{JsObject, JsString, JsValue}

/**
 * Protocol compliant operation message type
 *
 * @param _type   Operation Type
 * @param id      Given the ID for this operation, if any
 * @param payload Given payload with the operation, if any
 */
case class OperationMessage(
  _type: String,
  id: Option[OID] = None,
  payload: Option[JsValue] = None
) extends Encodable {
  /**
   * JSON Of self
   *
   * @return JsObject
   */
  def jsObject: JsObject = (id, payload) match {
    case (Some(i), Some(p)) =>
      JsObject("type" -> JsString(_type), "id" -> JsString(i), "payload" -> p)
    case (None, Some(p)) =>
      JsObject("type" -> JsString(_type), "payload" -> p)
    case (Some(i), None) =>
      JsObject("type" -> JsString(_type), "id" -> JsString(i))
    case _ =>
      JsObject("type" -> JsString(_type))
  }
}

object OperationMessage {

  /** Protocol compliant full Operation message */
  def apply(_type: String, oid: OID, payload: JsValue) =
    new OperationMessage(_type, Some(oid), Some(payload))

  /** Protocol compliant full Operation message */
  def apply(_type: String, oid: OID, payload: Encodable) =
    new OperationMessage(_type, Some(oid), Some(payload.jsObject))

  /** Protocol compliant Operation message with no payload */
  def apply(_type: String, oid: OID) =
    new OperationMessage(_type, Some(oid), None)

  /** Protocol compliant message with just a type */
  def just(_type: String) =
    new OperationMessage(_type)
}
