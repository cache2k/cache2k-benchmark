package org.cache2k.benchmark.zoo;

/*
 * #%L
 * cache2k-benchmark-zoo
 * %%
 * Copyright (C) 2013 headissue GmbH, Munich
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.cache2k.CacheSource;
import org.cache2k.benchmark.BenchmarkCache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * Cache implementation just with a HashMap. Eviction is just done randomly.
 * No expiry. No parallel access allowed. This implementation extends the
 * BenchmarkCache directly which saves some indirection like in the case
 * of a real cache. Main purpose is to have a speed reference of almost a
 * HashMap.get() in case of a cache hit.
 *
 * @author Jens Wilke; created: 2013-11-24
 */
public class HashMapBenchmarkCache<K,T> extends BenchmarkCache<K, T> {

  Random random = new Random(1802);
  int maxElements;
  int hitCnt;
  int missCnt;
  int evictCnt;
  HashMap<K,T> map = new HashMap<>();
  CacheSource<K, T> cacheSource;

  public HashMapBenchmarkCache(CacheSource<K, T> _cacheSource, int _maxElements) {
    if (_maxElements <= 0) {
      throw new IllegalArgumentException("Maximum size must be greater 0");
    }
    if (_cacheSource == null) {
      throw new NullPointerException("Please specify a source");
    }
    this.maxElements = _maxElements;
    this.cacheSource = _cacheSource;
  }

  @Override
  public int getCacheSize() {
    return maxElements;
  }

  @Override
  public T get(K key) {
    T v = map.get(key);
    if (v != null) {
      hitCnt++;
      return v;
    }
    missCnt++;
    if (map.size() >= maxElements) {
      randomlyEvictEntry();
    }
    try {
      v = cacheSource.get(key);
    } catch (Throwable t) {
      throw new RuntimeException("never happens in testing", t);
    }
    map.put(key, v);
    return v;
  }

  private void randomlyEvictEntry() {
    evictCnt++;
    Iterator<K> it = map.keySet().iterator();
    int cnt = random.nextInt(map.size());
    while (cnt-- > 0) { it.hasNext(); it.next(); }
    it.hasNext();
    map.remove(it.next());
  }

  @Override
  public void destroy() {
  }

  @Override
  public String getStatistics() {
    return "HashMapBenchmarkCache(size=" + map.size() +
      ", usageCnt=" + (missCnt + hitCnt) +
      ", hitCnt=" + hitCnt +
      ", missCnt=" + missCnt +
      ", evictCnt=" + evictCnt + ")";
  }

  @Override
  public int getMissCount() {
    throw new UnsupportedOperationException();
  }

}
