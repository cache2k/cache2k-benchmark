package org.cache2k.benchmark.jmh.suite.noEviction.symmetrical;

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
import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Populate a cache of type Integer,Integer with 100k, 1M and 5M entries.
 * Benchmark is executed in single shot with variable threads.
 */
@State(Scope.Benchmark)
public class PopulateParallelOnceBenchmark extends BenchmarkBase {

  @Param({"1000000", "2000000", "4000000", "8000000"})
  public int size = 1000 * 1000;
  protected final AtomicInteger offset = new AtomicInteger(0);

  BenchmarkCache <Integer, Integer> cache;

  @Setup(Level.Iteration)
  public void setup() throws Exception {
    getsDestroyed = cache = getFactory().create(size);
  }

  @AuxCounters @State(Scope.Thread)
  public static class ThreadState {
    public long operations;
  }

  @Benchmark @BenchmarkMode(Mode.SingleShotTime)
  public long populateChunkInCache(ThreadState ts, BenchmarkParams p) {
    int _chunkSize = size / p.getThreads();
    int _startIndex = offset.getAndAdd(_chunkSize);
    int _endIndex = _startIndex + _chunkSize;
    for (int i = _startIndex; i < _endIndex; i++) {
      cache.put(i, i);
    }
    ts.operations = _chunkSize;
    return _chunkSize;
  }

}
