package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: implementation variants
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
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

import org.cache2k.benchmark.traces.CacheAccessTraceCordaSmall;
import org.cache2k.benchmark.traces.CacheAccessTraceCpp;
import org.cache2k.benchmark.traces.CacheAccessTraceOrmAccessBusy;
import org.cache2k.benchmark.traces.CacheAccessTraceGlimpse;
import org.cache2k.benchmark.traces.CacheAccessTraceMulti2;
import org.cache2k.benchmark.traces.CacheAccessTraceOltp;
import org.cache2k.benchmark.traces.CacheAccessTraceOrmAccessNight;
import org.cache2k.benchmark.traces.CacheAccessTraceScarabProds;
import org.cache2k.benchmark.traces.CacheAccessTraceScarabRecs;
import org.cache2k.benchmark.traces.CacheAccessTraceSprite;
import org.cache2k.benchmark.traces.CacheAccessTraceUmassFinancial1;
import org.cache2k.benchmark.traces.CacheAccessTraceUmassFinancial2;
import org.cache2k.benchmark.traces.CacheAccessTraceUmassWebSearch1;
import org.cache2k.benchmark.traces.CacheAccessTraceWeb07;
import org.cache2k.benchmark.traces.CacheAccessTraceWeb12;
import org.cache2k.benchmark.util.AccessTrace;
import org.cache2k.benchmark.util.DistAccessPattern;
import org.cache2k.benchmark.util.Patterns;
import org.cache2k.benchmark.util.RandomAccessPattern;
import org.cache2k.benchmark.util.ZipfianPattern;
import org.junit.Test;

/**
 * @author Jens Wilke; created: 2015-01-13
 */
public class TracesAndTestsCollection extends BenchmarkingBase {

  public static final AccessTrace traceRandomForSize1Replacement =
    new AccessTrace(new RandomAccessPattern(10), 1000);

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
  @Test
  public void benchmarkRandom10_1() throws Exception {
    AccessTrace t = traceRandomForSize1Replacement;
    runBenchmark(t, 1);
  }

  public static final AccessTrace trace1001misses =
    new AccessTrace(
      Patterns.sequence(1000),
      Patterns.sequence(1000),
      Patterns.sequence(1000, 1001));

  /**
   * Test whether a cache with configured size of 1000 elements
   * holds at least 1000 elements. We do this by running a trace
   * from 0 to 999 twice and then request 1000. This yields
   * exactly 1001 cache misses.
   */
  @Test
  public void benchmark1001missis_1000() throws Exception {
    runBenchmark(trace1001misses, 1000);
  }

  @Test
  public void benchmarkWeb12_75()  throws Exception {
    runBenchmark(CacheAccessTraceWeb12.getInstance(), 75);
  }

  @Test
  public void benchmarkWeb12_300()  throws Exception {
    runBenchmark(CacheAccessTraceWeb12.getInstance(), 300);
  }

  @Test
  public void benchmarkWeb12_1200()  throws Exception {
    runBenchmark(CacheAccessTraceWeb12.getInstance(), 1200);
  }

  @Test
  public void benchmarkWeb12_3000()  throws Exception {
    runBenchmark(CacheAccessTraceWeb12.getInstance(), 3000);
  }

  @Test
  public void benchmarkWeb07_75()  throws Exception {
    runBenchmark(CacheAccessTraceWeb07.getInstance(), 75);
  }

  @Test
  public void benchmarkWeb07_300()  throws Exception {
    runBenchmark(CacheAccessTraceWeb07.getInstance(), 300);
  }

  @Test
  public void benchmarkWeb07_1200()  throws Exception {
    runBenchmark(CacheAccessTraceWeb07.getInstance(), 1200);
  }

  @Test
  public void benchmarkWeb07_3000()  throws Exception {
    runBenchmark(CacheAccessTraceWeb07.getInstance(), 3000);
  }

