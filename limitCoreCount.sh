#!/bin/bash

# change the CPU core count via the CPU hot plug feature # limiting the 
# CPUs for a process via task set does not work properly. In Java 8, 
# the method getRuntime().availableProcessors() doesn't take that into account.

if test `id -u` -ne 0; then
  exec sudo -E $0 "$@";
fi

cd /sys/devices/system/cpu
for I in cpu[0-9]*; do 
  if [ "cpu0" = "$I" ]; then
    continue;
  fi
  if test "$1" -gt "${I:3}"; then
    echo 1 > $I/online;
  else
    echo 0 > $I/online;
  fi
done
