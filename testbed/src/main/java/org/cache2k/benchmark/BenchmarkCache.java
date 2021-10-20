package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;

/**
 * Interface to a cache implementation we use for benchmarking.
 *
 * @author Jens Wilke
 */
public abstract class BenchmarkCache<K, V> implements AutoCloseable {

  /**
   * Return the element that is present in the cache.
   * If created via {@link BenchmarkCacheFactory#createLoadingCache(Class, Class, int, BenchmarkCacheLoader)}
   * the source will be called to populate the cache.
   */
  public V get(K key) {
    throw new UnsupportedOperationException();
  }

  public Map<K, V> getAll(Iterable<K> keys) {
    throw new UnsupportedOperationException();
  }

  /**
   * Puts an entry in the cache. Needed for read/write benchmark.
   */
  public void put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Remove entry from cache. EHCache3 does not support remove with success flag, so
   * we do not return a boolean.
   */
  public void remove(K key) { throw new UnsupportedOperationException();}

  /**
   * Get the exact cache size, preferably via iteration.
   */
  public long getSize() { return -1; }

  protected static long count(Iterator<?> it) {
    int cnt = 0;
    while(it.hasNext()) { it.next(); cnt++; }
    return cnt;
  }

  /** free up all resources of the cache */
  public void close() { }

  public EvictionStatistics getEvictionStatistics() {
    return new EvictionStatistics() {};
  }

  public void clear() { throw new UnsupportedOperationException(); }

}
