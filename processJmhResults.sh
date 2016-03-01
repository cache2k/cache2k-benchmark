#!/bin/bash

# processJmhResults.sh

# Copyright 2013-2016 headissue GmbH, Jens Wilke

# This script processes the benchmark results into svg graphics. Needed tools: gnuplot and jq.
# 
# Run it with the following subcommands: 
#
# process   paint nice diagrams

RESULT="target/jmh-result";
SITE="../cache2k/src/site/resources/benchmark-result";

processCommandLine() {
  pars="$#";
  while true; do
    case "$1" in
      --dir) RESULT="$2"; shift 1;;
      -*) echo "unknown option: $1"; exit 1;;
      *) break;;
    esac
    shift 1;
  done
  if test -z "$1"; then
    echo "Run with: processBenchmarkResults.sh copyData | process | copyToSite";
  else
   "$@";
  fi
}

json() {
cat $RESULT/data.json;
}

# pivot "<impl>,<impl2>,..."
#
# Input format:
#
# benchmarkMostlyHit_6E|org.cache2k.benchmark.LruCacheBenchmark|0.596
# benchmarkMostlyHit_6E|org.cache2k.benchmark.LruSt030709CacheBenchmark|0.248
# benchmarkMostlyHit_6E|org.cache2k.benchmark.RecentDefaultCacheBenchmark|0.573
# benchmarkRandom_6E|org.cache2k.benchmark.ArcCacheBenchmark|0.169
# benchmarkRandom_6E|org.cache2k.benchmark.ClockCacheBenchmark|0.138
# . . .

# Output format:
#
# benchmarkAllMiss_6E 0.207 0.403 
# benchmarkEffective90_6E 0.047 0.074 
# benchmarkEffective95_6E 0.04 0.059 
#

pivot() {
local cols="$1";
shift;
while [ "$1" != "" ]; do
  cols="${cols},$1";
  shift;
done
awk -v cols="$cols" "$pivot_awk";
}

pivot_awk=`cat <<"EOF"
BEGIN { FS=",";
  keysCnt = split(cols, colKeys, ",");
}
  
  row!=$1 { flushRow(); row=$1; for (i in data) delete data[i]; }
  { data[$2]=$3; }

END { flushRow(); }

function flushRow() {
 if (row == "") return;
 printf ("%s ", row);
 for (k = 1; k <= keysCnt; k++) {
   key=colKeys[k];
   printf ("%s ", data[key]);
 }
 printf "\n";
}
EOF
`

# renameBenchmarks
#
# Strip benchmark from the name.
#
cleanName() {
  awk '{ sub(/benchmark/, "", $1); print; }';
}

copyData() {
test -d $RESULT || mkdir -p $RESULT;
cp benchmark.log $RESULT/;
cp thirdparty/target/junit-benchmark.xml $RESULT/thirdparty-junit-benchmark.xml;
cp zoo/target/junit-benchmark.xml $RESULT/zoo-junit-benchmark.xml;
cp thirdparty/target/cache2k-benchmark-result.csv $RESULT/thirdparty-cache2k-benchmark-result.csv;
cp zoo/target/cache2k-benchmark-result.csv $RESULT/zoo-cache2k-benchmark-result.csv;
}

