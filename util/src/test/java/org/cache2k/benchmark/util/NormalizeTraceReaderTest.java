package org.cache2k.benchmark.util;

/*
 * #%L
 * cache2k-benchmark-util
 * %%
 * Copyright (C) 2013 - 2014 headissue GmbH, Munich
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

import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * @author Jens Wilke; created: 2013-11-19
 */
public class NormalizeTraceReaderTest {

  @Test
  public void testBasicProperties() throws Exception {
    AccessTrace t = new AccessTrace(getAccessPattern());
  }

  private AccessPattern getAccessPattern() {
    return new NormalizeTraceReader(
      this.getClass().getResourceAsStream("/trace-mt-20121220-partial.txt"),
      Charset.forName("UTF-8"));
  }

  @Test
  public void testSame() throws Exception {
    AccessTrace t1 = new AccessTrace(getAccessPattern());
    AccessPattern p =
      new Base36TraceReader(
        this.getClass().getResourceAsStream("/trace-mt-20121220-partial.txt"),
        Charset.forName("UTF-8"));
    AccessTrace t2 = new AccessTrace(new NormalizePatternFilter(p));
  }

}
