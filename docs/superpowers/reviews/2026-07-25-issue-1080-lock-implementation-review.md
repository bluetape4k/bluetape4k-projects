# Issue #1080 Lock Delivery 1 구현 리뷰

## 범위와 게이트

- 대상: `infra/lettuce`의 Lock Delivery 1 구현, 테스트, 영문/한국어 문서, 다이어그램
- 기준: `docs/superpowers/plans/2026-07-25-issue-1080-lettuce-lock-family-plan.md` Task 13
- 통과 조건: 성능, 안정성, 보안, 운영, 개발자/API, 사용자/호출자 6개 독립 렌즈와 주 세션 통합에서
  모두 `P0=0`, `P1=0`
- 교정 원칙: P0/P1만 이번 전달의 차단 결함으로 수정하고, 비차단 P2/P3는 근거와 후속 범위를 남긴다.

## 성능 렌즈

최종 판정:

```text
P0=0
P1=0
```

- `LockPerformanceTest`와 `validate-lock-performance.py`가 warm/cold command budget, bounded retry/watchdog,
  Redis responsiveness, retained-state 보고서를 검증한다.
- P2: `LockPerformanceTest.kt:531-544`가 전체 `tryAcquire`를 측정하고 `:376-381`에서 같은 sample을
  hot-lock wait와 Redis command latency에 모두 기록해 두 값이 중복된다.
- P2: `LockPerformanceTest.kt:360-365`는 waiter/queue cap을 보고하지만 `:558-569`가 두 retained count를
  실제 queued workload 없이 0으로 고정해 queue growth 증거가 약하다.
- P3: scheduler 특성화는 deterministic/manual scheduler에 의존한다.

위 항목은 현재 command/task bound를 깨뜨리는 P0/P1 증거가 아니므로 Delivery 1 차단 항목으로 올리지 않았다.

## 안정성 렌즈

초기 리뷰 판정:

```text
P0=0
P1=2
```

- P1: `LockWaitSupport.kt:170-173`은 공개 async acquire future가 cancellation/close로 먼저 끝난 뒤 Redis
  acquire가 성공하면 `Acquired`/`Reentered` handle을 버린다. 이 경로를 사용하는
  `LockCommandExecutor.kt:198-204`와 `FencedLockScript.kt:896-903`은 반환되지 않은 Lock을 TTL까지 남길 수 있다.
- P1: read/write 전용 async retry의 `ReadWriteLockScript.kt:1157-1165`도 완료된 public future 뒤의 성공
  handle을 정리하지 않는다. `ReadWriteLockScript.kt:1168-1183`의 cancel/close 처리는 queued waiter 제거와
  pending cancel만 수행하므로 reader/writer phase 진행을 TTL까지 막을 수 있다.
- 비교 근거: `MultiLockScript.kt:257-258`은 abandoned acquire를 별도 cleanup 경로로 넘긴다.

두 경로에 best-effort abandoned-acquire release/reconciliation과 회귀 테스트를 추가한 뒤 같은 안정성 렌즈로
재검토한다.

교정 후 근거:

- distributed와 fenced async acquire는 public future가 먼저 종료된 뒤 도착한 성공 handle을 raw release로
  정리한다.
- read/write public mapping과 raw retry completion은 모두 late success cleanup을 실행한다.
- `LockObservationRecorderTest`가 distributed, fenced, read/write의 non-cancellable late completion을
  재현하고 정확히 한 번 release됨을 고정한다.

재리뷰 최종 판정:

```text
P0=0
P1=0
```

## 보안 렌즈

초기 리뷰는 `LockOwnerId`와 `LockRequestId`가 read/write Lua의 `|` 구분자와 NUL digest 경계를 깨뜨릴 수 있는
값을 허용한다는 P1을 발견했다.

교정 근거:

- `LockIdentity.kt`의 공통 생성 경계가 `|`와 모든 ISO control character를 거부한다.
- `LockOwnerId.readResolve()`와 `LockRequestId.readResolve()`도 같은 검증을 다시 실행한다.
- `LockIdentityTest`가 `|`, NUL, newline, tab 거부, 안전한 Unicode 허용, 직렬화 변조 거부를 증명한다.
- distributed, fair, fenced, read/write, spin, multi-lock은 모두 검증된 identity만 ARGV 구성에 사용한다.

재리뷰 최종 판정:

```text
P0=0
P1=0
```

잔여 P2: `CoordinationRuntimeLimits.maxRegistrations`는 task/watchdog 등록에는 적용되지만 Lock object 등록
자체에는 적용되지 않는다. 외부 입력으로 무제한 객체를 만들고 닫지 않는 애플리케이션에 대한 별도 object
cap과 N+1 회귀 테스트를 후속 범위로 남긴다.

## 운영 렌즈

초기 리뷰 판정:

```text
P0=0
P1=1
```

공개 `LockObservationSink`와 카탈로그가 operation/reconcile/integrity, runtime gauge, wait/retry/latency,
watchdog, cleanup 신호를 제공한다고 선언하지만, 실제 Lock family 구현은 주로 capacity rejection과 ownership
loss counter/event만 방출했다. 내부 `CoordinationObserver` 카탈로그 테스트는 공개 sink의 실제 production
방출을 증명하지 못했다. 이 P1은 실제 방출 경계와 production-path 회귀 테스트를 추가한 뒤 같은 운영 렌즈로
재검토한다.

