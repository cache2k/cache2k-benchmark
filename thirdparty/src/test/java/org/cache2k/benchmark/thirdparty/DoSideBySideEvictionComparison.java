package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
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

import org.cache2k.benchmark.BenchmarkCache;
import org.cache2k.benchmark.EvictionListener;
import org.cache2k.benchmark.PrototypeCacheFactory;
import org.cache2k.benchmark.cache.Cache2kStarFactory;
import org.cache2k.benchmark.prototype.evictionPolicies.Cache2kV14Eviction;
import org.cache2k.benchmark.traces.Traces;
import org.cache2k.benchmark.util.AccessTrace;
import org.junit.Ignore;
import org.junit.Test;

import java.io.PrintStream;

/**
 * Run a trace against two cache implementations and compare eviction.
 * Used to debug algorithm differences.
 *
 * @author Jens Wilke
 */
public class DoSideBySideEvictionComparison {

  public static void main(String[] args) {
    int capacity = 1000;
    long missCount1 = 0;
    long missCount2 = 0;
    final PrintStream out = System.out;
    AccessTrace trace = Traces.GLIMPSE.get();
    BenchmarkCache<Integer, Integer> cache1 = PrototypeCacheFactory.of(Cache2kV14Eviction.class)
      .withEvictionListener(new EvictionListener<Object>() {
        @Override
        public void evicted(Object key) {
          out.print(" evict1: " + key);
        }
      }).create(Integer.class, Integer.class, capacity);
    BenchmarkCache<Integer, Integer> cache2 = new Cache2kStarFactory()
      .withEvictionListener(new EvictionListener<Object>() {
        @Override
        public void evicted(Object key) {
          out.print(" evict2: " + key);
        }
      }).create(Integer.class, Integer.class, capacity);
    Integer[] objTrace = trace.getObjectArray();
    int step = 0;
    for (Integer k : objTrace) {
      out.print(step);
      out.print(": ");
      out.print(k);
      out.print(" ");
      Integer v1 = cache1.get(k);
      Integer v2 = cache2.get(k);
      if (v1 == null) {
        out.print("O");
      } else {
        out.print("X");
      }
      if (v2 == null) {
        out.print("O");
      } else {
        out.print("X");
      }
      if (v1 == null) {
        cache1.put(k, k);
        missCount1++;
      }
      if (v2 == null) {
        cache2.put(k, k);
        missCount2++;
      }
      out.println();
      if (step == 1009) {
        System.err.println(cache1);
        System.err.println(cache2);
        return;
      }
      step++;
    }

  }

}
