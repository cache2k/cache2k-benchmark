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
 * Trace of database object access of a eCommerce web application during the night time.
 *
 * <p>Trace description: The accessed objects comprise of a mixture of
 * product inventory, availability per price and also customer data.
 * Objects are keyed by type, id and a index (e.g. the 3rd price of a product).
 * All data is normalized into numbers starting at 0 (or 1 for sub-ids) and
 * then collapsed into a single integer consisting of: Bits 27-31: type,
 * Bits 9-26: id, Bits 0-9: index.
 *
 * @author Jens Wilke
 */
public class CacheAccessTraceOrmAccessNight {

  public static AccessTrace getInstance() {
    return TraceCache.getTraceLazy("trace-mt-db-20160419-night.trc.bin.bz2");
  }

}
