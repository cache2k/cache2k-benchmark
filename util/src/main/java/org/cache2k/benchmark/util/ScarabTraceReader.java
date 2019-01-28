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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.PrimitiveIterator;

/**
 * Read in scarab trace which is a sequence of longs and map it to integers.
 *
 * @author Jens Wilke
 */
public class ScarabTraceReader extends AccessPatternAdapter {

  public ScarabTraceReader(InputStream in) {
    super(
      new ObjectToIntegerMapper(
        new TraceIterator(
          new DataInputStream(
            new BufferedInputStream(in)))));
  }

  private static final class TraceIterator implements PrimitiveIterator.OfLong {

    private final DataInputStream input;

    private TraceIterator(DataInputStream input) {
      this.input = input;
    }

    @Override
    public boolean hasNext() {
      try {
        input.mark(100);
        input.readLong();
        input.reset();
        return true;
      } catch (EOFException e) {
        return false;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public long nextLong() {
      try {
        return input.readLong();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

  }

}
