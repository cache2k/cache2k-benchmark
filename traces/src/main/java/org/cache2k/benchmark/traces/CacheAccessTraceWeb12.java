package org.cache2k.benchmark.traces;

/*
 * #%L
 * cache2k-benchmark-traces
 * %%
 * Copyright (C) 2013 headissue GmbH, Munich
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

import org.cache2k.benchmark.util.AccessTrace;

/**
 * Normalized access trace (HTTP requests) on a product detail page in december.
 *
 * @author Jens Wilke; created: 2013-11-20
 */
public class CacheAccessTraceWeb12 {

  public static AccessTrace getInstance() {
    return TraceCache.getTraceLazy("trace-mt-20121220.trc.bin.gz");
  }

}
