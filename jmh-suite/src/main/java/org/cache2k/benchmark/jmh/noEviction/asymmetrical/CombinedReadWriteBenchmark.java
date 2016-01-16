package org.cache2k.benchmark.jmh.noEviction.asymmetrical;

/*
 * #%L
 * Cache benchmark suite based on JMH.
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.util.AccessPattern;
import org.cache2k.benchmark.util.ScrambledZipfianPattern;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Three benchmarks. "ro" doing reads only in 8 threads, "rw" doing reads and writes
 * in 6 and 2 threads, "wo" doing writes in 8 threads. The cache is populated in advance
 * with the test data set. No eviction and no inserts happen during the benchmark time.
 * The test data size is 11k, the cache size 32k.
 *
 * <p>This benchmark is almost identical to the one in caffeine.</p>
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

  @Param("DEFAULT")
  String cacheFactory;

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    getsDestroyed = cache = getFactory().create(SIZE * 2);
    ints = new Integer[SIZE];
    AccessPattern _pattern = new ScrambledZipfianPattern(ITEMS);
    for (int i = 0; i < SIZE; i++) {
      ints[i] = _pattern.next();
      cache.put(ints[i], i);
    }
  }

  @Benchmark @Group("readOnly") @GroupThreads(8)
  public Integer readOnly(ThreadState threadState) {
    return cache.getIfPresent(ints[threadState.index++ & MASK]);
  }

  @Benchmark @Group("writeOnly") @GroupThreads(8)
  public void writeOnly(ThreadState threadState) {
    cache.put(ints[threadState.index++ & MASK], 0);
  }

  @Benchmark @Group("readWrite") @GroupThreads(6)
  public Integer readWrite_get(ThreadState threadState) {
    return cache.getIfPresent(ints[threadState.index++ & MASK]);
  }

  @Benchmark @Group("readWrite") @GroupThreads(2)
  public void readWrite_put(ThreadState threadState) {
    cache.put(ints[threadState.index++ & MASK], 0);
  }

}
