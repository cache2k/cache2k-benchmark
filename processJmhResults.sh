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

# CACHE_FACTORY_LIST=org.cache2k.benchmark.thirdparty.CaffeineCacheFactory

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
awk -F, 'BEGIN { scale=1000*1000; } { printf ("%s,%s,%.2f\n", $1, $2, $3/scale); }'
}

scaleBytesToMegaBytes4MemAlloc() {
awk -F, 'BEGIN { scale=1000*1000; } { printf ("%s,%s,%.2f,%.2f,%.2f,%.2f,%s\n", $1, $2, $3/scale,$4/scale,$5/scale,$6/scale,$7); }'
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

# pivot4

# Instead of pivoting only one score, this pivots 4 values. The score, error, lower and upper confidence.

# Input format:

# Params, Impl, Score, Error, Lower Confidence, Upper Confidence

# "2-80","org.cache2k.benchmark.thirdparty.GuavaCacheFactory",1602500.3473090807,322572.59684567206,1279927.7504634087,1925072.9441547527
# "3-10","org.cache2k.benchmark.thirdparty.GuavaCacheFactory",1934111.3466509539,11853.390790690266,1922257.9558602637,1945964.737441644
#  "3-20","org.cache2k.benchmark.thirdparty.GuavaCacheFactory",1632899.76591479,9903.399664738625,1622996.3662500514,1642803.1655795288

pivot4() {
local cols="$1";
shift;
while [ "$1" != "" ]; do
  cols="${cols},$1";
  shift;
done
awk -v cols="$cols" "$pivot_With_Confidence_awk";
}

pivot_With_Confidence_awk=`cat <<"EOF"
BEGIN { FS=",";
  keysCnt = split(cols, colKeys, ",");
}

  row!=$1 { flushRow(); row=$1; for (i in data) delete data[i]; }
  { data[$2]=$3;
    error[$2]=$4;
    lower[$2]=$5;
    upper[$2]=$6;
  }

END { flushRow(); }

function flushRow() {
 if (row == "") return;
 printf ("%s ", row);
 for (k = 1; k <= keysCnt; k++) {
   key=colKeys[k];
   v=data[key];
   # missing data points need to have a ?
   if (v=="") {
     printf ("0 0 0 0 ");
   } else {
     printf ("%s %s %s %s ", data[key], error[key], lower[key], upper[key]);
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

plotHistogramHeader() {
local in="$1";
local out="`dirname "$in"`/`basename "$in" .dat`.svg";
local title="$2";
local yTitle="$3";
local xTitle="$4";
local maxYRange=`maxYRange < "$in"`;
echo "set terminal svg"
echo "set boxwidth 0.9 absolute";
echo "set style fill solid 1.00 border -1";
echo "set key outside right top vertical Right noreverse noenhanced autotitles nobox";
echo "set datafile missing '-'";
echo "set style data histograms";

echo "set xtics border in scale 0,0 nomirror rotate by -45  offset character 0, 0, 0 autojustify";
echo 'set xtics norangelimit font "1"';
echo "set xtics   ()"

test -z "$xTitle" || echo "set xlabel '${xTitle}'";
test -z "$yTitle" || echo "set ylabel '${yTitle}'";

echo "set yrange [ 0.0 : $maxYRange ] noreverse nowriteback";

echo "set format y '%.1s%c'"

cat - << "EOF"
# nomirror means do not put tics on the opposite side of the plot
set xtics nomirror
set ytics nomirror

# On the Y axis put a major tick every 5
# set ytics 1000000

# Split in 5 for minor tics
set mytics 5

# Line style for axes
# Define a line style (we're calling it 80) and set
# lt = linetype to 0 (dashed line)
# lc = linecolor to a gray defined by that number
# set style line 80 lt 0 lc rgb "#111111"
set style line 80 lt 1 lc rgbcolor "black"

# Set the border using the linestyle 80 that we defined
# 3 = 1 + 2 (1 = plot the bottom line and 2 = plot the left line)
# back means the border should be behind anything else drawn
set border 3 back ls 80

# Line style for grid
# Define a new linestyle (81)
# linetype = 0 (dashed line)
# linecolor = gray
# lw = lineweight, make it half as wide as the axes lines
set style line 82 lt 0 lc rgb "#808080" lw 0.5

set style line 81 lt 19 lc rgb "#404040" lw 0.5

# Draw the grid lines for both the major and minor tics
# set grid xtics
set grid ytics mytics ls 81, ls 82

# Put the grid behind anything drawn and use the linestyle 81
# set grid back ls 81
EOF
}

highContrast() {
# http://colorbrewer2.org/....
cnt=1;
colors="d7191c ffffbf fdae61 abdda4 2b83ba 1b7837"
# colors="aaaabf fdae61 abdda4 2b83ba 1b7837"

for I in $colors; do
  echo "set style line $cnt lt rgb \"#$I\"";
  cnt=$(( $cnt + 1 ));
done
cnt=0;
echo "set palette defined ( \\";
for I in $colors; do
 if [ $cnt -gt 0 ]; then echo ", \\"; fi
 echo -n " $cnt '#$I'"
 cnt=$(( $cnt + 1 ));
done
echo ")";
echo "set palette maxcolors $cnt";
}

plotData() {
echo "set style histogram clustered gap 2 title  offset character 0, 0, 0";
local in="$1";
echo  -n "plot '$in' using 2:xtic(1) ti col ls 1";
cols=$(( `head -n 1 "$in" | wc -w` ));
idx=3;
while [ $idx -le $cols ]; do
  echo -n ", '' u $idx ti col ls $(( $idx - 1 ))";
  idx=$(( $idx + 1 ));
done
}

plotDataWithConfidence() {
echo "set style histogram errorbars gap 2 lw 0.5 title  offset character 0, 0, 0";
local in="$1";
local idx=2;
local ls=1;
echo  -n "plot '$in' using 2:4:5:xtic(1) ti col($idx) ls ${ls}";
cols=$(( `head -n 1 "$in" | wc -w` ));
# TODO: replace with plot for ....
ls=$(( $ls + 1 ));
idx=$(( $idx + 4 ));
while [ $idx -lt $cols ]; do
  echo -n ", '' u $idx:$(( idx + 2)):$(( idx + 3)) ti col($idx) ls ${ls}";
  ls=$(( $ls + 1 ));
  idx=$(( $idx + 4 ));
done
}


plot() {
local plot="plotData";
while true; do
  case "$1" in
    --withConfidence) plot="plotDataWithConfidence";;
    # --dir) RESULT="$2"; shift 1;;
    -*) echo "unknown option: $1"; return 1;;
    *) break;;
  esac
  shift 1;
done
local in="$1";
local out="`dirname "$in"`/`basename "$in" .dat`.svg";
local title="$2";
local yTitle="$3";
local xTitle="$4";
local maxYRange=`maxYRange < "$in"`;

if [ "`cat $in | wc -l`" -eq 1 ]; then
  echo "No data, skipping: $in";
  return;
fi

if false; then
echo "$out ....";
(
plotHistogramHeader "$in" "$title" "$yTitle" "$xTitle";
echo "set output '$out'"
# http://stackoverflow.com/questions/15549830/how-to-get-gnuplot-to-use-a-centered-multi-line-title-with-left-aligned-lines
# the title is always centered over the graph, excluding the legend!
# echo "set title '$title'";
echo "set title \"$title\"";
highContrast;
$plot "$in";
) > "$in".plot
gnuplot "$in".plot;
fi

# just a copy of above but leaving out the title for texts/blogs providing an image title
local out="`dirname "$in"`/`basename "$in" .dat`-notitle.svg";
echo "$out ....";
(
plotHistogramHeader "$in" "$title" "$xTitle" "$yTitle";
echo "set output '$out'"
highContrast;
$plot "$in";
) > "${in}-notitle.plot"
gnuplot "${in}-notitle.plot";

# just a copy of above but leaving out the title for texts/blogs providing an image title
local out="`dirname "$in"`/`basename "$in" .dat`-notitle-print.svg";
echo "$out ....";
(
plotHistogramHeader "$in" "$title" "$yTitle" "$xTitle";
echo "set output '$out'"
echo "set style fill pattern border"
# highContrast;
echo "set colorsequence podo"
$plot "$in";
) > "${in}-notitle-print.plot"
gnuplot "${in}-notitle-print.plot";
}

# example:
# 1-20,org.cache2k.benchmark.Cache2kFactory,0.00,65.72,993.08,614.8539540861144
# 1-20,org.cache2k.benchmark.thirdparty.CaffeineCacheFactory,0.00,71.27,1000.42,852.382233323991
# 1-20,org.cache2k.benchmark.thirdparty.EhCache2Factory,0.00,58.28,606.75,204.83546236165807
extractMemoryThreadsHitRate() {
local query=`cat << EOF
.[] |  select (.benchmark | contains (".$1") ) |
  [ (.threads | tostring) + "-" + .params.entryCount + "-" + .params.$2, .params.cacheFactory,
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"].score,
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"].scoreError,
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"].scoreConfidence[0],
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"].scoreConfidence[1],
    .["secondaryMetrics"]["+forced-gc-mem.used.after"].score,
    .["secondaryMetrics"]["+forced-gc-mem.used.after"].scoreError,
    .["secondaryMetrics"]["+forced-gc-mem.used.after"].scoreConfidence[0],
    .["secondaryMetrics"]["+forced-gc-mem.used.after"].scoreConfidence[1],
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"].score,
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"].scoreError,
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"].scoreConfidence[0],
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"].scoreConfidence[1],
    .["secondaryMetrics"]["+forced-gc-mem.total"].score,
    .["secondaryMetrics"]["+forced-gc-mem.total"].scoreError,
    .["secondaryMetrics"]["+forced-gc-mem.total"].scoreConfidence[0],
    .["secondaryMetrics"]["+forced-gc-mem.total"].scoreConfidence[1],
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].score * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreError * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreConfidence[0] * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreConfidence[1] * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"].score * 1000 * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"].scoreError* 1000 * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"].scoreConfidence[0] * 1000 * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"].scoreConfidence[1] * 1000 * 1000
  ] | @csv
