package org.cache2k.benchmark.jmh;

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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Record misc secondary result metrics.
 *
 * @author Jens Wilke
 */
public class MiscResultRecorderProfiler implements InternalProfiler {

  public static final String SECONDARY_RESULT_PREFIX = "+misc.";

  static final Map<String, CounterResult> counters = new ConcurrentHashMap<>();
  static final Map<String, ProfilerResult> results = new ConcurrentHashMap<>();

  /**
   * Insert the counter value as secondary result. If a value is already inserted the
   * counter value is added to the existing one. This can be used to collect and sum up
   * results from different threads.
   */
  public static void addCounterResult(String key, long _counter, String _unit, AggregationPolicy _aggregationPolicy) {
    CounterResult r = counters.computeIfAbsent(key, any -> new CounterResult());
    r.aggregationPolicy = _aggregationPolicy;
    r.unit = _unit;
    r.counter.addAndGet(_counter);
    r.key = key;
  }

  public static long getCounterResult(String key) {
    return counters.getOrDefault(key, new CounterResult()).counter.get();
  }

  /**
   * Insert the counter value as secondary result. An existing counter value is replaced.
   */
  public static void setResult(String key, double _result, String _unit, AggregationPolicy _aggregationPolicy) {
    results.put(key,
      new ProfilerResult(SECONDARY_RESULT_PREFIX + key,_result, _unit, _aggregationPolicy));
  }

  @Override
  public void beforeIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams) {
    counters.clear();
  }

  @Override
  public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams, final IterationResult result) {
    List<ProfilerResult> all = new ArrayList<>();
    counters.values().stream()
      .map(e ->
        new ProfilerResult(SECONDARY_RESULT_PREFIX + e.key, (double) e.counter.get(), e.unit, e.aggregationPolicy))
      .sequential().forEach(e -> all.add(e));
    all.addAll(results.values());
    return all;
  }

  @Override
  public String getDescription() {
    return "Adds additional results gathered by the benchmark as secondary results.";
  }

  static class CounterResult {
    String key;
    String unit;
    AggregationPolicy aggregationPolicy;
    AtomicLong counter = new AtomicLong();
  }

}
