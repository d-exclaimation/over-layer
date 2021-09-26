//
//  OverMessage.scala
//  over-layer
//
//  Created by d-exclaimation on 8:04 PM.
//


package io.github.dexclaimation.overlayer.protocol.common

import io.github.dexclaimation.overlayer.model.Subtypes.OID
import sangria.ast.Document
import spray.json.JsObject

/** GraphQL Intents from the incoming websocket message */
sealed trait GraphMessage

object GraphMessage {
  /** GraphQL Connection Initialization Acknowledgment (''Client has set itself up to receive and send messages'') */
  case class GraphInit() extends GraphMessage

  /** GraphQL Operation Start (''A new operation with an id is being requested'') */
  case class GraphStart(oid: OID, ast: Document, op: Option[String], vars: JsObject) extends GraphMessage

  /** GraphQL Operation Stop (''A request ending an operation using the id'') */
  case class GraphStop(oid: OID) extends GraphMessage

  /** GraphQL Operation doesn't met requirement to be understood by the server */
  case class GraphError(oid: OID, message: String) extends GraphMessage

  /** GraphQL Message doesn't met requirement to be understood by the server */
  case class GraphException(message: String) extends GraphMessage

  /** GraphQL Ping Message checking for alive connection */
  case class GraphPing() extends GraphMessage

  /** GraphQL Request to terminate the entire connection */
  case class GraphTerminate() extends GraphMessage
}
