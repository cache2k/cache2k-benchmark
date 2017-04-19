package org.cache2k.benchmark.jmh;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2017 headissue GmbH, Munich
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

import java.lang.reflect.Method;

/**
 * @author Jens Wilke
 */
public class PlatformUtil {

    /**
     * Obtain process ID via official API on Java 9.
     */
    private static Long getProcessIdJava9() {
      try {
        Class c = Class.forName("java.lang.ProcessHandle");
        Method _current = c.getDeclaredMethod("current");
        Method _getPid = c.getDeclaredMethod("getPid");
        Object _handle = _current.invoke(null);
        return (Long) _getPid.invoke(_handle);
      } catch (Exception ex) {
        System.err.println("ForcedGcMemoryProfiler: error obtaining PID");
        ex.printStackTrace();
      }
      return null;
    }

    static boolean isJava9() {
      String _version = System.getProperty("java.version");
      return _version.startsWith("9");
    }

    /**
     * Hack to obtain process ID. Should work on Unix/Linux and Windows.
     */
    static Long getProcessId() {
      if (isJava9()) {
        return getProcessIdJava9();
      }
      try {
        java.lang.management.RuntimeMXBean _runtimeMXBean =
          java.lang.management.ManagementFactory.getRuntimeMXBean();
        java.lang.reflect.Field jvm = _runtimeMXBean.getClass().getDeclaredField("jvm");
        jvm.setAccessible(true);
        sun.management.VMManagement mgm = (sun.management.VMManagement) jvm.get(_runtimeMXBean);
        Method _method = mgm.getClass().getDeclaredMethod("getProcessId");
        _method.setAccessible(true);
        return ((Integer) _method.invoke(mgm)).longValue();
      } catch (Exception ex) {
        System.err.println("ForcedGcMemoryProfiler: error obtaining PID");
        ex.printStackTrace();
      }
      return null;
    }

}
