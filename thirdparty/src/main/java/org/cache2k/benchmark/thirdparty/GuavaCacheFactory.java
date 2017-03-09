package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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
import org.cache2k.benchmark.BenchmarkCacheSource;
import org.cache2k.benchmark.LoadingBenchmarkCache;

import java.util.concurrent.TimeUnit;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class GuavaCacheFactory extends BenchmarkCacheFactory {

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    MyBenchmarkCache c = new MyBenchmarkCache();
    c.size = _maxElements;
    CacheBuilder cb = builder(_maxElements);
    c.cache = cb.build();
    return c;
  }

  @Override
  public <K, V> LoadingBenchmarkCache<K, V> createLoadingCache(
    final Class<K> _keyType, final Class<V> _valueType,
    final int _maxElements, final BenchmarkCacheSource<K, V> _source) {
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
      cb.expireAfterWrite(5 * 60, TimeUnit.SECONDS);
    }
    cb.concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2);
    return cb;
  }

  static class MyBenchmarkCache extends BenchmarkCache<Integer, Integer> {

    int size;
    Cache<Integer, Integer> cache;

    @Override
    public int getCacheSize() {
      return size;
    }

    @Override
    public Integer getIfPresent(final Integer key) {
      return cache.getIfPresent(key);
    }

    @Override
    public void put(final Integer key, final Integer value) {
      cache.put(key, value);
    }

    @Override
    public void close() {
      cache.cleanUp();
    }

  }

  static class MyLoadingBenchmarkCache<K, V> extends LoadingBenchmarkCache<K, V> {

    int size;
    LoadingCache<K, V> cache;

    @Override
    public int getCacheSize() {
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
