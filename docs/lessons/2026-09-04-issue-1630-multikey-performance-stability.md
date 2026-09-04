# 이슈 #1630 multi-key lease 성능 특성화 안정화 교훈 (2026-09-04)

관련 이슈: [#1630](https://github.com/bluetape4k/bluetape4k-projects/issues/1630)
· milestone `2.1.0`
영향 module: `:bluetape4k-lettuce`

## 맥락

`LettuceMultiKeyLeasePerformanceTest`는 key 수 32와 8의 acquire latency를 한 번의
측정에서 p95로 비교했다. historical reproduction에서는 두 번 성공한 뒤 한 번
`Expected <13.390084> to be less than or equal to <3.6915>`가 발생했다. 현재
checkout에서 같은 task를 세 번 실행했을 때도 concurrency 16 raw p95 비율이 약
`0.69x`에서 `3.97x`까지 변해 단일 p95 표본 창이 환경 잡음에 민감하다는 가설을
확인했다.

## 결정

1. 기본 `test`와 CI 필수 check는 그대로 두고, 성능 특성화 전용
   `multiKeyLeasePerformanceTest` task에서만 측정한다. Testcontainers 충돌을 막기
   위해 전용 lane은 직렬 실행한다.
2. 각 조합을 독립 측정 3회 수행하고, 매회 warm-up 20회와 measured 300회를 사용한다.
   회귀 기준은 각 회차의 p95를 `median-of-run-p95`로 집계하며 normalized p95 비율
   한도 `4.0`은 완화하지 않는다.
3. acquire/release/probe 샘플 수와 raw run, Redis/Java/Kotlin/Lettuce 버전, CPU 수,
   executor pool, aggregation policy를 JSON 보고서에 남긴다. 예상 가능한 Redis/timeout
   샘플 오류만 집계하고 assertion·취소·interrupt·fatal 오류는 원래 예외로 중단한다.
   회차별 worker는 completion latch로 bounded 종료를 확인하며, 종료하지 않으면 다음
   회차로 진행하지 않고 fatal failure로 중단한다. 초기 Redis version이 유효하면 이후
   조회 실패가 해당 값을 덮어쓰지 않도록 진단 오류만 누적한다.
   assertion 또는 환경 초기화가 실패해도 `status=failed`와 실패 type/message를 원자적으로
   기록한 뒤 원래 예외를 다시 던진다.
4. Gradle `Test` task가 선언한 output 경로를 system property로 테스트에 전달한다.
   이렇게 해야 worktree나 Test worker의 `user.dir` 차이로 보고서가 다른 checkout에
   기록되지 않는다.

## 결과

대표 실행은 Redis `8.8.1`, warm-up `20`, measured `300`, measurement runs `3`,
`median-of-run-p95`로 완료됐다. 집계 결과는 6개 조합, acquire/release 최소 각
`900` 샘플, probe 최소 `188` 샘플이며 errors와 timeouts는 모두 `0`이었다. 보고서는
다음 경로에 생성된다.

`infra/lettuce/build/reports/multi-key-lease-performance/results.json`

보고서에 실패 상태를 직접 유도하는 대신, synthetic result를 사용한 단위 계약 테스트로
`status=failed`와 `failure` 필드를 고정했다. 실제 Redis 회귀 assertion은 여전히
집계 결과에서 실행되므로 threshold를 올려 false green을 만들지 않는다.

## 검증

- RED: 기존 구현에서 measured rounds `100 < 300`, sample-count/aggregation metadata
  누락, failed report metadata 누락을 재현했고 기존 characterization test는 통과했다.
- GREEN 계약: 측정 round, median aggregation, sample/report metadata, 실패 원인 보존,
  JSON escape, non-recoverable error 정책, probe late-failure 재확인, round cleanup primary,
  worker 종료 대기, Redis version fallback 보존을 고정한 계약 테스트 10개가 통과했다.
- 실제 경로: explicit Colima Docker와 shared Testcontainers lock으로
  `:bluetape4k-lettuce:multiKeyLeasePerformanceTest` 통과 (`SUCCESS: Executed 11 tests in
  23.5s`, characterization `21.3s`, `BUILD SUCCESSFUL in 1m 6s`).
- JSON 파싱으로 `status=passed`, `measurementRuns=3`, `rawRunCount=3`, 샘플 수,
  executor metadata, `probeErrorCount=0`, `probeFailure=null`, `redisVersionFailure=null`,
  measurement failure count, errors/timeouts와 두 concurrency의 ratio를 재확인했다. 각
  `MeasurementFailure`에는 `run` 식별자도 포함된다.
- `BLUETAPE4K_MANUAL_ROOT`를 중앙 checkout의
  `docs/manual/bluetape4k-projects`로 지정한 뒤 직접 Gradle 9.7.0 repository
  `build`가 `BUILD SUCCESSFUL in 28m 35s`와 `1062 actionable tasks`로 통과했다.
- 같은 root로 `NearJCacheDocumentationTest` `6/6`과 `:bluetape4k-cache-lettuce:test`
  `461/461`도 별도 재실행해 통과했다. `multiKeyLeasePerformanceTest`는 전체 build에서
  `11/11`로 통과했다.
- `:bluetape4k-lettuce:compileTestKotlin`은 27개 task 기준 통과했고 `git diff --check`도
  통과했다. hosted CI run `33901895924`도 exact head에서 성공했다.

## 놓친 점과 주의사항

- 첫 번째 context-mode 실행은 worktree 인자를 보존하지 않아 원본 checkout의 오래된
  report를 읽게 했다. 보고서 output을 Gradle system property로 고정하고, 이후 명령에
  명시적인 `cd <worktree>`를 사용해 이 증거를 폐기하고 정확한 worktree에서 재실행했다.
- `BLUETAPE4K_MANUAL_ROOT`를 `bluetape4k.github.io/docs/manual`까지만 지정하면
  `NearJCacheDocumentationTest`가 `en/modules/...`를 찾지 못한다. GNO
  `bluetape4k-github`의 Issue #1588과 merged PR #1593, 그리고 저장소 checklist를
  대조해 실제 root가 `docs/manual/bluetape4k-projects`임을 확인하고 재실행했다.
- 로컬 3회 baseline은 모두 통과했으므로 historical 실패를 동일 환경에서 재현하지
  못했다. 대신 issue의 exact assertion과 synthetic RED를 함께 보존했다.
- 전용 성능 task와 report는 준비됐지만 dedicated CI lane 및 required-check 정책은
  아직 구성하지 않았다. 따라서 현재는 release gate가 아니다.
- README 실행 예제에도 shared `bluetape-testcontainers.lock`을 포함해 worktree 간
  Testcontainers 직렬화 약속을 실행 명령과 일치시켰다.

## 향후 지침

- multi-key lease Lua script 또는 `maxKeys`를 변경하면 기본 test만으로 완료 처리하지
  말고 전용 task를 직렬 실행하고 JSON의 raw run과 aggregation metadata를 함께 보관한다.
- 측정 안정성을 이유로 ratio limit을 완화하지 않는다. noise가 계속되면 별도 이슈에서
  executor/CPU/Redis image를 통제하거나 CI 전용 lane을 추가하고, 기존 raw evidence와
  회귀 신호를 비교한다.
- 성능 report writer를 수정할 때는 성공·실패 양쪽 상태, 원자적 이동, primary exception
  보존, output path property 테스트를 함께 검토한다. round worker를 취소할 때는
  `Future.cancel(true)`만 믿지 말고 실제 callable completion을 bounded하게 기다린다.

## 문서 SPW 감사

- SPW-01: PASS — 독자는 Lettuce 성능 task와 CI를 유지보수하는 개발자이며, 실행 명령과
  report schema를 바로 제공한다.
- SPW-02: PASS — 맥락, 결정, 결과, 검증, 놓친 점, 향후 재발 방지 규칙을 포함한다.
- SPW-03: PASS — 한국어 기술 문체를 사용하고 task, option, 식별자, URL, 수치와 exact
  error를 보존했다.
- SPW-04: PASS — RED/GREEN 로그, exact worktree report, parsed JSON, 중앙 manual root
  계약과 hosted CI run `33901895924`를 실행 결과와 대조했다.
- SPW-05: PASS — Markdown 구조, 명령, 링크와 report path를 재검토했다.

## 한국어 자연스러움 감사

- KO-01: PASS — code token, 명령, URL, 수치와 미검증 범위를 보존했다.
- KO-02: PASS — 추상적인 품질 주장을 피하고 historical assertion, 비율 범위와 샘플
  수를 사용했다.
- KO-03: PASS — 원인·결정·주의사항을 직접 서술하고 불필요한 명사화를 줄였다.
- KO-04: PASS — `measurement`, `aggregation`, `report`, `required-check` 용어를
  문서와 README에서 일관되게 사용했다.
- KO-05: PASS — 장식적 비유와 홍보 표현을 사용하지 않았다.
- KO-06: PASS — 제목, 본문, 목록, 링크와 code token을 다시 읽어 확인했다. 이 lesson은
  한국어 단일 문서이므로 별도 locale 대응은 없다.
- KO-07: PASS — `audit-korean-terms.mjs`에서 lesson은 신규 findings 0건이었다. README의
  기존 `snapshot` loanword findings 2건(line 79, 89)은 이번 변경 hunk 밖의 기존 문구로
  남겨 두고 receipt에 범위를 기록한다.
