package org.cache2k.benchmark.impl2015;

/*
 * #%L
 * Benchmarks: implementation variants
 * %%
 * Copyright (C) 2013 - 2018 headissue GmbH, Munich
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

import java.io.Serializable;
import java.util.List;

/**
 * Cache configuration. Adheres to bean standard.
 *
 * @author Jens Wilke; created: 2013-06-25
 */
public class CacheConfig<K, V> implements Serializable {

  private String name;
  private Class<?> implementation;
  private int maxSize = 2000;
  private int entryCapacity = 2000;
  private int maxSizeHighBound = Integer.MAX_VALUE;
  private int maxSizeLowBound = 0;
  private int heapEntryCapacity = -1;
  private boolean backgroundRefresh = false;
  private long expiryMillis  = 10 * 60 * 1000;
  private long exceptionExpiryMillis = -1;
  private boolean keepDataAfterExpired = true;
  private boolean sharpExpiry = false;
  private List<Object> moduleConfiguration;
  private boolean suppressExceptions = true;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getEntryCapacity() {
    return entryCapacity;
  }

  public void setEntryCapacity(int v) {
    this.entryCapacity = v;
  }

  /**
   * @deprecated Use {@link #getEntryCapacity()}
   */
  public int getMaxSize() {
    return entryCapacity;
  }

  /**
   * @deprecated Use {@link #setEntryCapacity(int)}
   */
  public void setMaxSize(int v) {
    this.entryCapacity = v;
  }

  /**
   * @deprecated not used.
   */
  public int getMaxSizeHighBound() {
    return maxSizeHighBound;
  }

  /**
   * @deprecated not used.
   */
  public void setMaxSizeHighBound(int maxSizeHighBound) {
    if (maxSize > maxSizeHighBound) {
      maxSize = maxSizeHighBound;
    }
    this.maxSizeHighBound = maxSizeHighBound;
  }

  /**
   * @deprecated not used.
   */
  public int getMaxSizeLowBound() {
    return maxSizeLowBound;
  }

  /**
   * @deprecated not used.
   */
  public void setMaxSizeLowBound(int maxSizeLowBound) {
    if (maxSize < maxSizeLowBound) {
      maxSize = maxSizeLowBound;
    }
    this.maxSizeLowBound = maxSizeLowBound;
  }

  public boolean isBackgroundRefresh() {
    return backgroundRefresh;
  }

  public void setBackgroundRefresh(boolean backgroundRefresh) {
    this.backgroundRefresh = backgroundRefresh;
  }

  void checkNull(Object v) {
    if (v == null) {
      throw new NullPointerException("null value not allowed");
    }
  }

  public boolean isEternal() {
    return expiryMillis == -1 || expiryMillis == Long.MAX_VALUE;
  }

  /**
   * Set cache entry don't expiry by time.
   */
  public void setEternal(boolean v) {
    if (v) {
      this.expiryMillis = -1;
    }
  }

  /**
   * @depcrecated use {@link #setExpiryMillis}
   */
  public void setExpirySeconds(int v) {
    if (v == -1 || v == Integer.MAX_VALUE) {
      expiryMillis = -1;
    }
    expiryMillis = v * 1000;
  }

  public int getExpirySeconds() {
    if (isEternal()) {
      return -1;
    }
    return (int) (expiryMillis / 1000);
  }

  public long getExpiryMillis() {
    return expiryMillis;
  }

  /**
   * The expiry value of all entries. If an entry specific expiry calculation is
   * determined this is the maximum expiry time. A value of -1 switches expiry off, that
   * means entries are kept for an eternal time, a value of 0 switches caching off.
   */
  public void setExpiryMillis(long expiryMillis) {
    this.expiryMillis = expiryMillis;
  }

  public long getExceptionExpiryMillis() {
    return exceptionExpiryMillis;
  }

  /**
   */
  public void setExceptionExpiryMillis(long v) {
    exceptionExpiryMillis = v;
  }

  public boolean isKeepDataAfterExpired() {
    return keepDataAfterExpired;
  }

  /**
   * Expired data is kept in the cache until the entry is evicted by the replacement
   * algorithm. This consumes memory, but if the data is accessed again the previous
   * data can be used by the cache source for optimizing, e.g. for a get if-modified-since.
   *
   * @see org.cache2k.CacheSourceWithMetaInfo
   */
  public void setKeepDataAfterExpired(boolean v) {
    this.keepDataAfterExpired = v;
  }

  public boolean isSharpExpiry() {
    return sharpExpiry;
  }

  /**
   */
  public void setSharpExpiry(boolean sharpExpiry) {
    this.sharpExpiry = sharpExpiry;
  }

  public boolean isSuppressExceptions() {
    return suppressExceptions;
  }

  /**
   */
  public void setSuppressExceptions(boolean suppressExceptions) {
    this.suppressExceptions = suppressExceptions;
  }

  public int getHeapEntryCapacity() {
    return heapEntryCapacity;
  }

  /**
   * Maximum number of entries that the cache keeps in the heap.
   * Only relevant if a storage modules is defined.
   */
  public void setHeapEntryCapacity(int v) {
    this.heapEntryCapacity = v;
  }

  public List<Object> getModuleConfiguration() {
    return moduleConfiguration;
  }

  public void setModuleConfiguration(List<Object> moduleConfiguration) {
    this.moduleConfiguration = moduleConfiguration;
  }

  public Class<?> getImplementation() {
    return implementation;
  }

  public void setImplementation(Class<?> cacheImplementation) {
    this.implementation = cacheImplementation;
  }

}
