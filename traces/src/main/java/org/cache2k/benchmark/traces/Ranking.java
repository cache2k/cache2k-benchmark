package org.cache2k.benchmark.traces;

/*
 * #%L
 * Benchmarks: Access trace collection
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author Jens Wilke
 */
public class Ranking {

	private Map<String, Map<Long, List<Result>>> trace2size2resultList = new HashMap<>();

	public void readEvaluationResults() {
		File dir = new File("evaluation-results");
		if (dir.exists() || dir.isDirectory()) {
			readFromDir(dir);
		} else {
			dir = new File("../evaluation-results");
			if (dir.exists() || dir.isDirectory()) {
				readFromDir(dir);
			}
		}
	}

	private void readFromDir(final File _dir) {
		for (File f : _dir.listFiles()) {
			readCsvFile(f);
		}
	}

	public void readCsvFile(File file) {
		try {
			LineNumberReader r = new LineNumberReader(new FileReader(file));
			String line;
			while ((line = r.readLine()) != null) {
				add(readCsvLine(line, null));
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static Result readCsvLine(String s, String otherImplementation) {
		try {
			String[] sa = s.split("\\|");
			Result r = new Result();
			r.traceName = sa[0];
			r.implementationName = otherImplementation != null ? otherImplementation : sa[1];
			r.cacheSize = Long.parseLong(sa[2]);
			r.traceLength = Long.parseLong(sa[4]);
			r.missCount = Long.parseLong(sa[5]);
			return r;
		} catch (Exception ex) {
			throw new RuntimeException("Cannot parse line: " + s, ex);
		}
	}

	public void add(Result res) {
		Map<Long, List<Result>> size2res = trace2size2resultList.computeIfAbsent(res.traceName, k -> new HashMap<>());
		List<Result> list = size2res.computeIfAbsent(res.cacheSize, k -> new ArrayList<>());
		list.add(res);
		list.sort(new Comparator<Result>() {
			@Override
			public int compare(final Result o1, final Result o2) {
				return (int) (o1.missCount - o2.missCount);
			}
		});
	}

	public void printSummary(Ranking currentRun, String[] comparisonImplementations) {
		collectSummary(currentRun, (r,l) -> summaryLine(r,l, 3));
		if (comparisonImplementations != null && comparisonImplementations.length > 0) {
			double[] diffSum = new double[comparisonImplementations.length];
			int[] counts = new int[comparisonImplementations.length];
			collectSummary(currentRun, (r, l) -> summaryPick(r, l, comparisonImplementations, diffSum, counts));
			System.out.print("- - -");
			for (int i = 0; i < counts.length; i++) {
				System.out.print(String.format(" - %.3f", diffSum[i] / counts[i]));
			}
		}
	}

	public Collection<Result> getAllResults() {
		return
			trace2size2resultList
				.values().stream().flatMap(m -> m.values().stream().flatMap(Collection::stream)).collect(Collectors.toList());
	}

	public void collectSummary(Ranking currentRun, BiConsumer<Result, List<Result>> collect) {
		for (Map.Entry<String, Map<Long, List<Result>>> entry : currentRun.trace2size2resultList.entrySet()) {
			String traceName = entry.getKey();
			List<Long> sizes = new ArrayList<>();
			sizes.addAll(entry.getValue().keySet());
			sizes.sort(Long::compareTo);
			for (long cacheSize : sizes) {
				Result result = entry.getValue().get(cacheSize).get(0);
				List<Result> ranking = getRanking(traceName, cacheSize);
				collect.accept(result, ranking);
			}
		}
	}

	public void summaryLine(Result result, List<Result> ranking, int count) {
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
		System.out.println(sb);
	}

	public void summaryPick(Result result, List<Result> ranking, String[] implementations, double[] diffSum, int[] count) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s %d %.3f",
			result.traceName,
			result.cacheSize,
			result.getHitPercent()));
		for (int i = 0; i < implementations.length; i++) {
			String implementation = implementations[i];
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
		System.out.println(sb);
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
