package org.cache2k.benchmark;

/*
 * #%L
 * cache2k-benchmark-zoo
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Use a {@link ConcurrentHashMap} as cache implementation. No eviction is done. Intended
 * for benchmarks when no eviction is needed, meaning, the cache size is larger then the
 * data set.
 *
 * @author Jens Wilke
 */
public class ConcurrentHashMapFactory extends BenchmarkCacheFactory {

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    return new MyCache(new ConcurrentHashMap<Integer,Integer>(), _maxElements, null);
  }

  @Override
  public BenchmarkCache<Integer, Integer> create(Source s, int _maxElements) {
    return new MyCache(new ConcurrentHashMap<Integer,Integer>(), _maxElements, s);
  }

  static class MyCache extends BenchmarkCache<Integer, Integer> {

    int maxElements;
    Map<Integer, Integer> map;
    BenchmarkCacheFactory.Source source;

    public MyCache(Map<Integer, Integer> map, int maxElements, Source source) {
      this.map = map;
      this.maxElements = maxElements;
      this.source = source;
    }

    @Override
    public void destroy() {
      getMissCount();
      map = null;
    }

    @Override
    public void put(Integer key, Integer value) {
      map.put(key, value);
    }

    @Override
    public Integer getIfPresent(Integer key) {
      return map.get(key);
    }

    /**
     * Simulate read through by calling source.
     */
    @Override
    public Integer get(Integer key) {
      Integer v = map.get(key);
      if (v == null) {
        v = source.get(key);
        map.put(key, v);
      }
      return v;
    }

    @Override
    public int getCacheSize() {
      return map.size();
    }

    @Override
    public int getMissCount() {
      if (map.size() > maxElements) {
        throw new IllegalArgumentException("only relevant for benchmarking with 100% hit rate");
      }
      return 0;
    }
  }

}
