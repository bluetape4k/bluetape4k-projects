# #1474 bounded admission 구현 계획 검토

## 검토 범위와 기준

- 대상 계획: `docs/superpowers/plans/2026-08-23-issue-1474-bounded-admission-plan.md`
- 기준 설계: `docs/superpowers/specs/2026-08-23-issue-1474-bounded-admission-design.md`
- 설계 검토: `docs/review/2026-08-23-issue-1474-bounded-admission-spec-review.md`
- 저장소 규칙: `AGENTS.md`, `bluetape-workflow`, `bluetape-full-feature`,
  `bluetape-kotlin-patterns`, `kotlin-coroutines-skill`
- 검토 방식: Performance, Stability, Security, Operator/Ops, Developer/API,
  User/Caller 여섯 관점과 main-session 통합

## 통합 판정

| 우선순위 | 관점 | 근거 | 조치 | 재검토 |
| --- | --- | --- | --- | --- |
| P0 | 전체 | 승인 설계의 범위, 비범위, 중지 조건이 계획에 반영됨 | 없음 | main 통합 완료 |
| P1 | 전체 | 구현 순서가 worktree → RED → GREEN → module gate → static/API → review/commit → verification으로 닫힘 | 없음 | main 통합 완료 |
| P2 | Performance | admission 이전에 batch 사본을 만드는 기존 의미를 유지하므로 overflow에서도 entry 사본 할당은 발생함 | payload 크기 상한은 이번 범위 밖으로 고정하고, finite burst에서 child 수·accepted 호출 수만 측정 | Task 2, 6 |
| P2 | Stability | `CoroutineStart.LAZY`가 시작되지 않는 close/취소 경로에서 permit이 stranded될 수 있음 | 취소된 scope의 두 callback 테스트로 `job.start() == false` caller-side release를 결정적으로 검증하도록 계획 보강 | Task 2 Step 4 |
| P2 | Stability | child의 일반 예외와 `CancellationException`은 서로 다른 sibling/lifecycle 의미를 가짐 | 일반 예외 sibling 격리 테스트를 별도 단계로 추가하고 기존 cancellation 테스트와 분리 | Task 2 Step 3, 5 |
| P2 | Security | overflow log가 raw key/value/source를 포함하면 기존 redaction 계약을 깨뜨림 | `maxInFlightCallbacks = 1`의 포화 callback에 secret token을 넣고 operation/cache id/cap만 남는지 검증 | Task 2 Step 6 |
| P2 | Operator/Ops | public metric 또는 runtime tuning surface가 없으면 overflow 규모를 운영 중 직접 계측할 수 없음 | 승인 설계대로 sanitized debug log만 제공하고 metric backend·public configuration은 별도 후속 범위로 유지 | Task 3, 6 |
| P2 | Developer/API | public one-argument constructor와 provider registration을 건드리면 누적 train의 ABI 경계가 흔들림 | cap은 top-level private 기본값과 `@JvmSynthetic internal forTest`에만 두고, `javap` 및 near-cache contract gate를 실행 | Task 3–5 |
| P2 | User/Caller | overflow가 durable delivery로 오해될 수 있음 | Korean KDoc에 non-blocking admission, 기본 cap 64, 즉시 거부, close 무대기 의미를 명시하고 README 변경은 N/A로 기록 | Task 3, 5 |

## 여섯 관점별 확인

### Performance

- `tryAcquire()`만 callback thread의 admission 경로에 두고 suspend 또는 내부 queue를
  추가하지 않는다.
- listener별 child job 상한을 injected cap으로 검증하며, batch 내부 entry 수에 새 제한을
  추가하지 않는다.
- `runBlocking`, `Thread.sleep`, 전역 scope, blocking join/drain을 금지하는 source scan이
  Task 6에 있다.
- payload 할당량 benchmark는 승인 설계의 비범위이며, 이 판단을 P2 disposition으로
  기록했다.

### Stability

- `finally` permit 반환, `CancellationException` 재전파, `SupervisorJob` sibling 격리,
  `close()`의 cancel-only 동작을 각각 테스트한다.
- 취소된 scope 테스트가 lazy child 미시작 경로를 덮어 close 경쟁에서의 이중 반환과
  permit 고갈을 구분한다.
- Testcontainers 변경은 없고, full module test는 저장소의 순차 실행 규칙을 따른다.

### Security

- 기존 trace/error log와 새 overflow debug log 모두 sanitized cache id와 operation 같은
  low-cardinality 값만 기록한다.
