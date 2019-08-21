package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
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

import org.cache2k.benchmark.EvictionBenchmarkRunnerRule;
import org.cache2k.benchmark.EvictionTestVariation;
import org.cache2k.benchmark.PrototypeCacheFactory;
import org.cache2k.benchmark.TraceCollections;
import org.cache2k.benchmark.cache.CaffeineSimulatorOptPolicyFactory;
import org.cache2k.benchmark.cache.CaffeineStarFactory;
import org.cache2k.benchmark.prototype.evictionPolicies.Cache2kV12Eviction;
import org.cache2k.benchmark.prototype.evictionPolicies.Cache2kV14Eviction;
import org.cache2k.benchmark.traces.Traces;
import org.cache2k.benchmarks.clockProPlus.CAR;
import org.cache2k.benchmarks.clockProPlus.ClockProP;
import org.cache2k.benchmarks.clockProPlus.SimpleCacheFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Example how to use the eviction test variation builder and test runner
 * to run eviction benchmarks on a combination of caches, sizes and traces.
 *
 * @author Jens Wilke
 */
@RunWith(Parameterized.class)
public class SomeTracesWithSomeCachesBenchmark {

  public final static EvictionTestVariation.Builder CACHES = new EvictionTestVariation.Builder()
    .add(SimpleCacheFactory.of(ClockProP.class))
    .add(SimpleCacheFactory.of(CAR.class))
    .add(PrototypeCacheFactory.of(Cache2kV14Eviction.class))
    .add(new CaffeineStarFactory())
    .add(new CaffeineSimulatorOptPolicyFactory().setName("OPT"));

  public final static EvictionTestVariation.Builder TRACES =
    new EvictionTestVariation.Builder()
      .add(Traces.FINANCIAL1_1M)
      .add(Traces.OLTP, 128, 256, 512)
      .add(Traces.SCARAB_RECS);

  @ClassRule
  public static EvictionBenchmarkRunnerRule runner = new EvictionBenchmarkRunnerRule()
    .candidateAndPeers(CACHES)
    .setReadStoredResults(false);

  @Parameterized.Parameters(name="{0}")
  public static Iterable<? extends Object> data() {
    return new EvictionTestVariation.Builder().merge(CACHES).merge(TRACES).build();
  }

  private EvictionTestVariation variation;

  public SomeTracesWithSomeCachesBenchmark(final EvictionTestVariation variation) {
    this.variation = variation;
  }

  @Test
  public void test() {
    runner.runBenchmark(variation);
  }

}
