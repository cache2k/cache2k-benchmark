package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

/*
 * #%L
 * Benchmarks: JMH suite.
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

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.results.AggregationPolicy;

import static org.cache2k.benchmark.jmh.MiscResultRecorderProfiler.*;

/**
 * @author Jens Wilke
 */
@State(Scope.Thread)
public class HitCountRecorder {

  private static final Object LOCK = new Object();

  public long hitCount;
  public long missCount;

  @TearDown(Level.Iteration)
  public void tearDown() {
    synchronized (LOCK) {
      addCounterResult(
        "hitCount", hitCount, "hit", AggregationPolicy.AVG
      );
      addCounterResult(
        "missCount", missCount, "miss", AggregationPolicy.AVG
      );
      addCounterResult(
        "opCount", hitCount + missCount, "op", AggregationPolicy.AVG
      );
      double _missCount = getCounterResult("missCount");
      double _operations = getCounterResult("opCount");
      setResult("hitrate", 100.0 - _missCount * 100.0 / _operations, "percent", AggregationPolicy.AVG);
    }
  }

}
