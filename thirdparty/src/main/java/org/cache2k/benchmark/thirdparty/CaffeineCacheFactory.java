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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class CaffeineCacheFactory extends BenchmarkCacheFactory {

  private boolean sameThreadEviction = false;

  CaffeineCacheFactory sameThreadEviction(boolean f) {
    sameThreadEviction = f;
    return this;
  }

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.size = _maxElements;
    createCache(_maxElements, c);
    return c;
  }

  private void createCache(final int _maxElements, final MyBenchmarkCacheAdapter _adapter) {
    Caffeine b = Caffeine.newBuilder().maximumSize(_maxElements);
    if (sameThreadEviction) {
      b.executor(Runnable::run);
      b.initialCapacity(_maxElements);
    }
    if (withExpiry) {
      b.expireAfterWrite(5 * 60, TimeUnit.SECONDS);
    }
    _adapter.cache = b.build();
  }

  static class MyBenchmarkCacheAdapter extends BenchmarkCache<Integer, Integer> {

    int size;
    Cache<Integer, Integer> cache;

    @Override
    public int getCacheSize() {
      return size;
    }

    @Override
    public Integer getIfPresent(final Integer key) {
      return cache.getIfPresent(key);
    }

    @Override
    public void put(final Integer key, final Integer value) {
      cache.put(key, value);
    }

    @Override
    public void destroy() {
      cache.cleanUp();
    }

    @Override
    public String getStatistics() {
      return cache.toString();
    }

  }

}
