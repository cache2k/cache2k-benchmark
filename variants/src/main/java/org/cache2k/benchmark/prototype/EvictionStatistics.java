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
 * Statistics of some eviction internals relevant for evaluation.
 * Misses and hits are counted by the harness.
 *
 * @author Jens Wilke
 */
public interface EvictionStatistics {

	/**
	 * For clock like eviction algorithms: Total number of entries scanned for the eviction.
	 * A scan means the entry was accessed and includes the
	 * entry that is evicted.
	 */
	default long getScanCount() { return -1; }

	/**
	 * Count of evicted entries.
	 */
	default long getEvictionCount() { return -1; }

	/**
	 * For algorithms with adaption:
	 * An opaque value from the eviction policy about its adaption.
	 * E.g. arcP for ARC.
	 */
	default double getAdaptionValue() { return Double.NaN; }

	/**
	 * For algorithms with adaption and two entry sets: Average number of entries
	 * that were hold in a hot space.
	 */
	default double getAverageHotPercentage() { return Double.NaN; }

	/**
	 * Calculated value.
	 */
	default double getScansPerEviction() {
		if (getEvictionCount() >= 0 && getScanCount() >= 0) {
			return getScanCount() * 1D / getEvictionCount();
		}
		return Double.NaN;
	}

}
