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

import java.util.HashMap;

/**
 * @author Jens Wilke
 */
public class HashMapFactory extends BenchmarkCacheFactory<EvictionTuning.None> {

  @Override
  public <K, V> BenchmarkCache<K, V> create(Class<K> keyType, Class<V> valueType, int capacity) {
    return new ConcurrentHashMapFactory.MyCache(new HashMap<K, V>(), capacity);
  }

}
