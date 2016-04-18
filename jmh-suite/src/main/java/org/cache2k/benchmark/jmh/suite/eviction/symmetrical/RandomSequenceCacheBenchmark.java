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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prepopulate cache with 100k entries and access it in a random pattern
 * with different hot rates.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class RandomSequenceCacheBenchmark extends BenchmarkBase {

  public static final int ENTRY_COUNT = 100 * 1000;
  public static final int PATTERN_COUNT = 1000 * 1000;

  @Param({"20", "50", "80"})
  public int hitRate = 0;

  private final static AtomicInteger offset = new AtomicInteger(0);

  @State(Scope.Thread)
  public static class ThreadState {
    long index = offset.getAndAdd(PATTERN_COUNT / 16);
  }

  BenchmarkCache<Integer, Integer> cache;

  Integer[] ints;

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    getsDestroyed = cache = getFactory().create(ENTRY_COUNT);
    ints = new Integer[PATTERN_COUNT];
    AccessPattern _pattern =
      new RandomAccessPattern((int) (ENTRY_COUNT * (100D / hitRate)));
    for (int i = 0; i < PATTERN_COUNT; i++) {
      ints[i] = _pattern.next();
    }
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, HitCountRecorder rec) {
    int idx = (int) (threadState.index++ % PATTERN_COUNT);
    Integer k = ints[idx];
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
