package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects results of eviction benchmarks and is able to print a comparison summary.
 * Can also read results from previous test runs.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("WeakerAccess")
public class EvictionRanking {

  public static final String EVALUATION_RESULTS_DIR = "evaluation-results";
  private final Map<String, Map<Long, List<Result>>> trace2size2resultList = new HashMap<>();

  /**
   * Read results for comparison from directory {@value EVALUATION_RESULTS_DIR}. Either
   * in current directory or in parent directory.
   */
  public synchronized void readEvaluationResults() {
    readEvaluationResults(EVALUATION_RESULTS_DIR);
  }

  /**
   * Read evaluation results
   *
   * @param directory directory of CSV files to read.
   */
  public synchronized void readEvaluationResults(String directory) {
    File dir = new File(directory);
    if (dir.exists() || dir.isDirectory()) {
      readFromDir(dir);
    } else {
      dir = new File("../" + directory);
      if (dir.exists() || dir.isDirectory()) {
        readFromDir(dir);
      }
    }
  }

  private void readFromDir(final File directory) {
    for (File f : Objects.requireNonNull(directory.listFiles())) {
      readCsvFile(f);
    }
  }

  /**
   * Read this CSV file.
   *
   * @param file
   */
  public void readCsvFile(File file) {
    try {
      LineNumberReader r = new LineNumberReader(new FileReader(file));
      String line;
      while ((line = r.readLine()) != null) {
        add(readCsvLine(line));
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static Result readCsvLine(String s) {
    try {
      String[] sa = s.split("\\|");
      Result r = new Result();
      r.traceName = sa[0];
      r.implementationName = sa[1];
      r.cacheSize = Long.parseLong(sa[2]);
      r.traceLength = Long.parseLong(sa[4]);
      r.missCount = Long.parseLong(sa[5]);
      return r;
    } catch (Exception ex) {
      throw new RuntimeException("Cannot parse line: " + s, ex);
    }
  }

  public synchronized void add(Result res) {
    Map<Long, List<Result>> size2res =
      trace2size2resultList.computeIfAbsent(res.traceName, k -> new HashMap<>());
    List<Result> list = size2res.computeIfAbsent(res.cacheSize, k -> new ArrayList<>());
    list.add(res);
    list.sort((o1, o2) -> (int) (o1.missCount - o2.missCount));
  }

  public void addAll(Iterable<Result> list) {
    list.forEach(this::add);
  }

  public void addAll(Stream<Result> stream) {
    stream.forEach(this::add);
  }

  public synchronized void writeTopSummary(PrintWriter writer, EvictionRanking currentRun, int count) {
    collectSummary(writer, currentRun, (r, l) -> summaryLine(r, l, count));
  }

  public synchronized void printSummary(PrintWriter writer, EvictionRanking currentRun,
                                        List<String> peers) {
    if (peers != null && peers.size() > 0) {
      double[] diffSum = new double[peers.size()];
      int[] counts = new int[peers.size()];
      collectSummary(writer, currentRun, (r, l) -> summaryPick(r, l, peers, diffSum, counts));
      writer.print("- -");
      for (int i = 0; i < counts.length; i++) {
        writer.print(String.format(" - %.3f", diffSum[i] / counts[i]));
      }
      writer.println();
    }
  }

  public synchronized Collection<Result> getAllResults() {
    return
      trace2size2resultList
        .values().stream().flatMap(m -> m.values()
        .stream().flatMap(Collection::stream)).collect(Collectors.toList());
  }

  public List<Result> getRanking(String traceName, long cacheSize) {
    Map<Long, List<Result>> size2res = trace2size2resultList.get(traceName);
    if (size2res != null) {
      List<Result> list = size2res.get(cacheSize);
      if (list != null) {
        return list;
      }
    }
    return Collections.emptyList();
  }

  public String getTop3(String traceName, long cacheSize) {
    List<Result> res = getRanking(traceName, cacheSize);
    res = res.subList(0, Math.min(res.size(), 3));
    StringBuilder sb = new StringBuilder();
    sb.append("top3:");
    res.forEach(r ->
      sb.append(" ")
        .append(r.implementationName)
        .append('=')
        .append(String.format("%.3f",r.getHitPercent()))
    );
    return sb.toString();
  }

  private void collectSummary(PrintWriter writer, EvictionRanking currentRun,
                              BiFunction<Result, List<Result>, String> collect) {
    for (Map.Entry<String, Map<Long, List<Result>>> entry :
      currentRun.trace2size2resultList.entrySet()) {
      String traceName = entry.getKey();
      List<Long> sizes = new ArrayList<>();
      sizes.addAll(entry.getValue().keySet());
      sizes.sort(Long::compareTo);
      for (long cacheSize : sizes) {
        Result result = entry.getValue().get(cacheSize).get(0);
        List<Result> ranking = getRanking(traceName, cacheSize);
        writer.println(collect.apply(result, ranking));
      }
    }
  }

  private String summaryLine(Result result, List<Result> ranking, int count) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s %d %.3f",
        result.traceName,
        result.cacheSize,
        result.getHitPercent()));
    count = Math.min(count, ranking.size());
    for (int i = 0; i < count; i++) {
      Result best = ranking.get(i);
      sb.append(String.format(" %s %.3f %.3f",
        best.implementationName,
        best.getHitPercent(),
        result.getHitPercent() - best.getHitPercent()));
    }
    return sb.toString();
  }

  private String summaryPick(Result result, List<Result> ranking,
                             List<String> implementations, double[] diffSum, int[] count) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s %d %.3f",
      result.traceName,
      result.cacheSize,
      result.getHitPercent()));
    for (int i = 0; i < implementations.size(); i++) {
      String implementation = implementations.get(i);
      final int index = i;
      ranking.stream()
        .filter(r -> r.getImplementationName().equals(implementation)).findAny()
        .ifPresent(r -> {
          double diff = result.getHitPercent() - r.getHitPercent();
          sb.append(String.format(" %.3f %.3f",
          r.getHitPercent(),
          diff));
          count[index]++;
          diffSum[index] += diff;
        });
    }
    return sb.toString();
  }

  public static class Result {

    private String implementationName;
    private String traceName;
    private long cacheSize;
    private long traceLength;
    private long missCount;

    public double getHitPercent() {
      return (traceLength - missCount) * 100D / traceLength;
    }

    public String getImplementationName() {
      return implementationName;
    }

    public void setImplementationName(final String v) {
      implementationName = v;
    }

    public String getTraceName() {
      return traceName;
    }

    public void setTraceName(final String v) {
      traceName = v;
    }

    public long getCacheSize() {
      return cacheSize;
    }

    public void setCacheSize(final long v) {
      cacheSize = v;
    }

    public long getTraceLength() {
      return traceLength;
    }

    public void setTraceLength(final long v) {
      traceLength = v;
    }

    public long getMissCount() {
      return missCount;
    }

    public void setMissCount(final long v) {
      missCount = v;
    }

  }

}
