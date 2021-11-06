package org.cache2k.benchmark.traces;

/*
 * #%L
 * Benchmarks: Access trace collection
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
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

import com.github.benmanes.caffeine.cache.simulator.parser.lirs.LirsTraceReader;
import com.github.benmanes.caffeine.cache.simulator.parser.wikipedia.WikipediaTraceReader;
import org.cache2k.benchmark.util.AccessPattern;
import org.cache2k.benchmark.util.LongToIntMapper;
import org.cache2k.benchmark.util.Patterns;
import org.cache2k.benchmark.util.RandomAccessPattern;
import org.cache2k.benchmark.util.TraceDirectory;
import org.cache2k.benchmark.util.TraceSupplier;
import org.cache2k.benchmark.util.UmassTraceReader;
import org.cache2k.benchmark.util.ZipfianPattern;

/**
 * @author Jens Wilke
 */
public interface Traces {

	TraceSupplier WIKIPEDIA1 =
		fromLongStream(() -> new WikipediaTraceReader(
			TraceDirectory.resolveFile("wikipedia/wiki.1190153705.gz")).keys())
			.name("wikipedia1")
			.sizes(512, 1024, 2048, 4096);

	/**
	 * Corda trace from the Caffeine simulator.
	 *
	 * @see CordaTraceReader
	 */
	TraceSupplier CORDA_SMALL =
		fromLongStream(() -> new CordaTraceReader("trace_vaultservice.gz")
			.events().mapToLong(x -> x.key()))
		.name("corda-small")
		.sizes(512, 1024);

	/**
	 * Corda trace from the Caffeine simulator.
	 *
	 * @see CordaTraceReader
	 */
	TraceSupplier CORDA_LARGE =
		fromLongStream(() -> new CordaTraceReader("trace_vaultservice_large.gz")
			.events().mapToLong(x -> x.key()))
			.name("corda-large")
			.sizes(512, 1024);

	/**
	 * Corda trace from the Caffeine simulator. Every request is repeated ten times and
	 * transposed into a separate number space.
	 *
	 * @see CordaTraceReader
	 */
	TraceSupplier CORDA_SMALL_10X =
		of(() ->
			Patterns.explode(AccessPattern.of(new CordaTraceReader("trace_vaultservice.gz")
				.events().mapToLong(x -> x.key()).mapToInt(new LongToIntMapper())), 10))
			.name("corda-small-10x")
			.sizes(512 * 10, 1024 * 10);

	/**
	 * Reference trace used in the LIRS and CLOCK-Pro paper. This trace
	 * is used to compare the hit percentages in the papers to the
	 * implementation to ensure correctness.
	 */
	TraceSupplier LOOP =
		fromLongStream(() -> new LirsTraceReader("loop.trace.gz").keys())
			.name("loop")
			.sizes(256, 512);

	/**
	 * Trace combining {@link #CORDA_SMALL} {@link #LOOP} and {@link #CORDA_SMALL}.
	 * used for adaptive cache evaluations.
	 */
	TraceSupplier CORDA_LOOP_CORDA =
		of(() -> Patterns.concat(CORDA_SMALL.get().newPattern(), LOOP.get().newPattern(), CORDA_SMALL.get().newPattern()))
			.name("corda-loop-corda")
			.sizes(512, 1024);

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
		of("trace-cpp.trc.bin.gz")
		.name("cpp")
		.sizes(20, 35, 50, 80, 100, 300, 500);

	/**
	 * Reference trace used in the LIRS and CLOCK-Pro paper. This trace
	 * is used to compare the hit percentages in the papers to the
	 * implementation to ensure correctness.
	 *
	 * <p/>Short description from the CLOCK-Pro paper: sprite is from the
	 * Sprite network file system, which contains requests to a file
	 * server from client workstations for a two-day period. The trace
	 * is a member of the temporally-clustered pattern group.
	 */
	TraceSupplier SPRITE =
		of("trace-sprite.trc.bin.gz").name("sprite")
		.sizes(100, 200, 400, 600, 800, 1000);

	/**
	 * Reference trace used in the LIRS and CLOCK-Pro paper. This trace
	 * is used to compare the hit percentages in the papers to the
	 * implementation to ensure correctness.
	 *
	 * <p>Short description from CLOCK-Pro paper: multi2 is obtained by executing
	 * three workloads, cs, cpp, and postgres, together. The trace is a member
	 * of the mixed pattern group.
	 */
	TraceSupplier MULTI2 =
		of("trace-multi2.trc.bin.gz").name("multi2")
		.sizes(600, 1800, 3000);

