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
import org.cache2k.benchmark.jmh.HeapProfiler;
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
 * The benchmark runs in single shot
 */
@State(Scope.Benchmark)
public class PopulateParallelOnceBenchmark extends BenchmarkBase {

  @Param({ "2000000", "4000000", "8000000"})
  public int entryCount = 1000 * 1000;
  protected final AtomicInteger offset = new AtomicInteger(0);

  BenchmarkCache<Integer, Integer> cache;

  @Setup(Level.Iteration)
  public void setup() {
    cache = getFactory().create(Integer.class, Integer.class, entryCount);
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    HeapProfiler.recordAndClose(cache);
  }

  @AuxCounters @State(Scope.Thread)
  public static class ThreadState {
    public long operations;
  }

  @Benchmark @BenchmarkMode(Mode.SingleShotTime)
  public long populateChunkInCache(ThreadState ts, BenchmarkParams p) {
    int chunkSize = entryCount / p.getThreads();
    int startIndex = offset.getAndAdd(chunkSize);
    int endIndex = startIndex + chunkSize;
    for (int i = startIndex; i < endIndex; i++) {
      cache.put(i, i);
    }
    ts.operations = chunkSize;
    return chunkSize;
  }

}
