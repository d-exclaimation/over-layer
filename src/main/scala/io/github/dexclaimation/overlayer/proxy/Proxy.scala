//
//  ProxyStore.scala
//  over-layer
//
//  Created by d-exclaimation on 9:20 PM.
//

package io.github.dexclaimation.overlayer.proxy

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import io.github.dexclaimation.overlayer.envoy.EnvoyMessage.{Acid, Subscribe, Unsubscribe}
import io.github.dexclaimation.overlayer.envoy.{Envoy, EnvoyMessage}
import io.github.dexclaimation.overlayer.model.SchemaConfig
import io.github.dexclaimation.overlayer.model.Subtypes._
import io.github.dexclaimation.overlayer.protocol.OverWebsocket
import io.github.dexclaimation.overlayer.proxy.ProxyActions._
import io.github.dexclaimation.overlayer.utils.ExceptionUtil.tolerate

import scala.collection.mutable

/**
 * A Proxy Actor managing distribution of operations and top-level state
 *
 * @param protocol The GraphQL Subscription Sub Protocol used.
 * @param config   The Schema Configuration.
 * @param context  The Actor Context for the Abstract Behavior.
 */
class Proxy[Ctx, Val](
  val protocol: OverWebsocket,
  val config: SchemaConfig[Ctx, Val],
  override val context: ActorContext[ProxyActions]
) extends AbstractBehavior(context) {

  /** Client State mapped to the PID */
  private val refs: mutable.Map[PID, Ref] = mutable.Map.empty[PID, Ref]

  /** Envoy State mapped to the PID */
  private val envoys: mutable.Map[PID, ActorRef[EnvoyMessage]] = mutable.Map.empty[PID, ActorRef[EnvoyMessage]]

  /**
   * Implement this method to process an incoming message and return the next behavior.
   *
   * The returned behavior can in addition to normal behaviors be one of the canned special objects:
   *
   *  - returning `stopped` will terminate this Behavior.
   *  - returning `this` or `same` designates to reuse the current Behavior.
   *  - returning `unhandled` keeps the same Behavior and signals that the message was not yet handled.
   *
   */
  def onMessage(msg: ProxyActions): Behavior[ProxyActions] = receive(msg) {
    case Connect(pid, ref) => refs.update(pid, ref)
    case Disconnect(pid) => tolerate {
      refs.remove(pid)
      envoys.get(pid).foreach(_ ! Acid())
      envoys.remove(pid)
    }
    case StartOp(pid, oid, ast, ctx, op, vars) => refs.get(pid).foreach { ref =>
      val envoy = envoys.getOrElse(pid,
        context.spawn(
          behavior = Behaviors.setup(new Envoy[Ctx, Val](pid, ref, ctx, config, protocol, _)),
          name = pid,
        )
      )
      envoy ! Subscribe(oid, ast, op, vars)

      envoys.update(pid, envoy)
    }

    case StopOp(pid, oid) => envoys
      .get(pid)
      .foreach(_ ! Unsubscribe(oid))
  }

  private def receive(msg: ProxyActions)(handler: ProxyActions => Unit): Behavior[ProxyActions] = {
    handler(msg)
    this
  }
}

object Proxy {

  /**
   * Setup a full Actor behavior using the ProxyStore
   */
  def behavior[Ctx, Val](
    protocol: OverWebsocket,
    config: SchemaConfig[Ctx, Val]
  ): Behavior[ProxyActions] = {
    Behaviors.setup(new Proxy[Ctx, Val](protocol, config, _))
  }
}