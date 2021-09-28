# CHANGELOG

### v0.1.8

- Fixed keepAlive mechanism to not send initially (immediately) as was suppose to be handled by `OverWebsocket`'s `init` method.

### v0.1.7

- Fixed `subscriptions-transport-ws` keepAlive mechanism to be periodical even when message is passing from the server.
- Added logic to ignore `Ping` confirmation / `Pong` for `graphql-ws`.
- Added keepAlive finite duration to be an option for constructing `OverTransportLayer`.
- Updated websocket `Flow` to couple completion for `Source` and `Sink`.

### v0.1.6

- Fixed GraphQL Error for ending subscription's operations.
- Moved `PoisonPill` matcher to `completionMatcher` from `failureMatcher` for Websocket's `Ref` to remove unnecessary exceptions.
- Added abstractions to pass `OpMsg` to `ActorRef[String]` / Websocket's `Ref`.
- Added utilities for responding to HTTP POST Request.

### v0.1.5

- Fixed timeouts for `OverTransportLayer` flow creation.
- Added timeouts for graphql subscriptions streams. Added logic to resubscribe on completed failure and pipe back to `Envoy`.
- Removed the used of future from `SpawnProtocol` and unwrap immediately.

## v0.1.0

First working and published transport layer for akka-http and sangria-graphql over websocket using `subscriptions-transport-ws` and `graphql-ws`. Mostly code implementation are aligned and derived from [`whiskey`](https://www.github.com/d-exclaimation/whiskey) package. 

