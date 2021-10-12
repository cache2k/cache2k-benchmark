package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

/*
 * #%L
 * Benchmarks: JMH suite.
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

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import static org.cache2k.benchmark.jmh.MiscResultRecorderProfiler.*;

/**
 * Thread state that counts total cache requests and miss counter and adds the results to
 * the recorded metrics.
 *
 * <p>AuxCounters gives as a more precise normalized throughput for requests/s, since
 * it uses the actual measure time per thread.
 *
 * @author Jens Wilke
 */
@State(Scope.Thread) @AuxCounters()
public class RequestRecorder {

  static final String REQUESTS_METRIC = "requests";
  static final String MISSES_METRIC = "misses";
  static final String HITRATE_METRIC = "hitrate";

  public long misses;
  public long requests;
  public long bulkRequests;

  @Setup(Level.Iteration)
  public void setup() {
    misses = requests = bulkRequests = 0;
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    if (misses > 0) { addCounter(MISSES_METRIC, misses); }
    if (bulkRequests > 0) { addCounter("bulkRequests", bulkRequests); }
    addCounterWithThroughput(REQUESTS_METRIC, requests);
    updateHitRate();
  }

  /**
   * If not present at thread level, add the miss count at the end of the iteration.
   */
  public static void recordMissCount(long missCount) {
    addCounter("misses", missCount);
    updateHitRate();
  }

  public synchronized static void updateHitRate() {
    long missCount = getIterationCounterResult(MISSES_METRIC);
    long requestCount = getIterationCounterResult(REQUESTS_METRIC);
    if (requestCount == 0L) {
      return;
    }
    double hitRate = 100.0 - missCount * 100.0D / requestCount;
    setValue(HITRATE_METRIC, hitRate, "percent");
  }

}
