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
org.cache2k.benchmark.thirdparty.EhCache2Factory \
org.cache2k.benchmark.thirdparty.TCache1Factory";

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
echo "set terminal svg size 800,600"
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

colorScheme() {
local cnt=1;
local colors="$@";
for I in $colors; do
  echo "set style line $cnt lt rgb \"$I\"";
  cnt=$(( $cnt + 1 ));
done
cnt=0;
echo "set palette defined ( \\";
for I in $colors; do
 if [ $cnt -gt 0 ]; then echo ", \\"; fi
 echo -n " $cnt '$I'"
 cnt=$(( $cnt + 1 ));
done
echo ")";
echo "set palette maxcolors $cnt";
}

highContrast() {
# http://colorbrewer2.org/....
colorScheme "#d7191c #ffffbf #fdae61 #abdda4 #2b83ba #1b7837"
# colors="aaaabf fdae61 abdda4 2b83ba 1b7837"
}

memoryColors() {
colorScheme "#a6cee3 #1f78b4 #b2df8a #33a02c #fb9a99 #e31a1c #fdbf6f #ff7f00"
}

printColors() {
colorScheme "#1b9e77 #d95f02 #7570b3 #e7298a #66661e #e6ab02 #a6761d #666666";
}

printColorsDark() {
colorScheme "#1b9e77 #d95f02 #7570b3 #444444 #e7298a #66a61e #e6ab02 #666666"
}

plotData() {
echo "set style histogram clustered gap 2 title  offset character 0, 0, 0";
local in="$1";
local idx="$2";
idx=$(( $idx + 1 ));
local endIndex=$(( $3 + 1 ));
echo  -n "plot '$in' using $idx:xtic(1) ti col ls $(( $idx - 1 ))";
cols=$(( `head -n 1 "$in" | wc -w` ));
idx=$(( $idx + 1 ));
while [ $idx -le $cols ] && [ $idx -le $endIndex ]; do
  echo -n ", '' u $idx ti col ls $(( $idx - 1 ))";
  idx=$(( $idx + 1 ));
done
}

plotDataWithConfidence() {
echo "set style histogram errorbars gap 2 lw 0.5 title  offset character 0, 0, 0";
local in="$1";
local idx="$2";
local endIndex=$(( ( $3 - 1 ) * 4 + 2 ));
local ls=$idx;
idx=$(( ($idx - 1) * 4 + 2 ));
echo  -n "plot '$in' using $idx:$(( idx + 2)):$(( idx + 3)):xtic(1) ti col($idx) ls ${ls}";
cols=$(( `head -n 1 "$in" | wc -w` ));
# TODO: replace with plot for ....
ls=$(( $ls + 1 ));
idx=$(( $idx + 4 ));
while [ $idx -lt $cols ] && [ $idx -le $endIndex ]; do
  echo -n ", '' u $idx:$(( idx + 2)):$(( idx + 3)) ti col($idx) ls ${ls}";
  ls=$(( $ls + 1 ));
  idx=$(( $idx + 4 ));
done
}

plot() {
local plot="plotData";
local colorScheme="highContrast";
local startIndex=1;
local endIndex=100;
while true; do
  case "$1" in
    --withColors) colorScheme="$2"; shift 1;;
    --startIndex) startIndex="$2"; shift 1;;
    --endIndex) endIndex="$2"; shift 1;;
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
  unset graphName;
  echo "No data, skipping: $in";
  return;
fi
graphName=`basename $in .dat`;

if false; then
echo "$out ....";
(
plotHistogramHeader "$in" "$title" "$yTitle" "$xTitle";
echo "set output '$out'"
# http://stackoverflow.com/questions/15549830/how-to-get-gnuplot-to-use-a-centered-multi-line-title-with-left-aligned-lines
# the title is always centered over the graph, excluding the legend!
# echo "set title '$title'";
echo "set title \"$title\"";
$colorScheme;
$plot "$in" $startIndex $endIndex;
) > "$in".plot
gnuplot "$in".plot;
fi

# just a copy of above but leaving out the title for texts/blogs providing an image title
local out="`dirname "$in"`/`basename "$in" .dat`-notitle.svg";
echo "$out ....";
(
plotHistogramHeader "$in" "$title" "$xTitle" "$yTitle";
echo "set output '$out'"
$colorScheme;
$plot "$in" $startIndex $endIndex;
) > "${in}-notitle.plot"
gnuplot "${in}-notitle.plot";

