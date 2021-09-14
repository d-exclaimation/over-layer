//
//  ProtoMessage.scala
//  over-layer
//
//  Created by d-exclaimation on 9:01 AM.
//


package io.github.dexclaimation.overlayer.protocol.common

import io.github.dexclaimation.overlayer.model.Subtypes.OID
import spray.json.{JsObject, JsString, JsValue}

case class ProtoMessage(
  _type: String,
  id: Option[OID] = None,
  payload: Option[JsValue] = None
) {
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

  /**
   * Json String of self
   *
   * @return String
   */
  def json: String = jsObject.compactPrint
}

object ProtoMessage {
  def Operation(_type: String, oid: OID, payload: JsValue) =
    ProtoMessage(_type, Some(oid), Some(payload))

  def NoPayload(_type: String, oid: OID) =
    ProtoMessage(_type, Some(oid), None)

  def NoID(_type: String, payload: JsValue) =
    ProtoMessage(_type, payload = Some(payload))

  def Empty(_type: String) =
    ProtoMessage(_type)
}
