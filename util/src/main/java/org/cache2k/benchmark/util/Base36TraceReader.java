package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.Charset;

/**
 * Read in an access log trace from an event guide web application.
 * Each line is the request of an event detail information.
 * The code is the integer ID base 36.
 *
 * @author Jens Wilke; created: 2013-11-19
 */
public class Base36TraceReader extends AccessPattern {

  LineNumberReader reader;
  int value;

  /**
   * Read in, default charset.
   */
  public Base36TraceReader(InputStream in, Charset cs) {
    reader = new LineNumberReader(new InputStreamReader(in, cs));
  }

  @Override
  public boolean isEternal() {
    return false;
  }

  @Override
  public boolean hasNext() {
    try {
      String s;
      do {
        s = reader.readLine();
        if (s == null) {
          return false;
        }
      } while (s.startsWith("#"));
        value = Integer.parseInt(s, 36);
    } catch(Exception ex) {
      throw new RuntimeException(ex);
    }
    return true;
  }

  @Override
  public int next() {
    return value;
  }

  @Override
  public void close() {
    try {
      reader.close();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
