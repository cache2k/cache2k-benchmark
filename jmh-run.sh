#!/bin/bash

# jmh-run.sh

# Copyright 2016 headissue GmbH, Jens Wilke

# This script to run a benchmark suite with JMH.
#
# Parameters:
#
# --quick   quick run with reduced benchmark time, to check for errors
# --no3pty  only benchmark cache2k and JDK build ins, used for continuous benchmarking against performance regressions
# --perf    run the benchmark with Linux perf (needs testing)
# JAVA_HOME

set -e;
# set -x;

# http://mechanical-sympathy.blogspot.de/2011/11/biased-locking-osr-and-benchmarking-fun.html
# http://www.oracle.com/technetwork/tutorials/tutorials-1876574.html
# test -n "$BENCHMARK_JVM_ARGS" || BENCHMARK_JVM_ARGS="-server -Xmx2G -XX:+UseG1GC";

# biased locking delay is 4000 by default, enable from the start to minimize effects on the first benchmark iteration
# (check with: ava  -XX:+UnlockDiagnosticVMOptions -XX:+PrintFlagsFinal 2>/dev/null | grep BiasedLockingStartupDelay)
test -n "$BENCHMARK_JVM_ARGS" || BENCHMARK_JVM_ARGS="-server -Xmx10G -XX:BiasedLockingStartupDelay=0 -verbose:gc";
# extra G1 args
# BENCHMARK_JVM_ARGS="$BENCHMARK_JVM_ARGS -XX:+UseG1GC -XX:-G1UseAdaptiveConcRefinement -XX:G1ConcRefinementGreenZone=2G -XX:G1ConcRefinementThreads=0";

# -wi warmup iterations
# -w warmup time
# -i number of iterations
# -r time
# -f how many time to fork a single benchmark

test -n "$BENCHMARK_QUICK" || BENCHMARK_QUICK="-f 1 -wi 1 -w 3s -i 2 -r 3s -foe true";

# -f 2 / -i 2 has not enough confidence, there is sometime one outlier
# 2 full warmups otherwise there is big jitter with G1
# -gc true: careful with -gc true, this seems to influence the measures performance significantly
test -n "$BENCHMARK_DILIGENT" || BENCHMARK_DILIGENT="-f 3 -wi 2 -w 60s -i 3 -r 60s";

# longer test run for expiry tests
test -n "$BENCHMARK_DILIGENT_LONG" || BENCHMARK_DILIGENT_LONG="-f 2 -wi 1 -w 180s -i 2 -r 180s";
# test -n "$BENCHMARK_DILIGENT_LONG" || BENCHMARK_DILIGENT_LONG="-f 2 -wi 2 -w 15s -i 3 -r 15s";


# setup for blog article:
# 5x30 warumups needed for cache2k 10M performance with CMS
# test -n "$BENCHMARK_DILIGENT" || BENCHMARK_DILIGENT="-f 2 -wi 5 -w 30s -i 3 -r 30s";

# other experiments:
# test -n "$BENCHMARK_DILIGENT" || BENCHMARK_DILIGENT="-gc true -f 3 -wi 5 -w 30s -i 5 -r 30s";
# test -n "$BENCHMARK_DILIGENT" || BENCHMARK_DILIGENT="-f 3 -wi 5 -w 30s -i 5 -r 30s";
# test -n "$BENCHMARK_DILIGENT" || BENCHMARK_DILIGENT="-gc true -f 2 -wi 0 -w 40s -i 8 -r 20s";

# Tinker benchmark options to do profiling and add assembler code output (linux only).
# Needs additional disassembly library to display assembler code
# see: http://psy-lob-saw.blogspot.de/2013/01/java-print-assembly.html
# and, see: https://wiki.openjdk.java.net/display/HotSpot/PrintAssembly
# download from: https://kenai.com/projects/base-hsdis/downloads
# install with e.g.: mv ~/Downloads/linux-hsdis-amd64.so jdk1.8.0_45/jre/lib/amd64/hsdis-amd64.so.
# For profiling only do one fork, but more measurement iterations
# profilers are described here: http://java-performance.info/introduction-jmh-profilers
# hsdis is available as Ubuntu package: sudo apt-get install libhsdis0-fcml
test -n "$BENCHMARK_PERFASM" || BENCHMARK_PERFASM="-f 1 -wi 1 -w 10s -i 1 -r 20s -prof perfasm";
# longer test run for expiry tests
test -n "$BENCHMARK_PERFASM_LONG" || BENCHMARK_PERFASM_LONG="-f 1 -wi 1 -w 180s -i 1 -r 180s -prof perfasm";

