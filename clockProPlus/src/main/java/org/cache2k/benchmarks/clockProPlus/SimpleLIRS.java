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
import org.cache2k.benchmark.EvictionTuning;

/**
 * @author Cong Li
 */
@SuppressWarnings({"WeakerAccess", "unused", "Duplicates"})
public class SimpleLIRS implements ISimpleCache {

  public SimpleLIRS(int size, Tuning tuning) {
    this.size = size;
    this.hirsRatio = tuning.getHirsRatio();
    this.hirsSize = (int)((double)this.size * this.hirsRatio + 0.5);
    this.lirsSize = this.size - this.hirsSize;
    this.currentHIRSSize = 0;
    this.currentLIRSSize = 0;
    this.lirStack = new LinkedMap<>();
    this.hirStack = new LinkedMap<>();
    this.residentHIRList = new LinkedMap<>();
    this.nonresidentHIRsInStack = 0;
  }

  @Override
  public boolean request(Integer address) {
    if (this.currentHIRSSize + this.currentLIRSSize > this.size + 1) {
      System.err.println("Error");
    }
    if (this.lirStack.containsKey(address)) {
      CacheMetaData data = this.lirStack.get(address);
      if (data.isLIR()) {
        this.hitInLIRS(address, data);
        return true;
      }
      return this.hitInHIRInLIRStack(address, data);
    }
    if (this.residentHIRList.containsKey(address)) {
      this.hitInHIRList(address);
      return true;
    }
    this.processMiss(address);
    return false;
  }

  @Override
  public String toString() {
    return String.format("LIRS(%d,%.4f)", this.size, this.hirsRatio);
  }

  protected void hitInHIRList(Integer key) {
    CacheMetaData data = this.residentHIRList.get(key);
    this.residentHIRList.remove(key);
    this.residentHIRList.put(key, data);
    this.lirStack.put(key, data);
    this.hirStack.put(key, data);
    this.limitStackSize();
  }

  protected void hitInLIRS(Integer key, CacheMetaData data) {
    Integer firstKey = this.lirStack.firstKey();
    this.lirStack.remove(key);
    this.lirStack.put(key, data);
    if (firstKey == (int)key) {
      this.pruneStack();
    }
  }

  protected boolean hitInHIRInLIRStack(Integer key, CacheMetaData data) {
    boolean result = data.isResident();
    data.setLIRState(true);
    this.lirStack.remove(key);
    this.hirStack.remove(key);
    if (result) {
      this.residentHIRList.remove(key);
      this.currentHIRSSize--;
    } else {
      data.setResidentState(true);
      this.nonresidentHIRsInStack--;
    }
    if (this.currentLIRSSize >= this.lirsSize) {
      this.ejectLIR();
    }
    this.lirStack.put(key, data);
    this.currentLIRSSize++;
    return result;
  }

  protected void ejectLIR() {
    Integer firstKey = this.lirStack.firstKey();
    CacheMetaData tmpData = this.lirStack.get(firstKey);
    tmpData.setLIRState(false);
    this.lirStack.remove(firstKey);
    this.currentLIRSSize--;
    if (this.currentHIRSSize >= this.hirsSize) {
      this.ejectResidentHIR();
    }
    this.residentHIRList.put(firstKey, tmpData);
    this.currentHIRSSize++;
    this.pruneStack();
  }

  protected void ejectResidentHIR() {
    Integer firstKey = this.residentHIRList.firstKey();
    this.residentHIRList.remove(firstKey);
    if (this.lirStack.containsKey(firstKey)) {
      CacheMetaData tmpData = this.lirStack.get(firstKey);
      tmpData.setResidentState(false);
      this.nonresidentHIRsInStack++;
    }
    this.currentHIRSSize--;
  }

  protected void limitStackSize() {
    int pruneSize = this.currentHIRSSize + this.currentLIRSSize + this.nonresidentHIRsInStack - this.size * 2;
    Integer currentKey = this.hirStack.firstKey();
    while (pruneSize > 0) {
      Integer nextKey = this.hirStack.nextKey(currentKey);
      CacheMetaData tmpData = this.lirStack.get(currentKey);
      this.lirStack.remove(currentKey);
      this.hirStack.remove(currentKey);
      if (!tmpData.isResident()) {
        this.nonresidentHIRsInStack--;
      }
      pruneSize--;
      currentKey = nextKey;
    }
  }

  protected void pruneStack() {
    Integer firstKey = this.lirStack.firstKey();
    CacheMetaData tmpData = this.lirStack.get(firstKey);
    while (!tmpData.isLIR()) {
      this.lirStack.remove(firstKey);
      this.hirStack.remove(firstKey);
      if (!tmpData.isResident()) {
        this.nonresidentHIRsInStack--;
      }
      firstKey = this.lirStack.firstKey();
      tmpData = this.lirStack.get(firstKey);
    }
  }

  protected void processMiss(Integer key) {
    if (this.currentLIRSSize < this.lirsSize && this.currentHIRSSize == 0) {
      CacheMetaData data = new CacheMetaData();
      data.setLIRState(true);
      data.setResidentState(true);
      this.lirStack.put(key, data);
      this.currentLIRSSize++;
      return;
    } else if (this.currentHIRSSize >= this.hirsSize) {
      this.ejectResidentHIR();
    }
    CacheMetaData data = new CacheMetaData();
    data.setLIRState(false);
    data.setResidentState(true);
    this.lirStack.put(key, data);
    this.hirStack.put(key, data);
    this.residentHIRList.put(key, data);
    this.currentHIRSSize++;
    this.limitStackSize();
  }

  protected LinkedMap<Integer, CacheMetaData> lirStack;
  protected LinkedMap<Integer, CacheMetaData> hirStack;
  protected LinkedMap<Integer, CacheMetaData> residentHIRList;
  protected int size;
  protected double hirsRatio;
  protected int lirsSize;
  protected int hirsSize;
  protected int currentLIRSSize;
  protected int currentHIRSSize;
  protected int nonresidentHIRsInStack;

  public static class Tuning implements EvictionTuning {

    private double hirsRatio = 0.01;

    public Tuning() { }

    public Tuning(final double hirsRatio) {
      this.hirsRatio = hirsRatio;
    }

    public double getHirsRatio() {
      return hirsRatio;
    }

    @Override
    public String toString() {
      return String.format("%.2f", hirsRatio);
    }

  }

}
