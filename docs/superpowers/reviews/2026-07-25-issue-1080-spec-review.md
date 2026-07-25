# Issue #1080 설계 명세 독립 리뷰 기록

## 1. 범위와 결론

- 대상 명세:
  `docs/superpowers/specs/2026-07-25-issue-1080-lettuce-locks-synchronizers-design.md`
- 대상 단계: Type A Full Feature 설계 게이트
- 리뷰 관점: performance, stability, security, operator, developer/API, user/caller
- 최초 리뷰: `P0=0`, `P1=21`
- 교정 후 재검토: 여섯 관점 모두 `P0=0`, `P1=0`
- 결론: 작성된 설계 명세는 사용자 확인 및 후속 implementation plan 작성 단계로 넘길 수 있다.
  Production code, Gradle build 및 Redis integration test는 아직 실행 대상이 아니다.

## 2. 최초 P1과 교정 결과

| 관점 | 최초 | 주요 blocker | 명세 교정 | 재검토 |
|---|---:|---|---|---:|
| Performance | P1=2 | Redis command budget 부재, fair cleanup bound/progress 부재 | warm/cold command budget, queue 10,000 cap, cleanup 64/256 batch, `CleanupPending`과 FIFO no-bypass 고정 | P1=0 |
| Stability | P1=4 | dead waiter liveness, cancellation/admission race, reader starvation, generation ABA | Redis-side deadline, compare-delete reconcile state, phase-fair boundary, monotonic non-reset generation 및 close/late-completion 계약 고정 | P1=0 |
| Security | P1=3 | identity entropy, input/script validation, DoS bound 부재 | 128-bit 이상 CSPRNG, owner trust boundary, name/KEYS/ARGV/reply bound, wait/lease/retry/watchdog/permit cap 고정 | P1=0 |
| Operator | P1=2 | positive observability contract와 rollout/rollback 부재 | metric dimension allow-list, lifecycle event, redaction, additive rollout, namespace isolation, drain/cleanup 및 runbook gate 고정 | P1=0 |
| Developer/API | P1=5 | public surface, failure channel, handle/identity invariant, package boundary 부재 | 객체별 factory/config/operation/result/lifecycle 표, sealed-result 단일 경계, handle/ID 규칙, `coordination.internal` 경계 고정 | P1=0 |
| User/Caller | P1=5 | identity 사용법, reconcile 절차, multi-permit handle, migration path 부재 | caller identity lifecycle, recovery decision table, 전량 single-use permit handle, old/new API migration matrix 고정 | P1=0 |

## 3. 수렴한 핵심 결정

### 3.1 Delivery와 API 동결

- 구현 순서는 `Lock family → Synchronizer family → API convergence`로 유지한다.
- 두 family는 독립 PR과 독립 검증 단위를 가진다.
- 전체 public operation, identity, handle, result/failure 의미는 구현 전에 이 명세로 고정한다.
- 마지막 convergence는 breaking rewrite나 새 algorithm이 아니라 source-compatible
  naming/config 교정과 두 구현의 불일치 제거만 수행한다.

### 3.2 Caller contract

- Java thread가 아닌 caller-supplied logical owner가 reentrancy domain이다.
- Request ID는 logical operation마다 새로 만들고 ambiguity reconciliation까지 재사용한다.
- Redis dispatch 전 validation만 동기 예외이며, dispatch 뒤 예상 가능한 상태는
  operation-specific sealed result 하나로 전달한다.
- Cancellation은 Redis mutation 미실행의 증명이 아니므로 새 identity로 blind retry하지 않는다.
- Multi-permit acquisition은 `N` permit 전체를 나타내는 single-use handle 하나를 반환하고
  초기 버전에서 partial release를 지원하지 않는다.

### 3.3 Redis safety와 진행성

- 모든 mutation은 atomic Lua와 owner/generation 검증을 사용한다.
- Fair queue는 Redis-side deadline, bounded stale cleanup 및 FIFO no-bypass를 적용한다.
- Read/write lock은 reader와 writer 모두의 starvation을 막는 phase-fair boundary를 사용한다.
- Lock, permit, latch generation은 Redis가 monotonic하게 발급하고 cleanup에서 reset하지 않는다.
- Warm `EVALSHA`는 operation당 최대 1 command, cold `NOSCRIPT`는 최대 2 command로 제한한다.

### 3.4 운영과 보안

- Identity는 CSPRNG 최소 128 bit이며 raw owner/request/permit/key/token을 log/metric에 노출하지 않는다.
- Input, queue, wait, lease, retry, watchdog, permit 및 script reply는 명세한 hard bound를 갖는다.
- Runtime은 shared scheduler를 소유하고 object/runtime close와 late completion을 generation/task
  identity로 방어한다.
- Rollout은 additive opt-in과 versioned namespace를 사용하며 rollback/drain/cleanup과
  operator runbook을 delivery별로 갱신한다.

## 4. 후속 구현 게이트에서 증명할 항목

- Testcontainers Redis에서 lock/synchronizer별 blocking/async/suspend semantic parity
- warm/cold script command budget과 contention benchmark
- fair stale-head cleanup, phase-fair progress, cancellation/admission reconciliation
- CSPRNG 및 모든 input/capacity bound의 fail-closed 동작
- monotonic generation, duplicate/late mutation 방지, close 뒤 task leak 없음
- metric cardinality/redaction, rollout namespace isolation 및 compatibility regression
- compile-tested KDoc/README usage와 operator runbook

## 5. 최종 판정

| 관점 | P0 | P1 | 판정 |
|---|---:|---:|---|
| Performance | 0 | 0 | PASS |
| Stability | 0 | 0 | PASS |
| Security | 0 | 0 | PASS |
| Operator | 0 | 0 | PASS |
| Developer/API | 0 | 0 | PASS |
| User/Caller | 0 | 0 | PASS |
| Main-session integration | 0 | 0 | PASS |

작성된 명세의 검토 게이트는 통과했다. 다음 단계는 사용자의 written-spec 확인이며,
확인 전에는 implementation plan 또는 production code를 작성하지 않는다.
