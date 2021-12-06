#!/bin/bash

# jmh-run.sh

# Copyright 2016 - 2021 headissue GmbH, Jens Wilke

# This script to run a benchmark suite with JMH.

# stop after error
set -e;

# switch command echo on for debugging
# set -x;

test -n "$BENCHMARK_THREADS" || {
BENCHMARK_THREADS="2 4 8";
CPU_COUNT=`cat /proc/cpuinfo | grep "^processor" | wc -l`;
if [ $CPU_COUNT -gt 31 ]; then
  BENCHMARK_THREADS="4 8 16 32";
fi
}
echo "Machine CPU count: $CPU_COUNT, selecting thread configuration: "$BENCHMARK_THREADS;

test -n "$BENCHMARK_IMPLS" || BENCHMARK_IMPLS="caffeine ehcache3 cache2k"

# http://mechanical-sympathy.blogspot.de/2011/11/biased-locking-osr-and-benchmarking-fun.html
# http://www.oracle.com/technetwork/tutorials/tutorials-1876574.html
# test -n "$BENCHMARK_JVM_ARGS" || BENCHMARK_JVM_ARGS="-server -Xmx2G -XX:+UseG1GC";

# biased locking delay is 4000 by default, enable from the start to minimize effects on the first benchmark iteration
# (check with: ava  -XX:+UnlockDiagnosticVMOptions -XX:+PrintFlagsFinal 2>/dev/null | grep BiasedLockingStartupDelay)
# -Xmx10G: we don't limit the heap, so Java is taking plenty from the OS
test -n "$BENCHMARK_JVM_ARGS" || BENCHMARK_JVM_ARGS="-server -XX:BiasedLockingStartupDelay=0";
# -verbose:gc
# extra G1 args
# BENCHMARK_JVM_ARGS="$BENCHMARK_JVM_ARGS -XX:+UseG1GC -XX:-G1UseAdaptiveConcRefinement -XX:G1ConcRefinementGreenZone=2G -XX:G1ConcRefinementThreads=0";

# -wi warmup iterations
# -w warmup time
# -i number of iterations
# -r time
# -f how many time to fork a single benchmark

# only test whether everything is running through
test -n "$BENCHMARK_QUICK" || BENCHMARK_QUICK="-f 1 -wi 1 -w 1s -i 1 -r 1s -foe true";

# have fast but at least three iterations to detect outliers
test -n "$BENCHMARK_NORMAL" || BENCHMARK_NORMAL="-f 1 -wi 2 -w 5s -i 3 -r 5s";
# test -n "$BENCHMARK_NORMAL" || BENCHMARK_NORMAL="-f 1 -wi 2 -w 2s -i 3 -r 2s";

# -f 2 / -i 2 has not enough confidence, there is sometimes one outlier
# 2 full warmups otherwise there is big jitter with G1
# -gc true: careful with -gc true, this seems to influence the measures performance significantly
test -n "$BENCHMARK_DILIGENT" || BENCHMARK_DILIGENT="-f 3 -wi 2 -w 10s -i 2 -r 10s";

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


# How to use -perf perf and -perf perfasm profiler with Ubuntu: Provide perf support and
# disassembler

# apt-get install perf linux-tools-generic libhsdis0-fcml
#
# When using the hwe kernel, e.g.:
#
# apt-get install linux-tools-generic-hwe-20.04

# Old information:
# Tinker benchmark options to do profiling and add assembler code output (linux only).
# Needs additional disassembly library to display assembler code
# see: http://psy-lob-saw.blogspot.de/2013/01/java-print-assembly.html
# and, see: https://wiki.openjdk.java.net/display/HotSpot/PrintAssembly
# download from: https://kenai.com/projects/base-hsdis/downloads
# install with e.g.: mv ~/Downloads/linux-hsdis-amd64.so jdk1.8.0_45/jre/lib/amd64/hsdis-amd64.so.
# For profiling only do one fork, but more measurement iterations
# profilers are described here: http://java-performance.info/introduction-jmh-profilers
# hsdis is available as Ubuntu package: sudo apt-get install libhsdis0-fcml
test -n "$BENCHMARK_PERFASM" || BENCHMARK_PERFASM="-f 1 -wi 1 -w 10s -i 1 -r 20s -prof perfasm:hotThreshold=0.05";
# longer test run for expiry tests
test -n "$BENCHMARK_PERFASM_LONG" || BENCHMARK_PERFASM_LONG="-f 1 -wi 1 -w 180s -i 1 -r 180s -prof perfasm:hotThreshold=0.05";

# hs_gc: detailed counters from the GC implementation
STANDARD_PROFILER="-prof comp -prof gc";

# STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.ForcedGcMemoryProfiler";
STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.LinuxVmProfiler";
STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.MiscResultRecorderProfiler";
STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.GcProfiler";
STANDARD_PROFILER="$STANDARD_PROFILER -prof org.cache2k.benchmark.jmh.HeapProfiler";

EXTRA_PROFILER="";

EXTRA_PARAMETERS="";

# not used yet
PERF_NORM_OPTIONS="-prof perfnorm:useDefaultStat=true"

OPTIONS="$BENCHMARK_NORMAL";
OPTIONS_LONG="$BENCHMARK_NORMAL_LONG";

if test -z "$JAVA_HOME"; then
  echo "JAVA_HOME needs to be set" 1>&2
  exit 1;
fi

java=$JAVA_HOME/bin/java

