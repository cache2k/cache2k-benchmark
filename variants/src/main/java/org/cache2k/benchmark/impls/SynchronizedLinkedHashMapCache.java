package org.cache2k.benchmark.impls;

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

import org.cache2k.benchmark.BenchmarkCache;

import java.util.Map;

/**
 * Using simply a {@code LinkedHashMap} is not thread safe. This implementation
 * wraps the access in synchronization statements.
 * <p>
 * Side note: Wrapping the map with {@link java.util.Collections#synchronizedMap(Map)} would be more elegant,
 * but we use this code for demonstration purposes.
 * <p>
 * A better variant for higher parellelism is the {@link PartitionedLinkedHashMapCache}.
 *
 * @author Jens Wilke
 * @see PartitionedLinkedHashMapCache
 */
public class SynchronizedLinkedHashMapCache<K,V> extends BenchmarkCache<K,V> {

	final private LinkedHashMapCache<K,V> backingMap;

	public SynchronizedLinkedHashMapCache(final int size) {
		backingMap = new LinkedHashMapCache<>(size);
	}

	@Override
	public void put(K key, V value) {
		synchronized (backingMap) {
			backingMap.put(key, value);
		}
	}

	@Override
	public V get(K key) {
		synchronized (backingMap) {
			return backingMap.get(key);
		}
	}

	@Override
	public void remove(final K key) {
		synchronized (backingMap) {
			backingMap.remove(key);
		}
	}

}
