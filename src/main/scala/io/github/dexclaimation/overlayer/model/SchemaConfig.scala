//
//  SchemaConfig.scala
//  over-layer
//
//  Created by d-exclaimation on 8:33 AM.
//


package io.github.dexclaimation.overlayer.model

import sangria.execution.deferred.DeferredResolver
import sangria.execution.{DeprecationTracker, ExceptionHandler, Middleware, QueryReducer}
import sangria.schema.Schema
import sangria.validation.QueryValidator

case class SchemaConfig[Ctx, Val](
  schema: Schema[Ctx, Val],
  root: Val,
  queryValidator: QueryValidator = QueryValidator.default,
  deferredResolver: DeferredResolver[Ctx] = DeferredResolver.empty,
  exceptionHandler: ExceptionHandler = ExceptionHandler.empty,
  deprecationTracker: DeprecationTracker = DeprecationTracker.empty,
  middleware: List[Middleware[Ctx]] = Nil,
  maxQueryDepth: Option[Int] = None,
  queryReducers: List[QueryReducer[Ctx, _]] = Nil
)
