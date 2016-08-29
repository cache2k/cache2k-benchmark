/*********************************************************************************
 * Copyright 2016-present trivago GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **********************************************************************************/

package org.cache2k.benchmark.thirdparty;

import com.trivago.triava.tcache.TCacheFactory;
import com.trivago.triava.tcache.core.Builder;
import com.trivago.triava.tcache.eviction.Cache;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

/**
 * Factory for TCache
 *
 * @author Christian Esken
 */
public class TCache1Factory extends BenchmarkCacheFactory {

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.size = _maxElements;
    createCache(_maxElements, c);
    return c;
  }

  private void createCache(final int _maxElements, final MyBenchmarkCacheAdapter _adapter) {
	
	  Builder<Integer, Integer> b = TCacheFactory.standardFactory().builder();
	  b.setExpectedMapSize(_maxElements);

      /**
       * Set a cache name. For some reason, auto-assigning a name does not work properly in this JMH based test.
       * The issue is that the TCacheFactory.anonymousCacheId.incrementAndGet() returns 1 in all cases. This
       * makes no real sense, as that AtomicInteger variable is static. It may be related to JVM forking by JMH, but
       * it still seems odd. For now, just generate random Cache names, in the hope they will not clash.
       */
    String id = "tcache-" + new Random().nextInt();
    b.setId(id);

    if (withExpiry)
    {
      b.setMaxCacheTime(5 * 60);
      b.setMaxIdleTime(5 * 60);
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
      return cache.get(key);
    }

    @Override
    public void put(final Integer key, final Integer value) {
      cache.put(key, value);
    }

    @Override
    public void destroy() {
      cache.shutdown();
    }

    @Override
    public String getStatistics() {
      return cache.statistics().toString();
    }

  }

}
