package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
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

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process a trace to calculate the percentage for the optimum
 * replacement strategy according to the Belady algorithm. The idea
 * is, that whenever a cache eviction is done, the element is chosen which
 * use is longest ahead or with no more use at all.
 *
 * <p/>The algorithm is quite fast. Instead of examining every cache
 * entry on eviction, we remember the position of the next occurrence
 * in a tree.
 *
 * @author Jens Wilke; created: 2013-10-16
 */
public class OptimumReplacementCalculation {

  private static Map<CacheKey, Long> trace2opt = new ConcurrentHashMap<>();

  public static long getCached(AccessTrace t, long cacheSize) {
    CacheKey k = new CacheKey(t, cacheSize);
    return trace2opt.computeIfAbsent(k,
      x -> new OptimumReplacementCalculation(k.cacheSize, k.trace).getHitCount());
  }

  private long size;
  private long hit;
  private int step;
  private int[] trace;
  private int almostMax = Integer.MAX_VALUE;

  /**
   * Position of any value currently in the cache. This is the data structure
   * we fetch the value to be evicted from. pos2value.values() represents the
   * cache content.
   */
  private TreeMap<Integer, Integer> pos2value = new TreeMap<>();

  public OptimumReplacementCalculation(long _size, AccessTrace t) {
    this(_size, t.getArray());
  }

  public OptimumReplacementCalculation(long _size, int[] _trace) {
    size = _size;
    trace = _trace;
    for (step = 0; step < trace.length;  step++) {
      step(trace[step]);
    }
  }

  void step(int v) {
    Integer val = pos2value.remove(step);
    if (val != null) {
      hit++;
    } else {
      if (size == pos2value.size()) {
        pos2value.pollLastEntry();
      }
    }
    int pos = findNextPosition(v);
    pos2value.put(pos, v);
  }

  int findNextPosition(int v) {
    int[] ia = trace;
    for (int i = step + 1; i < ia.length; i++) {
      if (ia[i] == v) {
        return i;
      }
    }
    return almostMax--;
  }

  public long getHitCount() {
    return hit;
  }

  private static class CacheKey {
    private AccessTrace trace;
    private long cacheSize;

    public CacheKey(final AccessTrace _trace, final long _cacheSize) {
      trace = _trace;
      cacheSize = _cacheSize;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CacheKey __cacheKey = (CacheKey) o;

      if (cacheSize != __cacheKey.cacheSize) return false;
      return trace.equals(__cacheKey.trace);

    }

    @Override
    public int hashCode() {
      int res = trace.hashCode();
      res = 31 * res + (int) (cacheSize ^ (cacheSize >>> 32));
      return res;
    }
  }

}
