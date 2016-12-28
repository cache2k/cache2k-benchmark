package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Jens Wilke
 */
public class EhCache2FactoryTest {

  @Test
  public void checkStripeSize() {
    assertEquals(1, EhCache2Factory.cpuCount2StripeCount(1));
    assertEquals(2, EhCache2Factory.cpuCount2StripeCount(2));
    assertEquals(4, EhCache2Factory.cpuCount2StripeCount(3));
    assertEquals(4, EhCache2Factory.cpuCount2StripeCount(4));
    assertEquals(8, EhCache2Factory.cpuCount2StripeCount(5));
    assertEquals(8, EhCache2Factory.cpuCount2StripeCount(8));
  }

}
