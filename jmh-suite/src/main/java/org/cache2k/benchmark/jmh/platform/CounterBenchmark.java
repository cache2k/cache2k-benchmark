package org.cache2k.benchmark.jmh.platform;

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
