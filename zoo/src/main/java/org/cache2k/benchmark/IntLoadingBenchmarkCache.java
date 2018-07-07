package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: implementation variants
 * %%
 * Copyright (C) 2013 - 2018 headissue GmbH, Munich
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
 * Specialized version for int cache keys.
 *
 * @author Jens Wilke
 */
public abstract class IntLoadingBenchmarkCache<V> extends LoadingBenchmarkCache<Integer, V> {

  public static <K, V> LoadingBenchmarkCache<K, V> wrap(final LoadingBenchmarkCache<K, V> c) {
    return (LoadingBenchmarkCache<K, V> ) wrapToInt((LoadingBenchmarkCache<Integer, V>) c);
  }

  /**
   * If the cache does not natively support integer key, just wrap.
   */
  public static <V> IntLoadingBenchmarkCache<V> wrapToInt(final LoadingBenchmarkCache<Integer, V> c) {
    return new IntLoadingBenchmarkCache<V>() {
      @Override
      public V get(final Integer key) {
        return c.get(key);
      }

      @Override
      public void put(final Integer key, final V value) {
        c.put(key, value);
      }

      @Override
      public void close() {
        c.close();
      }

      @Override
      public int getCacheSize() {
        return c.getCacheSize();
      }

      @Override
      public String toString() {
        return c.toString();
      }
    };
  }

  @Override
  public V get(final Integer key) {
    return get((int) key);
  }

  @Override
  public void put(final Integer key, final V value) {
    put((int) key, value);
  }

  /**
   * Return the element that is present in the cache or invoke the loader.
   */
  public V get(int key) {
    return get((Integer) key);
  }

  /**
   * Puts an entry in the cache. Needed for read/write benchmark.
   */
  public void put(int key, V value) {
    put((Integer) key, value);
  }

}
