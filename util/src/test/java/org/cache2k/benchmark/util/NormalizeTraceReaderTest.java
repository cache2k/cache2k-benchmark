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
