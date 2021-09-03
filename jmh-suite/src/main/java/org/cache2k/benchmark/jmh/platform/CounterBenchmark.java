package org.cache2k.benchmark.jmh.platform;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class CounterBenchmark {

  static Random random = new Random();

  long synchronizedCounter1;
  long synchronizedCounter2;
  AtomicLong counter1 = new AtomicLong();
  AtomicLong counter2 = new AtomicLong();
  LongAdder adderCounter1 = new LongAdder();
  LongAdder adderCounter2 = new LongAdder();

  final int spreadSize = 10;
  AtomicLongArray spreadCounter = new AtomicLongArray(spreadSize);
  AtomicLongArray spreadCounter2 = new AtomicLongArray(spreadSize);

  @State(Scope.Thread)
  public static class IndexState {
    long index = random.nextInt(1234);
  }

  @Benchmark
  public long incrementAtomicLongSingle() {
    return counter1.incrementAndGet();
  }

  @Benchmark
  public long incrementSynchronizedSingle() {
    synchronized (this) {
      return synchronizedCounter1++;
    }
  }

  @Benchmark @Threads(10)
  public long incrementAtomicLong10Threads() {
    return counter1.incrementAndGet();
  }

  @Benchmark @Threads(10)
  public long increment2AtomicLong10Threads() {
    return counter1.incrementAndGet() + counter2.incrementAndGet();
  }

  @Benchmark @Threads(10)
  public long increment2Synchronized10Threads() {
    synchronized (this) {
      return synchronizedCounter1++ + synchronizedCounter2++;
    }
  }

  @Benchmark @Threads(10)
  public long incrementLongAdder10Threads() {
    adderCounter1.increment();
    return 4711;
  }

  @Benchmark @Threads(10)
  public long increment2LongAdder10Threads() {
    adderCounter1.increment();
    adderCounter2.increment();
    return 4711;
  }

  @Benchmark @Threads(10)
  public long incrementSpread(IndexState s) {
    return spreadCounter.incrementAndGet((int) (s.index++ % spreadSize));
  }

  @Benchmark @Threads(10)
  public long increment2Spread(IndexState s) {
    return spreadCounter.incrementAndGet((int) (s.index++ % spreadSize)) +
      spreadCounter2.incrementAndGet((int) (s.index++ % spreadSize));
  }

}
