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

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jens Wilke
 */
public class ZipfianTest {

  @Test
  public void test() {
    ScrambledZipfianPattern g = new ScrambledZipfianPattern(123, 900);
    Set<Long> uniqueVals = new HashSet<>();
    for (int i = 0; i < 10000; i++) {
      uniqueVals.add(g.nextLong());
    }
    assertEquals(900, uniqueVals.size());
  }

}
