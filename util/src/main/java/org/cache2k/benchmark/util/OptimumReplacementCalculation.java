package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
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

import java.util.TreeMap;

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

  private int size;
  private int hit;
  private int step = 0;
  private int[] trace;
  private int almostMax = Integer.MAX_VALUE;

  /**
   * Position of any value currently in the cache. This is the data structure
   * we fetch the value to be evicted from. pos2value.values() represents the
   * cache content.
   */
  private TreeMap<Integer, Integer> pos2value = new TreeMap<>();

  public OptimumReplacementCalculation(int _size, int[] _trace) {
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

  public int getHitCount() {
    return hit;
  }

}
