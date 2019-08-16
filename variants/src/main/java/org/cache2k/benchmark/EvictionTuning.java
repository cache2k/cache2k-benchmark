package org.cache2k.benchmark;

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
 * Marker for a bean that holds tuning parameters for the eviction strategy.
 *
 * @author Jens Wilke
 */
public interface EvictionTuning {

	/**
	 * Compact string to add to the cache implementation name to determine the actual
	 * tuning configuration.
	 */
	default String getNameSuffix() {
		return toString();
	}

	/**
	 * Denotes that no tuning is taken.
	 */
	class None implements EvictionTuning {
		private None() {}
	}

}
