#!/bin/bash

# processJmhResults.sh

# Copyright 2013-2016 headissue GmbH, Jens Wilke

# This script processes the benchmark results into svg graphics. Needed tools: gnuplot and jq.
# 
# Run it with the following subcommands: 
#
# process   paint nice diagrams

set -e
# set -x

RESULT="target/jmh-result";
SITE="../cache2k/src/site/resources/benchmark-result";

# replace class names by short name of each cache implementation for graph labeling
cacheShortNames() {
local script=`cat << EOF
s/org.cache2k.benchmark.thirdparty.EhCache2Factory/EhCache2/
s/org.cache2k.benchmark.thirdparty.EhCache3Factory/EhCache3/
s/org.cache2k.benchmark.thirdparty.CaffeineCacheFactory/Caffeine/
s/org.cache2k.benchmark.thirdparty.GuavaCacheFactory/Guava/
s/org.cache2k.benchmark.Cache2kFactory/cache2k/
s/org.cache2k.benchmark.thirdparty.TCache1Factory/tCache/
EOF
`
sed "$script";
}

CACHE_FACTORY_LIST="org.cache2k.benchmark.Cache2kFactory \
org.cache2k.benchmark.thirdparty.CaffeineCacheFactory \
org.cache2k.benchmark.thirdparty.GuavaCacheFactory \
org.cache2k.benchmark.thirdparty.EhCache2Factory";

# "org.cache2k.benchmark.thirdparty.EhCache3Factory";

# for TCache we need to add:
# "org.cache2k.benchmark.thirdparty.TCache1Factory";
          
CACHE_LABEL_LIST=`echo $CACHE_FACTORY_LIST | cacheShortNames`;

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

