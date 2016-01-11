package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * cache2k-benchmark-thirdparty
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class GuavaCacheFactory extends BenchmarkCacheFactory {

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.loader = new MyCacheLoader();
    c.size = _maxElements;
    CacheBuilder cb =
      CacheBuilder.newBuilder()
        .maximumSize(_maxElements);
    if (withExpiry) {
      cb.expireAfterWrite(5 * 60, TimeUnit.SECONDS);
    }
    c.cache = cb.build(c.loader);
    return c;
  }

  @Override
  public BenchmarkCache<Integer, Integer> create(Source s, int _maxElements) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.loader = new MyCacheLoaderWithSource(s);
    c.size = _maxElements;
    CacheBuilder cb =
      CacheBuilder.newBuilder()
        .maximumSize(_maxElements);
    if (withExpiry) {
      cb.expireAfterWrite(5 * 60, TimeUnit.SECONDS);
    }
    c.cache = cb.build(c.loader);
    return c;
  }

  static class MyCacheLoader extends CacheLoader<Integer,Integer> {

    int missCnt;

    @Override
    public Integer load(Integer key) throws Exception {
      missCnt++;
      return key;
    }
  }

  static class MyCacheLoaderWithSource extends  MyCacheLoader {

    Source source;

    MyCacheLoaderWithSource(Source source) {
      this.source = source;
    }

    @Override
    public Integer load(Integer key) throws Exception {
      missCnt++;
      return source.get(key);
    }
  }

  static class MyBenchmarkCacheAdapter extends BenchmarkCache<Integer, Integer> {

    MyCacheLoader loader;
    int size;
    LoadingCache<Integer, Integer> cache;

    @Override
    public int getCacheSize() {
      return size;
    }

    @Override
    public Integer get(Integer key) {
      try {
        return cache.get(key);
      } catch (ExecutionException e) {
        return null;
      }
    }

    @Override
    public void destroy() {
      cache.cleanUp();
    }

    @Override
    public int getMissCount() {
      return loader.missCnt;
    }

  }

}
