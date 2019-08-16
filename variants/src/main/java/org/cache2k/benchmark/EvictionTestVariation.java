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

import org.cache2k.benchmark.util.TraceSupplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jens Wilke
 */
public class EvictionTestVariation {

	private final TraceSupplier traceSupplier;
	private final AnyCacheFactory cacheFactory;
	private final int cacheSize;

	public EvictionTestVariation(final TraceSupplier traceSupplier, final AnyCacheFactory cacheFactory, final int cacheSize) {
		this.traceSupplier = traceSupplier;
		this.cacheFactory = cacheFactory;
		this.cacheSize = cacheSize;
	}

	public TraceSupplier getTraceSupplier() {
		return traceSupplier;
	}

	public AnyCacheFactory getCacheFactory() {
		return cacheFactory;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	@Override
	public String toString() {
		return cacheFactory.getName() + "#" + cacheSize + "@" + traceSupplier.getName();
	}

	public static class Builder {

		private Set<AnyCacheFactory> caches = new HashSet<>();
		private Map<TraceSupplier, Set<Integer>> trace2sizes = new HashMap<>();

		public Builder add(AnyCacheFactory factory) {
			caches.add(factory);
			return this;
		}

		public Builder add(TraceSupplier trace, int... cacheSizes) {
			Set<Integer> sizes = trace2sizes.computeIfAbsent(trace, x -> new HashSet<>());
			for (int i : (cacheSizes.length == 0 ? trace.getSizes() : cacheSizes)) { sizes.add(i); }
			return this;
		}

		public Builder merge(Builder builder) {
			caches.addAll(builder.caches);
			builder.trace2sizes.forEach((trace, sizes) ->
				{
					Set<Integer> ourSizes = trace2sizes.computeIfAbsent(trace, x -> new HashSet<>());
					ourSizes.addAll(sizes);
				}
			);
			return this;
		}

		public Collection<EvictionTestVariation> build() {
			final List<EvictionTestVariation> result = new ArrayList<>();
			caches.forEach(cache ->
				trace2sizes.forEach((trace, sizes) ->
					sizes.forEach(size ->
						result.add(new EvictionTestVariation(trace, cache, size))
					)));
			return result;
		}

	}
}
