package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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
import org.cache2k.benchmark.BenchmarkCacheSource;
import org.cache2k.benchmark.LoadingBenchmarkCache;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.util.ZipfianPattern;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Benchmark of a loading cache with penalty on a zipfian sequence.
 *
 * <p>The pattern is precomputed, each thread has its own random generator and
 * accesses the pattern at a different random offset for each operation. This yields about 4.5M op/s
 * for Caffeine. Problems: Memory consumption makes it hard to determine the real occupied
 * memory. The needed pattern will get too big when we increase the cache size.
 *
 * <p>Best choice is to generate the pattern on the fly, see {@link ZipfianSequenceLoadingBenchmark}.
 *
 * @author Jens Wilke
 * @see ZipfianSequenceLoadingBenchmark
 * @see ZipfianLoopingPrecomputedSequenceLoadingBenchmark
 */
public class ZipfianHoppingPrecomputedSequenceLoadingBenchmark extends BenchmarkBase {

  public static final int PATTERN_COUNT = 2_000_000;

  private static final AtomicInteger offsetCount = new AtomicInteger();

  @Param({"10", "80"})
  public int factor = 0;

  @Param({"100000"})
  public int entryCount = 100_000;

  final static Random randomSeed = new Random(1802);

  private Integer[] pattern;

  private final ZipfianSequenceLoadingBenchmark.DataSource source = new ZipfianSequenceLoadingBenchmark.DataSource();

  @State(Scope.Thread)
  public static class ThreadState {

    RandomGenerator generator = new XorShift1024StarRandomGenerator(randomSeed.nextLong());

  }

  LoadingBenchmarkCache<Integer, Integer> cache;

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    getsDestroyed = cache = getFactory().createLoadingCache(Integer.class, Integer.class, entryCount, source);
    ZipfianPattern _generator = new ZipfianPattern(1802, entryCount * factor);
    pattern = new Integer[PATTERN_COUNT];
    for (int i = 0; i < PATTERN_COUNT; i++) {
      pattern[i] = _generator.next();
    }
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    HitCountRecorder.recordMissCount(source.missCount.longValue());
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState _threadState, HitCountRecorder _recorder) {
    _recorder.opCount++;
    Integer k = pattern[_threadState.generator.nextInt(PATTERN_COUNT)];
    Integer v = cache.get(k);
    return k + v;
  }

}
