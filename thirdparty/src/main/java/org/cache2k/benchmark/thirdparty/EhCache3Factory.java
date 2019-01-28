package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheSource;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.ResourceType;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class EhCache3Factory extends BenchmarkCacheFactory {

  static final org.ehcache.CacheManager MANAGER = CacheManagerBuilder.newCacheManagerBuilder().build(true);
  static final String CACHE_NAME = "testCache";

  @Override
  protected <K, V> BenchmarkCache<K, V> createSpecialized(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
    return new MyBenchmarkCache<K,V>(createCacheConfiguration(_keyType, _valueType, _maxElements));
  }

  protected <K,V> CacheConfiguration<K,V> createCacheConfiguration(final Class<K> _keyType, final Class<V> _valueType, int _maxElements) {
    return CacheConfigurationBuilder.newCacheConfigurationBuilder(_keyType, _valueType,
      ResourcePoolsBuilder.heap(_maxElements)).build();
  }

  @Override
  public <K, V> BenchmarkCache<K, V> createUnspecializedLoadingCache(final Class<K> _keyType, final Class<V> _valueType,
                                                                     final int _maxElements, final BenchmarkCacheSource<K, V> _source) {
    CacheLoaderWriter<K,V> lw = new CacheLoaderWriter<K, V>() {
      @Override
      public V load(final K key) throws Exception {
        return _source.load(key);
      }

      @Override
      public void write(final K key, final V value) throws Exception {

      }

      @Override
      public void delete(final K key) throws Exception {

      }
    };
    CacheConfiguration<K,V> cfg =
      CacheConfigurationBuilder.newCacheConfigurationBuilder(_keyType, _valueType,
        ResourcePoolsBuilder.heap(_maxElements))
        .withLoaderWriter(lw)
        .build();
    return new MyBenchmarkCache<K,V>(cfg);
  }

  class MyBenchmarkCache<K,V> extends BenchmarkCache<K, V> {

    CacheConfiguration config;
    org.ehcache.Cache<K,V> cache;

    MyBenchmarkCache(CacheConfiguration<K, V> cfg) {
      this.config = cfg;
      cache = MANAGER.createCache(CACHE_NAME, cfg);
    }

    @Override
    public int getCapacity() {
      return (int) config.getResourcePools().getPoolForResource(ResourceType.Core.HEAP).getSize();
    }

    @Override
    public V get(K key) {
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
      MANAGER.removeCache(CACHE_NAME);
    }

    @Override
    public String toString() {
      return cache.toString();
    }

  }

}
