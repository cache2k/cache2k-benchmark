package org.cache2k.benchmark.jmh;

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

import org.cache2k.benchmark.jmh.MiscResultRecorderProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ScalarResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract some internal metrics from the to string output and record
 * it in the result.
 *
 * @author Jens Wilke
 */
public class Cache2kMetricsRecorder {

  public final static String RESULT_PREFIX = "+c2k.stat.";

  static Stat extract(String _statisticsString) {
    if (!_statisticsString.startsWith("Cache") && !_statisticsString.contains("integrityState=")) {
      return null;
    }
    Stat x = new Stat();
    String[] sa = _statisticsString.split(", |\\(|\\), |\\)\\)");
    for (String s : sa) {
      if (s.startsWith("coldScanCnt=") || s.startsWith("hotScanCnt=")) {
        x.scanCount += Long.parseLong(s.split("=")[1]);
      }
      if (s.startsWith("evict=")) {
        x.evictCount = Long.parseLong(s.split("=")[1]);
      }
      if (s.startsWith("get=")) {
        x.getCount = Long.parseLong(s.split("=")[1]);
      }
    }
    return x;
  }

  static class Stat {

    long scanCount;
    long evictCount;
    long getCount;

  }

  static volatile Stat before;

  public static void saveStatsAfterSetup(String statisticsString) {
    before = extract(statisticsString);
  }

  public static void recordStatsAfterIteration(String statisticsString) {
    System.err.println(statisticsString);
    Stat now = extract(statisticsString);
    if (now == null) {
      return;
    }
    if (before == null) {
      throw new IllegalStateException("saveStats() needs to be called before iteration.");
    }
    long scanCount = now.scanCount - before.scanCount;
    long evictCount = now.evictCount - before.evictCount;
    long getCount = now.getCount - before.getCount;
    List<Result> l = new ArrayList<>();
    l.add(new ScalarResult(RESULT_PREFIX + "scanCount", scanCount, "counter", AggregationPolicy.AVG));
    l.add(new ScalarResult(RESULT_PREFIX + "evictCount", evictCount, "counter", AggregationPolicy.AVG));
    l.add(new ScalarResult(RESULT_PREFIX + "getCount", getCount, "counter", AggregationPolicy.AVG));
    if (evictCount > 0) {
      l.add(new ScalarResult(RESULT_PREFIX + "scanPerEviction", scanCount * 1.0D / evictCount, "counter", AggregationPolicy.AVG));
    }
    l.forEach(MiscResultRecorderProfiler::addResult);
    before = now;
  }

}
