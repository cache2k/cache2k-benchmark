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

# set -x
# set -e

RESULT="target/benchmark-result";
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
for I in $RESULT/*.csv; do
  cat $I;
done
}

printImplementations() {
printCacheCsv | awk -F "|" '{print $2; }' | sort | uniq
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


highContrast() {
# http://colorbrewer2.org/....
# quantitative 12 set with b/w bars mixed in
cnt=1;
colors="#a6cee3 #1f78b4 #b2df8a #33a02c #e31a1c #fb9a99 #fdbf6f #ff7f00 #cab2d6 #6a3d9a #888888 #ffff99 #b15928 #000000"

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
highContrast;
# echo "i = 22";
# echo "plot '$in' using 2:xtic(1) ti col, '' u 3 ti col, '' u 4 ti col";
#if [ "`cat $in | wc -l`" -lt 3 ]; then
#  echo  -n "plot '$in' using 2 ti col";
#else
  echo  -n "plot '$in' using 2:xtic(1) ti col ls 1";
#fi
cols=$(( `head -n 1 "$in" | wc -w` ));
  idx=3;
  while [ $idx -le $cols ]; do
    echo -n ", '' u $idx ti col ls $(( $idx - 1 ))";
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

traces="Web07 Web12 Cpp Sprite Multi2 Oltp Zipf900 Zipf10k TotalRandom1000 \
       UmassWebSearch1 UmassFinancial1 UmassFinancial2 \
       OrmAccessBusytime OrmAccessNight Glimpse ScarabRecs ScarabProds";

clean() {
rm -rf $RESULT/*.dat;
rm -rf $RESULT/*.svg;
rm -rf $RESULT/*.plot;
}

process() {
clean;
header="Size OPT LRU CLOCK EHCache3 Guava Caffeine cache2k RAND";
impls="org.cache2k.benchmark.thirdparty.CaffeineSimulatorOptBenchmark \
	   org.cache2k.benchmark.LruCacheBenchmark \
       org.cache2k.benchmark.ClockCacheBenchmark \
       org.cache2k.benchmark.thirdparty.EhCache3Benchmark \
       org.cache2k.benchmark.thirdparty.GuavaCacheBenchmark \
       org.cache2k.benchmark.thirdparty.CaffeineBenchmark \
       org.cache2k.benchmark.Cache2kDefaultBenchmark \
       org.cache2k.benchmark.RandomCacheBenchmark";
for I in $traces; do
  f=$RESULT/trace${I}hitrateProducts.dat;
  (
  echo $header;
  printHitrate | grep "^benchmark${I}_" | alongSize | sort -n -k1 -t'|' | \
    pivot $impls | \
    stripEmpty
  ) > $f
  plot $f "Hitrates for $I trace";
done



}

processExtensive() {
# S/WTLfu S/WTLfu90
header="Size OPT LRU S/LRU CLOCK Cache2k ARC CAR S/Lirs EHCache2 Guava Caffeine S/Mru S/Lfu RAND";
#         org.cache2k.benchmark.thirdparty.CaffeineSimulatorWTinyLfuBenchmark \
#        org.cache2k.benchmark.thirdparty.CaffeineSimulatorWTinyLfu90Benchmark \
impls="org.cache2k.benchmark.thirdparty.CaffeineSimulatorOptBenchmark \
	org.cache2k.benchmark.LruCacheBenchmark \
        org.cache2k.benchmark.thirdparty.CaffeineSimulatorLruBenchmark \
        org.cache2k.benchmark.ClockCacheBenchmark \
        org.cache2k.benchmark.ClockProPlusCacheBenchmark \
        org.cache2k.benchmark.ArcCacheBenchmark \
        org.cache2k.benchmark.CarCacheBenchmark \
        org.cache2k.benchmark.thirdparty.CaffeineSimulatorLirsBenchmark \
        org.cache2k.benchmark.thirdparty.EhCache2Benchmark \
        org.cache2k.benchmark.thirdparty.GuavaCacheBenchmark \
        org.cache2k.benchmark.thirdparty.CaffeineBenchmark \
        org.cache2k.benchmark.thirdparty.CaffeineSimulatorMruBenchmark \
        org.cache2k.benchmark.thirdparty.CaffeineSimulatorLfuBenchmark \
        org.cache2k.benchmark.RandomCacheBenchmark";
for I in $traces; do
  f=$RESULT/trace${I}hitrate.dat;
  (
  echo $header;
  printHitrate | grep "^benchmark${I}_" | alongSize | sort -n -k1 -t'|' | \
    pivot $impls | \
    stripEmpty
  ) > $f
  plot $f "Hitrates for $I trace";
done
header="Size OPT LRU CLOCK EHCache2 Guava Caffeine Caffeine- cache2k RAND";
impls="org.cache2k.benchmark.thirdparty.CaffeineSimulatorOptBenchmark \
	   org.cache2k.benchmark.LruCacheBenchmark \
       org.cache2k.benchmark.ClockCacheBenchmark \
       org.cache2k.benchmark.thirdparty.EhCache2Benchmark \
       org.cache2k.benchmark.thirdparty.GuavaCacheBenchmark \
       org.cache2k.benchmark.thirdparty.CaffeineBenchmark \
       org.cache2k.benchmark.thirdparty.CaffeineRegularBenchmark \
       org.cache2k.benchmark.Cache2kDefaultBenchmark \
       org.cache2k.benchmark.RandomCacheBenchmark";
for I in $traces; do
  f=$RESULT/trace${I}hitrateProductsCaffeineRegular.dat;
  (
  echo $header;
  printHitrate | grep "^benchmark${I}_" | alongSize | sort -n -k1 -t'|' | \
    pivot $impls | \
    stripEmpty
  ) > $f
  plot $f "Hitrates for $I trace";
done
}

processExp() {
header="Size c2k-0809 c2k-1.0 Caffeine=";
impls="cache2k-1.1-20170809 \
       cache2k-1.0 \
       org.cache2k.benchmark.thirdparty.CaffeineRegularBenchmark";
for I in $traces; do
  f=$RESULT/trace${I}exp.dat;
  (
  echo $header;
  printHitrate | grep "^benchmark${I}_" | alongSize | sort -n -k1 -t'|' | \
    pivot $impls | \
    stripEmpty
  ) > $f
  plot $f "Hitrates for $I trace";
done
}

processFour() {
header="Size OPT LRU CLOCK EHCache3 Guava Caffeine cache2k RAND";
impls="org.cache2k.benchmark.thirdparty.CaffeineSimulatorOptBenchmark \
	   org.cache2k.benchmark.LruCacheBenchmark \
       org.cache2k.benchmark.ClockCacheBenchmark \
       org.cache2k.benchmark.thirdparty.EhCache3Benchmark \
       org.cache2k.benchmark.thirdparty.GuavaCacheBenchmark \
       org.cache2k.benchmark.thirdparty.CaffeineBenchmark \
       org.cache2k.benchmark.Cache2kDefaultBenchmark \
       org.cache2k.benchmark.RandomCacheBenchmark";
for I in $traces; do
  f=$RESULT/trace${I}hitrateProductsCaffeineRegular.dat;
  (
  echo $header;
  printHitrate | grep "^benchmark${I}_" | alongSize | sort -n -k1 -t'|' | \
    pivot $impls | \
    stripEmpty
  ) > $f
  plot $f "Hitrates for $I trace";
done
}

processPresent() {
impls="`printImplementations`";
header="Size "$impls;
for I in $traces; do
  f=$RESULT/trace${I}hitrateProductsPresent.dat;
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

