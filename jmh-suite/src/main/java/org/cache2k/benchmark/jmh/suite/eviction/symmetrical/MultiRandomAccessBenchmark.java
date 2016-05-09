package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Access the cache in a random pattern of 1M length. Each thread has a unique
 * random pattern. The cache has a capacity of 100k entries. The number of unique
 * keys in the random pattern is adjusted to the requested target hit rate, for
 * example for a target hit rate of 50% 200k different keys are used.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class MultiRandomAccessBenchmark extends BenchmarkBase {

  public static final int ENTRY_COUNT = 100 * 1000;
  public static final int PATTERN_COUNT = 1000 * 1000;

  @Param({"1", "20", "50", "80"})
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
    public void setup(MultiRandomAccessBenchmark _parent) {
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
    getsDestroyed = cache = getFactory().create(ENTRY_COUNT);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, HitCountRecorder rec) {
    int idx = (int) (threadState.index++ % PATTERN_COUNT);
    Integer k = threadState.ints[idx];
    Integer v = cache.getIfPresent(k);
    if (v == null) {
      cache.put(k, k);
      rec.missCount++;
    } else {
      rec.hitCount++;
    }
    return idx;
  }

}
