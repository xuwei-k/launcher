/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbti;

import java.io.File;
import java.util.concurrent.Callable;

public interface GlobalLock
{
	public <T> T apply(File lockFile, Callable<T> run);
}