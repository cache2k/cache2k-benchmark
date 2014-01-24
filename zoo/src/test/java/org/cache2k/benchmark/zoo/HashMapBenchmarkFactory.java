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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.Cache2kFactory;

/**
 * Produces a new {@link BenchmarkCache} instance for benchmark runs.
 *
 * @author Jens Wilke; created: 2013-12-08
 */
public class HashMapBenchmarkFactory extends BenchmarkCacheFactory {

  @Override
  public BenchmarkCache<Integer, Integer> create(int _maxElements) {
    final Cache2kFactory.CountingDataSource<Integer,Integer> cds = new Cache2kFactory.CountingDataSource<>();
    HashMapBenchmarkCache _cache = new HashMapBenchmarkCache(cds, _maxElements) {
      @Override
      public int getMissCount() {
        return cds.getMissCount();
      }
    };
    return _cache;
  }

}
