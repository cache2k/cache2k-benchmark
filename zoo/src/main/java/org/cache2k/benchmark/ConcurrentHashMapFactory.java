package org.cache2k.benchmark;

/*
 * #%L
 * zoo
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use a {@link ConcurrentHashMap} as cache implementation. No eviction is done. Intended
 * for benchmarks when no eviction is needed, meaning, the cache size is larger then the
 * data set.
 *
 * @author Jens Wilke
 */
public class ConcurrentHashMapFactory extends BenchmarkCacheFactory {

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    return new MyCache(new ConcurrentHashMap<Integer,Integer>(), _maxElements);
  }

  static class MyCache extends BenchmarkCache<Integer, Integer> {

    int maxElements;
    Map<Integer, Integer> map;

    public MyCache(Map<Integer, Integer> map, int maxElements) {
      this.map = map;
      this.maxElements = maxElements;
    }

    @Override
    public void destroy() {
      map = null;
    }

    @Override
    public void put(Integer key, Integer value) {
      map.put(key, value);
    }

    @Override
    public Integer getIfPresent(Integer key) {
      return map.get(key);
    }

    @Override
    public int getCacheSize() {
      return map.size();
    }

    @Override
    public String getStatistics() {
      return "mapimpl=" + map.getClass().getName();
    }
  }

}
