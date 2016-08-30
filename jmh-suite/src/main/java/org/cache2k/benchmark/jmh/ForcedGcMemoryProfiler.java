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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
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

  static boolean virtualMachineLoaded;
  static Object virtualMachine;

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
      System.err.println();
      System.err.println("Heap histogram after memory settled: ");
      printHeapHisto(System.out, 30);
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

  private static void printHeapHisto(PrintStream out, int _maxLines) {
    Object obj = getJvmVirtualMachine();
    if (obj == null) {
      return;
    }
    try {
      Method heapHistoMethod = obj.getClass().getMethod("heapHisto", Object[].class);
      InputStream in = (InputStream) heapHistoMethod.invoke(obj, new Object[] { new Object[] { "-all" } });
      LineNumberReader r = new LineNumberReader(new InputStreamReader(in));
      String s;
      while ((s = r.readLine()) != null) {
        out.println(s);
        if (r.getLineNumber() > _maxLines) {
          break;
        }
      }
      r.close();
      in.close();
    } catch (Exception ex) {
      System.err.println("ForcedGcMemoryProfiler: error attaching");
      ex.printStackTrace();
    }
  }

  private static boolean attachingTried;
  private static Object attachedVm;

  /**
   * Attach to our virtual machine via the attach API and return the
   * instance of type {@code com.sun.tools.attach.VirtualMachine}
   */
  private static synchronized Object getJvmVirtualMachine() {
    if (attachingTried) {
      return attachedVm;
    }
    attachingTried = true;
    Class<?> vmClass = findAttachVmClass();
    Integer pid = getProcessId();
    if (vmClass != null && pid != null) {
      try {
        Method m = vmClass.getMethod("attach", String.class);
        attachedVm = m.invoke(vmClass, Integer.toString(pid));
      } catch (Throwable ex) {
        System.err.println("ForcedGcMemoryProfiler: error attaching via attach API");
        ex.printStackTrace();
        return null;
      }
    }
    return attachedVm;
  }

  /**
   * Loads java VirtualMachine, expects that tools.jar is reachable via JAVA_HOME environment
   * variable.
   */
  private static Class<?> findAttachVmClass() {
    final String virtualMachineClassName = "com.sun.tools.attach.VirtualMachine";
    try {
      return Class.forName(virtualMachineClassName);
    } catch (ClassNotFoundException ignore) {
    }
    String _javaHome = System.getenv("JAVA_HOME");
    if (_javaHome == null) {
      System.err.println("ForcedGcMemoryProfiler: tools.jar missing? Add JAVA_HOME.");
      return null;
    }
    File f = new File(new File(_javaHome, "lib"), "tools.jar");
    if (!f.exists()) {
      System.err.println("ForcedGcMemoryProfiler: tools.jar not found in JAVA_HOME/lib/tools.jar.");
      return null;
    }
    try {
      final URL url = f.toURI().toURL();
      ClassLoader cl = URLClassLoader.newInstance(new URL[]{url});
      return Class.forName(virtualMachineClassName, true, cl);
    } catch (Exception ex) {
      System.err.println("ForcedGcMemoryProfiler: Cannot load " + virtualMachineClassName + " from " + f);
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * Hack to obtain process ID. Should work on Unix/Linux and Windows.
   */
  private static Integer getProcessId() {
    try {
      java.lang.management.RuntimeMXBean _runtimeMXBean =
        java.lang.management.ManagementFactory.getRuntimeMXBean();
      java.lang.reflect.Field jvm = _runtimeMXBean.getClass().getDeclaredField("jvm");
      jvm.setAccessible(true);
      sun.management.VMManagement mgm = (sun.management.VMManagement) jvm.get(_runtimeMXBean);
      java.lang.reflect.Method _method = mgm.getClass().getDeclaredMethod("getProcessId");
      _method.setAccessible(true);
      return (Integer) _method.invoke(mgm);
    } catch (Exception ex) {
      System.err.println("ForcedGcMemoryProfiler: error obtaining PID");
      ex.printStackTrace();
    }
    return null;
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
