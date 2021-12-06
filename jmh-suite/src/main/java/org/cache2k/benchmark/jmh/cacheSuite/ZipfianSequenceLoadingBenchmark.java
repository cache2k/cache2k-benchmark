package org.cache2k.benchmark.jmh.cacheSuite;

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
import org.cache2k.benchmark.jmh.Cache2kMetricsRecorder;
import org.cache2k.benchmark.jmh.HeapProfiler;
import org.cache2k.benchmark.jmh.MiscResultRecorderProfiler;
import org.cache2k.benchmark.jmh.RequestRecorder;
import org.cache2k.benchmark.jmh.attic.ZipfianLoopingPrecomputedSequenceLoadingBenchmark;
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

  @Param({"110", "500"})
  public int percent = 0;

  @Param({"100000", "1000000"})
  public int entryCount = 100_000;

  public static final int READTHROUGH_OVERHEAD_TOKES = 789;

  private final DataLoader source = new DataLoader();

  /** Use thread safe RPNG to give each thread state another seed. */
  final Random offsetSeed = new Random(1802);

  @State(Scope.Thread)
  public static class ThreadState {

    ZipfianPattern pattern;

    @Setup(Level.Iteration)
    public void setup(ZipfianSequenceLoadingBenchmark benchmark) {
      pattern = new ZipfianPattern(benchmark.offsetSeed.nextLong(),
        calculateRange(benchmark.entryCount, benchmark.percent));
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
    int range = calculateRange(entryCount, percent);
    BenchmarkCacheFactory<?> f = getFactory();
    cache = f.createLoadingCache(Integer.class, Integer.class, entryCount, source);
    /*
       fill the cache completely, so memory is already expanded at maximum
       this way the benchmark runs on better steady state and jitter is reduced.
       we don't want to measure insert performance, but read + eviction

       TODO: Better remove that pre population, because it introduces a phase change.
        The cache sees single thread usage first
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
    RequestRecorder.recordMissCount(source.missCount.longValue());
    MiscResultRecorderProfiler.setValue("cacheSize", cache.getSize(), "entries");
    String statString = cache.toString();
    Cache2kMetricsRecorder.recordStatsAfterIteration(statString);
  }

  @TearDown()
  public void tearDownBenchmark() {
    HeapProfiler.keepReference(cache);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, RequestRecorder rec) {
    rec.requests++;
    Integer v = cache.get(threadState.pattern.next());
    return v;
  }

  public static class DataLoader extends BenchmarkCacheLoader<Integer, Integer> {

    public final LongAdder missCount = new LongAdder();

    /**
     * The loader increments the miss counter and  burns CPU via JMH's black hole
     * to have a relevant miss penalty.
     */
    @Override
    public Integer load(Integer key) {
      missCount.increment();
      Blackhole.consumeCPU(READTHROUGH_OVERHEAD_TOKES);
      return key * 2 + 11;
    }

  }

}
