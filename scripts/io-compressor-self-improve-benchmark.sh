#!/usr/bin/env bash
#
# Runs the IO same-condition compressor self-improvement benchmark through the
# Gradle kotlinx-benchmark task and prints a JSON summary on the last line.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_ROOT}"

MODULE=":bluetape4k-io"
TASK="${MODULE}:testSelfImproveBenchmark"
INCLUDE_REGEX="${BENCHMARK_INCLUDE_REGEX:-.*SameConditionCompressorBenchmark.compress.*}"
PRIMARY_BENCHMARK="${PRIMARY_BENCHMARK:-io.bluetape4k.io.benchmark.SameConditionCompressorBenchmark.compress}"
PRIMARY_PAYLOAD_KIND="${PRIMARY_PAYLOAD_KIND:-*}"
PRIMARY_PAYLOAD_SIZE="${PRIMARY_PAYLOAD_SIZE:-*}"
PRIMARY_COMPRESSOR="${PRIMARY_COMPRESSOR:-gzip,deflate,zstd,lz4}"
REPORT_ROOT="io/io/build/reports/benchmarks/selfImprove"

echo "[io-compressor-self-improve] running ${TASK} include=${INCLUDE_REGEX}" >&2

./gradlew "${TASK}" \
  -PbenchmarkInclude="${INCLUDE_REGEX}" \
  --no-build-cache \
  --rerun-tasks \
  -q >&2

LATEST_JSON="$(find "${REPORT_ROOT}" -type f -name '*.json' -print 2>/dev/null | sort | tail -n 1 || true)"
if [[ -z "${LATEST_JSON}" ]]; then
    echo "[io-compressor-self-improve] no benchmark json result found under ${REPORT_ROOT}" >&2
    exit 2
fi

echo "[io-compressor-self-improve] result json: ${LATEST_JSON}" >&2

python3 "${REPO_ROOT}/scripts/parse-io-compressor-self-improve.py" \
  "${LATEST_JSON}" \
  "${PRIMARY_BENCHMARK}" \
  "${PRIMARY_PAYLOAD_KIND}" \
  "${PRIMARY_PAYLOAD_SIZE}" \
  "${PRIMARY_COMPRESSOR}"
