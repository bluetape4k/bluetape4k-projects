# Issue #1080 Synchronizer Delivery 구현 계획 검토

**대상:** `docs/superpowers/plans/2026-07-27-issue-1080-lettuce-synchronizers-plan.md`

**범위:** Delivery 2의 semaphore, expirable semaphore, count-down latch 계획만 검토한다. Lock 변경, Delivery 3 API convergence, 구현 diff는 제외한다.

## 최초 독립 검토

| Priority | Area | Evidence | Required edit | Result |
|---|---|---|---|---|
| P1 | Semaphore initialization | `trySetPermits`가 latch result를 반환하도록 계획됨 | `SemaphoreInitializationResult`와 `Initialized/AlreadyInitialized/InvalidCapacity`를 고정 | 반영 |
| P1 | Semaphore reconcile | 승인 명세의 `reconcile`이 공개 operation/test에서 누락 | blocking/async/suspend reconcile 및 ambiguous/owned/released/stale test 추가 | 반영 |
| P1 | Latch API | 승인 surface의 `getCount`/`inspect`와 mutation `requestId`가 누락 | 정확한 operation과 `LatchRequestId`/idempotent replay test 추가 | 반영 |
| P1 | Result taxonomy | `Contended`/`NotFound`가 명세의 `Unavailable`/`Deleted`와 불일치 | operation별 결과군과 명세의 stable variant 이름으로 교정 | 반영 |
| P1 | Expirable permit cardinality | `N` permit에 단일 permit ID/deadline처럼 읽힐 수 있음 | 정확히 `N`개의 unique ID/deadline을 가진 하나의 atomic handle로 고정 | 반영 |
| P1 | Verification command | 계획의 `:infra:lettuce` project path가 실제 settings topology와 불일치 | 모든 명령을 `:bluetape4k-lettuce`로 교정하고 baseline compile 실행 | 반영 |

## 주 세션 통합 보강

- 모든 결과군에 stable `Closed` lifecycle 결과를 요구한다.
- backend/integrity failure는 caller-controlled 문자열 대신 bounded enum과 recovery action을 사용한다.
- existing `CoordinationRuntime`, `CoordinationDeadline`, hard registration cap, non-owning scheduler/connection lifecycle을 재사용한다.
- active hold가 있을 때 capacity shrink를 명시적으로 거절하고 테스트한다.
- expirable multi-permit renew/release는 전체 handle에 원자적으로 적용하며 partial mutation은 지원하지 않는다.
- Redis command budget, bounded cleanup, 100 contender, 10,000 registration cap, 5회 순차 Testcontainers lifecycle을 검증한다.
- README/runbook에 Redis ACL/TLS/credential 책임, namespace 격리, bounded metrics/alerts, rollback/key cleanup을 포함한다.

## 2차 독립 검토

| Priority | Area | Evidence | Required edit | Result |
|---|---|---|---|---|
| P1 | Expirable `N`-permit storage | 각 unit lease에 전체 `permits=N`을 중복 저장하면 cleanup/release가 `N²`을 복구할 수 있음 | allocation total과 unit lease identity/deadline을 분리하고 `N=3`이 정확히 3만 복구하는 protocol test 추가 | 반영 |
| P1 | Custom codec wire slot | codec wire bytes가 derived key slot을 바꾸는 사전 dispatch 검증이 계획에 누락 | semaphore, expirable semaphore, latch 각각 custom `RedisCodec` split-slot fixture 추가 | 반영 |

## 최종 재검토

독립 reviewer가 수정된 현재 계획을 다시 읽고 남은 P0/P1을 판정한다. 구현은 아래 결과가 `P0=0 / P1=0`일 때만 시작한다.

```text
P0=0
P1=0
```
