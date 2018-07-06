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

import it.unimi.dsi.util.XorShift1024StarRandomGenerator;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.jmh.ForcedGcMemoryProfiler;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Random;

/**
 * Penetrate a 100k entry cache with a random pattern that produces a
 * specified hitrate. Use un-looped random sequence with XorShift1024StarRandom.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class RandomSequenceBenchmark extends BenchmarkBase {

  @Param({"50", "95"})
  public int hitRate = 0;

  @Param({"1000000"})
  public int entryCount = 100_000;

  /** Use thread safe RPNG to give each thread state another seed. */
  private final static Random offsetSeed = new Random(1802);

  private int range;

  @State(Scope.Thread)
  public static class ThreadState {
    XorShift1024StarRandomGenerator generator = new XorShift1024StarRandomGenerator(offsetSeed.nextLong());
  }

  BenchmarkCache<Integer, Integer> cache;

  @Setup
  public void setupBenchmark() {
    cache = getFactory().create(entryCount);
    range = (int) (entryCount * (100D / hitRate));
    /*
       fill the cache completely, so memory is already expanded at maximum
       this way the benchmark runs on better steady state and jitter is reduced.
       we don't want to measure insert performance, but read + eviction
     */
    XorShift1024StarRandomGenerator _generator = new XorShift1024StarRandomGenerator(1802);
    for (int i = 0; i < entryCount * 2; i++) {
      Integer v = _generator.nextInt(range);
      cache.put(v, v);
    }
    String _statString = cache.toString();
    System.out.println("Cache stats after seeding: " + _statString);
    Cache2kMetricsRecorder.saveStats(_statString);
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    ForcedGcMemoryProfiler.keepReference(this);
    String _statString = cache.toString();
    System.out.println(_statString);
    System.out.println("availableProcessors: " + Runtime.getRuntime().availableProcessors());
    Cache2kMetricsRecorder.recordStats(_statString);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, HitCountRecorder rec) {
    Integer k = threadState.generator.nextInt(range);
    Integer v = cache.getIfPresent(k);
    if (v == null) {
      cache.put(k, k);
      rec.missCount++;
    } else {
      rec.hitCount++;
    }
    return k;
  }

}
