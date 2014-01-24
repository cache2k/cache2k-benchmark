package org.cache2k.benchmark;

/*
 * #%L
 * cache2k-benchmark-zoo
 * %%
 * Copyright (C) 2013 - 2014 headissue GmbH, Munich
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

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * Benchmark to see how just a HashMap performs on this system.
 *
 * @author Jens Wilke; created: 2013-11-24
 */
@BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 2)
public class HashMapReferenceBenchmark extends AbstractBenchmark {

  /**
   * Simulate 5 million cache accesses and 5 million minus 1000 hits and a cache size
   * of 1000 entries. The runtime is about 50ms on an X220, that is to low to
   * be accurate however we want to have a comparable result to the other benchmarks
   * in the hit case.
   */
  @Test
  public void benchmarkHashMapHit() {
   Cache2kFactory.CountingDataSource<Integer,Integer> g = new Cache2kFactory.CountingDataSource<Integer,Integer>();
    HashMap<Integer, Integer> map = new HashMap<>();
    int cnt = 0;
    for (int i = 0; i < 1000; i++) {
      map.put(i, g.get(i));
      cnt++;
    }
    int sum = 0;
    for (int j = 1; j < 5000; j++) {
      for (int i = 0; i < 1000; i++) {
        sum += map.get(i);
        cnt++;
      }
    }
    Assert.assertNotEquals(4711, sum);
    Assert.assertNotEquals(4711, cnt);
  }

}
