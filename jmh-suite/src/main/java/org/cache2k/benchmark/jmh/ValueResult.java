package org.cache2k.benchmark.jmh;

/*
 * #%L
 * Benchmarks: JMH suite.
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

import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.util.ListStatistics;
import org.openjdk.jmh.util.Statistics;

import java.util.Collection;

public class ValueResult extends Result<ValueResult> {

    private final boolean withThroughput;

    public ValueResult(ResultRole role, String label, double n, String unit, AggregationPolicy policy, boolean withThroughput) {
        this(role, label, of(n), unit, policy, withThroughput);
    }

    ValueResult(ResultRole role, String label, Statistics s, String unit, AggregationPolicy policy, boolean withThroughput) {
        super(role, label, s, unit, policy);
        this.withThroughput = withThroughput;
    }

    @Override
    protected Aggregator<ValueResult> getThreadAggregator() {
        return new ValueResultAggregator(AggregationPolicy.SUM);
    }

    @Override
    protected Aggregator<ValueResult> getIterationAggregator() {
        return new ValueResultAggregator(AggregationPolicy.AVG);
    }

    @Override
    protected ValueResult getZeroResult() {
        return new ValueResult(role, label, 0, unit, policy, withThroughput);
    }

    public boolean isWithThroughput() {
        return withThroughput;
    }

    static class ValueResultAggregator implements Aggregator<ValueResult> {

        AggregationPolicy policy;

        public ValueResultAggregator(AggregationPolicy policy) {
            this.policy = policy;
        }

        @Override
        public ValueResult aggregate(Collection<ValueResult> results) {
            ListStatistics stats = new ListStatistics();
            for (ValueResult r : results) {
                stats.addValue(r.getScore());
            }
            return new ValueResult(
                    aggregateRoles(results),
                    aggregateLabels(results),
                    stats,
                    aggregateUnits(results),
                    policy,
                    aggregateWithThroughput(results)

            );
        }
    }

    static ResultRole aggregateRoles(Collection<? extends Result> results) {
        ResultRole result = null;
        for (Result r : results) {
            if (result == null) {
                result = r.getRole();
            } else if (result != r.getRole()) {
                throw new IllegalStateException("Combining the results with different roles");
            }
        }
        return result;
    }

    static String aggregateUnits(Collection<? extends Result> results) {
        String result = null;
        for (Result r : results) {
            if (result == null) {
                result = r.getScoreUnit();
            } else if (!result.equals(r.getScoreUnit())) {
                throw new IllegalStateException("Combining the results with different units");
            }
        }
        return result;
    }

    static String aggregateLabels(Collection<? extends Result> results) {
        String result = null;
        for (Result r : results) {
            if (result == null) {
                result = r.getLabel();
            } else if (!result.equals(r.getLabel())) {
                throw new IllegalStateException("Combining the results with different labels");
            }
        }
        return result;
    }

    static boolean aggregateWithThroughput(Collection<? extends ValueResult> results) {
        boolean result = false;
        for (ValueResult r : results) {
            result = r.isWithThroughput();
            break;
        }
        for (ValueResult r : results) {
            if (result != r.isWithThroughput()) {
                throw new IllegalStateException("Combining the results with different withThroughput settings");
            }
        }
        return result;
    }

}
