package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheSource;
import org.cache2k.benchmark.IntLoadingBenchmarkCache;
import org.cache2k.benchmark.LoadingBenchmarkCache;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Factory for Caffeine
 *
 * @author Jens Wilke
 */
public class CaffeineCacheFactory extends BenchmarkCacheFactory {

  private boolean sameThreadEviction = false;
  private boolean fullEvictionCapacity = false;

  CaffeineCacheFactory sameThreadEviction(boolean f) {
    sameThreadEviction = f;
    return this;
  }

  CaffeineCacheFactory fullEvictionCapacity(boolean f) {
    fullEvictionCapacity = f;
    return this;
  }

  @Override
  protected <K, V> BenchmarkCache<K, V> createUnspecialized(
    final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.size = _maxElements;
    c.cache = createCache(_maxElements).build();
    return c;
  }

  @Override
  public <K, V> LoadingBenchmarkCache<K, V> createUnspecializedLoadingCache(
    final Class<K> _keyType, final Class<V> _valueType,
    final int _maxElements, final BenchmarkCacheSource<K, V> _source) {
    CacheLoader<K,V> l = new CacheLoader<K, V>() {
      @Override
      public V load(final K key) {
        return _source.load(key);
      }
    };
    MyLoadingBenchmarkCache c = new MyLoadingBenchmarkCache();
    c.size = _maxElements;
    c.cache = createCache(_maxElements).build(l);
    return c;
  }

  private Caffeine createCache(final int _maxElements) {
    Caffeine b = Caffeine.newBuilder().maximumSize(_maxElements);
    if (sameThreadEviction) {
      b.executor(Runnable::run);
    }
    if (fullEvictionCapacity) {
      b.initialCapacity(_maxElements);
    }
    if (withExpiry) {
      b.expireAfterWrite(2 * 60, TimeUnit.SECONDS);
    }
    return b;
  }

  static class MyBenchmarkCacheAdapter<K,V> extends BenchmarkCache<K, V> {

    int size;
    Cache<K, V> cache;

    @Override
    public int getCacheSize() {
      return size;
    }

    @Override
    public V getIfPresent(final K key) {
      return cache.getIfPresent(key);
    }

    @Override
    public void put(final K key, final V value) {
      cache.put(key, value);
    }

    @Override
    public void close() {
      cache.cleanUp();
    }

    @Override
    public String toString() {
      return cache.toString();
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
      return cache.get(key);
    }

    @Override
    public void put(final K key, final V value) {
      cache.put(key, value);
    }

    @Override
    public void close() {
      cache.cleanUp();
    }

    @Override
    public String toString() {
      return cache.toString();
    }

  }

}
