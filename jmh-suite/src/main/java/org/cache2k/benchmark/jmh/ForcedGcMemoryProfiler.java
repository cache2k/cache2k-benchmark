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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Record the used heap memory of a benchmark iteration by forcing a full garbage collection.
 *
 * @author Jens Wilke
 */
public class ForcedGcMemoryProfiler implements InternalProfiler {

  static boolean enable;
  static long usedMemory;
  static long usedMemorySettled;
  static long totalMemory;
  static long gcTimeMillis;

  /**
   * Called from the benchmark when the objects are still referenced to record the
   * used memory. This enforces a full garbage collection.
   */
  public static void recordUsedMemory() {
    if (enable) {
      long t0 = System.currentTimeMillis();
      long m2 = usedMemory = getUsedMemory();
      do {
        try {
          Thread.sleep(567);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        usedMemorySettled = m2;
        m2 = getUsedMemory();
      } while (m2 < usedMemorySettled);
      gcTimeMillis = System.currentTimeMillis() - t0;
    }
  }

  /**
   * Trigger a gc, wait for completion and return used memory. Inspired from JMH approach.
   *
   * <p>Before we had the approach of detecting the clearing of
   * a weak reference. Maybe this is not reliable, since when cleared the GC run may not
   * be finished.
   *
   * @see org.openjdk.jmh.runner.BaseRunner#runSystemGC()
   */
  private static long getUsedMemory() {
    final int MAX_WAIT_MSEC = 20 * 1000;
    List<GarbageCollectorMXBean> _enabledBeans = new ArrayList<GarbageCollectorMXBean>();
    for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
      long count = b.getCollectionCount();
      if (count != -1) {
        _enabledBeans.add(b);
      }
    }
    if (_enabledBeans.isEmpty()) {
      System.err.println("WARNING: MXBeans can not report GC info. System.gc() invoked, pessimistically waiting " + MAX_WAIT_MSEC + " msecs");
      try {
        TimeUnit.MILLISECONDS.sleep(MAX_WAIT_MSEC);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return -1;
    }
    long _beforeGcCount = countGc(_enabledBeans);
    long t0 = System.currentTimeMillis();
    System.gc();
    while (System.currentTimeMillis() - t0 < MAX_WAIT_MSEC) {
      try {
        Thread.sleep(234);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      if (countGc(_enabledBeans) > _beforeGcCount) {
        MemoryUsage mu = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        totalMemory = mu.getCommitted();
        return mu.getUsed();
      }
    }
    System.err.println("WARNING: System.gc() was invoked but couldn't detect a GC occurring, is System.gc() disabled?");
    return -1;
  }

  private static long countGc(final List<GarbageCollectorMXBean> _enabledBeans) {
    long cnt = 0;
    for (GarbageCollectorMXBean bean : _enabledBeans) {
      cnt += bean.getCollectionCount();
    }
    return cnt;
  }

  @Override
  public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams, final IterationResult result) {
    if (usedMemory == 0) {
      return Collections.emptyList();
    }
    return Arrays.asList(
      new ProfilerResult("+forced-gc-mem.used.settled", (double) usedMemorySettled, "bytes", AggregationPolicy.AVG),
      new ProfilerResult("+forced-gc-mem.used.after", (double) usedMemory, "bytes", AggregationPolicy.AVG),
      new ProfilerResult("+forced-gc-mem.total", (double) totalMemory, "bytes", AggregationPolicy.AVG),
      new ProfilerResult("+forced-gc-mem.gcTimeMillis", (double) gcTimeMillis, "ms", AggregationPolicy.AVG)
    );
  }

  @Override
  public void beforeIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams) {
    enable = true;
  }

  @Override
  public String getDescription() {
    return "Adds used memory to the result, if recorded via recordUsedMemory()";
  }

}
