
package org.cache2k.benchmark.zoo;

/*
 * #%L
 * cache2k-benchmark-zoo
 * %%
 * Copyright (C) 2013 headissue GmbH, Munich
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
 * A very simple cache implementations. Looks on the wall clock for every request to
 * check for expiry. For eviction it probes over 20 elements and selects the one lowest
 * usage counter. Served well in production since the year 2000, but does not yield
 * very good evition efficiency.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("all")
public class SimpleCache<K, T> implements Cache<K, T> {

  /** Maximum amount of elements in cache */
  int maxElements;
  /** Time in seconds we keep an element */
  long maxLinger;

  Hashtable<Object, Entry> hash;
  private CacheSource<K, T> cacheSource;
  /** Statistics */
  int prefetchCnt;
  int usageCnt;
  int missCnt;
  int refreshCnt;
  Enumeration<Entry> throwOutEnumeration;
  /** We give the cache a name for resource statistics */
  String name = hashCode() + "";

  public SimpleCache() {
    this("noname", null, 200, 60 * 10);
  }

  public SimpleCache(String _name) {
    this(_name, null, 200, 60 * 10);
  }

  public SimpleCache(
            String _name,
            CacheSource<K, T> g,
            int _maxElements,
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

  /**
   * instead of specifying a getter, it is possible to override this method
   */
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
  }

  /**
   * Returns object mapped to key
   */
  @Override
  public T get(K key) {
    long t = System.currentTimeMillis();
    Entry e = null;
    synchronized (this) {
      e = hash.get(key);
      if (e == null) {
        missCnt++;
        e = new Entry();
        e.key = key;
        hash.put(key, e);
        if (hash.size() >= maxElements) {
          throwOneOut();
        }
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
    e.lastUsage = usageCnt++;
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
    Entry e = null;
    synchronized (this) {
      e = hash.get(key);
      if (e == null) {
        missCnt++;
        e = new Entry();
        e.key = key;
        hash.put(key, e);
        if (hash.size() >= maxElements) {
          throwOneOut();
        }
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
    Entry e = null;
    synchronized (this) {
      e = hash.get(key);
    }
    if (e != null && e.timeStamp >= t) {
      return e.value;
    }
    return null;
  }

  @Override
  public void put(K key, T value) {
    long t = System.currentTimeMillis();
    Entry e = null;
    synchronized (this) {
      e = hash.get(key);
      if (e == null) {
        missCnt++;
        e = new Entry();
        e.key = key;
        hash.put(key, e);
        if (hash.size() >= maxElements) {
          throwOneOut();
        }
      }
    }
    synchronized (e) {
      e.value = value;
      e.timeStamp = t + maxLinger;
    }
  }

  /**
   * Remove the object mapped to key from the cache.
   */
  @Override
  public void remove(K key) {
    hash.remove(key);
  }

  void throwOneOut() {
    Enumeration<Entry> enu = throwOutEnumeration;
    if (enu == null) {
      enu = hash.elements();
    }
    Entry _candidate = null;
    int _itsLu = 0;
    long t = System.currentTimeMillis();
    for (int i = 0; i < 20; i++) {
      if (!enu.hasMoreElements()) {
        enu = hash.elements();
        enu.hasMoreElements();
      }
      Entry e = enu.nextElement();
      if (e == null) {
        i = 0;
        continue;
      }
      long t2 = e.timeStamp;
      if (t2 <= 0) {
        continue;
      }
      if (t2 < t) {
        _candidate = e;
        break;
      }
      int c = Math.abs(usageCnt - e.lastUsage);
      if (_candidate == null || c > _itsLu) {
        _candidate = e;
        _itsLu = c;
      }
    }
    throwOutEnumeration = enu;
    if (_candidate != null) {
      synchronized (_candidate) {
        hash.remove(_candidate.key);
      }
    }
  }

  public void destroy() {

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

  /** A entry in the hash table */
  class Entry {

    int lastUsage;
    long timeStamp;
    Object key;
    T value;

  }

}
