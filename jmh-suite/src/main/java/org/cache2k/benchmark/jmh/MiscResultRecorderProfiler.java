package org.cache2k.benchmark.jmh;

/*
 * #%L
 * Cache benchmark suite based on JMH.
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
