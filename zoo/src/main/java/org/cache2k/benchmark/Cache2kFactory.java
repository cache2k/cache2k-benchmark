package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: implementation variants
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

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class Cache2kFactory extends BenchmarkCacheFactory {

  AtomicInteger counter = new AtomicInteger();
  boolean disableStatistics = true;

  @Override
  public BenchmarkCache<Integer, Integer> create(final int _maxElements) {
    Cache2kBuilder<Integer, Integer> b =
    new Cache2kBuilder<Integer, Integer>(){}
      .name("testCache-" + counter.incrementAndGet())
      .entryCapacity(_maxElements)
      .refreshAhead(false)
      .strictEviction(true);
    if (withExpiry) {
      b.expireAfterWrite(5 * 60, TimeUnit.SECONDS);
    } else {
      b.eternal(true);
    }
    if (disableStatistics) {
      b.disableStatistics(true);
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
