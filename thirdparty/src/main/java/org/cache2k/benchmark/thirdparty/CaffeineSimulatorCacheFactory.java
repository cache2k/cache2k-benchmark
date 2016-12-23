package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
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

import com.github.benmanes.caffeine.cache.simulator.policy.Policy;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.SimulatorPolicy;

/**
 * Integrates the caffeine simulator. This only produces meaningful results
 * for running traces. Not used for performance tests. The caffeine simulator
 * implements a bunch of different eviction algorithms. We use this for
 * comparison of the eviction strategies.
 *
 * @author Jens Wilke; created: 2013-12-08
 */
public class CaffeineSimulatorCacheFactory extends BenchmarkCacheFactory {

  PolicyFactory policy;
  Config config;

  CaffeineSimulatorCacheFactory policy(PolicyFactory f) {
    policy = f;
    return this;
  }

  CaffeineSimulatorCacheFactory config(Config cfg) {
    config = cfg;
    return this;
  }

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    MyBenchmarkCacheAdapter c = new MyBenchmarkCacheAdapter();
    c.size = _maxElements;
    String _config =
      "maximum-size = " + _maxElements + "\n";
    Config f = ConfigFactory.parseString(_config);
    c.policy = policy.create(f.withFallback(config));
    return c;
  }

  public interface PolicyFactory {

    Policy create(Config _config);

  }

  static class MyBenchmarkCacheAdapter extends BenchmarkCache<Integer, Integer> implements SimulatorPolicy {

    int size;
    Policy policy;

    @Override
    public int getCacheSize() {
      return size;
    }

    public void record(Integer v) {
      policy.record(v);
    }

    public long getMissCount() {
      policy.finished();
      return policy.stats().missCount();
    }

    @Override
    public void destroy() {
      policy.finished();
    }

    @Override
    public String getStatistics() {
      return policy.getClass().getSimpleName() + ", " + policy + ", stats=" + policy.stats();
    }

  }

}
