package org.cache2k.benchmark.cache;

/*
 * #%L
 * Benchmarks: third party products.
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

import com.github.benmanes.caffeine.cache.simulator.policy.AccessEvent;
import com.github.benmanes.caffeine.cache.simulator.policy.Policy;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.cache2k.benchmark.EvictionTuning;
import org.cache2k.benchmark.SimulatorPolicy;
import org.cache2k.benchmark.SimulatorPolicyFactory;
import org.cache2k.benchmark.EvictionStatistics;

/**
 * Integrates the caffeine simulator.
 *
 * @author Jens Wilke
 */
public abstract class CaffeineSimulatorPolicyFactory<T extends EvictionTuning> extends SimulatorPolicyFactory<T> {

  protected abstract Policy createCaffeinePolicy(Config configWithSize);

  @Override
  public SimulatorPolicy create(final int capacity) {
    String config = "maximum-size = " + capacity + "\n";
    Config f = ConfigFactory.parseString(config);
    return new Adapter(createCaffeinePolicy(f));
  }

  private static class Adapter implements SimulatorPolicy {

    private boolean finishCalled;
    private Policy policy;

    private Adapter(final Policy policy) {
      this.policy = policy;
    }

    @Override
    public void record(Integer v) {
      policy.record(AccessEvent.forKey(v));
    }

    @Override
    public long getMissCount() {
      if (!finishCalled) {
        policy.finished();
        finishCalled = true;
      }
      return policy.stats().missCount();
    }

    @Override
    public void close() {
      if (!finishCalled) {
        policy.finished();
        finishCalled = true;
      }
    }

    @Override
    public String toString() {
      return policy.getClass().getSimpleName() + ", " + policy + ", stats=" + policy.stats();
    }

    @Override
    public EvictionStatistics getEvictionStatistics() {
      return new EvictionStatistics() {
        @Override
        public long getEvictionCount() {
          return policy.stats().evictionCount();
        }
      };
    }
  }

}
