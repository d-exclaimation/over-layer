//
//  ExceptionUtil.scala
//  over-layer
//
//  Created by d-exclaimation on 12:32 AM.
//

package io.github.dexclaimation.overlayer.utils

import scala.util.{Failure, Success, Try}

object ExceptionUtil {

  /**
   * Function Lambda to safely run a block code that may throw an error, and ignore the error
   *
   * @param fallible Fallible function.
   */
  def safe(fallible: => Unit): Unit = Try(fallible) match {
    case Failure(_) => ()
    case Success(_) => ()
  }

}
