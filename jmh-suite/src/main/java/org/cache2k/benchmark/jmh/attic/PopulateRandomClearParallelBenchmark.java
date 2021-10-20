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

import it.unimi.dsi.util.XoShiRo256StarStarRandom;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.jmh.HeapProfiler;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.ThreadParams;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Insert into the cache and clear at random intervals at average
 * every {@link #AVERAGE_CLEAR_INTERVAL} per thread.
 *
 * @see org.cache2k.benchmark.jmh.cacheSuite.PopulateParallelClearBenchmark
 */
@State(Scope.Benchmark)
public class PopulateRandomClearParallelBenchmark extends BenchmarkBase {

  public static final int ENTRY_COUNT = Integer.MAX_VALUE;
  public static final int AVERAGE_CLEAR_INTERVAL = 100_000;

  protected final AtomicInteger offset = new AtomicInteger(0);

  BenchmarkCache<Integer, Integer> cache;

  @Setup(Level.Iteration)
  public void setup() {
    cache = getFactory().create(Integer.class, Integer.class, ENTRY_COUNT);
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    HeapProfiler.recordAndClose(cache);
  }

  @State(Scope.Thread) @AuxCounters
  public static class ThreadState {
    int index;
    int limit;
    int nextClear;
    Random random;
    public long clearCount;

    @Setup(Level.Iteration)
    public void setup(PopulateRandomClearParallelBenchmark benchmark,
                      BenchmarkParams params,
                      ThreadParams threadParams) {
      int delta = Integer.MAX_VALUE / params.getThreads();
      index = delta * threadParams.getThreadIndex();
      limit = index + delta;
      random = new XoShiRo256StarStarRandom(index);
      nextClear = index + random.nextInt(AVERAGE_CLEAR_INTERVAL);
    }

  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState ts, BenchmarkParams p) {
    if (ts.index == ts.limit) {
      throw new RuntimeException("limit reached");
    }
    cache.put(ts.index, ts.index++);
    if (ts.index == ts.nextClear) {
      cache.clear();
      ts.clearCount++;
      ts.nextClear = ts.index + ts.random.nextInt(AVERAGE_CLEAR_INTERVAL);
    }
    return ts.index;
  }

}