EOF
`
json | \
    jq -r "$query" | \
    sort | tr -d '"'
     # | scaleBytesToMegaBytes4x4MemAlloc
}

stripFirstColumn() {
sed 's/^[^ ]* \(.*\)/\1/'
}

plotMemoryGraphs() {
read param;
read title;
read benchmark;
while read key description; do
  f=$RESULT/${benchmark}Memory$key.dat
  (
    echo "threads usedHeap/settled error lower upper usedHeap/fin error lower upper usedHeap/max() error lower upper totalHeap error lower upper VmHWM error lower upper allocRate(byte/s) error lower upper";
    extractMemoryThreadsHitRate $benchmark $param | tr , " " | shortenParamValues | grep "^$key" | stripFirstColumn | cacheShortNames
  ) > $f
  plot --withConfidence $f "$title\n$description" "cache" "Bytes"
done
}

plotMemoryGraphsSettled() {
read param;
read title;
read benchmark;
while read key description; do
  f=$RESULT/${benchmark}MemorySettled$key.dat
  (
    echo "threads usedHeap/settled error lower upper";
    extractMemoryThreadsHitRate $benchmark $param | tr , " " | shortenParamValues | grep "^$key" | stripFirstColumn | cacheShortNames
  ) > $f
  plot --withConfidence $f "$title\n$description" "cache" "Bytes"
done
}

plotEffectiveHitrate() {
name="$1";
param="$2";
suffix="$3";
filter="$4";
local prods="$CACHE_FACTORY_LIST";
if test -n "$suffix"; then
f=$RESULT/${name}EffectiveHitrate-${suffix}.dat
else
f=$RESULT/${name}EffectiveHitrate.dat
fi
graphName=`basename $f .dat`;
(
header4 "$prods";
# echo "threads $CACHE_LABEL_LIST";
# TODO: we cannot calculate with confidences
# previous version:  100 - .["secondaryMetrics"]["+misc.missCount"].score * 100 / .["secondaryMetrics"]["+misc.opCount"]["score"],
# new:      .["secondaryMetrics"]["+misc.hitrate"].score,
local query=`cat << EOF
.[] |  select (.benchmark | contains (".${name}") ) |
  [ (.threads | tostring) +  "-" + .params.entryCount + "-" + .params.$param,
     .params.cacheFactory,
     .["secondaryMetrics"]["+misc.hitrate"].score,
     .["secondaryMetrics"]["+misc.hitrate"].scoreError,
     .["secondaryMetrics"]["+misc.hitrate"].scoreConfidence[0],
     .["secondaryMetrics"]["+misc.hitrate"].scoreConfidence[1]
  ] | @csv
