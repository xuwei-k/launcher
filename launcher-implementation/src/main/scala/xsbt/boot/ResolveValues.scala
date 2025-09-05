/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbt.boot

import Pre.*

object ResolveValues:
  def apply(conf: LaunchConfiguration): LaunchConfiguration = (new ResolveValues(conf))()
  private def trim(s: String): Option[String] = if s eq null then None else notEmpty(s.trim)
  private def notEmpty(s: String): Option[String] = if isEmpty(s) then None else Some(s)

import ResolveValues.trim
final class ResolveValues(conf: LaunchConfiguration):
  private def propertiesFile = conf.boot.properties
  private lazy val properties = readProperties(propertiesFile)
  def apply(): LaunchConfiguration =
    import conf.*
    val scalaVersion = resolve(conf.scalaVersion)
    val appVersion = resolve(app.version)
    val appName = resolve(app.name)
    val classifiers = resolveClassifiers(ivyConfiguration.classifiers)
    withNameAndVersions(scalaVersion, appVersion, appName, classifiers)
  def resolveClassifiers(classifiers: Classifiers): Classifiers =
    import ConfigurationParser.readIDs
    // the added "" ensures that the main jars are retrieved
    val scalaClassifiers = "" :: resolve(classifiers.forScala)
    val appClassifiers = "" :: resolve(classifiers.app)
    Classifiers(new Explicit(scalaClassifiers), new Explicit(appClassifiers))
  def resolve[T](v: Value[T])(using read: String => T): T =
    v match
      case e: Explicit[?] => e.value
      case i: Implicit[?] =>
        trim(properties.getProperty(i.name))
          .map(read)
          .orElse(i.default)
          .getOrElse(sys.error("no " + i.name + " specified in " + propertiesFile))
