package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Implementation and eviction variants
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

import org.cache2k.benchmark.prototype.EvictionStatistics;
import org.cache2k.benchmark.traces.Ranking;
import org.cache2k.benchmark.util.AccessTrace;

import java.io.CharArrayWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Runs eviction benchmarks, writes results to a CSV file and collects results
 * in a ranking.
 *
 * @author Jens Wilke; created: 2013-12-08
 */
public class EvictionBenchmarkRunner {

  private final Ranking ranking = new Ranking();

  private String name;
  private boolean multipleImplementations = false;
  private String autoCandidate;

  /**
   * Construct a new benchmark runner.
   *
   * @param name name of the benchmark suite, e.g. the class name
   */
  public EvictionBenchmarkRunner(final String name) {
    this.name = name;
  }

  public void readEvaluationResults() {
    ranking.readEvaluationResults();
  }

  public void printRankingSummary(String candidate, String[] peers) {
    CharArrayWriter buffer = new CharArrayWriter();
    PrintWriter out = new PrintWriter(buffer);
    if (candidate == null) {
      candidate = autoCandidate;
    }
    final String realCandidate = candidate;
    if (candidate != null) {
      Ranking current = new Ranking();
      Ranking others = new Ranking();
      current.addAll(ranking.getAllResults().stream()
        .filter(r -> r.getImplementationName().equals(realCandidate)));
      others.addAll(ranking.getAllResults().stream()
        .filter(r -> !r.getImplementationName().equals(realCandidate)));
      out.println();
      out.println("== Summary - Top 3 ===");
      out.println("trace size hitrate best hitrate diff 2nd-best hitrate diff 3rd-best hitrate diff");
      others.writeTopSummary(out, current, 3);
      out.println();
      out.println("== Summary - Peers ===");
      StringBuilder sb = new StringBuilder();
      for (String peer : peers) {
        sb.append(" " + peer + " - diff");
      }
      out.println("trace size hitrate" + sb.toString());
      others.printSummary(out, current, peers);
    }
    System.out.print(buffer.toString());
  }

  public long runBenchmark(EvictionTestVariation variation) {
    return runBenchmark(variation.getCacheFactory(), variation.getTraceSupplier().get(), variation.getCacheSize());
  }

  public long runBenchmark(AnyCacheFactory factory, AccessTrace trace, int capacity) {
    saveAutoCandidate(factory);
    long missCount =  0;
    if (factory instanceof BenchmarkCacheFactory) {
      BenchmarkCacheFactory benchmarkCacheFactory = (BenchmarkCacheFactory) factory;
      BenchmarkCache<Integer, Integer> cache = benchmarkCacheFactory.create(Integer.class, Integer.class, capacity);
      Integer[] objTrace = trace.getObjectArray();
      for (Integer k : objTrace) {
        Integer v = cache.get(k);
        if (v == null) {
          cache.put(k, k);
          missCount++;
        }
      }
      logHitRate(factory, capacity, cache.toString(), cache.getEvictionStats(), trace, missCount);
    } else {
      SimulatorPolicy policy = ((SimulatorPolicyFactory) factory).create(capacity);
      Integer[] objTrace = trace.getObjectArray();
      for (Integer k : objTrace) {
        policy.record(k);
      }
      missCount = policy.getMissCount();
      logHitRate(factory, capacity, policy.toString(), policy.getEvictionStatistics(), trace, missCount);
    }
    return missCount;
  }

  /**
   * Set {@link #autoCandidate} to the implementation name if only a single implementation
   * is tested in this run.
   */
  private void saveAutoCandidate(final AnyCacheFactory factory) {
    if (autoCandidate == null) {
      if (!multipleImplementations) {
        autoCandidate = factory.getName();
      }
    } else {
      multipleImplementations = !autoCandidate.equals(factory.getName());
      if (multipleImplementations) {
        autoCandidate = null;
      }
    }
  }

  private void logHitRate(AnyCacheFactory factory, long cacheSize, String cacheToString, EvictionStatistics stats, AccessTrace trace, long missCount) {
    saveHitRate(factory, cacheSize, cacheToString, stats, trace, missCount);
    System.out.println(cacheToString);
    System.out.flush();
  }

  private void saveHitRate(AnyCacheFactory factory, long cacheSize, String cacheToString, EvictionStatistics _evictionStats, AccessTrace _trace, long _missCount) {
    double hitRatePercent =
      (_trace.getLength() - _missCount) * 100D / _trace.getLength();
    String _traceName = _trace.getName();
    String _hitRate = String.format("%.3f", hitRatePercent);
    String s = "";
    if (cacheSize > 0) {
      s += "size=" + cacheSize + ", ";
    }
    s += "accessCount=" + _trace.getLength();
    s += ", missCount=" + _missCount + ", hitRatePercent=" + _hitRate;
    s += ", uniqueValues=" + _trace.getValueCount();
    String _cacheImplementation = factory.getName();
    System.out.println(_cacheImplementation + "@" + _traceName + ": " + s + " " + ranking.getTop3(_traceName, cacheSize));
    String _csvLine =
      _traceName + "|" +  // 1
      factory.getName() + "|" + // 2
      cacheSize + "|" + // 3
      String.format("%.3f", hitRatePercent) + "|" + // 4
      _trace.getLength() + "|" + // 5
      _missCount + "|" + // 6
      _evictionStats.getEvictionCount() + "| "  + // 7
      _evictionStats.getScanCount() +  "| " + // 8
      String.format("%.3f",_evictionStats.getScansPerEviction()) + "|" + // 9
      cacheToString; // 10
    writeCsv(_csvLine);
    Ranking.Result result = new Ranking.Result();
    result.setImplementationName(factory.getName());
    result.setTraceName(_traceName);
    result.setCacheSize(cacheSize);
    result.setMissCount(_missCount);
    result.setTraceLength(_trace.getLength());
    ranking.add(result);
  }

  /**
   * In case we run concurrently, control file access.
   */
  private static final Object CSV_WRITE_LOCK = new Object();

  private static void writeCsv(String csvLine) {
    String s = System.getProperty("cache2k.benchmark.result.csv");
    if (s == null) {
      return;
    }
    synchronized (CSV_WRITE_LOCK) {
      try {
        PrintWriter w = new PrintWriter(new FileWriter(s, true));
        w.println(csvLine);
        w.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