unset dry;
unset quick;

usage() {
  echo "Usage: $0 [ options ] complete"
  echo "--quick          Run a smoke test only, not the full benchmark"
  echo "--diligent       Run benchmark with longer iteration time"
  echo "--dry            Log the command lines to execute, but do not run test"
  echo "--perfasm"
  echo "--perfnorm"
  echo "--impls <impls>  Test only the given cache implementations, default: $BENCHMARK_IMPLS"
  echo ""
  echo "Command:"
  echo ""
  echo "complete         runs the benchmark suite"
}

processCommandLine() {
  while true; do
    case "$1" in
      --quick) quick=true;
               EXTRA_PARAMETERS="-p entryCount=100000 -p percent=110"
               BENCHMARK_THREADS="4";
               OPTIONS="$BENCHMARK_QUICK";
               OPTIONS_LONG="$BENCHMARK_QUICK";;
      --diligent) OPTIONS="$BENCHMARK_DILIGENT";
                   OPTIONS_LONG="$BENCHMARK_DILIGENT_LONG";;
      --perfasm) OPTIONS="$BENCHMARK_PERFASM";
                 OPTIONS_LONG="$BENCHMARK_PERFASM_LONG";;
      --perfnorm) EXTRA_PROFILER=$EXTRA_PROFILER" $PERF_NORM_OPTIONS";;
      --impls) BENCHMARK_IMPLS="$2"; shift; ;;
      --dry) dry=true;
             java="dryEcho";;
      --echo|--debug) set -x;;
      -*) echo "unknown option: $1"; usage; exit 1;;
      *) if test -z "$1"; then
            usage; exit 1;
          fi
        "$1";
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
echo "Finished at: `date`"
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
test -f $JAR || JAR="benchmarks.jar";


TARGET="$HOME/jmh-result";
test -d $TARGET || mkdir -p $TARGET;

startTimer;

# we use taskset for limiting cores, that works properly with newer JDKs
# in previous benchmarks we used OS CPU hotplugging, which requires root.
limitCores() {
if test -n "$dry"; then
  shift;
  "$@";
  return;
fi
local cnt=$1;
shift;
taskset -c 0-$(( $cnt - 1)) "$@";
}

implementations="`cat - << "EOF"
cache2k -p cacheFactory=org.cache2k.benchmark.cache.Cache2kFactory
cache2kj -p cacheFactory=org.cache2k.benchmark.JCacheFactory -p cacheProvider=org.cache2k.jcache.provider.JCacheProvider
cache2kw -p cacheFactory=org.cache2k.benchmark.Cache2kWiredFactory
caffeine -p cacheFactory=org.cache2k.benchmark.cache.CaffeineCacheFactory
ehcache3 -p cacheFactory=org.cache2k.benchmark.cache.EhCache3Factory
chm -p cacheFactory=org.cache2k.benchmark.ConcurrentHashMapFactory
slhm -p cacheFactory=org.cache2k.benchmark.SynchronizedLinkedHashMapFactory
plhm -p cacheFactory=org.cache2k.benchmark.PartitionedLinkedHashMapFactory
guava -p cacheFactory=org.cache2k.benchmark.thirdparty.GuavaCacheFactory
chm -p cacheFactory=org.cache2k.benchmark.ConcurrentHashMapFactory
slhm -p cacheFactory=org.cache2k.benchmark.SynchronizedLinkedHashMapFactory
plhm -p cacheFactory=org.cache2k.benchmark.PartitionedLinkedHashMapFactory
EOF
`"

benchmark() {
local impl="$1";
local benchmark="$2";
local threads="$3";
local param="$4";
factory="`echo "$implementations" | awk "/^$impl / { print substr(\\$0, length(\\$1) + 2); }"`"
runid="$impl-$benchmark-$threads";
fn="$TARGET/result-$runid";
echo;
echo "## $runid";
sync
limitCores $threads $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS $STANDARD_PROFILER  $EXTRA_PROFILER \
     $EXTRA_PARAMETERS -t $threads -p shortName=$impl $param $factory \
     -rf json -rff "$fn.json" \
     2>&1 | tee $fn.out | filterProgress
if test -n "$dry"; then
  cat $fn.out;
else
  echo "=> $fn.out";
fi
}


complete() {

# benchmarks we still monitor, but do not run through all thread variations
reducedBenchmarks="ZipfianSequenceBulkLoadingBenchmark IterationBenchmark"

# current benchmarks with detailed output
benchmarks="ZipfianSequenceLoadingBenchmark PopulateParallelClearBenchmark PopulateParallelOnceBenchmark PopulateParallelTwiceBenchmark";

# reducedBenchmarks=""
# benchmarks="IterationBenchmark"
echo $BENCHMARK_IMPLS;
for impl in $BENCHMARK_IMPLS; do
  for benchmark in $benchmarks; do
    for threads in $BENCHMARK_THREADS; do
      benchmark $impl $benchmark $threads;
    done
  done
done
# run this set of benchmarks with less threads
for impl in $BENCHMARK_IMPLS; do
  for benchmark in $reducedBenchmarks; do
    for threads in 8; do
      benchmark $impl $benchmark $threads;
    done
  done
done
for impl in $BENCHMARK_IMPLS; do
  benchmark $impl ZipfianSequenceLoadingBenchmark 4 "-p tti=true -p percent=110"
done
stopTimer;
}

processCommandLine "$@";
