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

import it.unimi.dsi.util.XoShiRo256StarStarRandom;
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

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class IterationBenchmark extends BenchmarkBase {

  @Param({"100000", "1000000"})
  public int entryCount = 100_000;

  @State(Scope.Thread)
  public static class ThreadState {

    Iterator<Integer> keyInterator;

    @Setup(Level.Iteration)
    public void setup() { keyInterator = null; }
  }

  BenchmarkCache<Integer, Integer> cache;

  @Setup
  public void setupBenchmark() {
    BenchmarkCacheFactory f = getFactory();
    cache = f.create(Integer.class, Integer.class, entryCount);
    Random random = new XoShiRo256StarStarRandom();
    for (int i = 0; i < entryCount * 2; i++) {
      Integer v = random.nextInt(entryCount * 2);
      cache.put(v, v);
    }
    String statString = cache.toString();
    System.out.println("Cache stats after seeding: " + statString);
    Cache2kMetricsRecorder.saveStatsAfterSetup(statString);
  }

  @Setup(Level.Iteration)
  public void setup() throws Exception {
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
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
    Iterator<Integer> it = threadState.keyInterator;
    if (it == null || !it.hasNext()) {
      threadState.keyInterator = it = cache.keys();
      if (!it.hasNext()) {
        throw new IllegalStateException("fresh iterator is expected to return always a element");
      }
    }
    return it.next();
  }

}
