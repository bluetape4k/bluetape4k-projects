# Issue #1070 — Event Sourcing 및 projection primitive 평가 기록

## 결정

**결정: 추출 보류(`defer pending another consumer`). 현재 구현은 애플리케이션 로컬에 유지한다.**

두 개의 독립 도메인에서 비슷한 Event Sourcing과 projection 개념을 확인했지만,
지금 public module로 추출하면 PostgreSQL 트랜잭션, stream ordering, projection
복구 의미를 감춘 generic repository가 될 가능성이 높다. 직접 Exposed DSL 구현과
비교한 append/replay/rebuild/storage benchmark도 아직 없다.

따라서 이 이슈에서는 production code나 public primitive를 추가하지 않는다. 다음
독립 도메인이 같은 semantic contract를 다시 요구하고, 공통 compatibility suite와
정량 benchmark를 확보한 뒤에만 별도 추출 이슈를 만든다.

## 평가 범위와 현재 근거

평가 대상은 다음 workshop 구현과 live GitHub 이슈다.

| 소비자/비교 대상 | 역할 | 현재 상태 | 판단에 사용한 근거 |
| --- | --- | --- | --- |
| [#552 정규화 usage billing ledger](https://github.com/bluetape4k/bluetape4k-workshop/issues/552) | normalized PostgreSQL state와 append-only ledger를 사용하는 baseline | CLOSED | [`usage-metering-billing-ledger/README.md`](https://github.com/bluetape4k/bluetape4k-workshop/blob/develop/commerce/usage-metering-billing-ledger/README.md), `BillingCloseService`, immutable ledger/invoice 계약 |
| [#553 Event-sourced usage billing](https://github.com/bluetape4k/bluetape4k-workshop/issues/553) | event store, expected-version append, upcast, snapshot, projection generation을 사용하는 billing consumer | CLOSED | [`usage-metering-billing-event-sourcing/README.md`](https://github.com/bluetape4k/bluetape4k-workshop/blob/develop/commerce/usage-metering-billing-event-sourcing/README.md), `eventstore/`, `projection/`, PostgreSQL integration tests |
| [#555 usage billing microservices](https://github.com/bluetape4k/bluetape4k-workshop/issues/555) | 다섯 서비스의 local outbox/inbox, ordering, deferred/quarantine, restart를 검증하는 동일 billing series의 분리 변형 | CLOSED | [`usage-billing-microservices-composition-tests/README.md`](https://github.com/bluetape4k/bluetape4k-workshop/blob/develop/commerce/usage-billing-microservices-composition-tests/README.md), composition integration tests |
| [#538 Event-sourced promotion/voucher campaign](https://github.com/bluetape4k/bluetape4k-workshop/issues/538) | billing과 무관한 campaign/voucher 도메인의 event store와 rebuildable projection consumer | CLOSED | [`event-sourced-promotion-voucher-campaign/README.md`](https://github.com/bluetape4k/bluetape4k-workshop/blob/develop/commerce/event-sourced-promotion-voucher-campaign/README.md), event-store/projection/operations source와 integration tests |

#538은 #1070의 원래 의존성 목록에는 없지만, “billing 외부에서도 유용한가”를
검증하는 독립 소비자 근거로 함께 읽었다. 반대로 #555는 운영 경계가 늘어난
중요한 사례지만 같은 usage billing 의미를 유지하므로 독립 business consumer 두
곳으로 중복 계산하지 않는다.

## 반복 개념과 애플리케이션 전용 개념

### 공통 후보

| 후보 | #553과 #538에서 확인한 반복 | 추출 판단 |
| --- | --- | --- |
| Event envelope와 schema discriminator | tenant, stream identity, event type/version, occurred/recorded time, bounded payload, checksum 또는 hash | **보류**. actor/metadata 개인정보 경계와 hash 입력 규칙이 다르며, 한쪽의 envelope를 공통 DTO로 만들면 schema evolution 책임을 숨긴다. |
| Expected-version append와 stream head CAS | stream version, optimistic conflict, append-only history, transaction 안의 head/event write | **보류**. 두 구현 모두 PostgreSQL row/unique constraint와 transaction 순서를 공개해야 한다. `ExposedJdbcRepository`의 일반 CRUD로 감싸면 correctness 경계를 잃는다. |
| Pure reducer와 deterministic replay | event sequence를 순서대로 fold하고 동일 state/digest를 재현 | **보류**. 가장 작은 공통 후보지만 event decoding, tenant policy, reducer version을 호출자가 소유한다는 contract와 migration cost를 먼저 검증해야 한다. |
| Snapshot validity와 genesis fallback | reducer version, stream version, last hash/position 검증 후 invalid snapshot을 폐기하고 full replay | **보류**. snapshot metadata와 invalidation/retention 정책이 두 도메인에서 다르다. |
| Upcaster chain | 명시적 event type/version registry와 contiguous one-step conversion | **보류**. upcast depth, unknown schema, payload size/security 정책을 통합하기 전에 compatibility fixture가 필요하다. |
| Projection checkpoint/generation | durable checkpoint, lease/fence, duplicate suppression, BUILDING/ACTIVE 전환, rebuild | **보류**. checkpoint는 저장소를 숨길 수 없고, generation switch와 poison handling은 read-model 운영 정책에 종속된다. |
| Inbox/outbox/quarantine | #555에서 local transaction과 메시지 delivery 경계를, #538에서 projection poison/recovery를 확인 | **애플리케이션 로컬 유지**. outbox와 projection poison은 이름은 비슷하지만 retry, ownership, redrive authority가 다르다. |

### 애플리케이션 전용

가격 version과 billing close, immutable invoice/ledger, voucher allocation/redeem,
campaign capacity, operator 권한, actor erasure mapping, HTTP idempotency response,
Kafka topic/consumer routing은 공통 primitive에 넣지 않는다. 이들은 각 도메인의
financial 또는 business authority이며 generic event store가 결정할 수 없다.

## Candidate API sketch

아래는 구현 제안이 아니라, 추출을 다시 검토할 때 지켜야 할 경계의 스케치다.
현재 repository나 public API에는 추가하지 않았다.

```kotlin
interface EventStreamStore<E : Any> {
    fun append(
        transaction: PostgresTransaction,
        stream: StreamId,
        expectedVersion: Long,
        events: List<NewEvent<E>>,
    ): AppendResult

    fun load(
        transaction: PostgresTransaction,
        stream: StreamId,
        afterVersion: Long,
        limit: Int,
    ): EventPage<E>
}

interface EventReducer<S : Any, E : Any> {
    val initialState: S
    fun evolve(state: S, event: E): S
}

interface SnapshotPolicy<S : Any> {
    fun validate(snapshot: Snapshot<S>, streamHead: StreamHead): SnapshotValidity
}

interface ProjectionCheckpointStore {
    fun claim(request: CheckpointClaim): CheckpointLease
    fun advance(lease: CheckpointLease, position: GlobalPosition): AdvanceResult
}
```

이 스케치는 다음을 전제로 한다.

- `PostgresTransaction`과 `GlobalPosition`은 의도적으로 저장소 의미를 드러낸다.
  transaction을 숨긴 `Repository<E, ID>`나 in-memory exactly-once 추상화는 만들지 않는다.
- tenant predicate, event serialization, hash input, expected-version conflict,
  checkpoint fencing, retry/quarantine 정책은 각 application이 명시적으로 소유한다.
- API가 의미 있는 중복을 제거한다는 것을 두 consumer의 contract test와 migration
  diff로 증명하지 못하면 이 스케치를 구현으로 승격하지 않는다.

## Module 위치 선택지

| 선택지 | 결론 | 이유 |
| --- | --- | --- |
| `bluetape4k-exposed-jdbc`에 추가 | 거부 | Exposed 공통 저장소 계층은 transaction/SQL dialect/tenant/append semantics의 권위자가 아니다. 추가하면 모든 consumer가 불필요한 dependency와 generic CRUD surface를 얻게 된다. |
| `bluetape4k-event-sourcing` 단일 public module | 보류 | 두 도메인의 공통 의미, ownership, versioning, migration path와 benchmark가 정해지지 않았다. 지금 만들면 workshop 구현을 library 요구사항으로 오인하게 된다. |
| workshop test-support contract | 현재 선택 | production class를 공유하지 않고 JSON fixture와 black-box assertion만 먼저 비교할 수 있다. 이 contract가 반복되면 별도 projects module을 다시 제안한다. |
| 각 application의 local primitive | 현재 유지 | 각 구현이 PostgreSQL transaction, recovery, security, projection authority를 직접 드러내며 현재 acceptance를 충족한다. |

## 도입 게이트 판정

| 게이트 | 판정 | 증거와 남은 조건 |
| --- | --- | --- |
| 독립 production-shaped consumer 두 곳이 같은 semantic contract를 요구 | **부분 충족** | #553 usage billing과 #538 promotion/voucher는 독립 도메인이다. 다만 두 구현이 동일한 transaction/failure contract를 요구한다고 말할 만큼의 shared test는 아직 없다. #555는 같은 billing series라 별도 consumer로 세지 않았다. |
| transaction/ordering/failure를 숨기지 않는 API가 중복을 줄임 | **미충족** | 두 구현의 event store와 projection API를 side-by-side로 비교한 migration proof가 없고, transaction context를 노출할 public boundary도 합의하지 않았다. |
| PostgreSQL contention/replay/snapshot/checkpoint/schema/tenant compatibility suite | **부분 충족** | 각 workshop은 PostgreSQL integration/architecture test를 보유하지만 공통 black-box suite는 없다. 우선 test-support contract로 중복 시나리오를 고정해야 한다. |
| ownership/dependency direction/migration/versioning | **미충족** | projects module owner, 공개 패키지, compatibility policy, 기존 consumer migration 순서가 정해지지 않았다. |
| append/replay/rebuild/allocation/storage benchmark | **미충족** | 현재 `stressTest`는 10,000 event/command의 correctness와 recovery를 확인하는 회귀 테스트이며 직접 Exposed DSL 대비 비용을 측정하지 않는다. allocation, row/storage overhead, p95 latency 비교도 없다. |

모든 게이트가 충족되지 않았으므로 지금은 library 구현이나 release 가능한
implementation issue를 만들지 않는다. #1070은 “추출 보류” 상태로 남기고, 다음
독립 consumer가 생길 때 이 기록의 gate matrix를 갱신한다.

## 최소 reusable test contract 제안

추출 전에 workshop test-support에서 다음 시나리오를 두 구현에 각각 실행할 수
있어야 한다. 공통 fixture는 production repository나 entity를 공유하지 않고,
command/HTTP 또는 명시적인 adapter 경계만 사용한다.

1. **Append CAS** — 같은 stream과 expected version으로 경쟁한 append 중 하나만
   commit되고, conflict가 안정된 결과로 분류된다.
2. **Atomicity** — event와 stream head가 함께 commit되거나 함께 rollback된다.
   receipt/outbox/checkpoint를 포함하는 transaction 경계를 테스트에서 확인한다.
3. **Replay and snapshot** — genesis replay와 valid snapshot replay가 같은 state,
   version, digest를 만들고 invalid snapshot은 원본 history를 수정하지 않고
   genesis로 돌아간다.
4. **Schema evolution** — contiguous upcast chain, unknown mandatory version,
   payload/metadata bound, checksum/hash 검증을 각각 확인한다.
5. **Projection recovery** — duplicate, stale owner, checkpoint gap, poison event,
   rebuild generation switch, active generation 보존을 확인한다.
6. **Tenant and security** — 모든 stream/event/snapshot/checkpoint 조회가 tenant를
   조건에 포함하고, payload·credential·raw identity가 로그/metric/response로
   새지 않음을 확인한다.
7. **Observable recovery** — lag, retry, quarantine, rebuild state가 bounded
   low-cardinality signal로 노출되고 operator action이 audit trail을 남긴다.
8. **Cost profile** — 동일 workload에서 직접 Exposed DSL과 후보 adapter의 append,
   replay, rebuild latency, allocation, row/storage overhead를 같은 JDK/DB/fixture
   조건으로 기록한다. 이 수치는 capacity 보장이 아니라 extraction decision 근거다.

## 보안·운영·성능 위험

- **보안:** 공통 envelope가 actor, credential, request body, tenant boundary를
  무심코 직렬화하면 두 consumer의 개인정보 삭제/보존 정책을 위반할 수 있다.
- **트랜잭션:** transaction을 감춘 generic repository는 append와 head CAS,
  projection mutation과 checkpoint, local state와 outbox의 atomicity를 보장하지
  못한다.
- **복구:** checkpoint, lease, generation, poison 상태를 하나의 “retry” API로
  합치면 stale worker나 재처리 중복을 숨긴다.
- **호환성:** event type/version registry를 library가 소유하면 각 consumer의
  schema evolution을 동시에 version 관리해야 한다. 독립 release 정책이 없다.
- **성능:** serialization, wrapper allocation, extra metadata/hash와 generic
  dispatch 비용을 현재 자료로 정량화할 수 없다. stress test 통과를 overhead
  허용 증거로 해석하지 않는다.
- **운영:** #555의 outbox/inbox와 #538의 projection poison은 복구 authority와
  redrive 경로가 다르다. 공통 운영 API는 오히려 잘못된 조치를 유도할 수 있다.

## DoD와 후속 조건

- [x] #552, #553, #555와 독립 consumer #538의 live 상태와 source를 확인했다.
- [x] 반복 개념과 application-specific 개념을 분리했다.
- [x] 후보 API와 module 위치 선택지를 제시했지만 production code는 추가하지 않았다.
- [x] compatibility/security/transaction/performance/operation 위험과 최소 test
  contract를 기록했다.
- [x] `extract now / defer pending another consumer / keep application-local` 중
  `defer pending another consumer`를 선택했다.
- [ ] 공통 compatibility suite와 직접 구현 대비 benchmark — 다음 독립 consumer가
  생긴 뒤 후속 작업.
- [ ] public module 구현/PR — 모든 도입 게이트 통과 전에는 생성하지 않음.

### 작성·검증 메모

- 문서 언어: 한국어 기술 문서. API 이름, 경로, 명령, URL, issue 번호는 원문을 보존했다.
- 범위: Type E 문서 변경이며 production behavior와 public API를 변경하지 않는다.
- 1인 개발자 lane에서는 CG-14 human-review를 N/A로 두되, source/read-back,
  diff, link, stale-token 검증과 live issue read-back은 별도로 수행한다.
