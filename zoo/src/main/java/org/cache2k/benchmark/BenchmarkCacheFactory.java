package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: implementation variants
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

/**
 * @author Jens Wilke
 */
public abstract class BenchmarkCacheFactory {

  protected boolean withExpiry;

  public final IntBenchmarkCache<Integer> create(int _maxElements) {
    return createWithIntKey(Integer.class, _maxElements);
  }

  /**
   * Create a cache that is capable to store any kind of object.
   */
  public final <K, V> BenchmarkCache<K, V> createUnspecialized(int _maxElements) {
    return (BenchmarkCache<K, V> ) createSpecialized(Object.class, Object.class, _maxElements);
  }

  public <V> IntBenchmarkCache<V> createWithIntKey(Class<V> _valueType, int _maxElements) {
    BenchmarkCache<Integer, V> c = createSpecialized(Integer.class, _valueType, _maxElements);
    if (c instanceof IntBenchmarkCache) {
      return (IntBenchmarkCache<V>) c;
    }
    return IntBenchmarkCache.wrap(c);
  }

  public <K,V> BenchmarkCache<K, V> create(Class<K> _keyType, Class<V> _valueType, int _maxElements) {
    BenchmarkCache<K, V> c = createSpecialized(_keyType, _valueType, _maxElements);
    return c;
  }

  public <K,V> BenchmarkCache<K, V> createUnspecializedLoadingCache(
    Class<K> _keyType, Class<V> _valueType,
    int _maxElements, BenchmarkCacheSource<K,V> _source) {
    throw new UnsupportedOperationException();
  }

  public <K,V> BenchmarkCache<K, V> createLoadingCache(
    Class<K> _keyType, Class<V> _valueType,
    int _maxElements, BenchmarkCacheSource<K,V> _source) {
    BenchmarkCache<K,V> c = createUnspecializedLoadingCache(_keyType, _valueType, _maxElements, _source);
    if (_keyType == Integer.class) {
      return  (BenchmarkCache<K, V>) IntBenchmarkCache.wrap((BenchmarkCache<Integer, V>) c);
    }
    return c;
  }

  public BenchmarkCacheFactory withExpiry(boolean v) {
    withExpiry = v;
    return this;
  }

  /**
   * Creates a benchmark cache or its subclass, depending on the type information.
   * Passes on type information to the cache implementation, if that is supported.
   */
  protected <K,V> BenchmarkCache<K, V> createSpecialized(Class<K> _keyType, Class<V> _valueType, int _maxElements) {
    throw new UnsupportedOperationException();
  }

}
