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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads in a file line by line and maps all distinct line contents to an
 * integer value. The line content can be anything.
 *
 * @author Jens Wilke; created: 2013-11-15
 */
public class NormalizeTraceReader extends AccessPattern {

  int value = 0;
  int index = -1;
  LineNumberReader reader;
  ArrayList<Integer> trace = new ArrayList<>();
  Map<String, Integer> mapping = new HashMap<>();

  public NormalizeTraceReader(InputStream s, Charset cs) {
    this(new LineNumberReader(new InputStreamReader(s, cs)));
  }

  public NormalizeTraceReader(LineNumberReader r) {
    reader = r;
  }

  public NormalizeTraceReader(File f, Charset cs) throws IOException {
    this(new FileInputStream(f), cs);
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
    return trace.get(index++);
  }

  void readAll() throws Exception {
    String s;
    while ((s = reader.readLine()) != null) {
      Integer t = mapping.get(s);
      if (t == null) {
        t = value++;
        mapping.put(s, t);
      }
      trace.add(t);
    }
    reader.close();
  }

}
