#!/bin/bash

# processJmhResults.sh

# Copyright 2013-2016 headissue GmbH, Jens Wilke

# This script processes the benchmark results into svg graphics.
#
# Needed tools: asciidoctor gnuplot jq pandoc
# 
# Run it with the following subcommands: 
#
# process   paint nice diagrams

# stop on error
set -e

# print commands
# set -x

RESULT="target/jmh-result";
SITE="../cache2k/src/site/resources/benchmark-result";

# replace class names by short name of each cache implementation for graph labeling
cacheShortNames() {
local script=`cat << EOF

s/org.cache2k.benchmark.ConcurrentHashMapFactory/CHM/
s/org.cache2k.benchmark.SynchronizedLinkedHashMapFactory/SLHM/
s/org.cache2k.benchmark.PartitionedLinkedHashMapFactory/PLHM/
s/org.cache2k.benchmark.cache.EhCache2Factory/EhCache2/
s/org.cache2k.benchmark.cache.EhCache3Factory/EhCache3/
s/org.cache2k.benchmark.cache.CaffeineCacheFactory/Caffeine/
s/org.cache2k.benchmark.cache.GuavaCacheFactory/Guava/
s/org.cache2k.benchmark.cache.Cache2kFactory/cache2k/
s/org.cache2k.benchmark.cache.TCache1Factory/tCache/
s/org.cache2k.benchmark.ConcurrentHashMapFactory0/CHM~/
s/org.cache2k.benchmark.SynchronizedLinkedHashMapFactory0/SLHM~/
s/org.cache2k.benchmark.thirdparty.EhCache2Factory0/EhCache2~/
s/org.cache2k.benchmark.thirdparty.EhCache3Factory0/EhCache3~/
s/org.cache2k.benchmark.thirdparty.CaffeineCacheFactory0/Caffeine~/
s/org.cache2k.benchmark.thirdparty.GuavaCacheFactory0/Guava~/
s/org.cache2k.benchmark.Cache2kFactory0/cache2k~/
s/org.cache2k.benchmark.thirdparty.TCache1Factory0/tCache~/
EOF
`
sed "$script";
}

CACHE_FACTORY_LIST="org.cache2k.benchmark.cache.Cache2kFactory \
org.cache2k.benchmark.cache.CaffeineCacheFactory \
org.cache2k.benchmark.cache.EhCache3Factory";
CACHE_FACTORY_LIST_BASE="org.cache2k.cache.Cache2kFactory0 \
org.cache2k.benchmark.cache.CaffeineCacheFactory0 \
org.cache2k.benchmark.cache.EhCache3Factory0";

# "org.cache2k.benchmark.thirdparty.EhCache3Factory";

# for TCache we need to add:
# "org.cache2k.benchmark.thirdparty.TCache1Factory";

# CACHE_FACTORY_LIST=org.cache2k.benchmark.thirdparty.CaffeineCacheFactory

CACHE_LABEL_LIST=`echo $CACHE_FACTORY_LIST | cacheShortNames`;

withBase() {
CACHE_FACTORY_LIST="$CACHE_FACTORY_LIST $CACHE_FACTORY_LIST_BASE";
CACHE_LABEL_LIST=`echo $CACHE_FACTORY_LIST | cacheShortNames`;
}

processCommandLine() {
  pars="$#";
  while true; do
    case "$1" in
      --dir) RESULT="$2"; shift 1;;
      --debug) set -x;;
      --interactive) set +e;;
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
     printf ("%s ", v);
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

# TODO: fix normed allocation rate
memoryMetricsHeader() {
echo -n "product";
for I in maxCommitted.afterGc VmHWM maxUsed.afterGc VmRSS.fin liveObjects "allocRate(byte/s)" "allocRate(byte/op)"; do
  echo -n " $I error lower upper";
done
echo;
}

#
# Input is a benchmark name like Xy or Xy-variant
# Prints jq select statement
#
benchmarkSelector() {
if [[ "$1" = *"-"* ]]; then
  local variant="${1#*-}";
  local benchmark="${1%-*}";
  echo "select ((.benchmark | contains (\"$benchmark\")) and (.params.variant == \"${variant}\"))";
else
  echo "select ((.benchmark | contains (\"$1\")) and (.params.variant | . == null or . == \"\"))";
fi
}

