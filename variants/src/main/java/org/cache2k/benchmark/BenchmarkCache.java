package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Implementation and eviction variants
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
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
import java.util.ArrayList;
import java.util.List;

/**
 * Interface to a cache implementation we use for benchmarking.
 *
 * @author Jens Wilke; created: 2013-06-15
 */
public abstract class BenchmarkCache<K, V> implements Closeable {

  private List<EvictionListener<K>> evictionListeners = new ArrayList<>();

  /**
   * Return the element that is present in the cache.
   * If created via {@link BenchmarkCacheFactory#createLoadingCache(Class, Class, int, BenchmarkCacheSource)}
   * the source will be called to populate the cache.
   */
  public V get(K key) {
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

  /** free up all resources of the cache */
  public void close() { }

  /** Configured maximum entry count of the cache */
  public abstract int getCapacity();

  /**
   * Return the original implementation. We use this for experimentation to
   * get and set some runtime parameters during benchmarks runs.
   */
  public Object getOriginalCache() { return null; }

  /**
   * Registers an eviction listener if the cache implementation supports it.
   * If the cache implementation does not support it, does nothing.
   */
  public final void registerEvictionNotifier(EvictionListener el) {
    evictionListeners.add(el);
  }

  public final Iterable<EvictionListener<K>> getEvictionListeners() {
    return evictionListeners;
  }

}
