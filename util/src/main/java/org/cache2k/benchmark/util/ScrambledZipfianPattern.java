package org.cache2k.benchmark.util;

/*
 * #%L
 * util
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
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

/**
 * This code is adopted from the YCSB benchmark,
 *
 * original source:
 * https://github.com/brianfrankcooper/YCSB/blob/master/core/src/main/java/com/yahoo/ycsb/generator/ScrambledZipfianGenerator.java
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

/**
 * A generator of a zipfian distribution. It produces a sequence of items, such that some items are more popular than others, according
 * to a zipfian distribution. When you construct an instance of this class, you specify the number of items in the set to draw from, either
 * by specifying an itemcount (so that the sequence is of items from 0 to itemcount-1) or by specifying a min and a max (so that the sequence is of
 * items from min to max inclusive). After you construct the instance, you can change the number of items by calling nextInt(itemcount) or nextLong(itemcount).
 *
 * Unlike @ZipfianPattern, this class scatters the "popular" items across the itemspace. Use this, instead of @ZipfianPattern, if you
 * don't want the head of the distribution (the popular items) clustered together.
 */
public class ScrambledZipfianPattern extends AbstractEternalAccessPattern {

  ZipfianPattern gen;
  long min, max, itemcount;

  /******************************* Constructors **************************************/

  /**
   * Create a zipfian generator for the specified number of items.
   * @param _items The number of items in the distribution.
   */
  public ScrambledZipfianPattern(long _items) {
    this(0,_items-1);
  }

  /**
   * Create a zipfian generator for items between min and max.
   * @param min The smallest integer to generate in the sequence.
   * @param max The largest integer to generate in the sequence.
   */
  public ScrambledZipfianPattern(long min, long max) {
    this(min, max, ZipfianPattern.ZIPFIAN_CONSTANT);
  }

  /**
   * Create a zipfian generator for the specified number of items using the specified zipfian constant.
   *
   * @param _items The number of items in the distribution.
   * @param _zipfianconstant The zipfian constant to use.
   */
	/*
	public ScrambledZipfianPattern(long _items, double _zipfianconstant)
	{
		this(0,_items-1,_zipfianconstant);
	}
*/

  /**
   * Create a zipfian generator for items between min and max (inclusive) for the specified zipfian constant. If you
   * use a zipfian constant other than 0.99, this will take a long time to complete because we need to recompute zeta.
   * @param min The smallest integer to generate in the sequence.
   * @param max The largest integer to generate in the sequence.
   * @param _zipfianconstant The zipfian constant to use.
   */
  public ScrambledZipfianPattern(long min, long max, double _zipfianconstant) {
    this.min = min;
    this.max = max;
    itemcount = this.max - this.min +1;
    if (_zipfianconstant == USED_ZIPFIAN_CONSTANT) {
      gen = new ZipfianPattern(0, ITEM_COUNT,_zipfianconstant,ZETAN);
    } else {
      gen = new ZipfianPattern(0, ITEM_COUNT,_zipfianconstant);
    }
  }

  /**
   * Return the next int in the sequence.
   */
  int nextInt() {
    return (int)nextLong();
  }

  /**
   * Return the next long in the sequence.
   */
  long nextLong() {
    long ret = gen.nextLong();
    ret = min + fNVhash64(ret) % itemcount;
    return ret;
  }

  @Override
  public int next() throws Exception {
    return nextInt();
  }

  public static final double ZETAN = 26.46902820178302;
  public static final double USED_ZIPFIAN_CONSTANT=0.99;
  public static final long ITEM_COUNT=10000000000L;

  public static final long FNV_offset_basis_64 = 0xCBF29CE484222325L;
  public static final long FNV_prime_64 = 1099511628211L;

  /**
   * 64 bit FNV hash. Produces more "random" hashes than (say) String.hashCode().
   *
   * @param val The value to hash.
   * @return The hash value
   */
  public static long fNVhash64(long val) {
    long hashval = FNV_offset_basis_64;

    for (int i=0; i<8; i++) {
      long octet=val&0x00ff;
      val=val>>8;

      hashval = hashval ^ octet;
      hashval = hashval * FNV_prime_64;
    }
    return Math.abs(hashval);
  }

}
