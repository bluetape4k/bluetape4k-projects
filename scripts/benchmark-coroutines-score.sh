#!/usr/bin/env bash
# Coroutines Flow 벤치마크 실행 및 primary score(기하평균) 출력
# 마지막 줄: {"primary": <score>, "scores": {...}}
set -e
REPO="$(cd "$(dirname "$0")/.." && pwd)"
LOG=$(mktemp /tmp/coroutines-bench-XXXXXX.log)

cd "$REPO"
./gradlew :bluetape4k-coroutines:coroutinesFlowBenchmark --rerun-tasks >"$LOG" 2>&1

python3 - "$LOG" <<'PYEOF'
import sys, re, json, math

log = open(sys.argv[1]).read()
pattern = re.compile(r'CoroutinesFlowBenchmark\.(\w+)\s+thrpt\s+\d+\s+([\d.]+)')
scores = {m.group(1): float(m.group(2)) for m in pattern.finditer(log)}

if not scores:
    print(json.dumps({"primary": 0, "error": "no results parsed"}))
    sys.exit(1)

geo = math.exp(sum(math.log(v) for v in scores.values()) / len(scores))
print(json.dumps({"primary": round(geo, 3), "scores": {k: round(v, 3) for k, v in scores.items()}}))
PYEOF
rm -f "$LOG"
