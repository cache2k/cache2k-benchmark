package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.results.AggregationPolicy;

import static org.cache2k.benchmark.jmh.MiscResultRecorderProfiler.*;

/**
 * Thread state that counts hit, miss and total operations and adds the results to
 * the recorded metrics.
 *
 * <p>Its expected that the miss counter is incremented always. The hit counter
 * or the operations counter (total number of operations) may be incremented
 * alternatively.
 *
 * @author Jens Wilke
 */
@State(Scope.Thread)
public class HitCountRecorder {

  private static final Object LOCK = new Object();

  public long hitCount;
  public long missCount;
  public long opCount;

  @TearDown(Level.Iteration)
  public void tearDown() {
    synchronized (LOCK) {
      if (hitCount > 0) {
        addCounterResult("hitCount", hitCount, "hit", AggregationPolicy.AVG);
      }
      if (missCount > 0) {
        addCounterResult("missCount", missCount, "miss", AggregationPolicy.AVG);
      }
      if (opCount == 0) {
        opCount = hitCount + missCount;
      }
      addCounterResult("opCount", opCount, "op", AggregationPolicy.AVG);
      updateHitrate();
      hitCount = missCount = opCount = 0;
    }
  }

  public static void recordOpCount(long _opCount) {
    synchronized (LOCK) {
      addCounterResult(
        "opCount", _opCount, "op", AggregationPolicy.AVG
      );
      updateHitrate();
    }
  }

  public static void recordMissCount(long _missCount) {
    synchronized (LOCK) {
      addCounterResult(
        "missCount", _missCount, "op", AggregationPolicy.AVG
      );
      updateHitrate();
    }
  }

  private static void updateHitrate() {
    System.err.println("Thread " + Thread.currentThread());
    long _missCountSum = getCounterResult("missCount");
    long _opCountSum = getCounterResult("opCount");
    if (_opCountSum == 0L) {
      System.err.println("Skipping hitrate calculation opCountSum=0");
      return;
    }
    System.err.println("opCountSum " + _opCountSum);
    System.err.println("missCountSum " + _missCountSum);
    double _hitRate = 100.0 - _missCountSum * 100.0 / _opCountSum;
    System.err.println("hitRate: " + _hitRate);
    setResult("hitrate", _hitRate, "percent", AggregationPolicy.AVG);
  }

}
