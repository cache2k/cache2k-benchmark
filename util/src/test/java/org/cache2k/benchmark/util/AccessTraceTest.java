package org.cache2k.benchmark.util;

/*
 * #%L
 * util
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
