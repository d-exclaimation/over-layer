//
//  StreamExtensions.scala
//  over-layer
//
//  Created by d-exclaimation on 8:15 PM.
//

package io.github.dexclaimation.overlayer.implicits

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Merge, Sink, Source}

import scala.concurrent.duration.{Duration, FiniteDuration}

object StreamExtensions {
  /** Source Extensions */
  implicit final class SourceExtensions[Out, Mat](firstSource: Source[Out, Mat]) {
    /**
     * Add additional output Source stream to an existing one,
     * with a single completion strategy.
     */
    def also(secondSource: Source[Out, _]): Source[Out, NotUsed] =
      Source
        .combine(firstSource, secondSource)(Merge(_, eagerComplete = true))

    def idleIfFinite(duration: Duration): Source[Out, Mat] = duration match {
      case _: Duration.Infinite => firstSource
      case finite: FiniteDuration => firstSource.idleTimeout(finite)
    }
  }

  /** Sink Extensions */
  implicit final class SinkExtensions[In, Mat](sink: Sink[In, Mat]) {

    /** Add additional transform effect before sink */
    def withBefore[In2](f: In2 => In): Sink[In2, NotUsed] =
      Flow[In2]
        .map(f)
        .to(sink)
  }
}
