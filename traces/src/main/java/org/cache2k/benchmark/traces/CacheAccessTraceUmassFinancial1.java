package org.cache2k.benchmark.traces;

/*
 * #%L
 * Benchmarks: Access trace collection
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

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.cache2k.benchmark.util.AccessTrace;
import org.cache2k.benchmark.util.Patterns;
import org.cache2k.benchmark.util.UmassTraceReaderLbaOnly;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * UMass Financial1 trace. Truncated to one million requests using only
 * read access and only the first LBA of a sequence.
 *
 * <p>fileName=Financial1.spc.bz2, sha1sum=5f705113ef5ab0b7cf033894e03ff0b050927ffd
 *
 * @author Jens Wilke
 * @see UmassTraceReaderLbaOnly
 */
public class CacheAccessTraceUmassFinancial1 {

  static int LIMIT_LENGTH = 1000 * 1000;

  static AccessTrace provideUmassTrace(String s) throws IOException {
    return
      new AccessTrace(
        Patterns.strip(
          new UmassTraceReaderLbaOnly(
            new BZip2CompressorInputStream(
              new FileInputStream(
                TraceResourceDirectory.TRACE_DIRECTORY +
                  "/umass.edu/" + s))), LIMIT_LENGTH))
      .disableOptHitCount();
  }

  static final TraceCache.Provider PROVIDER = new TraceCache.Provider() {
    @Override
    public AccessTrace provide() throws IOException {
      return provideUmassTrace("Financial1.spc.bz2");
    }
  };

  public static AccessTrace getInstance() {
    return TraceCache.getTraceLazy(CacheAccessTraceUmassFinancial1.class.getName(), PROVIDER);
  }

}
