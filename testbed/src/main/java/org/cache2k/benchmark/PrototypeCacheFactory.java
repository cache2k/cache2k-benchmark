package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
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
import org.cache2k.benchmark.prototype.PrototypeCache;

import java.lang.reflect.Constructor;

/**
 * Creates a cache with the specified eviction policy.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("Duplicates")
public final class PrototypeCacheFactory<T extends EvictionTuning> extends BenchmarkCacheFactory<T> {

	private Class<? extends EvictionPolicy> evictionClass;

	public static PrototypeCacheFactory of(Class<? extends EvictionPolicy> eviction) {
		return new PrototypeCacheFactory().withEviction(eviction);
	}

	public PrototypeCacheFactory withEviction(Class<? extends EvictionPolicy> eviction) {
		this.evictionClass = eviction;
		String suffix = "eviction";
		String name = eviction.getSimpleName().toLowerCase();
		if (name.endsWith(suffix)) {
			name = name.substring(0, name.length() - suffix.length());
		}
		setNamePrefix(name);
		return this;
	}

	protected EvictionPolicy createPolicy(int capacity) {
		Constructor constructor = evictionClass.getConstructors()[0];
		try {
			if (constructor.getParameterTypes().length == 1) {
				return (EvictionPolicy) constructor.newInstance(capacity);
			} else {
				return (EvictionPolicy) constructor.newInstance(capacity, getTuning());
			}
		} catch (Exception ex) {
			throw new IllegalArgumentException("Creating: " + evictionClass.getName(), ex);
		}
	}

	/**
	 * Create a default tunable by obtaining the class from the constructor argument.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T getDefaultTuning() {
		try {
			if (evictionClass.getConstructors().length != 1) {
				throw new IllegalArgumentException("Eviction class needs to have only one constructor");
			}
			Constructor constructor = evictionClass.getConstructors()[0];
			if (constructor.getParameterTypes().length == 1) {
				return null;
			}
			if (constructor.getParameterTypes().length != 2) {
				throw new IllegalArgumentException("Cache implementation needs exactly one constructor.");
			}
			return (T) constructor.getParameterTypes()[1].newInstance();
		} catch (Exception ex) {
			throw new RuntimeException("Creating tuning for: " + evictionClass.getName(), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K, V> BenchmarkCache<K, V> create(
		Class<K> keyType, Class<V> valueType, int capacity) {
		return (BenchmarkCache<K,V>) new PrototypeCache(createPolicy(capacity), getEvictionListeners());
	}

}
