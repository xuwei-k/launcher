/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbt.boot

import Pre.*
import scala.collection.immutable.List

class Enumeration extends Serializable:
  def elements: List[Value] = members
  private lazy val members: List[Value] =
    val c = getClass
    val correspondingFields = ListMap(c.getDeclaredFields.map(f => (f.getName, f))*)
    c.getMethods.toList flatMap { method =>
      if method.getParameterTypes.length == 0 && classOf[Value].isAssignableFrom(
          method.getReturnType
        )
      then
        for (
          field <- correspondingFields.get(method.getName)
          if field.getType == method.getReturnType
        ) yield method.invoke(this).asInstanceOf[Value]
      else Nil
    }
  def value(s: String) = new Value(s, 0)
  def value(s: String, i: Int) = new Value(s, i)
  final class Value(override val toString: String, val id: Int) extends Serializable
  def toValue(s: String): Value =
    elements
      .find(_.toString == s)
      .getOrElse(error("expected one of " + elements.mkString(",") + " (got: " + s + ")"))
