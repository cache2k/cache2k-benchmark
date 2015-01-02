package org.cache2k.benchmark;

/*
 * #%L
 * cache2k-benchmark-zoo
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

import org.cache2k.Cache;
import org.cache2k.CacheBuilder;
import org.cache2k.CacheSource;
import org.cache2k.impl.CanCheckIntegrity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class Cache2kFactory extends BenchmarkCacheFactory {

  Class<?> implementation;

  @Override
  public BenchmarkCache<Integer, Integer> create(final int _maxElements) {
    final CountingDataSource<Integer, Integer> _source = new CountingDataSource<>();
    final Cache<Integer, Integer> c =
      CacheBuilder.newCache(Integer.class, Integer.class)
      .name("testCache")
      .implementation(implementation)
      .source(_source)
      .expirySecs(withExpiry ? 5 * 60 : Integer.MAX_VALUE)
      .maxSize(_maxElements)
      .backgroundRefresh(false)
      .build();
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
        if (c instanceof CanCheckIntegrity) {
          ((CanCheckIntegrity) c).checkIntegrity();
        }
      }

      @Override
      public Object getOriginalCache() {
        return c;
      }
    };
  }

  public Cache2kFactory implementation(Class<?> c) {
    implementation = c;
    return this;
  }

  /**
   * @author Jens Wilke; created: 2013-06-24
   */
  public static class CountingDataSource<K, T> implements CacheSource<K, T> {

      private AtomicInteger missCnt = new AtomicInteger();

      protected final void incrementMissCount() {
        missCnt.incrementAndGet();
      }

      public final int getMissCount() {
        return missCnt.intValue();
      }

      @Override
      public T get(K o) {
        incrementMissCount();
        return (T) o;
      }

  }
}
