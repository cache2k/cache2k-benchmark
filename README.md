# Benchmarks for cache2k

This is a benchmark package for cache2k, but also contains general useful utilities for
general benchmarking and experimenting with caches.

Please see the [cache2k homepage](http://cache2k.org) for a discussion of the benchmark
results.

## Checkout the benchmark suite

To run the benchmarks for the latest release:

```
version=v0.21;
git clone -b $version https://github.com/headissue/cache2k-benchmark.git
```

Please replace the version, if needed.

You can also checkout an run the benchmarks against the latest cache2k version.
Please checkout and install the snapshot version of cache2k first, by:

```
git clone https://github.com/headissue/cache2k.git
cd cache2k
mvn install
```

## Running the benchmarks

The benchmarks are run via JUnit and the Maven surefire plugin.

```
cd cache2k-benchmark
mvn -Pbenchmark test
```

There is a shell script provided to draw nice graphics via Gnuplot:

```
bash processBenchmarkResults.sh copyData
bash processBenchmarkResults.sh process
```

The graphics will be put in `target/benchmark-reults`.

## The maven modules

### util

Java utility classes for cache benchmarking. Useful in general to produce, merge, read and write access
traces or endless randomized access patterns. Also has a fast calculation implementation
of Beladys OPT hitrate.

### thirdparty

Adaption of other cache products (at the moment Infinispan, Google Guava and EHCache) to the benchmark suite.

### traces

Traces which are used within the benchmarks to measure cache efficiency on real world access patterns.

For each trace a Java class is available to read the trace into memory and to calculate
basic metrics on it. The traces are represented as compressed file with 4-byte integers.

The original source of the trace Cpp, Glimpse, Multi2 and Sprite is from the authors of these
papers:

  * J. Kim, J. Choi, J. Kim, S. Noh, S. Min, Y. Cho, and C. Kim,
    "A Low-Overhead, High-Performance Unified Buffer Management Scheme
    that Exploits Sequential and Looping References",
    *4th Symposium on Operating System Design & Implementation, October 2000.*
  *  D. Lee, J. Choi, J. Kim, S. Noh, S. Min, Y. Cho and C. Kim,
    "On the Existence of a Spectrum of Policies that Subsumes the Least Recently Used
     (LRU) and Least Frequently Used (LFU) Policies", *Proceeding of 1999 ACM
     SIGMETRICS Conference, May 1999.*

The traces Web07 and Web12 are application traces provided by headissue GmbH

The OLTP trace was used within the ARC paper:

  * Nimrod Megiddo and Dharmendra S. Modha, "ARC: A Self-Tuning, Low Overhead 
    Replacement Cache," USENIX Conference on File and Storage Technologies (FAST 03), 
    San Francisco, CA, pp. 115-130, March 31-April 2, 2003. 

