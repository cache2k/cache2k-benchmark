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

import com.github.benmanes.caffeine.cache.simulator.parser.corda.CordaTraceReader;

import static org.cache2k.benchmark.util.TraceSupplier.*;

import org.cache2k.benchmark.util.RandomAccessPattern;
import org.cache2k.benchmark.util.TraceSupplier;
import org.cache2k.benchmark.util.ZipfianPattern;

/**
 * @author Jens Wilke
 */
public interface Traces {

	/**
	 * Corda trace from the Caffeine simulator.
	 *
	 * @see CordaTraceReader
	 */
	TraceSupplier CORDA_SMALL =
		fromLongStream(() -> new CordaTraceReader("trace_vaultservice.gz").events())
		.name("corda-small");

	/**
	 * Reference trace used in the LIRS and CLOCK-Pro paper. This trace
	 * is used to compare the hit percentages in the papers to the
	 * implementation to ensure correctness.
	 *
	 * <p>Short description from CLOCK-Pro paper: cpp is a GNU C
	 * compiler pre-processor trace. The total size of C source
	 * programs used as input is roughly 11 MB. The trace is a
	 * member of the probabilistic pattern group.
	 */
	TraceSupplier CPP =
		of("trace-cpp.trc.bin.gz").name("cpp");

	int TRACE_LENGTH_3M = 3 * 1000 * 1000;

	TraceSupplier RANDOM_1000_10K =
		of(() -> new RandomAccessPattern(1000).strip(10_000))
			.name("random1000-10k");

	int ZIPF10K_TRACE_LENGTH_10M = 10 * 1000 * 1000;

	TraceSupplier ZIPFIAN_10K_10M =
		of(() -> new ZipfianPattern(1802,10000).strip(ZIPF10K_TRACE_LENGTH_10M))
			.name("zipf10K-10M");

	TraceSupplier ZIPFIAN_900_3M =
		of(() -> new ZipfianPattern(1802,900).strip(TRACE_LENGTH_3M))
			.name("zipf900-3M");

}
