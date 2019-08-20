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

import java.io.File;

/**
 * Resolve trace files in external directory.
 *
 * @author Jens Wilke
 */
public class TraceDirectory {

  public static final String DIRECTORY_ENV = "org.cache2k.benchmark.traces";
  public static final String DEFAULT_DIRECTORY = "/opt/headissue/cache2k-benchmark-trace";

  public static String resolveFile(String fileName) {
    fileName.replace("/", File.separator);
    String directory = System.getenv(DIRECTORY_ENV);
    if (directory == null) {
      directory = DEFAULT_DIRECTORY;
    }
    if (!new File(directory).isDirectory()) {
      throw new IllegalArgumentException(
        "External trace directory is missing. " +
        "Specify the environment variable " + DIRECTORY_ENV + " to point to the trace files. " +
        "Looking for trace: " + fileName);
    }
    File result = new File(directory + File.separator + fileName);
    if (!result.isFile()) {
      throw new IllegalArgumentException("Cannot find " + fileName + " in " + directory);
    }
    return result.toString();
  }
}
