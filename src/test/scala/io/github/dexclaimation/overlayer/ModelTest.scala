//
//  ModelTest.scala
//  over-layer
//
//  Created by d-exclaimation on 4:47 PM.
//

package io.github.dexclaimation.overlayer

import io.github.dexclaimation.overlayer.model.PoisonPill
import io.github.dexclaimation.overlayer.model.Subtypes.PID
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ModelTest extends AnyWordSpec with Matchers {

  "Custom string-based models" that {

    "is PoisonPill" should {

      "return the proper pattern when applied" in {
        PoisonPill() match {
          case pill: String => pill shouldEqual "@Websocket.Message.PoisonPill"
          case _ => fail("Pattern does not return string")
        }
      }

      "be able to match in partial function" in {
        val completionMatcher: PartialFunction[String, Unit] = PoisonPill.partialFunction

        completionMatcher(PoisonPill())
      }
    }

    "is PID" should {

      "return a random unique ID for each websocket process" in {
        val process1 = PID()
        val process2 = PID()
        val process3 = PID()

        process1 shouldNot equal(process2)
        process2 shouldNot equal(process3)
        process3 shouldNot equal(process1)
      }
    }
  }
}
