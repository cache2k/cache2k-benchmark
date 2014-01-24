package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * cache2k-benchmark-thirdparty
 * %%
 * Copyright (C) 2013 - 2014 headissue GmbH, Munich
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
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

  static class MySimpleCacheSource {

    int missCnt;

    public Integer get(Integer key) {
      missCnt++;
      return key;
    }

  }

  static class MyBenchmarkCache extends BenchmarkCache<Integer, Integer> {

    Cache<Integer, Integer> cache;
    MySimpleCacheSource source = new MySimpleCacheSource();

    MyBenchmarkCache(Cache<Integer, Integer> cache) {
      this.cache = cache;
    }

    /**
     * Delegates to a cache source if not found. It seems rather complicated to
     * define a custom cache loader, so simulate a loading cache.
     */
    @Override
    public Integer get(Integer key) {
      Integer v = cache.get(key);
      if (v == null) {
        v = source.get(key);
        cache.put(key, v);
      }
      return v;
    }

    @Override
    public void destroy() {
      cache.getCacheManager().removeCache(CACHE_NAME);
    }

    @Override
    public int getCacheSize() {
      return cache.getCacheConfiguration().eviction().maxEntries();
    }

    @Override
    public int getMissCount() {
      return source.missCnt;
    }

    @Override
    public String getStatistics() {
      return cache.toString() + ": size=" + cache.size();
    }

  }

}
