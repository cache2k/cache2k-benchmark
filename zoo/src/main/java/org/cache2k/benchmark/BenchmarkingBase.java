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

import org.junit.After;
import static org.junit.Assert.*;
import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
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

  protected boolean skipMultithreaded = false;
  protected BenchmarkCacheFactory factory = new Cache2kFactory();

  BenchmarkCache<Integer, Integer> cache = null;

  public BenchmarkCache<Integer, Integer> freshCache(int _maxElements) {
    if (cache != null) {
      throw new IllegalStateException("Two caches in one test? Please call destroyCache() first");
    }
    return cache = factory.create(_maxElements);
  }

  public BenchmarkCache<Integer, Integer> freshCache(BenchmarkCacheFactory.Source s, int _maxElements) {
    if (cache != null) {
      throw new IllegalStateException("Two caches in one test? Please call destroyCache() first");
    }
    return cache = factory.create(s, _maxElements);
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

  public final void runMultiThreadBenchmark(
    final BenchmarkCache<Integer, Integer> c,
    int _threadCount,
    int _startOffset,
    AccessTrace... _traces) throws Exception {
    Thread[] _threads = new Thread[_threadCount];
    for (int i = 0; i < _threadCount; i++) {
      final int[] _trace = _traces[i % _traces.length].getArray();
      final int _startIdx = (_startOffset * i) % _trace.length;
      Runnable r = new Runnable() {
        @Override
        public void run() {
          int idx = _startIdx;
          do {
            c.get(_trace[idx]);
            idx = (idx + 1) % _trace.length;
          } while (idx != _startIdx);
        }
      };
      Thread t = new Thread(r);
      t.start();
      _threads[i] = t;
    }
    for (int i = 0; i < _threadCount; i++) {
      _threads[i].join();
    }

  }

  public final void runBenchmark(BenchmarkCache<Integer, Integer> c, AccessTrace t) {
    Integer[] _trace = t.getObjectTrace();
    for (Integer v : _trace) {
      c.get(v);
    }
  }

  public final int runBenchmark(AccessTrace t, int _cacheSize) {
    return runBenchmark(null, t, _cacheSize);
  }

  public final int runBenchmark(BenchmarkCacheFactory.Source s, AccessTrace t, int _cacheSize) {
    BenchmarkCache<Integer, Integer> c;
    if (s == null) {
      c = freshCache(_cacheSize);
    } else {
      c = freshCache(s, _cacheSize);
    }
    runBenchmark(c, t);
    logHitRate(c, t, c.getMissCount());
    c.destroy();
    return
      ((t.getTraceLength() - c.getMissCount()) * 10000 + t.getTraceLength() / 2) / t.getTraceLength();
  }

  public final int runMultiThreadBenchmark(BenchmarkCacheFactory.Source s, int _threadCount, AccessTrace t, int _cacheSize) throws Exception {
    assertFalse(skipMultithreaded);
    BenchmarkCache<Integer, Integer> c = freshCache(s, _cacheSize);
    runMultiThreadBenchmark(c, _threadCount, t.getTraceLength() / _threadCount, t);
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
    long _usedMem = -1;
    if (c.getCacheSize() > 3000) {
      _usedMem = calculateUsedMemory();
    }
    saveHitRate(_testName, c.getCacheSize(), _trace, _optHitRate, _missCount, _usedMem);
    c.checkIntegrity();
    String _cacheStatistics = c.getStatistics();
    System.out.println(_cacheStatistics);
    System.out.flush();
  }

  private long calculateUsedMemory() {
    long _usedMem;
    System.out.println("cache2k benchmark is requesting GC (record used memory)...");
    try {
      Runtime.getRuntime().gc();
      Thread.sleep(55);
      Runtime.getRuntime().gc();
      Thread.sleep(55);
      Runtime.getRuntime().gc();
      Thread.sleep(55);
      Runtime.getRuntime().gc();
      Thread.sleep(55);
    } catch (Exception ignore) { }
    long _total;
    long _total2;
    long _count = -1;
    do {
      _count++;
      _total = Runtime.getRuntime().totalMemory();
      try {
        Thread.sleep(25);
      } catch (Exception ignore) { }
      long _free = Runtime.getRuntime().freeMemory();
      _total2 = Runtime.getRuntime().totalMemory();
      _usedMem = _total - _free;
    } while (_total != _total2);
    System.out.println("looped for stable total memory, count=" + _count + ", total=" + _total + ", used=" + _usedMem);
    return _usedMem;
  }

  void saveHitRate(String _testName, int _cacheSize, AccessTrace _trace, int _optHitRate, long _missCount, long _usedMem) {
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
    s += ", usedMem=" + _usedMem;
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
      String.format("%.2f", _trace.getRandomHitRate(_cacheSize).getFactor() * 100) + "|" +  // 8
      _usedMem; // 9
    if (!benchmarkName2csv.containsKey(_testName)) {
      benchmarkName2csv.put(_testName, _csvLine);
      writeCsv(_csvLine);
    }
  }

  void writeCsv(String _csvLine) {
    String s = System.getProperty("cache2k.benchmark.result.csv");
    if (s == null) {
      return;
    }
    try {
      PrintWriter w = new PrintWriter(new FileWriter(s, true));
      w.println(_csvLine);
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
