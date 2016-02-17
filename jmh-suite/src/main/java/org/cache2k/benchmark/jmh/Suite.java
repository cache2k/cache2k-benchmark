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
import org.cache2k.benchmark.Cache2kWithExpiryFactory;
import org.cache2k.benchmark.ConcurrentHashMapFactory;
import org.cache2k.benchmark.HashMapFactory;
import org.cache2k.benchmark.jmh.noEviction.memory.PopulateParallelOnceBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs the benchmark suite for caches, that is a set of benchmarks scenarios, times
 * a set of cache implementation variants.
 */
public class Suite extends Common {

  protected Class<?>[] implementationsNotThreadSafe =
    new Class<?>[]{HashMapFactory.class};

  /**
   * Set of implementations that do not have any eviction capabilities, e.g.
   * {@link java.util.concurrent.ConcurrentHashMap}. We only run a subset of the
   * suite against those.
   */
  protected Class<?>[] implementationsWithoutEviction =
    new Class<?>[]{
      Cache2kWithExpiryFactory.class,
      ConcurrentHashMapFactory.class};

  protected Class<?>[] implementationsWithEviction =
    new Class<?>[]{Cache2kFactory.class};

  /**
   * TODO: do we want to have an only single threaded test also?
   */

  @Override
  public void run() throws Exception {
    runSuiteThatDoesNotNeedEviction();
  }

  private void runSuiteThatDoesNotNeedEviction() throws RunnerException {
    List<Class> _implSet = new ArrayList<Class>();
    _implSet.addAll(Arrays.asList(implementationsWithEviction));
    _implSet.addAll(Arrays.asList(implementationsWithoutEviction));
    for (Class<?> c : _implSet) {
      OptionsBuilder ob = commonOptions();
      parseArgs(ob);
      ob.include("org.cache2k.benchmark.jmh.noEviction.asymmetrical..*");
      Options opt = ob
        .param("cacheFactory", c.getCanonicalName())
        .build();
      new org.openjdk.jmh.runner.Runner(opt).run();
    }

  }

}
