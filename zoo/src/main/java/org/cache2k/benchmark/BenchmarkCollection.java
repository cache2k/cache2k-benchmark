package org.cache2k.benchmark;

/*
 * #%L
 * cache2k-benchmark-zoo
 * %%
 * Copyright (C) 2013 - 2015 headissue GmbH, Munich
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

/**
 * All cache benchmarks in one class. There are three different types
 * of methods: The test* methods check for some basic cache behaviour
 * to ensure sanity, the benchmarkXy methods do a speed measurement,
 * the benchmarkXy_NNN do a efficiency test on a given trace.
 */
@SuppressWarnings(value={"unchecked", "unused"})
@BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 2)
public class BenchmarkCollection extends TracesAndTestsCollection {

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
    logHitRate(c, allMissTrace, c.getMissCount());
    c.destroy();
  }

  @Test
  public void benchmarkMiss_5000() {
    BenchmarkCache<Integer, Integer> c = freshCache(5000);
    runBenchmark(c, allMissTrace);
    logHitRate(c, allMissTrace, c.getMissCount());
    c.destroy();
  }

  @Test
  public void benchmarkMiss_50000() {
    BenchmarkCache<Integer, Integer> c = freshCache(50000);
    runBenchmark(c, allMissTrace);
    logHitRate(c, allMissTrace, c.getMissCount());
    c.destroy();
  }

  @Test
  public void benchmarkMiss_500000() {
    BenchmarkCache<Integer, Integer> c = freshCache(500000);
    runBenchmark(c, allMissTrace);
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

  @Test
  public void benchmarkEff90() throws Exception {
    runBenchmark(effective90Trace, 500);
  }

  @Test
  public void benchmarkEff90Threads2() throws Exception {
    runMultiThreadBenchmark(2, effective90Trace, 500);
  }

  @Test
  public void benchmarkEff90Threads4() throws Exception {
    runMultiThreadBenchmark(4, effective90Trace, 500);
  }

  @Test
  public void benchmarkEff90Threads6() throws Exception {
    runMultiThreadBenchmark(6, effective90Trace, 500);
  }

  @Test
  public void benchmarkEff90Threads8() throws Exception {
    runMultiThreadBenchmark(8, effective90Trace, 500);
  }

  static final AccessTrace effective95Trace =
    new AccessTrace(new DistAccessPattern(900), TRACE_LENGTH);

  @Test
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
