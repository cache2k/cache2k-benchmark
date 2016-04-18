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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;

/**
 * EHCache2 not using the self populating cache.
 *
 * @author Jens Wilke
 */
public class EhCacheDirectFactory extends BenchmarkCacheFactory {

  static final CacheManager CACHE_MANAGER = CacheManager.getInstance();
  static final String CACHE_NAME = "testCache";

  Algorithm algorithm = Algorithm.DEFAULT;

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    return this.create(null, _maxElements);
  }

  @Override
  public BenchmarkCache<Integer, Integer> create(Source _source, int _maxElements) {
    return new MyBenchmarkCache(_source, createCacheConfiguration(_maxElements));
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

  public EhCacheDirectFactory algorithm(Algorithm v) {
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

  static class DelegatingEntryFactory extends MyCacheEntryFactory {

    Source source;

    DelegatingEntryFactory(Source source) {
      this.source = source;
    }

    @Override
    public Object createEntry(Object key) throws Exception {
      missCount++;
      return source.get((Integer) key);
    }

  }

  class MyBenchmarkCache extends BenchmarkCache<Integer, Integer> {

    CacheConfiguration config;
    MyCacheEntryFactory entryFactory;
    Ehcache cache;

    MyBenchmarkCache(Source _source, CacheConfiguration v) {
      this.config = v;
      Cache _testCache = new Cache(v);
      CACHE_MANAGER.addCache(_testCache);
      if (_source != null) {
        entryFactory = new DelegatingEntryFactory(_source);
      } else {
        entryFactory = new MyCacheEntryFactory();
      }
      cache = _testCache;
      cache.flush();
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
    public Integer get(Integer key) {
      Element e = cache.get(key);
      if (e == null) {
        put(key, key);
      }
      return key;
    }

    @Override
    public void destroy() {
      CACHE_MANAGER.removeCache(CACHE_NAME);
    }

    @Override
    public int getMissCount() {
      return entryFactory.missCount;
    }
  }

  public enum Algorithm { DEFAULT, CLOCK }

}
