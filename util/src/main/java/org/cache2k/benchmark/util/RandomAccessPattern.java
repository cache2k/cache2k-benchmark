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

import it.unimi.dsi.util.XoShiRo256StarStarRandom;

import java.util.Random;

/**
 * Random generated access pattern. A pseudo random number generator is used,
 * so after initializing always the same number sequence is generated.
 *
 * @author Jens Wilke
 */
public class RandomAccessPattern extends AbstractEternalAccessPattern {

  protected Random rng = new XoShiRo256StarStarRandom(1802);

  protected int upper;

  /**
   * New random number pattern.
   *
   * @param upperBound generates numbers between zero and this minus one.
   */
  public RandomAccessPattern(int upperBound) {
    upper = upperBound;
  }

  @Override
  public int next() {
    return rng.nextInt(upper);
  }

}
