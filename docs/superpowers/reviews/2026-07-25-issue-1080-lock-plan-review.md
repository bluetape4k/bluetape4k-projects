# Issue #1080 Lock Delivery 구현 계획 검토

**검토 대상:** `docs/superpowers/plans/2026-07-25-issue-1080-lettuce-lock-family-plan.md`
**검토 범위:** Delivery 1 Lock family 계획만 검토하며 생산 코드와 Synchronizer delivery는 제외한다.
**통과 조건:** Performance, Stability, Security, Operator, Developer/API, User/Caller 및 주 세션 통합 검토가 모두 `P0=0 / P1=0`이어야 한다.

## 1. 검토 방식

- 여섯 관점을 서로 독립적인 읽기 전용 검토로 수행했다.
- 최초 검토에서 발견한 모든 P0/P1을 계획에 반영한 뒤, 수정된 현재 문서를 다시 열어 같은 관점으로 재검토했다.
- 주 세션은 승인된 설계 요구사항 추적성, 공개 타입/팩토리 일관성, 작업 의존성, 검증 명령, PR/merge 경계를 별도로 확인했다.
- 이 문서는 계획 품질 게이트의 기록이며 구현 완료 증거가 아니다.

## 2. 최초 검토와 교정

| 관점 | 최초 결과 | 핵심 지적 | 계획 교정 |
|---|---:|---|---|
| Performance | P0=0 / P1=2 | topology task를 등록 전에 호출할 수 있었고 generic performance tag와 충돌 가능성이 있었다 | Task 10에서 `coordination-lock-topology`와 task 등록을 먼저 고정하고, Task 11에 고유 `coordination-lock-performance` tag 및 Gradle task discovery를 고정했다 |
| Stability | P0=1 / P1=1 | 재진입 release/renew의 ambiguous replay가 hold count를 중복 변경할 수 있었고 close 이후 안정 결과가 없었다 | 요청별 단일 hold, 단일 소비 release, bounded terminal marker, replacement renew, idempotent downgrade, 전 결과군의 `Closed`를 고정했다 |
| Security | P0=0 / P1=0 | P2로 `maxKeys`, fencing epoch, namespace 검증 경계의 명확화가 필요했다 | `maxKeys` 1..32 사전 검증, Lua 정수 정밀도 상한, colon-separated namespace와 key/input/reply byte 상한을 고정했다 |
| Operator | P0=0 / P1=1 | event callback만으로는 queue/watchdog/task leak 등 운영 신호를 scrape 가능한 계약으로 만들 수 없었다 | counter/gauge/histogram/event 이름과 bounded dimensions, sink 격리, §7 위험 신호 매핑, marker 기반 runbook 검증을 고정했다 |
| Developer/API | P0=0 / P1=2 | object별 specialized handle signature와 12개 blocking/suspend 타입의 standalone/Cluster factory matrix가 불충분했다 | 공통 signature template의 정확한 handle 치환, fenced/read-write 전용 연산, 전 factory/config/scheduler/sink 조합과 Java fixture를 고정했다 |
| User/Caller | P0=0 / P1=2 | suspend object의 close 계약과 재진입 handle 사용법이 불명확했다 | 모든 suspend 타입에 non-suspending idempotent `close()`, nested reentry handle별 정확히 한 번 release, Java 예제 및 legacy migration 표를 고정했다 |

주 세션 통합 검토에서 no-config Java overload를 약속하면서 concrete factory 예제가 Kotlin default argument에
의존하는 불일치를 추가로 발견했다. 예제를 명시적인 no-config/config overload 쌍으로 교정해 factory matrix와
일치시켰다.

첫 교정 후 재검토에서 Performance와 Stability가 각각 새 P1 한 건을 발견했다.

- 최대 10,000 watchdog과 tick당 256 dispatch만으로는 최소 TTL 전에 backlog를 비운다는 서비스 불변식이
  없었다. interval을 `100 ms..TTL/3`으로 제한하고 25-ms backlog drain, admission formula,
  1-second Redis-completion margin, late/missed ownership-loss 처리, deterministic 10,000-registration 검증을
  계획에 고정했다.
- Fair waiter가 `requestId`만으로 식별되면 서로 다른 owner가 같은 caller-supplied request ID를 사용할 때
  충돌할 수 있었다. object-scoped queue에서 `(ownerId, requestId)`를 collision-free length-prefixed tuple로
  인코딩하고 compare-delete/reconcile에 전체 tuple을 사용하도록 고정했다.

## 3. 교정 근거

- 공개 관측 계약과 cardinality 제한: 계획 §2.1 `LockObservation` / `LockObservationSink`
- 안정적인 close 결과와 취소 구분: 계획 §2.1 `Closed` lifecycle contract
- 입력, Lua 정수, multi-lock 크기, protocol reply 상한: 계획 §2.2 validation table
- Java/Kotlin factory와 handle 특수화: 계획 §2.3 factory matrix 및 exact signatures
- 요청별 재진입 hold, release/renew/downgrade replay: 계획 §3.3 acquisition semantics
- watchdog backlog 서비스 용량과 late/missed 처리: 계획 §3.1 runtime invariant 및 Tasks 2, 4, 11
- owner-qualified Fair waiter identity와 충돌 회귀: 계획 §3.3 및 Task 5
- topology/performance 전용 task와 report validator: 계획 Tasks 10-11
- nested reentry, Java compile fixture, marker runbook: 계획 Task 12
- 여섯 관점 구현 검토와 PR merge gate: 계획 Tasks 13-14

## 4. 최종 재검토 결과

| 관점 | P0 | P1 | 판정 |
|---|---:|---:|---|
| Performance | 0 | 0 | 통과 |
| Stability | 0 | 0 | 통과 |
| Security | 0 | 0 | 통과 |
| Operator | 0 | 0 | 통과 |
| Developer/API | 0 | 0 | 통과 |
| User/Caller | 0 | 0 | 통과 |
| Main-session integration | 0 | 0 | 통과 |

## 5. 잔여 위험과 다음 게이트

- 이 계획은 Redis Lua, 실제 Lettuce Cluster redirect, Testcontainers 동시성, Dokka/Java source compatibility를 아직 실행하지 않았다. 이는 구현 Task 1-13의 검증 대상이다.
- 공용 모델과 neutral runtime 파일은 충돌 가능성이 높으므로 Tasks 1-4는 주 구현자가 소유해야 한다.
- Redis/Testcontainers 검증은 worktree와 module 사이에서도 순차 실행해야 한다.
- Delivery 1 merge 전에는 Synchronizer 구현을 포함하지 않는다. merge 및 local sync 후 Delivery 2 계획을 현재 코드 기준으로 새로 작성한다.
- PR 생성은 계획의 고정된 repo/base/head 범위와 Task 14 전제조건을 만족한 뒤 수행한다. merge는 별도의 최신 사용자 승인 없이는 수행하지 않는다.

## 6. 게이트 판정

계획 검토 게이트는 통과했다.

```text
P0=0
P1=0
```

생산 코드 구현은 이 계획에 대한 실행 방식 승인 후 시작한다.
