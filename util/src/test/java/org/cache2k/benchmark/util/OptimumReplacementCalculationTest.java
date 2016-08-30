package org.cache2k.benchmark.util;

/*
 * #%L
 * util
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

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jens Wilke; created: 2013-11-14
 */
public class OptimumReplacementCalculationTest {

  int calcHits(int _size, int[] _trace) {
    OptimumReplacementCalculation c = new OptimumReplacementCalculation(_size, _trace);
    return c.getHitCount();
  }

  @Test
  public void test3_3() {
    int v = calcHits(1, new int[] {1, 2, 3});
    assertEquals(0, v);
  }

  @Test
  public void test2_2() {
    int v = calcHits(1, new int[] {1, 1});
    assertEquals(1, v);
  }

  @Test
  public void test2_6() {
    int v = calcHits(1, new int[] {1, 1, 1, 0, 0, 0});
    assertEquals(4, v);
  }

  @Test
  public void test4_4() {
    int v = calcHits(1, new int[] {1, 2, 3, 2});
    assertEquals(0, v);
  }

  @Test
  public void test2_4() {
    int v = calcHits(1, new int[] {1, 1, 0, 1});
    assertEquals(1, v);
  }

  @Test
  public void test4_6() {
    int v = calcHits(2, new int[] {2, 3, 1, 3, 0, 1});
    assertEquals(2, v);
  }

  @Test
  public void test7_11() {
    int v = calcHits(3, new int[] {0, 4, 2, 6, 4, 1, 5, 0, 3, 1, 1});
    assertEquals(4, v);
  }

  @Test
  public void test3_6() {
    int v = calcHits(2, new int[] {1, 1, 1, 0, 0, 0, 2, 2, 2, 1, 0, 0});
    assertEquals(8, v);
  }

  @Test
  public void testCompareToStraightForward() throws Exception {
    final int[] _VALUE_RANGE = new int[]{44, 77, 111, 22, 777};
    final int[] _TRACE_SIZE = new int[]{987, 876, 3712, 555, 7897};
    for (int i = 0; i < _VALUE_RANGE.length; i++) {
      AccessTrace t =
        new AccessTrace(new RandomAccessPattern(_VALUE_RANGE[i]), _TRACE_SIZE[i]);
      assertEquals(optTraceHits(t.getArray(), _VALUE_RANGE[i]/2), t.getOptHitCount(_VALUE_RANGE[i]/2));
    }
  }

  @Test
  public void testOptCalcMiss1K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(1000));
    assertEquals(0, t.getOptHitCount(500));
  }

  @Test
  public void testOptCalcMiss10K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(10 * 1000));
    assertEquals(0, t.getOptHitCount(500));
  }

  @Test
  public void testOptCalcMiss50K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(10 * 1000));
    assertEquals(0, t.getOptHitCount(500));
  }

  @Test
  public void testOptCalcMiss100K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(100 * 1000));
    assertEquals(0, t.getOptHitCount(500));
  }

  @Test
  public void testOptCalcMiss200K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(200 * 1000));
    assertEquals(0, t.getOptHitCount(500));
  }

  @Test
  public void testUpDownTrace() {
    final AccessTrace upDownTrace = new AccessTrace(
      Patterns.loop(
        Patterns.concat(Patterns.sequence(1000), Patterns.revert(Patterns.sequence(1000))),
        1000));
    assertEquals(1000, upDownTrace.getValueCount());
    assertEquals(999, upDownTrace.getHighValue());
    assertEquals(2000 * 1000 , upDownTrace.getTraceLength());
    assertEquals(50, upDownTrace.getOptHitRate(500).getPercent());
    assertEquals(500, upDownTrace.getOptHitRate(500).get3digit());
    assertEquals(4998, upDownTrace.getOptHitRate(500).get4digit());
  }

  /**
   * Calculate hit percentage according to Beladys optimal algorithm.
   */
  static double optTrace(int[] _trace, int _cacheSize) {
    return optTraceHits(_trace, _cacheSize) * 1.0D / _trace.length;
  }

  /**
   * Calculate number of hits according to Beladys optimal algorithm.
   */
  static int optTraceHits(int[] _trace, int _cacheSize) {
    Set<Integer> _cache = new HashSet<>();
    int hit = 0;
    int miss = 0;
    for (int i = 0; i < _trace.length; i++) {
      int key = _trace[i];
      if (_cache.contains(key)) {
        hit++;
        continue;
      }
      miss++;
      if (_cache.size() == _cacheSize) {
        _cache.remove(findEvictKey(_trace, i, _cache));
      }
      _cache.add(key);
    }
    return hit;
  }

  /**
   * Find the key that should evicted that has a future use that is longest away
   * from the current position. This is really slow, than we do a nested loop
   * over the cache and over the rest of the trace.
   */
  static int findEvictKey(int[] _trace, int _currentPos, Set<Integer> _cache) {
    int _futurePos = 0;
    int _selectedValue = -1;
    for (int v : _cache) {
      int p = futurePosition(_trace, _currentPos, v);
      if (p > _futurePos) { _selectedValue = v; _futurePos = p; }
    }
    return _selectedValue;
  }

  /**
   * Next usage of the value in the trace.
   */
  static int futurePosition(int[] ia, int _currentPos, int v) {
    for (int i = _currentPos; i < ia.length; i++) {
      if (ia[i] == v) {
        return i;
      }
    }
    return Integer.MAX_VALUE;
  }

}