# just a copy of above but leaving out the title for texts/blogs providing an image title
local out="`dirname "$in"`/`basename "$in" .dat`-notitle-print.svg";
echo "$out ....";
(
plotHistogramHeader "$in" "$title" "$yTitle" "$xTitle";
echo "set output '$out'"
echo "set style fill pattern border"
# printColors;
# highContrast;
# echo "set colorsequence podo"
# printColorsDark;
$plot "$in" $startIndex $endIndex;
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
    .["secondaryMetrics"]["+forced-gc-mem.usedHeap"].score,
    .["secondaryMetrics"]["+forced-gc-mem.usedHeap"].scoreError,
    .["secondaryMetrics"]["+forced-gc-mem.usedHeap"].scoreConfidence[0],
    .["secondaryMetrics"]["+forced-gc-mem.usedHeap"].scoreConfidence[1],
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
    .["secondaryMetrics"]["+c2k.gc.maximumCommittedAfterGc"].score,
    .["secondaryMetrics"]["+c2k.gc.maximumCommittedAfterGc"].scoreError,
    .["secondaryMetrics"]["+c2k.gc.maximumCommittedAfterGc"].scoreConfidence[0],
    .["secondaryMetrics"]["+c2k.gc.maximumCommittedAfterGc"].scoreConfidence[1],
    .["secondaryMetrics"]["+forced-gc-mem.used.VmRSS"].score * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmRSS"].scoreError * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmRSS"].scoreConfidence[0] * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmRSS"].scoreConfidence[1] * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].score * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreError * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreConfidence[0] * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreConfidence[1] * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"].score * 1000 * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"].scoreError * 1000 * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"].scoreConfidence[0] * 1000 * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate"].scoreConfidence[1] * 1000 * 1000,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate.norm"].score,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate.norm"].scoreError,
    .["secondaryMetrics"]["+c2k.gc.alloc.rate.norm"].scoreConfidence[0],
    .["secondaryMetrics"]["+c2k.gc.alloc.rate.norm"].scoreConfidence[1]
  ] | @csv
EOF
`
json | \
    jq -r "$query" | \
    sort | tr -d '"'
     # | scaleBytesToMegaBytes4x4MemAlloc
}

extractStaticPeakMemoryThreadsHitRate() {
local query=`cat << EOF
.[] |  select (.benchmark | contains (".$1") ) |
  [ (.threads | tostring) + "-" + .params.entryCount + "-" + .params.$2, .params.cacheFactory,
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"].score,
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"].scoreError,
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"].scoreConfidence[0],
    .["secondaryMetrics"]["+forced-gc-mem.used.settled"].scoreConfidence[1],
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].score * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreError * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreConfidence[0] * 1000,
    .["secondaryMetrics"]["+forced-gc-mem.used.VmHWM"].scoreConfidence[1] * 1000
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

# everything except alloc rate
plotMemoryGraphs() {
read param;
read title;
read benchmark;
while read key description; do
  f=$RESULT/${benchmark}Memory$key.dat
  (
#    echo "product usedHeap_settled error lower upper usedMem_settled error lower upper usedMem_fin error lower upper usedMem_max error lower upper totalMem_settled error lower upper totalMem_max error lower upper VmRSS error lower upper VmHWM error lower upper allocRate(byte/s) error lower upper allocRate(byte/op) error lower upper ";
    echo "product usedHeap_settled error lower upper usedMem_settled error lower upper usedMem_fin error lower upper usedMem_max error lower upper totalMem_settled error lower upper totalMem_max error lower upper VmRSS error lower upper VmHWM error lower upper";
    local tmp="$RESULT/tmp-plotMemoryGraphs-$benchmark-$param.data"
    test -f "$tmp" || extractMemoryThreadsHitRate $benchmark $param | tr , " " | shortenParamValues > "$tmp"
    cat "$tmp" | grep "^$key" | stripFirstColumn | cacheShortNames
  ) > $f
  plot --withConfidence --withColors memoryColors $f "$title\n$description" "cache" "Bytes"
done
}

