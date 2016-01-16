
package org.cache2k.benchmark.impl2015;

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

import java.io.Closeable;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public interface Cache<K, T> extends Iterable<CacheEntry<K, T>>, Closeable {

  String getName();

  void clear();

  T get(K key);

  CacheEntry<K, T> getEntry(K key);

  T peek(K key);

  CacheEntry<K, T> peekEntry(K key);

  boolean contains(K key);

  void put(K key, T value);

  void remove(K key);

  boolean remove(K key, T value);

  int getTotalEntryCount();

  ClosableIterator<CacheEntry<K, T>> iterator();

  void removeAll();

  void destroy();

  void close();

  boolean isClosed();

  String toString();

}
