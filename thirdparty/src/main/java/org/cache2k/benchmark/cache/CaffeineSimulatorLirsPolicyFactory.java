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
 * Factory for Caffeines' lirs policy.
 *
 * @author Jens Wilke
 * @see LirsPolicy
 */
public class CaffeineSimulatorLirsPolicyFactory
	extends CaffeineSimulatorPolicyFactory<CaffeineSimulatorLirsPolicyFactory.Tuning> {

	{
		setNamePrefix("lirs-cs");
	}

	/**
	 * @see <a href="https://github.com/ben-manes/caffeine/blob/master/simulator/src/main/resources/reference.conf"/>
	 */
	@Override
	protected Policy createCaffeinePolicy(final Config configWithSize) {
		Tuning tuning = getTuning();
		String config = "lirs {\n" +
			"  # The percentage for the HOT queue\n" +
			"  percent-hot = \"" + tuning.ratioHot + "\"\n" +
			"  # The multiple of the maximum size dedicated to non-resident entries\n" +
			"  non-resident-multiplier = \"2.0\"\n" +
			"  # The percentage of the hottest entries where the stack move is skipped\n" +
			"  percent-fast-path = \"" +tuning.ratioFastPath +"\" # \"0.05\" is reasonable\n" +
			"}\n";
    Config f = ConfigFactory.parseString(config);
    return new LirsPolicy(f.withFallback(configWithSize));
	}

	public static class Tuning implements EvictionTuning {
		private double ratioHot = 0.99;
		private double ratioFastPath = 0.0;

		public Tuning(final double ratioHot) {
			this.ratioHot = ratioHot;
		}

		public Tuning(final double ratioHot, final double ratioFastPath) {
			this.ratioHot = ratioHot;
			this.ratioFastPath = ratioFastPath;
		}

		@Override
		public String toString() {
			return String.format("%.2f,%.2f", ratioHot, ratioFastPath);
		}
	}

}
