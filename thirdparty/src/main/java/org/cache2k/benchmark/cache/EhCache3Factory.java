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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.BulkBenchmarkCacheLoader;
import org.cache2k.benchmark.ProductCacheFactory;
import org.ehcache.Cache;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;

import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Jens Wilke
 */
public class EhCache3Factory extends ProductCacheFactory {

  static final org.ehcache.CacheManager MANAGER = CacheManagerBuilder.newCacheManagerBuilder().build(true);
  static final String CACHE_NAME = "testCache";

  @Override
  public <K, V> BenchmarkCache<K, V> create(Class<K> keyType, Class<V> valueType, int capacity) {
    return new MyBenchmarkCache<K,V>(createCacheConfiguration(keyType, valueType, capacity));
  }

  protected <K,V> CacheConfiguration<K,V> createCacheConfiguration(
    Class<K> keyType, Class<V> valueType, int capacity) {
    return builder(keyType, valueType, capacity).build();
  }

  private <K, V> CacheConfigurationBuilder<K, V> builder(Class<K> keyType, Class<V> valueType, int capacity) {
    CacheConfigurationBuilder<K, V> b = CacheConfigurationBuilder.newCacheConfigurationBuilder(keyType, valueType,
      ResourcePoolsBuilder.heap(capacity));
    Duration d = Duration.ofMinutes(5);
    if (withExpiry) {
      b.withExpiry(new ExpiryPolicy<K, V>() {
        @Override
        public Duration getExpiryForCreation(K key, V value) {
          return d;
        }
        @Override
        public Duration getExpiryForAccess(K key, Supplier<? extends V> value) {
          return null;
        }
        @Override
        public Duration getExpiryForUpdate(K key, Supplier<? extends V> oldValue, V newValue) {
          return d;
        }
      });
    }
    if (withTimeToIdle) {
      b.withExpiry(new ExpiryPolicy<K, V>() {
        @Override
        public Duration getExpiryForCreation(K key, V value) {
          return d;
        }
        @Override
        public Duration getExpiryForAccess(K key, Supplier<? extends V> value) {
          return d;
        }
        @Override
        public Duration getExpiryForUpdate(K key, Supplier<? extends V> oldValue, V newValue) {
          return d;
        }
      });
    }
    return b;
  }

  @Override
  public <K, V> BenchmarkCache<K, V> createLoadingCache(Class<K> keyType, Class<V> valueType,
                                                        int capacity, BenchmarkCacheLoader<K, V> loader) {
    CacheLoaderWriter<K, V> lw;
    if (loader instanceof BulkBenchmarkCacheLoader) {
      lw = new CacheLoaderWriter<K, V>() {
        @Override
        public V load(K key) {
          return loader.load(key);
        }
        @Override
        public Map<K, V> loadAll(Iterable<? extends K> keys) {
          return ((BulkBenchmarkCacheLoader) loader).loadAll(keys);
        }
        @Override
        public void write(K key, V value) { }
        @Override
        public void delete(K key) throws Exception { }
       };
    } else {
     lw = new CacheLoaderWriter<K, V>() {
        @Override
        public V load(K key) throws Exception {
          return loader.load(key);
        }
        @Override
        public Map<K, V> loadAll(Iterable<? extends K> keys) {
         return ((BulkBenchmarkCacheLoader) loader).loadAll(keys);
        }
        @Override
        public void write(K key, V value) { }
        @Override
        public void delete(K key) { }
      };
    }
    CacheConfiguration<K,V> cfg =
      builder(keyType, valueType, capacity)
        .withLoaderWriter(lw)
        .build();
    return new MyBenchmarkCache<K,V>(cfg);
  }

  class MyBenchmarkCache<K,V> extends BenchmarkCache<K, V> {

    CacheConfiguration config;
    org.ehcache.Cache<K,V> cache;

    MyBenchmarkCache(CacheConfiguration<K, V> cfg) {
      this.config = cfg;
      cache = MANAGER.createCache(CACHE_NAME, cfg);
    }

    @Override
    public V get(K key) {
      return cache.get(key);
    }

    @Override
    public Map<K, V> getAll(Iterable<K> keys) {
      Set<K> keySet = new HashSet<>();
      for (K key : keys) { keySet.add(key); }
      return cache.getAll(keySet);
    }

    @Override
    public void put(K key, V value) {
      cache.put(key, value);
    }

    @Override
    public void remove(K key) {
      cache.remove(key);
    }

    @Override
    public void close() {
      MANAGER.removeCache(CACHE_NAME);
    }

    @Override
    public long getSize() { return count(cache.iterator()); }

    @Override
    public String toString() {
      return cache.toString();
    }

    @Override
    public void clear() { cache.clear(); }

    @Override
    public Iterator<K> keys() {
      Iterator<Cache.Entry<K, V>> entryIterator = cache.iterator();
      return new Iterator<K>() {
        @Override
        public boolean hasNext() {
          return entryIterator.hasNext();
        }

        @Override
        public K next() {
          return entryIterator.next().getKey();
        }
      };
    }
  }

}
