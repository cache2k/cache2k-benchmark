package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
 * %%
 * Copyright (C) 2013 - 2018 headissue GmbH, Munich
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
import com.github.benmanes.caffeine.cache.simulator.policy.irr.LirsPolicy;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.cache2k.benchmark.BenchmarkCollection;

/**
 * @author Jens Wilke; created: 2013-06-13
 */
public class CaffeineSimulatorLirsBenchmark extends BenchmarkCollection {

  /**
   * @see <a href="https://github.com/ben-manes/caffeine/blob/master/simulator/src/main/resources/reference.conf"/>
   */
  public final String LIRS_CONFIG =
    "lirs {\n" +
    "  # The percentage for the HOT queue\n" +
    "  percent-hot = \"0.99\"\n" +
    "  # The multiple of the maximum size dedicated to non-resident entries\n" +
    "  non-resident-multiplier = \"2.0\"\n" +
    "  # The percentage of the hottest entries where the stack move is skipped\n" +
    "  percent-fast-path = \"0.0\" # \"0.05\" is reasonable\n" +
    "}\n";

  {
    factory =
      new CaffeineSimulatorCacheFactory()
        .config(ConfigFactory.parseString(LIRS_CONFIG))
        .policy(new CaffeineSimulatorCacheFactory.PolicyFactory() {
          @Override
          public Policy create(final Config _config) {
            return new LirsPolicy(_config);
          }
        });
  }

}
