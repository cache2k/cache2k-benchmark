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

/**
 * How fast is <code>System.currentTimeMillis()</code>. How much do we loose
 * if put it in a separate class? Result: All variants deliver 110K ops/ms on
 * the test machine.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("unused")
public class SystemTimerBenchmark {

  @Benchmark
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Benchmark
  public long millisWithVariableClock() { return HoldImpl.clock.millis(); }

  @Benchmark
  public long millisWithConstantClock() { return HoldImplFinal.clock.millis(); }

  @Benchmark
  public long millisWithTwoClockImpls() {
    return HoldImplFinal.clock.millis() + HoldImpl0.clock.millis();
  }

  interface Clock {
    long millis();
  }

  static class ClockImpl implements Clock {
    @Override
    public long millis() {
      return System.currentTimeMillis();
    }
  }

  static class ClockImplAlways0 implements Clock {
    @Override
    public long millis() {
      return 0;
    }
  }

  static class HoldImpl {
    static Clock clock = new ClockImpl();
  }

  static class HoldImpl0 {
    static Clock clock = new ClockImplAlways0();
  }

  static class HoldImplFinal {
    final static Clock clock = new ClockImpl();
  }

}
