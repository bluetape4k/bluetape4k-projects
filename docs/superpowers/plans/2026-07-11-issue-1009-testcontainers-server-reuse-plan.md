# Testcontainers Server 재사용 구현 계획

## 목표

모든 Testcontainers Server wrapper의 기본값을 non-reuse로 바꾸되, 명시적인
local reuse와 test JVM별 singleton Launcher는 유지한다.

## 작업

- [x] source-policy regression test를 추가하고 기존 `reuse: Boolean = true`
  기본값 111건에 대해 RED를 확인한다.
- [x] production Server의 모든 `reuse` 기본값을 `false`로 바꾼다.
- [x] `true`가 기본값이라고 설명하는 KDoc을 갱신한다.
- [x] 명시적인 Floci Launcher guard를 유지하고 암시적인
  `withReuse(true)`가 남지 않았는지 확인한다.
- [x] policy, compile, 집중 대표 test, 비례적인 module validation을 순차적으로
  실행한다.
- [x] PR #1010 범위와 DoD를 갱신하고 CI를 기다린 다음, downstream mutex 작업
  전에 대체 `1.11.1-SNAPSHOT`을 게시한다.

## 검증

```shell
./gradlew :bluetape4k-testcontainers:test \
  --tests 'io.bluetape4k.testcontainers.ContainerReusePolicyTest' \
  --no-daemon --no-configuration-cache

./gradlew :bluetape4k-testcontainers:compileKotlin \
  :bluetape4k-testcontainers:compileTestKotlin \
  --no-daemon --no-configuration-cache

rg -n 'reuse\s*:\s*Boolean\s*=\s*true|withReuse\(true\)' \
  testing/testcontainers/src/main/kotlin
```

최종 검색 결과에는 일치 항목이 없어야 한다.

## Evidence

- RED: 구현 전 policy test가 영향받는 파일 52개를 찾았다.
- GREEN: 기본값 111건을 `false`로 바꾼 뒤 policy test를 통과했다.
- Floci, PostgreSQL, Redis, Consul, NATS, Neo4j 대표 integration test를
  통과했다(총 27개 test).
- 전체 module: 7분 3초 동안 449개 test, 실패 0개, skip 25개.
