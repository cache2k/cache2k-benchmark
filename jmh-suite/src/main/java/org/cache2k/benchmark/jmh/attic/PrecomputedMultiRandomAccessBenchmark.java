package org.cache2k.benchmark.jmh.attic;

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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.jmh.RequestRecorder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Random;

/**
 * Access the cache in a random pattern of 1M length. Each thread has a unique
 * random pattern. The cache has a capacity of 100k entries. The number of unique
 * keys in the random pattern is adjusted to the requested target hit rate, for
 * example for a target hit rate of 50% 200k different keys are used.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class PrecomputedMultiRandomAccessBenchmark extends BenchmarkBase {

  public static final int ENTRY_COUNT = 100_000;
  public static final int PATTERN_COUNT = 2_000_000;

  @Param({"30", "50", "80", "90", "95"})
  public int hitRate = 0;

  /**
   * Generate a deterministic sequence of seeds that we use for the
   * access sequences of each thread.
   */
  private final static Random seedGenerator = new Random(1802);

  @State(Scope.Thread)
  public static class ThreadState {
    Integer[] ints;
    long index = 0;

    /**
     * Initialize thread state with unique trace.
     */
    @Setup(Level.Iteration)
    public void setup(PrecomputedMultiRandomAccessBenchmark _parent) {
      Random random = new Random(seedGenerator.nextInt());
      ints = new Integer[PATTERN_COUNT];
      int _keySpace = (int) (ENTRY_COUNT * (100D / _parent.hitRate));
      for (int i = 0; i < PATTERN_COUNT; i++) {
        ints[i] = random.nextInt(_keySpace);
      }
    }
  }

  BenchmarkCache<Integer, Integer> cache;

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    cache = getFactory().create(ENTRY_COUNT);
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    RequestRecorder.updateHitRate();
    recordMemoryAndDestroy(cache);
    cache = null;
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, RequestRecorder rec) {
    int idx = (int) (threadState.index++ % PATTERN_COUNT);
    Integer k = threadState.ints[idx];
    Integer v = cache.get(k);
    rec.requests++;
    if (v == null) {
      cache.put(k, k);
      rec.misses++;
    }
    return idx;
  }

}
