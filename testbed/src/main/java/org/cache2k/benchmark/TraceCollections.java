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

import org.cache2k.benchmark.traces.Traces;

/**
 * @author Jens Wilke
 */
public class TraceCollections {

	public static final EvictionTestVariation.Builder ALL_TRACES =
		new EvictionTestVariation.Builder()
			.add(Traces.SPRITE)
			.add(Traces.CPP)
			.add(Traces.MULTI2)
			.add(Traces.GLIMPSE)
			.add(Traces.OLTP)
			.add(Traces.ORM_BUSY)
			.add(Traces.ORM_NIGHT)
			.add(Traces.WEB07)
			.add(Traces.WEB12)
			.add(Traces.FINANCIAL1)
			.add(Traces.FINANCIAL2)
			.add(Traces.WEBSEARCH1)
			/* we don't use these traces, too big
			.add(Traces.WEBSEARCH2)
			.add(Traces.WEBSEARCH3)
			 */
			.add(Traces.SCARAB_PRODS)
			.add(Traces.SCARAB_RECS)
			.add(Traces.CORDA_SMALL)
			.add(Traces.CORDA_SMALL_10X)
			.add(Traces.CORDA_LOOP_CORDA)
			.add(Traces.LOOP)
			.add(Traces.WIKIPEDIA1)
			.add(Traces.ZIPFIAN_900_3M)
			// .add(Traces.ZIPFIAN_900_1M)
			.add(Traces.ZIPFIAN_10K_10M)
			// .add(Traces.ZIPFIAN_10K_3M)
			.add(Traces.RANDOM_1000_1M)
			// .add(Traces.RANDOM_1000_100K)
			// .add(Traces.RANDOM_1000_10K)
			;

	public static final EvictionTestVariation.Builder REGRESSION_TRACES =
		new EvictionTestVariation.Builder()
			.add(Traces.SPRITE)
			.add(Traces.CPP)
			.add(Traces.MULTI2)
			.add(Traces.GLIMPSE)
			.add(Traces.OLTP)
			.add(Traces.ORM_BUSY)
			.add(Traces.ORM_NIGHT)
			.add(Traces.WEB07)
			.add(Traces.WEB12)
			.add(Traces.FINANCIAL1_1M)
			.add(Traces.FINANCIAL2_1M)
			.add(Traces.WEBSEARCH1_1M)
			/* we don't use these traces, too big
			.add(Traces.WEBSEARCH2)
			.add(Traces.WEBSEARCH3)
			 */
			.add(Traces.SCARAB_PRODS)
			.add(Traces.SCARAB_RECS)
			.add(Traces.CORDA_SMALL)
			.add(Traces.CORDA_SMALL_10X)
			.add(Traces.CORDA_LOOP_CORDA)
			.add(Traces.LOOP)
			.add(Traces.WIKIPEDIA1)
			.add(Traces.ZIPFIAN_900_3M)
			// .add(Traces.ZIPFIAN_900_1M)
			.add(Traces.ZIPFIAN_10K_10M)
			// .add(Traces.ZIPFIAN_10K_3M)
			.add(Traces.RANDOM_1000_1M)
		// .add(Traces.RANDOM_1000_100K)
		// .add(Traces.RANDOM_1000_10K)
		;

}
