package org.cache2k.benchmark.zoo;

/*
 * #%L
 * cache2k-benchmark-zoo
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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
import org.cache2k.CacheEntryProcessor;
import org.cache2k.CacheManager;
import org.cache2k.ClosableIterator;
import org.cache2k.EntryProcessingResult;
import org.cache2k.FetchCompletedListener;

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
  public boolean contains(K key) {
    throw new UnsupportedOperationException();
  }

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

  @Override
  public boolean putIfAbsent(K key, T value) {
    return false;
  }

  @Override
  public CacheEntry<K, T> peekEntry(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CacheEntry<K, T> getEntry(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() { destroy(); }

  @Override
  public boolean isClosed() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CacheManager getCacheManager() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void removeAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final T peekAndReplace(K key, T _value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean replace(K key, T _newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean replace(K key, T _oldValue, T _newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final T peekAndRemove(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final T peekAndPut(K key, T value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean remove(K key, T value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void fetchAll(Set<? extends K> keys, boolean replaceExistingValues, FetchCompletedListener l) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final <R> Map<K, EntryProcessingResult<R>> invokeAll(Set<? extends K> keys, CacheEntryProcessor<K, T, R> p, Object... _objs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Map<K, T> peekAll(Set<? extends K> keys) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <X> X requestInterface(Class<X> _type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends T> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <R> R invoke(K key, CacheEntryProcessor<K, T, R> entryProcessor, Object... _args) {
    throw new UnsupportedOperationException();
  }
}
