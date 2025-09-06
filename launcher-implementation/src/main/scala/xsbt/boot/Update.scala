/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbt.boot

import java.io.File

enum UpdateTarget:
  case UpdateScala(classifiers: List[String])
  case UpdateApp(id: Application, classifiers: List[String], override val tpe: String)
  def tpe: String = this match
    case UpdateScala(_)       => "scala"
    case UpdateApp(_, _, tpe) => tpe

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
