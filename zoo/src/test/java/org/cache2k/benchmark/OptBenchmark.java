package org.cache2k.benchmark;

/*
 * #%L
 * zoo
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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.cache2k.benchmark.util.OptimumReplacementCalculation;
import org.junit.Ignore;

/**
 * Calculate the maximum hit rate possible by using a cache with the size of the
 * of the trace value space.
 *
 * @author Jens Wilke; created: 2013-06-13
 */
@Ignore("use Caffeine for faster opt reference calculation")
public class OptBenchmark extends RandomCacheBenchmark {

  @Override
  public BenchmarkCache<Integer, Integer> freshCache(int _maxElements) {
    return new MyCache(_maxElements);
  }

  public static class MyCache extends BenchmarkCache<Integer, Integer> implements SimulatorPolicy {

    IntList trace = new IntArrayList();
    int size;

    public MyCache(final int _size) {
      size = _size;
    }

    @Override
    public long getMissCount() {
      return
        trace.size() -
        new OptimumReplacementCalculation(size, trace.toIntArray()).getHitCount();
    }

    @Override
    public void record(final Integer v) {
      trace.add(v);
    }

    @Override
    public void destroy() {

    }

    @Override
    public int getCacheSize() {
      return size;
    }
  }

}
