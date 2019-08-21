package org.cache2k.benchmarks.clockProPlus;

/*
 * #%L
 * Benchmarks: Clock-Pro+ and other eviction policies
 * %%
 * Copyright (C) 2018 - 2019 Cong Li, Intel Corporation
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

import org.cache2k.benchmark.EvictionTuning;
import org.cache2k.benchmark.SimulatorPolicy;
import org.cache2k.benchmark.SimulatorPolicyFactory;
import org.cache2k.benchmark.EvictionStatistics;

import java.lang.reflect.Constructor;

/**
 * Factory class to build a {@link SimulatorPolicy} with a eviction policy provided
 * by an implementation of {@link ISimpleCache} and additional tuning parameters.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("Duplicates")
public class SimpleCacheFactory<T extends EvictionTuning> extends SimulatorPolicyFactory<T> {

  public static SimpleCacheFactory of(Class<? extends ISimpleCache> cache) {
    return new SimpleCacheFactory().setCacheClass(cache);
  }

  private Class<? extends ISimpleCache> cacheClass;

  @Override
  public SimulatorPolicy create(final int capacity) {
    Constructor constructor = cacheClass.getConstructors()[0];
    ISimpleCache cache;
    try {
      if (constructor.getParameterTypes().length == 1) {
        cache = (ISimpleCache) constructor.newInstance(capacity);
      } else {
        cache = (ISimpleCache) constructor.newInstance(capacity, getTuning());
      }
    } catch (Exception ex) {
      throw new IllegalArgumentException(" " + cacheClass.getName(), ex);
    }
    return new Adapter(cache);
  }

  /**
   * Create a default tunable by obtaining the class from the constructor argument.
   */
  @SuppressWarnings("unchecked")
  @Override
  public T getDefaultTuning() {
    try {
      Constructor constructor = cacheClass.getConstructors()[0];
      if (constructor.getParameterTypes().length == 1) {
        return null;
      }
      if (constructor.getParameterTypes().length != 2) {
        throw new IllegalArgumentException("Cache implementation needs exactly one constructor.");
      }
      return (T) constructor.getParameterTypes()[1].newInstance();
    } catch (Exception ex) {
      throw new RuntimeException("Cannot create default tuning for: " + cacheClass.getName(), ex);
    }
  }

  public Class<? extends ISimpleCache> getCacheClass() {
    return cacheClass;
  }

  public SimpleCacheFactory setCacheClass(final Class<? extends ISimpleCache> v) {
    cacheClass = v;
    setNamePrefix(v.getSimpleName().toLowerCase() + "-cli");
    return this;
  }

  private static class Adapter implements SimulatorPolicy  {

    private ISimpleCache cache;
    private long missCount = 0;

    public Adapter(final ISimpleCache cache) {
      this.cache = cache;
    }

    @Override
    public void record(final Integer v) {
      if (!cache.request(v)) {
        missCount++;
      }
    }

    @Override
    public long getMissCount() {
      return missCount;
    }

    @Override
    public String toString() {
      return cache.toString();
    }

    @Override
    public EvictionStatistics getEvictionStatistics() {
      return cache.getEvictionStatistics();
    }
  }

}
