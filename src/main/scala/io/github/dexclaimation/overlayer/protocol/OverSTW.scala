//
//  OverSTW.scala
//  over-layer
//
//  Created by d-exclaimation on 5:25 PM.
//

package io.github.dexclaimation.overlayer.protocol

import io.github.dexclaimation.overlayer.model.Subtypes.Ref
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage._
import io.github.dexclaimation.overlayer.protocol.common.{GraphMessage, ProtoMessage}
import spray.json.{JsObject, JsString, JsValue}

import scala.util.control.NonFatal

object OverSTW extends OverWebsocket {
  def name = "graphql-ws"

  private val GQL_START = "start"
  private val GQL_STOP = "stop"
  private val GQL_DATA = "data"
  private val GQL_ERROR = "error"
  private val GQL_COMPLETE = "complete"

  private val GQL_CONNECTION_TERMINATE = "connection_terminate"
  private val GQL_CONNECTION_KEEP_ALIVE = "ka"
  private val GQL_CONNECTION_ACK = "connection_ack"
  private val GQL_CONNECTION_INIT = "connection_init"

  def decoder(json: JsValue): GraphMessage = try {
    json.asJsObject.getFields("type", "payload", "id") match {
      // Start Operation
      case Seq(JsString(GQL_START), JsObject(payload), JsString(id)) => decodeStart(payload, id)

      case Seq(JsString(GQL_STOP), JsString(id)) => GraphStop(id)

      // Connection Init
      case Seq(JsString(GQL_CONNECTION_INIT), _: JsObject) => GraphInit()
      case Seq(JsString(GQL_CONNECTION_INIT)) => GraphInit()

      // Connection Termination
      case Seq(JsString(GQL_CONNECTION_TERMINATE)) => GraphTerminate()

      // Irrelevant JsObject
      case _ => GraphError("Invalid operation message type")
    }
  } catch {
    case NonFatal(_) => GraphError("Invalid operation message type")
  }

  def init(ref: Ref): Unit = {
    ref ! ProtoMessage.Empty(_type = GQL_CONNECTION_ACK).json
    ref ! ProtoMessage.Empty(_type = GQL_CONNECTION_KEEP_ALIVE).json
  }

  def next = GQL_DATA

  def complete = GQL_COMPLETE

  def error = GQL_ERROR
}