# hs_gc: detailed counters from the GC implementation
STANDARD_PROFILER="-prof comp -prof gc -prof hs_rt -prof hs_gc";

STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.ForcedGcMemoryProfiler";
STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.LinuxVmProfiler";
STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.MiscResultRecorderProfiler";
STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.GcProfiler";

EXTRA_PROFILER="";

# not used yet
PERF_NORM_OPTIONS="-prof perfnorm:useDefaultStat=true"

OPTIONS="$BENCHMARK_DILIGENT";
OPTIONS_LONG="$BENCHMARK_DILIGENT_LONG";

if test -z "$JAVA_HOME"; then
  echo "JAVA_HOME needs to be set" 1>&2
  exit 1;
fi

java=$JAVA_HOME/bin/java

unset cache2k;
unset no3pty;
unset dry;
unset backends;
unset quick;

usage() {
  echo "Usage: $0 options"
  echo "--quick              Run smoke test only, not the full benchmark"
  echo "--perfasm"
  echo "--perfnorm"
  echo "--no3pty             Do not test 3rd-party backends"
  echo "--backends backends  Test the given backends, e.g. 'thirdparty.TCache1Factory Cache2kFactory"
  echo "--dry                Log the command lines to execute, but do not run test"
}

processCommandLine() {
  while true; do
    case "$1" in
      --quick) quick=true;
               OPTIONS="$BENCHMARK_QUICK";
               OPTIONS_LONG="$BENCHMARK_QUICK";;
      --perfasm) OPTIONS="$BENCHMARK_PERFASM";
                 OPTIONS_LONG="$BENCHMARK_PERFASM_LONG";;
      --perfnorm) EXTRA_PROFILER=$EXTRA_PROFILER" $PERF_NORM_OPTIONS";;
      --no3pty) no3pty=true;;
      --cache2k) cache2k=true;;
      --backends) backends="$2"; shift; ;;
      --dry) dry=true;
             java="dryEcho";;
      -*) echo "unknown option: $1"; usage; exit 1;;
      *) "$1";
         stopTimer;
         exit 0;;
    esac
    shift 1;
  done
}

filterProgress() {
awk '/^# Run .*/ { print; }';
}

START_TIME=0;

startTimer() {
START_TIME=`date +%s`;
}

stopTimer() {
local t=`date +%s`;
echo "Total runtime $(( $t - $START_TIME ))s";
}

# quote argument if it is containing whitespace
dryEcho() {
  echo -n "java";
  for i in "$@"; do
    #if [[ $i =~ [[:space:]] ]]; then
    #  echo -n ' "'"$i"'"'
    #else
    #  echo -n " $i";
    #fi
    printf " %q" "$i";
  done
  echo;
}

JAR="jmh-suite/target/benchmarks.jar";

unset SINGLE_THREADED;
unset NO_EVICTION;

# add tests of JDK internal stuff (useful as base line reference), if not cache2k only is requested.
if test -z "$cache2k"; then

  # Implementations with no eviction (actually not a cache) and not thread safe
  SINGLE_THREADED="HashMapFactory"

  # Implementations with no eviction (actually not a cache) and thread safe
  NO_EVICTION="ConcurrentHashMapFactory"

fi

# Implementations with complete caching features
COMPLETE="Cache2kFactory"

TARGET="$HOME/jmh-result";
test -d $TARGET || mkdir -p $TARGET;

if test -n "$backends"; then
  COMPLETE="$backends";
elif test -z "$no3pty"; then
  COMPLETE="$COMPLETE thirdparty.CaffeineCacheFactory thirdparty.GuavaCacheFactory thirdparty.EhCache3Factory";
  # "thirdparty.EhCache3Factory";
fi

startTimer;

cpuList() {
if [ "$1" = 1 ]; then
  echo "1";
elif [ "$1" = 2 ]; then
  echo "1,2";
elif [ "$1" = 3 ]; then
  echo "1,2,3";
elif [ "$1" = 4 ]; then
  echo "1,2,3,0";
fi
}

limitCoresViaTaskSet() {
if test -n "$dry"; then
  shift;
  "$@";
  return;
fi
local cnt=$1;
shift;
taskset -c `cpuList $cnt` "$@";
}

limitCores() {
if test -z "$dry"; then
  ./limitCoreCount.sh $1;
fi
shift;
"$@";
}

