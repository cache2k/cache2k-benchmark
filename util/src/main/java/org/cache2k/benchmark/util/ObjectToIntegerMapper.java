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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Read in an access pattern of arbitrary objects supporting eqauls and hashCode
 * and map all values to new values starting from 0.
 *
 * @author Jens Wilke
 */
public class ObjectToIntegerMapper extends AccessPattern {

  private int value = 0;
  private Iterator<?> iterator;
  private Map<Object, Integer> mapping = new HashMap<>();

  public ObjectToIntegerMapper(Iterator<?> it) {
    iterator = it;
  }

  @Override
  public boolean isEternal() {
    return false;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public int next() {
    Object v = iterator.next();
    Integer t = mapping.get(v);
    if (t == null) {
      t = value++;
      mapping.put(v, t);
    }
    return t;
  }

}
