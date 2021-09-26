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
case class OpMsg(
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

object OpMsg {

  /** Protocol compliant full Operation message */
  def apply(_type: String, oid: OID, payload: JsValue) =
    OpMsg(_type, Some(oid), Some(payload))

  /** Protocol compliant Operation message with no payload */
  def NoPayload(_type: String, oid: OID) =
    OpMsg(_type, Some(oid), None)

  /** Protocol compliant Operation message with no id */
  def NoID(_type: String, payload: JsValue) =
    OpMsg(_type, payload = Some(payload))

  /** Protocol compliant message with just a type */
  def Empty(_type: String) =
    OpMsg(_type)
}
