package org.cache2k.benchmark;

/*
 * #%L
 * zoo
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;

import static org.junit.Assert.*;

import org.cache2k.benchmark.util.AccessTrace;
import org.cache2k.benchmark.util.DistAccessPattern;
import org.cache2k.benchmark.util.Patterns;
import org.cache2k.benchmark.util.RandomAccessPattern;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * All cache benchmarks in one class. There are three different types
 * of methods: The test* methods check for some basic cache behaviour
 * to ensure sanity, the benchmarkXy methods do a speed measurement,
 * the benchmarkXy_NNN do a efficiency test on a given trace.
 */
@SuppressWarnings(value={"unchecked", "unused"})
@BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 2)
public class BenchmarkCollection extends TracesAndTestsCollection {

  public static final boolean SKIP_MULTI_THREAD = true;

  public static final int TRACE_LENGTH = 3 * 1000 * 1000;

  public static final AccessTrace traceRandomForSize1Replacement =
    new AccessTrace(new RandomAccessPattern(10), 1000);

  public static final AccessTrace trace1001misses =
    new AccessTrace(
      Patterns.sequence(1000),
      Patterns.sequence(1000),
      Patterns.sequence(1000, 1001));

  static final AccessTrace mostlyHitTrace =
    new AccessTrace(new Patterns.InterleavedSequence(0, 500, 1, 0, TRACE_LENGTH / 500));

  @Test
  public void benchmarkHits() {
    runBenchmark(mostlyHitTrace, 500);
  }

  @Test
  public void benchmarkHits2000() {
    runBenchmark(mostlyHitTrace, 2000);
  }

  static final AccessTrace allMissTrace =
    new AccessTrace(Patterns.sequence(TRACE_LENGTH))
      .setOptHitCount(500, 0)
      .setRandomHitCount(500, 0)
      .setOptHitCount(50000, 0)
      .setRandomHitCount(50000, 0)
      .setOptHitCount(500000, 0)
      .setRandomHitCount(500000, 0)
      .setOptHitCount(5000000, 0)
      .setRandomHitCount(5000000, 0)
      .setOptHitCount(5000, 0)
      .setRandomHitCount(5000, 0);

  @Test
  public void benchmarkMiss() {
    BenchmarkCache<Integer, Integer> c = freshCache(500);
    runBenchmark(c, allMissTrace);
    assertEquals("allways miss", c.getMissCount(), allMissTrace.getTraceLength());
    logHitRate(c, allMissTrace, c.getMissCount());
    c.destroy();
  }

  @Test
  public void benchmarkMiss_5000() {
    BenchmarkCache<Integer, Integer> c = freshCache(5000);
    runBenchmark(c, allMissTrace);
    assertEquals("allways miss", c.getMissCount(), allMissTrace.getTraceLength());
    logHitRate(c, allMissTrace, c.getMissCount());
    c.destroy();
  }

  @Test
  public void benchmarkMiss_50000() {
    BenchmarkCache<Integer, Integer> c = freshCache(50000);
    runBenchmark(c, allMissTrace);
    assertEquals("allways miss", c.getMissCount(), allMissTrace.getTraceLength());
    logHitRate(c, allMissTrace, c.getMissCount());
    c.destroy();
  }

  @Test
  public void benchmarkMiss_500000() {
    BenchmarkCache<Integer, Integer> c = freshCache(500000);
    runBenchmark(c, allMissTrace);
    assertEquals("always miss", c.getMissCount(), allMissTrace.getTraceLength());
    logHitRate(c, allMissTrace, c.getMissCount());
    c.destroy();
  }

  public static final AccessTrace randomTrace =
    new AccessTrace(new RandomAccessPattern(1000), TRACE_LENGTH);

  @Test
  public void benchmarkRandom()  throws Exception {
    runBenchmark(randomTrace, 500);
  }


  static final AccessTrace effective90Trace =
    new AccessTrace(new DistAccessPattern(1000), TRACE_LENGTH);

  class MultiThreadSource extends BenchmarkCacheFactory.Source {

    @Override
    public int get(int v) {
      Random random = ThreadLocalRandom.current();
      int sum = v;
      for (int i = 0; i < 1000; i++) {
        sum += random.nextInt();
      }
      return sum;
    }
  }

  @Test
  public void benchmarkEff90() throws Exception {
    runBenchmark(effective90Trace, 500);
  }

  @Test
  public void benchmarkEff95Threads1() throws Exception {
    runMultiThreadBenchmark(new MultiThreadSource(), 1, effective95Trace, 500);
  }

  @Test
  public void benchmarkEff95Threads2() throws Exception {
    runMultiThreadBenchmark(new MultiThreadSource(), 2, effective95Trace, 500);
  }

  @Test
  public void benchmarkEff95Threads4() throws Exception {
    if (SKIP_MULTI_THREAD) { return; }
    runMultiThreadBenchmark(new MultiThreadSource(), 4, effective95Trace, 500);
  }

  @Test
  public void benchmarkEff95Threads6() throws Exception {
    if (SKIP_MULTI_THREAD) { return; }
    runMultiThreadBenchmark(new MultiThreadSource(), 6, effective95Trace, 500);
  }

  @Test
  public void benchmarkEff95Threads8() throws Exception {
    if (SKIP_MULTI_THREAD) { return; }
    runMultiThreadBenchmark(new MultiThreadSource(), 8, effective95Trace, 500);
  }

  static final AccessTrace effective95Trace =
    new AccessTrace(new DistAccessPattern(900), TRACE_LENGTH);

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkEff95() throws Exception {
    runBenchmark(effective95Trace, 500);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom_100() {
    runBenchmark(randomTrace, 100);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom_200() {
    runBenchmark(randomTrace, 200);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom_350() {
    runBenchmark(randomTrace, 350);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom_500() {
    runBenchmark(randomTrace, 500);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom_800() {
    runBenchmark(randomTrace, 800);
  }



}
