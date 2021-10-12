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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.util.AccessPattern;
import org.cache2k.benchmark.util.RandomAccessPattern;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prepopulate cache with 100k entries and access it in a random pattern
 * with different hot rates.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class PrecomputedRandomSequenceCacheBenchmark extends BenchmarkBase {

  public static final int ENTRY_COUNT = 100_000;
  public static final int PATTERN_COUNT = 1_000_000;

  @Param({"20", "50", "80"})
  public int hitRate = 0;

  private final static AtomicInteger offset = new AtomicInteger(0);

  @State(Scope.Thread)
  public static class ThreadState {
    long index = offset.getAndAdd(PATTERN_COUNT / 16);
  }

  BenchmarkCache<Integer, Integer> cache;
  Integer[] ints;

  @Setup(Level.Trial)
  public void setupBenchmark() throws Exception {
    ints = new Integer[PATTERN_COUNT];
    AccessPattern _pattern =
      new RandomAccessPattern((int) (ENTRY_COUNT * (100D / hitRate)));
    for (int i = 0; i < PATTERN_COUNT; i++) {
      ints[i] = _pattern.next();
    }
  }

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
    Integer k = ints[idx];
    rec.requests++;
    Integer v = cache.get(k);
    if (v == null) {
      cache.put(k, k);
      rec.misses++;
    }
    return idx;
  }

}
