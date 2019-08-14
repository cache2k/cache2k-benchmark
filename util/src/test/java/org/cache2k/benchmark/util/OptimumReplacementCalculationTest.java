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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jens Wilke; created: 2013-11-14
 */
public class OptimumReplacementCalculationTest {

  long calcHits(int _size, int[] _trace) {
    OptimumReplacementCalculation c = new OptimumReplacementCalculation(_size, _trace);
    return c.getHitCount();
  }

  long calcHits(AccessTrace t, int _size) {
    return new OptimumReplacementCalculation(_size, t).getHitCount();
  }

  @Test
  public void test3_3() {
    long v = calcHits(1, new int[] {1, 2, 3});
    assertEquals(0, v);
  }

  @Test
  public void test2_2() {
    long v = calcHits(1, new int[] {1, 1});
    assertEquals(1, v);
  }

  @Test
  public void test2_6() {
    long v = calcHits(1, new int[] {1, 1, 1, 0, 0, 0});
    assertEquals(4, v);
  }

  @Test
  public void test4_4() {
    long v = calcHits(1, new int[] {1, 2, 3, 2});
    assertEquals(0, v);
  }

  @Test
  public void test2_4() {
    long v = calcHits(1, new int[] {1, 1, 0, 1});
    assertEquals(1, v);
  }

  @Test
  public void test4_6() {
    long v = calcHits(2, new int[] {2, 3, 1, 3, 0, 1});
    assertEquals(2, v);
  }

  @Test
  public void test7_11() {
    long v = calcHits(3, new int[] {0, 4, 2, 6, 4, 1, 5, 0, 3, 1, 1});
    assertEquals(4, v);
  }

  @Test
  public void test3_6() {
    long v = calcHits(2, new int[] {1, 1, 1, 0, 0, 0, 2, 2, 2, 1, 0, 0});
    assertEquals(8, v);
  }

  @Test
  public void testOptCalcMiss1K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(1000));
    assertEquals(0, calcHits(t, 500));
  }

  @Test
  public void testOptCalcMiss10K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(10 * 1000));
    assertEquals(0, calcHits(t, 500));
  }

  @Test
  public void testOptCalcMiss50K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(10 * 1000));
    assertEquals(0, calcHits(t, 500));
  }

  @Test
  public void testOptCalcMiss100K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(100 * 1000));
    assertEquals(0, calcHits(t, 500));
  }

  @Test
  public void testOptCalcMiss200K() {
    AccessTrace t = new AccessTrace(Patterns.sequence(200 * 1000));
    assertEquals(0, calcHits(t, 500));
  }

  @Test
  public void testUpDownTrace() {
    final AccessTrace upDownTrace = new AccessTrace(
      Patterns.loop(
        Patterns.concat(Patterns.sequence(1000), Patterns.revert(Patterns.sequence(1000))),
        1000));
    assertEquals(1000, upDownTrace.getValueCount());
    assertEquals(999, upDownTrace.getHighValue());
    assertEquals(2000 * 1000 , upDownTrace.getLength());

  }

}
