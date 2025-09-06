/* sbt -- Simple Build Tool
 * Copyright 2009, 2010, 2011  Mark Harrah
 */
package xsbt.boot

import java.io.File

sealed trait UpdateTarget:
  def tpe: String; def classifiers: List[String]
final class UpdateScala(val classifiers: List[String]) extends UpdateTarget:
  def tpe = "scala"
final class UpdateApp(val id: Application, val classifiers: List[String], val tpe: String)
    extends UpdateTarget

final class UpdateConfiguration(
    val bootDirectory: File,
    val ivyHome: Option[File],
    val scalaOrg: String,
    val scalaVersion: Option[String],
    val repositories: List[xsbti.Repository],
    val checksums: List[String]
):
  val resolutionCacheBase = new File(bootDirectory, "resolution-cache")
  def getScalaVersion = scalaVersion match
    case Some(sv) => sv;
    case None     => ""

final class UpdateResult(
    val success: Boolean,
    val scalaVersion: Option[String],
    val appVersion: Option[String]
)

/** Ensures that the Scala and application jars exist for the given versions or else downloads them. */
final class Update(config: UpdateConfiguration):
  import config.bootDirectory
  bootDirectory.mkdirs

  lazy val coursierUpdate = new CousierUpdate(config)

  /** The main entry point of this class for use by the Update module.  It runs Ivy */
  def apply(target: UpdateTarget, reason: String): UpdateResult =
    coursierUpdate(target, reason)
end Update
