package org.cache2k.benchmark.traces;

/*
 * #%L
 * traces
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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
 * Reference trace used in the LIRS and CLOCK-Pro paper. This trace
 * is used to compare the hit percentages in the papers to the
 * implementation to ensure correctness.
 *
 * <p/>Short description from CLOCK-Pro paper: cpp is a GNU C
 * compiler pre-processor trace. The total size of C source
 * programs used as input is roughly 11 MB. The trace is a
 * member of the probabilistic pattern group.
 *
 * @author Jens Wilke; created: 2013-11-20
 */
public class CacheAccessTraceCpp {

  public static AccessTrace getInstance() {
    return TraceCache.getTraceLazy("trace-cpp.trc.bin.gz");
  }

}
