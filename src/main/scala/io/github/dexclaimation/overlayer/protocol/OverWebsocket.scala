//
//  OverWebsocket.scala
//  over-layer
//
//  Created by d-exclaimation on 5:20 PM.
//


package io.github.dexclaimation.overlayer.protocol

import akka.http.scaladsl.model.ws.TextMessage
import io.github.dexclaimation.overlayer.implicits.JsValueExtensions._
import io.github.dexclaimation.overlayer.model.Subtypes.{OID, Ref}
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage.{Error, Req, Start}
import sangria.ast.OperationType
import spray.json.{JsObject, JsValue}

import scala.util.Success

/** GraphQL Over Websocket Sub Protocols Specification */
trait OverWebsocket {
  /** Sub protocol name used in Websocket handler */
  def name: String

  /** Periodical message to be sent to keep connection alive */
  def keepAlive: TextMessage.Strict

  /** Custom JSON Decoder to the proper intent */
  def decoder(json: JsValue): GraphMessage

  /** Initialization Callback */
  def init(ref: Ref): Unit

  /** Next Data type name */
  def next: String

  /** Completed Data type name */
  def complete: String

  /** Error type name */
  def error: String

  /** Decode Payload into queryAst & operationName & variables, otherwise return an error intent */
  def parse(payload: JsObject, id: OID): GraphMessage = payload
    .graphql
    .map { case (ast, op, vars) => ast
      .operationType(op)
      .map {
        case OperationType.Subscription => Start(id, ast, op, vars)
        case _ => Req(id, ast, op, vars)
      }
    }
    .map(_.map(Success.apply))
    .flatMap(_.getOrElse(noOperation))
    .unwrap { e =>
      Error(id, e.getMessage)
    }
}


object OverWebsocket {
  /** GraphQL Websocket using Apollo's `subscriptions-transport-ws` */
  val subscriptionsTransportWs: OverWebsocket = OverSTW

  /** GraphQL Websocket using The Guild's `graphql-ws` */
  val graphqlWs: OverWebsocket = OverGW
}