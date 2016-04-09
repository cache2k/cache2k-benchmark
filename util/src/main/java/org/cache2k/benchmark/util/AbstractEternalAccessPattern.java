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
 * Base class for generated patterns such as random.
 *
 * @author Jens Wilke; created: 2013-11-14
 */
public abstract class AbstractEternalAccessPattern extends AccessPattern {

  /**
   * Always true.
   */
  public final boolean isEternal() {
    return true;
  }

  /**
   * Always true.
   */
  public final boolean hasNext() throws Exception {
    return true;
  }

}
