package org.cache2k.benchmark.thirdparty;

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

import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.EvictionMatrixTestBase;
import org.cache2k.benchmark.EvictionTestVariation;
import org.cache2k.benchmark.TraceCollections;
import org.cache2k.benchmark.cache.CaffeineStarFactory;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Jens Wilke
 */
@RunWith(Parameterized.class)
public class RunAllWithTheOne extends EvictionMatrixTestBase {

	private static BenchmarkCacheFactory resolveFactory() {
		String cacheFactoryParam = "benchmark.cache";
		String name = System.getProperty(cacheFactoryParam);
		if (name == null) {
			throw new IllegalArgumentException("Cache factory missing: " + cacheFactoryParam);
		}
		if (!name.contains(".")) {
			name = CaffeineStarFactory.class.getPackage().getName() + "." + name;
		}
		try {
			return (BenchmarkCacheFactory) Class.forName(name).newInstance();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public final static EvictionTestVariation.Builder CONFIGURED_CACHE = new EvictionTestVariation.Builder()
		.add(resolveFactory());

	@Parameterized.Parameters(name="{0}")
	public static Iterable<? extends Object> data() {
		return new EvictionTestVariation.Builder().merge(CONFIGURED_CACHE).merge(TraceCollections.ALL_TRACES).build();
	}

	public RunAllWithTheOne(final Object point) {
		super((EvictionTestVariation) point);
	}

}
