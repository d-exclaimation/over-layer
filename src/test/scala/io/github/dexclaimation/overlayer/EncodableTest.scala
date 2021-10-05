//
//  EncodableTest.scala
//  over-layer
//
//  Created by d-exclaimation on 5:11 PM.
//

package io.github.dexclaimation.overlayer

import io.github.dexclaimation.overlayer.protocol.common.OperationMessage
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.{JsNull, JsObject, JsString}

class EncodableTest extends AnyWordSpec with Matchers {

  "OperationMessage" when {
    val CONST_TYPE = "required"
    val op1 = OperationMessage.just(CONST_TYPE)
    val op2 = OperationMessage(CONST_TYPE, "1")
    val op3 = OperationMessage(CONST_TYPE, "2", JsNull)

    ".jsObject called" should {

      "be able to return parseable JsObject" in {
        (op1 :: op2 :: op3 :: Nil)
          .map(_.jsObject)
          .foreach {
            case _: JsObject => succeed
            case _ => fail("Does not return JsObject")
          }
      }

      "always contains a `type` field of type JsString" in {
        (op1 :: op2 :: op3 :: Nil)
          .map(_.jsObject)
          .map(_.fields("type"))
          .foreach {
            case JsString(CONST_TYPE) => succeed
            case _ => fail("Missing `type` field of type JsString")
          }
      }
    }

    ".json called" should {

      "be able to return json string" in {
        (op1 :: op2 :: op3 :: Nil)
          .map(_.json)
          .foreach(json => assert(json.contains("{") && json.contains("type")))
      }
    }
  }
}
