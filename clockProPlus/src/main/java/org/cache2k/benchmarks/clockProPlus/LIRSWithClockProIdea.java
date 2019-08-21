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
@SuppressWarnings({"Duplicates", "unused", "WeakerAccess"})
public class LIRSWithClockProIdea implements ISimpleCache {

  public LIRSWithClockProIdea(int size, SimpleLIRS.Tuning tuning) {
    this.size = size;
    this.hirsRatio = tuning.getHirsRatio();
    this.hirsSize = (int)((double)this.size * this.hirsRatio + 0.5);
    this.lirsSize = this.size - this.hirsSize;
    this.currentHIRSSize = 0;
    this.currentLIRSSize = 0;
    this.demotedBlocks = 0;
    this.lirStack = new LinkedMap<>();
    this.hirStack = new LinkedMap<>();
    this.residentHIRList = new LinkedMap<>();
    this.nonresidentHIRsInStack = 0;
    this.totalSize = 0;
    this.totalCount = 0;
    this.totalAdjustment = 0;
    this.adjusts = 0;
  }

  @Override
  public boolean request(Integer address) {
    if (this.currentHIRSSize + this.currentLIRSSize > this.size) {
      System.err.println("Error");
      System.exit(0);
    }
    if (this.currentHIRSSize + this.currentLIRSSize
      + this.nonresidentHIRsInStack > this.size * 2) {
      System.err.println("Error");
      System.exit(0);
    }

    this.totalSize += this.lirsSize;
    this.totalCount++;

    if (this.lirStack.containsKey(address)) {
      CacheMetaData data = this.lirStack.get(address);
      if (data.isLIR()) {
        this.hitLIRInLIRS(address, data);
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
    return String.format("LIRSWithClockProIdea(%d,%.4f)", this.size, this.hirsRatio);
  }

  public double getFinalAvg() {
    return this.totalSize / this.totalCount / (double)this.size;
  }

  public double getAdjustment() {
    return this.totalAdjustment / this.adjusts;
  }

  protected void hitLIRInLIRS(Integer key, CacheMetaData data) {
    Integer firstKey = this.lirStack.firstKey();
    this.lirStack.remove(key);
    this.lirStack.put(key, data);
    if (firstKey == (int)key) {
      this.pruneStack();
    }
  }

  protected void pruneStack() {
    if (this.lirStack.isEmpty()) {
      return;
    }
    Integer firstKey = this.lirStack.firstKey();
    CacheMetaData tmpData = this.lirStack.get(firstKey);
    while (!tmpData.isLIR()) {
      this.lirStack.remove(firstKey);
      this.hirStack.remove(firstKey);
      this.adjustSize(false);
      if (!tmpData.isResident()) {
        this.nonresidentHIRsInStack--;
      }
      if (this.lirStack.isEmpty()) {
        break;
      }
      firstKey = this.lirStack.firstKey();
      tmpData = this.lirStack.get(firstKey);
    }
  }

  protected boolean hitInHIRInLIRStack(Integer key, CacheMetaData data) {
    this.adjustSize(true);
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
    while (this.currentLIRSSize >= this.lirsSize) {
      this.ejectLIR();
    }
    while (this.currentHIRSSize + this.currentLIRSSize >= this.size) {
      this.ejectResidentHIR();
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
    tmpData.demote(true);
    this.demotedBlocks++;
    this.residentHIRList.put(firstKey, tmpData);
    this.currentHIRSSize++;
    this.pruneStack();
  }

  protected void ejectResidentHIR() {
    Integer firstKey = this.residentHIRList.firstKey();
    CacheMetaData data = this.residentHIRList.remove(firstKey);
    if (this.lirStack.containsKey(firstKey)) {
      CacheMetaData tmpData = this.lirStack.get(firstKey);
      tmpData.setResidentState(false);
      this.nonresidentHIRsInStack++;
    }
    if (data.isDemoted()) {
      data.demote(false);
      this.demotedBlocks--;
    }
    this.currentHIRSSize--;
  }

  protected void hitInHIRList(Integer key) {
    CacheMetaData data = this.residentHIRList.get(key);
    if (data.isDemoted()) {
      data.demote(false);
      this.demotedBlocks--;
    }
    this.residentHIRList.remove(key);
    this.residentHIRList.put(key, data);
    this.lirStack.put(key, data);
    this.hirStack.put(key, data);
    this.limitStackSize();
  }

  protected void limitStackSize() {
    int pruneSize = this.currentHIRSSize + this.currentLIRSSize
      + this.nonresidentHIRsInStack - this.size * 2;
    Integer currentKey = this.hirStack.firstKey();
    while (pruneSize > 0) {
      Integer nextKey = this.hirStack.nextKey(currentKey);
      CacheMetaData tmpData = this.lirStack.get(currentKey);
      this.lirStack.remove(currentKey);
      this.hirStack.remove(currentKey);
      this.adjustSize(false);
      if (!tmpData.isResident()) {
        this.nonresidentHIRsInStack--;
      }
      pruneSize--;
      currentKey = nextKey;
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
    } else {
      while (this.currentHIRSSize + this.currentLIRSSize >= this.size) {
        while (this.currentLIRSSize > this.lirsSize) {
          this.ejectLIR();
        }
        this.ejectResidentHIR();
      }
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

  protected void adjustSize(boolean hitHIR) {
    int delta;
    if (hitHIR) {
      delta = 1;
    } else {
      delta = -1;
    }
    int oldSize = this.hirsSize;
    this.hirsSize += delta;
    if (this.hirsSize < 1) {
      this.hirsSize = 1;
    }
    if (this.hirsSize > this.size - 1) {
      this.hirsSize = this.size - 1;
    }
    int adjustedSize = Math.abs(this.hirsSize - oldSize);
    this.adjusts++;
    this.totalAdjustment += adjustedSize;
    this.lirsSize = this.size - this.hirsSize;
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
  protected int demotedBlocks;

  protected double totalSize;
  protected double totalCount;

  protected double totalAdjustment;
  protected double adjusts;

}
