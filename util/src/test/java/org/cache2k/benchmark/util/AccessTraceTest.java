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

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * @author Jens Wilke; created: 2013-11-19
 */
public class AccessTraceTest {

  @Test
  public void testWriteRead() throws Exception {
    AccessTrace t = new AccessTrace(getAccessPattern());
    File f = new File("tmp-" + System.currentTimeMillis());
    t.write(f);
    AccessTrace t2 = new AccessTrace(f);
    assertEquals(t.getTraceLength(), t2.getTraceLength());
    assertEquals(t.getHighValue(), t2.getHighValue());
    assertEquals(t.getLowValue(), t2.getLowValue());
    assertEquals(t.getValueCount(), t2.getValueCount());
    f.delete();
  }

  private AccessPattern getAccessPattern() {
    return new NormalizeTraceReader(
      this.getClass().getResourceAsStream("/trace-mt-20121220-partial.txt"),
      Charset.forName("UTF-8"));
  }

  public void testReadWriteNormalizedTrace() throws Exception {
    NormalizeTraceReader p = new NormalizeTraceReader(
          this.getClass().getResourceAsStream("/xy.txt"),
          Charset.forName("UTF-8"));
    AccessTrace t = new AccessTrace(p);
    System.err.println("Access count: " + t.getTraceLength());
    File f = new File("xy.trc.bin");
    t.write(f);
  }

  public void testReadWriteIntegerTrace() throws Exception {
    IntegerTraceReader p = new IntegerTraceReader(
          this.getClass().getResourceAsStream("/xy.txt"),
          Charset.forName("UTF-8"));
    AccessTrace t = new AccessTrace(p);
    System.err.println("Access count: " + t.getTraceLength());
    File f = new File("xy.trc.bin");
    t.write(f);
  }

}
