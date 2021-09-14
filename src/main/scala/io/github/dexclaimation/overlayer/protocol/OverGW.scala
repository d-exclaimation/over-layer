//
//  OverGW.scala
//  over-layer
//
//  Created by d-exclaimation on 5:27 PM.
//

package io.github.dexclaimation.overlayer.protocol

import io.github.dexclaimation.overlayer.model.Subtypes.Ref
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage._
import io.github.dexclaimation.overlayer.protocol.common.{GraphMessage, ProtoMessage}
import spray.json.{JsObject, JsString, JsValue}

import scala.util.control.NonFatal

object OverGW extends OverWebsocket {
  def name = "graphql-transport-ws"

  private val Subscribe = "subscribe"
  private val Next = "next"
  private val Error = "error"
  private val Complete = "complete"

  private val Ping = "ping"
  private val ConnectionAck = "connection_ack"
  private val ConnectionInit = "connection_init"

  def decoder(json: JsValue): GraphMessage = try {
    json.asJsObject.getFields("type", "payload", "id") match {

      case Seq(JsString(Subscribe), JsObject(p), JsString(id)) => decodeStart(p, id)

      // Complete operation
      case Seq(JsString(Complete), JsString(id)) => GraphStop(id)

      // Initial connection
      case Seq(JsString(ConnectionInit), _: JsObject) => GraphInit()
      case Seq(JsString(ConnectionInit)) => GraphInit()

      // Ping operation
      case Seq(JsString(Ping), _: JsObject) => GraphPing()
      case Seq(JsString(Ping)) => GraphPing()

      // Irrelevant JsObject
      case _ => GraphError(
        "No \"type\" field that properly matches the protocol spec or object schema does not conform to spec"
      )
    }
  } catch {
    case NonFatal(_) => GraphError(
      "No \"type\" field that properly matches the protocol spec or object schema does not conform to spec"
    )
  }

  def init(ref: Ref) = ref ! ProtoMessage.Empty(_type = ConnectionAck).json

  def next = Next

  def complete = Complete

  def error = Error
}
