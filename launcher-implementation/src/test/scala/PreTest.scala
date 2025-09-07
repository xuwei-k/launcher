/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbt.boot

import java.io.File
import java.util.Arrays.equals as arrEquals
import org.scalacheck.*

object PreTest extends Properties("Pre"):
  import Pre.*
  property("isEmpty") = Prop.forAll((s: String) => (s.isEmpty == isEmpty(s)))
  property("isNonEmpty") = Prop.forAll((s: String) => (isEmpty(s) != isNonEmpty(s)))
  property("assert true") =
    assert(true); true
  property("assert false") = Prop.throws(classOf[AssertionError])(assert(false))
  property("assert true with message") = Prop.forAll { (s: String) =>
    assert(true, s); true
  }
  property("assert false with message") =
    Prop.forAll((s: String) => Prop.throws(classOf[AssertionError])(assert(false, s)))
  property("require false") =
    Prop.forAll((s: String) => Prop.throws(classOf[IllegalArgumentException])(require(false, s)))
  property("require true") = Prop.forAll { (s: String) =>
    require(true, s); true
  }
  property("error") = Prop.forAll((s: String) => Prop.throws(classOf[BootException])(error(s)))
  property("toBoolean") =
    Prop.forAll((s: String) => trap(toBoolean(s)) == trap(java.lang.Boolean.parseBoolean(s)))
  property("toArray") = Prop.forAll((list: List[Int]) => arrEquals(list.toArray, toArray(list)))
  property("toArray") =
    Prop.forAll((list: List[String]) => objArrEquals(list.toArray, toArray(list)))
  property("concat") = Prop.forAll(genFiles, genFiles) { (a: Array[File], b: Array[File]) =>
    (a ++ b) sameElements concat(a, b)
  }
  property("array") = Prop.forAll(genFiles) { (a: Array[File]) =>
    array(a.toList*) sameElements Array(a*)
  }
  property("substituteTilde") =
    val userHome = System.getProperty("user.home")
    assert(substituteTilde("~/path") == s"$userHome/path")
    assert(substituteTilde("~\\") == s"$userHome\\")
    assert(substituteTilde("~") == userHome)
    assert(substituteTilde("~x") == "~x")
    assert(substituteTilde("x~/") == "x~/")
    true

  given Arbitrary[File] = Arbitrary {
    for (i <- Arbitrary.arbitrary[Int]) yield new File(i.toString)
  }
  val genFiles: Gen[Array[File]] = Arbitrary.arbitrary[Array[File]]

  def trap[T](t: => T): Option[T] =
    try Some(t)
    catch case _: Exception => None

  private def objArrEquals[T <: AnyRef](a: Array[T], b: Array[T]): Boolean =
    arrEquals(a.asInstanceOf[Array[AnyRef]], b.asInstanceOf[Array[AnyRef]])
