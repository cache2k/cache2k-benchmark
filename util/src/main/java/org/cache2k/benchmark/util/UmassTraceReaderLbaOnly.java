package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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

/**
 * Reads in a trace file from the UMass Trace Repository.
 * Only reads and only the LBA will are emitted.
 *
 * <p>Documentation and trace files can be downloaded at
 * <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMass Trace Repository</a>.
 *
 * @author Jens Wilke
 */
public class UmassTraceReaderLbaOnly extends AccessPattern {

  LineNumberReader reader;
  int lba;

  public UmassTraceReaderLbaOnly(InputStream in) {
    reader = new LineNumberReader(new InputStreamReader(in));
  }

  @Override
  public boolean hasNext() throws Exception {
    char rw = ' ';
    do {
      String s = reader.readLine();
      if (s == null) {
        return false;
      }
      try {
        String[] sa = s.split(",", 5);
        if (sa.length < 4) { continue; }
        lba = Integer.parseInt(sa[1]);
        rw = Character.toLowerCase(sa[3].charAt(0));
      } catch (Exception ex) {
        throw new IOException("Error at line number " + reader.getLineNumber(), ex);
      }
    } while (rw == 'w');
    return true;
  }

  @Override
  public boolean isEternal() {
    return false;
  }

  @Override
  public int next() throws Exception {
    return lba;
  }

}
