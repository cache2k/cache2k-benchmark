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
import org.cache2k.benchmark.util.AccessPattern;
import org.cache2k.benchmark.util.AccessTrace;
import org.cache2k.benchmark.util.ObjectToIntegerMapper;

import java.io.IOException;

/**
 * @author Jens Wilke
 */
public final class CacheAccessTraceCordaSmall {

	private static final String FILE = "/com/github/benmanes/caffeine/cache/simulator/parser/corda/trace_vaultservice.gz";
	private static AccessTrace TRACE;

	public static AccessTrace getInstance() {
		if (TRACE != null) {
			return TRACE;
		}
		try {
			AccessPattern _pattern =
				new ObjectToIntegerMapper(
					new CordaTraceReader(FILE).events().iterator());
			return TRACE = new AccessTrace(_pattern);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
