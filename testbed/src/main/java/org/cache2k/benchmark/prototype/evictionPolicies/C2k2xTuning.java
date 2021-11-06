package org.cache2k.benchmark.prototype.evictionPolicies;

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

import org.cache2k.benchmark.EvictionTuning;

/**
 * @author Jens Wilke
 */
public class C2k2xTuning implements EvictionTuning {

	private int hotMaxPercentage = 97;

	private int hitCounterDecreaseShift = 6;

	/** Needed for creation by reflection */
	@SuppressWarnings("unused")
	public C2k2xTuning() { }

	public C2k2xTuning(int hotMaxPercentage) {
		this.hotMaxPercentage = hotMaxPercentage;
	}

	public int getGhostCutOff() {
		return Integer.MAX_VALUE;
	}

	public int getHotMaxPercentage() {
		return hotMaxPercentage;
	}

	public int getHitCounterDecreaseShift() {
		return hitCounterDecreaseShift;
	}

	public boolean isDoNotRememberHots() {
		return false;
	}

	public String toString() {
		// return String.format("%d,%d", hotMaxPercentage, hitCounterDecreaseShift);
		return String.format("%d", hotMaxPercentage);
	}

	public int getGhostMaxPercentage() {
		return 50;
	}

	/**
	 * cache2k V1.4 is base line.
	 */
	public static class V14Tuning extends C2k2xTuning {
		@Override public String toString() { return "V14"; }
	}

	/**
	 * cache2k V2.4: limit the ghost size instead of using a fixed percentage from the cache size.
	 * Benchmarks show, with big cache sizes (1M) a big history does not contribute much to a
	 * better hit rate, however, it produces a lot of random memory access.
	 *
	 * <p>Further ideas: Use a function and rather than a hard limit. Change history to bloom filters.
	 */
	public static class V24Tuning extends C2k2xTuning {
		public V24Tuning() { }
		public V24Tuning(int hotMaxPercentage) { super(hotMaxPercentage); }
		@Override public int getGhostCutOff() { return 3_000; }
		@Override public String toString() { return "V24"; }
	}

	/**
	 * In cache2k V2.6 we don insert evicted hot entries into ghost list.
	 */
	public static class V26Tuning extends V24Tuning {
		public V26Tuning() { super(94); }
		public V26Tuning(int hotMaxPercentage) { super(hotMaxPercentage); }
		@Override public boolean isDoNotRememberHots() { return true; }
		// @Override public int getGhostCutOff() { return Integer.MAX_VALUE; }
		@Override public String toString() { return "V26"; }
	}

}
