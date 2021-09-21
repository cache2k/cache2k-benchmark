package org.cache2k.benchmark.jmh.suite.noEviction.symmetrical;

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
import org.cache2k.benchmark.jmh.suite.eviction.symmetrical.ZipfianSequenceLoadingBenchmark;
import org.cache2k.benchmark.util.ZipfianPattern;
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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Populate a cache with in parallel with multiple threads.
 */
@State(Scope.Benchmark)
public class PopulateParallelBenchmark extends BenchmarkBase {

  @Param({"2147483647"})
  public int entryCount = 1000 * 1000;
  protected final AtomicInteger offset = new AtomicInteger(0);

  BenchmarkCache<Integer, Integer> cache;

  @Setup(Level.Iteration)
  public void setup() {
    cache = getFactory().create(Integer.class, Integer.class, entryCount);
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    recordMemoryAndDestroy(cache);
  }

  @State(Scope.Thread)
  public static class ThreadState {
    public int index;
    public int limit;

    @Setup(Level.Iteration)
    public void setup(PopulateParallelBenchmark benchmark, BenchmarkParams params) {
      int delta = Integer.MAX_VALUE / params.getThreads();
      index = benchmark.offset.getAndAdd(delta);
      limit = index + delta;
    }

  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState ts, BenchmarkParams p) {
    if (ts.index == ts.limit) {
      throw new RuntimeException("limit reached");
    }
    cache.put(ts.index, ts.index++);
    return ts.index;
  }

}
