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

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.integration.CacheLoader;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class Cache2kFactory extends BenchmarkCacheFactory {

  AtomicInteger counter = new AtomicInteger();
  boolean disableStatistics = true;

  @Override
  public BenchmarkCache<Integer, Integer> create(final int _maxElements) {
    final Cache<Integer, Integer> c = createInternal(Integer.class, Integer.class, _maxElements, null);
    return new BenchmarkCache<Integer, Integer>() {

      @Override
      public int getCacheSize() {
        return _maxElements;
      }

      @Override
      public Integer getIfPresent(Integer key) {
        return c.peek(key);
      }

      @Override
      public void put(Integer key, Integer value) {
        c.put(key, value);
      }

      @Override
      public void close() {
        c.close();
      }

      @Override
      public String toString() {
        return c.toString();
      }

      @Override
      public Object getOriginalCache() {
        return c;
      }
    };

  }

  @Override
  public <K, V> LoadingBenchmarkCache<K, V> createLoadingCache(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements, final BenchmarkCacheSource<K, V> _source) {
    final Cache<K, V> c = createInternal(_keyType, _valueType, _maxElements, _source);
    return new LoadingBenchmarkCache<K, V>() {
      @Override
      public V get(final K key) {
        return c.get(key);
      }

      @Override
      public void put(final K key, final V value) {
        c.put(key, value);
      }

      @Override
      public String toString() {
        return c.toString();
      }

      @Override
      public Object getOriginalCache() {
        return c;
      }

      @Override
      public void close() {
        c.close();
      }

      @Override
      public int getCacheSize() {
        return _maxElements;
      }
    };
  }

  <K,V> Cache<K, V> createInternal(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements, final BenchmarkCacheSource<K, V> _source) {
    Cache2kBuilder<K, V> b =
      Cache2kBuilder.of(_keyType, _valueType)
        .name("testCache-" + counter.incrementAndGet())
        .entryCapacity(_maxElements)
        .refreshAhead(false)
        .strictEviction(true);
    if (withExpiry) {
      b.expireAfterWrite(2 * 60, TimeUnit.SECONDS);
    } else {
      b.eternal(true);
    }
    if (disableStatistics) {
      b.disableStatistics(true).strictEviction(false).boostConcurrency(true).disableLastModificationTime(true);
    }
    if (_source != null) {
      b.loader(new CacheLoader<K, V>() {
        @Override
        public V load(final K key) throws Exception {
          return _source.load(key);
        }
      });
    }
    return b.build();
  }

}
