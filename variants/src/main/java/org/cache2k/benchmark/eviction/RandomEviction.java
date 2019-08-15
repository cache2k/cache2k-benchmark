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

import it.unimi.dsi.util.XoShiRo256StarStarRandom;

import java.util.Random;

/**
 * Eviction policy which just selects the evicted item by random.
 * Distributes all entries randomly into a bucket list and then picks
 * round robin for eviction.
 *
 * @author Jens Wilke
 */
public class RandomEviction<K,V> extends EvictionPolicy<K, V, RandomEviction.Entry> {

	private Random random = new XoShiRo256StarStarRandom(1802);
	private Entry[] buckets;
	private int evictionBucketIdx = 0;

	@Override
	public void setCapacity(final int v) {
		super.setCapacity(v);
		buckets = new Entry[getCapacity()];
	}

	@Override
	public Entry newEntry(final K key, final V value) {
		Entry e = new Entry(key, value);
		int bucketIdx = random.nextInt(buckets.length);
		e.next = buckets[bucketIdx];
		buckets[bucketIdx] = e;
		return e;
	}

	@Override
	public void recordHit(final Entry e) {
	}

	@Override
	public Entry evict() {
		Entry e;
		while ( (e = buckets[evictionBucketIdx]) == null) {
			evictionBucketIdx++;
			if (evictionBucketIdx >= buckets.length) {
				evictionBucketIdx = 0;
			}
		}
		buckets[evictionBucketIdx] = e.next;
		return e;
	}

	@Override
	public void remove(final Entry e) {
		e.removedFromList();
	}

	static class Entry extends LinkedEntry<Entry, Object, Object> {

		private Entry next;
		public Entry(final Object _key, final Object _value) {
			super(_key, _value);
		}

	}

}
