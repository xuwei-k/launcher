/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbti;

import java.io.File;

/**
 * Marker interface for classloader with just scala-library.
 */
public interface LibraryClassLoader
{
  public String scalaVersion();
}
