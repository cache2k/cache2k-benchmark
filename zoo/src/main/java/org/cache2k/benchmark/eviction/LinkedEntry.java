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

/**
 * Cached entry with linked list.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("WeakerAccess")
public class LinkedEntry<E extends LinkedEntry,K,V> extends Entry<K,V> {

	public E next;
	public E prev;

	public LinkedEntry(final K _key, final V _value) {
		super(_key, _value);
	}

	/*
	 * **************************************** LRU list operation *******************************************
	 */

	/**
	 * Reset next as a marker
	 */
	public final void removedFromList() {
		next = null;
		// we need to clear the pre link also, otherwise it is a mem leak,
		// there are canceled timer tasks hold by the timer thread, that link via the prev
		// reference to all other entries previously evicted!
		prev = null;
	}

	@SuppressWarnings("unchecked")
	public E shortCircuit() {
		return next = prev = (E) this;
	}

	@SuppressWarnings({"Duplicates", "unchecked"})
	public static <E extends LinkedEntry> E insertIntoTailCyclicList(final E _head, final E e) {
		if (_head == null) {
			return (E) e.shortCircuit();
		}
		e.next = _head;
		e.prev = _head.prev;
		_head.prev = e;
		e.prev.next = e;
		return _head;
	}

	@SuppressWarnings({"Duplicates", "unchecked"})
	public static <E extends LinkedEntry> E removeFromCyclicList(final E _head, E e) {
		assert (!(e == e.prev) || (e == e.next));
		assert (!(e == e.next) || (e == e.prev));
		assert (!(e.prev == e) || (e == _head));
		// assert(!(e.next == e) || (e == _head)); always true checked above ;jw
		if (e.next == e) {
			e.removedFromList();
			return null;
		}
		E _eNext = (E) e.next;
		e.prev.next = _eNext;
		e.next.prev = e.prev;
		e.removedFromList();
		return e == _head ? _eNext : _head;
	}

	@SuppressWarnings("unchecked")
	public static <E extends LinkedEntry> E removeFromCyclicList(final E e) {
		assert (!(e == e.prev) || (e == e.next));
		assert (!(e == e.next) || (e == e.prev));
		E _eNext = (E) e.next;
		e.prev.next = _eNext;
		e.next.prev = e.prev;
		e.removedFromList();
		return _eNext == e ? null : _eNext;
	}

}