plotMem() {
local title="";
local description="";
local variant="";
local filter="";
local startIndex=1;
local endIndex=100;
local sort="";
while true; do
  case "$1" in
    --title) title="$2"; shift 1;;
    --description) description="$2"; shift 1;;
    --variant) variant="$2"; shift 1;;
    --filter) filter="$2"; shift 1;;
    --startIndex) startIndex="$2"; shift 1;;
    --endIndex) endIndex="$2"; shift 1;;
    --sort) sort="1";;
    -*) echo "unknown option: $1"; return 1;;
    *) break;;
  esac
  shift 1;
done
local benchmark="$1";
local param="$2";
local key="$3";
  f=$RESULT/${benchmark}Memory$key$variant.dat
  (
    echo "product usedHeap_settled error lower upper usedMem_settled error lower upper usedMem_fin error lower upper usedMem_max error lower upper totalMem_settled error lower upper totalMem_max error lower upper VmRSS error lower upper VmHWM error lower upper allocRate(byte/s) error lower upper allocRate(byte/op) error lower upper";
    local tmp="$RESULT/tmp-plotMemoryGraphs-$benchmark-$param.data"
    test -f "$tmp" || extractMemoryThreadsHitRate $benchmark $param | tr , " " | shortenParamValues > "$tmp"
    cat "$tmp" | grep "^$key" | stripFirstColumn | cacheShortNames \
    | { if test -n "$sort"; then sort -k$(( ( $startIndex - 1) * 4 + 2 )) -g; else cat -; fi } \
    | grep "$filter" || true
  ) > $f
  plot --withConfidence --withColors memoryColors --startIndex "$startIndex" --endIndex "$endIndex" $f "$title\n$description" "cache" "Bytes"
}

plotStaticPeakMem() {
local title="";
local description="";
local variant="";
local filter="";
local startIndex=1;
local endIndex=100;
local sort="";
while true; do
  case "$1" in
    --title) title="$2"; shift 1;;
    --description) description="$2"; shift 1;;
    --variant) variant="$2"; shift 1;;
    --filter) filter="$2"; shift 1;;
    --startIndex) startIndex="$2"; shift 1;;
    --endIndex) endIndex="$2"; shift 1;;
    --sort) sort="1";;
    -*) echo "unknown option: $1"; return 1;;
    *) break;;
  esac
  shift 1;
done
local benchmark="$1";
local param="$2";
local key="$3";
  f=$RESULT/${benchmark}StaticPeakMemory$key$variant.dat
  (
    echo "product usedMem_settled error lower upper VmHWM error lower upper";
    local tmp="$RESULT/tmp-plotStaticPeakMemoryGraphs-$benchmark-$param.data"
    test -f "$tmp" || extractStaticPeakMemoryThreadsHitRate $benchmark $param | tr , " " | shortenParamValues > "$tmp"
    cat "$tmp" | grep "^$key" | stripFirstColumn | cacheShortNames \
    | { if test -n "$sort"; then sort -k$(( ( $startIndex - 1) * 4 + 2 )) -g; else cat -; fi } \
    | grep "$filter" || true
  ) > $f
  plot --withConfidence --withColors memoryColors --startIndex "$startIndex" --endIndex "$endIndex" $f "$title\n$description" "cache" "Bytes"
}



plotMemoryGraphsSettled() {
read param;
read title;
read benchmark;
while read key description; do
  f=$RESULT/${benchmark}MemorySettled$key.dat
  (
    echo "threads usedHeap_settled error lower upper usedMem_settled error lower upper usedMem_fin error lower upper";
    local tmp="$RESULT/tmp-plotMemoryGraphsSettled-$benchmark-$param.data"
    test -f "$tmp" || extractMemoryThreadsHitRate $benchmark $param | tr , " " | shortenParamValues > "$tmp"
    cat "$tmp" | grep "^$key" | stripFirstColumn | cacheShortNames
  ) > $f
  plot --withConfidence --withColors memoryColors $f "$title\n$description" "cache" "Bytes"
done
}

