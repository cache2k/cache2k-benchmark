package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
 * %%
 * Copyright (C) 2013 - 2018 headissue GmbH, Munich
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

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.typesafe.config.ConfigException;
import org.cache2k.Cache2kBuilder;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheSource;
import org.cache2k.jcache.ExtendedMutableConfiguration;
import org.cache2k.jcache.JCacheConfiguration;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.core.EhcacheManager;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.spi.loaderwriter.BulkCacheLoadingException;
import org.ehcache.spi.loaderwriter.BulkCacheWritingException;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Factory for JCache based caches.
 *
 * @author Jens Wilke
 */
public class JCacheCacheFactory extends BenchmarkCacheFactory {

  private String cacheName = "default";
  private String provider;

  CacheManager resolveCacheManager() {
    return
      javax.cache.Caching.getCachingProvider(provider).getCacheManager();
  }

  @Override
  protected <K, V> BenchmarkCache<K, V> createSpecialized(
    final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.size = _maxElements;
    String _cacheName = constructCacheName(_maxElements);
    CacheManager mgr = resolveCacheManager();
    if (mgr.getClass().getName().toString().contains("Eh107")) {
       CacheConfiguration<K, V> _eh107Configuration =
         CacheConfigurationBuilder.newCacheConfigurationBuilder(_keyType, _valueType,
          ResourcePoolsBuilder.heap(_maxElements)).build();
        c.cache = mgr.createCache(_cacheName,
          Eh107Configuration.fromEhcacheCacheConfiguration(_eh107Configuration));
    } else if (mgr.getClass().getName().toString().contains("cache2k")) {
      c.cache = mgr.createCache(_cacheName,
        ExtendedMutableConfiguration.of(
          Cache2kBuilder.of(_keyType, _valueType)
            .entryCapacity(_maxElements)));
    } else {
      c.cache = mgr.getCache(_cacheName);
    }
    if (c.cache == null) {
      throw new NullPointerException("No cache returned for name: " + _cacheName);
    }
    return c;
  }

  @Override
  public <K, V> BenchmarkCache<K, V> createUnspecializedLoadingCache(
    final Class<K> _keyType, final Class<V> _valueType,
    final int _maxElements, final BenchmarkCacheSource<K, V> _source) {

    final CacheLoader<K,V> l = new CacheLoader<K, V>() {
      @Override
      public Map<K, V> loadAll(final Iterable<? extends K> keys) throws CacheLoaderException {
        throw new UnsupportedOperationException();
      }
      @Override
      public V load(final K key) {
        return _source.load(key);

      }
    };
    String _cacheName = constructCacheName(_maxElements);
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.size = _maxElements;
    CacheManager mgr = resolveCacheManager();
    if (mgr.getClass().getName().toString().contains("Eh107")) {
      CacheConfiguration<K, V> _eh107Configuration =
        CacheConfigurationBuilder.newCacheConfigurationBuilder(_keyType, _valueType,
          ResourcePoolsBuilder.heap(_maxElements))
          .withLoaderWriter(new CacheLoaderWriter<K, V>() {
            @Override
            public V load(final K _k) throws Exception {
              return _source.load(_k);
            }

            @Override
            public Map<K, V> loadAll(final Iterable<? extends K> _iterable) throws BulkCacheLoadingException, Exception {
              Map<K, V> m = new HashMap<>();
              for (K key : _iterable) {
                m.put(key, load(key));
              }
              return m;
            }

            @Override
            public void write(final K _k, final V _v) throws Exception {

            }

            @Override
            public void writeAll(final Iterable<? extends Map.Entry<? extends K, ? extends V>> _iterable) throws BulkCacheWritingException, Exception {

            }

            @Override
            public void delete(final K _k) throws Exception {

            }

            @Override
            public void deleteAll(final Iterable<? extends K> _iterable) throws BulkCacheWritingException, Exception {

            }
          })
          .build();
      c.cache = mgr.createCache(_cacheName,
        Eh107Configuration.fromEhcacheCacheConfiguration(_eh107Configuration));
    } else if (mgr.getClass().getName().toString().contains("cache2k")) {
      c.cache = mgr.createCache(_cacheName,
        ExtendedMutableConfiguration.of(
          Cache2kBuilder.of(_keyType, _valueType)
            .loader(k -> _source.load(k))
            .entryCapacity(_maxElements)));
    } else if (mgr.getClass().getName().contains("caffeine")) {
      CaffeineConfiguration<K,V> cfg = new CaffeineConfiguration<>();
      cfg.setCacheLoaderFactory(new FactoryBuilder.SingletonFactory(l));
      cfg.setMaximumSize(OptionalLong.of(_maxElements));
      c.cache = mgr.createCache(_cacheName, cfg);
    } else {
      c.cache = mgr.createCache(_cacheName,
        new MutableConfiguration<>().setCacheLoaderFactory(
          new FactoryBuilder.SingletonFactory(l)
        ));
    }
    if (c.cache == null) {
      throw new NullPointerException("No cache returned for name: " + _cacheName);
    }
    return c;
  }

  private String constructCacheName(final int _maxElements) {
    return cacheName + _maxElements + (withExpiry ? "withExpiry" : "");
  }

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(final String v) {
    cacheName = v;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(final String v) {
    provider = v;
  }

  static class MyBenchmarkCacheAdapter<K,V> extends BenchmarkCache<K, V> {

    int size;
    Cache<K, V> cache;

    @Override
    public int getCapacity() {
      return size;
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
      cache.close();
    }

    @Override
    public String toString() {
      return cache.toString();
    }

  }

}
