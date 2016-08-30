package org.cache2k.benchmark.jmh.suite.noEviction.symmetrical;

/*
 * #%L
 * Cache benchmark suite based on JMH.
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
 * Populate a cache of type Integer,Integer with 1M, 2M, 4M, 8M entries.
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
