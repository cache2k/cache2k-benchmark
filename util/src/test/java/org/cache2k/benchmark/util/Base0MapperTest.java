package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
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
public class Base0MapperTest {

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
    AccessTrace t2 = new AccessTrace(new Base0Mapper(p));
    assertEquals(t1.getValueCount(), t2.getValueCount());
    assertEquals(t1.getHighValue(), t2.getHighValue());
    assertEquals(t1.getLength(), t2.getLength());
    assertEquals(t1.getArray()[0], t2.getArray()[0]);
    assertEquals(t1.getArray()[1], t2.getArray()[1]);
    assertEquals(t1.getArray()[7], t2.getArray()[7]);
  }

}
