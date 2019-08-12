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

import org.cache2k.benchmark.eviction.Cache2kV12Eviction;
import org.cache2k.benchmark.traces.CacheAccessTraceOrmAccessNight;
import org.junit.Ignore;
import org.junit.Test;

/**
 * For testing two evictions variants via IDE.
 *
 * @author Jens Wilke
 */
@Ignore
public class CompareTwoTest {

	final int steps = 50000;

	@Test
	public void test1() {
		BenchmarkCache<Integer, Integer> c =
			new Cache2kForEvictionBenchmarkFactory().createUnspecialized(50);
		TracesAndTestsCollection.runBenchmark(c, CacheAccessTraceOrmAccessNight.getInstance(), steps);
		System.out.println(c);
	}

	@Test
	public void test2() {
		BenchmarkCache<Integer, Integer> c =
			new ExperimentalEvictionCacheFactory()
				.eviction(Cache2kV12Eviction::new).createUnspecialized(50);
		TracesAndTestsCollection.runBenchmark(c, CacheAccessTraceOrmAccessNight.getInstance(), steps);
		System.out.println(c);
	}

}
