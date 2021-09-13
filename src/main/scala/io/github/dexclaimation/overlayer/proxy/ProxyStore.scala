//
//  ProxyStore.scala
//  over-layer
//
//  Created by d-exclaimation on 9:20 PM.
//

package io.github.dexclaimation.overlayer.proxy

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import io.github.dexclaimation.overlayer.model.Subtypes._
import io.github.dexclaimation.overlayer.proxy.ProxyActions._

import scala.collection.mutable

class ProxyStore[Ctx, Val](
  override val context: ActorContext[ProxyActions]
) extends AbstractBehavior(context) {

  private val refs: mutable.Map[PID, Ref] = mutable.Map.empty[PID, Ref]

  def onMessage(msg: ProxyActions): Behavior[ProxyActions] = receive(msg) {
    case Connect(pid, ref) => refs.update(pid, ref)
    case Disconnect(pid) => refs.remove(pid)
    case StartOp(pid, oid, ast, ctx, op, vars) =>
    case StopOp(pid, oid) =>
  }

  private def receive(msg: ProxyActions)(handler: ProxyActions => Unit): Behavior[ProxyActions] = {
    handler(msg)
    this
  }
}

object ProxyStore {
  def behavior[Ctx, Val]: Behavior[ProxyActions] =
    Behaviors.setup(new ProxyStore[Ctx, Val](_))
}