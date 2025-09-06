/* sbt -- Simple Build Tool
 * Copyright 2009 Mark Harrah
 */
package xsbt.boot

import Pre.*
import java.net.{ MalformedURLException, URL }

object CheckProxy:
  def apply(): Unit =
    import ProxyProperties.*
    for pp <- Seq(http, https, ftp) do setFromEnv(pp)

  private def setFromEnv(conf: ProxyProperties): Unit =
    import conf.*
    val proxyURL = System.getenv(envURL)
    if isDefined(proxyURL) && !isPropertyDefined(sysHost) && !isPropertyDefined(sysPort) then
      try
        val proxy = new URL(proxyURL)
        setProperty(sysHost, proxy.getHost)
        val port = proxy.getPort
        if port >= 0 then System.setProperty(sysPort, port.toString)
        copyEnv(envUser, sysUser)
        copyEnv(envPassword, sysPassword)
      catch
        case e: MalformedURLException =>
          Console.err.println(s"[warn] [launcher] could not parse $envURL setting: ${e.toString}")

  private def copyEnv(envKey: String, sysKey: String): Unit =
    setProperty(sysKey, System.getenv(envKey))
  private def setProperty(key: String, value: String): Unit =
    if value != null then System.setProperty(key, value)
    ()
  private def isPropertyDefined(k: String) = isDefined(System.getProperty(k))
  private def isDefined(s: String) = s != null && isNonEmpty(s)
