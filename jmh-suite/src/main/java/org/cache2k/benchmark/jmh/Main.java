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

import org.cache2k.benchmark.Cache2kFactory;
import org.cache2k.benchmark.ChmNoEvictionFactory;
import org.openjdk.jmh.profile.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

/**
 * Run the test suite with predefined parameters.
 */
public class Main {

  public static void main(String[] args) throws Exception {
    Main m = new Main();
    m.parseArgs(args);
    m.runTests();
  }

  static void quickOptions(OptionsBuilder ob) {
    ob.measurementTime(new TimeValue(1000, TimeUnit.MICROSECONDS)).forks(2).warmupIterations(1).measurementIterations(2);
  }

  static void dilligentOptions(OptionsBuilder ob) {
    ob.measurementTime(new TimeValue(10, TimeUnit.SECONDS)).forks(3).warmupIterations(5).measurementIterations(3);
  }

  static void addPerfAsm(OptionsBuilder ob) {
    ob.addProfiler(LinuxPerfAsmProfiler.class);
    ob.measurementIterations(5);
    ob.forks(1);
  }

  boolean quick = false;
  boolean perf = false;

  Class<?>[] benchmarksWithoutEviction =
          new Class<?>[]{Cache2kFactory.class, ChmNoEvictionFactory.class};

  public void parseArgs(String[] args) {
    for (String arg : args) {
      if ("--quick".equals(arg)) {
        quick = true;
      }
      if ("--perf".equals(arg)) {
        perf = true;
      }
    }
  }

  public void runTests() throws Exception {
    for (Class<?> c : benchmarksWithoutEviction) {
      OptionsBuilder ob = new OptionsBuilder();
      if (quick) {
        quickOptions(ob);
      } else {
        dilligentOptions(ob);
      }
      ob.timeUnit(TimeUnit.MILLISECONDS);
      ob.addProfiler(CompilerProfiler.class); // compiler statistics
      ob.addProfiler(HotspotRuntimeProfiler.class); // locks and monitors
      ob.addProfiler(GCProfiler.class); // garbage collection
      ob.jvmArgs("-server", "-Xmx2G",  "-XX:+UseG1GC",  "-XX:-UseBiasedLocking");
      if (perf) {
        addPerfAsm(ob);
      }
      Options opt = ob
              .param("cacheFactory", c.getCanonicalName())
              .build();
      new Runner(opt).run();
    }
  }

}
