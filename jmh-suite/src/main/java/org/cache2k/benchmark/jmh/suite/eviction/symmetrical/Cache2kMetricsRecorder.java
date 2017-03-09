package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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

  /**
   * Populate a list, so we can do a unit test
   */
  static void recordStats(List<Result> l, String _statisticsString) {
    if (!_statisticsString.startsWith("Cache") && !_statisticsString.contains("integrityState=")) {
      return;
    }
    String[] sa = _statisticsString.split(", |\\(|\\), ");
    long _scanCount = 0;
    long _evictCount = 0;
    for (String s : sa) {
      if (s.startsWith("coldScanCnt=") || s.startsWith("hotScanCnt=")) {
        _scanCount += Long.parseLong(s.split("=")[1]);
      }
      if (s.startsWith("evict=")) {
        _evictCount = Long.parseLong(s.split("=")[1]);
      }
    }
    l.add(new ScalarResult(RESULT_PREFIX + "scanCount", _scanCount, "counter", AggregationPolicy.AVG));
    l.add(new ScalarResult(RESULT_PREFIX + "evictCount", _evictCount, "counter", AggregationPolicy.AVG));
    if (_evictCount > 0) {
      l.add(new ScalarResult(RESULT_PREFIX + "scanPerEviction", _scanCount * 1.0D / _evictCount, "counter", AggregationPolicy.AVG));
    }
  }

  public static void recordStats(String _statisticsString) {
    List<Result> l = new ArrayList<>();
    recordStats(l, _statisticsString);
    l.forEach(MiscResultRecorderProfiler::setResult);
  }

}
