package org.cache2k.benchmark.util;

/*
 * #%L
 * util
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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
