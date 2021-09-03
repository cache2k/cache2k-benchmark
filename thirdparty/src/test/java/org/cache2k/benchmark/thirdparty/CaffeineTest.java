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

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.cache.JCacheCacheFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;

/**
 * @author Jens Wilke
 */
public class CaffeineTest {

	@Test
	public void test() {
		Cache c =
			Caffeine.newBuilder().maximumWeight(1000).weigher(new Weigher<Object, Object>() {
				@Override
				public int weigh(final Object key, final Object value) {
					return value.hashCode() & 0x7f;
				}
			}).build();
		c.put(1, 123);
		c.put(1, 512);
		c.put(1, 0);
	}

	@Test
	public void testJCacheDefault10000Configuration() {
		JCacheCacheFactory f = new JCacheCacheFactory();
		f.setProvider(com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider.class.getName());
		// f.setCacheName("benchmark");
		BenchmarkCache<Integer, Integer> _cache = f.create(10000);
		_cache.put(1, 3);
	}

	@Test
	public void testJCacheDefault10000ConfigurationAndLoader() {
		JCacheCacheFactory f = new JCacheCacheFactory();
		f.setProvider(com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider.class.getName());
		BenchmarkCache<Integer, Integer> _cache = f.createLoadingCache(Integer.class, Integer.class, 10000, new BenchmarkCacheLoader<Integer, Integer>() {
			@Override
			public Integer load(final Integer key) {
				return key;
			}
		});
		assertEquals(3, (int) _cache.get(3));
	}

	@Test
	public void testAsyncLoader() throws Exception {
		AsyncLoadingCache c =
		Caffeine.newBuilder()
			.buildAsync(new AsyncCacheLoader<Object, Object>() {
				@Override
				public CompletableFuture<Object> asyncLoad(final Object key, final Executor executor) {
					return CompletableFuture.completedFuture("okay");
				}
			});
		CompletableFuture<Map> cf = c.getAll(Arrays.asList(1, 2, 3));
		Map m = cf.get();
	}

}
