package org.cache2k.benchmark;

/*
 * #%L
 * cache2k-benchmark-zoo
 * %%
 * Copyright (C) 2013 - 2015 headissue GmbH, Munich
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

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.cache2k.Cache;
import org.cache2k.CacheBuilder;
import org.cache2k.CacheSource;
import org.cache2k.impl.ClockCache;
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
@BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 2)
public class DateFormattingBenchmark extends AbstractBenchmark {

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
      CacheBuilder.newCache(Date.class, String.class)
        .source(new CacheSource<Date, String>() {
          @Override
          public synchronized String get(Date o) {
            return df.format(o);
          }
        })
        .implementation(ClockCache.class)
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
      CacheBuilder.newCache(Date.class, String.class)
        .source(new CacheSource<Date, String>() {
          @Override
          public String get(Date o) {
            DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
            return df.format(o);
          }
        })
        .implementation(ClockCache.class)
        .build();
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      w.print(c.get(d));
    }
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
      CacheBuilder.newCache(Locale.class, Cache.class)
        .source(new CacheSource<Locale, Cache>() {
          @Override
          public Cache get(final Locale l) {
            return
              CacheBuilder.newCache(Integer.class, Cache.class)
                .source(new CacheSource<Integer, Cache>() {
                  public Cache get(final Integer f) {
                    return CacheBuilder.newCache(Date.class, String.class)
                      .source(new CacheSource<Date, String>() {
                        public String get(Date d) {
                          DateFormat df = DateFormat.getDateInstance(f, l);
                          return df.format(d);
                        }
                      })
                      .implementation(ClockCache.class)
                      .build();
                  }
              })
              .implementation(ClockCache.class)
              .build();
          }
        })
        .implementation(ClockCache.class)
        .build();
    PrintWriter w = new PrintWriter(new CharArrayWriter());
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      w.print(c.get(Locale.FRANCE).get(DateFormat.LONG).get(d));
    }
  }

  /**
   * Work with cache key object and one big cache.
   */
  @Test
  public void testWithCacheAndKeyObject() {
    Cache<CacheKey, String> c =
      CacheBuilder.newCache(CacheKey.class, String.class)
        .source(new CacheSource<CacheKey, String>() {
          @Override
          public String get(CacheKey o) {
            DateFormat df = DateFormat.getDateInstance(o.format, o.locale);
            return df.format(o.date);
          }
        })
        .implementation(ClockCache.class)
        .build();
    PrintWriter w = new PrintWriter(new CharArrayWriter());
    List<Date> l = provideListWith3MillionDates();
    for (Date d : l) {
      w.print(c.get(new CacheKey(Locale.FRANCE, DateFormat.LONG, d)));
    }
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
