package org.cache2k.benchmark.zoo;

/*
 * #%L
 * cache2k-benchmark-zoo
 * %%
 * Copyright (C) 2013 - 2014 headissue GmbH, Munich
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.cache2k.Cache;
import org.cache2k.CacheSource;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A cache with LRU evection, that does a time lookup on every request for the expiry
 * decision. Experimental version from 2008. We keep it for reference benchmarks.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("all")
public class FairSimpleCache<K, T> extends AbstractCache<K, T> {

  /** Maximum amount of elements in cache */
  int maxElements;
  /** Time in seconds we keep an element */
  long maxLinger;

  Hashtable<K, Entry<T>> hash;
  private CacheSource<K, T> cacheSource;
  /** Statistics */
  int prefetchCnt;
  int usageCnt;
  int missCnt;
  int refreshCnt;
  Enumeration<Entry<T>> throwOutEnumeration;
  Entry<T> head;
  /** We give the cache a name for resource statistics */
  String name = hashCode() + "";

  public FairSimpleCache() {
    this("noname", null, 200, 1000 * 60 * 10);
  }

  public FairSimpleCache(String _name) {
    this(_name, null, 200, 1000 * 60 * 10);
  }

  public FairSimpleCache(String _name, CacheSource<K, T> g, int _maxElements,
    int _lingerSeconds) {
    name = _name;
    maxElements = _maxElements;
    maxLinger = _lingerSeconds * 1000;
    cacheSource = g;
    init();
  }

  public String getName() {
    return name;
  }

  public void setCacheSource(CacheSource<K, T> g) {
    cacheSource = g;
  }

  public void setResourceName(String s) {
    name = s;
  }

  /** Set the maximum size of the cache */
  public void setMaxElements(int n) {
    maxElements = n;
  }

  /**
   * Set the time in seconds after which the cache does an update of the element
   */
  public void setLingerSeconds(int s) {
    maxLinger = s;
  }

  /** instead of specifying a getter, it is possible to override this method */
  protected T getReal(K key) {
    try {
      return cacheSource.get(key);
    } catch (Throwable ignore) {
      throw new RuntimeException("never happens in testing", ignore);
    }
  }

  protected void init() {
    clear();
    if (maxElements == 0) {
      throw new RuntimeException("maxElements must by >0");
    }
  }

  @Override
  public void clear() {
    hash = new Hashtable<>();
    head = new Entry<>();
    head.next = head;
    head.prev = head;
  }

  final Entry<T> newEntry(K key) {
    Entry<T> e = new Entry<>();
    e.key = key;
    hash.put(key, e);
    insertInList(e);
    if (hash.size() > maxElements) {
      throwOneOut();
    }
    return e;
  }

  /**
   * Returns object mapped to key
   */
  @Override
  public T get(K key) {
    long t = System.currentTimeMillis();
    Entry<T> e = null;
    synchronized (this) {
      e = hash.get(key);
      if (e == null) {
        missCnt++;
        e = newEntry(key);
      } else {
        moveToFront(e);
      }
    }
    if (e.timeStamp < t) {
      synchronized (e) {
        if (e.timeStamp < t) {
          e.timeStamp = -1;
          refreshCnt++;
          e.value = getReal(key);
          e.timeStamp = t + maxLinger;
        }
      }
    }
    usageCnt++;
    return e.value;
  }

  /**
   * Almost same as get. If the object is currently retrived by a prallel thread
   * this method returns immediately.
   */
  @Override
  public void prefetch(K key) {
    prefetchCnt++;
    long t = System.currentTimeMillis();
    Entry<T> e = null;
    synchronized (this) {
      e = hash.get(key);
      if (e == null) {
        missCnt++;
        e = newEntry(key);
      } else {
        moveToFront(e);
      }
    }
    if (e.timeStamp != -1 && e.timeStamp < t) {
      synchronized (e) {
        if (e.timeStamp != -1 && e.timeStamp < t) {
          e.timeStamp = -1;
          refreshCnt++;
          e.value = getReal(key);
          e.timeStamp = t + maxLinger;
        }
      }
    }
    e.lastUsage = usageCnt++;
  }

  /**
   * Same as get but only returns the object if it is in the cache, null
   * otherwise. No request to the layer below is made.
   */
  @Override
  public T peek(K key) {
    long t = System.currentTimeMillis();
    Entry<T> e = null;
    synchronized (this) {
      e = hash.get(key);
      if (e != null && e.timeStamp >= t) {
        moveToFront(e);
        return e.value;
      }

    }
    return null;
  }

  @Override
  public void put(K key, T value) {
    long t = System.currentTimeMillis();
    Entry<T> e = null;
    synchronized (this) {
      e = hash.get(key);
      if (e == null) {
        missCnt++;
        e = newEntry(key);
      }
      e.value = value;
      e.timeStamp = t + maxLinger;
    }
  }

  /**
   * Remove the object mapped to key from the cache.
   */
  @Override
  public void remove(K key) {
    synchronized (this) {
      Entry<T> e = hash.remove(key);
      if (e != null) {
        removeFromList(e);
      }
    }
  }

  @Override
  public void destroy() {
  }

  /**
   * Called under big lock.
   */
  void throwOneOut() {
    Entry<T> e = head.prev;
    removeFromList(e);
    hash.remove(e.key);
  }

  /** Return status information */
  @Override
  public String toString() {
    int _realUsageCnt = usageCnt - prefetchCnt;
    String _hitRate = "-";
    if (_realUsageCnt > 0) {
      Double hr =
                (_realUsageCnt - refreshCnt - missCnt) * 100.0 / _realUsageCnt;
      _hitRate = Double.toString(hr);
    }
    return "Cache{" + name + "}" + "(" +
                "size=" + hash.size() + ", " +
                "usageCnt=" + _realUsageCnt + ", " +
                "refreshCnt=" + (refreshCnt - missCnt) + ", " +
                "missCnt=" + missCnt + ", " +
                "prefetchCnt=" + prefetchCnt + ", " +
                "hitRate=" + _hitRate + "%)";
  }

  final void removeFromList(Entry<T> e) {
    e.prev.next = e.next;
    e.next.prev = e.prev;
  }

  final void insertInList(Entry<T> e) {
    Entry<T> _head = head;
    e.prev = _head;
    e.next = _head.next;
    e.next.prev = e;
    _head.next = e;
  }

  final void moveToFront(Entry<T> e) {
    removeFromList(e);
    insertInList(e);
  }

  /** An entry in the hash table */
  static class Entry<T> {
    int lastUsage;
    long timeStamp;
    Object key;
    T value;
    Entry<T> next;
    Entry<T> prev;
  }

}