교정 후 근거:

- `LockObservationRecorder`가 runtime, Redis script, caller wait 신호를 공개 Lock observation 카탈로그로
  변환하고 sink 실패를 실행 의미와 분리한다.
- `LockProductionObservationTest`가 실제 Redis와 `SCRIPT FLUSH`를 사용해 모든 public Lock kind의 Redis
  latency, wait latency, retry count, request hold, NOSCRIPT fallback 방출을 검증한다.
- Spin은 distributed registration을 재사용해 같은 logical object를 중복 계수하지 않는다.
- sync/suspend wait time은 observation 시작 전에 검증되며, 회귀 테스트가 invalid 호출 뒤
  `ACTIVE_REQUEST_HOLDS`가 다음 유효 호출에서 정확히 `1 -> 0`으로 복구됨을 증명한다.

재리뷰 최종 판정:

```text
P0=0
P1=0
```

## 개발자/API 렌즈

최종 판정:

```text
P0=0
P1=0
```

- blocking/async/suspend factory와 lifecycle method의 typed result/handle parity, Java-visible factory,
  public/internal package 경계, 기존 primitive의 source compatibility에서 차단 결함이 발견되지 않았다.
- P3: `LockConfig.kt:60`의 `FencedLockConfig(lock, epoch)`는 Java 호출자가 Kotlin default argument를 쓸 수
  없지만 `LettuceLockJavaDocumentationTest.java:33,50`은 config를 인자로만 받아 Java-side construction을
  증명하지 않는다. Java compile fixture와 `ofEpoch`/overload는 후속 polish 후보이다.
- P3: `LettuceFairLock.kt:20-92`를 포함한 일부 wrapper family의 public method-level KDoc이 class KDoc보다
  얇다. 공개 결과 semantics를 각 method에서 바로 찾게 하는 KDoc 보강은 후속 polish 후보이다.

## 사용자/호출자 렌즈

초기 리뷰 판정:

```text
P0=0
P1=1
```

- P1: `LockResult.kt:19-23`의 `Reentered`도 정확히 한 번 해제해야 하는 성공 handle이지만
  `CoordinationLocks.md:50-58,91-99`와 한국어 sibling의 blocking/suspend 예제가 `Acquired`만 해제했다.
- P1: 두 문서의 async 예제 `CoordinationLocks.md:75-80`과 한국어 sibling은 `Ambiguous`를 `else`로 버려
  `LockResult.kt:96-101`의 동일 owner/request reconciliation 의무를 누락했다.
- `LockDocumentationTest.kt:39-107`, `LockApiSurfaceTest.kt:589-602,625-629`,
  `LettuceLockJavaDocumentationTest.java:91-107`도 이 안전한 branch shape을 고정하지 못했다.

영문/한국어 예제와 Kotlin/Java compile fixture가 `Acquired`/`Reentered` release 및 `Ambiguous` reconcile을
모두 수행하도록 교정한 뒤 같은 사용자/호출자 렌즈로 재검토한다.

교정 후 근거:

- blocking: `CoordinationLocks.md:56-63`과 한국어 sibling이 두 성공 handle을 release하고 `Ambiguous`를
  같은 owner/request로 reconcile한 뒤 recovered `Owned` handle도 release한다.
- async: 두 문서의 `:82-92`가 두 성공 handle을 release하고 `Ambiguous`를 `reconcileAsync`로 넘긴 뒤
  recovered `Owned` handle을 `releaseAsync`한다.
- suspend: 두 문서의 `:108-118`이 같은 규약을 유지한다.
- compile/guard: `LockApiSurfaceTest.kt:589-615,649-653`,
  `LettuceLockJavaDocumentationTest.java:91-125`, `LockDocumentationTest.kt:63-75`.

재리뷰 최종 판정:

```text
P0=0
P1=0
```

## 주 세션 통합 증거

- `LockObservationRecorderTest`: 7 tests, 7 passing
- `:bluetape4k-lettuce:test`: 841 tests, 841 passing
- `coordinationLockTopologyRecoveryTest`: 1 test, 1 passing
- `coordinationLockPerformanceTest`: 1 test, 1 passing
- `:bluetape4k-lettuce:check`: success, Kover verification 포함
- `:bluetape4k-lettuce:dokkaGenerate`: success
- root `detekt`: success
- README diagram validator regression: 8 tests, 8 passing
- 정확한 Lock diagram 대상 4개: `total=4 failed=0`
- diagram evidence ledger: `Required checks: 84/84; N/A: 4; Blocked: 0`
- `git diff --check`: clean

## 최종 게이트

| 렌즈 | P0 | P1 |
|---|---:|---:|
| 성능 | 0 | 0 |
| 안정성 | 0 | 0 |
| 보안 | 0 | 0 |
| 운영 | 0 | 0 |
| 개발자/API | 0 | 0 |
| 사용자/호출자 | 0 | 0 |

Lock Delivery 1은 여섯 렌즈와 주 세션 검증에서 차단 결함이 없으므로 PR 준비 게이트를 통과한다.
