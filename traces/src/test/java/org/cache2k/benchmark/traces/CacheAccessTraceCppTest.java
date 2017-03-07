package org.cache2k.benchmark.traces;

/*
 * #%L
 * Benchmarks: Access trace collection
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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

import org.cache2k.benchmark.util.AccessTrace;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Jens Wilke; created: 2013-11-21
 */
public class CacheAccessTraceCppTest {

  @Test
  public void test() {
    AccessTrace t = CacheAccessTraceCpp.getInstance();
    assertEquals(9047, t.getTraceLength());
    assertEquals(1223, t.getValueCount());
    assertEquals(8251, t.getOptHitCount(100) * 10000 / t.getTraceLength());
    assertEquals(264, t.getOptHitRate(20).get3digit());
    assertEquals(465, t.getOptHitRate(35).get3digit());
    assertEquals(628, t.getOptHitRate(50).get3digit());
    assertEquals(791, t.getOptHitRate(80).get3digit());
    assertEquals(825, t.getOptHitRate(100).get3digit());
    assertEquals(865, t.getOptHitRate(300).get3digit());
    assertEquals(865, t.getOptHitRate(500).get3digit());
    assertEquals(865, t.getOptHitRate(700).get3digit());
  }

}
