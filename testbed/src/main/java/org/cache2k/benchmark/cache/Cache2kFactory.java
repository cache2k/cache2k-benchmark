package org.cache2k.benchmark.cache;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
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

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheEntry;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.BulkBenchmarkCacheLoader;
import org.cache2k.benchmark.EvictionListener;
import org.cache2k.benchmark.ProductCacheFactory;
import org.cache2k.event.CacheEntryEvictedListener;
import org.cache2k.event.CacheEntryUpdatedListener;
import org.cache2k.io.BulkCacheLoader;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class Cache2kFactory extends ProductCacheFactory {

  private final AtomicInteger counter = new AtomicInteger();
  private boolean maximumPerformance = true;
  private boolean strictEviction = false;
  private boolean wiredCache = false;

  @Override
  public <K, V> BenchmarkCache<K, V> create(Class<K> keyType, Class<V> valueType, int capacity) {
    Cache<K, V> cache = createInternal(keyType, valueType, capacity, null);
    return new BenchmarkCache<K, V>() {

      @Override
      public V get(K key) {
        return cache.peek(key);
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
        cache.close();
      }

      @Override
      public String toString() {
        return cache.toString();
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public <K, V> BenchmarkCache<K, V> createLoadingCache(Class<K> keyType, Class<V> valueType,
                                                        int maxElements,
                                                        BenchmarkCacheLoader<K, V> loader) {
    Cache<K, V> c = createInternal(keyType, valueType, maxElements, loader);
    return new BenchmarkCache<K, V>() {
      @Override
      public V get(K key) {
        return c.get(key);
      }

      @Override
      public Map<K, V> getAll(Iterable<K> keys) { return c.getAll(keys); }

      @Override
      public void put(K key, V value) {
        c.put(key, value);
      }

      @Override
      public void remove(K key) {
        c.remove(key);
      }

      @Override
      public String toString() {
        return c.toString();
      }

      @Override
      public void close() {
        c.close();
      }
    };
  }

  private <K,V> Cache<K, V> createInternal(Class<K> keyType, Class<V> valueType,
                                           int maxElements, BenchmarkCacheLoader<K, V> loader) {
    Cache2kBuilder<K, V> b =
      Cache2kBuilder.of(keyType, valueType)
        .name("testCache-" + counter.incrementAndGet())
        .entryCapacity(maxElements)
        .refreshAhead(false)
        .strictEviction(strictEviction);
    if (wiredCache) {
      b.addListener(new CacheEntryUpdatedListener<K, V>() {
        @Override
        public void onEntryUpdated(Cache<K, V> cache, CacheEntry<K, V> currentEntry,
                                   CacheEntry<K, V> entryWithNewData) {
        }
      });
    }
    if (withExpiry) {
      b.expireAfterWrite(2 * 60, TimeUnit.SECONDS);
    } else {
      b.eternal(true);
    }
    if (maximumPerformance) {
      b.disableStatistics(true)
        .strictEviction(false)
        .boostConcurrency(true)
        .recordModificationTime(false);
    } else {
      b.strictEviction(true);
    }
    for (EvictionListener l : getEvictionListeners()) {
      b.addListener((CacheEntryEvictedListener<K, V>) (cache, entry) -> {
        l.evicted(entry.getKey());
      });
    }
    if (loader != null) {
      if (loader instanceof BulkBenchmarkCacheLoader) {
        b.bulkLoader(new BulkCacheLoader<K, V>() {
          @Override
          public V load(K key) throws Exception {
            return loader.load(key);
          }
          @Override
          public Map<K, V> loadAll(Set<? extends K> set) throws Exception {
            return ((BulkBenchmarkCacheLoader<K, V>) loader).loadAll(set);
          }
        });
      } else {
        b.loader(key -> loader.load(key));
      }
    }
    return b.build();
  }

  public void setMaximumPerformance(boolean v) {
    maximumPerformance = v;
  }

  public void setStrictEviction(boolean v) {
    strictEviction = v;
  }

  public void setWiredCache(boolean v) {
    wiredCache = v;
  }

}
