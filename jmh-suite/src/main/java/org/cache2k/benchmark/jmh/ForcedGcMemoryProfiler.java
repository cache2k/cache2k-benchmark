package org.cache2k.benchmark.jmh;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
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

  /**
   * Parse the linux {@code /proc/self/status} and add everything prefixed with "Vm" as metric to
   * the profiling result.
   */
  private static void addLinuxVmStats(List<ProfilerResult> l) {
    try {
      LineNumberReader r = new LineNumberReader(new InputStreamReader(new FileInputStream("/proc/self/status")));
      String _line;
      while ((_line = r.readLine()) != null) {
        if (!_line.startsWith("Vm")) {
          continue;
        }
        String[] sa = _line.split("\\s+");
        if (sa.length != 3) {
          throw new IOException("Format error: 3 elements expected");
        }
        if (!sa[2].equals("kB")) {
          throw new IOException("Format error: unit kB expected, was: " + sa[2]);
        }
        String _name = sa[0].substring(0, sa[0].length() - 1);
        l.add(
          new ProfilerResult("+forced-gc-mem.used." + _name, (double) Long.parseLong(sa[1]), "kB", AggregationPolicy.AVG)
        );
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams, final IterationResult result) {
    if (usedMemory == 0) {
      return Collections.emptyList();
    }
    List<ProfilerResult> l = new ArrayList<>();
    addLinuxVmStats(l);
    l.addAll(Arrays.asList(
      new ProfilerResult("+forced-gc-mem.used.settled", (double) usedMemorySettled, "bytes", AggregationPolicy.AVG),
      new ProfilerResult("+forced-gc-mem.used.after", (double) usedMemory, "bytes", AggregationPolicy.AVG),
      new ProfilerResult("+forced-gc-mem.total", (double) totalMemory, "bytes", AggregationPolicy.AVG),
      new ProfilerResult("+forced-gc-mem.gcTimeMillis", (double) gcTimeMillis, "ms", AggregationPolicy.AVG)
    ));
    return l;
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
