package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: implementation variants
 * %%
 * Copyright (C) 2013 - 2018 headissue GmbH, Munich
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

import org.cache2k.benchmark.impls.SynchronizedLinkedHashMapCache;

/**
 * @author Jens Wilke
 */
public class SynchronizedLinkedHashMapFactory extends BenchmarkCacheFactory {

	@Override
	protected <K, V> BenchmarkCache<K, V> createSpecialized(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
		return new SynchronizedLinkedHashMapCache<>(_maxElements);
	}

}
