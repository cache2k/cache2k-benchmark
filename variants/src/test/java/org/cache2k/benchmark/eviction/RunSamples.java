package org.cache2k.benchmark.eviction;

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

import org.cache2k.benchmark.Cache2k2015Factory;
import org.cache2k.benchmark.EvaluationCacheFactory;
import org.cache2k.benchmark.EvictionBenchmarkRunnerRule;
import org.cache2k.benchmark.EvictionTestVariation;
import org.cache2k.benchmark.TraceCollections;
import org.cache2k.benchmark.eviction.evaluationCache.ArcEviction;
import org.cache2k.benchmark.eviction.evaluationCache.CarEviction;
import org.cache2k.benchmark.eviction.evaluationCache.ClockEviction;
import org.cache2k.benchmark.eviction.evaluationCache.LruEviction;
import org.cache2k.benchmark.impl2015.ArcCache;
import org.cache2k.benchmark.impl2015.CarCache;
import org.cache2k.benchmark.impl2015.ClockCache;
import org.cache2k.benchmark.impl2015.LruCache;
import org.cache2k.benchmark.traces.Traces;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Jens Wilke
 */
@RunWith(Parameterized.class)
public class RunSamples {

	@ClassRule
	public static EvictionBenchmarkRunnerRule runner = new EvictionBenchmarkRunnerRule()
		.candidate("new")
		.peers("old");

	public final static EvictionTestVariation.Builder CACHES = new EvictionTestVariation.Builder()
		.add(
			new EvaluationCacheFactory().withEviction(() -> new CarEviction())
			.setName("new")
		)
		.add(
			new Cache2k2015Factory().implementation(CarCache.class)
			.setName("old")
		)
		;

	public final static EvictionTestVariation.Builder TRACES =
		new EvictionTestVariation.Builder()
			.add(Traces.SPRITE)
			.add(Traces.CPP)
			.add(Traces.MULTI2)
			.add(Traces.GLIMPSE)
			.add(Traces.OLTP)
			.add(Traces.ORM_BUSY)
			.add(Traces.RANDOM_1000_100K)
			.add(Traces.RANDOM_1000_10K);

	@Parameterized.Parameters(name="{0}")
	public static Iterable<? extends Object> data() {
		return new EvictionTestVariation.Builder().merge(CACHES).merge(TRACES).build();
	}

	private EvictionTestVariation variation;

	public RunSamples(final EvictionTestVariation variation) {
		this.variation = variation;
	}

	@Test
	public void test() {
		runner.runBenchmark(variation);
	}

}
