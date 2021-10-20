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
import org.cache2k.benchmark.BulkBenchmarkCacheLoader;
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.jmh.Cache2kMetricsRecorder;
import org.cache2k.benchmark.jmh.HeapProfiler;
import org.cache2k.benchmark.jmh.MiscResultRecorderProfiler;
import org.cache2k.benchmark.jmh.RequestRecorder;
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

  public final static int READTHROUGH_OVERHEAD_TOKES = 789;

  @Param({"110", "500"})
  public int percent = 0;

  @Param({"100000", "1000000"})
  public int entryCount = 100_000;

  /**
   * Make every nth request a bulk request.
   */
  public static final int BULK_STEP = 5;
  /**
   * Minimum number of entries requested in one bulk request
   */
  public static final int BULK_COUNT_MINIMUM = 10;
  /**
   * Maximum number of entries requested in one bulk request
   */
  public static final int BULK_COUNT_MAXIMUM = 300;
  public static final int BULK_RANGE = BULK_COUNT_MAXIMUM - BULK_COUNT_MINIMUM;

  @Param({"false"})
  public boolean expiry = false;

  private final DataLoader source = new DataLoader();

  /** Use thread safe RPNG to give each thread state another seed. */
  final Random offsetSeed = new Random(1802);

  @State(Scope.Thread)
  public static class ThreadState {

    ZipfianPattern keyPattern;
    XoShiRo256StarStarRandom bulkSizeRandom;
    int bulkCountDown;

    @Setup(Level.Iteration)
    public void setup(ZipfianSequenceBulkLoadingBenchmark benchmark) {
      bulkCountDown = BULK_STEP;
      final long items = benchmark.entryCount * 1L * benchmark.percent / 100;
      if (items > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Overflow, key range to high");
      }
      keyPattern = new ZipfianPattern(benchmark.offsetSeed.nextLong(), items);
      bulkSizeRandom = new XoShiRo256StarStarRandom(benchmark.offsetSeed.nextLong());
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
      keyPattern = null;
    }
  }

  BenchmarkCache<Integer, Integer> cache;

  @Setup
  public void setupBenchmark() {
    int range = entryCount * percent / 100;
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
  }

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    source.bulkMissCount.reset();
    source.bulkCallCount.reset();
    source.singleCallCount.reset();
    Cache2kMetricsRecorder.saveStatsAfterSetup(cache.toString());
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    MiscResultRecorderProfiler.addCounter("singleLoaderCalls", source.singleCallCount.longValue());
    MiscResultRecorderProfiler.addCounter("bulkLoaderCalls", source.bulkCallCount.longValue());
    MiscResultRecorderProfiler.setValue("cacheSize", cache.getSize(), "entries");
    RequestRecorder.recordMissCount(source.bulkMissCount.longValue() + source.singleCallCount.longValue());
    Cache2kMetricsRecorder.recordStatsAfterIteration(cache.toString());
    HeapProfiler.recordAndClose(cache);
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, RequestRecorder rec) {
    if (--threadState.bulkCountDown > 0) {
      rec.requests++;
      return cache.get(threadState.keyPattern.next());
    }
    threadState.bulkCountDown = BULK_STEP;
    int bulkCount = threadState.bulkSizeRandom.nextInt(BULK_RANGE) + BULK_COUNT_MINIMUM;
    Set<Integer> keySet = new HashSet<>(bulkCount);
    int base = threadState.keyPattern.next();
    for (int i = 0; i < bulkCount; i++) {
      keySet.add(base + i);
    }
    rec.bulkRequests++;
    rec.requests += bulkCount;
    Map<Integer, Integer> result = cache.getAll(keySet);
    return validateResult(keySet, result);
  }

  private int validateResult(Set<Integer> keySet, Map<Integer, Integer> result) {
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

    LongAdder bulkMissCount = new LongAdder();
    LongAdder bulkCallCount = new LongAdder();
    LongAdder singleCallCount = new LongAdder();

    /**
     * The loader increments the miss counter and  burns CPU via JMH's blackhole
     * to have a relevant miss penalty.
     */
    @Override
    public Integer load(Integer key) {
      singleCallCount.increment();
      Blackhole.consumeCPU(READTHROUGH_OVERHEAD_TOKES);
      return keyToValue(key);
    }

    @Override
    public Map<Integer, Integer> loadAll(Iterable<? extends Integer> keys) {
      bulkCallCount.increment();
      Map<Integer, Integer> result = new HashMap<>();
      int cnt = 0;
      for (Integer key : keys) {
        result.put(key, keyToValue(key));
        cnt++;
      }
      Blackhole.consumeCPU(READTHROUGH_OVERHEAD_TOKES);
      bulkMissCount.add(cnt);
      return result;
    }

  }

  private static int keyToValue(int key) {
    return key * 2 + 11;
  }

}
