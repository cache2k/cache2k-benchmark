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

/**
 * @author Jens Wilke
 */
public class TraceResourceDirectory {

  public final static String PROPERTY_NAME = "trace.directory";
  public final static String DEFAULT_DIRECTORY = "/opt/headissue/cache2k-benchmark-trace";
  public final static String TRACE_DIRECTORY;

  static {
    TRACE_DIRECTORY = System.getProperty(PROPERTY_NAME,DEFAULT_DIRECTORY);
  }

}
