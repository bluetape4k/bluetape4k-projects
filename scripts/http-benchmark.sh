#!/usr/bin/env bash
#
# HTTP Client 벤치마크 실행 스크립트
#
# kotlinx-benchmark Gradle task 를 실행하고, 결과 JSON 을
# .omc/self-improve/scripts/parse-benchmark.py 로 파싱하여
# 마지막 줄에 `{"primary": <rps>, "clients": {...}}` 을 출력합니다.
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_ROOT}"

MODULE=":bluetape4k-http"
# kotlinx-benchmark 플러그인이 등록한 target 이름이 "test" 이면
# Gradle task 이름은 `<target>Benchmark` = `testBenchmark` 가 됩니다.
TASK="${MODULE}:testBenchmark"
REPORT_DIR="io/http/build/reports/benchmarks"

echo "[http-benchmark] running ${TASK} ..." >&2

./gradlew "${TASK}" --rerun-tasks -q 2>&1 | tail -200 >&2 || {
  echo "[http-benchmark] gradle task failed" >&2
  exit 1
}

# kotlinx-benchmark 는 build/reports/benchmarks/<target>/<timestamp>/<target>.json 에
# JMH json result 를 기록합니다. 가장 최근 파일을 찾아 파서에 전달합니다.
LATEST_JSON="$(ls -1t "${REPORT_DIR}"/*/*/*.json 2>/dev/null | head -n 1 || true)"

if [[ -z "${LATEST_JSON}" ]]; then
  echo "[http-benchmark] no benchmark json result found under ${REPORT_DIR}" >&2
  exit 2
fi

echo "[http-benchmark] result json: ${LATEST_JSON}" >&2

python3 "${REPO_ROOT}/.omc/self-improve/scripts/parse-benchmark.py" "${LATEST_JSON}"
