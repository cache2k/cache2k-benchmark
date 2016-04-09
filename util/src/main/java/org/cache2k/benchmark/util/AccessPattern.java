package org.cache2k.benchmark.util;

/*
 * #%L
 * util
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
 * Requests to a cache as a stream of integer numbers, which represent
 * the cache key.
 *
 * @author Jens Wilke; created: 2013-08-25
 */
public abstract class AccessPattern {

  /**
   * The pattern does never end, {@link #hasNext()} always returns true.
   */
  public abstract boolean isEternal();

  public abstract boolean hasNext() throws Exception;

  public abstract int next() throws Exception;

  /**
   * Needs to be called after pattern is read to free up resources.
   */
  public void close() throws Exception { }

}
