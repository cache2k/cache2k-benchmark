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

import java.util.Collections;
import java.util.Map;

/**
 * Bulk version of cache loader
 *
 * @author Jens Wilke
 */
public abstract class BulkBenchmarkCacheLoader<K, V> extends BenchmarkCacheLoader<K, V> {

  public V load(K key) {
    return loadAll(Collections.singleton(key)).get(key);
  }

  /**
   * Bulk load.
   */
  public abstract Map<K, V> loadAll(Iterable<? extends K> keys);

}
