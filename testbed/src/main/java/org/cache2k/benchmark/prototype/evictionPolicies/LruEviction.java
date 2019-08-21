package org.cache2k.benchmark.prototype.evictionPolicies;

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

import org.cache2k.benchmark.prototype.EvictionPolicy;
import org.cache2k.benchmark.prototype.LinkedEntry;

/**
 * Eviction policy based on LRU.
 *
 * @author Jens Wilke
 */
public class LruEviction<K,V> extends EvictionPolicy<K, V, LruEviction.Entry> {

	private Entry head = new Entry(null,null).shortCircuit();

	/**
	 * Created via reflection.
	 */
	@SuppressWarnings("unused")
	public LruEviction(final int capacity) {
		super(capacity);
	}

	@Override
	public Entry newEntry(final K key, final V value) {
		Entry e = new Entry(key, value);
		head.insertInList(e);
		return e;
	}

	@Override
	public void recordHit(final Entry e) {
		head.moveToFront(e);
	}

	@Override
	public Entry evict() {
		return head.prev.removeFromList();
	}

	@Override
	public void close(final long expectedSize) {
		assert expectedSize == head.listSize();
	}

	@Override
	public void remove(final Entry e) {
		e.removedFromList();
	}

	static class Entry extends LinkedEntry<Entry, Object, Object> {

		public Entry(final Object _key, final Object _value) {
			super(_key, _value);
		}

	}

}
