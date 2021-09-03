package org.cache2k.benchmark.impls;

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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple, not thread safe cache based on the {@code LinkedHashMap}
 *
 * @author Jens Wilke
 * @see <a href="http://chriswu.me/blog/a-lru-cache-in-10-lines-of-java/"/>
 */
public class LinkedHashMapCache<K,V> extends LinkedHashMap<K,V> {

  private final int cacheSize;

  public LinkedHashMapCache(int cacheSize) {
    super(16, 0.75F, true);
    this.cacheSize = cacheSize;
  }

  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() >= cacheSize;
  }

  public int getCacheSize() {
    return cacheSize;
  }

}
