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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Read in a pattern completely and map all values to new values starting from 0.
 *
 * @author Jens Wilke
 */
class Base0Mapper extends AccessPattern {

  int value = 0;
  int index = -1;
  AccessPattern pattern;
  ArrayList<Integer> trace = new ArrayList<>();
  Map<Integer, Integer> mapping = new HashMap<>();

  Base0Mapper(AccessPattern p) {
    pattern = p;
  }

  @Override
  public boolean isEternal() {
    return false;
  }

  @Override
  public boolean hasNext() throws Exception {
    if (index < 0) {
      readAll();
      index = 0;
    }
    return index < trace.size();
  }

  @Override
  public int next() throws Exception {
    if (index < 0) {
      throw new IllegalAccessException("first call hasNext()");
    }
    return trace.get(index++);
  }

  void readAll() throws Exception {
    while (pattern.hasNext()) {
      int v = pattern.next();
      Integer t = mapping.get(v);
      if (t == null) {
        t = value++;
        mapping.put(v, t);
      }
      trace.add(t);
    }

  }

}
