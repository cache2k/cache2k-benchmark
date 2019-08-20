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

import org.cache2k.benchmark.prototype.EvictionPolicy;
import org.cache2k.benchmark.EvictionStatistics;
import org.cache2k.benchmark.prototype.LinkedEntry;

/**
 * Eviction policy based on the Clock-Pro idea with changes as used in cache2k V1.2.
 * This code is retained as-is and doesn't get improvements. The eviction results are
 * not 100% identical to cache2k V1.2, since the V1.2 version is triggering eviction
 * after an insert.
 *
 * <p>The Clock-Pro algorithm is explained by the authors in
 * <a href="http://www.ece.eng.wayne.edu/~sjiang/pubs/papers/jiang05_CLOCK-Pro.pdf">CLOCK-Pro:
 * An Effective Improvement of the CLOCK Replacement</a>
 * and <a href="http://www.slideshare.net/huliang64/clockpro">Clock-Pro: An Effective
 * Replacement in OS Kernel</a>.
 *
 * <p>This version uses a static allocation for hot and cold space sizes. No online or dynamic
 * optimization is done. Instead of the term "non resident cold pages" the word "ghost" is
 * used in this code. The "ghosts" only keep a hash code of the key, which is sufficient to decide
 * whether this key was seen before. Instead a of one clock cold and hot pages are kept in
 * two separate clocks, which means the entry sequence reshuffles on the promotion from
 * cold to hot. Instead of just a reference bit, a reference counter is used. On eviction
 * from the hot clock the counter is taken into account and entries with fewer references
 * are evicted first.
 *
 * @author Jens Wilke
 */
@SuppressWarnings({"Duplicates", "WeakerAccess"})
public class Cache2kV12Eviction<K, V> extends EvictionPolicy<K, V, Cache2kV12Eviction.Entry> {

  private long hotHits;
  private long coldHits;
  private long ghostHits;

  private long hotRunCnt;
  private long hotScanCnt;
  private long coldRunCnt;
  private long coldScanCnt;

  private int coldSize;
  private int hotSize;

  private Entry handCold;
  private Entry handHot;

  private Cache2kV12Eviction.Ghost[] ghosts;
  private Cache2kV12Eviction.Ghost ghostHead = new Ghost().shortCircuit();
  private int ghostSize = 0;
  private static final int GHOST_LOAD_PERCENT = 63;
  private Cache2kV1Tuning tuning;
  private long reshuffleCnt;

  /**
   * Created via reflection.
   */
  public Cache2kV12Eviction(int capacity, Cache2kV1Tuning tuning) {
    super(capacity);
    this.tuning = tuning;
    coldSize = 0;
    hotSize = 0;
    handCold = null;
    handHot = null;
    ghosts = new Ghost[4];
  }

  @Override
  public Entry newEntry(K key, V value) {
    Entry e = new Entry(key, value);
    insertIntoReplacementList(e);
    return e;
  }

  @Override
  public void recordHit(final Entry e) {
    e.hitCnt++;
  }

  @Override
  public Entry evict() {
    Entry e = findEvictionCandidate();
    removeFromReplacementListOnEvict(e);
    return e;
  }

  @Override
  public void remove(final Entry e) {
    removeFromReplacementList(e);
  }

  private long sumUpListHits(Entry e) {
    if (e == null) { return 0; }
    long cnt = 0;
    Entry head = e;
    do {
      cnt += e.hitCnt;
      e = e.next;
    } while (e != head);
    return cnt;
  }

  public long getHotMax() {
    return getSize() * tuning.hotMaxPercentage / 100;
  }

  public long getGhostMax() {
    return getSize() / 2 + 1;
  }

  /**
   * Track the entry on the ghost list and call the usual remove procedure.
   */
  public void removeFromReplacementListOnEvict(final Entry e) {
    insertCopyIntoGhosts(e);
    removeFromReplacementList(e);
  }

