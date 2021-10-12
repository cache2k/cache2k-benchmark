package org.cache2k.benchmark.jmh;

/*
 * #%L
 * Benchmarks: JMH suite.
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

import org.cache2k.benchmark.BenchmarkCacheFactory;
import org.cache2k.benchmark.cache.Cache2kFactory;
import org.cache2k.benchmark.cache.JCacheCacheFactory;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.Closeable;
import java.io.IOException;

/**
 * Base for all JMH cache benchmarks, controlling the cache lifecycle and
 * recording memory usage.
 *
 * @author Jens Wilke
 */
@State(Scope.Benchmark)
public class BenchmarkBase {

  @Param("DEFAULT")
  public String cacheFactory;

  @Param("")
  public String cacheProvider;

  @Param("")
  public String cacheName;

  @Param("")
  public String enableStatistics;

  @Param("")
  public String shortName;

  public BenchmarkCacheFactory getFactory() {
    try {
      if ("DEFAULT".equals(cacheFactory)) {
        cacheFactory = Cache2kFactory.class.getCanonicalName();
      }
      BenchmarkCacheFactory factoryInstance =
        (BenchmarkCacheFactory) Class.forName(cacheFactory).newInstance();
      if (factoryInstance instanceof JCacheCacheFactory) {
      	JCacheCacheFactory jCacheCacheFactory = (JCacheCacheFactory) factoryInstance;
      	jCacheCacheFactory.setCacheName(cacheName);
      	jCacheCacheFactory.setProvider(cacheProvider);
			}
      if (!"".equals(enableStatistics)) {
        factoryInstance.withStatistics(true);
      }
      return factoryInstance;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void recordMemoryAndDestroy(Closeable _closeable) {
    ForcedGcMemoryProfiler.recordUsedMemory();
    closeIfNeeded(_closeable);
  }

  public void closeIfNeeded(final Closeable closeable) {
    if (closeable != null) {
      System.out.println();
      String startString = closeable.toString();
      System.out.println(startString);
      System.out.println("availableProcessors: " + Runtime.getRuntime().availableProcessors());
      try {
        closeable.close();
      } catch (IOException _ignore) {
      }
    }
  }

}
