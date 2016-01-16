package org.cache2k.benchmark.jmh;

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

import org.cache2k.benchmark.jmh.platform.CounterBenchmark;
import org.cache2k.benchmark.jmh.platform.SystemTimerBenchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Run platform related benchmarks.
 */
public class Platform extends Common {

  @Override
  public void run() throws Exception {
    OptionsBuilder ob = commonOptions();
    parseArgs(ob);
    ob.include(CounterBenchmark.class.getName());
    ob.threads(4);
    new Runner(ob.build()).run();

  }

}
