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

/**
 * This code is adopted from the YCSB benchmark,
 *
 * original source:
 * https://github.com/brianfrankcooper/YCSB/blob/master/core/src/main/java/com/yahoo/ycsb/generator/ZipfianGenerator.java
 *
 * original license:
 *
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

import it.unimi.dsi.util.XoShiRo256StarStarRandom;
import it.unimi.dsi.util.XorShift1024StarRandomGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A generator of a zipfian distribution. It produces a sequence of items, such that some items are more popular than others, according
 * to a zipfian distribution. When you construct an instance of this class, you specify the number of items in the set to draw from, either
 * by specifying an itemcount (so that the sequence is of items from 0 to itemcount-1) or by specifying a min and a max (so that the sequence is of
 * items from min to max inclusive). After you construct the instance, you can change the number of items by calling nextInt(itemcount) or nextLong(itemcount).
 *
 * Note that the popular items will be clustered together, e.g. item 0 is the most popular, item 1 the second most popular, and so on (or min is the most
 * popular, min+1 the next most popular, etc.) If you don't want this clustering, and instead want the popular items scattered throughout the
 * item space, then use ScrambledZipfianPattern instead.
 *
 * Be aware: initializing this generator may take a long time if there are lots of items to choose from (e.g. over a minute
 * for 100 million objects). This is because certain mathematical values need to be computed to properly generate a zipfian
 * skew, and one of those values (zeta) is a sum sequence from 1 to n, where n is the itemcount. Note that if you increase
 * the number of items in the set, we can compute a new zeta incrementally, so it should be fast unless you have added
 * millions of items. However, if you decrease the number of items, we recompute zeta from scratch, so this can take a long time.
 *
 * The algorithm used here is from "Quickly Generating Billion-Record Synthetic Databases", Jim Gray et al, SIGMOD 1994.
 */
@SuppressWarnings("unused")
public class ZipfianPattern extends AbstractEternalAccessPattern {

  public static final double ZIPFIAN_CONSTANT = 0.99;

  /**
   * Number of items.
   */
  long items;

  /**
   * Min item to generate.
   */
  long base;

  /**
   * The zipfian constant to use.
   */
  double zipfianconstant;

  /**
   * Computed parameters for generating the distribution.
   */
  double alpha,zetan,eta,theta,zeta2theta;

  Random randomGenerator;

  /******************************* Constructors **************************************/

  /**
   * Create a zipfian generator for the specified number of items.
   * @param _items The number of items in the distribution.
   */
  public ZipfianPattern(long _randomSeed, long _items) {
    this(_randomSeed, 0,_items-1);
  }

  /**
   * Create a zipfian generator for items between min and max.
   * @param _min The smallest integer to generate in the sequence.
   * @param _max The largest integer to generate in the sequence.
   */
  public ZipfianPattern(long _randomSeed, long _min, long _max) {
    this(_randomSeed, _min, _max, ZIPFIAN_CONSTANT);
  }

  /**
   * Create a zipfian generator for the specified number of items using the specified zipfian constant.
   *
   * @param _items The number of items in the distribution.
   * @param _zipfianconstant The zipfian constant to use.
   */
  public ZipfianPattern(long _randomSeed, long _items, double _zipfianconstant) {
    this(_randomSeed, 0, _items-1,_zipfianconstant);
  }

  /**
   * Create a zipfian generator for items between min and max (inclusive) for the specified zipfian constant.
   * @param min The smallest integer to generate in the sequence.
   * @param max The largest integer to generate in the sequence.
   * @param _zipfianconstant The zipfian constant to use.
   */
  public ZipfianPattern(long _randomSeed, long min, long max, double _zipfianconstant) {
    this(_randomSeed, min, max, _zipfianconstant, zeta(max - min + 1 , _zipfianconstant));
  }

  /**
   * Create a zipfian generator for items between min and max (inclusive) for the specified zipfian constant, using the precomputed value of zeta.
   *
   * @param min The smallest integer to generate in the sequence.
   * @param max The largest integer to generate in the sequence.
   * @param _zipfianconstant The zipfian constant to use.
   * @param _zetan The precomputed zeta constant.
   */
  public ZipfianPattern(long _randomSeed, long min, long max, double _zipfianconstant, double _zetan) {
    items = max - min + 1;
    base = min;
    zipfianconstant = _zipfianconstant;

    theta = zipfianconstant;

    zeta2theta = zeta(2,theta);

    alpha = 1.0 / (1.0 - theta);
    zetan = _zetan;

    eta = (1 - Math.pow(2.0/items,1-theta))/(1-zeta2theta/zetan);
    randomGenerator = new XoShiRo256StarStarRandom(_randomSeed);
  }

  /**************************************************************************/

  final static Map<String, Double> zetaStaticMap = new HashMap<>();

  /**
   * Precomputed zeta constants to save setup time.
   */
  static {
    zetaStaticMap.put("2|0.99", 1.5034777750283594);
    zetaStaticMap.put("900|0.99", 7.61617572503887);
    zetaStaticMap.put("10000|0.99", 10.224361459595578);
    zetaStaticMap.put("1000000|0.99", 15.391849746037371);
    zetaStaticMap.put("8000000|0.99", 17.80436406783243);
    zetaStaticMap.put("10000000|0.99", 18.066242574968303);
    zetaStaticMap.put("80000000|0.99", 20.534952035464187);
    zetaStaticMap.put("100000000|0.99", 20.80293049002014);
    zetaStaticMap.put("800000000|0.99", 23.329143628120455);
    zetaStaticMap.put("200000000|0.99", 21.639171532673963);
  }

  /**
   * Compute the zeta constant needed for the distribution. Remember computed constants
   * in a hash map, since we may initialize the same pattern in multiple threads.
   *
   * @param n The number of items to compute zeta over.
   * @param theta The zipfian constant.
   */
  static double zeta(long n, double theta) {
    synchronized (zetaStaticMap) {
      String k = n + "|" + theta;
      Double d = zetaStaticMap.get(k);
      if (d != null) {
        return d;
      }
      double sum = 0;
      for (long i = 0; i < n; i++) {
        sum += 1 / (Math.pow(i + 1, theta));
      }
      zetaStaticMap.put(k, sum);
      System.out.println("new zeta constant: " + k + " -> " + sum);
      return sum;
    }
  }

  /****************************************************************************************/

  /**
   * Generate the next item as a long.
   *
   * @return The next item in the sequence.
   */
  long nextLong()  {
    double u = randomGenerator.nextDouble();
    double uz = u * zetan;
    if (uz < 1.0) {
      return base;
    }
    if (uz< 1.0 + Math.pow(0.5,theta)) {
      return base + 1;
    }
    return base + (long)((items) * Math.pow(eta*u - eta + 1, alpha));
  }

  /**
   * Return the next value, skewed by the Zipfian distribution. The 0th item will be the most popular, followed by the 1st, followed
   * by the 2nd, etc. (Or, if min != 0, the min-th item is the most popular, the min+1th item the next most popular, etc.) If you want the
   * popular items scattered throughout the item space, use ScrambledZipfianPattern instead.
   */
  int nextInt() {
    return (int)nextLong();
  }

  /**
   * Return the next value, skewed by the Zipfian distribution. The 0th item will be the most popular, followed by the 1st, followed
   * by the 2nd, etc. (Or, if min != 0, the min-th item is the most popular, the min+1th item the next most popular, etc.) If you want the
   * popular items scattered throughout the item space, use ScrambledZipfianPattern instead.
   */
  @Override
  public int next() {
    return nextInt();
  }

}
