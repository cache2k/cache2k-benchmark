package org.cache2k.benchmark.eviction;

/*
 * #%L
 * Benchmarks: implementation variants
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

import java.util.HashMap;
import java.util.Map;

/**
 * Implements the basic cache functions with a hash map and delegates to a
 * {@link EvictionPolicy} policy for eviction decisions. Not thread safe. This is
 * just for evaluating and experimentation with different eviction strategies.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("FieldCanBeLocal")
public class BaseCache<E extends Entry<K,V>, K,V>
	extends BenchmarkCache<K,V> {

	private final EvictionPolicy<K, V, E> eviction;
	private final Map<K, E> content = new HashMap<>();
	private boolean debugEvictions = false;
	private boolean debugHits = false;
	private int evictionStep = 0;

	public BaseCache(final EvictionPolicy<K, V, E> eviction) {
		this.eviction = eviction;
	}

	@Override
	public void put(final K key, final V value) {
		// DEBUG System.out.println(key);
		if (content.size() >= eviction.getCapacity()) {
			E e = eviction.evict();
			if (debugEvictions) {
				System.out.println("EVICT " + (evictionStep++) + ": " + e.getKey());
			}
			content.remove(e.getKey());
		}
		E e = eviction.newEntry(key, value);
		content.put(key, e);
	}

	@Override
	public V get(final K key) {
		E e = content.get(key);
		if (e != null) {
			if (debugHits) {
				System.out.println("HIT " + key);
			}
			eviction.recordHit(e);
			return e.getValue();
		}
		return null;
	}

	@Override
	public void remove(final K key) {
		E e = content.get(key);
		if (e != null) {
			eviction.remove(e);
		}
		content.remove(key);
	}

	@Override
	public int getCapacity() {
		return eviction.getCapacity();
	}

	@Override
	public String toString() {
		return "ExperimentalCache{" +
			"eviction=" + eviction +
			'}';
	}

}
