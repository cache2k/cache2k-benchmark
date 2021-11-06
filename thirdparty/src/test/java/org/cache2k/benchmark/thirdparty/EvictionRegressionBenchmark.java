package org.cache2k.benchmark.thirdparty;

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

import org.cache2k.benchmark.EvictionBenchmarkRunnerRule;
import org.cache2k.benchmark.EvictionTestVariation;
import org.cache2k.benchmark.PrototypeCacheFactory;
import org.cache2k.benchmark.TraceCollections;
import org.cache2k.benchmark.prototype.evictionPolicies.C2k2xTuning;
import org.cache2k.benchmark.prototype.evictionPolicies.C2k2xEviction;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Compare the current cache2k eviction algorithm against the previous
 * one and Caffeine.
 *
 * @author Jens Wilke
 */
@RunWith(Parameterized.class)
public class EvictionRegressionBenchmark {

	public static final EvictionTestVariation.Builder CACHES = new EvictionTestVariation.Builder()
		.add(PrototypeCacheFactory.of(C2k2xEviction.class).setTuning(new C2k2xTuning.V24Tuning()))
		.add(PrototypeCacheFactory.of(C2k2xEviction.class).setTuning(new C2k2xTuning.V26Tuning()))
		// .add(new CaffeineStarFactory())
		// .add(new Cache2kStarFactory())
		;

	@ClassRule
	public static EvictionBenchmarkRunnerRule runner = new EvictionBenchmarkRunnerRule()
		.candidateAndPeers(CACHES)
		.setReadStoredResults(false);

	@Parameterized.Parameters(name="{0}")
	public static Iterable<? extends Object> data() {
		return new EvictionTestVariation.Builder()
			.merge(CACHES).merge(TraceCollections.REGRESSION_TRACES).build();
	}

	private EvictionTestVariation variation;

	public EvictionRegressionBenchmark(EvictionTestVariation variation) {
		this.variation = variation;
	}

	@Test
	public void test() {
		runner.runBenchmark(variation);
	}

}
