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
