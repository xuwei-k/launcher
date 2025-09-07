/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbt.boot

import org.scalacheck.*
import Prop.*

object CacheTest extends Properties("Cache"):
  property("Cache") = Prop.forAll { (_: Int, keys: List[Int], map: Int => Int) =>
    val cache = new Cache((i: Int, _: Unit) => map(i))
    def toProperty(key: Int) =
      ("Key " + key) |: ("Value: " + map(key)) |: (cache.apply(key, ()) == map(key))
    Prop.all(keys.map(toProperty)*)
  }
