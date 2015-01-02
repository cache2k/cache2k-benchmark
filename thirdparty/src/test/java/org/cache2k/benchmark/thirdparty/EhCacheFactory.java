package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * cache2k-benchmark-thirdparty
 * %%
 * Copyright (C) 2013 - 2015 headissue GmbH, Munich
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class EhCacheFactory extends BenchmarkCacheFactory {

  static final String CACHE_NAME = "testCache";

  Algorithm algorithm = Algorithm.DEFAULT;

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    return new MyBenchmarkCache(createCacheConfiguration(_maxElements));
  }

  protected CacheManager getManager() {
    return CacheManager.getInstance();
  }

  protected CacheConfiguration createCacheConfiguration(int _maxElements) {
    MemoryStoreEvictionPolicy _policy = MemoryStoreEvictionPolicy.LRU;
    switch (algorithm) {
      case CLOCK: _policy = MemoryStoreEvictionPolicy.CLOCK; break;
    }
    CacheConfiguration c = new CacheConfiguration(CACHE_NAME, _maxElements)
      .memoryStoreEvictionPolicy(_policy)
      .eternal(true)
      .diskExpiryThreadIntervalSeconds(0)
      .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
    if (withExpiry) {
      c.setEternal(false);
      c.setTimeToLiveSeconds(5 * 60);
    }
    return c;
  }

  public EhCacheFactory algorithm(Algorithm v) {
    algorithm = v;
    return this;
  }

  static class MyCacheEntryFactory implements CacheEntryFactory {

    int missCount;

    @Override
    public Object createEntry(Object key) throws Exception {
      missCount++;
      return key;
    }

  }

  class MyBenchmarkCache extends BenchmarkCache<Integer, Integer> {

    CacheConfiguration config;
    MyCacheEntryFactory entryFactory;
    SelfPopulatingCache cache;

    MyBenchmarkCache(CacheConfiguration v) {
      this.config = v;
      Cache _testCache = new net.sf.ehcache.Cache(v);
      getManager().addCache(_testCache);
      cache = new SelfPopulatingCache(_testCache, entryFactory = new MyCacheEntryFactory());
    }

    @Override
    public int getCacheSize() {
      return (int) config.getMaxEntriesLocalHeap();
    }

    @Override
    public Integer get(Integer key) {
      return (Integer) cache.get(key).getObjectValue();
    }

    @Override
    public void destroy() {
      CacheManager.getInstance().removeCache("testCache");
    }

    @Override
    public int getMissCount() {
      return entryFactory.missCount;
    }
  }

  public enum Algorithm { DEFAULT, CLOCK }

}
