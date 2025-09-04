/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Mark Harrah
 */
package xsbt.boot

import java.lang.ref.{ Reference, SoftReference }
import java.util.HashMap

final class Cache[K, X, V](create: (K, X) => V):
  private val delegate = new HashMap[K, Reference[V]]
  def apply(k: K, x: X): V = synchronized { getFromReference(k, x, delegate.get(k)) }
  private def getFromReference(k: K, x: X, existingRef: Reference[V]) =
    if existingRef eq null then newEntry(k, x) else get(k, x, existingRef.get)
  private def get(k: K, x: X, existing: V) =
    if existing == null then newEntry(k, x) else existing
  private def newEntry(k: K, x: X): V =
    val v = create(k, x)
    Pre.assert(v != null, "Value for key " + k + " was null")
    delegate.put(k, new SoftReference(v))
    v
