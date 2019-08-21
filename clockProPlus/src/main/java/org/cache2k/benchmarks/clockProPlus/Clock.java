package org.cache2k.benchmarks.clockProPlus;

/*
 * #%L
 * Benchmarks: Clock-Pro+ and other eviction policies
 * %%
 * Copyright (C) 2018 - 2019 Cong Li, Intel Corporation
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

import org.apache.commons.collections4.map.LinkedMap;

/**
 * Eviction algorithm based on the original CLOCK idea.
 *
 * @author Cong Li
 */
public class Clock implements ISimpleCache {

	public Clock(int size) {
		this.size = size;
		this.clock = new LinkedMap<Integer, CacheMetaData>();
	}

	@Override
	public boolean request(Integer address) {
		if (this.exists(address)) {
			CacheMetaData data = this.clock.get((Integer)address);
			data.setReference(true);
			return true;
		}
		if (this.size > this.clock.size()) {
			this.insert(address);
		} else {
			this.replace(address);
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("Clock(%d)", this.size);
	}

	public int getCurrentSize() {
		return this.clock.size();
	}

	protected boolean exists(int address) {
		return this.clock.containsKey(address);
	}

	protected void move() {
		Integer firstKey = this.clock.firstKey();
		CacheMetaData data = this.clock.remove(firstKey);
		this.clock.put(firstKey, data);
	}

	protected void insert(int address) {
		if (this.size < this.clock.size()) {
			System.err.println("Error");
		}
		CacheMetaData data = new CacheMetaData();
		data.setAddress(address);
		this.clock.put(address, data);
		//this.checkSize();
	}

	protected CacheMetaData check() {
		Integer firstKey = this.clock.firstKey();
		return this.clock.get(firstKey);
	}

	protected CacheMetaData evict() {
		Integer firstKey = this.clock.firstKey();
		return this.clock.remove(firstKey);
	}

	protected void replace(int address) {
		while (true) {
			CacheMetaData data = this.check();
			if (data.isReferenced()) {
				data.setReference(false);
			} else {
				this.evict();
				this.insert(address);
				break;
			}
			this.move();
		}
	}

	protected void checkSize() {
	}

	protected LinkedMap<Integer, CacheMetaData> clock;
	protected int size;

}
