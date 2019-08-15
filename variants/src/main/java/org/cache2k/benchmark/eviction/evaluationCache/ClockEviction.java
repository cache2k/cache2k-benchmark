package org.cache2k.benchmark.eviction.evaluationCache;

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

import org.cache2k.benchmark.eviction.EvictionPolicy;
import org.cache2k.benchmark.eviction.EvictionStats;
import org.cache2k.benchmark.eviction.LinkedEntry;
import org.cache2k.benchmark.impl2015.ClockCache;

/**
 * Clock eviction which simulates LRU on a clock structure.
 *
 * @author Jens Wilke
 */
public class ClockEviction<K,V> extends EvictionPolicy<K, V, ClockEviction.Entry> {

	private long hits;
	private long scan24hCnt;
	private long scanCnt;
	private long size;
	private Entry hand;

	@Override
	public Entry newEntry(final K key, final V value) {
		Entry e = new Entry(key, value);
		insertIntoReplacementList(e);
		return e;
	}

	@Override
	public void recordHit(final Entry e) {
		e.hitCnt++;
	}

	@Override
	public Entry evict() {
		Entry e = findEvictionCandidate();
		removeEntryFromReplacementList(e);
		return e;
	}

	@Override
	public void close(final long expectedSize) {
		assert expectedSize == size;
		assert expectedSize == hand.listSize();
	}

	@Override
	public void remove(final Entry e) {
		e.removedFromList();
	}

	private void insertIntoReplacementList(Entry e) {
		assert e.hitCnt == 0;
		size++;
		hand = LinkedEntry.insertIntoTailCyclicList(hand, e);
	}

	private Entry findEvictionCandidate() {
		assert hand != null;
		assert  size > 0;
		int scanCnt = 0;
		while (hand.hitCnt > 0) {
			scanCnt++;
			hits += hand.hitCnt;
			hand.hitCnt = 0;
			hand = (Entry) hand.next;
		}
		if (scanCnt > size) {
			scan24hCnt++;
		}
		this.scanCnt += scanCnt;
		return hand;
	}

	private void removeEntryFromReplacementList(Entry e) {
		hand = LinkedEntry.removeFromCyclicList(hand, e);
		hits += e.hitCnt;
		size--;
	}

	@Override
	public EvictionStats getEvictionStats() {
		return new EvictionStats() {
			@Override
			public long getScanCount() {
				return scanCnt;
			}

		};
	}

	@Override
	public String toString() {
		return "ClockEviction(" +
			"hits=" + hits +
			", scanCnt=" + scanCnt +
			", scan24hCnt=" + scan24hCnt +
			", size=" + size +
			')';
	}

	static class Entry extends LinkedEntry<Entry, Object, Object> {

		public Entry(final Object _key, final Object _value) {
			super(_key, _value);
		}

		private int hitCnt;

	}

}
