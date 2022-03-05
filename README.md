<p align="center">
<img src="https://github.com/d-exclaimation/over-layer/blob/main/over-layer.png" width="175" alt="logo" style="margin:1rem;"/>
</p>
<p align="center"> <h1>OverLayer</h1></p>


A GraphQL over Websocket Stream-based Transport Layer on Akka.

## Setup

**Latest Version**: `1.0.3`

```sbt
"io.github.d-exclaimation" %% "over-layer" % latestVersion
```

## GraphQL Over Websockets

In the GraphQL world, we have the concept of a "subscription" which is a request from the client to subscribe a stream
of data that is sent from the server. This is usually done using websocket with a additional subprotocol.

This package is a websocket transport layer for managing, encoding, and
decoding [Sangria](https://github.com/sangria-graphql/sangria-akka-streams) stream based operations into the proper
sub-protocol format using Akka Actors.

## Usage/Examples

- [Documentation](https://overlayer.netlify.app/)
- [Getting Started](https://overlayer.netlify.app/docs/intro)

### Protocols

- [subscriptions-transport-ws](https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md)
- [graphql-ws](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md)

### Feedback

If you have any feedback, feel free to reach out through the issues tab or through my
Twitter [@d_exclaimation](https://twitter.com/d_exclaimation).
