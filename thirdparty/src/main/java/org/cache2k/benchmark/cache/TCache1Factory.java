/*
 * Additional copyright:
 * Copyright 2016 trivago GmbH
 */

package org.cache2k.benchmark.cache;

/*
 * #%L
 * Benchmarks: third party products.
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
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

import com.trivago.triava.tcache.TCacheFactory;
import com.trivago.triava.tcache.core.Builder;
import com.trivago.triava.tcache.eviction.Cache;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.ProductCacheFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for TCache
 *
 * @author Christian Esken
 */
public class TCache1Factory extends ProductCacheFactory {
  AtomicInteger counter = new AtomicInteger();

  @Override
  public <K, V> BenchmarkCache<K, V> create(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
    TCacheFactory factory = TCacheFactory.standardFactory();
    Builder<K, V> b = factory.builder();
    b.setExpectedMapSize(_maxElements);

    /**
     * Set a cache name. For some reason, auto-assigning a name does not work properly in this JMH based test.
     * The issue is that the TCacheFactory.anonymousCacheId.incrementAndGet() returns 1 in all cases. This
     * makes no real sense, as that AtomicInteger variable is static. It may be related to JVM forking by JMH, but
     * it still seems odd. Following Cache2kFactory, use an AtomicInteger.
     */
    String id = "tcache-" + counter.incrementAndGet();
    b.setId(id);

    if (withExpiry) {
      b.setMaxCacheTime(5 * 60);
      b.setMaxIdleTime(5 * 60);
    }

    return new MyBenchmarkCacheAdapter<K,V>(b, _maxElements, factory);
  }

  static class MyBenchmarkCacheAdapter<K,V> extends BenchmarkCache<K, V> {

    final int size;
    final Cache<K, V> cache;
    final public TCacheFactory factory;

    public MyBenchmarkCacheAdapter(Builder<K, V> builder, int maxElements, TCacheFactory factory) {
      super();
      this.size = maxElements;
      this.factory = factory;
      this.cache = builder.build();
    }

    @Override
    public V get(final K key) {
      return cache.get(key);
    }

    @Override
    public void put(final K key, final V value) {
      cache.put(key, value);
    }

    @Override
    public void remove(final K key) {
      cache.remove(key);
    }

    @Override
    public void close() {
      factory.destroyCache(cache.id());
    }

    @Override
    public String toString() {
      return cache.statistics().toString();
    }

  }

}
