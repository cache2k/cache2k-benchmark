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

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.tukaani.xz.XZInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

/**
 * A finite list of requests to a cache with some calculated properties.
 *
 * @author Jens Wilke; created: 2013-11-14
 */
public class AccessTrace implements Iterable<Integer> {

  private AccessPattern pattern;
  private int[] trace = null;
  private Integer[] objectTrace = null;
  private int valueCount = -1;
  private int lowValue = -Integer.MAX_VALUE;
  private int highValue = Integer.MIN_VALUE;
  private String name;

  /**
   * Read in a compressed trace
   */
  public AccessTrace(String _fileName) {
    InputStream _resourceInput = AccessTrace.class.getResourceAsStream(
      "/org/cache2k/benchmark/traces/" +
      _fileName);
    try {
      InputStream _inputForTrace;
      if (_fileName.endsWith(".bz2")) {
        _inputForTrace = new BZip2CompressorInputStream(_resourceInput);
      } else if (_fileName.endsWith(".xz")) {
        _inputForTrace = new XZInputStream(_resourceInput);
      } else {
        _inputForTrace = new GZIPInputStream(_resourceInput);
      }
      readFromStream(_inputForTrace);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Read in access trace from file. The file format is binary integer
   * values (4 bytes) in sequence, the order is big endian.
   */
  public AccessTrace(File f) throws IOException {
    FileChannel in = new FileInputStream(f).getChannel();
    ByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
    trace = new int[(int) (in.size() / 4)];
    buf.order(ByteOrder.BIG_ENDIAN);
    buf.asIntBuffer().get(trace);
    in.close();
  }

  /**
   * Read in access trace. The format is binary integer
   * values (4 bytes) in sequence, the order is big endian.
   */
  public AccessTrace(InputStream in) throws IOException {
    readFromStream(in);
  }

  public AccessTrace(int[] trace) {
    this.trace = trace;
  }

  private void readFromStream(final InputStream in) throws IOException {
    byte[] ba = readToByteArray(in);
    ByteBuffer buf = ByteBuffer.wrap(ba);
    trace = new int[ba.length / 4];
    buf.order(ByteOrder.BIG_ENDIAN);
    buf.asIntBuffer().get(trace);
    in.close();
  }

  private byte[] readToByteArray(InputStream in) throws IOException {
    byte[] ba = new byte[2048];
    int pos = 0;
    do {
      int _possibleLength = ba.length - pos;
      int l = in.read(ba, pos, _possibleLength);
      if (l > 0) {
        pos += l;
        if (pos >= ba.length) {
          byte[] ba2 = new byte[ba.length * 2];
          System.arraycopy(ba, 0, ba2, 0, ba.length);
          ba = ba2;
        }
      } else {
        break;
      }
    } while (true);
    byte[] ba2 = new byte[pos];
    System.arraycopy(ba, 0, ba2, 0, pos);
    return ba2;
  }

  /**
   * New trace of complete pattern.
   */
  public AccessTrace(AccessPattern... ps) {
    AccessPattern p = Patterns.concat(ps);
    if (p.isEternal()) {
      throw new IllegalArgumentException("Pattern is expected not to be eternal");
    }
    pattern = p;
  }

  public AccessTrace name(String s) {
    if (name != null) {
      throw new IllegalStateException();
    }
    name = s;
    return this;
  }

  public Integer[] getObjectArray() {
    if (objectTrace != null) {
      return objectTrace;
    }
    int[] _trace = getArray();
    Integer[] ia = new Integer[_trace.length];
    for (int i = 0; i < _trace.length; i++) {
      ia[i] = _trace[i];
    }
    return objectTrace = ia;
  }

  /**
   * Return the array of the trace. It is not allowed to modify the array, since this
   * is the trace data itself. This is a poor API, however, when used in benchmarking we
   * don't want to have to array copy in the timing.
   */
  public int[] getArray() {
    if (trace != null) {
      return trace;
    }
    try {
      trace = prepareTrace(pattern);
    } catch (Exception e) {
      throw new IllegalArgumentException("Error creating trace", e);
    }
    return trace;
  }

  public void write(File f) throws IOException {
    FileChannel out = new RandomAccessFile(f, "rw").getChannel();
    ByteBuffer buf = out.map(FileChannel.MapMode.READ_WRITE, 0, getArray().length * 4);
    buf.order(ByteOrder.BIG_ENDIAN);
    buf.asIntBuffer().put(getArray());
    out.close();
  }

  /**
   * Return an access pattern which starts at the beginning of the trace.
   */
  public AccessPattern newPattern() {
    final int[] ia = getArray();
    return new AccessPattern() {
      int idx = 0;

      @Override
      public boolean isEternal() {
        return false;
      }

      @Override
      public boolean hasNext() {
        return idx < ia.length;
      }

      @Override
      public int next() {
        return ia[idx++];
      }
    };
  }

  /**
   * Return the distinct values in this trace.
   */
  public int getValueCount() {
    if (valueCount < 0) {
      initStatistics();
    }
    return valueCount;
  }

  public int getHighValue() {
    if (valueCount < 0) {
      initStatistics();
    }
    return highValue;
  }

  public int getLowValue() {
    if (valueCount < 0) {
      initStatistics();
    }
    return lowValue;
  }

  public int getLength() {
    return getArray().length;
  }

  private void initStatistics() {
    IntSet _values = new IntOpenHashSet();
    for (int v : getArray()) {
      _values.add(v);
      if (v < lowValue) {
        lowValue = v;
      }
      if (v > highValue) {
        highValue = v;
      }
    }
    valueCount = _values.size();
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return String.format("AccessTrace(name=%s, length=%d, values=%d)",
      getName(),
      getLength(),
      getValueCount());
  }

  private static int[] prepareTrace(AccessPattern p, int _maxSize) throws Exception {
    int[] ia = new int[1024];
    int i = 0;
    while (p.hasNext() && i < _maxSize) {
      if (i >= ia.length) {
        int[] ia2 = new int[ia.length * 2];
        System.arraycopy(ia, 0, ia2, 0, i);
        ia = ia2;
      }
      ia[i] = p.next();
      i++;
    }
    int[] ia2 = new int[i];
    System.arraycopy(ia, 0, ia2, 0, i);
    p.close();
    return ia2;
  }

  /** Read until pattern ends */
  private static int[] prepareTrace(AccessPattern p) throws Exception {
    return prepareTrace(p, Integer.MAX_VALUE);
  }

  @Override
  public Iterator<Integer> iterator() {
    final int[] array = trace;
    return new Iterator<Integer>() {

      int idx = 0;

      @Override
      public boolean hasNext() {
        return idx < array.length;
      }

      @Override
      public Integer next() {
        return array[idx++];
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

}
