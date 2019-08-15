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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.cache.JCacheCacheFactory;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jens Wilke
 */
public class EHCache3Test {

	@Test
	public void testJCacheDefault10000Configuration() {
		JCacheCacheFactory f = new JCacheCacheFactory();
		f.setProvider(EhcacheCachingProvider.class.getName());
		// f.setCacheName("benchmark");
		BenchmarkCache<Integer, Integer> _cache = f.create(10000);
		_cache.put(1, 3);
	}

	@Test
	public void testJCacheDefault10000ConfigurationWithSource() {
		JCacheCacheFactory f = new JCacheCacheFactory();
		f.setProvider(EhcacheCachingProvider.class.getName());
		// f.setCacheName("benchmark");
		BenchmarkCache<Integer, Integer> _cache = f.createLoadingCache(Integer.class, Integer.class, 10000, new BenchmarkCacheLoader<Integer, Integer>() {
			@Override
			public Integer load(final Integer key) {
				return key;
			}
		});
		assertEquals(3, (int) _cache.get(3));
	}

}

