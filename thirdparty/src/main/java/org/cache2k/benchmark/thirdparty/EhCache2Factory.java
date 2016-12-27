package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheSource;
import org.cache2k.benchmark.LoadingBenchmarkCache;

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
    MyBenchmarkCache c = new MyBenchmarkCache();
    c.cache = new Cache(createCacheConfiguration(_maxElements));
    getManager().addCache(c.cache);
    c.size = _maxElements;
    return c;
  }

  @Override
  public <K, V> LoadingBenchmarkCache<K, V> createLoadingCache(
    final Class<K> _keyType, final Class<V> _valueType,
    final int _maxElements, final BenchmarkCacheSource<K, V> _source) {
    MyLoadingBenchmarkCache c = new MyLoadingBenchmarkCache();
    Ehcache ehc = new Cache(createCacheConfiguration(_maxElements));
    getManager().addCache(ehc);
    int _stripes = Runtime.getRuntime().availableProcessors();
    _stripes =  1 << (32 - Integer.numberOfLeadingZeros(_stripes - 1));
    c.cache = new SelfPopulatingCache(ehc,
      _stripes,
      new CacheEntryFactory() {
        @Override
        public Object createEntry(final Object key) throws Exception {
          return _source.load((K) key);
        }
    });
    c.size = _maxElements;
    return c;
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

    int size;
    Ehcache cache;

    @Override
    public int getCacheSize() {
      return size;
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
    public void close() {
      CacheManager.getInstance().removeCache("testCache");
    }

  }

  class MyLoadingBenchmarkCache<K, V> extends LoadingBenchmarkCache<K, V> {

    int size;
    Ehcache cache;

    @Override
    public int getCacheSize() {
      return size;
    }

    @Override
    public V get(final K key) {
      Element e = cache.get(key);
      if (e != null) {
        return (V) e.getObjectValue();
      }
      return null;
    }

    @Override
    public void put(final K key, final V value) {
      cache.put(new Element(key, value));
    }

    @Override
    public void close() {
      CacheManager.getInstance().removeCache("testCache");
    }

  }

  public enum Algorithm { DEFAULT, CLOCK }

}
