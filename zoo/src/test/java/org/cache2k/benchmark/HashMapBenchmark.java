package org.cache2k.benchmark;

/*
 * #%L
 * cache2k-benchmark-zoo
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

import org.cache2k.benchmark.zoo.HashMapBenchmarkFactory;
import org.junit.Ignore;

/**
 * @author Jens Wilke; created: 2013-06-13
 */
public class HashMapBenchmark extends BenchmarkCollection {

  {
    skipMultithreaded = true;
    factory = new HashMapBenchmarkFactory();
  }

  @Ignore @Override
  public void benchmarkMiss_500000() {}

  @Ignore @Override
  public void benchmarkMiss_50000() {}


  @Ignore @Override
  public void benchmarkEff95Threads1() throws Exception {
  }

  @Ignore @Override
  public void benchmarkEff95Threads2() throws Exception {
  }

  @Ignore @Override
  public void benchmarkEff95Threads4() throws Exception {
  }

  @Ignore @Override
  public void benchmarkEff95Threads6() throws Exception {
  }

  @Ignore @Override
  public void benchmarkEff95Threads8() throws Exception {
  }
}