  @Test
  public void benchmarkSprite_100() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 100);
  }

  @Test
  public void benchmarkSprite_200() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 200);
  }

  @Test
  public void benchmarkSprite_400() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 400);
  }

  @Test
  public void benchmarkSprite_600() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 600);
  }

  @Test
  public void benchmarkSprite_800() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 800);
  }

  @Test
  public void benchmarkSprite_1000() {
    runBenchmark(CacheAccessTraceSprite.getInstance(), 1000);
  }

  @Test
  public void benchmarkCpp_20() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 20);
  }

  @Test
  public void benchmarkCpp_35() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 35);
  }

  @Test
  public void benchmarkCpp_50() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 50);
  }

  @Test
  public void benchmarkCpp_80() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 80);
  }

  @Test
  public void benchmarkCpp_100() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 100);
  }

  @Test
  public void benchmarkCpp_300() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 300);
  }

  @Test
  public void benchmarkCpp_500() {
    runBenchmark(CacheAccessTraceCpp.getInstance(), 500);
  }

  @Test
  public void benchmarkGlimpse_500() {
    runBenchmark(CacheAccessTraceGlimpse.getInstance(), 500);
  }

  @Test
  public void benchmarkGlimpse_1000() {
    runBenchmark(CacheAccessTraceGlimpse.getInstance(), 1000);
  }

  @Test
  public void benchmarkGlimpse_2000() {
    runBenchmark(CacheAccessTraceGlimpse.getInstance(), 2000);
  }

  @Test
  public void benchmarkMulti2_600() {
    runBenchmark(CacheAccessTraceMulti2.getInstance(), 600);
  }

  @Test
  public void benchmarkMulti2_1800() {
    runBenchmark(CacheAccessTraceMulti2.getInstance(), 1800);
  }

  @Test
  public void benchmarkMulti2_3000() {
    runBenchmark(CacheAccessTraceMulti2.getInstance(), 3000);
  }

  @Test
  public void benchmarkOltp_1000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 1000);
  }

  @Test
  public void benchmarkOltp_2000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 2000);
  }

  @Test
  public void benchmarkOltp_5000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 5000);
  }

  @Test
  public void benchmarkOltp_10000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 10000);
  }

  @Test
  public void benchmarkOltp_15000() throws Exception {
    runBenchmark(CacheAccessTraceOltp.getInstance(), 15000);
  }

  @Test
  public void benchmarkUmassFinancial1_12500() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 12500);
  }

  @Test
  public void benchmarkUmassFinancial1_25000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 25000);
  }

  @Test
  public void benchmarkUmassFinancial1_50000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 50000);
  }

  @Test
  public void benchmarkUmassFinancial1_100000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 100000);
  }

  @Test
  public void benchmarkUmassFinancial1_200000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 200000);
  }

  @Test
  public void benchmarkUmassFinancial1_8192() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial1.getInstance(), 8192);
  }

  @Test
  public void benchmarkUmassFinancial2_5000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial2.getInstance(), 5000);
  }

  @Test
  public void benchmarkUmassFinancial2_10000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial2.getInstance(), 10000);
  }

  @Test
  public void benchmarkUmassFinancial2_20000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial2.getInstance(), 20000);
  }

  @Test
  public void benchmarkUmassFinancial2_40000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial2.getInstance(), 40000);
  }

  @Test
  public void benchmarkUmassFinancial2_80000() throws Exception {
    runBenchmark(CacheAccessTraceUmassFinancial2.getInstance(), 80000);
  }

  @Test
  public void benchmarkUmassWebSearch1_100000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch1.getInstance(), 100000);
  }

  @Test
  public void benchmarkUmassWebSearch1_200000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch1.getInstance(), 200000);
  }

  @Test
  public void benchmarkUmassWebSearch1_300000() throws Exception {
    runBenchmark(CacheAccessTraceUmassWebSearch1.getInstance(), 300000);
  }



  @Test
  public void benchmarkOrmAccessNight_625() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessNight.getInstance(), 625);
  }
  @Test
  public void benchmarkOrmAccessNight_1250() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessNight.getInstance(), 1250);
  }
  @Test
  public void benchmarkOrmAccessNight_2500() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessNight.getInstance(), 2500);
  }
  @Test
  public void benchmarkOrmAccessNight_5000() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessNight.getInstance(), 5000);
  }
  @Test
  public void benchmarkOrmAccessNight_10000() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessNight.getInstance(), 10000);
  }
  @Test
  public void benchmarkOrmAccessNight_20000() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessNight.getInstance(), 20000);
  }

  @Test
  public void benchmarkOrmAccessBusytime_625() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessBusy.getInstance(), 625);
  }
  @Test
  public void benchmarkOrmAccessBusytime_1250() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessBusy.getInstance(), 1250);
  }
  @Test
  public void benchmarkOrmAccessBusytime_2500() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessBusy.getInstance(), 2500);
  }
  @Test
  public void benchmarkOrmAccessBusytime_5000() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessBusy.getInstance(), 5000);
  }
  @Test
  public void benchmarkOrmAccessBusytime_10000() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessBusy.getInstance(), 10000);
  }
  @Test
  public void benchmarkOrmAccessBusytime_20000() throws Exception {
    runBenchmark(CacheAccessTraceOrmAccessBusy.getInstance(), 20000);
  }

  @Test
  public void benchmarkScarabRecs_25000() throws Exception {
    runBenchmark(CacheAccessTraceScarabRecs.getInstance(), 25000);
  }

  @Test
  public void benchmarkScarabRecs_50000() throws Exception {
    runBenchmark(CacheAccessTraceScarabRecs.getInstance(), 50000);
  }

  @Test
  public void benchmarkScarabRecs_75000() throws Exception {
    runBenchmark(CacheAccessTraceScarabRecs.getInstance(), 75000);
  }

  @Test
  public void benchmarkScarabRecs_100000() throws Exception {
    runBenchmark(CacheAccessTraceScarabRecs.getInstance(), 100000);
  }

  @Test
  public void benchmarkScarabProds_25000() throws Exception {
    runBenchmark(CacheAccessTraceScarabProds.getInstance(), 25000);
  }

  @Test
  public void benchmarkScarabProds_50000() throws Exception {
    runBenchmark(CacheAccessTraceScarabProds.getInstance(), 50000);
  }

  @Test
  public void benchmarkScarabProds_75000() throws Exception {
    runBenchmark(CacheAccessTraceScarabProds.getInstance(), 75000);
  }

  @Test
  public void benchmarkScarabProds_100000() throws Exception {
    runBenchmark(CacheAccessTraceScarabProds.getInstance(), 100000);
  }

  @Test
  public void benchmarkCordaSmall_512() throws Exception {
    runBenchmark(CacheAccessTraceCordaSmall.getInstance(), 512);
  }

  @Test
  public void benchmarkCordaSmall10x_5000() throws Exception {
    runBenchmark(cordaSmall10x, 5000);
  }

  static final AccessTrace cordaSmall10x =
    new AccessTrace(Patterns.explode(CacheAccessTraceCordaSmall.getInstance().newPattern(), 10));

  public static final int TRACE_LENGTH = 3 * 1000 * 1000;

  static final AccessTrace zipf900Trace =
    new AccessTrace(new ZipfianPattern(1802, 900), TRACE_LENGTH);

  @Test
  public void benchmarkZipf900_50() throws Exception {
    runBenchmark(zipf900Trace, 50);
  }

  @Test
  public void benchmarkZipf900_100() throws Exception {
    runBenchmark(zipf900Trace, 100);
  }

  @Test
  public void benchmarkZipf900_300() throws Exception {
    runBenchmark(zipf900Trace, 300);
  }

  @Test
  public void benchmarkZipf900_500() throws Exception {
    runBenchmark(zipf900Trace, 500);
  }

  @Test
  public void benchmarkZipf900_700() throws Exception {
    runBenchmark(zipf900Trace, 700);
  }

  public static final int ZIPF10K_TRACE_LENGTH = 10 * 1000 * 1000;

  static final AccessTrace zipf10kTrace =
    new AccessTrace(new ZipfianPattern(1802,10000), ZIPF10K_TRACE_LENGTH);


  @Test
  public void benchmarkZipf10k_500() throws Exception {
    runBenchmark(zipf10kTrace, 500);
  }


  @Test
  public void benchmarkZipf10k_2000() throws Exception {
    runBenchmark(zipf10kTrace, 2000);
  }


  @Test
  public void benchmarkZipf10k_8000() throws Exception {
    runBenchmark(zipf10kTrace, 8000);
  }

  static final AccessTrace effective95Trace =
    new AccessTrace(new DistAccessPattern(900), TRACE_LENGTH);

  @Test
  public void benchmarkEff95_500() throws Exception {
    runBenchmark(effective95Trace, 500);
  }

  static final AccessTrace effective90Trace =
    new AccessTrace(new DistAccessPattern(1000), TRACE_LENGTH);

  @Test
  public void benchmarkEff90_500() throws Exception {
    runBenchmark(effective90Trace, 500);
  }

  public static final AccessTrace randomTrace1000 =
    new AccessTrace(new RandomAccessPattern(1000), TRACE_LENGTH);

  @Test
  public void benchmarkRandom1000_500()  throws Exception {
    runBenchmark(randomTrace1000, 500);
  }

  @Test
  public void benchmarkTotalRandom1000_100() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 100);
  }

  @Test
  public void benchmarkTotalRandom1000_200() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 200);
  }

  @Test
  public void benchmarkTotalRandom1000_350() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 350);
  }

  @Test
  public void benchmarkTotalRandom1000_500() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 500);
  }

  @Test
  public void benchmarkTotalRandom1000_800() {
    runBenchmark(BenchmarkCollection.randomTrace1000, 800);
  }

}