- secret key/value/source token 부재를 `ListAppender`로 검증하며 event payload의
  `toString()`을 로그에 넣지 않는다.

### Operator/Ops

- overflow는 조용한 성공으로 포장하지 않고 debug log로 관찰 가능하게 한다.
- metric backend, public cap tuning, release/dispatch는 범위 밖이며 계획의 N/A와
  explicit exclusions에 기록되어 있다.
- 실패 시 구현 commit만 branch-local revert하고 `develop`은 변경하지 않는 rollback이
  Task 8에 있다.

### Developer/API

- 기존 source의 `EventCopy`, `associate`, `LinkedHashSet`, `applyEvent` 경계를 유지한다.
- cap 검증은 양수 조건을 요구하고, public JVM constructor descriptor는 `javap`로
  확인한다.
- near-cache 등록 코드는 수정하지 않고 기존 registration/back-first contract test를
  실행한다.
- 새 dependency/module/catalog 등록이 없고 기존 local `Semaphore` 사용 패턴과 맞는다.

### User/Caller

- 기본 64는 내부 정책이며 caller가 조정할 수 있는 public API가 아니다.
- 포화 callback은 best-effort listener의 명시적 거부이며 durable delivery가 아님을
  KDoc과 설계 문서에서 설명한다.
- public usage 예제와 README는 API 변경이 없으므로 변경하지 않는다.

## Step 3-R 필수 조건 확인

| 조건 | 판정 | 계획 근거 |
| --- | --- | --- |
| 모든 설계 수용 기준의 concrete task 매핑 | PASS | Task 2–6 및 Task 8 최종 DoD |
| 실행 가능한 task 순서 | PASS | worktree 생성 후 RED에서 test seam을 추가하고 GREEN에서 source를 변경 |
| 후속 산출물에 대한 선행 의존성 없음 | PASS | review/commit과 verification worktree는 implementation head 이후에 실행 |
| 성공·실패·edge·concurrency·coroutine·lifecycle·backend 경로 | PASS | listener tests, cancellation/close/race, near-cache registration contract |
| 구체적인 검증 명령 | PASS | `repo-test-summary`, `detekt`, `build`, `javap`, `git diff --check`, terminology audit |
| README/KDoc 및 localized 문서 판단 | PASS | KDoc 수정, README/localized README N/A 근거 기록 |
| 모듈/BOM/CI/coverage/Testcontainers 판단 | PASS | module/dependency/catalog/CI/Kover/container 변경 N/A, full module gate 실행 |
| cancellation/dispatcher 경계 | PASS | `scope.launch`, `SupervisorJob`, `CancellationException`, cancel-only close |
| 성능·안정성 및 자원 정리 | PASS | cap/count barrier, permit release 두 경로, blocking 금지 source scan |
| rollback·compatibility·migration | PASS | ABI/provider gate와 branch-local revert, reapproval 중지 조건 |

## 수정 및 재검토 이력

main-session 통합에서 다음 계획 결함을 즉시 보완했다.

1. plan의 writer audit 경로 오탈자와 trailing whitespace를 실제 설치 경로로 수정했다.
2. overflow-redaction 테스트의 cap을 `1`로 명시하여 두 번째 callback이 실제로
   overflow되는 조건을 고정했다.
3. 일반 예외가 sibling을 취소하지 않는 회귀 테스트를 추가했다.
4. 취소된 scope에서 lazy child가 시작되지 않는 경로의 caller-side permit 반환 테스트를
   추가했다.

수정 후 plan은 placeholder/path scan, `git diff --check`, Korean terminology audit를
통과했다. P0=0, P1=0이며 P2는 위 표의 범위·검증 조치로 모두 처분되었다.

## 결론

계획은 구현에 전달할 수 있다. 다음 단계는 `feat/issue-1474-bounded-admission`
worktree를 정확한 plan head에서 만들고, 계획된 RED 테스트를 먼저 실행하는 것이다.
PR 생성과 merge는 이 검토의 범위가 아니며 fresh authority와 별도 exact-head 승인이
필요하다.

### 검토 DoD

- [x] 여섯 관점 검토 및 main-session 통합
- [x] P0/P1 없음
- [x] P2 처분 및 계획 반영
- [x] 설계 수용 기준과 verification command 매핑
- [x] rollback, ABI, provider registration, README/KDoc 경계 확인
- [x] plan self-review, diff check, Korean terminology audit
