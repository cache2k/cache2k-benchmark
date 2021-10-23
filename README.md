# Benchmarks for Java In Memory Caches.

This project benchmarks Java caches via JMH and also eviction efficiency by
playing back access traces.

Please see the [cache2k homepage - Benchmarks](http://cache2k.org/benchmarks.html) 
for a discussion of the benchmark results.

## Running the eviction benchmarks

The benchmarks are can be run via JUnit and the Maven surefire plugin.

```
mvn -DskipTests clean install; mvn -Dtest=EvictionComparisonBenchmark -pl thirdparty test
```

## Running the JMH benchmarks

To run the JHM benchmark suite:

```
# compile
mvn -DskipTests clean package
# run full suite with maximum detail
bash jmh-run.sh --diligent complete
# Alternatively the generated commands can be printed to start JMH manually:
bash jmh-run.sh --diligent --dry complete
```

## Traces

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

The OLTP trace was used within the ARC paper:

  * Nimrod Megiddo and Dharmendra S. Modha, "ARC: A Self-Tuning, Low Overhead 
    Replacement Cache," USENIX Conference on File and Storage Technologies (FAST 03), 
    San Francisco, CA, pp. 115-130, March 31-April 2, 2003. 

The traces Web07,  Web12, OrmAccessBusy and OrmAccessNight are application traces 
provided by headissue GmbH under the CC BY 4.0 license.