EOF
`
json | \
    jq -r "$query"  | \
    sort | tr -d '"' | \
    pivot4 $prods | shortenParamValues | sort | grep "$filter" | \
    stripEmpty
) > $f
plot --withConfidence $f "${name} / Effective Hit Rate" "threads - size - $param" "effective hitrate"
}

plotMemUsed() {
name="$1";
param="$2";
f=$RESULT/${name}MemUsed.dat
graphName=`basename $f .dat`;
(
echo "threads $CACHE_LABEL_LIST";
json | \
    jq -r ".[] |  select (.benchmark | contains (\".${name}\") ) | [ (.threads | tostring) + \"-\" + .params.entryCount + \"-\" + .params.$param, .params.cacheFactory, .[\"secondaryMetrics\"][\"+forced-gc-mem.used.after\"][\"score\"] ] | @csv"  | \
    sort | tr -d '"' | \
    pivot $CACHE_FACTORY_LIST|  shortenParamValues | sort | \
    stripEmpty
) > $f
plot $f "${name} / Used Heap Memory" "threads - size - $param" "used heap [bytes]"
}

plotMemUsedSettled() {
name="$1";
param="$2";
f=$RESULT/${name}MemUsedSettled.dat
graphName=`basename $f .dat`;
(
echo "threads $CACHE_LABEL_LIST";
json | \
    jq -r ".[] |  select (.benchmark | contains (\".${name}\") ) | [ (.threads | tostring) + \"-\" + .params.entryCount + \"-\" + .params.$param, .params.cacheFactory, .[\"secondaryMetrics\"][\"+forced-gc-mem.used.settled\"][\"score\"] ] | @csv"  | \
    sort | tr -d '"' | \
    pivot $CACHE_FACTORY_LIST |  shortenParamValues | sort | \
    stripEmpty
) > $f
plot $f "${name} / Used Heap Memory Settled" "threads - size - $param" "used heap settled [bytes]"
}

header4() {
local I;
local n;
echo -n "param ";
for I in $@; do
  n="`echo $I | cacheShortNames`";
  echo -n "$n error lowerConvidence upperConvidence ";
done
echo "";
}

shortenParamValues() {
awk "$shorten_awk";
}

shorten_awk=`cat <<"EOF"
{
  cnt = split($1, params, "-");
  out = "";
  for (I in params) {
    val=params[I];
    if (val >= 1000) {
      print "here";
      ex = 0;
      while (val % 10 == 0) {
        val /= 10;
        ex++;
      }
      val = val"E"ex;
    }
    if (out != "") {
      out = out "-" val;
    } else {
      out = out val;
    }
  }
  $1 = out;
  print;
}
EOF
`

# plot main score, typically through put in operations per second.
plotOps() {
name="$1";
param="$2";
suffix="$3";
filter="$4";
local prods="$CACHE_FACTORY_LIST";
if test -n "$suffix"; then
f=$RESULT/${name}-${suffix}.dat
else
f=$RESULT/${name}.dat
fi
graphName=`basename $f .dat`;
(
header4 "$prods";
json | \
    jq -r ".[] |  select (.benchmark | contains (\".${name}\") ) | [ (.threads | tostring) + \"-\" + .params.entryCount + \"-\" + .params.$param, .params.cacheFactory, .primaryMetric.score, .primaryMetric.scoreError, .primaryMetric.scoreConfidence[0], .primaryMetric.scoreConfidence[1]  ] | @csv" | \
    sort | tr -d '"' | \
    pivot4 $prods | sort -nt- | shortenParamValues | grep "$filter" | \
    stripEmpty
) > $f
plot --withConfidence $f "${name} / Throughput" "threads-size-$param" "ops/s"
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

noBenchmark() {
local I;
for I in $RESULT/result-*"$1"*.json; do
  if test -f $I; then
    return 1;
  fi
done
return 0;
}

# merge all results into single json file
bigJson() {
result=$RESULT/data.json
# A sequence of the lines "]", "[", "]" will be ignored, there may be an empty json file, if a run fails
# A sequence of the lines "]", "[" will be replaced with ","
cat $RESULT/result-*.json | awk '/^]/ { f=1; g=0; next; } f && /^\[/ { g=1; f=0; next; } g { print "  ,"; g=0; } { print; } END { print "]"; }' > $result
}

typesetPlainMarkDown() {
(
echo "![]($1-notitle.svg)"
echo "*$2 ([Alternative image]($1-notitle-print.svg), [Data file]($1.dat))*"
echo;
) >> $RESULT/typeset-plain.md
}

typesetAsciiDoc() {
(
echo ".$2";
echo "image::$1-notitle.svg[link=\"$1-notitle.svg\"]"
echo;
echo "link:$1-notitle-print.svg[Alternative image], link:$1.dat[Data file]"
echo;

echo "link:$1-notitle.svg[Color image], link:$1-notitle-print.svg[Alternative image], link:$1.dat[Data file]"
echo;
) >> $RESULT/typeset.adoc
}

cleanTypesetting() {
echo -n > $RESULT/typeset-plain.md
echo -n > $RESULT/typeset.adoc
}

graph() {
test -f $RESULT/$1-notitle.svg || return 0;
typesetPlainMarkDown "$1" "$2";
typesetAsciiDoc "$1" "$2";
}

process() {

bigJson;
cleanTypesetting;

noBenchmark PopulateParallelOnceBenchmark || {

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

}

noBenchmark ReadOnlyBenchmark || {
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
}

benchmarks="RandomSequenceBenchmark NeverHitBenchmark RandomAccessLongSequenceBenchmark MultiRandomAccessBenchmark GeneratedRandomSequenceBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I hitRate;
      graph "$graphName" "$I, operations per second (complete)";

      # plotOps $I hitRate "strip" "^.*-50 .*\|^.*-95 .*";
      graph "$graphName" "$I, operations per second";

#      plotMemUsed $I hitRate;
#      plotMemUsedSettled $I hitRate;
      # plotEffectiveHitrate $I hitRate;
      # graph "$graphName" "$I, effective hitrate by target hitrate (complete)";

      # plotEffectiveHitrate $I hitRate "strip" "^.*-50 .*\|^.*-95 .*";
      # graph "$graphName" "$I, effective hitrate by target hitrate";
  }
done

benchmarks="ZipfianLoadingSequenceBenchmark ZipfianDirectSequenceLoadingBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I factor;
      graph "$graphName" "$I, operations per second by Zipfian distribution factor (complete)";
      plotOps $I factor "strip" "^.*-10 .*\|^.*-80 .*";
      graph "$graphName" "$I, operations per second by Zipfian distribution factor";
#      plotMemUsed $I factor;
#      plotMemUsedSettled $I factor;
      plotEffectiveHitrate $I factor;
      graph "$graphName" "$I, Effective hitrate by Zipfian distribution factor (complete)";
      plotEffectiveHitrate $I factor "strip" "^.*-10 .*\|^.*-80 .*";
      graph "$graphName" "$I, Effective hitrate by Zipfian distribution factor";
  }
done

name=RandomSequenceBenchmark
noBenchmark $name || {
spec="`cat << EOF
hitRate
$name / Memory
$name
4-1E6-95 (at 4 threads, 95% hit rate)
4-1E6-80 (at 4 threads, 80% hit rate)
4-1E6-50 (at 4 threads, 50% hit rate)
EOF
`"
echo "$spec" | plotMemoryGraphsSettled
graph "${name}MemorySettled4-95" "$name, used heap at end of benchmark after settling, 4 threads and 95% hit rate"
graph "${name}MemorySettled4-80" "$name, used heap at end of benchmark after settling, 4 threads and 80% hit rate"
graph "${name}MemorySettled4-50" "$name, used heap at and of benchmark after settling, 4 threads and 50% hit rate"
echo "$spec" | plotMemoryGraphs
graph "${name}Memory4-95" "$name, memory statistics at 4 threads and 95% hit rate"
graph "${name}Memory4-80" "$name, memory statistics at 4 threads and 80% hit rate"
graph "${name}Memory4-50" "$name, memory statistics at 4 threads and 50% hit rate"
}

benchmarks="ZipfianLoadingSequenceBenchmark ZipfianDirectSequenceLoadingBenchmark"
for name in $benchmarks; do
noBenchmark $name || {
spec="`cat << EOF
factor
$name / Memory
$name
4-20 (at 4 threads, factor 20)
4-40 (at 4 threads, factor 40)
4-80 (at 4 threads, factor 80)
EOF
`"
echo "$spec" | plotMemoryGraphsSettled
graph "${name}MemorySettled4-20" "$name, used heap at end of benchmark after settling, 4 threads and factor 20"
graph "${name}MemorySettled4-40" "$name, used heap at end of benchmark after settling, 4 threads and factor 40"
graph "${name}MemorySettled4-80" "$name, used heap at and of benchmark after settling, 4 threads and factor 80"
echo "$spec" | plotMemoryGraphs
graph "${name}Memory4-20" "$name, memory statistics at 4 threads and factor 20"
graph "${name}Memory4-40" "$name, memory statistics at 4 threads and factor 40"
graph "${name}Memory4-80" "$name, memory statistics at 4 threads and factor 80"
}
done

if false; then
(
cat << EOF
hitRate
MultiRandomAccessBenchmark / Memory
MultiRandomAccessBenchmark
4-80 (at 4 threads, 80% hit rate)
4-50 (at 4 threads, 50% hit rate)
EOF
) | plotMemoryGraphs

(
cat << EOF
hitRate
NeverHitBenchmark / Memory
NeverHitBenchmark
1 (at one thread)
EOF
) | plotMemoryGraphs

fi

(
cd $RESULT;
pandoc -o typeset-plain-markdown.html typeset-plain.md
echo $RESULT/typeset-plain-markdown.html
asciidoctor -o typeset-adoc.html typeset.adoc
echo $RESULT/typeset-adoc.html
)

}

doZipfian() {
bigJson;
benchmarks="ZipfianSequenceLoadingBenchmark ZipfianLoopingPrecomputedSequenceLoadingBenchmark ZipfianHoppingPrecomputedSequenceLoadingBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I factor;
      # plotOps $I factor "stripFactors" "^.*-10 .*\|^.*-80 .*";
      plotOps $I factor "strip1E5" "^.*-1E5-.*";
      plotMemUsed $I factor;
      plotMemUsedSettled $I factor;
      plotEffectiveHitrate $I factor;
  }
done
I=ZipfianSequenceLoadingBenchmark;
noBenchmark $I || {
(
cat << EOF
factor
$I / Memory
$I
4-1E5-10 (at 4 threads, factor 10)
4-1E5-80 (at 4 threads, factor 80)
4-1E6-10 (at 4 threads, factor 10)
4-1E6-80 (at 4 threads, factor 80)
4-1E7-10 (at 4 threads, factor 10)
4-1E7-80 (at 4 threads, factor 80)
EOF
) | plotMemoryGraphs
}
}

doRandomSequence() {
benchmarks="RandomSequenceBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I hitRate;
      graph "$graphName" "$I, operations per second (complete)";

      plotOps $I hitRate "strip1E5" "^.*1E5-50 .*\|^.*1E5-95 .*";
      graph "$graphName" "$I, operations per second";

      plotOps $I hitRate "strip1E6" "^.*1E6-50 .*\|^.*1E6-95 .*";
      graph "$graphName" "$I, operations per second";

#      plotMemUsed $I hitRate;
#      plotMemUsedSettled $I hitRate;
      plotEffectiveHitrate $I hitRate;
      graph "$graphName" "$I, effective hitrate by target hitrate (complete)";
      plotEffectiveHitrate $I hitRate "strip1E5" "^.*-1E5-50 .*\|^.*-1E5-95 .*";
      graph "$graphName" "$I, effective hitrate by target hitrate";

      plotEffectiveHitrate $I hitRate "strip1E6" "^.*-1E6-50 .*\|^.*-1E6-95 .*";
      graph "$graphName" "$I, effective hitrate by target hitrate";
  }
done
}

processCommandLine "$@";
