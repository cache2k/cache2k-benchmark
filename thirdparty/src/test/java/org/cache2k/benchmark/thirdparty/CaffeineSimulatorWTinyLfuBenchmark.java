package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * thirdparty
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
import com.github.benmanes.caffeine.cache.simulator.policy.irr.LirsPolicy;
import com.github.benmanes.caffeine.cache.simulator.policy.sketch.WindowTinyLfuPolicy;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.cache2k.benchmark.BenchmarkCollection;

/**
 * Window Tiny-Lfu from the Caffeine simulator. Percent-Main=0.99
 *
 * @author Jens Wilke; created: 2013-06-13
 */
public class CaffeineSimulatorWTinyLfuBenchmark extends BenchmarkCollection {

  /**
   * @see <a href="https://github.com/ben-manes/caffeine/blob/master/simulator/src/main/resources/reference.conf"/>
   */
  protected String TINY_LFU_CONFIG =
    "  # The seed for randomized operations\n" +
      "  random-seed = \"1033096058\"" +
      "\n" +
      "  tiny-lfu {\n" +
      "    # CountMinSketch: count-min-4 (4-bit), count-min-64 (64-bit)\n" +
      "    # Table: random-table, tiny-table, perfect-table\n" +
      "    sketch = \"count-min-4\"\n" +
      "\n" +
      "    # If increments are conservative by only updating the minimum counters for CountMin sketches\n" +
      "    count-min.conservative = false\n" +
      "\n" +
      "    count-min-64 {\n" +
      "      eps = \"0.0001\"\n" +
      "      confidence = \"0.99\"\n" +
      "    }\n" +
      "\n" +
      "    count-min-4 {\n" +
      "      # periodic: Resets by periodically halving all counters\n" +
      "      # incremental: Resets by halving counters in an incremental sweep\n" +
      "      reset = \"periodic\"\n" +
      "\n" +
      "      # The incremental reset interval (the number of additions before halving counters)\n" +
      "      increment = 16\n" +
      "    }\n" +
      "  }\n" +
      " window-tiny-lfu {\n" +
      "    # The percentage for the MAIN space (PROBATION + PROTECTED)\n" +
      "    percent-main = [\"0.99\"]\n" +
      "    # The percentage for the PROTECTED MAIN queue\n" +
      "    percent-main-protected = \"0.80\"\n" +
      "    # The percentage of the hottest entries where the PROTECTED move is skipped\n" +
      "    percent-fast-path = \"0.0\" # \"0.05\" is reasonable\n" +
      "  }\n";

  {
    factory =
      new CaffeineSimulatorCacheFactory()
        .config(ConfigFactory.parseString(TINY_LFU_CONFIG))
        .policy(new CaffeineSimulatorCacheFactory.PolicyFactory() {
          @Override
          public Policy create(final Config _config) {
            return WindowTinyLfuPolicy.policies(_config).iterator().next();
          }
        });
  }

}
