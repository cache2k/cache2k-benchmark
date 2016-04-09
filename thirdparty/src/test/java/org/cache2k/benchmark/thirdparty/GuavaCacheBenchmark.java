package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * thirdparty
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

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.cache2k.benchmark.BenchmarkCollection;
import org.junit.Test;

/**
 * Runs the benchmark collection on Google Guava.
 *
 * @author Jens Wilke; created: 2013-12-01
 */
public class GuavaCacheBenchmark extends BenchmarkCollection {

  {
    factory = new GuavaCacheFactory();
  }

  /**
   * We expect the original test to fail for Google Guava.
   * As of 20131210, test fails with: expected:&lt;1001> but was:&lt;1764>
   */
  @Test(expected=AssertionError.class)
  @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
  public void testSize1000() throws Exception { 
    super.testSize1000();
  }

}
