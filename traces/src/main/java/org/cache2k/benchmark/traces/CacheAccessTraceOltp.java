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
 *
 *
 * @author Jens Wilke; created: 2015-01-22
 */
public class CacheAccessTraceOltp {

  public static AccessTrace getInstance() {
    return TraceCache.getTraceLazy("trace-oltp.trc.bin.gz")
      .setOptHitCount(1000, 490093)
      .setOptHitCount(2000, 552149)
      .setOptHitCount(5000, 624076)
      .setOptHitCount(10000, 667490)
      .setOptHitCount(15000, 686870);
  }

}
