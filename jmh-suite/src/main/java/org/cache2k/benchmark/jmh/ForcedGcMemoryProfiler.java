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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Record the used heap memory during a test run by forcing a full garbage collection.
 *
 * @author Jens Wilke
 */
public class ForcedGcMemoryProfiler implements InternalProfiler {

  static boolean enable;
  static long usedMemory;
  static long totalMemory;

  /**
   * Called from the benchmark when the objects are still referenced to record the
   * used memory. This enforces a full garbage collection.
   */
  public static void recordUsedMemory() {
    if (enable) {
      long m2 =  getUsedMemory();
      do {
        usedMemory = m2;
        m2 = getUsedMemory();
      } while (m2 < usedMemory);
    }
  }

  private static long getUsedMemory() {
    final AtomicBoolean finalizerRan = new AtomicBoolean(false);
    WeakReference<Object> ref = new WeakReference<Object>(
      new Object() {
        @Override protected void finalize() { finalizerRan.set(true); }
      });
    while (ref.get() != null && !finalizerRan.get()) {
      System.gc();
      System.runFinalization();
    }
    MemoryUsage mu = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    totalMemory = mu.getCommitted();
    return mu.getUsed();

  }

  @Override
  public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams, final IterationResult result) {
    if (usedMemory == 0) {
      return Collections.emptyList();
    }
    return Arrays.asList(
      new ProfilerResult("+forced-gc-mem.used", (double) usedMemory, "bytes", AggregationPolicy.MAX),
      new ProfilerResult("+forced-gc-mem.total", (double) totalMemory, "bytes", AggregationPolicy.MAX)
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
