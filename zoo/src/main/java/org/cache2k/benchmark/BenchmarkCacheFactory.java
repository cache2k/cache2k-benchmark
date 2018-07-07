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
 * @author Jens Wilke; created: 2013-12-08
 */
public abstract class BenchmarkCacheFactory {

  protected boolean withExpiry;

  public final IntBenchmarkCache<Integer> create(int _maxElements) {
    return (IntBenchmarkCache<Integer>) create(Integer.class, Integer.class, _maxElements);
  }

  /**
   * Caches not supporting specialized versions for the key type override this method.
   */
  protected <K,V> BenchmarkCache<K, V> createUnspecialized(Class<K> _keyType, Class<V> _valueType, int _maxElements) {
    throw new UnsupportedOperationException();
  }

  public <K,V> BenchmarkCache<K, V> create(Class<K> _keyType, Class<V> _valueType, int _maxElements) {
    BenchmarkCache<K, V> c = createUnspecialized(_keyType, _valueType, _maxElements);
    if (_keyType == Integer.class) {
      return IntBenchmarkCache.wrap(c);
    }
    return c;
  }

  public <K,V> LoadingBenchmarkCache<K, V> createUnspecializedLoadingCache(
    Class<K> _keyType, Class<V> _valueType,
    int _maxElements, BenchmarkCacheSource<K,V> _source) {
    throw new UnsupportedOperationException();
  }

  public <K,V> LoadingBenchmarkCache<K, V> createLoadingCache(
    Class<K> _keyType, Class<V> _valueType,
    int _maxElements, BenchmarkCacheSource<K,V> _source) {
    LoadingBenchmarkCache<K,V> c = createUnspecializedLoadingCache(_keyType, _valueType, _maxElements, _source);
    if (_keyType == Integer.class) {
      return IntLoadingBenchmarkCache.wrap(c);
    }
    return c;
  }

  public BenchmarkCacheFactory withExpiry(boolean v) {
    withExpiry = v;
    return this;
  }

}
