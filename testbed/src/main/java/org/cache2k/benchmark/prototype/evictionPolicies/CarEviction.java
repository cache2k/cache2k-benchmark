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
import org.cache2k.benchmark.EvictionStatistics;
import org.cache2k.benchmark.prototype.LinkedEntry;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the CAR replacement algorithm.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("Duplicates")
public class CarEviction<K,V> extends EvictionPolicy<K, V, CarEviction.Entry> {

	private int size;
	private int arcP = 0;
	private Map<K, Entry> b1Hash = new HashMap<>();
	private Map<K, Entry> b2Hash = new HashMap<>();
	private int t1Size = 0;
	private int t2Size = 0;
	private Entry t2Head = null;
	private Entry t1Head = null;
	private Entry b1Head = new Entry(null,null).shortCircuit();
	private Entry b2Head = new Entry(null,null).shortCircuit();

	private long hits;

	/**
	 * Created via reflection
	 */
	@SuppressWarnings("unused")
	public CarEviction(final int capacity) {
		super(capacity);
	}

	@Override
	public Entry newEntry(final K key, final V value) {
		Entry e = checkForGhost(key);
		if (e == null) {
			e = new Entry(key, value);
			insertIntoReplacementList(e);
		}
		return e;
	}

	@Override
	public void recordHit(final Entry e) {
		e.hitCnt++;
	}

	@Override
	public Entry evict() {
		Entry e = replace();
		removeEntryFromReplacementList(e);
		return e;
	}

	@Override
	public void close(final long expectedSize) {
		assert expectedSize == b1Hash.size() + b2Hash.size();
	}

	@Override
	public void remove(final Entry e) {
		e.removedFromList();
	}

	private void insertIntoReplacementList(Entry e) {
		assert e.hitCnt == 0;
		size++;
		t1Size++;
		t1Head = LinkedEntry.insertIntoTailCyclicList(t1Head, e);
	}

	private Entry checkForGhost(K key) {
		Entry e = b1Hash.remove(key);
		if (e != null) {
			e.removeFromList();
			b1HitAdaption();
			insertT2(e);
			return e;
		}
		e = b2Hash.remove(key);
		if (e != null) {
			e.removeFromList();
			b2HitAdaption();
			insertT2(e);
			return e;
		}
		return null;
	}

	private void b1HitAdaption() {
		// adaption:
		// +1 because we deleted already the entry from b1
		int _b1Size = b1Hash.size() + 1;
		int _b2Size = b2Hash.size();
		int _delta = _b1Size >= _b2Size ? 1 : _b2Size / _b1Size;
		arcP = Math.min(arcP + _delta, getCapacity());
		// replace(); postpone
	}

	private void b2HitAdaption() {
		// adaption:
		int _b1Size = b1Hash.size();
		// +1 because we deleted already the entry from b2
		int _b2Size = b2Hash.size() + 1;
		int _delta = _b2Size >= _b1Size ? 1 : _b1Size / _b2Size;
		arcP = Math.max(arcP - _delta, 0);
		// System.out.println("b2Hit, b1Size=" + _b1Size + ", b2Size=" + _b2Size + ", delta=" + -_delta + " => arpP=" + arcP);
		// replaceB2Hit(); postpone
	}

	private Entry replace() {
		Entry e;
		for (;;) {
			if (t1Size >= Math.max(1, arcP) || t2Size == 0) {
				e = t1Head;
				if (e.hitCnt == 0) {
					t1Head = e.next;
					break;
				}
				hits += e.hitCnt;
				e.hitCnt = 0;
				t1Head = LinkedEntry.removeFromCyclicList(t1Head, e);
				t1Size--;
				t2Head = LinkedEntry.insertIntoTailCyclicList(t2Head, e);
				t2Size++;
			} else {
				e = t2Head;
				if (e.hitCnt == 0) {
					t2Head = e.next;
					break;
				}
				hits += e.hitCnt;
				e.hitCnt = 0;
				t2Head = e.next;
			}
		}
		// TODO: is this independent from the above?
		evictGhosts();
		return e;
	}

	@SuppressWarnings({"SuspiciousMethodCalls"})
	private void evictGhosts() {
		// we leave out the x is not in B1 or B2 since this must be called, after the entry is removed
		// from the ghost lists.
		int b1Size = b1Hash.size();
		// TODO: experiment with >=
		if (t1Size + b1Size == getCapacity() && b1Size > 0) {
			Entry e = b1Head.prev;
			e.removeFromList();
			boolean f = b1Hash.remove(e.getKey()) != null;
			assert(f);
			return;
		}
		int b2Size = b2Hash.size();
		if (t1Size + t2Size + b1Size + b2Size > 2 * getCapacity() && b2Size > 0) {
			Entry e = b2Head.prev;
			e.removeFromList();
			boolean f = b2Hash.remove(e.getKey()) != null;
			assert(f);
		}
	}

	private void insertT2(Entry e) {
		t2Size++;
		t2Head = LinkedEntry.insertIntoTailCyclicList(t2Head, e);
	}

	@SuppressWarnings("unchecked")
	private void insertCopyIntoB1(Entry e) {
		e = copyEntryForGhost(e);
		b1Hash.put((K) e.getKey(), e);
		b1Head.insertInList(e);
	}

	@SuppressWarnings("unchecked")
	private void insertCopyIntoB2(Entry e) {
		e = copyEntryForGhost(e);
		b2Hash.put((K) e.getKey(), e);
		b2Head.insertInList(e);
	}

	private Entry copyEntryForGhost(Entry e) {
		Entry e2;
		e2 = new Entry(e.getKey(), e.getValue());
		return e2;
	}

	protected void removeEntryFromReplacementList(Entry e) {
		boolean t1Hit = t1Head != null && t1Head.prev == e;
		boolean t2Hit = t2Head != null && t2Head.prev == e;
		if (!t1Hit || !t2Hit) {
			if (t1Size < t2Size) {
				t1Hit = false;
				Entry x = t1Head;
				if (x != null) {
					do {
						if (x == e) {
							t1Hit = true;
							break;
						}
						x = x.next;
					} while (x != t1Head);
				}
			} else {
				t1Hit = true;
				Entry x = t2Head;
				if (x != null) {
					do {
						if (x == e) {
							t1Hit = false;
							break;
						}
						x = x.next;
					} while (x != t2Head);
				}
			}
		}
		if (t1Hit) {
			insertCopyIntoB1(e);
			t1Head = LinkedEntry.removeFromCyclicList(t1Head, e);
			t1Size--;
		} else {
			insertCopyIntoB2(e);
			t2Head = LinkedEntry.removeFromCyclicList(t2Head, e);
			t2Size--;
		}
	}

	@Override
	public EvictionStatistics getEvictionStats() {
		return new EvictionStatistics() {
			@Override
			public double getAdaptionValue() {
				return arcP;
			}
		};
	}

	@Override
	public String toString() {
		return "ClockEviction(" +
			"hits=" + hits +
			", size=" + size +
			')';
	}

	static class Entry extends LinkedEntry<Entry, Object, Object> {

		private Entry(final Object _key, final Object _value) {
			super(_key, _value);
		}

		private int hitCnt;

	}

}
