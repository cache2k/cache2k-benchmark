package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: implementation variants
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

/**
 * Create a cache2k implementation variant optimized, if no eviction needs to take place.
 * We use the random eviction algorithm, which does not count hits. This is interesting to
 * see how much overhead the hit recording needs in the other implementations.
 */
public class Cache2kNoEvictionFactory extends Cache2kFactory {

  {
    if (1 == 1)
      throw new UnsupportedOperationException();
  }

}
