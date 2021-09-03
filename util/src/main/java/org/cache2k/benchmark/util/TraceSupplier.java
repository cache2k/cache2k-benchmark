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

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * @author Jens Wilke
 */
public class TraceSupplier implements Supplier<AccessTrace> {

	private volatile WeakReference<AccessTrace> traceCache = new WeakReference<>(null);
	private Callable<AccessTrace> supplier;
	private String name;
	private int[] sizes = null;

	public static TraceSupplier fromLongStream(Callable<LongStream> supplier) {
		return new TraceSupplier(() ->
			new AccessTrace(
				supplier.call().mapToInt(new LongToIntMapper()).toArray()));
	}

	public static TraceSupplier fromIntStream(Callable<IntStream> supplier) {
		return new TraceSupplier(() ->
			new AccessTrace(
				supplier.call().toArray()));
	}

	public static TraceSupplier of(Callable<AccessPattern> supplier) {
		return new TraceSupplier(() -> new AccessTrace(supplier.call()));
	}

	public static TraceSupplier of(String fileName) {
		return new TraceSupplier(() -> new AccessTrace(fileName));
	}

	public TraceSupplier(Callable<AccessTrace> supplier) {
		this.supplier = supplier;
	}

	public TraceSupplier name(String name) {
		this.name = name;
		return this;
	}

	public TraceSupplier sizes(int... sizes) {
		this.sizes = sizes;
		return this;
	}

	public int[] getSizes() {
		return sizes;
	}

	@Override
	public AccessTrace get() {
		AccessTrace trace = traceCache.get();
		if (trace != null) {
			return trace;
		}
		try {
			trace = supplier.call();
		} catch (Exception ex) {
			sneakyThrow(ex);
		}
		trace.name(name);
		traceCache = new WeakReference<>(trace);
		return trace;
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return TraceSupplier.class.getSimpleName() + "(" + name + ")";
	}

}
