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

import java.util.Random;

/**
 * Operations on patterns. This is a toolbox to manipulate access patterns
 * and create artificial access traces. There is no heavy usage of this und not
 * much tests, so beware of bugs.
 *
 * @author Jens Wilke; created: 2013-11-24
 */
public class Patterns {

  public static AccessPattern explode(AccessPattern p, int _factor) {
    return new Explode(p, _factor);
  }

  /**
   * Sequence from 0 to size (exclusive)
   */
  public static AccessPattern sequence(int _size) {
    return new Sequence(0, _size);
  }

  public static AccessPattern sequence(int _start, int _end) {
    return new Sequence(_start, _end);
  }

  public static AccessPattern revert(AccessPattern p) {
    return new Revert(p);
  }

  public static AccessPattern loop(AccessPattern p, int n) {
    return new Loop(p, n);
  }

  public static AccessPattern concat(AccessPattern... p) {
    return new Concat(p);
  }

  public static AccessPattern strip(AccessPattern p, int _stripCount) {
    return new Strip(p, _stripCount);
  }

  public static class Sequence extends AccessPattern {

    int pos = 0;
    int end;

    /**
     * Counting up sequence from start to end (exclusive).
     */
    public Sequence(int _start, int _end) {
      pos = _start;
      end = _end;
    }

    @Override
    public boolean isEternal() {
      return false;
    }

    public boolean hasNext() {
      return pos < end;
    }

    public int next() {
      return pos++;
    }

  }

  /**
   * Sequence: start=1, increment=1, step=4, end=3, count=3, gives: 1 2 3  5 6 7  9 10 11
   */
  public static class InterleavedSequence extends AccessPattern {

    int pos;
    int start;
    int increment;
    int end;
    int step;
    int sequenceCount;

    public InterleavedSequence(int _start, int _end, int _increment, int _step, int _sequenceCount) {
      pos = _start;
      this.start = _start;
      this.increment = _increment;
      this.end = _end;
      this.step = _step;
      this.sequenceCount = _sequenceCount;
    }

    @Override
    public boolean isEternal() {
      return false;
    }

    public boolean hasNext() {
      if (pos >= end) {
        start += step;
        end += step;
        pos = start;
        sequenceCount--;
      }
      return sequenceCount > 0;
    }

    public int next() {
      return pos += increment;
    }

  }

  static class Loop extends AccessPattern {

    AccessPattern pattern;
    int size;
    int pos;
    int[] buffer;
    int count;
    int minValue = Integer.MAX_VALUE;
    int maxValue = 0;

    Loop(AccessPattern _pattern) {
      this(_pattern, -1);
    }

    Loop(AccessPattern pattern, int _count) {
      if (pattern.isEternal()) {
        throw new IllegalArgumentException("pattern is eternal");
      }
      this.pattern = pattern;
      count = _count;
      int i = 0;
      int ia[] = new int[10000];
      while (pattern.hasNext()) {
        int v = pattern.next();
        if (v > maxValue) { maxValue = v; }
        if (v < minValue) { minValue = v; }
        ia[i++] = v;
        if (i >= ia.length) {
          int ia2[] = new int[ia.length * 2];
          System.arraycopy(ia, 0, ia2, 0, ia.length);
          ia = ia2;
        }
      }
      buffer = ia;
      size = i;
      pos = 0;
    }

    @Override
    public boolean isEternal() {
      return count < 0;
    }

    public boolean hasNext() {
      return count > 0;
    }

    public int next() {
      int v = buffer[pos++];
      if (pos >= size) {
        pos = 0;
        count--;
      }
      return v;
    }

  }

  /**
   * Creates a pattern which is
   */
  public static class Explode extends Loop {

    int factor;
    int index = 0;

    public Explode(final AccessPattern pattern, final int _factor) {
      super(pattern, 1);
      factor = _factor;
    }

    public int next() {
      int v = buffer[pos];
      v += maxValue * index;
      if (v < 0) {
        throw new IllegalArgumentException("Integer overflow, is input pattern flattened? maxValue=" + maxValue);
      }
      index++;
      if (index >= factor) { pos++; index = 0; }
      if (pos >= size) {
        pos = 0;
        count--;
      }
      return v;
    }

  }

  public static class RevertLoop extends Loop {

    public RevertLoop(AccessPattern _pattern) {
      super(_pattern);
    }

    public RevertLoop(AccessPattern _pattern, int _count) {
      super(_pattern, _count);
    }

    public boolean hasNext() {
      return pos != 0;
    }

    public int next() {
      pos--;
      if (pos < 0) {
        pos = size - 1;
        count--;
      }
      return buffer[pos];
    }

  }

  public static class Revert extends RevertLoop {

    Revert(AccessPattern pattern) {
      super(pattern, 1);
    }

  }

  static class MyPattern extends PatternProxy {

    MyPattern() {
      AccessPattern _iseqs =
        new Strip(new ScatterMix(
          80, new Patterns.InterleavedSequence(10, 500, 3, 1,3),
          20, new RandomAccessPattern(1000)
        ), 50000
      );
      AccessPattern _hotterStuff =
        new Hotter(13, 4, new DistAccessPattern(999999));
      pattern = new ScatterMix(
          80, new Loop(_iseqs),
          20, _hotterStuff);
    }

  }

  /**
   * Buffer that writes to an array. When the buffer is full the least element
   * inserted gets overwritten.
   * We can retrieve the latest element with get(0), the second lates with get(1), etc.
   *
   */
  static class CircularIntBuffer {
    int[] buffer;
    int pos = 0;
    boolean full = false;

