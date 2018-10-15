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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.Cache;

import java.util.concurrent.TimeUnit;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class InfinispanCacheFactory extends BenchmarkCacheFactory {

  static final String CACHE_NAME = "testCache";
  static EmbeddedCacheManager cacheManager;

  static synchronized EmbeddedCacheManager getCacheMangaer() {
    if (cacheManager == null) {
      cacheManager = new DefaultCacheManager();
    }
    return cacheManager;
  }

  Algorithm algorithm = Algorithm.DEFAULT;

  public InfinispanCacheFactory algorithm(Algorithm v) {
    algorithm = v;
    return this;
  }

  @Override
  protected <K, V> BenchmarkCache<K, V> createSpecialized(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
    EmbeddedCacheManager m = getCacheMangaer();
    ConfigurationBuilder cb = new ConfigurationBuilder();

    cb.eviction().maxEntries(_maxElements);
    cb.storeAsBinary().disable();
    if (!withExpiry) {
      cb.expiration().disableReaper().lifespan(-1);
    } else {
      cb.expiration().lifespan(5 * 60, TimeUnit.SECONDS);
    }
    switch (algorithm) {
      case LRU: cb.eviction().strategy(EvictionStrategy.LRU); break;
      case LIRS: cb.eviction().strategy(EvictionStrategy.LIRS); break;
      case UNORDERED: cb.eviction().strategy(EvictionStrategy.UNORDERED); break;
    }
    m.defineConfiguration(CACHE_NAME, cb.build());
    Cache<Integer, Integer> _cache = m.getCache(CACHE_NAME);
    return new MyBenchmarkCache(_cache);

  }

  public enum Algorithm { DEFAULT, LRU, LIRS, UNORDERED }

  static class MyBenchmarkCache<K,V> extends BenchmarkCache<K, V> {

    Cache<K, V> cache;

    MyBenchmarkCache(Cache<K, V> cache) {
      this.cache = cache;
    }

    @Override
    public V get(final K key) {
      return cache.get(key);
    }

    @Override
    public void put(K key, V value) {
      cache.put(key, value);
    }

    @Override
    public void remove(final K key) {
      cache.remove(key);
    }

    @Override
    public void close() {
      cache.getCacheManager().removeCache(CACHE_NAME);
    }

    @Override
    public int getCapacity() {
      return cache.getCacheConfiguration().eviction().maxEntries();
    }

    @Override
    public String toString() {
      return cache.toString() + ": size=" + cache.size();
    }

  }

}
