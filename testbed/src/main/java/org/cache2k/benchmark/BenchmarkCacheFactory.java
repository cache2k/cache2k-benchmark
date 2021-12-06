package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory creates cache backed by a real cache implementation and wraps it into a BenchmarkCache.
 *
 * @author Jens Wilke
 */
public abstract class BenchmarkCacheFactory<T extends EvictionTuning> extends AnyCacheFactory<T> {

  private final List<EvictionListener<?>> evictionListeners = new ArrayList<>();
  protected boolean withExpiry;
  protected boolean withTimeToIdle;
  protected boolean withStatistics;

  @SuppressWarnings("unchecked")
  public <K, V> BenchmarkCache<K, V> create(int capacity) {
    return (BenchmarkCache<K, V>) create(Object.class, Object.class, capacity);
  }

  public abstract <K, V> BenchmarkCache<K, V> create(
    Class<K> keyType, Class<V> valueType, int capacity);

  public <K,V> BenchmarkCache<K, V> createLoadingCache(
    Class<K> keyType, Class<V> valueType,
    int maxElements, BenchmarkCacheLoader<K,V> loader) {
    throw new UnsupportedOperationException();
  }

  public BenchmarkCacheFactory<T> withStatistics(boolean v) {
    withStatistics = v;
    return this;
  }

  public BenchmarkCacheFactory<T> withExpiry(boolean v) {
    withExpiry = v;
    return this;
  }

  public BenchmarkCacheFactory<T> withTimeToIdle(boolean v) {
    withTimeToIdle = v;
    return this;
  }

  public BenchmarkCacheFactory<T> withEvictionListener(EvictionListener<?> listener) {
    evictionListeners.add(listener);
    return this;
  }

  public Iterable<EvictionListener<?>> getEvictionListeners() {
    return Collections.unmodifiableList(evictionListeners);
  }

  @Override
  public String toString() {
    return "BenchmarkCacheFactory{" +
      "name='" + getName() + '\'' +
      ", withExpiry=" + withExpiry +
      '}';
  }

}
