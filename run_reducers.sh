#!/bin/bash

set -e

usage() {
  >&2 echo "[!] USAGE: $0 <in> <grammar> <test> <out directory> [<args>] [<reducer>...]"
}

check_exists() { # file
  if [ ! -f "$1" ] ; then
    >&2 echo "[!] file '$1' does not exist"
    usage
    exit 2
  fi
}

if [ $# -lt 4 ] ; then
  usage
  exit 2
fi

readonly IN="$1"
readonly GRAMMAR="$2"
readonly TEST="$3"
readonly OUT_DIR="$4"

check_exists "$IN"
check_exists "$GRAMMAR"
check_exists "$TEST"

if [ $# -gt 4 ] ; then
  readonly ARGS=$5
else
  readonly ARGS=""
fi

if [ $# -gt 5 ] ; then
  shift 5
  reducers=$@
else
  shift 4
  reducers=( \
    "HDD*" \
    "CoarseHDD*" \
    "HDDr*" \
    "GTR*" \
    "Perses*" \
    "Pardis*" \
    "PardisHybrid*" \
  )
fi

readonly run="$(dirname "$0")/run.sh"
check_exists "$run"

for reducer in ${reducers[@]} ; do
  echo "=====[ $reducer ]====="

  out_dir_reducer="$OUT_DIR/$reducer"
  stats_file="$out_dir_reducer/stats.json"
  
  mkdir -p "$out_dir_reducer"

  "$run" --in "$IN" --grammar "$GRAMMAR" --test "$TEST" --reduce "$reducer" \
    --outDir "$out_dir_reducer" --keepIterationResults --statsJSON "$stats_file" $ARGS
done
