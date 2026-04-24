#!/usr/bin/env bash
# .kt 테스트 파일 편집 후 해당 모듈의 compileTestKotlin 자동 실행
# asyncRewake: true 와 함께 사용 — 컴파일 실패 시 exit 2로 Claude를 깨움

INPUT=$(cat)
FILE=$(echo "$INPUT" | jq -r '.tool_input.file_path // ""')

# .kt 테스트 파일만 처리
[[ "$FILE" == *.kt ]]         || exit 0
[[ "$FILE" == */src/test/* ]] || exit 0

# 프로젝트 루트 감지
ROOT=$(git -C "$(dirname "$FILE")" rev-parse --show-toplevel 2>/dev/null) || exit 0

# 모듈 디렉토리 추출: <root>/<group>/<module>/src/test/...  →  <module>
RELATIVE="${FILE#$ROOT/}"
MODULE_DIR=$(echo "$RELATIVE" | cut -d'/' -f2)
MODULE=":bluetape4k-$MODULE_DIR"

echo "[$MODULE] compileTestKotlin 검증 중..." >&2

OUTPUT=$(cd "$ROOT" && ./gradlew "$MODULE:compileTestKotlin" 2>&1)

if echo "$OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
    echo "[$MODULE] 컴파일 성공" >&2
    exit 0
else
    echo "[$MODULE] 컴파일 실패 — 오류 목록:"
    echo "$OUTPUT" | grep -E "^e: |^> Task|FAILED" | tail -20
    exit 2
fi
