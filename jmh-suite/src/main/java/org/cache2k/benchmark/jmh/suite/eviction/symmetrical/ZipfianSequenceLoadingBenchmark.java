package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

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

import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.BenchmarkCacheSource;
import org.cache2k.benchmark.LoadingBenchmarkCache;
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
 * <p>This implementation uses a separate zipfian pattern generator in each thread,
 * since the generation is not thread safe.
 *
 * <p>Generating the pattern during the benchmark run has some overhead, but when
 * the pattern is precomputed we cannot run the benchmark with bigger cache sizes.
 * This benchmark is about 40% slower then {@link ZipfianLoopingPrecomputedSequenceLoadingBenchmark}
 * with 100k entry count.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class ZipfianSequenceLoadingBenchmark extends BenchmarkBase {

  @Param({"5", "20"})
  public int factor = 0;

  @Param({"100000", "1000000"})
  public int entryCount = 100_000;

  @Param({"false"})
  public boolean expiry = false;

  private final DataSource source = new DataSource();

  /** Use thread safe RPNG to give each thread state another seed. */
  final Random offsetSeed = new Random(1802);

  @State(Scope.Thread)
  public static class ThreadState {

    ZipfianPattern pattern;

    @Setup(Level.Iteration)
    public void setup(ZipfianSequenceLoadingBenchmark _benchmark) {
      pattern = new ZipfianPattern(_benchmark.offsetSeed.nextLong(),
        _benchmark.entryCount * _benchmark.factor);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
      pattern = null;
    }
  }

  LoadingBenchmarkCache<Integer, Integer> cache;

  @Setup
  public void setupBenchmark() {
    int _range = entryCount * factor;
    BenchmarkCacheFactory f = getFactory();
    if (expiry) {
      f.withExpiry(true);
    }
    cache =
      f.createLoadingCache(Integer.class, Integer.class, entryCount, source);
    /*
       fill the cache completely, so memory is already expanded at maximum
       this way the benchmark runs on better steady state and jitter is reduced.
       we don't want to measure insert performance, but read + eviction
     */
    ZipfianPattern _generator = new ZipfianPattern(1802, _range);
    for (int i = 0; i < entryCount * 3; i++) {
      Integer v = _generator.next();
      cache.put(v, v);
    }
    String _statString = cache.toString();
    System.out.println("Cache stats after seeding: " + _statString);
    Cache2kMetricsRecorder.saveStats(_statString);
  }

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    source.missCount.reset();
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    HitCountRecorder.recordMissCount(source.missCount.longValue());
    ForcedGcMemoryProfiler.keepReference(this);
    String _statString = cache.toString();
    System.out.println(_statString);
    System.out.println("availableProcessors: " + Runtime.getRuntime().availableProcessors());
    Cache2kMetricsRecorder.recordStats(_statString);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, HitCountRecorder rec) {
    rec.opCount++;
    Integer v = cache.get(threadState.pattern.next());
    return v;
  }

  static class DataSource extends BenchmarkCacheSource<Integer, Integer> {

    LongAdder missCount = new LongAdder();

    /**
     * The loader increments the miss counter and  burns CPU via JMH's blackhole
     * to have a relevant miss penalty.
     */
    @Override
    public Integer load(final Integer key) {
      missCount.increment();
      Blackhole.consumeCPU(1000);
      return key * 2 + 11;
    }

  }

}
