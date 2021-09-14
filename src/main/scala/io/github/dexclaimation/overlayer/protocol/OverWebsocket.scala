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
import io.github.dexclaimation.overlayer.utils.GraphQLRequestParser
import sangria.ast.OperationType
import sangria.parser.QueryParser
import spray.json.{JsString, JsValue}

import scala.util.{Failure, Success}

trait OverWebsocket {
  def name: String

  def decoder(json: JsValue): GraphMessage

  def init(ref: Ref): Unit

  def next: String

  def complete: String

  def error: String

  def decodeStart(payload: Map[String, JsValue], id: String): GraphMessage = payload
    .get("query")
    .flatMap {
      case JsString(query) => {
        val op = GraphQLRequestParser.getOperationName(payload)
        val variables = GraphQLRequestParser.getVariables(payload)

        val res = QueryParser.parse(query) match {
          case Failure(_) => None
          case Success(ast) => ast.operation(op).map(_.operationType).flatMap {
            case OperationType.Subscription => Some(ast)
            case _ => None
          }
        }

        res.map(GraphStart(id, _, op, variables))
      }
      case _ => None
    }
    .getOrElse(
      GraphError("Cannot perform operation other than subscriptions through websocket")
    )
}


object OverWebsocket {
  val subscriptionsTransportWs: OverWebsocket = OverSTW

  val graphqlWs: OverWebsocket = OverGW
}