#
# Extract benchmark name from Xy-variant
#
benchmarkName() {
if [[ "$1" = *"-"* ]]; then
  echo "${1%-*}";
else
  echo "$1"
fi
}

# Extract memory metrics for a particular benchmark
#
# Usage: extractMemoryMetrics <benchmark> <paramName>
#
# example output:
# 1-20,org.cache2k.benchmark.Cache2kFactory,0.00,65.72,993.08,614.8539540861144
# 1-20,org.cache2k.benchmark.thirdparty.CaffeineCacheFactory,0.00,71.27,1000.42,852.382233323991
# 1-20,org.cache2k.benchmark.thirdparty.EhCache2Factory,0.00,58.28,606.75,204.83546236165807
extractMemoryMetrics() {
local selector="`benchmarkSelector "$1"`";
# .[] |  select (.benchmark | contains (".$1") ) |
local query=`cat << EOF
.[] | $selector |
  [ (.threads | tostring) + "-" + .params.entryCount + "-" + .params.$2, .params.cacheFactory,
    .["secondaryMetrics"]["+c2k.gc.maximumCommittedAfterGc"].score,
    .["secondaryMetrics"]["+c2k.gc.maximumCommittedAfterGc"].scoreError,
    .["secondaryMetrics"]["+c2k.gc.maximumCommittedAfterGc"].scoreConfidence[0],
    .["secondaryMetrics"]["+c2k.gc.maximumCommittedAfterGc"].scoreConfidence[1],
    .["secondaryMetrics"]["+linux.proc.status.VmHWM"].score * 1000,
    .["secondaryMetrics"]["+linux.proc.status.VmHWM"].scoreError * 1000,
    .["secondaryMetrics"]["+linux.proc.status.VmHWM"].scoreConfidence[0] * 1000,
    .["secondaryMetrics"]["+linux.proc.status.VmHWM"].scoreConfidence[1] * 1000,
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"].score,
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"].scoreError,
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"].scoreConfidence[0],
    .["secondaryMetrics"]["+c2k.gc.maximumUsedAfterGc"].scoreConfidence[1],
    .["secondaryMetrics"]["+linux.proc.status.VmRSS"].score * 1000,
    .["secondaryMetrics"]["+linux.proc.status.VmRSS"].scoreError * 1000,
    .["secondaryMetrics"]["+linux.proc.status.VmRSS"].scoreConfidence[0] * 1000,
    .["secondaryMetrics"]["+linux.proc.status.VmRSS"].scoreConfidence[1] * 1000,
    .["secondaryMetrics"]["+liveObjects"].score,
    .["secondaryMetrics"]["+liveObjects"].scoreError,
    .["secondaryMetrics"]["+liveObjects"].scoreConfidence[0],
    .["secondaryMetrics"]["+liveObjects"].scoreConfidence[1],
    .["secondaryMetrics"]["·gc.alloc.rate"].score * 1000 * 1000,
    .["secondaryMetrics"]["·gc.alloc.rate"].scoreError * 1000 * 1000,
    .["secondaryMetrics"]["·gc.alloc.rate"].scoreConfidence[0] * 1000 * 1000,
    .["secondaryMetrics"]["·gc.alloc.rate"].scoreConfidence[1] * 1000 * 1000,
    .["secondaryMetrics"]["·gc.alloc.rate.norm"].score,
    .["secondaryMetrics"]["·gc.alloc.rate.norm"].scoreError,
    .["secondaryMetrics"]["·gc.alloc.rate.norm"].scoreConfidence[0],
    .["secondaryMetrics"]["·gc.alloc.rate.norm"].scoreConfidence[1]
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

memoryMetrics() {
  local benchmark="$1";
  local param="$2";
  local tmp="$RESULT/tmp-memoryMetrics-$benchmark-$param.data"
  test -f "$tmp" || extractMemoryMetrics $benchmark $param | tr , " " | shortenParamValues > "$tmp"
  cat "$tmp"
}

# everything except alloc rate
plotMemoryGraphs() {
read param;
read title;
read benchmark;
while read key description; do
  f=$RESULT/${benchmark}Memory$key.dat
  (
    memoryMetrics "$benchmark" "$param" | grep "^$key" | stripFirstColumn | cacheShortNames
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
  f=$RESULT/${benchmark}-Memory-$key$variant.dat
  (
    memoryMetricsHeader
    memoryMetrics "$benchmark" "$param" | grep "^$key" | stripFirstColumn | cacheShortNames \
    | { if test -n "$sort"; then sort -k$(( ( $startIndex - 1) * 4 + 2 )) -g; else cat -; fi } \
    | grep "$filter" || true
  ) > $f
  plot --withConfidence --withColors memoryColors --startIndex "$startIndex" --endIndex "$endIndex" $f "$title\n$description" "cache" "Bytes"
}


plotEffectiveHitrate() {
name="$1";
param="$2";
suffix="$3";
filter="$4";
local prods="$CACHE_FACTORY_LIST";
if test -n "$suffix"; then
f=$RESULT/${name}-EffectiveHitrate-${suffix}.dat
else
f=$RESULT/${name}-EffectiveHitrate.dat
fi
(
header4 "$prods";
# echo "threads $CACHE_LABEL_LIST";
# TODO: we cannot calculate with confidences
# previous version:  100 - .["secondaryMetrics"]["+misc.missCount"].score * 100 / .["secondaryMetrics"]["+misc.opCount"]["score"],
# new:      .["secondaryMetrics"]["+misc.hitrate"].score,
local selector="`benchmarkSelector "$name"`";
# .[] |  select (.benchmark | contains (".${name}") ) |
local query=`cat << EOF
.[] | $selector |
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
local prods="org.cache2k.benchmark.cache.Cache2kFactory";
if test -n "$suffix"; then
f=$RESULT/${name}-ScanCount-${suffix}.dat
else
f=$RESULT/${name}-ScanCount.dat
fi
local selector="`benchmarkSelector "$name"`";
(
header4 "$prods";
local query=`cat << EOF
.[] | $selector |
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
awk "$shorten_awk_withE";
}

shorten_awk_withE=`cat <<"EOF"
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
      if (ex == 0) {
        suffix = "";
      } else if (ex == 3) {
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
  # third parameter might not be used, remove
  if (substr(out, length(out)) == "-") {
    out=substr(out, 0, length(out) -1);
  }
  $1 = out;
  print;
}
EOF
`

# plot main score, typically through put in operations per second.
# in case the benchmark has no additional parameter "none" can be specified
plotOps() {
name="$1";
param="$2";
suffix="$3";
filter="$4";
prods="$CACHE_FACTORY_LIST $5";
tmpext="";
if test -n "$6"; then
  prods="$6";
  # seperate tmp files if different list ist used!
  tmpext="-$suffix";
fi
if test -n "$suffix"; then
f=$RESULT/${name}-${suffix}.dat
else
f=$RESULT/${name}.dat
fi
local selector="`benchmarkSelector "$name"`";
(
header4 "$prods";
local tmp="$RESULT/tmp-plotOps-$name-$param$tmpext.data"
# old query: jq -r ".[] |  select (.benchmark | contains (\".${name}\") ) | [ (.threads | tostring) + \"-\" + .params.entryCount + \"-\" + .params.$param, .params.cacheFactory, .primaryMetric.score, .primaryMetric.scoreError, .primaryMetric.scoreConfidence[0], .primaryMetric.scoreConfidence[1]  ] | @csv" | \
# old query on primary result
local queryOld=`cat << EOF
.[] | $selector |
  [ (.threads | tostring) + "-" + .params.entryCount + "-" + .params.$param,
    .params.cacheFactory,
    .primaryMetric.score,
    .primaryMetric.scoreError,
    .primaryMetric.scoreConfidence[0],
    .primaryMetric.scoreConfidence[1]
    ] | @csv
EOF
`

local query=`cat << EOF
.[] | $selector |
  [ (.threads | tostring) +  "-" + .params.entryCount + "-" + .params.$param,
     .params.cacheFactory,
     .["secondaryMetrics"]["+misc.requests.throughput"].score,
     .["secondaryMetrics"]["+misc.requests.throughput"].scoreError,
     .["secondaryMetrics"]["+misc.requests.throughput"].scoreConfidence[0],
     .["secondaryMetrics"]["+misc.requests.throughput"].scoreConfidence[1]
  ] | @csv
EOF
`
# request throughput based on AuxCounters
local queryAuxCounters=`cat << EOF
.[] |  select (.benchmark | contains (".${name}") ) |
  [ (.threads | tostring) +  "-" + .params.entryCount + "-" + .params.$param,
     .params.cacheFactory,
     .["secondaryMetrics"]["requests"].score,
     .["secondaryMetrics"]["requests"].scoreError,
     .["secondaryMetrics"]["requests"].scoreConfidence[0],
     .["secondaryMetrics"]["requests"].scoreConfidence[1]
  ] | @csv
EOF
`


test -f "$tmp" || json | \
    jq -r "$query" | \
    sort | tr -d '"' | \
    pivot4 $prods | \
    sort -n -t- -k1,1 -k2,2 -k3,3 | shortenParamValues > "$tmp"
    cat "$tmp" | grep "$filter" | stripEmpty
) > $f
xLabel="threads-size-$param";
if [ "$param" = "none" ]; then
  xLabel="threads-size";
fi
plot --withConfidence $f "${name} / Throughput (higher is better)" "$xLabel" "ops/s"
}

# For one shot benchmarks, not used any more, plot main score, typically runtime
plotRuntime() {
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
local selector="`benchmarkSelector "$name"`";
(
header4 "$prods";
local tmp="$RESULT/tmp-plotOps-$name-$param.data"
test -f "$tmp" || json | \
    jq -r ".[] |  $selector | [ (.threads | tostring) + \"-\" + .params.entryCount, .params.cacheFactory, .primaryMetric.score, .primaryMetric.scoreError, .primaryMetric.scoreConfidence[0], .primaryMetric.scoreConfidence[1]  ] | @csv" | \
    sort | tr -d '"' | \
    pivot4 $prods | sort -n -t- -k1,1 -k2,2 -k3,3 | shortenParamValues > "$tmp"
    cat "$tmp" | grep "$filter" | stripEmpty
) > $f
plot --withConfidence $f "${name} / Runtime (lower is better)" "threads-size" "runtime[s]"
}

noBenchmark() {
local I;
if [[ "$1" = *"-"* ]]; then
  local variant="${1#*-}";
  local benchmark="${1%-*}";
  for I in $RESULT/result-*"$benchmark"*"$variant".json; do
    if test -f $I; then
     return 1;
    fi
  done
  return 0;
else
  for I in $RESULT/result-*"$1"*.json; do
    if test -f $I; then
     return 1;
    fi
  done
  return 0;
fi
}

#
# Merge all results into single JSON file
#
bigJson() {
result=$RESULT/data.json
# A sequence of the lines "]", "[", "]" will be ignored, there may be an empty json file, if a run fails
# A sequence of the lines "]", "[" will be replaced with ","
cat $RESULT/result-*.json | awk '/^]/ { f=1; g=0; next; } f && /^\[/ { g=1; f=0; next; } g { print "  ,"; g=0; } { print; } END { print "]"; }' > $result
}

websiteResult="`cat - << "EOF"
PopulateParallelOnceBenchmark-byThreads-4M
PopulateParallelTwiceBenchmark-byThreads-4M
ZipfianSequenceLoadingBenchmark-byThread-1Mx110
ZipfianSequenceLoadingBenchmark-byThread-1Mx500
ZipfianSequenceLoadingBenchmark-EffectiveHitrate-byThread-1Mx110
ZipfianSequenceLoadingBenchmark-EffectiveHitrate-byThread-1Mx500
ZipfianSequenceLoadingBenchmark-Memory-4-1M-500-liveObjects-sorted
ZipfianSequenceLoadingBenchmark-Memory-4-1M-500-VmHWM-sorted
PopulateParallelClearBenchmark
EOF
`"

websiteResultGrep=`echo "$websiteResult" | awk '{print $0"|"; }'`;

typesetPlainMarkDown() {
(
echo "![](benchmark-result/$1-notitle.svg)"
echo
echo "*$2 ([Alternative image](benchmark-result/$1-notitle-print.svg), [Data file](benchmark-result/$1.dat))*"
echo;
) >> $RESULT/typeset-plain.md
# find substring, see: https://stackoverflow.com/questions/229551/how-to-check-if-a-string-contains-a-substring-in-bash
if [[ "$websiteResultGrep" == *"$1|"* ]]; then
  (
  echo "![](benchmark-result/$1-notitle.svg)"
  echo
  echo "*$2 ([Alternative image](benchmark-result/$1-notitle-print.svg), [Data file](benchmark-result/$1.dat))*"
  echo;
  ) >> $RESULT/website.md
  mkdir -p $RESULT/benchmark-result;
  cp -a $RESULT/$1-notitle.svg $RESULT/$1.dat $RESULT/$1-notitle-print.svg $RESULT/benchmark-result/
fi
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
echo -n > $RESULT/website.md
rm -rf $RESULT/benchmark-result
}

# add graph to result report
graph() {
test -f $RESULT/$1-notitle.svg || return 0;
typesetPlainMarkDown "$1" "$2";
typesetAsciiDoc "$1" "$2";
}

processGraphs() {
for I in PopulateParallelOnceBenchmark PopulateParallelTwiceBenchmark; do
noBenchmark $I || {
    plotRuntime I runtime;
    graph "$graphName" "I";

    for T in 4; do
      plotRuntime $I hitRate "bySize-${T}" "^$T-.*";
      graph "$graphName" "$I, runtime by size for ${T} threads";
    done

    for S in 2M 4M 8M; do
      plotRuntime $I hitRate "byThreads-${S}" "^.*-${S}";
      graph "$graphName" "$I, runtime by thread count for ${S} cache size";
    done
}
done

benchmarks="ReadOnlyBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I hitRate "naive" "" "" "org.cache2k.benchmark.ConcurrentHashMapFactory org.cache2k.benchmark.SynchronizedLinkedHashMapFactory org.cache2k.benchmark.thirdparty.GuavaCacheFactory";
      graph "$graphName" "$I, operations per second with simple cache based on LinkedHashMap";
  }
done

benchmarks="ReadOnlyBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I hitRate "naive2" "" "" "org.cache2k.benchmark.ConcurrentHashMapFactory org.cache2k.benchmark.SynchronizedLinkedHashMapFactory org.cache2k.benchmark.thirdparty.GuavaCacheFactory  org.cache2k.benchmark.PartitionedLinkedHashMapFactory";
      graph "$graphName" "$I, operations per second with simple cache based on LinkedHashMap with single or partitioned locking";
  }
done

benchmarks="ReadOnlyBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I hitRate "" "" org.cache2k.benchmark.ConcurrentHashMapFactory;
      graph "$graphName" "$I, operations per second (complete)";

      for TC in 10 4; do
        for HR in 100 50 33; do
          plotOps $I hitRate "bySize-${TC}x$HR" "^$TC-.*-$HR .*" org.cache2k.benchmark.ConcurrentHashMapFactory;
          graph "$graphName" "$I, operations per second by cache size at $TC threads and $HR percent target hit rate";
        done
      done

      for S in 100K 1M 10M; do
        for HR in 100 50 33; do
          plotOps $I hitRate "byThread-${S}x$HR" "^.*-$S-$HR .*" org.cache2k.benchmark.ConcurrentHashMapFactory;
          graph "$graphName" "$I, operations per second by thread count at $HR percent target hit rate and $S cache size";
        done
      done

      for TC in 10 4; do
        for S in 100K 1M 10M; do
          plotOps $I hitRate "byHitrate-$TC-${S}" "^$TC-$S-.* .*" org.cache2k.benchmark.ConcurrentHashMapFactory;
          graph "$graphName" "$I, operations per second by hit rate at $TC threads and $S cache size";
        done
      done

  }
done

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

THREADS="2 4 8 16 32"
PERCENT="110 500"
MEMORY="100K 1M 10M"

# benchmarks with percent parameter
param=percent;
txt="Zipfian distribution percentage"
benchmarks="ZipfianSequenceLoadingBenchmark ZipfianSequenceBulkLoadingBenchmark ZipfianSequenceLoadingBenchmark-tti"
# benchmarks="ZipfianSequenceLoadingBenchmark-tti"
for I in $benchmarks; do
  echo $I
  noBenchmark $I || {
      plotOps $I $param;
      graph "$graphName" "$I, operations per second by Zipfian $txt (complete)";
      plotEffectiveHitrate $I $param;
      graph "$graphName" "$I, Effective hitrate by $txt (complete)";

     thread=4; size=1M; percent=500;
     plotMem --startIndex 1 --endIndex 1 --variant "-heapCommittedAfterGc" $I $param "$thread-$size-$percent";
     graph "$graphName" "$I, $txt, $thread threads, $size cache entries, $txt $percent, maximum heap committed after regular gc";

     plotMem --startIndex 2 --endIndex 2 --sort --variant "-VmHWM-sorted" $I $param "$thread-$size-$percent";
     graph "$graphName" "$I, $thread threads, $size cache entries, $txt $percent, peak memory usage reported by the operating system (VmHWM), sorted by best performance";

     plotMem --startIndex 5 --endIndex 5 --sort --variant "-liveObjects-sorted" $I $param "$thread-$size-$percent";
     graph "$graphName" "$I, $thread threads, $size cache entries, $txt $percent, total bytes of live objects as reported by jmap";

     # plotMem --startIndex 4 --endIndex 4 --variant "-totalHeapAfterGc" $name $param "$focus";
    # graph "$graphName" "$name, $txt, total heap used after gc";

#     plotMem --startIndex 8 --endIndex 8 --sort --variant "-VmHWM-sorted" $name factor "$thread-$size-$factor";
#     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, peak memory usage reported by the operating system (VmHWM), sorted by best performance";
#     plotMem --startIndex 1 --endIndex 1 --sort --variant "-usedHeap-sorted" $name factor "$thread-$size-$factor";
#     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, used heap memory, sorted by best performance";
#     plotMem --startIndex 9 --endIndex 9 --variant "-allocRate" $name factor "$thread-$size-$factor";
#     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, allocation rate in bytes per second";
#     plotMem --startIndex 10 --endIndex 10 --variant "-allocRatePerOp" $name factor "$thread-$size-$factor";
#     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, allocation rate in bytes per operation";
#     plotStaticPeakMem $name factor "$thread-$size-$factor";
#     graph "$graphName" "$name, $thread threads, $size cache entries, Zipfian factor $factor, static and peak memory usage";

# needs fixing
#      plotMem $I $param;
#      graph "$graphName" "$I, mem by $txt (complete)";

      THREAD_MATCH=".*";
      THREAD_NAME="any";
      for P in $PERCENT; do
        plotOps $I percent "bySize-${THREAD_NAME}x$P" "^$THREAD_MATCH-.*-$P .*";
        graph "$graphName" "$I, operations per second by cache size at $THREAD_NAME threads and Zipfian percentage $P";
        plotEffectiveHitrate $I percent "bySize-${THREAD_NAME}x$P" "^$THREAD_MATCH-.*-$P .*";
        graph "$graphName" "$I, effective hit rate by cache size at $THREAD_NAME threads and Zipfian percentage $P";
        plotScanCount $I percent "bySize-${THREAD_NAME}x$P" "^$THREAD_MATCH-.*-$P .*";
        graph "$graphName" "$I, scan count by cache size at $THREAD_NAME threads and Zipfian percentage $P";
      done

      for S in $MEMORY; do
        for P in $PERCENT; do
          plotOps $I percent "byThread-${S}x$P" "^.*-${S}-$P .*";
          graph "$graphName" "$I, operations per second by thread count with cache size ${S} and Zipfian percentage $P";
          plotEffectiveHitrate $I percent "byThread-${S}x$P" "^.*-${S}-$P .*";
          graph "$graphName" "$I, effective hit rate by thread count with cache size $S  and Zipfian percentage $P";
        done
      done

# no detailed reports
true || {
      for TC in $THREADS; do
        for P in $PERCENT; do
          plotOps $I percent "bySize-${TC}x$P" "^$TC-.*-$P .*";
          graph "$graphName" "$I, operations per second by cache size at $TC threads and Zipfian percentage $P";
          plotEffectiveHitrate $I percent "bySize-${TC}x$P" "^$TC-.*-$P .*";
          graph "$graphName" "$I, effective hit rate by cache size at $TC threads and Zipfian percentage $P";
          plotScanCount $I percent "bySize-${TC}x$P" "^$TC-.*-$P .*";
          graph "$graphName" "$I, scan count by cache size at $TC threads and Zipfian percentage $P";
        done
      done

      for TC in $THREADS; do
          for S in $MEMORY; do
              plotOps $I percent "byPercent-${S}-${TC}" "^$TC-$S-.* .*";
              graph "$graphName" "$I, operations per second by Zipfian percentage with cache size ${S} at $TC threads";
              plotEffectiveHitrate $I percent "byPercent-${S}-${TC}" "^$TC-$S-.* .*";
              graph "$graphName" "$I, effective hit rate by Zipfian percentage with cache size ${S} at $TC threads";
              plotScanCount $I percent "byPercent-${S}-${TC}" "^$TC-$S-.* .*";
              graph "$graphName" "$I, scan count by Zipfian percentage with cache size ${S} at $TC threads";
          done
      done
}
  }
done

# benchmark which have no additional parameter
benchmarks="PopulateParallelClearBenchmark IterationBenchmark"
for I in $benchmarks; do
  noBenchmark $I || {
      plotOps $I none;
      graph "$graphName" "$I, operations per second (complete)";

     param=none;
     thread=16; size=100K;
     plotMem --startIndex 1 --endIndex 1 --variant "-heapCommittedAfterGc" $name $param "$thread-$size";
     graph "$graphName" "$name, $txt, $thread threads, $size cache entries,maximum heap committed after regular gc";

     plotMem --startIndex 2 --endIndex 2 --sort --variant "-VmHWM-sorted" $name $param "$thread-$size";
     graph "$graphName" "$name, $thread threads, $size cache entries, peak memory usage reported by the operating system (VmHWM), sorted by best performance";

      # not relevant
      # plotEffectiveHitrate $I none;
      # graph "$graphName" "$I, Effective hitrate (complete)";

#      plotMemUsed $I factor;
#      plotMemUsedSettled $I factor;
  }
done

}

unusedGraphs() {

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
1-100K-10 (at 1 threads, factor 10)
2-100K-10 (at 2 threads, factor 10)
4-100K-10 (at 4 threads, factor 10)
8-100K-10 (at 8 threads, factor 10)
1-1M-5 (at 1 threads, factor 5)
4-1M-5 (at 4 threads, factor 5)
8-1M-5 (at 8 threads, factor 5)
1-1M-10 (at 1 threads, factor 10)
4-1M-10 (at 4 threads, factor 10)
8-1M-10 (at 8 threads, factor 10)
1-1M-20 (at 1 threads, factor 20)
4-1M-20 (at 4 threads, factor 20)
8-1M-20 (at 8 threads, factor 20)
4-10M-5 (at 4 threads, factor 5)
4-10M-20 (at 4 threads, factor 20)
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

}

# copy result images to cache2k on my workspace
copyToWebsite() {
TARGET=`echo ~/ideaWork/cache2k*/cache2k/src/site/resources/benchmark-result/`
if ! test -d "$TARGET"; then
  echo "Target dir not available, result: $TARGET" ;
  exit 1;
fi
cp -av $RESULT/benchmark-result/* $TARGET/
}

process() {

bigJson;
cleanTypesetting;
# clean tmp files.
rm -f $RESULT/tmp-*;

processGraphs;

(
cd $RESULT;
pandoc -o typeset-plain-markdown.html typeset-plain.md
echo $RESULT/typeset-plain-markdown.html
pandoc -o website-markdown.html website.md
echo $RESULT/website-markdown.html
asciidoctor -o typeset-adoc.html typeset.adoc
echo $RESULT/typeset-adoc.html
)

}

processCommandLine "$@";
