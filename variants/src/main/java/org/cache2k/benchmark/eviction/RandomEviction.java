package org.cache2k.benchmark.eviction;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Eviction policy which just selects the evicted item by random.
 *
 * @author Jens Wilke
 */
public class RandomEviction<K,V> extends EvictionPolicy<K, V, RandomEviction.Entry> {

	private Random random = new Random(1802);
	private HashMap<String, Entry> map = new HashMap<>();

	@Override
	public Entry newEntry(final K key, final V value) {
		Entry e = new Entry(key, value);
		map.put(Long.toHexString(random.nextLong()) + key, e);
		return e;
	}

	@Override
	public void recordHit(final Entry e) {
	}

	@Override
	public Entry evict() {
		Iterator<Map.Entry<String, Entry>> it = map.entrySet().iterator();
		Entry e = it.next().getValue();
		it.remove();
		return e;
	}

	@Override
	public void close(final long expectedSize) {
		assert expectedSize == map.size();
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
