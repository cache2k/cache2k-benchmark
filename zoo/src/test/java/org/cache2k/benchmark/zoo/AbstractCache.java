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
import org.cache2k.CacheEntry;
import org.cache2k.ClosableIterator;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implement advanced methods that the experimental caches don't
 * have and need. Just throw {@link UnsupportedOperationException}.
 *
 * @author Jens Wilke; created: 2014-03-30
 */
public abstract class AbstractCache<K, T> implements Cache<K, T> {

  @Override
  public Map<K, T> getAll(Set<? extends K> keys) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAllAtOnce(Set<K> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void prefetch(Set<K> keys) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void prefetch(List<K> keys, int _startIndex, int _afterEndIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClosableIterator<CacheEntry<K, T>> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getTotalEntryCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void purge() { }

  @Override
  public void flush() { }

}
