# CHANGELOG

### v1.0.3

- Updated to latest major versions of all dependencies

### v1.0.2

- Updated to latest version of all dependencies

### v1.0.1

- Fixed operation message type for resolver error in `Query/Mutation` operations.
- Updated dependencies and scala version.

## v1.0.0

Websocket transport feature complete with unit tests and major bug fixes, published with scala compiler version.

### v0.2.3

- Fixed issue with invalid `type` on incoming messages to terminate the connection.
- Removed deprecated, unused code.
- Made `GqlError` into proper case class.
- Renamed `Route` middleware directives to `applyMiddleware` from `ws`.

### v0.2.2

- Fixed issue with sub-protocol error message to be user understandable.
- Deprecated functions that are being replaced, unused, or that have no relation to the package.
- Replaced old query parser `decodeStart` that use `Option` with `parse` that use `Try` to allow error message to be
  preserved.
- Added utilities for `SpawnProtocol` and `AskPattern`.

### v0.2.1

- Fixed issue with error before and after execution, where before goes as `GQL_ERROR` / `Error`, but after goes
  as `GQL_DATA` / `Next`.

## v0.2

- Renamed `Proxy` to `OverEngine`.
- Added multiple new message types for error and output handling for `Envoy` and `OverEngine`.
- Added throughout `recover` for error handling for both future and stream results.
- Updated `OperationMessage`, Added `GqlError` for proper error formats.
- Added capabilities to respond to non-streaming operation using `GraphImmediate` and `StatelessOp`.
- Updated `OverEngine` to be able to execute operation and pipe result back to self with proper error handling.
- Kill / Terminate connection on `GraphException`.
- Moved `onInit` to wait after acknowledgement.
- Updated `timeoutDuration` to allow `Duration.Inf` which will remove timeout.

### v0.1.8

- Fixed keepAlive mechanism to not send initially (immediately) as was suppose to be handled by `OverWebsocket`'s `init`
  method.

### v0.1.7

- Fixed `subscriptions-transport-ws` keepAlive mechanism to be periodical even when message is passing from the server.
- Added logic to ignore `Ping` confirmation / `Pong` for `graphql-ws`.
- Added keepAlive finite duration to be an option for constructing `OverTransportLayer`.
- Updated websocket `Flow` to couple completion for `Source` and `Sink`.

### v0.1.6

- Fixed GraphQL Error for ending subscription's operations.
- Moved `PoisonPill` matcher to `completionMatcher` from `failureMatcher` for Websocket's `Ref` to remove unnecessary
  exceptions.
- Added abstractions to pass `OpMsg` to `ActorRef[String]` / Websocket's `Ref`.
- Added utilities for responding to HTTP POST Request.

### v0.1.5

- Fixed timeouts for `OverTransportLayer` flow creation.
- Added timeouts for graphql subscriptions streams. Added logic to resubscribe on completed failure and pipe back
  to `Envoy`.
- Removed the used of future from `SpawnProtocol` and unwrap immediately.

## v0.1.0

First working and published transport layer for akka-http and sangria-graphql over websocket
using `subscriptions-transport-ws` and `graphql-ws`. Mostly code implementation are aligned and derived
from [`whiskey`](https://www.github.com/d-exclaimation/whiskey) package. 

