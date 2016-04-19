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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * A finite list of requests to a cache with some calculated properties.
 *
 * @author Jens Wilke; created: 2013-11-14
 */
public class AccessTrace implements Iterable<Integer> {

  AccessPattern pattern;
  private int[] trace = null;
  private Integer[] objectTrace = null;
  int valueCount = -1;
  int lowValue = -Integer.MAX_VALUE;
  int highValue = Integer.MIN_VALUE;
  HashMap<Integer, Integer> size2opt = new HashMap<>();
  HashMap<Integer, Integer> size2random = new HashMap<>();

  /**
   * Read in access trace from file. The file format is binary integer
   * values (4 bytes) in sequence, the order is big endian.
   */
  public AccessTrace(File f) throws IOException {
    FileChannel in = new FileInputStream(f).getChannel();
    ByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
    trace = new int[(int) (in.size() / 4)];
    buf.order(ByteOrder.BIG_ENDIAN);
    buf.asIntBuffer().get(getTrace());
    in.close();
  }

  /**
   * Read in access trace. The format is binary integer
   * values (4 bytes) in sequence, the order is big endian.
   */
  public AccessTrace(InputStream in) throws IOException {
    byte[] ba = readToByteArray(in);
    ByteBuffer buf = ByteBuffer.wrap(ba);
    trace = new int[(int) (ba.length / 4)];
    buf.order(ByteOrder.BIG_ENDIAN);
    buf.asIntBuffer().get(getTrace());
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

  /**
   * New trace of complete pattern.
   */
  public AccessTrace(int _maxSize, AccessPattern... ps) {
    AccessPattern p = Patterns.concat(ps);
    pattern = Patterns.strip(p, _maxSize);
  }

  /**
   * New trace from partial pattern. Reads the pattern to the end but at most
   * the maximum size.
   */
  public AccessTrace(AccessPattern p, int _maxSize) {
    pattern = Patterns.strip(p, _maxSize);
  }

  public Integer[] getObjectTrace() {
    if (objectTrace != null) {
      return objectTrace;
    }
    int[] _trace = getTrace();
    Integer[] ia = new Integer[_trace.length];
    for (int i = 0; i < _trace.length; i++) {
      ia[i] = _trace[i];
    }
    return objectTrace = ia;
  }

  public int[] getTrace() {
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

  /**
   * Opt calculation is extremely slow for traces with no hits at all.
   * This is used to predefine the opt hit count to a known value.
   */
  public AccessTrace setOptHitCount(int _size, int _count) {
    size2opt.put(_size, _count);
    return this;
  }

  public AccessTrace setRandomHitCount(int _size, int _count) {
    size2random.put(_size, _count);
    return this;
  }

  public AccessTrace disableOptHitCount() {
    size2opt = null;
    return this;
  }

  public void write(File f) throws IOException {
    FileChannel out = new RandomAccessFile(f, "rw").getChannel();
    ByteBuffer buf = out.map(FileChannel.MapMode.READ_WRITE, 0, getTrace().length * 4);
    buf.order(ByteOrder.BIG_ENDIAN);
    buf.asIntBuffer().put(getTrace());
    out.close();
  }

  /**
   * Return an access pattern which starts at the beginning of the trace.
   */
  public AccessPattern newPattern() {
    final int[] ia = getTrace();
    return new AccessPattern() {
      int idx = 0;

      @Override
      public boolean isEternal() {
        return false;
      }

      @Override
      public boolean hasNext() throws Exception {
        return idx < ia.length;
      }

      @Override
      public int next() throws Exception {
        return ia[idx++];
      }
    };
  }

  /**
   * Return the array of the trace. It is not allowed to modify the array, since this
   * is the trace data itself. This is a poor API, however, when used in benchmarking we
   * don't want to have to array copy in the timing.
   */
  public int[] getArray() {
    return getTrace();
  }

  /**
   * Returns the hits according to Beladys optimal algorithm for the given cache size.
   * The calculation is done only once for a trace and a size.
   */
  public int getOptHitCount(int _size) {
    if (_size <= 0) {
      throw new IllegalArgumentException("size must be greater 0");
    }
    if (size2opt == null) {
      return 0;
    }
    Integer v = size2opt.get(_size);
    if (v != null) {
      return v;
    }
    OptimumReplacementCalculation c = new OptimumReplacementCalculation(_size, getTrace());
    size2opt.put(_size, c.getHitCount());
    return c.getHitCount();
  }

  public HitRate getOptHitRate(int _size) {
    return new HitRate(getOptHitCount(_size));
  }

  public HitRate getRandomHitRate(int _size) {
    if (_size <= 0) {
      throw new IllegalArgumentException("size must be greater 0");
    }
    Integer v = size2random.get(_size);
    if (v == null) {
      v = calcRandomHits(_size);
      size2random.put(_size, v);
    }
    return new HitRate(v);
  }

  int calcRandomHits(int _size, int _seed) {
    IntSet _cache = new IntOpenHashSet();
    IntList _list = new IntArrayList();
    Random _random = new Random(_seed);
    int _hitCnt = 0;
    for (int v : getTrace()) {
      if(_cache.contains(v)) {
        _hitCnt++;
      } else {
        if (_cache.size() == _size) {
          int cnt = _random.nextInt(_cache.size());
          _cache.remove(_list.get(cnt));
          _list.remove(cnt);
        }
        _cache.add(v);
        _list.add(v);
      }
    }
    return _hitCnt;
  }

  int calcRandomHits(int _size) {
    return
      (calcRandomHits(_size, 1802) +
      calcRandomHits(_size, 4711)) / 2;
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

  public int getTraceLength() {
    return getTrace().length;
  }

  private void initStatistics() {
    IntSet _values = new IntOpenHashSet();
    for (int v : getTrace()) {
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

  public String toString() {
    return Arrays.toString(getTrace());
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

  public class HitRate {
    int hitCount;

    public HitRate(int hitCount) {
      this.hitCount = hitCount;
    }

    public int getCount() {
      return hitCount;
    }

    public double getFactor() {
      return hitCount * 1D / getTraceLength();
    }

    /** Hitrate in percent from 0 to 100 */
    public int getPercent() {
      return getPer(100);
    }

    /** Hitrate in millis from 0 to 1000 */
    public int get3digit() {
      return getPer(1000);
    }

    /** Hitrate from 0 to 10000 */
    public int get4digit() {
      return getPer(10000);
    }

    public int getPer(long v) {
      if (getTraceLength() == 0) {
        throw new IllegalStateException("empty trace");
      }
      return (int) ((hitCount * v + getTraceLength() / 2) / getTraceLength());
    }

  }

  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {

      int idx = 0;

      @Override
      public boolean hasNext() {
        return idx < getTrace().length;
      }

      @Override
      public Integer next() {
        return getTrace()[idx++];
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

}
