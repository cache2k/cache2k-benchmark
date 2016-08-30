package org.cache2k.benchmark.util;

/*
 * #%L
 * util
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

/**
 * Random generated access pattern, that follows a gaussian distribution
 * between 0 and upper (exclusive).
 *
 * @author Jens Wilke; created: 2013-11-24
 */
public class DistAccessPattern extends RandomAccessPattern {

  public DistAccessPattern(int upper) {
    super(upper);
  }

  @Override
  public int next() {
    double d = rng.nextGaussian();
    d = Math.abs(d);
    int v = (int) (d / 4 * upper);
    if (v >= upper) {
      v = 0;
    }
    return v;
  }

}
