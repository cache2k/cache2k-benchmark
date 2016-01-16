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

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.profile.*;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Run the test suite with predefined parameters.
 */
public abstract class Common {

  private String[] arguments;

  public String[] getArguments() {
    return arguments;
  }

  /**
   * Arguments passed from command line.
   */
  public void setArguments(String[] v) {
    this.arguments = v;
  }

  public OptionsBuilder commonOptions() {
    OptionsBuilder ob = new OptionsBuilder();
    ob.mode(Mode.Throughput);
    ob.addProfiler(CompilerProfiler.class); // compiler statistics
    ob.addProfiler(HotspotRuntimeProfiler.class); // locks and monitors
    ob.addProfiler(GCProfiler.class); // garbage collection
    ob.jvmArgs(
      "-server",
      "-Xmx2G",
      "-XX:+PrintGC",
      "-XX:+PrintGCTimeStamps"
    );
    ob.timeUnit(TimeUnit.MILLISECONDS);
    dilligent(ob);
    return ob;
  }

  /**
   * Fast execution to check results.
   */
  @SuppressWarnings("unused")
  public void quick(OptionsBuilder ob) {
    ob.measurementTime(new TimeValue(1000, TimeUnit.MICROSECONDS)).forks(2).warmupIterations(1).measurementIterations(2);
  }

  /**
   * That't for the representative benchmark.
   */
  @SuppressWarnings("unused")
  public void dilligent(OptionsBuilder ob) {
    ob.measurementTime(new TimeValue(10, TimeUnit.SECONDS)).forks(3).warmupIterations(5).measurementIterations(5);
  }

  /**
   * Tinker benchmark options to do profiling and add assembler code output (linux only).
   * Needs additional disassembly library to display assembler code
   * see: http://psy-lob-saw.blogspot.de/2013/01/java-print-assembly.html
   * download from: https://kenai.com/projects/base-hsdis/downloads
   * install with e.g.: mv ~/Downloads/linux-hsdis-amd64.so jdk1.8.0_45/jre/lib/amd64/hsdis-amd64.so.
   *
   * <p>For profiling only do one fork, but more measurement iterations
   * <p>profilers are described here: http://java-performance.info/introduction-jmh-profilers
   </p>
   */
  @SuppressWarnings("unused")
  public void profile(OptionsBuilder ob) {
    ob.addProfiler(LinuxPerfAsmProfiler.class);
    ob.measurementIterations(5);
    ob.forks(1);
  }

  /**
   * Parse the arguments, actually we want just to modify some basic benchmark
   * options. Parses an argument and calls the method with the same name on the
   * class.
   */
  public void parseArgs(OptionsBuilder ob) {
    try {
      for (int i = 1; i < arguments.length; i++) {
        String arg = arguments[i];
        Method m = getClass().getMethod(arg, OptionsBuilder.class);
        m.invoke(this, ob);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public abstract void run() throws Exception;

}