scaleBytesToMegaBytes4MemAlloc() {
awk -F, '{ printf ("%s,%s,%.2f,%.2f,%.2f,%.2f,%s\n", $1, $2, $3/1024/1024,$4/1024/1024,$5/1024/1024,$6/1024/1024,$7); }'
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
echo "$out ....";
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
# http://stackoverflow.com/questions/15549830/how-to-get-gnuplot-to-use-a-centered-multi-line-title-with-left-aligned-lines
# the title is always centered over the graph, excluding the legend!
# echo "set title '$title'";
echo "set title \"$title\"";

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


# example:
# 1-20,org.cache2k.benchmark.Cache2kFactory,0.00,65.72,993.08,614.8539540861144
# 1-20,org.cache2k.benchmark.thirdparty.CaffeineCacheFactory,0.00,71.27,1000.42,852.382233323991
# 1-20,org.cache2k.benchmark.thirdparty.EhCache2Factory,0.00,58.28,606.75,204.83546236165807
extractMemoryThreadsHitRate() {
local query=`cat << EOF
.[] |  select (.benchmark | contains ("$1") ) |
  [ (.threads | tostring) + "-" + .params.hitRate, .params.cacheFactory,
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"]["score"],
    .["secondaryMetrics"]["+forced-gc-mem.used.after"]["score"],
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"]["score"],
    .["secondaryMetrics"]["+forced-gc-mem.total"]["score"],
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"]["score"]
  ] | @csv
EOF
`
json | \
    jq -r "$query" | \
    sort | tr -d '"' | scaleBytesToMegaBytes4MemAlloc
}

plotMemoryGraphs() {
read title;
read benchmark;
while read key description; do
  f=$RESULT/${benchmark}Memory$key.dat
  (
    echo "threads usedHeap/settled usedHeap/fin usedHeap/max() totalHeap allocRate(MB/s)";
    extractMemoryThreadsHitRate $benchmark | grep "^$key" | sed 's/^[^,]*,\(.*\)/\1/' | cacheShortNames | tr , " "
  ) > $f
  plot $f "$title\n$description" "MB" "cache"
done
}

plotEffectiveHitrate() {
name="$1";
f=$RESULT/${name}EffectiveHitrate.dat
(
echo "threads $CACHE_LABEL_LIST";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"${name}\") ) | [ (.threads | tostring) + \"-\" + .params.hitRate, .params.cacheFactory, .[\"secondaryMetrics\"][\"+misc.hitCount\"][\"score\"] * 100 / .[\"secondaryMetrics\"][\"+misc.opCount\"][\"score\"]] | @csv"  | \
    sort | tr -d '"' | \
    pivot $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "${name} / Effective Hit Rate" "effective hitrate" "threads - hit rate"
}

plotMemUsed() {
name="$1";
f=$RESULT/${name}MemUsed.dat
(
echo "threads $CACHE_LABEL_LIST";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"${name}\") ) | [ (.threads | tostring) + \"-\" + .params.hitRate, .params.cacheFactory, .[\"secondaryMetrics\"][\"+forced-gc-mem.used.after\"][\"score\"] ] | @csv"  | \
    sort | tr -d '"' | scaleBytesToMegaBytes | \
    pivot $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "${name} / Used Heap Memory" "MB" "threads - hit rate"
}

plotMemUsedSettled() {
name="$1";
f=$RESULT/${name}MemUsedSettled.dat
(
echo "threads $CACHE_LABEL_LIST";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"${name}\") ) | [ (.threads | tostring) + \"-\" + .params.hitRate, .params.cacheFactory, .[\"secondaryMetrics\"][\"+forced-gc-mem.used.settled\"][\"score\"] ] | @csv"  | \
    sort | tr -d '"' | scaleBytesToMegaBytes | \
    pivot $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "${name} / Used Heap Memory" "MB" "threads - hit rate"
}

plotOps() {
name="$1";
f=$RESULT/${name}.dat
(
echo "threads $CACHE_LABEL_LIST";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"${name}\") ) | [ (.threads | tostring) + \"-\" + .params.hitRate, .params.cacheFactory, .primaryMetric.score ] | @csv" | \
    sort | tr -d '"' | \
    pivot  $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "${name} / Throughput" "ops/s"
}

# not yet used
withExpiry() {

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

}

process() {

# merge all results into single json file
result=$RESULT/data.json
# A sequence of the lines "]", "[", "]" will be ignored, there may be an empty json file, if a run fails
# A sequence of the lines "]", "[" will be replaced with ","
cat $RESULT/result-*.json | awk '/^]/ { f=1; g=0; next; } f && /^\[/ { g=1; f=0; next; } g { print "  ,"; g=0; } { print; } END { print "]"; }' > $result


f=$RESULT/populateParallelOnce.dat
(
echo "threads-size CHM $CACHE_LABEL_LIST";
json | \
    jq -r '.[] |  select (.benchmark | contains ("PopulateParallelOnceBenchmark") ) | [ (.threads | tostring) + "-" + .params.size, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
            $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark multiple threads" "runtime in seconds" "threads - cache size (entries)"

for threads in 1 2 4; do
f=$RESULT/populateParallelOnce-$threads.dat
(
echo "size CHM $CACHE_LABEL_LIST";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"PopulateParallelOnceBenchmark\") ) | select (.threads == $threads ) | [ .params.size, .params.cacheFactory, .primaryMetric.score ] | @csv"  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
           $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark $threads threads" "runtime in seconds" "cache size (entries)"
done

f=$RESULT/populateParallelOnce-memory.dat
(
echo "size CHM $CACHE_LABEL_LIST";
json | \
    jq -r ".[] |  select (.benchmark | contains (\"PopulateParallelOnceBenchmark\") ) | select (.threads == 1 ) | [ .params.size, .params.cacheFactory, .[\"secondaryMetrics\"][\"+forced-gc-mem.used.after\"][\"score\"] ] | @csv"  | \
    sort | tr -d '"' | scaleBytesToMegaBytes | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
           $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "PopulateParallelOnceBenchmark heap size after forced GC" "MB" "cache size (entries)"

f=$RESULT/readOnly.dat
(
echo "threads-size CHM $CACHE_LABEL_LIST";
json | \
    jq -r '.[] |  select (.benchmark | contains ("ReadOnlyBenchmark") ) | [ (.threads | tostring) + "-" + .params.hitRate, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "ReadOnlyBenchmark (threads-hitRate)" "ops/s"

f=$RESULT/combinedReadWrite.dat
(
echo "threads-size CHM $CACHE_LABEL_LIST";
json | \
    jq -r '.[] |  select (.benchmark | contains ("CombinedReadWriteBenchmark") ) | [ .benchmark, .params.cacheFactory, .primaryMetric.score ] | @csv'  | \
    sed 's/org.cache2k.benchmark.jmh.suite.noEviction.asymmetrical.CombinedReadWriteBenchmark.//' | \
    sort | tr -d '"' | \
    pivot \
          "org.cache2k.benchmark.ConcurrentHashMapFactory" \
          $CACHE_FACTORY_LIST \
          | sort | \
    stripEmpty
) > $f
plot $f "CombinedReadWrite" "ops/s"



# RandomSequenceCacheBenchmark NeverHitBenchmark RandomAccessLongSequenceBenchmark MultiRandomAccessBenchmark GeneratedRandomSequenceBenchmark
for I in NeverHitBenchmark MultiRandomAccessBenchmark GeneratedRandomSequenceBenchmark; do
  plotOps $I;
  plotMemUsed $I;
  plotMemUsedSettled $I;
  plotEffectiveHitrate $I;
done

(
cat << EOF
GeneratedRandomSequenceBenchmark / Memory
GeneratedRandomSequenceBenchmark
4-80 (at 4 threads, 80% hit rate)
4-50 (at 4 threads, 50% hit rate)
EOF
) | plotMemoryGraphs


if false; then
(
cat << EOF
MultiRandomAccessBenchmark / Memory
MultiRandomAccessBenchmark
4-80 (at 4 threads, 80% hit rate)
4-50 (at 4 threads, 50% hit rate)
EOF
) | plotMemoryGraphs

(
cat << EOF
NeverHitBenchmark / Memory
NeverHitBenchmark
1 (at one thread)
EOF
) | plotMemoryGraphs

fi

}

processCommandLine "$@";
