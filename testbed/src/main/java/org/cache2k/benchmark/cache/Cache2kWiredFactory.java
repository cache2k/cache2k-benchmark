package org.cache2k.benchmark.cache;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
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

/**
 * Build a cache2k cache that is tuned towards optimal eviction.
 * Normally cache2k build multiple segments in multi core environments
 * to increase throughput. This will create only a single cache segment.
 *
 * @author Jens Wilke
 */
public class Cache2kWiredFactory extends Cache2kFactory {

  {
    setWiredCache(true);
    setName("cache2kw");
  }

}
