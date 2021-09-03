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
import org.openjdk.jmh.infra.ThreadParams;

import java.util.concurrent.atomic.LongAdder;

/**
 * Penetrate loading cache with a Zipfian pattern with distribution sizes
 * (entry count times factor). The cache loader has a penalty by burning CPU cycles.
 *
 * <p>This implementation uses precalculated zipfian sequence. To avoid adaption of the
 * cache to the repeating sequence the sequence length is much longer than the cache size.
 * Each thread increments the sequence index by a unique prime number, so that each sequence
 * used by each thread is unique as well.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class PrecalculatedZipfianSequenceLoadingBenchmark extends BenchmarkBase {

  public static final int[] PRIMES =
    {31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89};

  @Param({"5"})
  public int factor = 0;

  @Param({"100000", "1000000"})
  public int entryCount = 100_000;

  @Param({"false"})
  public boolean expiry = false;

  private final DataLoader source = new DataLoader();

  @State(Scope.Thread)
  public static class ThreadState {

    int index = 0;
    int increment;

    @Setup
    public void setup(ThreadParams parms) {
      increment = PRIMES[parms.getThreadIndex()];
      System.out.println(parms + ", increment " + increment);
    }

  }

  BenchmarkCache<Integer, Integer> cache;
  Integer[] sequence;
  int sequenceLen;

  @Setup
  public void setupBenchmark() {
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
    int range = entryCount * factor;
    ZipfianPattern pattern = new ZipfianPattern(1802, range);
    sequenceLen = entryCount * 10;
    sequence = new Integer[sequenceLen];
    for (int i = 0; i < sequenceLen; i++) {
      sequence[i] = pattern.next();
    }
    for (int i = 0; i < entryCount; i++) {
      Integer v = sequence[i];
      cache.put(v, v);
    }
    String _statString = cache.toString();
    System.out.println("Cache stats after seeding: " + _statString);
    Cache2kMetricsRecorder.saveStatsAfterSetup(_statString);
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
    Cache2kMetricsRecorder.recordStatsAfterIteration(_statString);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, HitCountRecorder rec) {
    rec.opCount++;
    int idx = threadState.index = (threadState.index + threadState.increment) % sequenceLen;
    Integer v = cache.get(sequence[idx]);
    return v;
  }

  static class DataLoader extends BenchmarkCacheLoader<Integer, Integer> {

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
