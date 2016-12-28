package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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
import org.openjdk.jmh.results.AggregationPolicy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.cache2k.benchmark.jmh.MiscResultRecorderProfiler.addCounterResult;

/**
 * @author Jens Wilke
 */
public class LoopingPrecomputedZipfianSequenceLoadingBenchmark extends BenchmarkBase {

  public static final int ENTRY_COUNT = 100_000;
  public static final int PATTERN_COUNT = 2_000_000;

  private static final AtomicInteger offsetCount = new AtomicInteger();

  @Param({"10", "80"})
  public int factor = 0;

  private Integer[] pattern;

  private final ZipfianLoadingSequenceBenchmark.DataSource source = new ZipfianLoadingSequenceBenchmark.DataSource();

  @State(Scope.Thread)
  public static class ThreadState {
    long index = PATTERN_COUNT / 16 * offsetCount.getAndIncrement();
    @TearDown
    public void tearDown() {
      addCounterResult(
        "opCount", index, "op", AggregationPolicy.AVG
      );
    }
  }

  LoadingBenchmarkCache<Integer, Integer> cache;

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    getsDestroyed = cache = getFactory().createLoadingCache(Integer.class, Integer.class, ENTRY_COUNT, source);
    ZipfianPattern _generator = new ZipfianPattern(1802, ENTRY_COUNT * factor);
    pattern = new Integer[PATTERN_COUNT];
    for (int i = 0; i < PATTERN_COUNT; i++) {
      pattern[i] = _generator.next();
    }
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    addCounterResult(
      "missCount", source.missCount.longValue(), "miss", AggregationPolicy.AVG
    );
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState) {
    Integer k = pattern[(int) (threadState.index++ % PATTERN_COUNT)];
    Integer v = cache.get(k);
    return k + v;
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