copyToSite() {
test -d "$SITE" || mkdir -p "$SITE";
cp "$RESULT"/* "$SITE"/;
}

# print a csv with the junit benchmark results. old version.
# the xml contains only the data from the last JVM fork.
printJubCsvOld() {
for I in $RESULT/*-junit-benchmark.xml; do
  xml2 < $I | 2csv -d"|" testname @name @classname @round-avg @round-stddev @benchmark-rounds
done
}

extract_jub_csv_from_log_awk=`cat <<"EOF"
/^Running / { class=$2; }
/measured [0-9]* out of/ { rounds=$3; split($1, A, ":"); name=A[1]; }
/ round: / {
  n=split(name, A, ".");
  method=A[n];
  roundavg=$2;
  split($4, A, "]");
  stddev=A[1];
  print method"|"class"|"roundavg"|"stddev"|"rounds;
}
EOF
`

stripEmpty() {
awk 'NF > 1 { print; }';
}

maxYRange_awk=`cat <<"EOF"
NR==1 { next; } 
{ for (i=2; i<=NF;i++) if ($i>range) range=$i; } 
END { 
  range=range*1.1; 
#   if (range > 100) { print 100; } else { print range; }
}
EOF
`

maxYRange() {
awk "$maxYRange_awk";
}

plot() {
local in="$1";
local out="`dirname "$in"`/`basename "$in" .dat`.svg";
local title="$2";
local yTitle="$3";
local xTitle="$4";
local maxYRange=`maxYRange < "$in"`;
(
echo "set terminal svg"
echo "set output '$out'"
echo "set boxwidth 0.9 absolute";
echo "set style fill solid 1.00 border lt -1";
echo "set key outside right top vertical Right noreverse noenhanced autotitles nobox";
echo "set style histogram clustered gap 2 title  offset character 0, 0, 0";
echo "set datafile missing '-'";
echo "set style data histograms";
echo "set xtics border in scale 0,0 nomirror rotate by -45  offset character 0, 0, 0 autojustify";
echo 'set xtics  norangelimit font "1"';
echo "set xtics   ()"
test -z "$xTitle" || echo "set xlabel '${xTitle}'";
test -z "$yTitle" || echo "set ylabel '${yTitle}'";
echo "set title '$title'";
echo "set yrange [ 0.0 : $maxYRange ] noreverse nowriteback";
# echo "i = 22";
# echo "plot '$in' using 2:xtic(1) ti col, '' u 3 ti col, '' u 4 ti col";
#if [ "`cat $in | wc -l`" -lt 3 ]; then
#  echo  -n "plot '$in' using 2 ti col";
#else
  echo  -n "plot '$in' using 2:xtic(1) ti col";
#fi
cols=$(( `head -n 1 "$in" | wc -w` ));
  idx=3;
  while [ $idx -le $cols ]; do
    echo -n ", '' u $idx ti col";
    idx=$(( $idx + 1 ));
  done
  echo ""; 
) > "$in".plot
gnuplot "$in".plot;
}

process() {
f=$RESULT/populateParallelOnceCache2k.dat
(
echo "threads-size cache2k cache2k+expiry ConcurrentHashMap";
json | \
    jq -r '.[] |  select (.benchmark | contains ("PopulateParallelOnceBenchmark") ) | [ (.threads | tostring) + "-" + .params.size, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.Cache2kWithExpiryFactory" \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark: Insert entries once with variable threads (threads-entryCount)" "runtime in seconds"

f=$RESULT/populateParallelOnce.dat
(
echo "threads-size CHM cache2k Caffeine Guava";
json | \
    jq -r '.[] |  select (.benchmark | contains ("PopulateParallelOnceBenchmark") ) | [ (.threads | tostring) + "-" + .params.size, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark: Insert entries once with variable threads (threads-entryCount)" "runtime in seconds"

for threads in 1 2 4; do
f=$RESULT/populateParallelOnce-$threads.dat
(
echo "threads-size CHM cache2k Caffeine Guava";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"PopulateParallelOnceBenchmark\") ) | select (.threads == $threads ) | [ .params.size, .params.cacheFactory, .primaryMetric.score ] | @csv"  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark: Insert entries once $threads threads (Cache size)" "runtime in seconds"
done

f=$RESULT/populateParallelOnce-memory.dat
(
echo "threads-size CHM cache2k Caffeine Guava";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"PopulateParallelOnceBenchmark\") ) | select (.threads == 1 ) | [ .params.size, .params.cacheFactory, .secondaryMetric.\+forced-gc-mem.total.score ] | @csv"  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark: Insert entries once (Cache size)" "used memory after forces GC"

f=$RESULT/readOnly.dat
(
echo "threads-size CHM cache2k Caffeine Guava";
json | \
    jq -r '.[] |  select (.benchmark | contains ("ReadOnlyBenchmark") ) | [ (.threads | tostring) + "-" + .params.hitRate, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "Random reads in 100k entries with variable hit rate (threads-hitRate)" "ops/s"

f=$RESULT/combinedReadWrite.dat
(
echo "threads-size CHM cache2k Caffeine Guava";
json | \
    jq -r '.[] |  select (.benchmark | contains ("CombinedReadWriteBenchmark") ) | [ .benchmark, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sed 's/org.cache2k.benchmark.jmh.suite.noEviction.asymmetrical.CombinedReadWriteBenchmark.//' | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "CombinedReadWrite" "ops/s"

}

processCommandLine "$@";
shift $shift;