  /**
   * Remove, expire or eviction of an entry happens. Remove the entry from the
   * replacement list data structure.
   *
   * <p>Why don't generate ghosts here? If the entry is removed because of
   * a programmatic remove or expiry we should not occupy any resources.
   * Removing and expiry may also take place when no eviction is needed at all,
   * which happens when the cache size did not hit the maximum yet. Producing ghosts
   * would add additional overhead, when it is not needed.
   */
  protected void removeFromReplacementList(Entry e) {
    if (e.isHot()) {
      hotHits += e.hitCnt;
      handHot = Entry.removeFromCyclicList(handHot, e);
      hotSize--;
    } else {
      coldHits += e.hitCnt;
      handCold = Entry.removeFromCyclicList(handCold, e);
      coldSize--;
    }
  }

  private void insertCopyIntoGhosts(Entry e) {
    int hc = e.getHashCode();
    Ghost g = lookupGhost(hc);
    if (g != null) {
      /*
       * either this is a hash code collision, or a previous ghost hit that was not removed.
       */
      Ghost.moveToFront(ghostHead, g);
      return;
    }
    if (ghostSize >= getGhostMax()) {
      g = ghostHead.prev;
      Ghost.removeFromList(g);
      boolean f = removeGhost(g, g.hash);
    } else {
      g = new Ghost();
    }
    g.hash = hc;
    insertGhost(g, hc);
    Ghost.insertInList(ghostHead, g);
  }

  public long getSize() {
    return hotSize + coldSize;
  }

  protected void insertIntoReplacementList(Entry e) {
    Ghost g = lookupGhost(e.getHashCode());
    if (g != null) {
      /*
       * don't remove ghosts here, save object allocations.
       * removeGhost(g, g.hash);  Ghost.removeFromList(g);
       */
      ghostHits++;
      e.setHot(true);
      hotSize++;
      handHot = Entry.insertIntoTailCyclicList(handHot, e);
      return;
    }
    coldSize++;
    handCold = Entry.insertIntoTailCyclicList(handCold, e);
  }

  private Entry runHandHot() {
    hotRunCnt++;
    Entry hand = handHot;
    Entry coldCandidate = hand;
    long lowestHits = Long.MAX_VALUE;
    long hotHits = this.hotHits;
    int initialMaxScan = hotSize >> 2 + 1;
    int maxScan = initialMaxScan;
    long decrease = ((hand.hitCnt + hand.next.hitCnt) >> tuning.hitCounterDecreaseShift) + 1;
    while (maxScan-- > 0) {
      long hitCnt = hand.hitCnt;
      if (hitCnt < lowestHits) {
        lowestHits = hitCnt;
        coldCandidate = hand;
        if (hitCnt == 0) {
          break;
        }
      }
      if (hitCnt < decrease) {
        hand.hitCnt = 0;
        hotHits += hitCnt;
      } else {
        hand.hitCnt = hitCnt - decrease;
        hotHits += decrease;
      }
      hand = hand.next;
    }
    this.hotHits = hotHits;
    long scanCount = initialMaxScan - maxScan;
    hotScanCnt += scanCount;
    reshuffleCnt++;
    handHot = Entry.removeFromCyclicList(hand, coldCandidate);
    hotSize--;
    coldCandidate.setHot(false);
    return coldCandidate;
  }

  /**
   * Runs cold hand an in turn hot hand to find eviction candidate.
   */
  protected Entry findEvictionCandidate() {
    coldRunCnt++;
    Entry hand = handCold;
    int scanCnt = 1;
    if (hand == null) {
      hand = refillFromHot(hand);
    }
    if (hand.hitCnt > 0) {
      hand = refillFromHot(hand);
      do {
        scanCnt++;
        coldHits += hand.hitCnt;
        hand.hitCnt = 0;
        Entry e = hand;
        reshuffleCnt++;
        hand = Entry.removeFromCyclicList(e);
        coldSize--;
        e.setHot(true);
        hotSize++;
        handHot = Entry.insertIntoTailCyclicList(handHot, e);
      } while (hand != null && hand.hitCnt > 0);
    }
    if (hand == null) {
      hand = refillFromHot(hand);
    }
    coldScanCnt += scanCnt;
    handCold = hand.next;
    return hand;
  }

  private Entry refillFromHot(Entry hand) {
    long hotMax = getHotMax();
    while (hotSize >  hotMax || hand == null) {
      Entry e = runHandHot();
      if (e != null) {
        hand =  Entry.insertIntoTailCyclicList(hand, e);
        coldSize++;
      }
    }
    return hand;
  }

