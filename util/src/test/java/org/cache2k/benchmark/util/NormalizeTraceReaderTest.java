package org.cache2k.benchmark.util;

/*
 * #%L
 * cache2k-benchmark-util
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
    assertEquals(718, t.getValueCount());
    assertEquals(717, t.getHighValue());
    assertEquals(1570, t.getTraceLength());
    assertEquals(848, t.getOptHitCount(100));
    assertEquals(54, t.getOptHitRate(100).getPercent());
    assertEquals(540, t.getOptHitRate(100).get3digit());
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
    assertEquals(t1.getValueCount(), t2.getValueCount());
    assertEquals(t1.getHighValue(), t2.getHighValue());
    assertEquals(t1.getTraceLength(), t2.getTraceLength());
    assertEquals(t1.getArray()[0], t2.getArray()[0]);
    assertEquals(t1.getArray()[1], t2.getArray()[1]);
    assertEquals(t1.getArray()[7], t2.getArray()[7]);
  }

}
