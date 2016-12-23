package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: implementation variants
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
