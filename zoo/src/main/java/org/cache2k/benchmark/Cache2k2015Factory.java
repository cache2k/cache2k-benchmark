package org.cache2k.benchmark;

/*
 * #%L
 * zoo
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

import org.cache2k.benchmark.impl2015.BaseCache;
import org.cache2k.benchmark.impl2015.Cache;
import org.cache2k.benchmark.impl2015.CacheConfig;
import org.cache2k.benchmark.impl2015.ClockProPlus64Cache;
import org.cache2k.benchmark.impl2015.ClockProPlusCache;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class Cache2k2015Factory extends BenchmarkCacheFactory {

  Class<?> implementation =  "64".equals(System.getProperty("sun.arch.data.model"))
          ? ClockProPlus64Cache.class : ClockProPlusCache.class;

  AtomicInteger counter = new AtomicInteger();

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    return this.create(null, _maxElements);
  }

  @Override
  public BenchmarkCache<Integer, Integer> create(Source s, final int _maxElements) {
    CountingDataSource<Integer, Integer> _usedSource;
    if (s == null) {
      _usedSource = new CountingDataSource<>();
    } else {
      _usedSource = new DelegatingSource(s);
    }

    final CountingDataSource<Integer, Integer> _source = _usedSource;
    final BaseCache bc;
    try {
      bc = (BaseCache) implementation.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    final Cache<Integer, Integer> c = bc;
    bc.setSource(_source);
    CacheConfig<Integer, Integer> cc = new CacheConfig<>();
    cc.setName("testCache-" + counter.incrementAndGet());
    cc.setExpirySeconds(withExpiry ? 5 * 60 : Integer.MAX_VALUE);
    cc.setEntryCapacity(_maxElements);
    cc.setBackgroundRefresh(false);
    bc.setCacheConfig(cc);
    bc.init();
    return new BenchmarkCache<Integer, Integer>() {

      @Override
      public int getCacheSize() {
        return _maxElements;
      }

      @Override
      public Integer get(Integer key) {
        return c.get(key);
      }

      @Override
      public Integer getIfPresent(Integer key) {
        return c.peek(key);
      }

      @Override
      public void put(Integer key, Integer value) {
        c.put(key, value);
      }

      @Override
      public void destroy() {
        c.destroy();
      }

      @Override
      public String getStatistics() {
        return c.toString();
      }

      @Override
      public int getMissCount() {
        return _source.getMissCount();
      }

      @Override
      public void checkIntegrity() {
        if (c instanceof BaseCache) {
          ((BaseCache) c).checkIntegrity();
        }
      }

      @Override
      public Object getOriginalCache() {
        return c;
      }
    };
  }

  public Cache2k2015Factory implementation(Class<?> c) {
    implementation = c;
    return this;
  }

  /**
   * @author Jens Wilke; created: 2013-06-24
   */
  public static class CountingDataSource<K, T> implements org.cache2k.benchmark.impl2015.CacheSource<K, T> {

    private int missCnt;

    protected final void incrementMissCount() {
      missCnt++;
    }

    public final int getMissCount() {
      return missCnt;
    }

    @Override
    public T get(K o) {
      incrementMissCount();
      return (T) o;
    }

  }

  public static class DelegatingSource extends CountingDataSource<Integer, Integer> {

    Source source;

    public DelegatingSource(Source source) {
      this.source = source;
    }

    public Integer get(Integer v) {
      incrementMissCount();
      return source.get(v);
    }

  }

}
