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
