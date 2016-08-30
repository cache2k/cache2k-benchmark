package org.cache2k.benchmark;

/*
 * #%L
 * zoo
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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

/**
 * Interface to a cache implementation we use for benchmarking.
 *
 * @author Jens Wilke; created: 2013-06-15
 */
public abstract class BenchmarkCache<K, T> {

  /**
   * Return the element that is present in the cache. The cache source will not be called in turn.
   */
  public T getIfPresent(K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Puts an entry in the cache. Needed for read/write benchmark.
   */
  public void put(K key, T value) {
    throw new UnsupportedOperationException();
  }

  /** free up all resources of the cache */
  public abstract void destroy();

  /** Configured maximum entry count of the cache */
  public abstract int getCacheSize();

  /** Statistics string produced by the cache. */
  public String getStatistics() { return "<none>"; }

  /** Optional, checks the cache integrity. Called after a run. */
  public void checkIntegrity() { }

  /**
   * Return the original implementation. We use this for experimentation to
   * get and set some runtime parameters during benchmarks runs.
   */
  public Object getOriginalCache() { return null; }

}
