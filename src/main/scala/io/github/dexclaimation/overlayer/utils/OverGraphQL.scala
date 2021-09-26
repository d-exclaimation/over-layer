//
//  GraphQLRequestParser.scala
//  over-layer
//
//  Created by d-exclaimation on 9:32 AM.
//

package io.github.dexclaimation.overlayer.utils

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import sangria.execution._
import sangria.execution.deferred.DeferredResolver
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import sangria.schema.Schema
import sangria.validation.QueryValidator
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object OverGraphQL {
  /** Get Variables from the request as Object */
  def getVariables(fields: Map[String, JsValue]): JsObject = fields.get("variables") match {
    case Some(o: JsObject) => o
    case _ => JsObject.empty
  }

  /** Get Operation Name from request */
  def getOperationName(fields: Map[String, JsValue]): Option[String] = fields
    .get("operationName")
    .collect {
      case JsString(value) => value
    }

  /**
   * GraphQL Execution Handler for `akka-http` and `spray-json`.
   *
   * *''mostly exception-less'' in that sangria executor exceptions and failure in query are already handled,
   * other possible exceptions are not.
   *
   * @param schema             Sangria GraphQL Schema.
   * @param ctx                Context for the schema.
   * @param root               Root value object for the schema.
   * @param queryValidator     Executor queryValidator.
   * @param deferredResolver   Any deferred resolver used by the executor.
   * @param exceptionHandler   Query Exception Handlers.
   * @param deprecationTracker Deprecation Trackers used by the executor.
   * @param middleware         Resolver middleware.
   * @param maxQueryDepth      Limit of the query depth can be resolved.
   * @param queryReducers      Query reducers for resolvers.
   * @return A ''*mostly exception-less'' Future of Status Code with a response JsValue.
   */
  def handle[Ctx, Val: ClassTag](
    js: JsValue,
    schema: Schema[Ctx, Val],
    ctx: Ctx,
    root: Val,
    queryValidator: QueryValidator = QueryValidator.default,
    deferredResolver: DeferredResolver[Ctx] = DeferredResolver.empty,
    exceptionHandler: ExceptionHandler = ExceptionHandler.empty,
    deprecationTracker: DeprecationTracker = DeprecationTracker.empty,
    middleware: List[Middleware[Ctx]] = Nil,
    maxQueryDepth: Option[Int] = None,
    queryReducers: List[QueryReducer[Ctx, _]] = Nil
  )(implicit ex: ExecutionContext): Future[(StatusCode, JsValue)] = {
    val JsObject(fields) = js
    val JsString(query) = fields.getOrElse("query", "")
    val operation = OverGraphQL.getOperationName(fields)
    val vars = OverGraphQL.getVariables(fields)

    QueryParser.parse(query) match {
      case Failure(error) =>
        Future.successful(BadRequest -> JsObject("error" -> JsString(error.getMessage)))

      case Success(queryAst) => Executor
        .execute(
          schema = schema,
          queryAst = queryAst,
          userContext = ctx,
          root = root,
          operationName = operation,
          variables = vars,
          queryValidator = queryValidator,
          deferredResolver = deferredResolver,
          exceptionHandler = exceptionHandler,
          deprecationTracker = deprecationTracker,
          middleware = middleware,
          maxQueryDepth = maxQueryDepth,
          queryReducers = queryReducers,
        )
        .map(OK -> _)
        .recover {
          case error: QueryAnalysisError => BadRequest -> error.resolveError
          case error: ErrorWithResolver => InternalServerError -> error.resolveError
        }
    }
  }
}
