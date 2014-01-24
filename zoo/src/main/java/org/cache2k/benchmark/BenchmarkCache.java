package org.cache2k.benchmark;

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

/**
 * Interface to a cache implementation we use for benchmarking. The
 * cache is read through and just returns the input key as value via
 * get. The real misses are counted by the calls the cache performs
 * to the loader or cache source.
 *
 * @author Jens Wilke; created: 2013-06-15
 */
public abstract class BenchmarkCache<K, T> {

  public abstract T get(K key);

  /** free up all resources of the cache */
  public abstract void destroy();

  /** Configured maximum entry count of the cache */
  public abstract int getCacheSize();

  /**
   * How many misses happened in the cache. It is illegal here to
   * use the miss counter of the target cache implementation. Instead
   * the misses need to be counted by the cache source.
   */
  public abstract int getMissCount();

  /** Statistics string produced by the cache. */
  public String getStatistics() { return "<none>"; }

  /** Optional, checks the cache integrity. Called after a run. */
  public void checkIntegrity() { }

  /**
   * Return the original implementation. We use this for experimentation to
   * get and set some runtime parameters during benchmarks runs.
   */
  public Object getOriginalCache() { return null; }

}
