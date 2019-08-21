package org.cache2k.benchmark.prototype;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
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

import org.cache2k.benchmark.EvictionStatistics;

/**
 * Abstract class / basic interface for a eviction policy.
 *
 * @author Jens Wilke
 */
public abstract class EvictionPolicy<K, V, E extends Entry> {

	private int capacity;

	/**
	 * Construct a new entry in the eviction policy data structures.
	 * Takes the key, so the policy can lookup in the history whether this entry
	 * was seen before.
	 */
	public abstract E newEntry(K key, V value);

	/**
	 * Record a hit for this entry.
	 */
	public abstract void recordHit(E e);

	/**
	 * Evict one entry.
	 */
	public abstract E evict();

	/**
	 * Remove the entry from the eviction data structures.
	 */
	public abstract void remove(E e);

	public EvictionPolicy(int capacity) {
		this.capacity = capacity;
	}

	public int getCapacity() { return capacity;	}

	/**
	 * Called after testing is finished. Implementations can check their
	 * internal data structures.
	 *
	 * @param expectedSize The current cache size.
	 */
	public void close(long expectedSize) { }

	public EvictionStatistics getEvictionStats() {
		return new EvictionStatistics() {};
	}

}
