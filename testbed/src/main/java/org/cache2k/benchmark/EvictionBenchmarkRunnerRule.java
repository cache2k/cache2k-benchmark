package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrap the runner into a JUnit rule
 *
 * @author Jens Wilke
 */
public class EvictionBenchmarkRunnerRule implements TestRule {

	private EvictionBenchmarkRunner runner;
	private String suiteName;
	private String candidate = null;
	private List<String> peers;
	private boolean readStoredResults;

	@Override
	public Statement apply(final Statement base, final Description description) {
		if (!description.isSuite()) {
			return base;
		}
		suiteName = description.getDisplayName();
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				runner = new EvictionBenchmarkRunner(suiteName);
				if (readStoredResults) {
					runner.readEvaluationResults();
				}
				base.evaluate();
				runner.printRankingSummary(candidate, peers);
			}
		};
	}

	public EvictionBenchmarkRunnerRule candidate(String s) {
		candidate = s;
		return this;
	}

	public EvictionBenchmarkRunnerRule peers(String... s) {
		peers = Arrays.asList(s);
		return this;
	}

	/**
	 * Sets first cache in the builder as comparison candidate and the others as peer
	 */
	public EvictionBenchmarkRunnerRule candidateAndPeers(EvictionTestVariation.Builder variations) {
		candidateAndPeers(variations.getCaches());
		return this;
	}

	/**
	 * Sets first cache as comparison candidate and the others as peer
	 */
	public EvictionBenchmarkRunnerRule candidateAndPeers(Collection<AnyCacheFactory> caches) {
		List<String> cacheNames =
			caches.stream().map(AnyCacheFactory::getName).collect(Collectors.toList());
		candidate  = cacheNames.remove(0);
		peers = cacheNames;
		return this;
	}

	public EvictionBenchmarkRunnerRule setReadStoredResults(final boolean v) {
		readStoredResults = v;
		return this;
	}

	public void runBenchmark(EvictionTestVariation variation) {
		runner.runBenchmark(variation);
	}

}
