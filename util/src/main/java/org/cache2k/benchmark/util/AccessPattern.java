package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
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

import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

/**
 * Requests to a cache as a stream of integer numbers, which represent
 * the cache key. Should not be used any more, with Java 8 better use
 * an {@code IntStream}.
 *
 * @author Jens Wilke
 */
public abstract class AccessPattern {

  public static AccessPattern of(IntStream stream) {
    final PrimitiveIterator.OfInt it = stream.iterator();
    return new AccessPattern() {
      @Override
      public boolean isEternal() {
        return false;
      }

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public int next() {
        return it.next();
      }
    };
  }

  /**
   * The pattern does never end, {@link #hasNext()} always returns true.
   */
  public abstract boolean isEternal();

  public abstract boolean hasNext();

  public abstract int next();

  /**
   * Needs to be called after pattern is read to free up resources.
   */
  public void close() { }

  public AccessPattern strip(int length) {
    return Patterns.strip(this, length);
  }

}
