package org.cache2k.benchmark.impls;

/*
 * #%L
 * Benchmarks: implementation variants
 * %%
 * Copyright (C) 2013 - 2018 headissue GmbH, Munich
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

import java.util.Map;

/**
 * Partitioned variant of a simple cache based on the {@code LinkedHashMap}.
 * Uses four lock objects to achieve a higher parallelism.
 *
 * @author Jens Wilke
 */
public class PartitionedLinkedHashMapCache<K,V> extends BenchmarkCache<K,V> {

	final private int PARTS = 4;
	final private int MASK = 3;
	final private int size;

	final private LinkedHashMapCache<K,V>[] backingMaps = new LinkedHashMapCache[PARTS];

	public PartitionedLinkedHashMapCache(final int size) {
		this.size = size;
		int partSize = (size + PARTS - 1) / PARTS;
		for (int i = 0; i < PARTS; i++) {
			backingMaps[i] = new LinkedHashMapCache<>(partSize);
		}
	}

	public void put(K key, V value) {
		LinkedHashMapCache<K,V> backingMap = backingMaps[key.hashCode() & MASK];
		synchronized (backingMap) {
			backingMap.put(key, value);
		}
	}

	public V get(K key) {
		LinkedHashMapCache<K,V> backingMap = backingMaps[key.hashCode() & MASK];
		synchronized (backingMap) {
			return backingMap.get(key);
		}
	}

	@Override
	public int getCapacity() {
		return size;
	}

}
