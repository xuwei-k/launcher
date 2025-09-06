/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbti;

/**
 * A launched application returns an instance of this class in order to communicate to the launcher
 * that the application finished and the launcher should exit with the given exit code.
 */
public interface Exit extends MainResult
{
	public int code();
}