  private Ghost lookupGhost(int hash) {
    Ghost[] tab = ghosts;
    int n = tab.length;
    int mask = n - 1;
    int idx = hash & (mask);
    Ghost e = tab[idx];
    while (e != null) {
      if (e.hash == hash) {
        return e;
      }
      e = e.another;
    }
    return null;
  }

  private void insertGhost(Ghost e2, int hash) {
    Ghost[] tab = ghosts;
    int n = tab.length;
    int mask = n - 1;
    int idx = hash & (mask);
    e2.another = tab[idx];
    tab[idx] = e2;
    ghostSize++;
    int maxFill = n * GHOST_LOAD_PERCENT / 100;
    if (ghostSize > maxFill) {
      expand();
    }
  }

  private void expand() {
    Ghost[] tab = ghosts;
    int n = tab.length;
    int mask;
    int idx;
    Ghost[] newTab = new Ghost[n * 2];
    mask = newTab.length - 1;
    for (Ghost g : tab) {
      while (g != null) {
        idx = g.hash & mask;
        Ghost next = g.another;
        g.another = newTab[idx];
        newTab[idx] = g;
        g = next;
      }
    }
    ghosts = newTab;
  }

  private boolean removeGhost(Ghost g, int hash) {
    Ghost[] tab = ghosts;
    int n = tab.length;
    int mask = n - 1;
    int idx = hash & (mask);
    Ghost e = tab[idx];
    if (e == g) {
      tab[idx] = e.another;
      ghostSize--;
      return true;
    } else {
      while (e != null) {
        Ghost another = e.another;
        if (another == g) {
          e.another = another.another;
          ghostSize--;
          return true;
        }
        e = another;
      }
    }
    return false;
  }

  @Override
  public EvictionStatistics getEvictionStats() {
    return new EvictionStatistics() {
      @Override
      public long getScanCount() {
        return coldScanCnt + hotScanCnt;
      }
      @Override
      public long getReshuffleCount() {
        return reshuffleCnt;
      }
    };
  }

  public String toString() {
    return this.getClass().getSimpleName() +
      "(coldSize=" + coldSize +
      ", hotSize=" + hotSize +
      ", hotMaxSize=" + getHotMax() +
      ", ghostSize=" + ghostSize +
      ", coldHits=" + (coldHits + sumUpListHits(handCold)) +
      ", hotHits=" + (hotHits + sumUpListHits(handHot)) +
      ", ghostHits=" + ghostHits +
      ", coldRunCnt=" + coldRunCnt +// identical to the evictions anyways
      ", coldScanCnt=" + coldScanCnt +
      ", hotRunCnt=" + hotRunCnt +
      ", hotScanCnt=" + hotScanCnt +
      ", totalScanCnt=" + (hotScanCnt + coldScanCnt) +
      ", reshuffleCnt=" + reshuffleCnt + ")";
  }

  /**
   * Ghost representing a entry we have seen and evicted from the cache. We only store
   * the hash to save memory, since holding the key references may cause a size overhead.
   */
  private static class Ghost {

    /** Modified hashcode of the key */
    int hash;
    /** Hash table chain */
    Ghost another;
    /** LRU double linked list */
    Ghost next;
    /** LRU double linked list */
    Ghost prev;

    Ghost shortCircuit() {
      return next = prev = this;
    }

    static void removeFromList(final Ghost e) {
      e.prev.next = e.next;
      e.next.prev = e.prev;
      e.next = e.prev = null;
    }

    static void insertInList(final Ghost head, final Ghost e) {
      e.prev = head;
      e.next = head.next;
      e.next.prev = e;
      head.next = e;
    }

    static void moveToFront(final Ghost head, final Ghost e) {
      removeFromList(e);
      insertInList(head, e);
    }

  }

  static class Entry extends LinkedEntry<Entry, Object, Object> {

    long hitCnt;
    boolean hot;

    private Entry(final Object key, final Object value) {
      super(key, value);
    }

    public int getHashCode() {
      int hc = getKey().hashCode();
      return hc ^ hc >>> 16;
    }

    public void setHot(final boolean v) {
      hot = v;
    }

    boolean isHot() {
      return hot;
    }

  }

}
