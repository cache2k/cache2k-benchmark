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
