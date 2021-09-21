package org.cache2k.benchmark.cache;

/*
 * #%L
 * Benchmarks: third party products.
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.ProductCacheFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Jens Wilke
 */
public class GuavaCacheFactory extends ProductCacheFactory {

  @Override
  public <K, V> BenchmarkCache<K, V> create(Class<K> keyType, Class<V> valueType, int capacity) {
    MyBenchmarkCache c = new MyBenchmarkCache();
    c.size = capacity;
    CacheBuilder cb = builder(capacity);
    c.cache = cb.build();
    return c;
  }

  @Override
  public <K, V> BenchmarkCache<K, V> createLoadingCache(
    Class<K> keyType, Class<V> valueType,
    int capacity, BenchmarkCacheLoader<K, V> loader) {
    MyLoadingBenchmarkCache c = new MyLoadingBenchmarkCache();
    c.size = capacity;
    CacheBuilder cb = builder(capacity);
    c.cache = cb.build(new CacheLoader<K, V>() {
      @Override
      public V load(K key) throws Exception {
        return loader.load(key);
      }
    });
    return c;
  }

  private CacheBuilder builder(int capacity) {
    CacheBuilder cb =
      CacheBuilder.newBuilder()
        .maximumSize(capacity);
    if (withExpiry) {
      cb.expireAfterWrite(2 * 60, TimeUnit.SECONDS);
    }
    return cb;
  }

  static class MyBenchmarkCache<K,V> extends BenchmarkCache<K, V> {

    int size;
    Cache<K, V> cache;

    @Override
    public V get(K key) {
      return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
      cache.put(key, value);
    }

    @Override
    public void remove(K key) {
      cache.invalidate(key);
    }

    @Override
    public void close() {
      cache.cleanUp();
    }

    @Override
    public void clear() { cache.asMap().clear(); }

  }

  static class MyLoadingBenchmarkCache<K, V> extends BenchmarkCache<K, V> {

    int size;
    LoadingCache<K, V> cache;

    @Override
    public V get(K key) {
      try {
        return cache.get(key);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public void put(K key, V value) {
      cache.put(key, value);
    }

    @Override
    public void close() {
      cache.cleanUp();
    }

    @Override
    public void clear() { cache.asMap().clear(); }

  }

}
