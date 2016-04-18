package org.cache2k.benchmark.jmh.suite.eviction.symmetrical;

/*
 * #%L
 * Cache benchmark suite based on JMH.
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

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.results.AggregationPolicy;

import static org.cache2k.benchmark.jmh.MiscResultRecorderProfiler.*;

/**
 * @author Jens Wilke
 */
@State(Scope.Thread)
public class HitCountRecorder {

  public long hitCount;
  public long missCount;

  @TearDown(Level.Iteration)
  public void tearDown() {
    addCounterResult(
      "hitCount", hitCount, "hit", AggregationPolicy.AVG
    );
    addCounterResult(
      "missCount", missCount, "miss", AggregationPolicy.AVG
    );
    addCounterResult(
      "opCount", hitCount + missCount, "op", AggregationPolicy.AVG
    );
    setResult(
      "hitRate",
      getCounterResult("hitCount") * 100D /
        getCounterResult("opCount"), "hitRate", AggregationPolicy.AVG
    );
  }

}
