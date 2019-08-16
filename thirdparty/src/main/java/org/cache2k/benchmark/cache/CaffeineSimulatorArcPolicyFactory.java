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
import com.github.benmanes.caffeine.cache.simulator.policy.adaptive.ArcPolicy;
import com.typesafe.config.Config;
import org.cache2k.benchmark.EvictionTuning;

/**
 * Build a caffeine simulator ARC policy
 *
 * @author Jens Wilke
 * @see ArcPolicy
 */
public class CaffeineSimulatorArcPolicyFactory extends CaffeineSimulatorPolicyFactory<EvictionTuning.None> {

	{
		setName("arc-cs");
	}

	@Override
	protected Policy createCaffeinePolicy(final Config configWithSize) {
		return new ArcPolicy(configWithSize);
	}

}
