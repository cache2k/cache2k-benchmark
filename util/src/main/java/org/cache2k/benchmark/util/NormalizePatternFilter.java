package org.cache2k.benchmark.util;

/*
 * #%L
 * util
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Read in a pattern completely and map all values to new values starting from 0.
 *
 * @author Jens Wilke; created: 2013-11-15
 */
class NormalizePatternFilter extends AccessPattern {

  int value = 0;
  int index = -1;
  AccessPattern pattern;
  ArrayList<Integer> trace = new ArrayList<>();
  Map<Integer, Integer> mapping = new HashMap<>();

  NormalizePatternFilter(AccessPattern p) {
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
