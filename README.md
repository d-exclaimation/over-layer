<p align="center">
<img src="./over-layer.png" width="300"/>
</p>
<p align="center"> <h1>OverLayer</h1></p>


A GraphQL over Websocket Stream-based Subscription Transport Layer on Akka.

## Setup

**Latest Version**: `0.0.1` **(Unreleased)**

```sbt
libraryDependencies += "io.github.d-exclaimation" % "over-layer" % latestVersion
```

## GraphQL Over Websockets

In the GraphQL world, we have the concept of a "subscription" which is a request from the client to subscribe a stream of data that is sent from the server. This is usually done using websocket with a additional subprotocol.

This package is a websocket transport layer for managing, encoding, and decoding [Sangria](https://github.com/sangria-graphql/sangria-akka-streams)'s stream based subscription into the proper sub-protocol format using Akka Actors.

## Usage/Examples

- [Documentation](./README.md)
### Protocols
- [subscriptions-transport-ws](https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md)
- [graphql-ws](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md)

 
