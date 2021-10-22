package org.cache2k.benchmark.jmh.cacheSuite;

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
import org.cache2k.benchmark.jmh.HeapProfiler;
import org.cache2k.benchmark.jmh.RequestRecorder;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.ThreadParams;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Insert new keys in multiple threads. After the double capacity count of inserts is reached
 * the cache is cleared. That means the benchmark is about pure inserting half of the time and
 * inserting with eviction of another half of the time.
 */
@State(Scope.Benchmark)
public class PopulateParallelClearBenchmark extends BenchmarkBase {

  @Param({"100000", "1000000"})
  public int entryCount = 100_000;

  /**
   * Clear after x percent of cache size inserts
   */
  @Param({"200"})
  public int clearPercent = 200;

  BenchmarkCache<Integer, Integer> cache;
  AtomicInteger clearArbiter = new AtomicInteger();

  @Setup
  public void setupBenchmark() {
    cache = getFactory().create(Integer.class, Integer.class, entryCount);
  }

  @SuppressWarnings("unchecked")
  @Setup(Level.Iteration)
  public void setup() {
    cache.clear();
    clearArbiter.set(0);
  }

  @TearDown
  public void tearDownBenchmark() {
    HeapProfiler.keepReference(cache);
  }

  @State(Scope.Thread) @AuxCounters
  public static class ThreadState {

    int startIndex;
    int index;
    int limit;
    int nextClear;
    int nextClearOffset;
    int clearCount;
    public long clearOpCounter;

    @Setup(Level.Iteration)
    public void setup(PopulateParallelClearBenchmark benchmark, BenchmarkParams params,
                      ThreadParams threadParams) {
      clearCount = 0;
      int delta = Integer.MAX_VALUE / params.getThreads();
      startIndex = index = delta * threadParams.getThreadIndex();
      limit = index + delta;
      nextClearOffset =
        benchmark.entryCount / params.getThreads() * benchmark.clearPercent / 100;
      nextClear = index + nextClearOffset;
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
      RequestRecorder.recordRequestCount(index - startIndex);
    }

  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState ts) {
    if (ts.index == ts.limit) {
      throw new RuntimeException("limit reached");
    }
    cache.put(ts.index, ts.index++);
    if (ts.index == ts.nextClear) {
      if (clearArbiter.compareAndSet(ts.clearCount++, ts.clearCount)) {
        cache.clear();
        ts.clearOpCounter++;
      }
      ts.nextClear = ts.nextClear + ts.nextClearOffset;
    }
    return ts.index;
  }

}
