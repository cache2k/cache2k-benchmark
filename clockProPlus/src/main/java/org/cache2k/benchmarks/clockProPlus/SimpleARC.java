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
 * @author Cong Li
 */
@SuppressWarnings("WeakerAccess")
public class SimpleARC implements ISimpleCache {

  public SimpleARC(int size) {
    this.size = size;
    this.recencySize = 0;
    this.topLRU1 = new LinkedMap<>();
    this.topLRU2 = new LinkedMap<>();
    this.bottomLRU1 = new LinkedMap<>();
    this.bottomLRU2 = new LinkedMap<>();
  }

  @Override
  public boolean request(Integer address) {
    if (this.topLRU2.containsKey(address)) {
      CacheMetaData data = this.topLRU2.get(address);
      this.topLRU2.remove(address);
      this.topLRU2.put(address, data);
      this.printSize();
      return true;
    }
    if (this.topLRU1.containsKey(address)) {
      CacheMetaData data = this.topLRU1.get(address);
      this.topLRU1.remove(address);
      this.topLRU2.put(address, data);
      this.printSize();
      return true;
    }
    if (this.bottomLRU1.containsKey(address)) {
      int delta = 0;
      if (this.bottomLRU1.size() >= this.bottomLRU2.size()) {
        delta = 1;
      } else {
        delta = this.bottomLRU2.size() / this.bottomLRU1.size();
      }
      this.recencySize += delta;
      if (this.recencySize > this.size) {
        this.recencySize = this.size;
      }
      this.replace(address);
      CacheMetaData data = this.bottomLRU1.get(address);
      this.bottomLRU1.remove(address);
      this.topLRU2.put(address, data);
      this.printSize();
      return false;
    }
    if (this.bottomLRU2.containsKey(address)) {
      int delta;
      if (this.bottomLRU2.size() >= this.bottomLRU1.size()) {
        delta = 1;
      } else {
        delta = this.bottomLRU1.size() / this.bottomLRU2.size();
      }
      this.recencySize -= delta;
      if (this.recencySize < 0) {
        this.recencySize = 0;
      }
      this.replace(address);
      CacheMetaData data = this.bottomLRU2.get(address);
      this.bottomLRU2.remove(address);
      this.topLRU2.put(address, data);
      this.printSize();
      return false;
    }
    if (this.topLRU1.size() + this.bottomLRU1.size() == this.size) {
      if (this.topLRU1.size() < this.size) {
        Integer firstKey = this.bottomLRU1.firstKey();
        this.bottomLRU1.remove(firstKey);
        this.replace(address);
      } else {
        Integer firstKey = this.topLRU1.firstKey();
        this.topLRU1.remove(firstKey);
      }
    } else {
      if (this.topLRU1.size() + this.bottomLRU1.size() +
        this.topLRU2.size() + this.bottomLRU2.size() >= this.size) {
        if (this.topLRU1.size() + this.bottomLRU1.size() +
          this.topLRU2.size() + this.bottomLRU2.size() == 2 * this.size) {
          Integer firstKey = this.bottomLRU2.firstKey();
          this.bottomLRU2.remove(firstKey);
        }
        this.replace(address);
      }
    }
    CacheMetaData data = new CacheMetaData();
    this.topLRU1.put(address, data);
    this.printSize();
    return false;
  }

  @Override
  public String toString() {
    return String.format("ARC(%d)", this.size);
  }

  protected void replace(Integer key) {
    if (!this.topLRU1.isEmpty() &&
      (this.topLRU1.size() > this.recencySize ||
        (this.bottomLRU2.containsKey(key) &&
          this.topLRU1.size() == this.recencySize))) {
      Integer firstKey = this.topLRU1.firstKey();
      CacheMetaData tmpData = this.topLRU1.get(firstKey);
      this.topLRU1.remove(firstKey);
      this.bottomLRU1.put(firstKey, tmpData);
    } else {
      Integer firstKey = this.topLRU2.firstKey();
      CacheMetaData tmpData = this.topLRU2.get(firstKey);
      this.topLRU2.remove(firstKey);
      this.bottomLRU2.put(firstKey, tmpData);
    }
  }

  protected void printSize() {
    if (this.topLRU1.size() + this.topLRU2.size() > this.size) {
      System.err.printf("%d, %d, %d, %d, %d\n", this.topLRU1.size(), this.topLRU2.size(),
        this.bottomLRU1.size(), this.bottomLRU2.size(), this.recencySize);
    }
  }

  protected int size;
  protected int recencySize;
  protected LinkedMap<Integer, CacheMetaData> topLRU1;
  protected LinkedMap<Integer, CacheMetaData> topLRU2;
  protected LinkedMap<Integer, CacheMetaData> bottomLRU1;
  protected LinkedMap<Integer, CacheMetaData> bottomLRU2;

}
