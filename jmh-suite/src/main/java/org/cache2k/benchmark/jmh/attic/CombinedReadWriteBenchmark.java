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
import org.cache2k.benchmark.util.AccessPattern;
import org.cache2k.benchmark.util.ScrambledZipfianPattern;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Three benchmarks. "ro" doing reads only in 16 threads, "rw" doing reads and writes
 * in 8 and 4 threads, "wo" doing writes in 8 threads. The cache is populated in advance
 * with the test data set. No eviction and no inserts happen during the benchmark time.
 * The test data size is 11k, the cache size 32k.
 *
 * <p>This benchmark is almost identical to the one in caffeine.
 */
@State(Scope.Group)
public class CombinedReadWriteBenchmark extends BenchmarkBase {

  private static AtomicInteger offset = new AtomicInteger(0);
  private static final int SIZE = (2 << 14);
  private static final int MASK = SIZE - 1;
  private static final int ITEMS = SIZE / 3;

  @State(Scope.Thread)
  public static class ThreadState {
    int index = offset.getAndAdd(SIZE / 16);
  }

  BenchmarkCache <Integer, Integer> cache;

  Integer[] ints;

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    cache = getFactory().create(SIZE * 2);
    ints = new Integer[SIZE];
    AccessPattern _pattern = new ScrambledZipfianPattern(1802, ITEMS);
    for (int i = 0; i < SIZE; i++) {
      ints[i] = _pattern.next();
      cache.put(ints[i], i);
    }
  }

  @Benchmark @Group("readOnly") @GroupThreads(4) @BenchmarkMode(Mode.Throughput)
  public Integer readOnly(ThreadState threadState) {
    return cache.get(ints[threadState.index++ & MASK]);
  }

  @Benchmark @Group("writeOnly") @GroupThreads(4) @BenchmarkMode(Mode.Throughput)
  public void writeOnly(ThreadState threadState) {
    cache.put(ints[threadState.index++ & MASK], 0);
  }

  @Benchmark @Group("readWrite") @GroupThreads(3) @BenchmarkMode(Mode.Throughput)
  public Integer readWrite_get(ThreadState threadState) {
    return cache.get(ints[threadState.index++ & MASK]);
  }

  @Benchmark @Group("readWrite") @GroupThreads(1) @BenchmarkMode(Mode.Throughput)
  public void readWrite_put(ThreadState threadState) {
    cache.put(ints[threadState.index++ & MASK], 0);
  }

}
