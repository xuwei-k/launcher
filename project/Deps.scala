import sbt._
import sbt.Keys._

object Deps {
  def lib(m: ModuleID) = libraryDependencies += m
  lazy val sbtIo = "org.scala-sbt" %% "io" % "1.10.5"
  lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.18.1"
  lazy val junit = "junit" % "junit" % "4.13.2"
  lazy val verify = "com.eed3si9n.verify" %% "verify" % "1.0.0"

  // TODO - these should be like the above, just ModuleIDs
  lazy val ivy = "org.scala-sbt.ivy" % "ivy" % "2.3.0-sbt-77cc781d727b367d3761f097d89f5a4762771d41"
  lazy val coursier = "io.get-coursier" %% "coursier" % "2.0.16"
  lazy val scalaCompiler = Def.setting("org.scala-lang" % "scala-compiler" % scalaVersion.value)
}
