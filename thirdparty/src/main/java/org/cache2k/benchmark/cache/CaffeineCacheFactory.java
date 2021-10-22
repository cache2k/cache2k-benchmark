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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.BulkBenchmarkCacheLoader;
import org.cache2k.benchmark.ProductCacheFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Factory for Caffeine cache
 *
 * @author Jens Wilke
 */
public class CaffeineCacheFactory extends ProductCacheFactory {

  private boolean sameThreadEviction = false;
  private boolean fullEvictionCapacity = false;

  /**
   * Runs eviction tasks within the calling thread. Otherwise eviction tasks are delayed
   * leading to false results when benchmarking eviction efficiency on access traces.
   */
  public CaffeineCacheFactory sameThreadEviction(boolean f) {
    sameThreadEviction = f;
    return this;
  }

  public CaffeineCacheFactory fullEvictionCapacity(boolean f) {
    fullEvictionCapacity = f;
    return this;
  }

  @Override
  public <K, V> BenchmarkCache<K, V> create(Class<K> keyType, Class<V> valueType, int capacity) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.cache = createCache(capacity).build();
    return c;
  }

  @Override
  public <K, V> BenchmarkCache<K, V> createLoadingCache(
    Class<K> keyType, Class<V> valueType, int capacity, BenchmarkCacheLoader<K, V> loader) {
    CacheLoader<K, V> l;
    if (loader instanceof BulkBenchmarkCacheLoader) {
      l = new CacheLoader<K, V>() {
        @Override
        public @Nullable V load(@NonNull K key) {
          return loader.load(key);
        }
        @Override
        public @NonNull Map<K, V> loadAll(@NonNull Set<? extends K> keys) {
          return ((BulkBenchmarkCacheLoader<K, V>) loader).loadAll(keys);
        }
      };
    } else {
      l = key -> loader.load(key);
    }
    MyLoadingBenchmarkCache c = new MyLoadingBenchmarkCache();
    c.cache = createCache(capacity).build(l);
    return c;
  }

  private Caffeine createCache(int capacity) {
    Caffeine b = Caffeine.newBuilder().maximumSize(capacity);
    if (sameThreadEviction) {
      b.executor(Runnable::run);
    }
    if (fullEvictionCapacity) {
      b.initialCapacity(capacity);
    }
    if (withExpiry) {
      b.expireAfterWrite(2 * 60, TimeUnit.SECONDS);
    }
    return b;
  }

  static class MyBenchmarkCacheAdapter<K,V> extends BenchmarkCache<K, V> {

    private Cache<K, V> cache;

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
    public long getSize() { return count(cache.asMap().keySet().iterator()); }

    @Override
    public String toString() {
      return cache.toString();
    }

    @Override
    public void clear() { cache.asMap().clear(); }

    @Override
    public Iterator<K> keys() {
      return cache.asMap().keySet().iterator();
    }
  }

  static class MyLoadingBenchmarkCache<K, V> extends BenchmarkCache<K, V> {

    private LoadingCache<K, V> cache;

    @Override
    public V get(K key) {
      return cache.get(key);
    }

    @Override
    public Map<K, V> getAll(Iterable<K> keys) {
      return cache.getAll(keys);
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
    public long getSize() { return count(cache.asMap().keySet().iterator()); }

    @Override
    public String toString() {
      return cache.toString();
    }

    @Override
    public void clear() { cache.asMap().clear(); }

    @Override
    public Iterator<K> keys() {
      return cache.asMap().keySet().iterator();
    }
  }

}
