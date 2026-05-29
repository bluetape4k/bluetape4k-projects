#!/usr/bin/env bash
#
# Run Ktor CIO vs Spring WebFlux benchmark tasks and copy the latest JSON
# reports into docs/benchmarks/raw for issue-backed evidence.
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_ROOT}"

MODULE=":web-framework-benchmark"
REPORT_ROOT="benchmark/web-framework-benchmark/build/reports/benchmarks"
RAW_DIR="docs/benchmarks/raw"
STAMP="$(date +%F)"

mkdir -p "${RAW_DIR}"

./gradlew \
    "${MODULE}:benchmarkStartupBenchmark" \
    "${MODULE}:benchmarkThroughputBenchmark" \
    "${MODULE}:benchmarkLatencyBenchmark" \
    --no-configuration-cache

copy_latest() {
    local config="$1"
    local target="$2"
    local latest

    latest="$(ls -1t "${REPORT_ROOT}/${config}"/*/benchmark.json 2>/dev/null | head -n 1 || true)"
    if [[ -z "${latest}" ]]; then
        echo "[web-framework-benchmark] missing ${config} benchmark JSON" >&2
        exit 2
    fi

    cp "${latest}" "${RAW_DIR}/${STAMP}-web-framework-${target}.json"
    echo "[web-framework-benchmark] copied ${config}: ${RAW_DIR}/${STAMP}-web-framework-${target}.json" >&2
}

copy_latest startup startup
copy_latest throughput throughput
copy_latest latency latency

jq -r '.[] | [.benchmark, .mode, .primaryMetric.score, .primaryMetric.scoreError, .primaryMetric.scoreUnit] | @tsv' \
    "${RAW_DIR}/${STAMP}-web-framework-"*.json
