package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Implementation and eviction variants
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
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
  protected <K, V> BenchmarkCache<K, V> createSpecialized(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
    final BaseCache bc;
    try {
      bc = (BaseCache) implementation.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    final Cache<K, V> c = bc;
    CacheConfig<K, V> cc = new CacheConfig<>();
    cc.setName("testCache-" + counter.incrementAndGet());
    cc.setExpirySeconds(withExpiry ? 2 * 60 : Integer.MAX_VALUE);
    cc.setEntryCapacity(_maxElements);
    cc.setBackgroundRefresh(false);
    cc.setKeepDataAfterExpired(false);
    bc.setCacheConfig(cc);
    bc.init();
    return new BenchmarkCache<K, V>() {

      @Override
      public int getCapacity() {
        return _maxElements;
      }

      @Override
      public V get(K key) {
        return c.peek(key);
      }

      @Override
      public void put(K key, V value) {
        c.put(key, value);
      }

      @Override
      public void remove(final K key) {
        c.remove(key);
      }

      @Override
      public void close() {
        c.destroy();
      }

      @Override
      public String toString() {
        return c.toString();
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

}
