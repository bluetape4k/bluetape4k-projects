# Issue #1070 Event Sourcing 및 projection primitive 후속 확인

## 결정

**결정: 추출 보류(`defer pending another consumer`). 현재 이벤트 소싱과 프로젝션 구현은 애플리케이션 로컬에 유지한다.**

이번 후속 확인에서도 공개 모듈로 승격할 근거가 충분하지 않았다. Workshop의
새 이슈 [#793](https://github.com/bluetape4k/bluetape4k-workshop/issues/793)은
기존 이벤트 소싱 기반 사용량 청구 소비자(consumer)의 Jackson·ID generator 정렬
작업이며, 아직 구현 PR이 없다. 새 사업 영역 소비자가 아니므로 #1070의 독립 소비자
게이트를 충족하지 않는다.

따라서 이번 변경은 평가 기록만 추가한다. 운영 코드, 공개 API, 의존성, 기존 소비자
이행은 변경하지 않는다.

## 확인 범위와 기준

- 확인일: `2026-08-27` (Asia/Seoul)
- live 조회 시각: `2026-08-27 12:51 KST` (`gh issue/pr view`, `gh api` 재확인)
- projects 기준: `origin/develop@f965b87aa8d456ce5f8961f48c592a3fc57397e1`
- Workshop 기준: `origin/develop@96a7eb829fb0cc625a3080553d9811a7b4df4dea`
- 원래 평가: [PR #1509](https://github.com/bluetape4k/bluetape4k-projects/pull/1509)
  — merge 완료, head `b7e9a1f11eabcac472f38874e9116525cfab00f7`, merge
  commit `9446c860248d7af53a00561b0b3764d7f465fee2`
- 대상 이슈: [#1070](https://github.com/bluetape4k/bluetape4k-projects/issues/1070)
  — 결정 댓글: [2026-08-25](https://github.com/bluetape4k/bluetape4k-projects/issues/1070#issuecomment-5410921144)
- 상위 Epic: [#1423](https://github.com/bluetape4k/bluetape4k-projects/issues/1423)

## 최신 live 상태

| 대상 | 현재 상태 | #1070 판단에 미치는 영향 |
| --- | --- | --- |
| [projects #1070](https://github.com/bluetape4k/bluetape4k-projects/issues/1070) | `OPEN`, milestone `2.0.0`, `debop`, `enhancement/design/infra/io/examples` | 2026-08-25 결정 댓글에서 추출 보류와 다음 독립 소비자 조건을 유지한다. |
| [projects PR #1509](https://github.com/bluetape4k/bluetape4k-projects/pull/1509) | `MERGED` | 원래 평가 산출물은 완료되었고, 이번 문서는 그 결정의 후속 기록이다. |
| [projects #1423](https://github.com/bluetape4k/bluetape4k-projects/issues/1423) | `OPEN` | #1070 평가 완료 후에도 공통 API 호환성 근거가 없어 Epic을 닫지 않는다. |
| [Workshop #792](https://github.com/bluetape4k/bluetape4k-workshop/issues/792) | `OPEN` Epic | 현재 Epic 상태를 확인했으며, #793의 후속 refactor와는 별도로 판단한다. |
| [Workshop #793](https://github.com/bluetape4k/bluetape4k-workshop/issues/793) | `OPEN`, PR 없음, 코드·테스트·detekt `PENDING` | #793 본문 기준 기존 사용량 청구 소비자 내부 정렬 이슈다. 독립 소비자나 공개 API 근거로 세지 않는다. |
| [Workshop #553](https://github.com/bluetape4k/bluetape4k-workshop/issues/553) | `CLOSED` | 이벤트 소싱 기반 사용량 청구 소비자로 계속 비교한다. |
| [Workshop #538](https://github.com/bluetape4k/bluetape4k-workshop/issues/538) | `CLOSED` | 사용량 청구와 다른 promotion/voucher 소비자로 독립 도메인 근거를 유지한다. |
| [Workshop #555](https://github.com/bluetape4k/bluetape4k-workshop/issues/555) | `CLOSED` | #553과 같은 사용량 청구 계열의 microservices 변형이므로 두 번째 독립 소비자로 중복 계산하지 않는다. |

## #793 소스 재확인

`commerce/usage-metering-billing-event-sourcing`는 현재 이벤트 소싱 기준 애플리케이션을
유지하며, 다음 직접 구현이 남아 있다. 소스 기준점(source anchor)은 파일과 줄을
고정해 재현할 수 있는 현재 `develop`의 위치를 뜻한다.

| 영역 | 소스 기준점 | 관찰 결과 |
| --- | --- | --- |
| JSON codec | [`application/DomainEventJsonCodec.kt:26,44`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/application/DomainEventJsonCodec.kt#L26-L44) | `jacksonObjectMapper()`와 `UUID.randomUUID()`를 기본값으로 사용한다. |
| canonical hash | [`eventstore/CanonicalEventHash.kt:4-11`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/eventstore/CanonicalEventHash.kt#L4-L11) | `tools.jackson.databind.ObjectMapper()`를 직접 만든다. |
| event stream | [`persistence/EventStoreRepository.kt:58-60`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/EventStoreRepository.kt#L58-L60) | stream head identity를 `UUID.randomUUID()`로 만든다. |
| projection/failure | [`ProjectionRepositories.kt:25-26`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/ProjectionRepositories.kt#L25-L26), [`:119-120`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/ProjectionRepositories.kt#L119-L120), [`:216-217`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/ProjectionRepositories.kt#L216-L217), [`BillingProjectionRepositories.kt:25-26`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/BillingProjectionRepositories.kt#L25-L26), [`:84-85`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/BillingProjectionRepositories.kt#L84-L85) | 저장되는 읽기 모델/실패 identity가 직접 UUID다. |
| receipt/기준 상태 | [`persistence/CommandReceiptRepository.kt:49-50`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/CommandReceiptRepository.kt#L49-L50), [`SnapshotRepository.kt:43-46`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/SnapshotRepository.kt#L43-L46) | receipt·기준 상태 identity가 직접 UUID다. 기준 상태는 이벤트 전체를 다시 읽지 않고 재생을 시작하는 저장된 집계 상태다. owner token과 persistence ID의 구분도 별도 검토가 필요하다. |
| worker lifecycle | [`worker/ProjectionScheduler.kt:32-38,58-64`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/worker/ProjectionScheduler.kt#L32-L64) | run·lease identity 생성 정책이 직접 UUID로 분산되어 있다. |

소스 기준점은 Workshop `origin/develop@96a7eb829fb0cc625a3080553d9811a7b4df4dea`에서
고정해 읽었다.

- [`DomainEventJsonCodec.kt`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/application/DomainEventJsonCodec.kt)
- [`CanonicalEventHash.kt`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/eventstore/CanonicalEventHash.kt)
- [`EventStoreRepository.kt`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/EventStoreRepository.kt)
- [`ProjectionRepositories.kt`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/ProjectionRepositories.kt)
- [`BillingProjectionRepositories.kt`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/BillingProjectionRepositories.kt)
- [`CommandReceiptRepository.kt`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/CommandReceiptRepository.kt)
- [`SnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/SnapshotRepository.kt)와 [`ProjectionScheduler.kt`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/worker/ProjectionScheduler.kt)
- [`BillingEventSourcingStressTest.kt#L56-L85`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/test/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/BillingEventSourcingStressTest.kt#L56-L85)와 [`#L244-L257`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/test/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/BillingEventSourcingStressTest.kt#L244-L257)

비교 소비자의 기준 문서는
[`usage-metering-billing-event-sourcing/README.md`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/README.md),
[`event-sourced-promotion-voucher-campaign/README.md`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/event-sourced-promotion-voucher-campaign/README.md),
[`usage-billing-microservices-composition-tests/README.md`](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-billing-microservices-composition-tests/README.md)다.

해당 모듈에는 codec/hash, event store, replay, 기준 상태, 프로젝션 generation,
tenant isolation, HTTP, recovery를 검증하는 테스트가 이미 있다. 그러나 이는 한
애플리케이션의 correctness·recovery 증거이며, 두 소비자가 공유할 공통 블랙박스
계약이나 공개 후보 API의 이행 증거는 아니다. #793 본문도 직접 mapper/ID 정렬과
계약 테스트를 후속 구현으로 남기고 있다.

## 도입 게이트 재판정

| 게이트 | 판정 | 최신 증거와 남은 조건 |
| --- | --- | --- |
| 서로 다른 운영형 소비자 두 곳 확인 | **충족** | [#553](https://github.com/bluetape4k/bluetape4k-workshop/issues/553) 사용량 청구와 [#538](https://github.com/bluetape4k/bluetape4k-workshop/issues/538) promotion/voucher가 서로 다른 도메인이다. [#793](https://github.com/bluetape4k/bluetape4k-workshop/issues/793)은 #553 내부 refactor이며 새 소비자가 아니다. |
| 두 소비자가 공유할 안정적 의미 계약 | **부분 충족** | 두 도메인이 있다는 사실은 확인했지만 공통 블랙박스 계약과 이행 경계는 정의하지 않았다. |
| 트랜잭션·순서·실패를 숨기지 않고 중복을 줄이는 공개 API | **미충족** | 두 구현의 트랜잭션, 스트림 순서, 프로젝션 복구를 같은 API로 옮기는 이행 diff가 없다. |
| PostgreSQL contention/replay/기준 상태/checkpoint/schema/tenant 호환성 검증 묶음 | **부분 충족** | 각 소비자의 개별 통합/아키텍처 테스트는 있으나 공통 블랙박스 검증 묶음이 없다. |
| 소유권/의존성 방향/이행/버전 관리 | **미충족** | projects 모듈의 owner, 공개 패키지, 버전 관리, 기존 소비자 이행 순서가 정해지지 않았다. |
| append/replay/rebuild/storage 벤치마크 | **미충족** | [10,000건 stress 경로](https://github.com/bluetape4k/bluetape4k-workshop/blob/96a7eb829fb0cc625a3080553d9811a7b4df4dea/commerce/usage-metering-billing-event-sourcing/src/test/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/BillingEventSourcingStressTest.kt#L56-L85)는 정확성과 복구만 확인한다. 직접 Exposed DSL과 후보 API의 latency, allocation, row/storage 비용 비교가 없다. |

판정은 `충족 / 부분 충족 / 미충족 / 부분 충족 / 미충족 / 미충족`이다. #793의
후속 구현이 완료되어도 같은 사용량 청구 계열이라는 사실은 독립 소비자 게이트를
자동으로 통과시키지 않는다.

## Kotlin·모듈 경계

이번 projects PR에는 Kotlin 운영 소스를 포함하지 않는다. Kotlin 패턴 지침 검토는
Workshop 소스 기준점을 확인하는 근거 수집에 한정하며, #793 구현의 수용을 대신하지
않는다.

- `Jackson.defaultJsonMapper`와 `Uuid.V7.nextId()` 적용 여부는 Workshop #793의
  별도 구현·테스트 범위다.
- owner/lease token처럼 cryptographic 또는 takeover 방지 계약이 필요한 값은
  persistence ID와 같은 generator로 기계적으로 치환하지 않는다.
- JSON wire format, canonical hash, optimistic concurrency, unique constraint,
  tenant predicate를 공개 구성 요소가 숨기지 않아야 한다.
- 공통 모듈을 만들기 전에 두 소비자의 소스·테스트 기준점과 트랜잭션 경계를 같은
  fixture로 재현해야 한다.

## 다음 재평가 조건

다음 조건을 모두 새 근거로 확보하기 전에는 공개 모듈 구현 PR을 만들지
않는다.

1. #553·#538과 의미가 겹치지 않는 독립 운영형 소비자가 추가된다.
2. 두 소비자에 적용할 append CAS, atomicity, replay/기준 상태, schema evolution,
   프로젝션 복구, tenant/security, 관찰 가능한 복구 블랙박스 계약을
   공통 fixture로 고정한다.
3. 트랜잭션·순서·복구 경계를 보존하는 이행 diff와 ownership, dependency direction,
   versioning, rollback 순서를 승인한다.
4. 같은 JDK/DB/workload에서 직접 Exposed DSL과 후보 API의 append/replay/rebuild,
   allocation, row/storage 비용을 반복 측정한다.
5. 위 증거를 바탕으로 별도 Type A 설계·계획 승인을 받는다.

## 검증 증거

- `git diff --check`: PASS.
- `node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs --json docs/superpowers/research/2026-08-27-issue-1070-event-sourcing-followup.md`: PASS, `findings=[]`.
- 고정 commit의 source path와 줄 fragment를 `gh api`로 재확인: PASS.
- live read-back에 사용한 명령과 결과:
  ```text
  gh issue view 1070 --repo bluetape4k/bluetape4k-projects --json number,state,title,milestone,labels,assignees,url
  gh pr view 1509 --repo bluetape4k/bluetape4k-projects --json number,state,headRefName,headRefOid,mergeCommit,url
  gh issue view 1423 --repo bluetape4k/bluetape4k-projects --json number,state,title,url
  gh issue view 792 --repo bluetape4k/bluetape4k-workshop --json number,state,title,url
  gh issue view 793 --repo bluetape4k/bluetape4k-workshop --json number,state,title,url
  gh issue view 553 --repo bluetape4k/bluetape4k-workshop --json number,state,title,url
  gh issue view 538 --repo bluetape4k/bluetape4k-workshop --json number,state,title,url
  gh issue view 555 --repo bluetape4k/bluetape4k-workshop --json number,state,title,url
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/application/DomainEventJsonCodec.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/eventstore/CanonicalEventHash.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/EventStoreRepository.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/ProjectionRepositories.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/BillingProjectionRepositories.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/CommandReceiptRepository.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/persistence/SnapshotRepository.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/worker/ProjectionScheduler.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  gh api 'repos/bluetape4k/bluetape4k-workshop/contents/commerce/usage-metering-billing-event-sourcing/src/test/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/BillingEventSourcingStressTest.kt?ref=96a7eb829fb0cc625a3080553d9811a7b4df4dea' --jq '.path + " " + .sha'
  ```
  결과: #1070·#1423·#792·#793 `OPEN`, #1509 `MERGED`, #553·#538·#555 `CLOSED`; 9개 source path가 모두 요청한 commit의 파일과 SHA를 반환했다. 문서의 `2026-08-27 12:51 KST` 시각에 확인했다.
- Kotlin/Gradle/Testcontainers 실행: `N/A` — 이 PR은 문서 전용이며 Kotlin 운영 소스를 변경하지 않는다. #793의 구현·테스트는 별도 이슈의 후속 범위다.

## DoD Status

- [x] #1070, #1423, #1509와 Workshop #792/#793/#538/#553/#555의 live 상태를 확인했다.
- [x] #793이 같은 사용량 청구 소비자의 후속 refactor이며 독립 소비자가 아님을 기록했다.
- [x] 실제 Kotlin 소스 기준점과 기존 개별 테스트 범위를 읽기 전용으로 대조했다.
- [x] 독립 소비자·호환성 검증 묶음·소유권·벤치마크 도입 게이트를 재판정했다.
- [x] `defer pending another consumer`와 application-local 유지 결정을 보존했다.
- [x] 운영 코드, 공개 API, 의존성, 이행을 변경하지 않았다.
- [ ] 공통 호환성 검증 묶음 — 새 독립 소비자 이후 후속 작업.
- [ ] 직접 Exposed DSL 대비 benchmark — 후보 API 경계와 반복 workload가 합의된 뒤 후속 작업.
- [ ] 공개 모듈 구현 — 모든 도입 게이트와 별도 Type A 승인 이후에만 수행한다.

### 작성·검증 메모

- 문서 언어: 한국어 기술 문서. 경로, 심볼, 명령, URL, commit·issue·PR 식별자는 원문을 보존했다.
- 범위: Type E 문서 후속 기록. 운영 동작과 공개 API를 변경하지 않는다.
- Kotlin 테스트와 Testcontainers 실행은 코드 변경이 없는 이번 문서 범위에서 `N/A`다. 문서의 소스 재확인과 링크/용어 검증으로 대체하지 않고 별도 증거로 구분한다.
- SPW-01~05: PASS — 독자·결정·근거·검증·DoD를 문서에 고정하고 최종 Markdown을 재확인했다.
- KO-01~KO-07: PASS — 사실·식별자·링크를 보존하고 문장/용어를 수동 검토했으며 contextual terminology audit에서 `findings=[]`를 확인했다.
