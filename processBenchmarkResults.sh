#!/bin/bash

# processBenchmarkResults.sh

# Copyright 2013 headissue GmbH, Jens Wilke

# This script processes the benchmark results into svg graphics. Needed tools: gnuplot and xml2. 
# 
# Run it with the following subcommands: 
#
# copyData    copy the data from the benchmark run to the target directory
# process     paint nice diagrams
# copyToSite  copy the diagrams and all results the the cache2k project site

RESULT="target/benchmark-result";
SITE="../cache2k/src/site/resources/benchmark-result";

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
    echo "Run with: processBenchmarkResults.sh process";
  else
   "$@";
  fi
}

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
BEGIN { FS="|";
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

# Extract junit benchmarks result by parsing the log.
# We use the workaround, since in the result XML file there is only the
# result of the last JVM fork. However, there is one decimal precision less.
# Format example:
# benchmarkRandmonThreads2|org.cache2k.benchmark.ArcCacheBenchmark|13.12|0.56|3
# benchmarkRandmonThreads4|org.cache2k.benchmark.ArcCacheBenchmark|40.21|0.34|3
# benchmarkRandmonThreads6|org.cache2k.benchmark.ArcCacheBenchmark|67.25|1.16|3
#
printJubCsv() {
cat $RESULT/benchmark.log | awk "$extract_jub_csv_from_log_awk"
}

printCacheCsv() {
for I in $RESULT/*-cache2k-benchmark-result.csv; do
  cat $I;
done
}

onlySpeed() {
grep -v "^benchmark.*_.*" | grep -v "^test"
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
}

printMemory() {
  printCacheCsv | awk -F\| '{ print $1"|"$2"|"$9; }'
}

alongSize() {
  awk -F\| '{ print $4"|"$2"|"$3; }';
}

printHitrate() {
  printCacheCsv;
}

process() {
rm -rf $RESULT/*.dat;
rm -rf $RESULT/*.svg;
rm -rf $RESULT/*.plot;

header="Size OPT LRU S/LRU CLOCK CP+ ARC CAR S/Lirs EHCache Infinispan Guava Caffeine RAND";
impls="org.cache2k.benchmark.thirdparty.CaffeineSimulatorOptBenchmark \
	org.cache2k.benchmark.LruCacheBenchmark \
        org.cache2k.benchmark.thirdparty.CaffeineSimulatorLruBenchmark \
        org.cache2k.benchmark.ClockCacheBenchmark \
        org.cache2k.benchmark.ClockProPlusCacheBenchmark \
        org.cache2k.benchmark.ArcCacheBenchmark \
        org.cache2k.benchmark.CarCacheBenchmark \
        org.cache2k.benchmark.thirdparty.CaffeineSimulatorLirsBenchmark \
        org.cache2k.benchmark.thirdparty.EhCache2Benchmark \
        org.cache2k.benchmark.thirdparty.InfinispanCacheBenchmark \
        org.cache2k.benchmark.thirdparty.GuavaCacheBenchmark \
        org.cache2k.benchmark.thirdparty.CaffeineBenchmark \
        org.cache2k.benchmark.RandomCacheBenchmark";
for I in Web07 Web12 Cpp Sprite Multi2 Oltp Zipf900 TotalRandom1000; do
  f=$RESULT/trace${I}hitrate.dat;
  (
  echo $header;
  printHitrate | grep "^benchmark${I}_" | alongSize | sort -n -k1 -t'|' | \
    pivot $impls | \
    stripEmpty
  ) > $f
  plot $f "Hitrates for $I trace";
done
}

processCommandLine "$@";

