//
//  Encodable.scala
//  over-layer
//
//  Created by d-exclaimation on 3:37 AM.
//


package io.github.dexclaimation.overlayer.model.json

import spray.json.JsObject

/** Json-Encodable Data Structure */
trait Encodable {
  def jsObject: JsObject

  def json: String = jsObject.compactPrint
}
