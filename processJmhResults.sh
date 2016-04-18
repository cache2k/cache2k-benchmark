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
    echo "Run with: processJmhResults.sh process";
  else
   "$@";
  fi
}

json() {
if test -f  $RESULT/data.json.gz; then
  cat $RESULT/data.json.gz;
else
  cat $RESULT/data.json
fi
}

# scaleToMegaBytes
#
# Scale the score down to megabytes with 2 digit precision.
#
scaleBytesToMegaBytes() {
awk -F, '{ printf ("%s,%s,%.2f\n", $1, $2, $3/1024/1024); }'
}

# pivot "<impl>,<impl2>,..."
#
# Input format:
#
# 1000000,org.cache2k.benchmark.Cache2kFactory,0.26509106983333336
# 2000000,org.cache2k.benchmark.Cache2kFactory,0.51293869775
# 4000000,org.cache2k.benchmark.Cache2kFactory,1.5091185168999999
# 8000000,org.cache2k.benchmark.Cache2kFactory,5.391231625266667
# 1000000,org.cache2k.benchmark.Cache2kWithExpiryFactory,0.39409388270000006
# 2000000,org.cache2k.benchmark.Cache2kWithExpiryFactory,0.9784698428333333
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
   v=data[key];
   # missing data points need to have a ?
   if (v=="") {
     printf ("? ");
   } else {
     printf ("%s ", data[key]);
   }
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

# just a copy of above but leaving out the title for texts/blogs providing an image title
local out="`dirname "$in"`/`basename "$in" .dat`-notitle.svg";
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
) > "${in}-notitle.plot"
gnuplot "${in}-notitle.plot";
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
plot $f "PopulateParallelOnceBenchmark multiple threads" "runtime in seconds" "threads - cache size (entries)"

f=$RESULT/populateParallelOnce.dat
(
echo "threads-size CHM cache2k Caffeine Guava EHCache2";
json | \
    jq -r '.[] |  select (.benchmark | contains ("PopulateParallelOnceBenchmark") ) | [ (.threads | tostring) + "-" + .params.size, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark multiple threads" "runtime in seconds" "threads - cache size (entries)"

for threads in 1 2 4; do
f=$RESULT/populateParallelOnce-$threads.dat
(
echo "size CHM cache2k Caffeine Guava EHCache2";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"PopulateParallelOnceBenchmark\") ) | select (.threads == $threads ) | [ .params.size, .params.cacheFactory, .primaryMetric.score ] | @csv"  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark $threads threads" "runtime in seconds" "cache size (entries)"
done

f=$RESULT/populateParallelOnce-memory.dat
(
echo "size CHM cache2k Caffeine Guava EhCache2";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"PopulateParallelOnceBenchmark\") ) | select (.threads == 1 ) | [ .params.size, .params.cacheFactory, .[\"secondaryMetrics\"][\"+forced-gc-mem.used\"][\"score\"] ] | @csv"  | \
    sort | tr -d '"' | scaleBytesToMegaBytes | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark heap size after forced GC" "MB" "cache size (entries)"

f=$RESULT/readOnly.dat
(
echo "threads-size CHM cache2k Caffeine Guava EhCache2";
json | \
    jq -r '.[] |  select (.benchmark | contains ("ReadOnlyBenchmark") ) | [ (.threads | tostring) + "-" + .params.hitRate, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "ReadOnlyBenchmark (threads-hitRate)" "ops/s"

f=$RESULT/combinedReadWrite.dat
(
echo "threads-size CHM cache2k Caffeine Guava EhCache2";
json | \
    jq -r '.[] |  select (.benchmark | contains ("CombinedReadWriteBenchmark") ) | [ .benchmark, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sed 's/org.cache2k.benchmark.jmh.suite.noEviction.asymmetrical.CombinedReadWriteBenchmark.//' | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "CombinedReadWrite" "ops/s"

f=$RESULT/neverHit.dat
(
echo "threads cache2k Caffeine Guava EhCache2";
json | \
    jq -r '.[] |  select (.benchmark | contains ("NeverHitBenchmark") ) | [ (.threads | tostring), .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "NeverHitBenchmark (threads)" "ops/s"

f=$RESULT/neverHitAfterGc.dat
(
echo "threads cache2k Caffeine Guava EhCache2";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"NeverHitBenchmark\") ) | [ (.threads | tostring), .params.cacheFactory, .[\"secondaryMetrics\"][\"+forced-gc-mem.used\"][\"score\"] ] | @csv"  | \
    sort | tr -d '"' | scaleBytesToMegaBytes | \
    pivot \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "NeverHitBenchmark (threads)" "MB" "threads"

f=$RESULT/RandomSequenceCacheBenchmark.dat
(
echo "threads cache2k Caffeine Guava EhCache2";
json | \
    jq -r '.[] |  select (.benchmark | contains ("RandomSequenceCacheBenchmark") ) | [ (.threads | tostring), .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "RandomSequenceCacheBenchmark (threads)" "ops/s"

f=$RESULT/RandomSequenceCacheBenchmarkGc.dat
(
echo "threads cache2k Caffeine Guava EhCache2";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"RandomSequenceCacheBenchmark\") ) | [ (.threads | tostring), .params.cacheFactory, .[\"secondaryMetrics\"][\"+forced-gc-mem.used\"][\"score\"] ] | @csv"  | \
    sort | tr -d '"' | scaleBytesToMegaBytes | \
    pivot \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "RandomSequenceCacheBenchmark (threads)" "MB" "threads"

f=$RESULT/RandomSequenceCacheBenchmarkThreadsHitrate.dat
(
echo "threads cache2k Caffeine Guava EhCache2";
json | \
    jq -r '.[] |  select (.benchmark | contains ("RandomSequenceCacheBenchmark") ) | [ (.threads | tostring) + "-" + .params.hitRate, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "RandomSequenceCacheBenchmark (threads-hitRate)" "ops/s"

f=$RESULT/RandomSequenceCacheBenchmarkThreadsHitrate2Hitrate.dat
(
echo "threads cache2k Caffeine Guava EhCache2";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"RandomSequenceCacheBenchmark\") ) | [ (.threads | tostring) + \"-\" + .params.hitRate, .params.cacheFactory, .[\"secondaryMetrics\"][\"+misc.hitCount\"][\"score\"] * 100 / .[\"secondaryMetrics\"][\"+misc.opCount\"][\"score\"]] | @csv"  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.Cache2kFactory" \
          "org.cache2k.benchmark.thirdparty.CaffeineCacheFactory" \
          "org.cache2k.benchmark.thirdparty.GuavaCacheFactory" \
          "org.cache2k.benchmark.thirdparty.EhCacheDirectFactory" \
          | sort | \
    stripEmpty
) > $f
plot $f "RandomSequenceCacheBenchmark (threads-hitRate)" "effective hitrate"


}

processCommandLine "$@";
shift $shift;
