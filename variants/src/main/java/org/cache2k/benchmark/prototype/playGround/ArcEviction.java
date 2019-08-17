package org.cache2k.benchmark.prototype.playGround;

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

import org.cache2k.benchmark.prototype.EvictionPolicy;
import org.cache2k.benchmark.prototype.LinkedEntry;

import java.util.HashMap;

/**
 * Adaptive Replacement Cache implementation. This implementation was used for production evaluation
 * in versions of cache2k before 1.0 and was later removed in favor of a Clock-Pro based implementation.
 *
 * <p>The implemented algorithm changed from cache2k version 0.19 to 0.20. Within version 0.20
 * the eviction is separated from the general cache access, because of the consequent entry locking
 * scheme an entry that is accessed, is always present in the cache. This means, the eviction is
 * postponed to the end of the cache operation. The ARC algorithm was augmented to reflect this.
 * This means for a cache operation that a newly inserted cache entry is also a candidate
 * for eviction at the end of the operation.
 *
 * <p>This algorithm is patented by IBM (6996676, 7096321, 7058766, 8612689) and Sun (7469320)
 *
 * @see <a href="http://www.usenix.org/event/fast03/tech/full_papers/megiddo/megiddo.pdf‎">A Self-Tuning, Low Overhead Replacement Cache</a>
 * @see <a href="http://en.wikipedia.org/wiki/Adaptive_replacement_cache">Wikipedia: Adaptive Replacement Cache</a>
 *
 * @author Jens Wilke
 */
public class ArcEviction<K,V> extends EvictionPolicy<K, V, ArcEviction.Entry> {

	private int arcP = 0;

	private HashMap<Object, Entry> b1Hash = new HashMap<>();

	private HashMap<Object, Entry> b2Hash = new HashMap<>();

	/** Statistics */
	private long t2Hit;
	private long t1Hit;

	private long t1Size = 0;
	private long t2Size = 0;

	private Entry t2Head = new Entry(null, null).shortCircuit();
	private Entry t1Head = new Entry(null, null).shortCircuit();
	private Entry b1Head = new Entry(null, null).shortCircuit();
	private Entry b2Head = new Entry(null, null).shortCircuit();

	private boolean b2HitPreferenceForEviction;

	public ArcEviction(final int capacity) {
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
		t2Head.moveToFront(e);
		if (e.withinT2) {
			t2Hit++;
		} else {
			e.withinT2 = true;
			t1Hit++;
			t1Size--;
			t2Size++;
		}
	}

	@Override
	public Entry evict() {
		Entry e = findEvictionCandidate();
		removeEntryFromReplacementList(e);
		return e;
	}

	@Override
	public void remove(final Entry e) {
		removeEntryFromReplacementList(e);
	}

	protected void insertIntoReplacementList(Entry e) {
		assert(!e.withinT2);
		t1Head.insertInList(e);
		t1Size++;
	}

	private Entry checkForGhost(Object key) {
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
		allMissEvictGhosts();
		return null;
	}

	private void removeEntryFromReplacementList(Entry e) {
		if (!e.withinT2) {
			t1Size--;
		} else {
			t2Size--;
		}
		e.removeFromList();
		e.withinT2 = false;
	}

	private void b1HitAdaption() {
		// adaption:
		// +1 because we deleted already the entry from b1
		int _b1Size = b1Hash.size() + 1;
		int _b2Size = b2Hash.size();
		int _delta = _b1Size >= _b2Size ? 1 : _b2Size / _b1Size;
		arcP = Math.min(arcP + _delta, getCapacity());
		b2HitPreferenceForEviction = false;
		// replace(); postpone
	}

	private void b2HitAdaption() {
		// adaption:
		int _b1Size = b1Hash.size();
		// +1 because we deleted already the entry from b2
		int _b2Size = b2Hash.size() + 1;
		int _delta = _b2Size >= _b1Size ? 1 : _b1Size / _b2Size;
		arcP = Math.max(arcP - _delta, 0);
		b2HitPreferenceForEviction = true;
		// replaceB2Hit(); postpone
	}

	private void insertT2(Entry e) {
		e.withinT2 = true;
		t2Head.insertInList(e);
		t2Size++;
	}

