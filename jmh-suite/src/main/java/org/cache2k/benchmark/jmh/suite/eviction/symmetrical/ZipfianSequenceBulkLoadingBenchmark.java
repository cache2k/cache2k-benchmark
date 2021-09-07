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
import org.cache2k.benchmark.BulkBenchmarkCacheLoader;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class ZipfianSequenceBulkLoadingBenchmark extends BenchmarkBase {

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

    ZipfianPattern keyPattern;
    ZipfianPattern bulkPattern;

    @Setup(Level.Iteration)
    public void setup(ZipfianSequenceBulkLoadingBenchmark benchmark) {
      keyPattern = new ZipfianPattern(benchmark.offsetSeed.nextLong(),
        benchmark.entryCount * benchmark.factor);
      bulkPattern = new ZipfianPattern(benchmark.offsetSeed.nextLong(), 100);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
      keyPattern = null;
    }
  }

  BenchmarkCache<Integer, Integer> cache;

  @Setup
  public void setupBenchmark() {
    int range = entryCount * factor;
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
      cache.put(v, keyToValue(v));
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
    HitCountRecorder.recordBulkLoadCount(source.bulkCount.longValue());
    ForcedGcMemoryProfiler.keepReference(this);
    String statString = cache.toString();
    System.out.println(statString);
    System.out.println("availableProcessors: " + Runtime.getRuntime().availableProcessors());
    Cache2kMetricsRecorder.recordStatsAfterIteration(statString);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, HitCountRecorder rec) {
    int bulkCount = threadState.bulkPattern.next();
    if (bulkCount < 50) {
      rec.opCount++;
      Integer v = cache.get(threadState.keyPattern.next());
      return v;
    }
    Set<Integer> keySet = new HashSet<>(bulkCount);
    int base = threadState.keyPattern.next();
    for (int i = 0; i < bulkCount; i++) {
      keySet.add(base + i);
    }
    rec.bulkOpCount++;
    rec.opCount += bulkCount;
    Map<Integer, Integer> result = cache.getAll(keySet);
    if (result.size() != keySet.size()) {
      throw new AssertionError("result map size mismatch expected=" + keySet.size() + ", actual=" + result.size());
    }
    int sum = 0;
    for (Map.Entry<Integer, Integer> entry : result.entrySet()) {
      if (!keySet.contains(entry.getKey())) {
        throw new AssertionError("unexpected key: " + entry.getKey());
      }
      if (entry.getValue() != keyToValue(entry.getKey())) {
        throw new AssertionError("unexpected value in result key=" + entry.getKey() + ", value=" + entry.getValue());
      }
      sum += entry.getValue();
    }
    return sum;
  }

  static class DataLoader extends BulkBenchmarkCacheLoader<Integer, Integer> {

    LongAdder missCount = new LongAdder();
    LongAdder bulkCount = new LongAdder();

    /**
     * The loader increments the miss counter and  burns CPU via JMH's blackhole
     * to have a relevant miss penalty.
     */
    @Override
    public Integer load(Integer key) {
      missCount.increment();
      Blackhole.consumeCPU(1234);
      return keyToValue(key);
    }

    @Override
    public Map<Integer, Integer> loadAll(Iterable<? extends Integer> keys) {
      bulkCount.increment();
      Map<Integer, Integer> result = new HashMap<>();
      int cnt = 0;
      for (Integer key : keys) {
        result.put(key, keyToValue(key));
        cnt++;
      }
      Blackhole.consumeCPU(56789 + cnt * 1234);
      missCount.add(cnt);
      return result;
    }

  }

  private static int keyToValue(int key) {
    return key * 2 + 11;
  }

}
