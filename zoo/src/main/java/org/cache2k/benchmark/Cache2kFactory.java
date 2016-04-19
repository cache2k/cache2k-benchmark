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

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheBuilder;
import org.cache2k.integration.CacheLoader;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class Cache2kFactory extends BenchmarkCacheFactory {

  AtomicInteger counter = new AtomicInteger();

  @Override
  public BenchmarkCache<Integer, Integer> create(final int _maxElements) {
    CacheBuilder<Integer, Integer> b =
    CacheBuilder.newCache(Integer.class, Integer.class)
      .name("testCache-" + counter.incrementAndGet())
      .expiryDuration(withExpiry ? 5 * 60 : Integer.MAX_VALUE, TimeUnit.SECONDS)
      .entryCapacity(_maxElements)
      .refreshAhead(false);
    if (withExpiry) {
      b.expiryDuration(5 * 60, TimeUnit.SECONDS);
    } else {
      b.eternal(true);
    }

    final Cache<Integer, Integer> c = b.build();
    return new BenchmarkCache<Integer, Integer>() {

      @Override
      public int getCacheSize() {
        return _maxElements;
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
      public void checkIntegrity() {
        c.toString();
      }

      @Override
      public Object getOriginalCache() {
        return c;
      }
    };

  }

}
