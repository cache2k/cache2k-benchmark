package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

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
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.jmh.ForcedGcMemoryProfiler;
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
import java.util.concurrent.atomic.LongAdder;

/**
 * Penetrate loading cache with a Zipfian pattern with distribution sizes
 * (entry count times factor). The cache loader has a penalty by burning CPU cycles.
 *
 * <p>This implementation uses a separate Zipfian pattern generator in each thread,
 * since the generation is not thread safe.
 *
 * <p>Generating the pattern during the benchmark run has some overhead, but when
 * the pattern is precomputed we cannot run the benchmark with huge cache sizes, since
 * the precomputed pattern needs to be bigger than the cache since to avoid the
 * cache is benefiting from repetition. This benchmark is about 40% slower then
 * {@link ZipfianLoopingPrecomputedSequenceLoadingBenchmark} with 100k entry count.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class ZipfianSequenceLoadingBenchmark extends BenchmarkBase {

  @Param({"5", "20"})
  public int factor = 0;

  @Param({"100000", "1000000" , "10000000"})
  public int entryCount = 100_000;

  @Param({"false"})
  public boolean expiry = false;

  private final DataLoader source = new DataLoader();

  /** Use thread safe RPNG to give each thread state another seed. */
  final Random offsetSeed = new Random(1802);

  @State(Scope.Thread)
  public static class ThreadState {

    ZipfianPattern pattern;

    @Setup(Level.Iteration)
    public void setup(ZipfianSequenceLoadingBenchmark benchmark) {
      pattern = new ZipfianPattern(benchmark.offsetSeed.nextLong(),
        calculateRange(benchmark.entryCount, benchmark.factor));
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
      pattern = null;
    }
  }

  BenchmarkCache<Integer, Integer> cache;

  static int calculateRange(int entryCount, int factor) {
    return (int) (entryCount * (factor / 100.0));
  }

  @Setup
  public void setupBenchmark() {
    int range = calculateRange(entryCount, factor);
    BenchmarkCacheFactory f = getFactory();
    if (expiry) {
      f.withExpiry(true);
    }
    cache = f.createLoadingCache(Integer.class, Integer.class, entryCount, source);
    /*
       fill the cache completely, so memory is already expanded at maximum
       this way the benchmark runs on better steady state and jitter is reduced.
       we don't want to measure insert performance, but read + eviction
     */
    ZipfianPattern generator = new ZipfianPattern(1802, range);
    for (int i = 0; i < entryCount * 3; i++) {
      Integer v = generator.next();
      cache.put(v, v);
    }
    String statString = cache.toString();
    System.out.println("Cache stats after seeding: " + statString);
    Cache2kMetricsRecorder.saveStatsAfterSetup(statString);
  }

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    source.missCount.reset();
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    HitCountRecorder.recordMissCount(source.missCount.longValue());
    ForcedGcMemoryProfiler.keepReference(this);
    String statString = cache.toString();
    System.out.println(statString);
    System.out.println("availableProcessors: " + Runtime.getRuntime().availableProcessors());
    Cache2kMetricsRecorder.recordStatsAfterIteration(statString);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, HitCountRecorder rec) {
    rec.opCount++;
    Integer v = cache.get(threadState.pattern.next());
    return v;
  }

  static class DataLoader extends BenchmarkCacheLoader<Integer, Integer> {

    LongAdder missCount = new LongAdder();

    /**
     * The loader increments the miss counter and  burns CPU via JMH's blackhole
     * to have a relevant miss penalty.
     */
    @Override
    public Integer load(Integer key) {
      missCount.increment();
      Blackhole.consumeCPU(200);
      return key * 2 + 11;
    }

  }

}
