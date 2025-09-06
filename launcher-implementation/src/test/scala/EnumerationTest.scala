package xsbt.boot

import org.scalacheck.*
import Prop.{ Exception as _, * }

object EnumerationTest extends Properties("Enumeration"):
  property("MultiEnum.toValue") = checkToValue(MultiEnum, multiElements*)
  property("MultiEnum.elements") = checkElements(MultiEnum, multiElements*)
  property("EmptyEnum.toValue") = checkToValue(EmptyEnum)
  property("EmptyEnum.elements") = EmptyEnum.elements.isEmpty
  property("SingleEnum.toValue") = checkToValue(SingleEnum, singleElements)
  property("SingleEnum.elements") = checkElements(SingleEnum, singleElements)

  def singleElements = ("A", SingleEnum.a)
  def multiElements =
    import MultiEnum.{ a, b, c }
    List(("A" -> a), ("B" -> b), ("C" -> c))

  def checkElements(`enum`: Enumeration, mapped: (String, Enumeration#Value)*) =
    val elements = `enum`.elements
    ("elements: " + elements) |:
      (mapped.forall { case (_, v) => elements.contains(v) } && (elements.length == mapped.length))

  def checkToValue(`enum`: Enumeration, mapped: (String, Enumeration#Value)*) =
    def invalid(s: String) =
      ("valueOf(" + s + ")") |:
        Prop.throws(classOf[Exception])(`enum`.toValue(s))
    def valid(s: String, expected: Enumeration#Value) =
      ("valueOf(" + s + ")") |:
        ("Expected " + expected) |:
        (`enum`.toValue(s) == expected)
    val map = Map(mapped*)
    Prop.forAll((s: String) =>
      map.get(s) match
        case Some(v) => valid(s, v)
        case None    => invalid(s)
    )
  object MultiEnum extends Enumeration:
    val a = value("A")
    val b = value("B")
    val c = value("C")
  object SingleEnum extends Enumeration:
    val a = value("A")
  object EmptyEnum extends Enumeration
