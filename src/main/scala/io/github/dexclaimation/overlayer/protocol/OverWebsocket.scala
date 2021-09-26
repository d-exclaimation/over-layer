//
//  OverWebsocket.scala
//  over-layer
//
//  Created by d-exclaimation on 5:20 PM.
//


package io.github.dexclaimation.overlayer.protocol

import io.github.dexclaimation.overlayer.model.Subtypes.Ref
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage
import io.github.dexclaimation.overlayer.protocol.common.GraphMessage.{GraphError, GraphStart}
import io.github.dexclaimation.overlayer.utils.OverGraphQL
import sangria.ast.OperationType
import sangria.parser.QueryParser
import spray.json.{JsString, JsValue}

import scala.util.{Failure, Success}

/** GraphQL Over Websocket Sub Protocols Specification */
trait OverWebsocket {
  /** Sub protocol name used in Websocket handler */
  def name: String

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
  def decodeStart(payload: Map[String, JsValue], id: String): GraphMessage = payload
    .get("query")
    .flatMap {
      case JsString(query) =>
        val op = OverGraphQL.getOperationName(payload)
        val variables = OverGraphQL.getVariables(payload)

        val res = QueryParser.parse(query) match {
          case Failure(_) => None
          case Success(ast) => ast.operation(op).map(_.operationType).flatMap {
            case OperationType.Subscription => Some(ast)
            case _ => None
          }
        }

        res.map(GraphStart(id, _, op, variables))

      case _ => None
    }
    .getOrElse(
      GraphError(id, "Invalid request query or non subscription operation")
    )
}


object OverWebsocket {
  /** GraphQL Websocket using Apollo's `subscriptions-transport-ws` */
  val subscriptionsTransportWs: OverWebsocket = OverSTW

  /** GraphQL Websocket using The Guild's `graphql-ws` */
  val graphqlWs: OverWebsocket = OverGW
}