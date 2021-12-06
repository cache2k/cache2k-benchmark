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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

/**
 * @author Jens Wilke
 */
public class WebLogReaderTest {

  /**
   * Sample log line. Out web log has more fields when compared to CLF, however the parser
   * probably works for CLF as well.
   */
  public final String LOG_LINE =
    "AL4 2.247.244.177 - [05/Dec/2020:12:01:21 +0100] TLSv1.2 " +
    "ECDHE-RSA-AES256-GCM-SHA384 host 200 3249 0.001 (MISS>0.001) -% \"GET /resource.html HTTP/1.1\" 123";

  @Test
  public void testTime() {
    long t = WebLogReader.extractTime(LOG_LINE);
    assertEquals(
      "2016-12-05T11:01:21",
      LocalDateTime.ofEpochSecond(t, 0, ZoneOffset.UTC).toString());
  }

  @Test
  public void testLocation() {
    String loc = WebLogReader.extractResourceLocation(LOG_LINE);
    assertEquals("/resource.html", loc);
  }

}
