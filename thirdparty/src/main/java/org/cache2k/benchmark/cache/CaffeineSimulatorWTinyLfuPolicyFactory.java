package org.cache2k.benchmark.cache;

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

import com.github.benmanes.caffeine.cache.simulator.policy.Policy;
import com.github.benmanes.caffeine.cache.simulator.policy.irr.LirsPolicy;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.cache2k.benchmark.EvictionTuning;

/**
 * Factory for Caffeines' WTiny-LFU policy.
 *
 * @author Jens Wilke
 * @see com.github.benmanes.caffeine.cache.simulator.policy.sketch.WindowTinyLfuPolicy
 */
public class CaffeineSimulatorWTinyLfuPolicyFactory
	extends CaffeineSimulatorPolicyFactory<CaffeineSimulatorWTinyLfuPolicyFactory.Tuning> {

	{
		setNamePrefix("wtinylfu-cs");
	}

	/**
	 * @see <a href="https://github.com/ben-manes/caffeine/blob/master/simulator/src/main/resources/reference.conf"/>
	 */
	@Override
	protected Policy createCaffeinePolicy(final Config configWithSize) {
		Tuning tuning = getTuning();
		String config =
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
			"    percent-main = [\"" + tuning.percentMain + "\"]\n" +
			"    # The percentage for the PROTECTED MAIN queue\n" +
			"    percent-main-protected = \"0.80\"\n" +
			"    # The percentage of the hottest entries where the PROTECTED move is skipped\n" +
			"    percent-fast-path = \"0.0\" # \"0.05\" is reasonable\n" +
			"  }\n";
    Config f = ConfigFactory.parseString(config);
    return new LirsPolicy(f.withFallback(configWithSize));
	}

	public static class Tuning implements EvictionTuning {
		private double percentMain = 0.99;

		public Tuning(final double percentMain) {
			this.percentMain = percentMain;
		}

		@Override
		public String toString() {
			return String.format("%.2f", percentMain);
		}
	}

}
