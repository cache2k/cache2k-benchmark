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
import org.cache2k.benchmark.traces.CacheAccessTraceCpp;
import org.cache2k.benchmark.traces.CacheAccessTraceGlimpse;
import org.cache2k.benchmark.traces.CacheAccessTraceMulti2;
import org.cache2k.benchmark.traces.CacheAccessTraceOltp;
import org.cache2k.benchmark.traces.CacheAccessTraceSprite;
import org.cache2k.benchmark.traces.CacheAccessTraceUmassFinancial1;
import org.cache2k.benchmark.traces.CacheAccessTraceUmassFinancial2;
import org.cache2k.benchmark.traces.CacheAccessTraceUmassWebSearch1;
import org.cache2k.benchmark.traces.CacheAccessTraceUmassWebSearch2;
import org.cache2k.benchmark.traces.CacheAccessTraceUmassWebSearch3;
import org.cache2k.benchmark.traces.CacheAccessTraceWeb07;
import org.cache2k.benchmark.traces.CacheAccessTraceWeb12;
import org.cache2k.benchmark.util.AccessTrace;
import org.cache2k.benchmark.util.ZipfianPattern;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jens Wilke; created: 2015-01-13
 */
public class TracesAndTestsCollection extends BenchmarkingBase {
  @Test
  @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void testSimple1() {
    BenchmarkCache<Integer, Integer> c = freshCache(1);
    int v = c.get(47);
    assertEquals(47, v);
    assertEquals(1, c.getMissCount());
    c.destroy();
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void testSimple2() {
    BenchmarkCache<Integer, Integer> c = freshCache(1);
    int v = c.get(47);
    assertEquals(47, v);
    assertEquals(1, c.getMissCount());
    v = c.get(47);
    assertEquals(47, v);
    assertEquals(1, c.getMissCount());
    v = c.get(48);
    assertEquals(48, v);
    assertEquals(2, c.getMissCount());
    c.destroy();
  }

  /**
   * Check whether the cache does a correct replacement and holds at most the
   * given elements. This is a test not a benchmark, just for sanity checking.
   *
   * <p/>Theory: Every cache will behave the same for size 1. However, that is not totally
   * true and depends whether eviction is done before or after a new entry is
   * put in the cache. Eviction is done after the entry was fetched, the eviction may
   * decide to evict the entry just processed and keep a less recent entry in the hot set.
   * This is why ARC and ClockPro yield lower miss counts.
   */
  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void testReplacement1() throws Exception {
    AccessTrace t = BenchmarkCollection.traceRandomForSize1Replacement;
    BenchmarkCache<Integer, Integer> c = freshCache(1);
    runBenchmark(c, t);
    assertEquals("expected == real missed", 909, c.getMissCount());

    logHitRate(c, t, c.getMissCount());
    c.destroy();
  }

  /**
   * Test whether a cache with configured size of 1000 elements
   * holds at least 1000 elements. We do this by running a trace
   * from 0 to 999 twice and then request 1000. This yields
   * exactly 1001 cache misses.
   */
  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void testSize1000() throws Exception {
    AccessTrace t = BenchmarkCollection.trace1001misses;
    BenchmarkCache<Integer, Integer> c = freshCache(1000);
    runBenchmark(c, t);
    assertEquals("expected == real missed", 1001, c.getMissCount());
    logHitRate(c, t, c.getMissCount());
    c.destroy();
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkWeb12_75()  throws Exception {
    runBenchmark(CacheAccessTraceWeb12.getInstance(), 75);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkWeb12_300()  throws Exception {
    runBenchmark(CacheAccessTraceWeb12.getInstance(), 300);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkWeb12_1200()  throws Exception {
    runBenchmark(CacheAccessTraceWeb12.getInstance(), 1200);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkWeb12_3000()  throws Exception {
    runBenchmark(CacheAccessTraceWeb12.getInstance(), 3000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkWeb07_75()  throws Exception {
    runBenchmark(CacheAccessTraceWeb07.getInstance(), 75);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkWeb07_300()  throws Exception {
    runBenchmark(CacheAccessTraceWeb07.getInstance(), 300);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkWeb07_1200()  throws Exception {
    runBenchmark(CacheAccessTraceWeb07.getInstance(), 1200);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkWeb07_3000()  throws Exception {
    runBenchmark(CacheAccessTraceWeb07.getInstance(), 3000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkSprite_100() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 100);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkSprite_200() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 200);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkSprite_400() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 400);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkSprite_600() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 600);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkSprite_800() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 800);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkSprite_1000() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 1000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkCpp_20() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 20);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkCpp_35() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 35);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkCpp_50() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 50);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkCpp_80() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 80);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkCpp_100() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 100);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkCpp_300() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 300);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkCpp_500() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 500);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkGlimpse_500() {
    runBenchmark(CacheAccessTraceGlimpse.getInstance(), 500);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkMulti2_600() {
    runBenchmark(CacheAccessTraceMulti2.getInstance(), 600);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkMulti2_1800() {
    runBenchmark(CacheAccessTraceMulti2.getInstance(), 1800);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkMulti2_3000() {
    runBenchmark(CacheAccessTraceMulti2.getInstance(), 3000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkOltp_1000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 1000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkOltp_2000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 2000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkOltp_5000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 5000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkOltp_10000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 10000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkOltp_15000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 15000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassFinancial1_50000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 50000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassFinancial1_100000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 100000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassFinancial1_200000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 200000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassFinancial2_10000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial2.getInstance(), 10000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassFinancial2_40000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial2.getInstance(), 40000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassFinancial2_90000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial2.getInstance(), 90000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch1_70000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch1.getInstance(), 70000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch1_80000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch1.getInstance(), 80000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch1_90000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch1.getInstance(), 90000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch2_100000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch2.getInstance(), 100000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch2_200000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch2.getInstance(), 200000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch2_300000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch2.getInstance(), 300000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch3_100000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch3.getInstance(), 100000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch3_200000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch3.getInstance(), 200000);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkUmassWebSearch3_300000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch3.getInstance(), 300000);
  }

  public static final int TRACE_LENGTH = 3 * 1000 * 1000;

  static final AccessTrace zipf900Trace =
    new AccessTrace(new ZipfianPattern(900), TRACE_LENGTH);

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkZipf900_100() throws Exception {
    runBenchmark(zipf900Trace, 100);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkZipf900_300() throws Exception {
    runBenchmark(zipf900Trace, 300);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkZipf900_500() throws Exception {
    runBenchmark(zipf900Trace, 500);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkZipf900_700() throws Exception {
    runBenchmark(zipf900Trace, 700);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkEff95() throws Exception {
    runBenchmark(BenchmarkCollection.effective95Trace, 500);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom1000_100() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 100);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom1000_200() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 200);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom1000_350() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 350);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom1000_500() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 500);
  }

  @Test @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void benchmarkTotalRandom1000_800() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 800);
  }
}
