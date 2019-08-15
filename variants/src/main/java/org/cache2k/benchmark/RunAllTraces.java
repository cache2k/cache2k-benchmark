package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Implementation and eviction variants
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

import org.cache2k.benchmark.cache.Cache2kStarFactory;
import org.cache2k.benchmark.traces.Traces;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Jens Wilke
 */
@RunWith(Parameterized.class)
public class RunAllTraces {

	@ClassRule
	public static EvictionBenchmarkRunnerRule rule = new EvictionBenchmarkRunnerRule()
		.peers("cache2kV12", "caffeine*");

	public final static EvictionTestVariation.Builder caches = new EvictionTestVariation.Builder()
		.add(new Cache2kStarFactory());

	public final static EvictionTestVariation.Builder traces = new EvictionTestVariation.Builder()
		.add(Traces.OLTP)
		.add(Traces.ORM_BUSY);

	@Parameterized.Parameters(name="{0}")
	public static Iterable<? extends Object> data() {
		return new EvictionTestVariation.Builder().merge(caches).merge(traces).build();
	}

	private EvictionTestVariation variation;

	public RunAllTraces(final EvictionTestVariation variation) {
		this.variation = variation;
	}

	@Test
	public void run() {
		rule.runBenchmark(variation);
	}

}
