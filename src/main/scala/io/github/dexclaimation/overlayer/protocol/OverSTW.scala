//
//  OverSTW.scala
//  over-layer
//
//  Created by d-exclaimation on 5:25 PM.
//

package io.github.dexclaimation.overlayer.protocol

import io.github.dexclaimation.overlayer.implicits.WebsocketExtensions._
import io.github.dexclaimation.overlayer.model.Subtypes.Ref
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage._
import io.github.dexclaimation.overlayer.protocol.common.{GraphMessage, OperationMessage}
import spray.json.{JsObject, JsString, JsValue}

import scala.util.control.NonFatal

/**
 * GraphQL over the `subscriptions-transport-ws` sub protocol
 *
 * {{{
 * OverTransportLayer(SchemaType, (), protocol = OverWebsocket.subscriptionTransportWs)
 * }}}
 */
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
      case Seq(JsString(GQL_START), payload: JsObject, JsString(id)) => parse(payload, id)

      case Seq(JsString(GQL_STOP), JsString(id)) => Stop(id)

      // Connection Init
      case Seq(JsString(GQL_CONNECTION_INIT), _: JsObject) => Init()
      case Seq(JsString(GQL_CONNECTION_INIT)) => Init()

      // Connection Termination
      case Seq(JsString(GQL_CONNECTION_TERMINATE)) => Terminate()

      // Irrelevant JsObject
      case _ => Exception("Invalid operation message type")
    }
  } catch {
    case NonFatal(_) => Exception("Invalid operation message type")
  }

  def init(ref: Ref): Unit = {
    ref <~ OperationMessage.just(_type = GQL_CONNECTION_ACK)
    ref <~ OperationMessage.just(_type = GQL_CONNECTION_KEEP_ALIVE)
  }

  def next = GQL_DATA

  def complete = GQL_COMPLETE

  def error = GQL_ERROR

  def keepAlive = OperationMessage.just(GQL_CONNECTION_KEEP_ALIVE).textMessage
}
