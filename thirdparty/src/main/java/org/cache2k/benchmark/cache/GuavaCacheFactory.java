package org.cache2k.benchmark.cache;

/*
 * #%L
 * Benchmarks: third party products.
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheLoader;

import java.util.concurrent.TimeUnit;

/**
 * @author Jens Wilke
 */
public class GuavaCacheFactory extends BenchmarkCacheFactory {

  @Override
  protected <K, V> BenchmarkCache<K, V> createSpecialized(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
    MyBenchmarkCache c = new MyBenchmarkCache();
    c.size = _maxElements;
    CacheBuilder cb = builder(_maxElements);
    c.cache = cb.build();
    return c;
  }

  @Override
  public <K, V> BenchmarkCache<K, V> createUnspecializedLoadingCache(
    final Class<K> _keyType, final Class<V> _valueType,
    final int _maxElements, final BenchmarkCacheLoader<K, V> _source) {
    MyLoadingBenchmarkCache c = new MyLoadingBenchmarkCache();
    c.size = _maxElements;
    CacheBuilder cb = builder(_maxElements);
    c.cache = cb.build(new CacheLoader<K, V>() {
      @Override
      public V load(final K key) throws Exception {
        return _source.load(key);
      }
    });
    return c;
  }

  private CacheBuilder builder(final int _maxElements) {
    CacheBuilder cb =
      CacheBuilder.newBuilder()
        .maximumSize(_maxElements);
    if (withExpiry) {
      cb.expireAfterWrite(2 * 60, TimeUnit.SECONDS);
    }
    return cb;
  }

  static class MyBenchmarkCache<K,V> extends BenchmarkCache<K, V> {

    int size;
    Cache<K, V> cache;

    @Override
    public int getCapacity() {
      return size;
    }

    @Override
    public V get(final K key) {
      return cache.getIfPresent(key);
    }

    @Override
    public void put(final K key, final V value) {
      cache.put(key, value);
    }

    @Override
    public void remove(final K key) {
      cache.invalidate(key);
    }

    @Override
    public void close() {
      cache.cleanUp();
    }

  }

  static class MyLoadingBenchmarkCache<K, V> extends BenchmarkCache<K, V> {

    int size;
    LoadingCache<K, V> cache;

    @Override
    public int getCapacity() {
      return size;
    }

    @Override
    public V get(final K key) {
      try {
        return cache.get(key);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public void put(final K key, final V value) {
      cache.put(key, value);
    }

    @Override
    public void close() {
      cache.cleanUp();
    }

  }

}
