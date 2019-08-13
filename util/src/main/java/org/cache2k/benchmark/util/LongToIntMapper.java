package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
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

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.LongToIntFunction;
import java.util.stream.LongStream;

/**
 * Maps a stream of longs to integers.
 *
 * @author Jens Wilke
 */
public class LongToIntMapper implements LongToIntFunction {

  private int counter = 0;
  private Long2IntMap mapping = new Long2IntOpenHashMap();

  @Override
  public int applyAsInt(final long value) {
    if (mapping.containsKey(value)) {
      return mapping.get(value);
    }
    int t = counter++;
    mapping.put(value, t);
    return t;
  }

}
