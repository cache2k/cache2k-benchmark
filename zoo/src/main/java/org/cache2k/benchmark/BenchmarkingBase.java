package org.cache2k.benchmark;

/*
 * #%L
 * cache2k-benchmark-zoo
 * %%
 * Copyright (C) 2013 - 2014 headissue GmbH, Munich
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

import org.junit.After;
import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cache2k.benchmark.util.AccessTrace;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class BenchmarkingBase extends AbstractBenchmark {

  static Map<String,String> benchmarkName2csv = new TreeMap<>();
  static HashSet<String> onlyOneResult = new HashSet<>();

  protected BenchmarkCacheFactory factory = new Cache2kFactory();

  BenchmarkCache<Integer, Integer> cache = null;

  public BenchmarkCache<Integer, Integer> freshCache(int _maxElements) {
    if (cache != null) {
      throw new IllegalStateException("Two caches in one test? Please call destroyCache() first");
    }
    return cache = factory.create(_maxElements);
  }

  public void destroyCache() {
    if (cache != null) {
      cache.destroy();
      cache = null;
    }
  }

  @After
  public void tearDown() {
    destroyCache();
  }

  public final void runBenchmark(BenchmarkCache<Integer, Integer> c, AccessTrace t) {
    int[] _trace = t.getArray();
    for (int v : _trace) {
      c.get(v);
    }
  }

  public final int runBenchmark(AccessTrace t, int _cacheSize) {
    BenchmarkCache<Integer, Integer> c = freshCache(_cacheSize);
    runBenchmark(c, t);
    logHitRate(c, t, c.getMissCount());
    c.destroy();
    return
      ((t.getTraceLength() - c.getMissCount()) * 10000 + t.getTraceLength() / 2) / t.getTraceLength();
  }

  public void logHitRate(BenchmarkCache c, AccessTrace _trace, long _missCount) {
    int _optHitRate = _trace.getOptHitRate(c.getCacheSize()).get4digit();
    String _testName = extractTestName();
    if (onlyOneResult.contains(_testName)) {
      return;
    }
    onlyOneResult.add(_testName);
    saveHitRate(_testName, c.getCacheSize(), _trace, _optHitRate, _missCount);
    if (c != null) {
      c.checkIntegrity();
      String _cacheStatistics = c.getStatistics();
      System.out.println(_cacheStatistics);
    }
    System.out.flush();
  }

  void saveHitRate(String _testName, int _cacheSize, AccessTrace _trace, int _optHitRate, long _missCount) {
    double _hitRateTimes100 =
      (_trace.getTraceLength() - _missCount) * 100D / _trace.getTraceLength();
    String _hitRate = String.format("%.2f", _hitRateTimes100);
    String s = "";
    if (_cacheSize > 0) {
      s += "size=" + _cacheSize + ", ";
    }
    s += "accessCount=" + _trace.getTraceLength();
    s += ", missCount=" + _missCount + ", hitRatePercent=" + _hitRate;
    if (_optHitRate >= 0) {
      s += ", optHitRatePercent=" + String.format("%.2f", _optHitRate * 1D / 100);
    }
    s += ", randomHitRatePercent=" + String.format("%.2f", _trace.getRandomHitRate(_cacheSize).getFactor() * 100);
    s += ", uniqueValues=" + _trace.getValueCount();
    System.out.println(_testName + ": " + s);
    int idx = _testName.lastIndexOf('.');
    String _cacheImplementation = _testName.substring(0, idx);
    String _benchmarkName = _testName.substring(idx + 1);
    String _csvLine =
      _benchmarkName + "|" +  // 1
      _cacheImplementation + "|" + // 2
        String.format("%.2f", _hitRateTimes100) + "|" + // 3
        _cacheSize + "|" + // 4
      _trace.getTraceLength() + "|" + // 5
      _trace.getValueCount() + "|" + // 6
      String.format("%.2f", _optHitRate * 1D / 100) + "|" + // 7
      String.format("%.2f", _trace.getRandomHitRate(_cacheSize).getFactor() * 100); // 8
    benchmarkName2csv.put(_testName, _csvLine);
    writeCsv();
  }

  void writeCsv() {
    String s = System.getProperty("cache2k.benchmark.result.csv");
    if (s == null) {
      return;
    }
    try {
      PrintWriter w = new PrintWriter(new FileWriter(s));
      for (String k : benchmarkName2csv.keySet()) {
        w.println(benchmarkName2csv.get(k));
      }
      w.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  String extractTestName() {
    Exception e = new Exception();
    int idx = 1;
    StackTraceElement[] _stackTrace = e.getStackTrace();
    do {
      String n = _stackTrace[idx].getMethodName();
      idx++;
      if (n.startsWith("benchmark") || n.startsWith("test")) {
        return this.getClass().getName() + "." + _stackTrace[idx - 1].getMethodName();
      }
    } while (true);
  }

}
