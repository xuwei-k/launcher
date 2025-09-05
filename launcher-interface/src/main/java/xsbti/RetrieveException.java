/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbti;

public final class RetrieveException extends RuntimeException
{
	private final String version;
	public RetrieveException(String version, String msg)
	{
		super(msg);
		this.version = version;
	}
	public String version() { return version; }
}