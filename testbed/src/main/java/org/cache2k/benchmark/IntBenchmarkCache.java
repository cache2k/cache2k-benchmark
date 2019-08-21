package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
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

/**
 * Specialized version with int cache key.
 *
 * @author Jens Wilke
 */
public abstract class IntBenchmarkCache<V> extends BenchmarkCache<Integer, V> {

  /**
   * If the cache does not natively support integer key, just wrap.
   */
  public static <V> IntBenchmarkCache<V> wrap(final BenchmarkCache<Integer, V> c) {
    return wrapToInt(c);
  }

  public static <V> IntBenchmarkCache<V> wrapToInt(final BenchmarkCache<Integer, V> c) {
    return new IntBenchmarkCache<V>() {
      @Override
      public V get(final Integer key) {
        return c.get(key);
      }

      @Override
      public void put(final Integer key, final V value) {
        c.put(key, value);
      }

      @Override
      public void remove(final Integer key) {
        c.remove(key);
      }

      @Override
      public void close() {
        c.close();
      }

      @Override
      public String toString() {
        return c.toString();
      }
    };
  }

  /**
   * Return the element that is present in the cache. The cache source will not be called in turn.
   */
  public V get(Integer key) {
    return getIfPresent((int) key);
  }

  /**
   * Puts an entry in the cache. Needed for read/write benchmark.
   */
  public void put(Integer key, V value) {
    put((int) key, value);
  }

  /**
   * Return the element that is present in the cache. The cache source will not be called in turn.
   */
  public V getIfPresent(int key) {
    return get((Integer) key);
  }

  /**
   * Puts an entry in the cache. Needed for read/write benchmark.
   */
  public void put(int key, V value) {
    put((Integer) key, value);
  }

  public void remove(int key) {
    remove(key);
  }

}
