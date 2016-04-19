package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * thirdparty
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
