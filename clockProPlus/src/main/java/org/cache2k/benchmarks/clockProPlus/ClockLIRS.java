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

import org.apache.commons.collections4.map.HashedMap;

/**
 * @author Cong Li
 */
@SuppressWarnings({"Duplicates", "WeakerAccess", "unused"})
public class ClockLIRS implements ISimpleCache {

  public ClockLIRS(int size, ClockPro.Tuning tuning) {
    this.size = size;
    this.clockMap = new HashedMap<>();
    this.coldTarget = (int)(this.size * tuning.getColdRatio());
    this.hotTarget = this.size - this.coldTarget;
    this.coldSize = 0;
    this.hotSize = 0;
    this.testSize = 0;
    this.handHot = null;
    this.handCold = null;
    this.handTest = null;
    this.lru = null;
  }

  @Override
  public boolean request(Integer address) {
    if (this.clockMap.containsKey(address)) {
      CacheMetaData data = this.clockMap.get(address);
      if (data.isResident()) {
        data.setReference(true);
        return true;
      }
    }
    this.advanceHandHot();
    this.advanceHandCold();
    if (this.clockMap.containsKey(address)) {
      CacheMetaData data = this.clockMap.get(address);
      this.assertStatement(data.isInStack(),
        "Error: hit a non-resident cold page out of the stack!");
      this.assertStatement(!data.isReferenced(),
        "Error: hit a referenced non-resident cold page");

      this.assertStatement(this.hotSize + this.coldSize == this.size, "Error: cache not full!");
      this.evictColdPage();

      data.setResidentState(true);
      this.testSize--;
      this.coldSize++;

      if (data == this.handTest) {
        this.advanceHandTest();
      }

      this.promoteColdPage(data);
      this.advanceHandCold();

      return false;
    }

    if (this.coldSize + this.hotSize == this.size) {
      this.evictColdPage();
    }

    CacheMetaData data = new CacheMetaData();
    data.setAddress(address);
    data.setResidentState(true);
    this.addToClock(data);
    if (this.coldSize == 0 && this.hotSize < this.hotTarget) {
      data.setLIRState(true);
      this.hotSize++;
    } else {
      data.setLIRState(false);
      if (this.handCold == null) {
        this.handCold = data;
      }
      this.coldSize++;
    }
    this.advanceHandCold();
    this.pruneTestPages();
    return false;
  }

  @Override
  public String toString() {
    return String.format("ClockLIRS(%d)", this.size);
  }

  protected void pruneTestPages() {
    while (this.coldSize + this.hotSize + this.testSize > this.size * 2) {
      this.assertStatement(this.handTest.isInStack() && !this.handTest.isLIR()
          && !this.handTest.isResident(),
        "Error: hand_test does not stop at a test page!");

      this.removeNonresidentColdPage(this.handTest);
    }
  }

  protected void evictColdPage() {
    this.assertStatement(!this.handCold.isLIR() && this.handCold.isResident() && !this.handCold.isReferenced(),
      "Error: hand_cold does not stop at a non-referenced resident cold page!");

    CacheMetaData data = this.handCold;
    this.handCold = this.handCold.getNext();
    data.setResidentState(false);
    this.coldSize--;
    this.testSize++;
    if (this.handTest == null) {
      this.handTest = data;
    }
    if (!data.isInStack()) {
      if (data == this.lru) {
        this.lru = data.getPrevious();
      }
      this.removeNonresidentColdPage(data);
    }
  }

  protected void promoteColdPage(CacheMetaData data) {
    data.setLIRState(true);
    this.hotSize++;
    this.coldSize--;

    this.moveToLRU(data);

    while (this.hotSize > this.hotTarget) {
      this.demoteHotPage();
    }
  }

  protected void demoteHotPage() {
    this.assertStatement(!this.handHot.isReferenced(), "Error: hand_hot stops on a referenced page!");

    CacheMetaData data = this.handHot;
    this.handHot = this.handHot.getNext();

    data.setLIRState(false);
    this.hotSize--;
    this.coldSize++;
    data.setInStackStatus(false);
    this.moveToLRU(data);

    this.advanceHandHot();
  }

  protected void advanceHandTest() {
    if (this.testSize == 0) {
      this.handTest = null;
      return;
    }
    while (this.handTest.isLIR() || this.handTest.isResident()) {
      this.handTest = this.handTest.getNext();
    }
  }

  protected void advanceHandHot() {
    if (this.handHot == null) {
      return;
    }
    CacheMetaData data = this.handHot;

    while (!data.isLIR() || data.isReferenced()) {
      CacheMetaData nextPosition = data.getNext();
      if (data.isLIR()) {
        data.setReference(false);
        this.lru = data;
      } else {
        if (data.isResident()) {
          if (data.isReferenced()) {
            data.setReference(false);
            this.lru = data;
            if (data.equals(this.handCold)) {
              this.handCold = nextPosition;
            }
          } else {
            data.setInStackStatus(false);
          }
        } else {
          this.removeNonresidentColdPage(data);
        }
        if (data.equals(this.handTest)) {
          this.handTest = nextPosition;
        }
      }
      data = nextPosition;
    }
    this.handHot = data;
  }

  protected void advanceHandCold() {
    if (this.coldSize == 0) {
      return;
    }

    while (this.handCold.isLIR() || !this.handCold.isResident() || this.handCold.isReferenced()) {
      CacheMetaData data = this.handCold;
      this.handCold = this.handCold.getNext();
      if (!data.isLIR()) {
        if (data.isReferenced()) {
          data.setReference(false);
          if (data.isInStack()) {
            this.promoteColdPage(data);
          } else {
            data.setInStackStatus(true);
            this.moveToLRU(data);
          }
        }
      }
    }
  }

  protected void addToClock(CacheMetaData data) {
    if (this.lru == null) {
      data.linkNext(data);
      this.lru = data;
      this.handHot = data;
    } else {
      data.insertAfter(this.lru);
      this.lru = data;
    }

    data.setInStackStatus(true);
    this.clockMap.put(data.getAddress(), data);
  }

  protected void moveToLRU(CacheMetaData data) {
    if (data == this.lru) {
      return;
    }

    data.unlink();
    data.insertAfter(this.lru);

    this.lru = data;
  }

  protected void removeNonresidentColdPage(CacheMetaData data) {
    Integer address = data.getAddress();
    if (data == this.handTest) {
      this.handTest = this.handTest.getNext();
    }
    this.clockMap.remove(address);
    data.unlink();
    this.testSize--;

    this.advanceHandTest();
  }

  protected void assertStatement(boolean result, String message) {
    if (!result) {
      System.err.println(message);
    }
  }

  public void outputStatus() {
    if (this.lru != null) {
      this.lru.outputLinkedList();
    }
    if (this.handHot != null) {
      System.out.printf("Hot: %d, ", this.handHot.getAddress());
    }
    if (this.handCold != null) {
      System.out.printf("Cold: %d, ", this.handCold.getAddress());
    }
    if (this.handTest != null) {
      System.out.printf("Test: %d, ", this.handTest.getAddress());
    }
    System.out.println();
  }

  protected HashedMap<Integer, CacheMetaData> clockMap;
  protected CacheMetaData handHot;
  protected CacheMetaData handCold;
  protected CacheMetaData handTest;
  protected CacheMetaData lru;
  protected int size;
  protected int coldSize;
  protected int hotSize;
  protected int testSize;
  protected int coldTarget;
  protected int hotTarget;

}
