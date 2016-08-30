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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.Cache2kFactory;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Base for all JMH cache benchmarks, controlling the cache lifecycle and
 * recording memory usage.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class BenchmarkBase {

  protected BenchmarkCache getsDestroyed;

  @Param("DEFAULT")
  public String cacheFactory;

  public BenchmarkCacheFactory getFactory() {
    try {
      if ("DEFAULT".equals(cacheFactory)) {
        cacheFactory = Cache2kFactory.class.getCanonicalName();
      }
      BenchmarkCacheFactory _factoryInstance =
        (BenchmarkCacheFactory) Class.forName(cacheFactory).newInstance();
      return _factoryInstance;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @TearDown(Level.Iteration)
  public void tearDownBase() throws Exception {
    ForcedGcMemoryProfiler.recordUsedMemory();
    if (getsDestroyed != null) {
      System.out.println();
      System.out.println(getsDestroyed.getStatistics());
      System.out.println("availableProcessors: " + Runtime.getRuntime().availableProcessors());
      getsDestroyed.destroy();
      getsDestroyed = null;
    }
  }

}