	// TODO: move to base
	private Entry cloneGhost(Entry e) {
		Entry e2 = new Entry(e.getKey(), e.getValue());
		return e2;
	}

	/**
	 * Called when no entry was hit within b1 or b2. This checks whether we need to
	 * remove some entries from the b1 and b2 lists.
	 */
	private void allMissEvictGhosts() {
		if ((t1Size + b1Hash.size()) >= getCapacity()) {
			// paper: t1Size < maxSize
			if (b1Hash.size() > 0) {
				Entry e = b1Head.prev;
				e.removeFromList();
				boolean f = b1Hash.remove(e.getKey()) != null;
				assert(f);
				// replace(); postponed
			} else {
				// b1 is empty
				assert t1Size >= getCapacity();
				if (b2Hash.size() >= getCapacity()) {
					Entry e = b2Head.prev;
					e.removeFromList();
					boolean f = b2Hash.remove(e.getKey()) != null;
					assert(f);
				}
			}
		} else {
			// total entries may exceed maxSize if an operation is going on.
			assert !(getLocalSize() <= getCapacity()) || ((t1Size + b1Hash.size()) < getCapacity());
			int _totalCnt = b1Hash.size() + b2Hash.size();
			if (_totalCnt >= getCapacity()) {
				if (b2Hash.size() == 0) {
					assert(b1Hash.size() > 0);
					Entry e = b1Head.prev;
					e.removeFromList();
					boolean f = b1Hash.remove(e.getKey()) != null;
					assert(f);
					return;
				}
				assert(b2Hash.size() > 0);
				Entry e = b2Head.prev;
				e.removeFromList();
				boolean f = b2Hash.remove(e.getKey()) != null;
				assert(f);
			}
			// replace(); postpone
		}
	}

	public long getLocalSize() {
		return t1Size + t2Size;
	}

	protected Entry findEvictionCandidate() {
		Entry e;
		if (b2HitPreferenceForEviction) {
			e = replaceB2Hit();
		} else {
			e = replace();
		}
		/* now we have too much entries in the b1/b2 lists, remove it */
		if (b1Hash.size() + b2Hash.size() > getCapacity()) {
			allMissEvictGhosts();
		}
		return e;
	}

	private Entry replace() {
		/* bug in paper algorithm: t2Size may be zero */
		return replace((t1Size > arcP) || t2Size == 0);
	}

	private Entry replaceB2Hit() {
		/* bug in the paper algorithm: t1Size may be zero */
		return replace((t1Size >= arcP && t1Size > 0) || t2Size == 0);
	}

	private Entry replace(boolean _fromT1) {
		Entry e;
		if (_fromT1) {
			assert(t1Size > 0);
			e = t1Head.prev;
			Entry e2 = cloneGhost(e);
			b1Head.insertInList(e2);
			b1Hash.put(e2.getKey(), e2);
		} else {
			assert(t2Size > 0);
			e = t2Head.prev;
			Entry e2 = cloneGhost(e);
			b2Head.insertInList(e2);
			b2Hash.put(e2.getKey(), e2);
		}
		return e;
	}

	@Override
	public void close(long expectedSize) {
		assert expectedSize >= b1Hash.size() + b2Hash.size();
		assert expectedSize == (t1Size + t2Size);
		assert b1Hash.size() == b1Head.listSize();
		assert b2Hash.size() == b2Head.listSize();
		assert t1Size == t1Head.listSize();
		assert t2Size == t2Head.listSize();
	}

	public String toString() {
		return  "ArcEviction(arcP=" + arcP + ", "
			+ "t1Hit=" + t1Hit + ", "
			+ "t2Hit=" + t2Hit + ", "
			+ "t1Size=" + t1Size + ", "
			+ "t2Size=" + t2Size + ", "
			+ "b1Size=" + b1Hash.size() + ", "
			+ "b2Size=" + b2Hash.size() +
			")" ;
	}

	static class Entry extends LinkedEntry<Entry, Object, Object> {

		public Entry(final Object _key, final Object _value) {
			super(_key, _value);
		}

		boolean withinT2;

	}

}