	/**
	 * Reference trace used in the LIRS and CLOCK-Pro paper. This trace
	 * is used to compare the hit percentages in the papers to the
	 * implementation to ensure correctness.
	 *
	 * <p>Short description from CLOCK-Pro paper: multi2 is obtained by executing
	 * three workloads, cs, cpp, and postgres, together. The trace is a member
	 * of the mixed pattern group.
	 */
	TraceSupplier GLIMPSE =
		of("trace-glimpse.trc.bin.gz").name("glimpse")
		.sizes(500, 1000, 2000);

	/**
	 * Trace "recs" from scarab research.
	 *
	 * @see <a href="https://github.com/ben-manes/caffeine/issues/106"/>
	 */
	TraceSupplier SCARAB_RECS =
		of("trace-scarab-recs-20160808T073231Z.trc.xz").name("scarab-recs")
		.sizes(25_000, 50_000, 75_000, 100_000);

	/**
	 * Trace "prods" from scarab research.
	 *
	 * @see <a href="https://github.com/ben-manes/caffeine/issues/106"/>
	 */
	TraceSupplier SCARAB_PRODS =
		of("trace-scarab-prods-20160808T073231Z.trc.xz").name("scarab-prods")
		.sizes(25_000, 50_000, 75_000, 100_000);

	/**
	 * Access trace (HTTP requests) on a product detail page in december.
	 *
	 * <p>This trace is provides by Jens Wilke / headissue GmbH.
	 */
	TraceSupplier WEB12 =
		of("trace-mt-20121220.trc.bin.gz").name("web12")
		.sizes(300, 1200, 3000);

	/**
	 * Access trace (HTTP requests) on a product detail page in july.
	 *
	 * <p>This trace is provides by Jens Wilke / headissue GmbH.
	 */
	TraceSupplier WEB07 =
		of("trace-mt-20130703.trc.bin.gz").name("web07")
		.sizes(300, 1200, 3000);

	/**
	 * UMass Financial1 trace. Truncated to one million requests.
	 *
	 * <p>fileName=Financial1.spc.bz2, sha1sum=5f705113ef5ab0b7cf033894e03ff0b050927ffd
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	 TraceSupplier FINANCIAL1_1M =
		TraceSupplier.of(() -> UmassTraceReader.of("Financial1.spc.bz2").strip(1_000_000))
		.name("financial1-1M")
		.sizes(12500, 25000, 50000, 100000, 200000);

	/**
	 * UMass Financial1 trace.
	 *
	 * <p>fileName=Financial1.spc.bz2, sha1sum=5f705113ef5ab0b7cf033894e03ff0b050927ffd
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier FINANCIAL1 =
		TraceSupplier.of(() -> UmassTraceReader.of("Financial1.spc.bz2"))
			.name("financial1")
			.sizes(12500, 25000, 50000, 100000, 200000);

	/**
	 * UMass Financial2 trace. Truncated to one million requests.
	 *
	 * <p>fileName=Financial2.spc.bz2, sha1sum=ca788d7ec7b3700c989c252752af54c68a637a4a
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier FINANCIAL2_1M =
		TraceSupplier.of(() -> UmassTraceReader.of("Financial2.spc.bz2").strip(1_000_000))
			.name("financial2-1M")
		  .sizes(5000, 10000, 20000, 40000, 80000);

	/**
	 * UMass Financial2 trace.
	 *
	 * <p>fileName=Financial2.spc.bz2, sha1sum=ca788d7ec7b3700c989c252752af54c68a637a4a
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier FINANCIAL2 =
		TraceSupplier.of(() -> UmassTraceReader.of("Financial2.spc.bz2"))
			.name("financial2")
			.sizes(5000, 10000, 20000, 40000, 80000);

	/**
	 * UMass WebSearch1 trace. Truncated to one million requests.
	 *
	 * <p>fileName=WebSearch1.spc.bz2, sha1sum=4952e8eee9ea0117d6fc010779b32c3260ce6ead
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier WEBSEARCH1_1M =
		TraceSupplier.of(() -> UmassTraceReader.of("WebSearch1.spc.bz2").strip(1_000_000))
			.name("umass-websearch1-1M")
		  .sizes(100000, 200000, 300000);

	/**
	 * UMass WebSearch1 trace. accessCount=31970644, uniqueValues=12299573, hitrate around 17% at 2M cache size.
	 *
	 * <p>fileName=WebSearch1.spc.bz2, sha1sum=4952e8eee9ea0117d6fc010779b32c3260ce6ead
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier WEBSEARCH1 =
		TraceSupplier.of(() -> UmassTraceReader.of("WebSearch1.spc.bz2"))
			.name("websearch1")
			.sizes(1_000_000, 2_000_000);

	/**
	 * UMass WebSearch2 trace. Truncated to one million requests.
	 *
	 * <p>fileName=WebSearch2.spc.bz2, sha1sum=6d44ce16f4233a74be4a42c54bce7cca1197098a
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier WEBSEARCH2_1M =
		TraceSupplier.of(() -> UmassTraceReader.of("WebSearch2.spc.bz2").strip(1_000_000))
			.name("websearch2-1M")
			.sizes(10000, 20000, 30000);

	/**
	 * UMass WebSearch2 trace.
	 *
	 * <p>fileName=WebSearch2.spc.bz2, sha1sum=6d44ce16f4233a74be4a42c54bce7cca1197098a
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier WEBSEARCH2 =
		TraceSupplier.of(() -> UmassTraceReader.of("WebSearch2.spc.bz2"))
			.name("websearch2")
			.sizes(1_000_000, 2_000_000);

	/**
	 * UMass WebSearch3 trace. Truncated to one million requests.
	 *
	 * <p>fileName=WebSearch3.spc.bz2, sha1sum=b01df4d3a1d3379d8a33dfb35958e8749e6c4e02
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier WEBSEARCH3_1M =
		TraceSupplier.of(() -> UmassTraceReader.of("WebSearch3.spc.bz2").strip(1_000_000))
			.name("websearch3-1M")
			.sizes(10000, 20000, 30000);

	/**
	 * UMass WebSearch3 trace.
	 *
	 * <p>fileName=WebSearch3.spc.bz2, sha1sum=b01df4d3a1d3379d8a33dfb35958e8749e6c4e02
	 * @see <a href="http://traces.cs.umass.edu/index.php/Storage/Storage">UMassTraceRepository</a>
	 */
	TraceSupplier WEBSEARCH3 =
		TraceSupplier.of(() -> UmassTraceReader.of("WebSearch3.spc.bz2"))
			.name("websearch3")
			.sizes(1_000_000, 2_000_000);

