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

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * Reads in a trace file from the UMass Trace Repository.
 * For simplification ignores ASU and size. In the Financial and WebSearch traces
 * its always 0 and 1 respectively. There is also no write in the files.
 *
 * <p>Documentation and trace files can be downloaded at
 * <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMass Trace Repository</a>.
 *
 * @author Jens Wilke
 */
public class UmassTraceReader extends AccessPattern {

  public static final String DIRECTORY_ENV = "org.cache2k.benchmark.traces.umass";
  public static final String DEFAULT_DIRECTORY = "/opt/headissue/cache2k-benchmark-trace//umass.edu";
  private static final int BLOCK_SIZE = 512;
  private static final int ASU_SPACE = 128;

  private LineNumberReader reader;
  private int lba;
  private int count;

  public static AccessPattern of(String fileName) throws IOException {
    String directory = System.getenv(DIRECTORY_ENV);
    if (directory == null) {
      directory = DEFAULT_DIRECTORY;
    }
    if (!new File(directory).isDirectory()) {
      throw new IllegalArgumentException(
        "UMass traces are missing. " +
        "Download the traces and specify the directory via environment variable: " + DIRECTORY_ENV);
    }
    return new UmassTraceReader(new BZip2CompressorInputStream(
      new FileInputStream(directory + File.separator + fileName)));
  }

  private UmassTraceReader(InputStream in) {
    reader = new LineNumberReader(new InputStreamReader(in));
  }

  @Override
  public boolean hasNext() {
    if (count > 0) {
      count--;
      lba += ASU_SPACE;
      return true;
    }
    try {
      for(;;) {
        String s = reader.readLine();
        if (s == null) {
          return false;
        }
        String[] sa = s.split(",", 5);
        int asu = Integer.parseInt(sa[0]);
        if (asu >= ASU_SPACE) {
          throw new IllegalArgumentException("asu too high: " + asu);
        }
        long addr = Integer.parseInt(sa[1]) * ASU_SPACE + asu;
        if (addr > Integer.MAX_VALUE) {
          throw new IllegalArgumentException("Integer overflow");
        }
        lba = (int) addr;
        int size = Integer.parseInt(sa[2]);
        count = size / BLOCK_SIZE - 1;
        return true;
      }
    } catch (Exception ex) {
      throw new RuntimeException("Error at line number " + reader.getLineNumber(), ex);
    }
  }

  @Override
  public boolean isEternal() {
    return false;
  }

  @Override
  public int next() {
    return lba;
  }

}
