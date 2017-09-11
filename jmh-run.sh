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
test -n "$BENCHMARK_JVM_ARGS" || BENCHMARK_JVM_ARGS="-server -Xmx10G -XX:BiasedLockingStartupDelay=0 -verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails";
# BENCHMARK_JVM_ARGS="$BENCHMARK_JVM_ARGS -XX:+UseG1GC";

# -wi warmup iterations
# -w warmup time
# -i number of iterations
# -r time
# -f how many time to fork a single benchmark
test -n "$BENCHMARK_QUICK" || BENCHMARK_QUICK="-f 1 -wi 0 -i 1 -r 1s -foe true";

# -f 2 / -i 2 has not enough confidence, there is sometime one outlier
# 2 full warmups otherwise there is big jitter with G1
# 5x30 warumups needed for cache2k 10M performance with CMS
# -gc true: careful with -gc true, this seems to influence the measures performance significantly
test -n "$BENCHMARK_DILIGENT" || BENCHMARK_DILIGENT="-f 2 -wi 5 -w 30s -i 3 -r 30s";
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
test -n "$BENCHMARK_PERFASM" || BENCHMARK_PERFASM="-f 1 -wi 1 -w 10s -i 1 -r 30s -prof perfasm";

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

unset cache2k;
unset no3pty;
unset dry;
unset backends;

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
      --quick) OPTIONS="$BENCHMARK_QUICK";;
      --perfasm) OPTIONS="$BENCHMARK_PERFASM";;
      --perfnorm) EXTRA_PROFILER=$EXTRA_PROFILER" $PERF_NORM_OPTIONS";;
      --no3pty) no3pty=true;;
      --cache2k) cache2k=true;;
      --backends) backends="$2"; shift; ;;
      --dry) dry=true;;
      -*) echo "unknown option: $1"; usage; exit 1;;
      *) break;;
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

processCommandLine "$@";

if test -z "$JAVA_HOME"; then
  echo "JAVA_HOME needs to be set" 1>&2
  exit 1;
fi

java=$JAVA_HOME/bin/java
if test -n "$dry"; then
  java="dryEcho";
fi

JAR="jmh-suite/target/benchmarks.jar";

unset SINGLE_THREADED;
unset NO_EVICTION;

# add tests of JDK internal stuff (useful as base line reference), if not cache22k only is requested.
if test -z "$cache2k"; then

  # Implementations with no eviction (actually not a cache) and not thread safe
  SINGLE_THREADED="HashMapFactory"

  # Implementations with no eviction (actually not a cache) and thread safe
  NO_EVICTION="ConcurrentHashMapFactory"

fi

# Implementations with complete caching features
COMPLETE="Cache2kFactory"

TARGET="/run/shm/jmh-result";
test -d $TARGET || mkdir -p $TARGET;

if test -n "$backends"; then
  COMPLETE="$backends";
elif test -z "$no3pty"; then
  COMPLETE="$COMPLETE thirdparty.CaffeineCacheFactory thirdparty.GuavaCacheFactory thirdparty.EhCache2Factory thirdparty.TCache1Factory";
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
# Multi threaded with variable thread counts, no eviction needed
#
# benchmarks="PopulateParallelOnceBenchmark ReadOnlyBenchmark";
benchmarks="";
for impl in $NO_EVICTION $COMPLETE; do
  for benchmark in $benchmarks; do
    for threads in 1 2 4; do
      runid="$impl-$benchmark-$threads";
      fn="$TARGET/result-$runid";
      echo;
      echo "## $runid";
      sync
      limitCores $threads $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS $STANDARD_PROFILER $EXTRA_PROFILER \
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

#
# Multi threaded with variable thread counts, with eviction
#
# benchmarks="NeverHitBenchmark MultiRandomAccessBenchmark GeneratedRandomSequenceBenchmark ZipfianLoadingSequenceBenchmark";
# benchmarks="RandomSequenceBenchmark ZipfianSequenceLoadingBenchmark ZipfianLoopingSequenceLoadingBenchmark";
# benchmarks="ZipfianLoopingPrecomputedSequenceLoadingBenchmark ZipfianHoppingPrecomputedSequenceLoadingBenchmark";
# benchmarks="RandomSequenceBenchmark";
benchmarks="ZipfianSequenceLoadingBenchmark RandomSequenceBenchmark";
for impl in $COMPLETE; do
  for benchmark in $benchmarks; do
    for threads in 1 2 4; do
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
    limitCores 4 $java -jar $JAR \\.$benchmark -jvmArgs "$BENCHMARK_JVM_ARGS" $OPTIONS $STANDARD_PROFILER  $EXTRA_PROFILER \
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

stopTimer;
