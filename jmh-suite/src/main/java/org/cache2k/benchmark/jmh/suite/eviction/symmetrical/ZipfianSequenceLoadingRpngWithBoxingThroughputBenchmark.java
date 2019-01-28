package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
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

import org.cache2k.benchmark.jmh.BenchmarkBase;
import org.cache2k.benchmark.jmh.ForcedGcMemoryProfiler;
import org.cache2k.benchmark.util.ZipfianPattern;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

/**
 * Just test the throughput of the RPNG when setup identically to the {@link ZipfianSequenceLoadingBenchmark}
 * together with boxing of an integer object.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class ZipfianSequenceLoadingRpngWithBoxingThroughputBenchmark extends BenchmarkBase {

  @Param({"5", "10", "20"})
  public int factor = 0;

  @Param({"100000", "1000000", "10000000"})
  public int entryCount = 100_000;

  /** Use thread safe RPNG to give each thread state another seed. */
  final Random offsetSeed = new Random(1802);

  @State(Scope.Thread)
  public static class ThreadState {

    ZipfianPattern pattern;

    @Setup(Level.Iteration)
    public void setup(ZipfianSequenceLoadingRpngWithBoxingThroughputBenchmark _benchmark) {
      pattern = new ZipfianPattern(_benchmark.offsetSeed.nextLong(),
        _benchmark.entryCount * _benchmark.factor);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
      pattern = null;
    }
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    ForcedGcMemoryProfiler.keepReference(this);
    System.out.println("availableProcessors: " + Runtime.getRuntime().availableProcessors());
  }

  @Benchmark @BenchmarkMode(Mode.Throughput)
  public long operation(ThreadState threadState, HitCountRecorder rec) {
    rec.opCount++;
    Integer i = threadState.pattern.next();
    return System.identityHashCode(i);
  }

}
