/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbti;

import java.io.File;

public interface AppConfiguration
{
	public String[] arguments();
	public File baseDirectory();
	public AppProvider provider();
}