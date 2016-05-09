package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * thirdparty
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;

/**
 * Factory for EHCache2
 *
 * @author Jens Wilke
 */
public class EhCache2Factory extends BenchmarkCacheFactory {

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

  public EhCache2Factory algorithm(Algorithm v) {
    algorithm = v;
    return this;
  }

  class MyBenchmarkCache extends BenchmarkCache<Integer, Integer> {

    CacheConfiguration config;
    Ehcache cache;

    MyBenchmarkCache(CacheConfiguration v) {
      this.config = v;
      Ehcache _testCache = cache = new net.sf.ehcache.Cache(v);
      getManager().addCache(_testCache);
    }

    @Override
    public int getCacheSize() {
      return (int) config.getMaxEntriesLocalHeap();
    }

    @Override
    public Integer getIfPresent(final Integer key) {
      Element e = cache.get(key);
      if (e != null) {
        return (Integer) e.getObjectValue();
      }
      return null;
    }

    @Override
    public void put(final Integer key, final Integer value) {
      cache.put(new Element(key, value));
    }

    @Override
    public void destroy() {
      CacheManager.getInstance().removeCache("testCache");
    }

  }

  public enum Algorithm { DEFAULT, CLOCK }

}
