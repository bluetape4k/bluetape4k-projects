# Issue #1297 Step 3-R 계획 리뷰

## 리뷰 범위

- 계획: `docs/superpowers/plans/2026-08-03-flow-operator-parity-plan.md`
- 명세 기준: `docs/superpowers/specs/2026-08-03-flow-operator-parity-design.md`
- 저장소: `bluetape4k-projects`, 격리 branch `feat/issue-1297-flow-operators`
- 기준점: `origin/develop`의 `f47da3e0da0e98da86d3361be5743ea74679f09c`.
  구현 전 `:bluetape4k-coroutines:test` 기준 검증이 통과했다.
- 실행 방식: 필수 계획 관점 6개를 main session에서 순차 검토하고 workflow
  receipt의 `main-review` fallback lane에 기록했다. Subagent 또는 병렬 구현
  lane은 사용하지 않았다.

## 필수 계획 검사

| # | 필수 검사 | 근거 | 결과 |
|---:|---|---|---|
| 1 | 모든 명세 요구사항과 DoD가 구체적인 task에 대응한다 | Task 1-5는 inventory, API 계열 3개, 문서, 테스트, benchmark, 후속 작업을 다루고 Task 6은 전체 검증, lesson, PR train, exact-head DoD를 다룬다. | PASS |
| 2 | 현재 코드에서 task 순서를 구현할 수 있다 | Task 1에서 중복 검색과 inventory를 먼저 고정하고 Task 2-4에서 코드를 작성한다. Task 5에서 완성된 API를 문서화하고 Task 6에서 통합 branch를 검증한다. | PASS |
| 3 | 뒤의 산출물에 선행 의존하는 task가 없다 | 각 테스트는 구현 전에 RED이며 README 예제는 최종 signature임을 명시하고 API task 뒤에 확정한다. 최종 리뷰는 모든 slice 뒤에 수행한다. | PASS |
| 4 | 테스트가 성공, 실패, 경계, 동시성, coroutine, 수명주기, backend capability를 다룬다 | 개수/시간, 부분 완료, upstream/fallback 오류, 잘못된 인자, 같은 시각 경합, `take(1)` 정리, 제한된 동시성/순서, child 실패를 명시했다. Module-local Flow 변경이므로 backend/Testcontainers는 명시적으로 N/A다. | PASS |
| 5 | 검증 명령이 구체적이고 대상이 명확하다 | 각 slice에 정확한 Gradle `--tests` 명령이 있고 Task 6에 module test/check/benchmark와 `git diff --check`가 있다. | PASS |
| 6 | README가 공개 동작을 다룬다 | Task 1과 Task 5에서 `README.md`와 `README.ko.md`를 동일한 예제와 경계 의미로 수정한다. | PASS |
| 7 | 한국어 KDoc과 영문 공개 GitHub 산출물 | Task 5는 한국어 우선 KDoc을 요구하고 Task 1은 영어 후속 issue 본문을 사용한다. Task 6은 `## DoD Status`로 끝나는 영어 PR metadata를 요구한다. | PASS |
| 8 | 새 module 등록/BOM/CI/resource | Module을 추가, 이동, 게시하지 않는다. Settings/catalog/dependency 범위를 변경하지 않음을 계획에 명시했다. | 범위 근거로 N/A |
| 9 | Spring Boot auto-configuration guard | Spring Boot module이나 auto-configuration을 수정하지 않는다. | N/A |
| 10 | Exposed deprecated import/receiver shadowing | Exposed module이나 receiver API를 수정하지 않는다. | N/A |
| 11 | Coroutine cancellation과 dispatcher 경계 | 실행 규칙에서 dispatcher 전환, blocking 호출, `GlobalScope`, 분리된 child를 금지한다. 테스트는 `runTest`, `finally` marker, cancellation assertion을 사용한다. | PASS |
| 12 | 성능/안정성: 할당, blocking, 정리, backpressure, Testcontainers | 공용 list collector, 가상 시간 timer, 제한된 내부 channel, semaphore permit, `finally` 정리, benchmark method를 명시했다. Polling/blocking 또는 Testcontainers 경로는 추가하지 않는다. | PASS. Testcontainers N/A |
| 13 | Module 간 중복/재사용 결정 | 표준 `flatMapLatest`, `buffer`, `conflate`, `combine`, `zip`, `retryWhen`은 대응 항목 또는 비목표로 유지하고 내부 개수/시간 collector 하나가 선택한 API 둘을 지원한다. | PASS |
| 14 | Rollback/호환성/이전 위험 | 기존 `concatMapEager`를 유지하고 새 API는 추가형이다. Cold 스냅숏 window와 첫 항목 timer 의미를 문서화하며 각 slice를 독립 commit으로 만들어 stacked rollback이 가능하다. | PASS |

## 관점별 통합 결과

| 관점 | 수정 후 결과 | 우선순위 | 판정 |
|---|---|---:|---|
| 성능 | Benchmark 범위가 timer 등록/list 할당과 가상 시간 timer 발생을 구분한다. | P1 해결 | PASS |
| 안정성 | 같은 시각의 수신/timeout 우선순위와 producer/fallback 정리에 정확한 테스트와 구현 순서가 있다. | P1 해결 | PASS |
| 보안 | 새 신뢰 경계, credential, persistence, deserialization, network 경로가 없다. | N/A | PASS |
| 운영 | 배포/configuration 범위가 없으며 외부 runtime 상호 운용성은 검증하지 않은 위험으로 명시한다. | P2 보류 | 문서화된 공백 전제 PASS |
| 개발자/API | Task 경계, overload 호환성, validation, import, 정확한 명령을 명시했다. | P1 해결 | PASS |
| 사용자/호출자 | README/KDoc, 이전 안내, 표준 Flow 대응, 오용 테스트를 명시했다. | P1 해결 | PASS |

## 종료 전 완료한 필수 수정

1. Issue 본문의 literal `\n` escaping을 shell `$'...'` quoting으로 바꾸고 생성
   후 assignee/milestone 검증을 명시했다.
2. 잘못된 인자, 같은 시각 경합, 반복 가능한 window, cancellation marker의
   정확한 테스트 case를 추가했다.
3. `TimeoutException` import/type 요구사항과 benchmark `days` import 요구사항을
   추가했다.
4. Dispatcher 전환 금지, blocking 금지, 분리된 child 금지 제약을 명시했다.

## 판정

- P0: **0**
- P1: 위 수정 후 **0**
- P2: **1건 보류**(외부 Rx/Reactor runtime 상호 운용성, 이 module-local
  slice의 범위가 아님)
- P3: 열린 항목 **0**
- Step 3-R 상태: **PASS**
- 구현 gate: **OPEN**. 이 리뷰와 Step 2-R 근거를 workflow receipt에 기록한
  뒤에만 Task 1을 시작한다.
