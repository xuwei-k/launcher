package xsbt.boot

import java.util.{ Set as julSet, Map as julMap }
import java.io.IOException
import java.io.File
import java.util.Arrays

import org.apache.ivy.core.IvyContext
import org.apache.ivy.core.event.EventManager
import org.apache.ivy.core.report.*
import org.apache.ivy.core.retrieve.*
import org.apache.ivy.core.event.retrieve.*
import org.apache.ivy.core.IvyPatternHelper
import org.apache.ivy.core.LogOptions
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.util.Message
import org.apache.ivy.util.FileUtil

import scala.collection.mutable.Set as mSet
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

private[xsbt] case class RetResult(
    destFile: File,
    artifact: ArtifactDownloadReport,
    totalSizeDownloaded: Long,
    copied: Boolean
)

object ParallelRetrieveEngine {
  final val KILO = 1024

  private val retrieveExecutionContext = ParallelExecution.executionContext
}

/** Define an ivy [[RetrieveEngine]] that retrieves dependencies in parallel. */
private[xsbt] class ParallelRetrieveEngine(
    settings: RetrieveEngineSettings,
    eventManager: EventManager
) extends RetrieveEngine(settings, eventManager) {

  // a port to parallel retrieve from https://github.com/apache/ant-ivy/blob/2.3.0/src/java/org/apache/ivy/core/retrieve/RetrieveEngine.java#L83
  override def retrieve(mrid: ModuleRevisionId, options: RetrieveOptions): RetrieveReport = {
    val report = new RetrieveReport()

    val moduleId = mrid.getModuleId()
    if LogOptions.LOG_DEFAULT.equals(options.getLog()) then {
      Message.info(":: retrieving :: " + moduleId)
    } else {
      Message.verbose(":: retrieving :: " + moduleId)
    }
    Message.verbose("\tcheckUpToDate=" + settings.isCheckUpToDate())
    val start = System.currentTimeMillis()

    val destFilePattern = IvyPatternHelper.substituteVariables(
      options.getDestArtifactPattern(),
      settings.getVariables()
    )
    val destIvyPattern =
      IvyPatternHelper.substituteVariables(options.getDestIvyPattern(), settings.getVariables())

    val confs = getConfs(mrid, options)
    if LogOptions.LOG_DEFAULT.equals(options.getLog()) then {
      Message.info("\tconfs: " + Arrays.asList(confs*))
    } else {
      Message.verbose("\tconfs: " + Arrays.asList(confs*))
    }
    if eventManager != null then {
      eventManager.fireIvyEvent(new StartRetrieveEvent(mrid, confs, options))
    }

    try {
      val artifactsToCopy = determineArtifactsToCopy(mrid, destFilePattern, options)
      val fileRetrieveRoot = settings.resolveFile(IvyPatternHelper.getTokenRoot(destFilePattern))

      report.setRetrieveRoot(fileRetrieveRoot)

      val _ =
        if destIvyPattern == null then null
        else settings.resolveFile(IvyPatternHelper.getTokenRoot(destIvyPattern))

      implicit val ec = ParallelRetrieveEngine.retrieveExecutionContext
      import scala.jdk.CollectionConverters.*
      type ValueType = julSet[String]
      val allRetrivedFuture = artifactsToCopy.entrySet().asScala.map {
        case artifactAndPaths: julMap.Entry[ArtifactDownloadReport, ValueType] @unchecked =>
          val artifact: ArtifactDownloadReport = artifactAndPaths.getKey()
          val archive: File = artifact.getLocalFile()

          if archive == null then {
            Message.verbose("\tno local file available for " + artifact + ": skipping")
            Future { mSet[RetResult]() }
          } else {
            Message.verbose("\tretrieving " + archive)

            Future.traverse(artifactAndPaths.getValue().asScala) { case path: String =>
              Future {
                IvyContext.getContext().checkInterrupted()
                val _ = settings.resolveFile(path)
                retrieveFile(
                  settings,
                  eventManager,
                  artifact,
                  archive,
                  path,
                  options
                )
              }
            }
          }
      }

      val allRetrived: mSet[RetResult] =
        Await.result(Future.reduceLeft(allRetrivedFuture.toList)(_ ++ _), Duration.Inf)

      val totalCopiedSize = allRetrived.foldLeft(0L) { case (sum, ret) =>
        if ret.copied then report.addCopiedFile(ret.destFile, ret.artifact)
        else report.addUpToDateFile(ret.destFile, ret.artifact)

        sum + ret.totalSizeDownloaded
      }

      val elapsedTime = System.currentTimeMillis() - start

      val msg2 =
        if settings.isCheckUpToDate() then
          (", " + report.getNbrArtifactsUpToDate() + " already retrieved")
        else
          ("" + " (" + (totalCopiedSize / ParallelRetrieveEngine.KILO) + "kB/" + elapsedTime + "ms)"
        )

      val msg = "\t" + report.getNbrArtifactsCopied() + " artifacts copied" + msg2

      if LogOptions.LOG_DEFAULT.equals(options.getLog()) then {
        Message.info(msg)
      } else {
        Message.verbose(msg)
      }
      Message.verbose("\tretrieve done (" + (elapsedTime) + "ms)")
      if this.eventManager != null then {
        this.eventManager.fireIvyEvent(
          new EndRetrieveEvent(
            mrid,
            confs,
            elapsedTime,
            report.getNbrArtifactsCopied(),
            report.getNbrArtifactsUpToDate(),
            totalCopiedSize,
            options
          )
        )
      }

      return report
    } catch {
      case ex: Exception =>
        throw new RuntimeException("problem during retrieve of " + moduleId + ": " + ex, ex)
    }
  }

  def retrieveFile(
      settings: RetrieveEngineSettings,
      eventManager: EventManager,
      artifact: ArtifactDownloadReport,
      archive: File,
      path: String,
      options: RetrieveOptions
  ): RetResult = {
    val destFile = settings.resolveFile(path)
    if !settings.isCheckUpToDate() || !upToDate(archive, destFile, options) then {
      Message.verbose("\t\tto " + destFile)
      if eventManager != null then {
        eventManager.fireIvyEvent(new StartRetrieveArtifactEvent(artifact, destFile))
      }
      if options.isMakeSymlinks() then {
        var symlinkCreated = false
        try {
          FileUtil.symlink(archive, destFile, null, true)
          symlinkCreated = true
        } catch {
          case ioe: IOException =>
            symlinkCreated = false
            // warn about the inability to create a symlink
            Message.warn("symlink creation failed at path " + destFile)
        }
        if !symlinkCreated then {
          // since symlink creation failed, let's attempt to an actual copy instead
          Message.info(
            "attempting a copy operation (since symlink creation failed) at path " + destFile
          );
          FileUtil.copy(archive, destFile, null, true);
        }
      } else {
        FileUtil.copy(archive, destFile, null, true);
      }
      if eventManager != null then {
        eventManager.fireIvyEvent(new EndRetrieveArtifactEvent(artifact, destFile))
      }
      val copiedSize = destFile.length()

      RetResult(destFile, artifact, copiedSize, true)
      // report.addCopiedFile(destFile, artifact);
    } else {
      Message.verbose("\t\tto " + destFile + " [NOT REQUIRED]")
      RetResult(destFile, artifact, 0L, false)
      // report.addUpToDateFile(destFile, artifact);
    }
  }

  def getConfs(mrid: ModuleRevisionId, options: RetrieveOptions) = {
    var confs = options.getConfs()
    if confs == null || (confs.length == 1 && "*".equals(confs(0))) then {
      try {
        val md = getCache().getResolvedModuleDescriptor(mrid)
        Message.verbose(
          "no explicit confs given for retrieve, using ivy file: " + md.getResource().getName()
        )
        confs = md.getConfigurationsNames()
        options.setConfs(confs)
      } catch {
        case e: IOException =>
          throw e
        case ex: Exception =>
          throw new IOException(ex.getMessage(), ex)
      }
    }

    confs
  }

  def getCache() = settings.getResolutionCacheManager()

  def upToDate(source: File, target: File, options: RetrieveOptions): Boolean = {
    if !target.exists() then {
      return false
    }

    val overwriteMode = options.getOverwriteMode()
    if RetrieveOptions.OVERWRITEMODE_ALWAYS.equals(overwriteMode) then {
      return false
    }

    if RetrieveOptions.OVERWRITEMODE_NEVER.equals(overwriteMode) then {
      return true
    }

    if RetrieveOptions.OVERWRITEMODE_NEWER.equals(overwriteMode) then {
      return source.lastModified() <= target.lastModified()
    }

    if RetrieveOptions.OVERWRITEMODE_DIFFERENT.equals(overwriteMode) then {
      return source.lastModified() == target.lastModified()
    }

    // unknown, so just to be sure
    return false;
  }

}
