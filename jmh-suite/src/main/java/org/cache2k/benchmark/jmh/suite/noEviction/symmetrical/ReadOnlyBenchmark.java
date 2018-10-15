package org.cache2k.benchmark.jmh.suite.noEviction.symmetrical;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2018 headissue GmbH, Munich
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

import it.unimi.dsi.util.XorShift1024StarRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.jmh.suite.eviction.symmetrical.Cache2kMetricsRecorder;
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
 * with different miss rates. The main aim of this benchmark is to check
 * how different miss rations influence the throughput.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class ReadOnlyBenchmark extends BenchmarkBase {

  public static final int PATTERN_COUNT = 1000 * 1000;

  @Param({"100", "50", "33"})
  public int hitRate = 0;

  @Param({"100000"})
  public int entryCount = 100 * 1000;

  private final static AtomicInteger offset = new AtomicInteger(0);

  @State(Scope.Thread)
  public static class ThreadState {
    long index = offset.getAndAdd(PATTERN_COUNT / 16);
  }

  BenchmarkCache<Integer, Integer> cache;

  Integer[] ints;

  @Setup(Level.Trial)
  public void setup() throws Exception {
    int _SAFETY_ADD = 20000;
    cache = getFactory().createUnspecialized(entryCount + _SAFETY_ADD);
    Cache2kMetricsRecorder.saveStats(cache.toString());
    ints = new Integer[PATTERN_COUNT];
    RandomGenerator generator = new XorShift1024StarRandomGenerator(1802);
    int _keyRange = entryCount * 100 / hitRate;
    for (int i = 0; i < PATTERN_COUNT; i++) {
      ints[i] = generator.nextInt(_keyRange);
    }
    /*
     * First round of iteration that exceeds the entry count so the eviction
     * kicks in. This is needed for Caffeine, otherwise Caffeine does no bookkeeping
     * during the benchmark.
     */
    for (int i = 0; i < entryCount + _SAFETY_ADD * 5; i++) {
      cache.put(i, i);
    }
    /*
     * Make sure the cache contains at least the range [ 0 .. entryCount -1 ]
     */
    int missing = 0;
    int tries = 20;
    do {
      if (tries-- <= 0) {
        throw new IllegalStateException("cache does not store at least entryCount elements, missing: " + missing);
      }
      if (missing > 0) {
        Thread.sleep(123);
      }
      missing = 0;
      for (int i = 0; i < entryCount; i++) {
        if (cache.get(i) == null) {
          cache.put(i, i);
          if (cache.get(i) == null) {
            throw new IllegalStateException("cache does not store the value");
          }
          missing++;
        }
      }
    } while (missing > 0);

    if (_keyRange > entryCount) {
      throw new IllegalArgumentException("key range to big, check hitrate <= 100");
    }
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    Cache2kMetricsRecorder.recordStats(cache.toString());
    closeIfNeeded(cache);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long read(ThreadState threadState) {
    int idx = (int) (threadState.index++ % PATTERN_COUNT);
    return cache.get(ints[idx]);
  }

}
