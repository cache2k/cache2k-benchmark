package org.cache2k.benchmark.util;

/*
 * #%L
 * Benchmarks: utilities
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
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
import static org.junit.Assert.*;

/**
 * @author Jens Wilke; created: 2013-11-15
 */
public class RandomAccessPatternTest {

  @Test
  public void test0() throws Exception {
    RandomAccessPattern p = new RandomAccessPattern(1);
    assertTrue(p.isEternal());
    assertTrue(p.hasNext());
    assertEquals(0, p.next());
    assertTrue(p.isEternal());
    assertTrue(p.hasNext());
    assertEquals(0, p.next());
  }
}
