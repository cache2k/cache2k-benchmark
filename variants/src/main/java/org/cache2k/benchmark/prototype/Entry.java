package org.cache2k.benchmark.prototype;

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

/**
 * A cached entry holds a key and value.
 *
 * @author Jens Wilke
 */
public class Entry<K, V> {

	private K key;
	private V value;

	public Entry(final K _key, final V _value) {
		key = _key;
		value = _value;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

}