    CircularIntBuffer(int _size) {
      buffer = new int[_size];
    }

    void add(int v) {
      buffer[pos++] = v;
      if (pos >= buffer.length) {
        pos = 0;
        full = true;
      }
    }
    /** 0 is the last inserted element */
    int get(int idx) {
      idx = pos - 1 - idx;
      if (idx < 0) {
        idx += buffer.length;
      }
      return buffer[idx];
    }
    int size() {
      return full ? buffer.length : pos;
    }
  }

  /**
   * Increase hotness/frequency of an input pattern by the given factor.
   */
  static class Hotter extends AccessPattern {

    AccessPattern pattern;
    Random random = new Random(4711);
    CircularIntBuffer buffer;

    int factor;
    int count = 0;

    /**
     * @param _pattern input pattern
     * @param _size buffer size, which we use to look back
     * @param _factor 2 means per input element two will be selected randomly from the look back buffer
     */
    Hotter(int _size, int _factor, AccessPattern _pattern) {
      pattern = _pattern;
      factor = _factor;
      buffer = new CircularIntBuffer(_size);
    }

    @Override
    public boolean isEternal() {
      return pattern.isEternal();
    }

    public boolean hasNext() {
      if (count > 0) {
        count--;
        return true;
      }
      if (!pattern.hasNext()) {
        return false;
      }
      count = factor;
      buffer.add(pattern.next());
      return true;
    }

    public int next() {
      return buffer.get(random.nextInt(buffer.size()));
    }

  }

  /**
   * Mix sequenced of two patterns together
   */
  static class SeqMix extends AccessPattern {

    Random random = new Random(9876);
    int len1;
    int len2;
    int countDown = -1;
    AccessPattern pattern1;
    AccessPattern pattern2;

    SeqMix(AccessPattern pattern1, int len1, AccessPattern pattern2, int len2) {
      this.len1 = len1;
      this.len2 = len2;
      this.pattern1 = pattern1;
      this.pattern2 = pattern2;
      countDown = random.nextInt(len1);
    }

    public boolean isEternal() {
      return pattern1.isEternal() || pattern2.isEternal();
    }

    public boolean hasNext() {
      if (pattern1 == pattern2) {
        return pattern1.hasNext();
      }
      if (countDown < 0) {
        AccessPattern p = pattern1; pattern1 = pattern2; pattern2 = p;
        int l = len1; len1 = len2; len2 = l;
        countDown = random.nextInt(len1);
      }
      countDown--;
      if (pattern1.hasNext()) {
        return true;
      }
      pattern2 = pattern1;
      return pattern2.hasNext();
    }

    public int next() {
      return pattern1.next();
    }

  }

  static class ScatterMix extends AccessPattern {

    Random random = new Random(5432);
    int probability1;
    int probability2;
    int countDown = -1;
    AccessPattern pattern;
    AccessPattern pattern1;
    AccessPattern pattern2;
    boolean endReached = false;

    ScatterMix(int _probability1, AccessPattern pattern1, int _probability2, AccessPattern _pattern2) {
      this.probability1 = _probability1;
      this.probability2 = _probability2;
      this.pattern1 = pattern1;
      this.pattern2 = _pattern2;
      countDown = random.nextInt(_probability1);
    }

    public boolean isEternal() {
      return pattern1.isEternal() || pattern2.isEternal();
    }

    public boolean hasNext() {
      if (pattern1 == pattern2) {
        return pattern.hasNext();
      }
      int v = random.nextInt(probability1 + probability2);
      if (v < probability1) {
        pattern = pattern1;
      } else {
        pattern = pattern2;
      }
      if (pattern.hasNext()) {
        return true;
      }
      if (pattern1 == pattern) {
        pattern1 = pattern = pattern2;
      } else {
        pattern2 = pattern = pattern1;
      }
      return pattern.hasNext();
    }

    public int next() {
      return pattern.next();
    }

  }

  static class Concat extends AccessPattern {

    int pos = 0;
    AccessPattern pattern[];

    protected Concat(AccessPattern... _pattern) {
      pattern =  _pattern;
    }

    public boolean isEternal() {
      for (AccessPattern p : pattern) {
        if (!p.isEternal()) { return false; }
      }
      return true;
    }

    public boolean hasNext() {
      while (pos < pattern.length) {
        if (pattern[pos].hasNext()) {
          return true;
        }
        pos++;
      }
      return false;
    }

    public int next() {
      return pattern[pos].next();
    }

    public void close() {
      if (pattern != null) {
        for (AccessPattern p : pattern) {
          if (p != null) {
            p.close();
          }
        }
        pattern = null;
      }
    }

  }

  static class Strip extends AccessPattern {

    AccessPattern pattern;
    int count;

    protected Strip(AccessPattern pattern, int count) {
      this.pattern = pattern;
      this.count = count;
    }

    public boolean isEternal() {
      return false;
    }

    public boolean hasNext() {
      if (count > 0) {
        count--;
        return pattern.hasNext();
      }
      if (pattern != null) {
        pattern.close();
        pattern = null;
      }
      return false;
    }

    public int next() {
      return pattern.next();
    }

    public void close() {
      if (pattern != null) {
        pattern.close();
        pattern = null;
      }
    }

  }

  static class PatternProxy extends AccessPattern {

    AccessPattern pattern;

    protected PatternProxy() {
    }

    PatternProxy(AccessPattern pattern) {
      this.pattern = pattern;
    }

    public boolean isEternal() {
      return pattern.isEternal();
    }

    public boolean hasNext() {
      return pattern.hasNext();
    }

    public int next() {
      return pattern.next();
    }
  }

}
