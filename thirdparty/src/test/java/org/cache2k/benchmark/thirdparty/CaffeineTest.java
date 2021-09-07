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
import com.github.benmanes.caffeine.cache.CacheWriter;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Weigher;
import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheLoader;
import org.cache2k.benchmark.cache.JCacheCacheFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
	public void testExpire() throws Exception {
		Cache c =
			Caffeine.newBuilder().expireAfterWrite(Duration.ofNanos(1)).build();
		c.put(1, 123);
		c.put(1, 512);
		c.put(1, 0);
		Thread.sleep(123);
		System.out.println(c.stats());
	}

	@Test
	public void testAfterWriteDistant() throws Exception {
		Cache c =
			Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(123456)).build();
		c.put(1, 123);
		c.getIfPresent(1);
		c.put(1, 456);
		System.out.println(c.stats());
	}

	@Test
	public void testJCacheDefault10000Configuration() {
		JCacheCacheFactory f = new JCacheCacheFactory();
		f.setProvider(com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider.class.getName());
		// f.setCacheName("benchmark");
		BenchmarkCache<Integer, Integer> _cache = f.create(Integer.class, Integer.class, 10000);
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

	@Test
	public void testExpiryWithRemovalListerner() {
		final AtomicBoolean removed = new AtomicBoolean(false);
		Cache c =
			Caffeine.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(2, TimeUnit.SECONDS)
			.removalListener(new RemovalListener<Object, Object>() {
				@Override
				public void onRemoval(@Nullable final Object key, @Nullable final Object value, @NonNull final RemovalCause cause) {
					removed.set(true);
				}
			}).build();
		c.put(1,2);
		while (c.asMap().containsKey(1)) { }
		assertTrue(removed.get());
	}

	@Test
	public void testExpiryWithCacheWriter() throws Exception {
		final AtomicBoolean removed = new AtomicBoolean(false);
		Cache c =
			Caffeine.newBuilder()
				.maximumSize(1000)
				.expireAfterWrite(2, TimeUnit.SECONDS)
				.writer(new CacheWriter<Object, Object>() {
					@Override
					public void write(@NonNull final Object key, @NonNull final Object value) {

					}

					@Override
					public void delete(@NonNull final Object key, @Nullable final Object value, @NonNull final RemovalCause cause) {
						removed.set(true);
					}
				})
				.build();
		long t0 = System.currentTimeMillis();
		c.put(1,2);
		while (c.asMap().containsKey(1)) { }
		System.out.println("Time elapsed: " + (System.currentTimeMillis() - t0) + ", removed=" + removed.get());
		// assertTrue(removed.get());
	}

}
