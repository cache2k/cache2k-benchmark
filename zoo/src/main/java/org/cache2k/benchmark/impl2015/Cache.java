
package org.cache2k.benchmark.impl2015;

/*
 * #%L
 * Benchmarks: implementation variants
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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
