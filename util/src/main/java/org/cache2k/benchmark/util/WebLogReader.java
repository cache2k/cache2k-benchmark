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

import it.unimi.dsi.util.XoShiRo256StarStarRandom;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Extract time and resource URL from a web log and produce a stream of integers with
 * the first integer representing a time in seconds and the second the unique resource
 * number. Both numbers start with 0.
 *
 * @author Jens Wilke
 */
public class WebLogReader {

  private static final DateTimeFormatter dateFormat =
    DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");

  /**
   * Parse access log from standard in returning binary output to standard out
   * On integer parameter may be specified to randomly skip every n-th key from the
   * output, which should create a smaller trace with similar properties.
   */
  public static void main(String[] args) throws IOException {
    LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(System.out));
    WebLogReader logReader;
    SortedMap<Long, Tuple> map = new TreeMap<>();
    int sampleKeys = 0; Integer.parseInt(args[0]);
    if (args.length > 0) {
      sampleKeys = Integer.parseInt(args[0]);
    }
    if (sampleKeys > 1) {
      int randomCeiling = sampleKeys;
      Random random = new XoShiRo256StarStarRandom(1802);
      logReader = new WebLogReader(() -> random.nextInt(randomCeiling) != 0);
    } else {
      logReader = new WebLogReader(Thinner.NO_SKIP);
    }
    String line;
    int sequenceCount = 0;
    while ((line = in.readLine()) != null) {
      Tuple t = logReader.parseLine(line);
      if (t == null) { continue; }
      map.put(toUpper(t.time) + sequenceCount++ , t);
    }
    if (args.length > 1) {
      long offset = map.firstKey();
      int startTime = Integer.parseInt(args[1]);
      int endTime = Integer.parseInt(args[2]);
      Map orig = map;
      map = map.tailMap(toUpper(startTime) + offset).headMap(toUpper(endTime) + offset);
      System.err.println("Cut away: " + (orig.size() - map.size()));
    }
    int timeOffset = map.values().stream().findFirst().get().time;
    System.err.println("Time range: " + ((map.lastKey() >> 32) - timeOffset));
    for (Tuple t : map.values()) {
      out.writeInt(t.time - timeOffset);
      out.writeInt(t.key);
    }
    System.err.println("Key count=" + logReader.count + ", request count=" + map.size());
    in.close();
    out.close();
  }

  /** Store the time in the upper part and a sequence number in the lower part */
  private static long toUpper(int time) {
    return time * 1L << 32;
  }

  public static long extractTime(String line) {
    int idx1 = line.indexOf('[');
    int idx2 = line.indexOf(']', idx1);
    final String timeString = line.substring(idx1 + 1, idx2);
    return
      ZonedDateTime.from(dateFormat.parse(timeString)).getLong(ChronoField.INSTANT_SECONDS);
  }

  public static String extractResourceLocation(String line) {
    int idx1 = line.indexOf('"');
    int idx2 = line.indexOf('"', idx1 + 1);
    String txt = line.substring(idx1 + 1, idx2);
    return txt.split(" ")[1];
  }

  /** Move time start from 1970 to 2000 to avoid integer overflow */
  private final long OFFSET_2000 = (2000 - 1970) * 365 * 24 * 60 * 60;
  private final Map<String, Integer> loc2int = new HashMap<String, Integer>();
  private final Thinner thinner;
  private int count = 0;

  public WebLogReader(Thinner thinner) {
    this.thinner = thinner;
  }

  public Tuple parseLine(String line) {
    int time = (int) (extractTime(line) - OFFSET_2000);
    int key = loc2int.computeIfAbsent(extractResourceLocation(line),
      x -> thinner.skip() ? -1 : count++);
    if (key == -1) { return null; }
    return new Tuple(key, time);
  }

  public static class Tuple {
    public final int key;
    public final int time;

    public Tuple(int key, int time) {
      this.key = key;
      this.time = time;
    }
  }

  interface Thinner {
    Thinner NO_SKIP = () -> false;
    boolean skip();
  }

}