plotMemoryGraphsUsed() {
read param;
read title;
read benchmark;
while read key description; do
  f=$RESULT/${benchmark}MemoryUsed$key.dat
  (
    echo "product usedHeap_settled error lower upper usedMem_settled error lower upper usedMem_fin error lower upper usedMem_max error lower upper";
    local tmp="$RESULT/tmp-plotMemoryGraphsUsed-$benchmark-$param.data"
    test -f "$tmp" || extractMemoryThreadsHitRate $benchmark $param | tr , " " | shortenParamValues > "$tmp"
    cat "$tmp" | grep "^$key" | stripFirstColumn | cacheShortNames
  ) > $f
  plot --withConfidence --withColors memoryColors $f "$title\n$description" "cache" "Bytes"
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
local tmp="$RESULT/tmp-plotEffectiveHitrate-$name-$param.data"
test -f "$tmp" || json | \
    jq -r "$query"  | sort | tr -d '"' | pivot4 $prods | sort -n -t- -k1,1 -k2,2 -k3,3 | shortenParamValues > "$tmp"
    cat "$tmp" | grep "$filter" | stripEmpty
) > $f
plot --withConfidence $f "${name} / Effective Hit Rate" "threads - size - $param" "effective hitrate"
}

plotScanCount() {
name="$1";
param="$2";
suffix="$3";
filter="$4";
local prods="org.cache2k.benchmark.Cache2kFactory";
if test -n "$suffix"; then
f=$RESULT/${name}ScanCount-${suffix}.dat
else
f=$RESULT/${name}ScanCount.dat
fi
(
header4 "$prods";
local query=`cat << EOF
.[] |  select (.benchmark | contains (".${name}") ) |
  [ (.threads | tostring) +  "-" + .params.entryCount + "-" + .params.$param,
     .params.cacheFactory,
     .["secondaryMetrics"]["+c2k.stat.scanPerEviction"].score,
     .["secondaryMetrics"]["+c2k.stat.scanPerEviction"].scoreError,
     .["secondaryMetrics"]["+c2k.stat.scanPerEviction"].scoreConfidence[0],
     .["secondaryMetrics"]["+c2k.stat.scanPerEviction"].scoreConfidence[1]
  ] | @csv
EOF
`
local tmp="$RESULT/tmp-plotScanCount-$name-$param.data"
test -f "$tmp" || json | \
    jq -r "$query"  | sort | tr -d '"' | pivot4 $prods | sort -n -t- -k1,1 -k2,2 -k3,3 | shortenParamValues > "$tmp"
    cat "$tmp" | grep "$filter" | stripEmpty
) > $f
plot --withConfidence $f "${name} / scans per eviction" "threads - size - $param" "scan count"
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

# shortenParamValuesWithE
#
# 8-100000-20 gets to: 8-1E5-20
#
shortenParamValuesWithE() {
awk "$shorten_awk";
}

shorten_awk=`cat <<"EOF"
{
  cnt = split($1, params, "-");
  out = "";
  for (I in params) {
    val=params[I];
    if (val >= 1000) {
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
      ex = 0;
      while (val % 1000 == 0) {
        val /= 1000;
        ex += 3;
      }
      suffix = "E" ex;
      if (ex == 3) {
        suffix = "K";
      } else if (ex == 6) {
        suffix = "M";
      } else if (ex == 8) {
        suffix = "G";
      }
      val = val suffix;
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
(
header4 "$prods";
local tmp="$RESULT/tmp-plotOps-$name-$param.data"
test -f "$tmp" || json | \
    jq -r ".[] |  select (.benchmark | contains (\".${name}\") ) | [ (.threads | tostring) + \"-\" + .params.entryCount + \"-\" + .params.$param, .params.cacheFactory, .primaryMetric.score, .primaryMetric.scoreError, .primaryMetric.scoreConfidence[0], .primaryMetric.scoreConfidence[1]  ] | @csv" | \
    sort | tr -d '"' | \
    pivot4 $prods | sort -n -t- -k1,1 -k2,2 -k3,3 | shortenParamValues > "$tmp"
    cat "$tmp" | grep "$filter" | stripEmpty
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
# clean tmp files.
rm -f $RESULT/tmp-*;

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

      for TC in 10 4; do
        for HR in 95 80 50; do
          plotOps $I hitRate "bySize-${TC}x$HR" "^$TC-.*-$HR .*";
          graph "$graphName" "$I, operations per second by cache size at $TC threads and $HR percent target hit rate";
          plotEffectiveHitrate $I hitRate "bySize-${TC}x$HR" "^$TC-.*-$HR .*";
          graph "$graphName" "$I, effective hit rate by cache size at $TC threads and $HR percent target hit rate";
          plotScanCount $I hitRate "bySize-${TC}x$HR" "^$TC-.*-$HR .*";
          graph "$graphName" "$I, scan count by cache size at $TC threads and $HR percent target hit rate";
        done
      done

      for S in 100K 1M 10M; do
        for HR in 95 80 50; do
          plotOps $I hitRate "byThread-${S}x$HR" "^.*-$S-$HR .*";
          graph "$graphName" "$I, operations per second by thread count at $HR percent target hit rate and $S cache size";
          plotEffectiveHitrate $I hitRate "byThread-${S}x$HR" "^.*-$S-$HR .*";
          graph "$graphName" "$I, effective hitrate by thread count at $HR percent target hit rate and $S cache size";
          plotScanCount $I hitRate "byThread-${S}x$HR" "^.*-$S-$HR .*";
          graph "$graphName" "$I, scan count by thread count at $HR percent target hit rate and $S cache size";
        done
      done

      for TC in 10 4; do
        for S in 100K 1M 10M; do
          plotOps $I hitRate "byHitrate-$TC-${S}" "^$TC-$S-.* .*";
          graph "$graphName" "$I, operations per second by hit rate at $TC threads and $S cache size";
          plotEffectiveHitrate $I hitRate "byHitrate-$TC-${S}" "^$TC-$S-.* .*";
          graph "$graphName" "$I, effective hit rate by thread count at $TC threads and $S cache size";
          plotScanCount $I hitRate "byHitrate-${TC}x${S}" "^$TC-$S-.* .*";
          graph "$graphName" "$I, scan count by hit rate at $TC threads and $S cache size";
        done
      done

  }
done

benchmarks="ZipfianSequenceLoadingBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I factor;
      graph "$graphName" "$I, operations per second by Zipfian distribution factor (complete)";

#      plotEffectiveHitrate $I factor;
#      graph "$graphName" "$I, Effective hitrate by Zipfian distribution factor (complete)";

      for TC in 10 4; do
        for F in 5 10 20; do
          plotOps $I factor "bySize-${TC}x$F" "^$TC-.*-$F .*";
          graph "$graphName" "$I, operations per second by cache size at $TC threads and Zipfian factor $F";
          plotEffectiveHitrate $I factor "bySize-${TC}x$F" "^$TC-.*-$F .*";
          graph "$graphName" "$I, effective hit rate by cache size at $TC threads and Zipfian factor $F";
          plotScanCount $I factor "bySize-${TC}x$F" "^$TC-.*-$F .*";
          graph "$graphName" "$I, scan count by cache size at $TC threads and Zipfian factor $F";
        done
      done

      for S in 100K 1M 10M; do
        for F in 5 10 20; do
          plotOps $I factor "byThread-${S}x$F" "^.*-${S}-$F .*";
          graph "$graphName" "$I, operations per second by thread count with cache size ${S} and Zipfian factor $F";
        done
      done

      for TC in 10 4; do
          for S in 100K 1M 10M; do
              plotOps $I factor "byFactor-${S}-${TC}" "^$TC-$S-.* .*";
              graph "$graphName" "$I, operations per second by Zipfian factor with cache size ${S} at $TC threads";
              plotEffectiveHitrate $I factor "byFactor-${S}-${TC}" "^$TC-$S-.* .*";
              graph "$graphName" "$I, effective hit rate by Zipfian factor with cache size ${S} at $TC threads";
              plotScanCount $I factor "byFactor-${S}-${TC}" "^$TC-$S-.* .*";
              graph "$graphName" "$I, scan count by Zipfian factor with cache size ${S} at $TC threads";
          done
      done


false && (
      plotOps $I factor "strip4x100Kx10" "^4-100K-10 .*";
      graph "$graphName" "$I, operations per second at 4 threads, 100K cache entries and Zipfian factor 10";

      plotOps $I factor "stripXx100Kx10" "^.*-100K-10 .*";
      graph "$graphName" "$I, operations per second with 100K cache entries and Zipfian factor 10";

      plotOps $I factor "stripXxXx10" "^.*-.*-10 .*";
      graph "$graphName" "$I, operations per second with Zipfian factor 10";

      plotOps $I factor "stripXxXx5" "^.*-.*-5 .*";
      graph "$graphName" "$I, operations per second with Zipfian factor 5";

      plotOps $I factor "strip4x80" "^4-.*-80 .*";
      graph "$graphName" "$I, operations per second at 4 threads and Zipfian factor 80";

      plotEffectiveHitrate $I factor "strip4x80" "^4-.*-80 .*";
      graph "$graphName" "$I, Effective hit rate at 4 threads and Zipfian factor 80";

      plotOps $I factor "strip10x80" "^10-.*-80 .*";
      graph "$graphName" "$I, operations per second at 10 threads and Zipfian factor 80";

      plotEffectiveHitrate $I factor "strip10x80" "^10-.*-80 .*";
      graph "$graphName" "$I, Effective hit rate at 10 threads and Zipfian factor 80";
)

#      plotMemUsed $I factor;
#      plotMemUsedSettled $I factor;
  }
done

name=RandomSequenceBenchmark
noBenchmark $name || {
spec="`cat << EOF
hitRate
$name / Memory
$name
4-1M-95 (at 4 threads, 95% hit rate)
4-1M-80 (at 4 threads, 80% hit rate)
4-1M-50 (at 4 threads, 50% hit rate)
EOF
`"
# echo "$spec" | plotMemoryGraphsSettled
echo "$spec" | plotMemoryGraphsUsed
echo "$spec" | plotMemoryGraphs
percents="50 80 95";
sizes="100K 1M 10M";
threads="1 4 8";
for percent in $percents; do
  for size in $sizes; do
    for thread in $threads; do
      graph "${name}MemoryUsed$thread-$size-$percent" "$name, used memory as reported by the JVM of benchmark and after settling, $thread threads, cache of $size entry capacity at $percent% hit rate"
#      graph "${name}MemoryHeap$thread-$size-$percent" "$name, used heap at end, after settling and after GC during run, $thread threads, cache of $size entry capacity at $percent% hit rate"
      graph "${name}Memory$thread-$size-$percent" "$name, all memory metrics, $thread threads, cache of $size entry capacity at $percent% hit rate"
    done
  done
done
}

benchmarks="ZipfianSequenceLoadingBenchmark"
for name in $benchmarks; do
noBenchmark $name || {
spec="`cat << EOF
factor
$name / Memory
$name
1-1M-10 (at 1 threads, factor 10)
4-1M-10 (at 4 threads, factor 10)
8-1M-10 (at 8 threads, factor 10)
1-100K-10 (at 1 threads, factor 10)
2-100K-10 (at 2 threads, factor 10)
4-100K-10 (at 4 threads, factor 10)
8-100K-10 (at 8 threads, factor 10)
EOF
`"
# echo "$spec" | plotMemoryGraphsSettled
echo "$spec" | plotMemoryGraphsUsed
echo "$spec" | plotMemoryGraphs
factors="5 10 20";
sizes="100K 1M 10M";
threads="1 2 4 8";
majorThreads="4";
for factor in $factors; do
  for size in $sizes; do
    for thread in $threads; do
      graph "${name}MemoryUsed$thread-$size-$factor" "$name, used memory of benchmark after settling, $thread threads, $size cache entries, Zipfian factor $factor"
#      graph "${name}MemoryHeap$thread-$size-$factor" "$name, used heap at end, after settling and after GC during run, $thread threads, cache of $size entry capacity at Zipfian factor $factor"
      graph "${name}Memory$thread-$size-$factor" "$name, complete memory statistics, $thread threads, $size cache entries, Zipfian factor $factor"
    done
  done
done
}

for factor in $factors; do
  for size in $sizes; do
    for thread in $majorThreads; do
     plotMem --startIndex 5 --endIndex 8 --variant "-total" $name factor "$thread-$size-$factor";
     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, metrics for total memory consumption";
     plotMem --startIndex 8 --endIndex 8 --sort --variant "-VmHWM-sorted" $name factor "$thread-$size-$factor";
     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, peak memory usage reported by the operating system (VmHWM), sorted by best performance";
     plotMem --startIndex 1 --endIndex 1 --sort --variant "-usedHeap-sorted" $name factor "$thread-$size-$factor";
     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, used heap memory, sorted by best performance";
     plotMem --startIndex 9 --endIndex 9 --variant "-allocRate" $name factor "$thread-$size-$factor";
     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, allocation rate in bytes per second";
     plotMem --startIndex 10 --endIndex 10 --variant "-allocRatePerOp" $name factor "$thread-$size-$factor";
     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, allocation rate in bytes per operation";
     plotStaticPeakMem $name factor "$thread-$size-$factor";
     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, static and peak memory usage";
    done
  done
done

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

processCommandLine "$@";
