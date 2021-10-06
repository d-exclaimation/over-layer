//
//  JsValueExtensions.scala
//  over-layer
//
//  Created by d-exclaimation on 4:14 PM.
//

package io.github.dexclaimation.overlayer.implicits

import sangria.ast
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString}

import scala.util.{Failure, Try}

/** JsValue Extensions */
object JsValueExtensions {

  def noOperation[T]: Failure[T] = Failure(
    new Error("Cannot conclude the GraphQL operation from the 'query' and 'operationName'")
  )

  /** GraphQL Based JsObject Parsing */
  implicit final class GqlJsObject(js: JsObject) {

    /** Operation Name from a GraphQL Request */
    def operationName: Option[String] = js.fields
      .get("operationName")
      .collect { case JsString(value) => value }

    /** Variables from a GraphQL Request */
    def variables: JsObject = js.fields
      .get("variables")
      .collect { case obj: JsObject => obj }
      .getOrElse(JsObject.empty)

    /** Query string from a GraphQL Request */
    def query: Try[String] = Try(js.fields("query"))
      .collect {
        case JsString(query) => query
      }

    /** Query AST from a GraphQL Request */
    def queryAst: Try[ast.Document] = query
      .flatMap(QueryParser.parse(_))


    /** GraphQL Request from a GraphQL Request */
    def graphql: Try[(ast.Document, Option[String], JsObject)] = queryAst
      .map((_, operationName, variables))
  }

  /** Try Extensions */
  implicit final class TryExtend[U](t: Try[U]) {

    /** Unwrap the success value of return the fallback */
    def unwrap(fallback: Throwable => U): U = t
      .fold(fallback, identity)

  }

}