#
# Benchmarks for maximum throughput / benchmark overhead
#
# benchmarks="ZipfianSequenceLoadingRpngThroughputBenchmark ZipfianSequenceLoadingRpngWithBoxingThroughputBenchmark";
benchmarkMaxThroughput() {
benchmarks="ZipfianSequenceLoadingRpngWithBoxingThroughputBenchmark";
impl="maxthroughput";
for benchmark in $benchmarks; do
    for threads in 1 2 4; do
      runid="$impl-$benchmark-$threads";
      fn="$TARGET/result-$runid";
      echo;
      echo "## $runid";
      sync
      limitCores $threads $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS $STANDARD_PROFILER  $EXTRA_PROFILER \
           -t $threads \
           -rf json -rff "$fn.json" \
           2>&1 | tee $fn.out | filterProgress
      if test -n "$dry"; then
        cat $fn.out;
      else
        echo "=> $fn.out";
      fi
    done
done
}

allImpls="`cat - << "EOF"
-p cacheFactory=org.cache2k.benchmark.cache.Cache2kFactory
-p cacheFactory=org.cache2k.benchmark.cache.Cache2kWiredFactory
-p cacheFactory=org.cache2k.benchmark.cache.GuavaCacheFactory
-p cacheFactory=org.cache2k.benchmark.cache.CaffeineCacheFactory
-p cacheFactory=org.cache2k.benchmark.cache.EhCache3Factory
EOF
`"

caches="`cat - << "EOF"
-p cacheFactory=org.cache2k.benchmark.Cache2kFactory
-p cacheFactory=org.cache2k.benchmark.JCacheFactory -p cacheProvider=org.cache2k.jcache.provider.JCacheProvider
EOF
`"

cache2k="`cat - << "EOF"
-p cacheFactory=org.cache2k.benchmark.Cache2kFactory
-p cacheFactory=org.cache2k.benchmark.Cache2kWiredFactory
EOF
`"

guava="`cat - << "EOF"
-p cacheFactory=org.cache2k.benchmark.thirdparty.GuavaCacheFactory
EOF
`"

maps="`cat - << "EOF"
-p cacheFactory=org.cache2k.benchmark.ConcurrentHashMapFactory
-p cacheFactory=org.cache2k.benchmark.SynchronizedLinkedHashMapFactory
-p cacheFactory=org.cache2k.benchmark.PartitionedLinkedHashMapFactory
EOF
`"

#
# Multi threaded with variable thread counts, no eviction needed
#
suiteNoEviction(){
if test -n "$quick"; then
  OPTIONS="-f 1 -wi 0 -i 1 -r 5s -foe true";
else
  OPTIONS="-f 2 -wi 2 -w 15s -i 3 -r 15s";
fi
benchmarks="PopulateParallelOnceBenchmark ReadOnlyBenchmark";
# benchmarks="";
# for impl in $NO_EVICTION $COMPLETE; do
(
  echo "$guava"
  echo "$maps"
) | while read impl; do
  for benchmark in $benchmarks; do
    for threads in 1 2 4; do
      # remove -p for a nice file name
      short="`echo "$impl" |sed "s/^\-p //g" | sed "s/ -p /:/g" `"
      runid="$short-$benchmark-$threads";
      fn="$TARGET/result-$runid";
      echo;
      echo "## $runid";
      sync
      limitCores $threads $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS -gc true $STANDARD_PROFILER $EXTRA_PROFILER \
           -t $threads $impl \
           -rf json -rff "$fn.json" \
           2>&1 | tee $fn.out | filterProgress
      if test -n "$dry"; then
        cat $fn.out;
      else
        echo "=> $fn.out";
      fi
    done
  done
done
}

#
# Compare CHM and a "naive" linkedhashmap with Guava
#
suiteNaiveNoEviction(){
if test -n "$quick"; then
  OPTIONS="-f 1 -wi 1 -w 3s -i 4 -r 3s -foe true";
else
  OPTIONS="-f 2 -wi 2 -w 15s -i 3 -r 15s";
fi
# benchmarks="";
# for impl in $NO_EVICTION $COMPLETE; do
benchmark=ReadOnlyBenchmark;
(
  echo "$allImpls"
  echo "$maps"
) | while read impl; do
  for threads in 1 2 3 4; do
    # remove -p for a nice file name
    short="`echo "$impl" |sed "s/^\-p //g" | sed "s/ -p /:/g" `"
    runid="$short-$benchmark-$threads";
    fn="$TARGET/result-$runid";
    echo;
    echo "## $runid";
    sync
    limitCores $threads $java -jar $JAR \\.$benchmark -p hitRate=100 -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS $STANDARD_PROFILER $EXTRA_PROFILER \
         -t $threads $impl \
         -rf json -rff "$fn.json" \
         2>&1 | tee $fn.out | filterProgress
    if test -n "$dry"; then
      cat $fn.out;
    else
      echo "=> $fn.out";
    fi
  done
done
}

suiteZipfian() {
# benchmarks="PrecalculatedZipfianSequenceLoadingBenchmark";
# benchmarks="ZipfianSequenceLoadingBenchmark PrecalculatedZipfianSequenceLoadingBenchmark";
benchmarks="ZipfianSequenceLoadingBenchmark";
for impl in $COMPLETE; do
  for benchmark in $benchmarks; do
    for threads in 4; do
      runid="$impl-$benchmark-$threads";
      fn="$TARGET/result-$runid";
      echo;
      echo "## $runid";
      sync
      limitCores $threads $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS $STANDARD_PROFILER  $EXTRA_PROFILER \
           -t $threads -p cacheFactory=org.cache2k.benchmark.$impl \
           -rf json -rff "$fn.json" \
           2>&1 | tee $fn.out | filterProgress
      if test -n "$dry"; then
        cat $fn.out;
      else
        echo "=> $fn.out";
      fi
    done
  done
done
}

complete() {
#
# Expiry: Multi threaded with variable thread counts, with eviction and expiry
#
false && {
benchmarks="ZipfianSequenceLoadingBenchmark";
for impl in $COMPLETE; do
  for benchmark in $benchmarks; do
    for threads in 4; do
      runid="${impl}-${benchmark}WithExpiry-${threads}";
      fn="$TARGET/result-$runid";
      echo;
      echo "## $runid";
      sync
      limitCores $threads $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS_LONG $STANDARD_PROFILER  $EXTRA_PROFILER \
           -t $threads -p cacheFactory=org.cache2k.benchmark.$impl -p expiry=true -p factor=1,5 \
           -rf json -rff "$fn.json" \
           2>&1 | tee $fn.out | filterProgress
      if test -n "$dry"; then
        cat $fn.out;
      else
        echo "=> $fn.out";
      fi
    done
  done
done
}


#
# Multi threaded with variable thread counts, with eviction
#
# benchmarks="NeverHitBenchmark MultiRandomAccessBenchmark GeneratedRandomSequenceBenchmark ZipfianLoadingSequenceBenchmark";
# benchmarks="RandomSequenceBenchmark ZipfianSequenceLoadingBenchmark ZipfianLoopingSequenceLoadingBenchmark";
# benchmarks="ZipfianLoopingPrecomputedSequenceLoadingBenchmark ZipfianHoppingPrecomputedSequenceLoadingBenchmark";
# benchmarks="RandomSequenceBenchmark";
benchmarks="ZipfianSequenceLoadingBenchmark RandomSequenceBenchmark";
(
  echo "$allImpls"
  # echo "$maps"
) | while read factory; do
  for benchmark in $benchmarks; do
    for threads in 1 2 4; do
      # remove -p for a nice file name
      impl="`echo "$factory" |sed "s/^\-p //g" | sed "s/ -p /:/g" `"
      runid="$impl-$benchmark-$threads";
      fn="$TARGET/result-$runid";
      echo;
      echo "## $runid";
      sync
      limitCores $threads $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS $STANDARD_PROFILER  $EXTRA_PROFILER \
           -t $threads $factory \
           -rf json -rff "$fn.json" \
           2>&1 | tee $fn.out | filterProgress
      if test -n "$dry"; then
        cat $fn.out;
      else
        echo "=> $fn.out";
      fi
    done
  done
done

#
# Multi threaded asymmetrical/fixed thread counts, no eviction needed, use always 4 cores
#
#benchmarks="CombinedReadWriteBenchmark";
benchmarks="";
for impl in $NO_EVICTION $COMPLETE; do
  for benchmark in $benchmarks; do
    runid="$impl-$benchmark";
    fn="$TARGET/result-$runid";
    echo;
    echo "## $runid";
    sync
    limitCores 4 $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS $STANDARD_PROFILER $EXTRA_PROFILER \
         -p cacheFactory=org.cache2k.benchmark.$impl \
         -rf json -rff "$fn.json" \
         2>&1 | tee $fn.out | filterProgress
    if test -n "$dry"; then
      cat $fn.out;
    else
      echo "=> $fn.out";
    fi
  done
done
}

# complete;

# stopTimer;

processCommandLine "$@";

