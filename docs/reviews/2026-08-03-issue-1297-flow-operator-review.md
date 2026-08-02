# Issue #1297 Flow 연산자 동등성 최종 리뷰

## 범위와 판정

- 작업 유형: Type-A Full Feature — 공개 Flow 연산자 API, 수명주기 의미,
  제한된 concurrency, 양국어 문서, benchmark 범위
- 상위 issue: #1297 (`enhancement(coroutines): add high-value RxJava/Reactor-style Flow operators`)
- 후속 issue: #1300 (`evaluate delay-error and explicit overflow Flow policies`)
- 기준점: `origin/develop`의 `f47da3e0da0e98da86d3361be5743ea74679f09c`
- 현재 구현 head: `2dbb7c80b`
- 범위: `bufferTimeout`, `windowTimeout`, `timeout`, `timeoutOrFallback`, 제한된
  `concatMapEager`. 외부 runtime adapter나 새 dependency는 포함하지 않는다.

## Workflow gate 근거

| Gate | 결과 | 근거 |
|---|---|---|
| WF-01 | PASS | Issue #1297 live metadata, Type-A 신호, 명시적 비목표를 설계와 계획에 기록했다. |
| WF-02/WF-03 | PASS | `docs/superpowers/plans/2026-08-03-flow-operator-parity-plan.md`의 순서화된 계획을 승인받았고 mutation 전에 사용자 승인을 기록했다. |
| WF-04/04A | PASS | `bluetape-workflow`, `bluetape-kotlin-patterns`, `bluetape-writer`, common gate를 읽고 receipt run `20260802T191529Z-efc41fe5`를 초기화했다. |
| CG-01–CG-05 | PASS | 격리 worktree, feature branch, 최신 AGENTS 정책, GNO/live issue 근거, 저장소 Kotlin pattern을 검증했다. |
| CG-06 | PASS | 공개 한국어 우선 KDoc, 일치하는 `README.md`/`README.ko.md` section, inventory matrix, 후속 issue 링크를 추가했다. |
| CG-07/CG-08 | PASS | RED/GREEN targeted test, 전체 module test/check, 순차 benchmark를 완료했다. |
| CG-09 | PASS | 재사용 가능한 수명주기, 가상 시간, 제한된 queue, benchmark 교훈을 commit된 lesson 문서에 기록했다. |
| CG-10 | PASS | Review/lesson commit 전에 P0=0/P1=0과 정확한 local head `2dbb7c80b`로 최종 diff/범위 리뷰를 수렴했다. |

## 계약 리뷰

### `bufferTimeout` / `windowTimeout`

- 수집 전에 양수인 `maxSize`와 `Duration`을 검증한다.
- Timer는 구독 시점이 아니라 batch/window의 첫 항목에서 시작한다.
- 개수와 timeout 경계는 비어 있지 않은 snapshot을 방출한다. 정상 완료 시 대기
  중인 부분 스냅숏을 방출하고 upstream 실패 시 해당 스냅숏을 폐기하면서
  원래 예외를 보존한다.
- `onReceiveCatching`을 `onTimeout`보다 먼저 등록하여 같은 가상 시각의 경합에서
  수신 값이 우선하는 문서화된 규칙을 결정적으로 만든다.
- `windowTimeout`은 실시간 단일 소비자 channel이 아니라 `List.asFlow()`를 통한
  반복 가능한 cold snapshot을 제공한다.
- Upstream producer cancellation은 `finally`에서 수행하며 downstream
  cancellation을 일반 실패로 변환하지 않는다.

### `timeout` / `timeoutOrFallback`

- 유휴 timer는 수집과 함께 시작하고 값을 방출할 때마다 다시 시작한다.
- 완료와 값 수신을 timeout보다 먼저 등록하여 정상 완료 경합 계약을 보존한다.
- Timeout cancellation은 명시적 upstream `Job`과 buffered `Channel`을 사용한다.
  Timeout 예외를 던지거나 fallback 수집을 시작하기 전에 `cancelAndJoin`으로
  upstream 정리를 완료한다.
- 호출자 cancellation은 `CancellationException`으로 유지하고 upstream 및
  fallback 실패는 다시 작성하지 않는다.
- Fallback은 upstream 정리 후 정확히 한 번만 수집한다.

### Bounded `concatMapEager`

- 추가한 overload는 `maxConcurrency > 0`과 `bufferCapacity >= 0`을 검증한다.
- `Semaphore`는 활성 내부 collector 수를 제한하고 내부 항목별 `Channel`은 대기
  출력을 제한한다. 용량 `0`은 명시적인 rendezvous 정책이다.
- 뒤의 내부 작업이 먼저 완료되어도 drain 순서는 원본 순서를 유지한다.
- Transform 실패, 내부 실패, downstream cancellation은 구조화된 child
  cancellation을 사용하며 `finally`에서 permit과 channel을 해제하고 닫는다.
- 호환성을 위해 기존 overload는 기존 `ConcurrentLinkedQueue` 경로를 유지한다.
  새 overload만 제한된 queue 계약을 제공한다.

## 검증 근거

- `./gradlew :bluetape4k-coroutines:test --no-configuration-cache --console=plain`
  — PASS, `SUCCESS: Executed 610 tests in 15.5s`.
- `./gradlew :bluetape4k-coroutines:check --no-configuration-cache --console=plain`
  — PASS, `BUILD SUCCESSFUL`, exit `0`.
- 대상 연산자 테스트 — PASS: buffer/window `8`, timeout/fallback `7`,
  bounded plus legacy concat `9`, zero failures/errors.
- `./gradlew :bluetape4k-coroutines:testCoroutinesFlowBenchmark
  --no-configuration-cache --console=plain` — PASS, ten benchmark methods
  generated/executed, including all three newly added parity cases.
- `git diff --check` — PASS.
- 새 구현/테스트 파일에는 `GlobalScope`, `runBlocking`, `System.nanoTime`,
  blocking sleep, 분리된 dispatcher 사용이 없다.

## 리뷰 관점과 지적

API/계약, concurrency, cancellation, 테스트, 문서, release/운영성이라는 관점
6개를 순차 검토했으며 모두 같은 결과로 수렴했다.

- P0(치명적): 0
- P1(높음): 0
- P2(중간): #1300으로 보류한 1건. Delay-error 의미와 명시적 overflow 정책에는
  별도의 호환성 결정과 외부 runtime matrix가 필요하다.
- P3(낮음): 0

해결되지 않은 local review blocker는 없다. 외부 Redisson/RxJava/Reactor
client와 실제 production 상호 운용성은 의도적으로 이 issue 범위에서 제외한다.
Local Flow 테스트로 추정하지 않고 후속 issue의 `PENDING` 항목으로 유지한다.

## 최종 판정

PR train을 게시하면 구현은 local 기준 merge-ready다. 정확한 PR head에서 GitHub
check/review를 확인하고 최신 merge 승인을 받을 때까지 CG-14부터 CG-16은
PENDING이다. 이 리뷰에는 merge, branch 삭제, integration worktree mutation을
포함하지 않는다.