	/**
	 * Trace of database object access of a eCommerce web application during the night time.
	 *
	 * <p>Trace description: The accessed objects comprise of a mixture of
	 * product inventory, availability per price and also customer data.
	 * Objects are keyed by type, id and a index (e.g. the 3rd price of a product).
	 * All data is normalized into numbers starting at 0 (or 1 for sub-ids) and
	 * then collapsed into a single integer consisting of: Bits 27-31: type,
	 * Bits 9-26: id, Bits 0-9: index.
	 *
	 * <p>This trace is provides by Jens Wilke / headissue GmbH.
	 */
	TraceSupplier ORM_NIGHT =
		TraceSupplier.of("trace-mt-db-20160419-night.trc.bin.bz2").name("orm-night")
		.sizes(625, 1250, 2500, 5000, 10000);

	/**
	 * Trace of database object access of a eCommerce web application during daytime.
	 *
	 * <p>This trace is provides by Jens Wilke / headissue GmbH.
	 *
	 * @see #ORM_NIGHT
	 */
	TraceSupplier ORM_BUSY =
		TraceSupplier.of("trace-mt-db-20160419-busy.trc.bin.bz2").name("orm-busy")
		.sizes(625, 1250, 2500, 5000, 10000);

	/**
	 * The OLTP trace is used in the ARC paper.
	 */
	TraceSupplier OLTP =
		TraceSupplier.of("trace-oltp.trc.bin.gz").name("oltp")
		.sizes(1000, 2000, 5000, 10000, 15000);

	TraceSupplier RANDOM_1000_100K =
		of(() -> new RandomAccessPattern(1000).strip(100_000))
		.name("random1000-100k")
		.sizes(100, 200, 350, 500, 800);

	TraceSupplier RANDOM_1000_1M =
		of(() -> new RandomAccessPattern(1000).strip(1000_000))
			.name("random1000-1M")
			.sizes(100, 200, 350, 500, 800);

	TraceSupplier RANDOM_1000_10K =
		of(() -> new RandomAccessPattern(1000).strip(10_000))
			.name("random1000-10k")
			.sizes(100, 200, 350, 500, 800);

	TraceSupplier ZIPFIAN_10K_10M =
		of(() -> new ZipfianPattern(1802,10000).strip(10_000_000))
			.name("zipf10K-3M")
			.sizes(500, 2000, 8000);

	TraceSupplier ZIPFIAN_10K_3M =
		of(() -> new ZipfianPattern(1802,10000).strip(3_000_000))
		.name("zipf10K-3M")
		.sizes(500, 1000, 2000);

	TraceSupplier ZIPFIAN_10K_1M =
		of(() -> new ZipfianPattern(1802,10000).strip(1_000_000))
			.name("zipf10K-1M")
			.sizes(500, 1000, 2000);

	TraceSupplier ZIPFIAN_900_3M =
		of(() -> new ZipfianPattern(1802,900).strip(3_000_000))
		.name("zipf900-3M")
		.sizes(50, 100, 300, 500, 700);

	TraceSupplier ZIPFIAN_900_1M =
		of(() -> new ZipfianPattern(1802,900).strip(1_000_000))
			.name("zipf900-1M")
			.sizes(50, 100, 300, 500, 700);

}
