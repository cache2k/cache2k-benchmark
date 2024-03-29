package org.cache2k.benchmark;

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

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheEntry;
import org.cache2k.io.CacheLoader;
import org.junit.Test;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * This is a proof of concept to cache the results of the Java date formatter.
 * The idea is that date formatting speed may be improved by caching, because there
 * will be not so many different dates to be displayed in one application.
 *
 * <p/>The most flexible approach is to use an associative caching approach or e.g.
 * instead of DateFormat.getDateInstance(format, locale).format(date) use
 * cache.get(locale).get(format).get(date). Another approach is to use a single
 * cache with a cache key e.g. cache.get(new CacheKey(locale, format, date). See
 * the test implementations below.
 *
 * @author Jens Wilke; created: 2013-12-08
 */
public class DateFormattingBenchmark {

  static List<Date> dates;

  static List<Date> provideListWith3MillionDates() {
    if (dates != null) {
      return dates;
    }
    Random r = new Random(1802);
    ArrayList<Date> l = new ArrayList<>();
    for (int i = 0; i < 3000000; i++) {
      l.add(new Date(r.nextInt(200)));
    }
    return dates = l;
  }

  /**
   * Straight forward formatting. Get a new formatter every time.
   */
  @Test
  public void testWithoutCacheAlwaysNewFormatter() {
    PrintWriter w = new PrintWriter(new CharArrayWriter());
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
      w.print(df.format(d));
    }
  }

  @Test
  public void testWithoutCacheSingleFormatter() {
    PrintWriter w = new PrintWriter(new CharArrayWriter());
    DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      w.print(df.format(d));
    }
  }

  /**
   * Work with a single date formatter, but synchronize the access.
   */
  @Test
  public void testWithCacheSingleFormatter() {
    final PrintWriter w = new PrintWriter(new CharArrayWriter());
    final DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
    Cache<Date, String> c =
      Cache2kBuilder.of(Date.class, String.class)
        .loader(new CacheLoader<Date, String>() {
          @Override
          public synchronized String load(Date o) {
            return df.format(o);
          }
        })
        .build();
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      w.print(c.get(d));
    }
  }

  /**
   * Always produce a new date formatter.
   */
  @Test
  public void testWithCacheNewFormatter() {
    final PrintWriter w = new PrintWriter(new CharArrayWriter());
    Cache<Date, String> c =
      Cache2kBuilder.of(Date.class, String.class)
        .loader(new CacheLoader<Date, String>() {
          @Override
          public String load(Date o) {
            DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
            return df.format(o);
          }
        })
        .build();
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      w.print(c.get(d));
    }
    c.close();
  }

  /**
   * Associative caching: This puts a cache, in the cache, within a cache.
   * Although three cache requests are made, the speed is comparable to the
   * other versions. Disadvantage: Since we have a bunch of caches, we don't
   * have control on how many date strings will be cached.
   *
   * <p/>Warning: This works, but it may hog memory since caches will not be garbage collected.
   * Either support noname caches which are garbage collected, or make use of an eviction event
   * to call destroy() on the cache.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testAssociativeCache() {
    Cache<Locale, Cache<Integer, Cache<Date, String>>> c = (Cache)
      Cache2kBuilder.of(Locale.class, Cache.class)
        .eternal(true)
        .name(DateFormattingBenchmark.class, "testAssociativeCache")
        .loader(new CacheLoader<Locale, Cache>() {
          @Override
          public Cache load(final Locale l) {
            return
              Cache2kBuilder.of(Integer.class, Cache.class)
                .name(DateFormattingBenchmark.class, "testAssociativeCache-" + l)
                .eternal(true)
                .loader(new CacheLoader<Integer, Cache>() {
                  public Cache load(final Integer _format) {
                    return Cache2kBuilder.of(Date.class, String.class)
                      .name(DateFormattingBenchmark.class, "testAssociativeCache-" + l + "-" + _format)
                      .loader(new CacheLoader<Date, String>() {
                        public String load(Date d) {
                          DateFormat df = DateFormat.getDateInstance(_format, l);
                          return df.format(d);
                        }
                      })
                      .build();
                  }
              })
              .build();
          }
        })
        .build();
    PrintWriter w = new PrintWriter(new CharArrayWriter());
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      w.print(c.get(Locale.FRANCE).get(DateFormat.LONG).get(d));
    }
    for (CacheEntry<Locale, Cache<Integer, Cache<Date, String>>> e : c.entries()) {
      for (CacheEntry<Integer, Cache<Date, String>> e1 : e.getValue().entries()) {
        e1.getValue().close();
      }
      e.getValue().close();
    }
    c.close();
  }

  /**
   * Work with cache key object and one big cache.
   */
  @Test
  public void testWithCacheAndKeyObject() {
    Cache<CacheKey, String> c =
      Cache2kBuilder.of(CacheKey.class, String.class)
        .eternal(true)
        .loader(new CacheLoader<CacheKey, String>() {
          @Override
          public String load(CacheKey o) {
            DateFormat df = DateFormat.getDateInstance(o.format, o.locale);
            return df.format(o.date);
          }
        })
        .build();
    PrintWriter w = new PrintWriter(new CharArrayWriter());
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      w.print(c.get(new CacheKey(Locale.FRANCE, DateFormat.LONG, d)));
    }
    c.close();
  }

  static class CacheKey {
    Locale locale;
    int format;
    Date date;

    CacheKey(Locale locale, int format, Date date) {
      this.locale = locale;
      this.format = format;
      this.date = date;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CacheKey that = (CacheKey) o;
      if (format != that.format) return false;
      if (!date.equals(that.date)) return false;
      if (!locale.equals(that.locale)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result = locale.hashCode();
      result = 31 * result + format;
      result = 31 * result + date.hashCode();
      return result;
    }
  }

}
