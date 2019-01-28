package org.cache2k.benchmark.jmh;

/*
 * #%L
 * Benchmarks: JMH suite.
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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ScalarResult;

import java.io.ByteArrayOutputStream;
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

/**
 * Record the used heap memory of a benchmark iteration by forcing a full garbage collection.
 *
 * @author Jens Wilke
 */
public class ForcedGcMemoryProfiler implements InternalProfiler {

  @SuppressWarnings("unused")
  private static Object keepReference;
  private static long usedMemory;
  private static long usedMemorySettled;
  private static long totalMemory;
  private static long gcTimeMillis;
  private static long usedHeapMemory = -1;
  private static volatile boolean enabled = false;

  /**
   * When benchmarking caches that don't have a cache manager we need to ensure that
   * the reference to the cache/benchmark is kept until we are finished with memory
   * consumption measurements.
   */
  public static void keepReference(Object _rootReferenceToKeep) {
    if (enabled) {
      keepReference = _rootReferenceToKeep;
    }
  }

  /**
   * Called from the benchmark when the objects are still referenced to record the
   * used memory. This enforces a full garbage collection.
   */
  public static void recordUsedMemory() {
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
    if (!PlatformUtil.isJava9()) {
      usedHeapMemory = printHeapHisto(System.out, 30);
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
      System.err.println("WARNING: MXBeans can not report GC info. Cannot extract reliable usage metric.");
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
      long _gcCount;
      if ((_gcCount = countGc(_enabledBeans)) > _beforeGcCount) {
        MemoryUsage _heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage _nonHeapUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        totalMemory = _heapUsage.getCommitted() + _nonHeapUsage.getCommitted();
        long _usedHeapMemory = _heapUsage.getUsed();
        long _usedNonHeap = _nonHeapUsage.getUsed();
        System.err.println("[getMemoryMXBean] usedHeap=" + _usedHeapMemory + ", usedNonHeap=" + _usedNonHeap + ", totalUsed=" + (_usedHeapMemory + _usedNonHeap) + ", gcCount=" + _gcCount);
        System.err.println("[Runtime totalMemory-freeMemory] used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        return _usedHeapMemory + _usedNonHeap;
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


  /**
   * oracles' java doc says that remoteDataDump outputs the same as ctrl-break, however,
   * heap information is missing and only threads are printed.
   */
  private static void dumpThreads(PrintStream out) {
    Object obj = getJvmVirtualMachine();
    if (obj == null) {
      return;
    }
    try {
      Method heapHistoMethod = obj.getClass().getMethod("remoteDataDump", Object[].class);
      InputStream in = (InputStream) heapHistoMethod.invoke(obj, new Object[] { new Object[] {} });
      LineNumberReader r = new LineNumberReader(new InputStreamReader(in));
      String s;
      while ((s = r.readLine()) != null) {
        out.println(s);
      }
      r.close();
      in.close();
    } catch (Exception ex) {
      System.err.println("ForcedGcMemoryProfiler: error attaching / reading histogram");
      ex.printStackTrace();
    }
    out.println();
  }

  private static long printHeapHisto(PrintStream out, int _maxLines) {
    Object obj = getJvmVirtualMachine();
    if (obj == null) {
      return 0;
    }
    boolean _partial = false;
    long _totalBytes = 0;
    try {
      Method heapHistoMethod = obj.getClass().getMethod("heapHisto", Object[].class);
      InputStream in = (InputStream) heapHistoMethod.invoke(obj, new Object[] { new Object[] { "-live" } });
      LineNumberReader r = new LineNumberReader(new InputStreamReader(in));
      String s;
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(buffer);
      while ((s = r.readLine()) != null) {
        if ( s.startsWith("Total")) {
          ps.println(s);
          String[] sa = s.split("\\s+");
          _totalBytes = Long.parseLong(sa[2]);
        } else if (r.getLineNumber() <= _maxLines) {
          ps.println(s);
        } else {
          if (!_partial) {
            ps.println("[ ... truncated ... ]");
          }
          _partial = true;
        }
      }
      r.close();
      in.close();
      ps.close();
      byte[] _histoOuptut = buffer.toByteArray();
      buffer = new ByteArrayOutputStream();
      ps = new PrintStream(buffer);
      ps.println("[Heap Histogram Live Objects] used=" + _totalBytes);
      ps.write(_histoOuptut);
      ps.println();
      ps.close();
      out.write(buffer.toByteArray());
    } catch (Exception ex) {
      System.err.println("ForcedGcMemoryProfiler: error attaching / reading histogram");
      ex.printStackTrace();
    }
    return _totalBytes;
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
    Long pid = PlatformUtil.getProcessId();
    if (vmClass != null && pid != null) {
      try {
        Method m = vmClass.getMethod("attach", String.class);
        attachedVm = m.invoke(vmClass, Long.toString(pid));
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
   * Parse the linux {@code /proc/self/status} and add everything prefixed with "Vm" as metric to
   * the profiling result.
   */
  private static void addLinuxVmStats(List<Result> l) {
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
          new ScalarResult("+forced-gc-mem.used." + _name, (double) Long.parseLong(sa[1]), "kB", AggregationPolicy.AVG)
        );
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams, final IterationResult result) {
    if (usedMemory <= 0) {
      recordUsedMemory();
      if (usedMemory <= 0) {
        return Collections.emptyList();
      }
    }
    List<Result> l = new ArrayList<>();
    addLinuxVmStats(l);
    l.addAll(Arrays.asList(
      new ScalarResult("+forced-gc-mem.used.settled", (double) usedMemorySettled, "bytes", AggregationPolicy.AVG),
      new ScalarResult("+forced-gc-mem.used.after", (double) usedMemory, "bytes", AggregationPolicy.AVG),
      new ScalarResult("+forced-gc-mem.total", (double) totalMemory, "bytes", AggregationPolicy.AVG),
      new ScalarResult("+forced-gc-mem.gcTimeMillis", (double) gcTimeMillis, "ms", AggregationPolicy.AVG),
      new ScalarResult("+forced-gc-mem.usedHeap", (double) usedHeapMemory, "bytes", AggregationPolicy.AVG)
    ));
    keepReference = null;
    return l;
  }

  @Override
  public void beforeIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams) {
    usedMemory = -1;
    enabled = true;
  }

  @Override
  public String getDescription() {
    return "Adds used memory to the result, if recorded via recordUsedMemory()";
  }

}
