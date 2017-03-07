package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: implementation variants
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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

  public abstract BenchmarkCache<Integer, Integer> create(int _maxElements);

  public <K,V> LoadingBenchmarkCache<K, V> createLoadingCache(
    Class<K> _keyType, Class<V> _valueType,
    int _maxElements, BenchmarkCacheSource<K,V> _source) {
    throw new UnsupportedOperationException();
  }

  public BenchmarkCacheFactory withExpiry(boolean v) {
    withExpiry = v;
    return this;
  }

}
