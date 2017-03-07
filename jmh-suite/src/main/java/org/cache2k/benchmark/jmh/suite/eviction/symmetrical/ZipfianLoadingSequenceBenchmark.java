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

import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

import static org.cache2k.benchmark.jmh.MiscResultRecorderProfiler.addCounterResult;
import static org.cache2k.benchmark.jmh.MiscResultRecorderProfiler.getCounterResult;
import static org.cache2k.benchmark.jmh.MiscResultRecorderProfiler.setResult;

/**
 * Penetrate loading cache with a Zipfian pattern with distribution sizes
 * (entry count times factor). The cache loader has a penalty by burning CPU cycles.
 *
 * <p>Integer objects are prepared in the keySpace array. The zipfian pattern is
 * prepared in the zipfianPattern array.
 * specified hitrate. Use un-looped random sequence with XorShift1024StarRandom.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class ZipfianLoadingSequenceBenchmark extends BenchmarkBase {

  public static final int ENTRY_COUNT = 100_000;
  public static final int PATTERN_COUNT = 25_000_000;

  public static final int MAX_FACTOR = 80;
  public static final int INTEGER_SPACE_COUNT = MAX_FACTOR * ENTRY_COUNT;

  @Param({"10", "20", "40", "80"})
  public int factor = 0;

  /**
   * Precomputed zipfian pattern
   */
  private int[] zipfianPattern;

  private final DataSource source = new DataSource();

  /**
   * Precompute the set of the needed integer objects.
   */
  private Integer[] keySpace;

  /** Use thread safe RPNG to give each thread state another seed. */
  private final static Random offsetSeed = new Random(1802);

  @State(Scope.Thread)
  public static class ThreadState {

    XorShift1024StarRandomGenerator randomGenerator = new XorShift1024StarRandomGenerator(offsetSeed.nextLong());
    long operationCount = 0;

    @TearDown(Level.Iteration)
    public void tearDown() {
      addCounterResult(
        "opCount", operationCount, "op", AggregationPolicy.AVG
      );
    }
  }

  LoadingBenchmarkCache<Integer, Integer> cache;

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    getsDestroyed = cache = getFactory().createLoadingCache(Integer.class, Integer.class, ENTRY_COUNT, source);
    keySpace = new Integer[INTEGER_SPACE_COUNT];
    for (int i = 0; i < INTEGER_SPACE_COUNT; i++) {
      keySpace[i] = i;
    }
    ZipfianPattern _generator = new ZipfianPattern(1802, ENTRY_COUNT * factor);
    zipfianPattern = new int[PATTERN_COUNT];
    for (int i = 0; i < PATTERN_COUNT; i++) {
      zipfianPattern[i] = _generator.next();
    }
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    addCounterResult(
      "missCount", source.missCount.longValue(), "miss", AggregationPolicy.AVG
    );
    double _missCount = getCounterResult("missCount");
    double _operations = getCounterResult("opCount");
    setResult("hitrate", 100.0 - _missCount * 100.0 / _operations, "percent", AggregationPolicy.AVG);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState) {
    threadState.operationCount++;
    int[] _zipfianPattern = zipfianPattern;
    Integer k = keySpace[_zipfianPattern[threadState.randomGenerator.nextInt(_zipfianPattern.length)]];
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
