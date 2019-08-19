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

import org.cache2k.benchmark.EvictionTuning;

/**
 * @author Jens Wilke
 */
public class Cache2kV1Tuning implements EvictionTuning {

	int hotMaxPercentage = 97;

	int hitCounterDecreaseShift = 6;

	/** Needed for creation by reflection */
	@SuppressWarnings("unused")
	public Cache2kV1Tuning() { }

	public Cache2kV1Tuning(final int hotMaxPercentage) {
		this.hotMaxPercentage = hotMaxPercentage;
	}

	public String toString() {
		// return String.format("%d,%d", hotMaxPercentage, hitCounterDecreaseShift);
		return String.format("%d", hotMaxPercentage);
	}

}
