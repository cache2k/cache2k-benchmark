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

import org.cache2k.benchmark.util.TraceSupplier;
import org.junit.After;
import org.cache2k.benchmark.util.AccessTrace;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class BenchmarkingBase {

  protected BenchmarkCacheFactory factory = new Cache2kStarFactory();

  BenchmarkCache<Integer, Integer> cache = null;

  public BenchmarkCache<Integer, Integer> freshCache(AccessTrace t, int _maxElements) {
    return freshCache(_maxElements);
  }

  public BenchmarkCache<Integer, Integer> freshCache(int _maxElements) {
    if (cache != null) {
      throw new IllegalStateException("Two caches in one test? Please call destroyCache() first");
    }
    return cache = factory.create(Integer.class, Integer.class, _maxElements);
  }

  public void destroyCache() {
    if (cache != null) {
      cache.close();
      cache = null;
    }
  }

  @After
  public void tearDown() {
    destroyCache();
  }

  public static final long runBenchmark(BenchmarkCache<Integer, Integer> c, AccessTrace t) {
    Integer[] _trace = t.getObjectArray();
    if (c instanceof SimulatorPolicy) {
      SimulatorPolicy p = (SimulatorPolicy) c;
      for (Integer k : _trace) {
        ((SimulatorPolicy) c).record(k);
      }
      return p.getMissCount();
    }
    long _missCount =  0;
    for (Integer k : _trace) {
      Integer v = c.get(k);
      if (v == null) {
        c.put(k, k);
        _missCount++;
      }
    }
    return _missCount;
  }

  public final int runBenchmark(TraceSupplier sup, int _cacheSize) {
    return runBenchmark(sup.get(), _cacheSize);
  }

  public final int runBenchmark(AccessTrace t, int _cacheSize) {
    BenchmarkCache<Integer, Integer> c;
    c = freshCache(t, _cacheSize);
    long _missCount = runBenchmark(c, t);
    logHitRate(c, t, _missCount);
    c.close();
    return
      ((t.getLength() - (int) _missCount) * 10000 + t.getLength() / 2) / t.getLength();
  }

  public void logHitRate(BenchmarkCache c, AccessTrace _trace, long _missCount) {
    saveHitRate(_trace, _missCount);
    String _cacheStatistics = c.toString();
    System.out.println(_cacheStatistics);
    System.out.flush();
  }

  void saveHitRate(AccessTrace _trace, long _missCount) {
    double hitRatePercent =
      (_trace.getLength() - _missCount) * 100D / _trace.getLength();
    String _traceName = _trace.getName();
    String _hitRate = String.format("%.3f", hitRatePercent);
    String s = "";
    long _cacheSize = cache.getCapacity();
    if (_cacheSize > 0) {
      s += "size=" + _cacheSize + ", ";
    }
    s += "accessCount=" + _trace.getLength();
    s += ", missCount=" + _missCount + ", hitRatePercent=" + _hitRate;
    s += ", uniqueValues=" + _trace.getValueCount();
    String _cacheImplementation = factory.getName();
    System.out.println(_cacheImplementation + "@" + _traceName + ": " + s);
    String _csvLine =
      _traceName + "|" +  // 1
      factory.getName() + "|" + // 2
      _cacheSize + "|" + // 3
      String.format("%.3f", hitRatePercent) + "|" + // 4
      _trace.getLength() + "|" + // 5
      _missCount + "|" + // 6
      0 + "| "  + // 7
      -1 +  "| " + // 8
      cache.toString(); // 9
    writeCsv(_csvLine);
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

}
