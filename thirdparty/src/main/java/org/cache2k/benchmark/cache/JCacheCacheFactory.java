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

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import org.cache2k.Cache2kBuilder;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.ProductCacheFactory;
import org.cache2k.jcache.ExtendedMutableConfiguration;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
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
public class JCacheCacheFactory extends ProductCacheFactory {

  private String cacheName = "default";
  private String provider;

  CacheManager resolveCacheManager() {
    return
      javax.cache.Caching.getCachingProvider(provider).getCacheManager();
  }

  @Override
  public <K, V> BenchmarkCache<K, V> create(
    Class<K> keyType, Class<V> valueType, int capacity) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    String _cacheName = constructCacheName(capacity);
    CacheManager mgr = resolveCacheManager();
    if (mgr.getClass().getName().toString().contains("Eh107")) {
       CacheConfiguration<K, V> _eh107Configuration =
         CacheConfigurationBuilder.newCacheConfigurationBuilder(keyType, valueType,
          ResourcePoolsBuilder.heap(capacity)).build();
        c.cache = mgr.createCache(_cacheName,
          Eh107Configuration.fromEhcacheCacheConfiguration(_eh107Configuration));
    } else if (mgr.getClass().getName().toString().contains("cache2k")) {
      c.cache = mgr.createCache(_cacheName,
        ExtendedMutableConfiguration.of(
          Cache2kBuilder.of(keyType, valueType)
            .entryCapacity(capacity)));
    } else {
      c.cache = mgr.getCache(_cacheName);
    }
    if (c.cache == null) {
      throw new NullPointerException("No cache returned for name: " + _cacheName);
    }
    return c;
  }

  @Override
  public <K, V> BenchmarkCache<K, V> createLoadingCache(Class<K> keyType, Class<V> valueType,
    int capacity, BenchmarkCacheLoader<K, V> loader) {

    CacheLoader<K,V> l = new CacheLoader<K, V>() {
      @Override
      public Map<K, V> loadAll(Iterable<? extends K> keys) throws CacheLoaderException {
        throw new UnsupportedOperationException();
      }
      @Override
      public V load(K key) {
        return loader.load(key);

      }
    };
    String cacheName = constructCacheName(capacity);
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    CacheManager mgr = resolveCacheManager();
    if (mgr.getClass().getName().toString().contains("Eh107")) {
      CacheConfiguration<K, V> _eh107Configuration =
        CacheConfigurationBuilder.newCacheConfigurationBuilder(keyType, valueType,
          ResourcePoolsBuilder.heap(capacity))
          .withLoaderWriter(new CacheLoaderWriter<K, V>() {
            @Override
            public V load(K key) throws Exception {
              return loader.load(key);
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
      c.cache = mgr.createCache(cacheName,
        Eh107Configuration.fromEhcacheCacheConfiguration(_eh107Configuration));
    } else if (mgr.getClass().getName().toString().contains("cache2k")) {
      c.cache = mgr.createCache(cacheName,
        ExtendedMutableConfiguration.of(
          Cache2kBuilder.of(keyType, valueType)
            .loader(k -> loader.load(k))
            .entryCapacity(capacity)));
    } else if (mgr.getClass().getName().contains("caffeine")) {
      CaffeineConfiguration<K,V> cfg = new CaffeineConfiguration<>();
      cfg.setCacheLoaderFactory(new FactoryBuilder.SingletonFactory(l));
      cfg.setMaximumSize(OptionalLong.of(capacity));
      c.cache = mgr.createCache(cacheName, cfg);
    } else {
      c.cache = mgr.createCache(cacheName,
        new MutableConfiguration<>().setCacheLoaderFactory(
          new FactoryBuilder.SingletonFactory(l)
        ));
    }
    if (c.cache == null) {
      throw new NullPointerException("No cache returned for name: " + cacheName);
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

    private Cache<K, V> cache;

    @Override
    public V get(K key) {
      return cache.get(key);
    }

    @Override
    public void put(K key, V value) {
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

    @Override
    public void clear() {
      cache.clear();
    }
  }

}
