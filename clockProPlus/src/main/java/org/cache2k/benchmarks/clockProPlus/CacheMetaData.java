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

/**
 * Meta data for a cache entry.
 *
 * @author Cong Li
 */
@SuppressWarnings("WeakerAccess")
public class CacheMetaData {

  public boolean isReferenced() {
    return this.referenced;
  }

  public void setReference(boolean referenced) {
    this.referenced = referenced;
  }

  public boolean isLIR() {
    return this.lir;
  }

  public void setLIRState(boolean lir) {
    this.lir = lir;
  }

  public boolean isResident() {
    return this.resident;
  }

  public void demote(boolean demotion) {
    this.demoted = demotion;
  }

  public boolean isDemoted() {
    return this.demoted;
  }

  public void setResidentState(boolean resident) {
    this.resident = resident;
  }

  public void setAddress(int address) {
    this.address = address;
  }

  public int getAddress() {
    return this.address;
  }

  public void setNextAccessTime(int nextAccessTime) {
    this.nextAccessTime = nextAccessTime;
  }

  public int getNextAccessTime() {
    return this.nextAccessTime;
  }

  @Override
  public boolean equals(Object other){
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (!(other instanceof CacheMetaData)) {
      return false;
    }
    CacheMetaData data = (CacheMetaData)other;
    if (data.getAddress() == this.getAddress()) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return ((Integer)this.getAddress()).hashCode();
  }

  public CacheMetaData getNext() {
    return this.next;
  }

  public CacheMetaData getPrevious() {
    return this.previous;
  }

  public void linkNext(CacheMetaData next) {
    this.next = next;
    this.next.previous = this;
  }

  public void linkPrevious(CacheMetaData previous) {
    this.previous = previous;
    this.previous.next = this;
  }

  public void insertAfter(CacheMetaData target) {
    CacheMetaData tmpNext = target.getNext();
    target.linkNext(this);
    tmpNext.linkPrevious(this);
  }

  public void insertBefore(CacheMetaData target) {
    CacheMetaData tmpPrevious = target.getPrevious();
    target.linkPrevious(this);
    tmpPrevious.linkNext(this);
  }

  public void unlink() {
    CacheMetaData previous = this.previous;
    CacheMetaData next = this.next;
    previous.next = next;
    next.previous = previous;
    this.previous = null;
    this.next = null;
  }

  public void outputLinkedList() {
    CacheMetaData current = this;
    while (true) {
      String hot = "C";
      if (current.isLIR()) {
        hot = "H";
      }
      String resident = "";
      if (current.isResident()) {
        resident = "R";
      }
      String access = "";
      if (current.isReferenced()) {
        access = "A";
      }
      String outOfStack = "";
      if (!current.isInStack()) {
        outOfStack = "O";
      }
      System.out.printf("%d(%s%s%s%s), ", current.getAddress(), hot, resident, access, outOfStack);
      if (current.getPrevious().equals(this)) {
        break;
      } else {
        current = current.getPrevious();
      }
    }
    System.out.println();
  }

  public boolean isInStack() {
    return this.inStack;
  }

  public void setInStackStatus(boolean inStack) {
    this.inStack = inStack;
  }

  protected boolean lir;
  protected boolean resident;
  protected boolean demoted;
  protected int address;
  protected int nextAccessTime;
  protected boolean referenced;
  protected boolean inStack;
  protected CacheMetaData previous;
  protected CacheMetaData next;

}
