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

import org.infinispan.util.concurrent.ConcurrentHashSet;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;

import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.ThroughputResult;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Record misc secondary result metrics.
 *
 * @author Jens Wilke
 */
public class MiscResultRecorderProfiler implements InternalProfiler {

  public static final String SECONDARY_COUNTER_PREFIX = "+misc.";

  static final ConcurrentHashSet<Result<?>>  resultSet = new ConcurrentHashSet<>();

  public static void addResult(Result<?> result) {
    resultSet.add(result);
  }

  public static void addCounter(String name, long count) {
    addCounter(name, count, false);
  }

  public static void addCounterWithThroughput(String name, long count) {
    addCounter(name, count, true);
  }
  public static void addCounter(String name, long count, boolean withThroughPut) {
    addResult(new ValueResult(ResultRole.SECONDARY, SECONDARY_COUNTER_PREFIX + name, (double) count,
      "counts", AggregationPolicy.AVG, withThroughPut));
  }

  public static void addValue(String name, double result, String unit) {
    addResult(new ValueResult(ResultRole.SECONDARY, SECONDARY_COUNTER_PREFIX + name, result,
      unit, AggregationPolicy.AVG, false));
  }

  public static void removeMetric(String name) {
    String effectiveLabel = SECONDARY_COUNTER_PREFIX + name;
    Iterator<Result<?>> it = resultSet.iterator();
    while (it.hasNext()) {
      if (it.next().getLabel().equals(effectiveLabel)) {
        it.remove();
      }
    }
  }

  @Override
  public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
    resultSet.clear();
  }

  public static long getIterationCounterResult(String label) {
    ValueResult vr = getIterationValueResult(label);
    return vr == null ? 0 : (long) vr.getScore();
  }

  /**
   * Get aggregated result from iteration.
   */
  public static ValueResult getIterationValueResult(String label) {
    String effectiveLabel = SECONDARY_COUNTER_PREFIX + label;
    Collection<ValueResult> results = new ArrayList<>();
    for (Result<?> x : resultSet) {
      if (effectiveLabel.equals(x.getLabel())) {
        results.add((ValueResult) x);
      }
    }
    if (results.isEmpty()) {
      return null;
    }
    return new ValueResult.ValueResultAggregator(AggregationPolicy.SUM).aggregate(results);
  }

  @Override
  public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams,
                                                     IterationParams iterationParams,
                                                     IterationResult result) {
    System.err.println(Thread.currentThread() + " WRAP UP");
    Collection<Result<? extends Result>> results = new ArrayList<>();
    results.addAll(resultSet);
    for (Result<?> r : resultSet) {
      if (r instanceof ValueResult) {
        ValueResult cr = (ValueResult) r;
        if (cr.isWithThroughput()) {
          TimeValue time = benchmarkParams.getMeasurement().getTime();
          results.add(new ThroughputResult(ResultRole.SECONDARY, cr.getLabel() + ".throughput",
                  cr.getScore(), time.getTimeUnit().toNanos(time.getTime()), TimeUnit.SECONDS));
        }
      }
    }
    return results;
  }

  @Override
  public String getDescription() {
    return "Adds additional results gathered by the benchmark as secondary results.";
  }

}
