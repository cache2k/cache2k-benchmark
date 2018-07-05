package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
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

/**
 * Requests to a cache as a stream of integer numbers, which represent
 * the cache key.
 *
 * @author Jens Wilke; created: 2013-08-25
 */
public abstract class AccessPattern {

  /**
   * The pattern does never end, {@link #hasNext()} always returns true.
   */
  public abstract boolean isEternal();

  public abstract boolean hasNext() throws Exception;

  public abstract int next() throws Exception;

  /**
   * Needs to be called after pattern is read to free up resources.
   */
  public void close() throws Exception { }

}
