//
//  StreamExtensions.scala
//  over-layer
//
//  Created by d-exclaimation on 8:15 PM.
//

package io.github.dexclaimation.overlayer.implicits

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Merge, Sink, Source}

object StreamExtensions {
  /** Source Extensions */
  implicit class SourceExtensions[Out](firstSource: Source[Out, _]) {
    /**
     * Add additional output Source stream to an existing one,
     * with a single completion strategy.
     */
    def also(secondSource: Source[Out, _]): Source[Out, NotUsed] =
      Source
        .combine(firstSource, secondSource)(Merge(_, eagerComplete = true))
  }

  /** Sink Extensions */
  implicit class SinkExtensions[In, Mat](sink: Sink[In, Mat]) {

    /** Add additional transform effect before sink */
    def withBefore[In2](f: In2 => In): Sink[In2, NotUsed] =
      Flow[In2]
        .map(f)
        .to(sink)
  }
}
