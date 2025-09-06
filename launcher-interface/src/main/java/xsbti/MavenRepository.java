/*
 * sbt
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package xsbti;

	import java.net.URL;

public interface MavenRepository extends Repository
{
	String id();
	URL url();
	boolean allowInsecureProtocol();
}