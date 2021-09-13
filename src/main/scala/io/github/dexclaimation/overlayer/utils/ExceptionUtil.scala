//
//  ExceptionUtil.scala
//  over-layer
//
//  Created by d-exclaimation on 12:32 AM.
//

package io.github.dexclaimation.overlayer.utils

import scala.util.{Failure, Success, Try}

object ExceptionUtil {
  def tolerate(fallible: => Unit): Unit = Try(fallible) match {
    case Failure(_) => ()
    case Success(_) => ()
  }
}
