package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * thirdparty
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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
