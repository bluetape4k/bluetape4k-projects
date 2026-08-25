# Issue #1320 — TenantContext 전파·정리 bridge 평가 기록

## 결정

**결정: 공통 bridge 승격을 보류한다. 현재 framework별 context carrier와 tenant routing/auth 경계를 애플리케이션 로컬에 유지한다.**

이슈 본문은 `ThreadLocal`과 `ThreadContextElement`가 여러 예제에 반복된다고
설명하지만, 현재 `exposed-r2dbc-workshop`의 최신 `develop` 소스는 이미 서로
다른 경계를 사용한다. Ktor는 `ApplicationCall.attributes`, schema-per-tenant
WebFlux는 `ReactorContext`와 `CoroutineContext`, connection-factory/security/
onboarding 예제는 명시적인 Reactor key와 별도의 `TenantId` 검증 계약을 사용한다.

따라서 지금 하나의 `TenantContext`를 공개하면 다음 의미를 섞게 된다.

- request carrier의 수명과 cleanup 방식
- tenant ID의 정규화·검증과 인증/인가 책임
- schema 전환 또는 `ConnectionFactory` 선택 같은 저장소 책임
- context가 없을 때 fail-closed 할지 default tenant를 허용할지에 대한 정책

이번 이슈에서는 production code, public module, 기존 workshop migration을
추가하지 않는다. 먼저 carrier 중립적인 최소 contract를 독립 fixture로 고정하고,
Servlet/virtual-thread 소비자까지 포함한 두 개 이상의 production-shaped consumer가
같은 의미를 실제로 요구하는지 확인한 뒤 별도 구현 이슈를 만든다.

## 평가 범위와 최신 상태

라이브 이슈는 [#1320](https://github.com/bluetape4k/bluetape4k-projects/issues/1320)
이며 `enhancement`, `design`, `examples`, `coroutines` 라벨과 `2.0.0` milestone,
`debop` assignee를 유지한다. 이슈 본문에 적힌 일부 경로와 구현 설명은 현재
workshop 구조와 달라졌으므로, 판단에는 최신 `develop` source와 README/spec/test를
우선 사용했다.

| 소비자/경계 | 현재 carrier와 책임 | source/test 근거 | 판단 |
| --- | --- | --- | --- |
| Ktor schema-per-tenant | `TenantPlugin`이 검증된 `Tenants.Tenant`를 `ApplicationCall.attributes`에 저장하고 `call.currentTenant()`가 없으면 오류 | [`TenantPlugin.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/07-multitenant-ktor/src/main/kotlin/exposed/r2dbc/multitenant/ktor/tenant/TenantPlugin.kt#L6-L40), [`KtorMultitenantApplicationTest.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/07-multitenant-ktor/src/test/kotlin/exposed/r2dbc/multitenant/ktor/KtorMultitenantApplicationTest.kt#L198-L218) | call 수명으로 격리되므로 `ThreadLocal` cleanup bridge를 도입할 이유가 없다. |
| WebFlux schema-per-tenant | `TenantFilter`가 헤더를 검증하고 Reactor key에 `TenantId`를 저장한다. `currentReactorTenant()`는 직접 호출 경로에서 default tenant로 fallback한다. | [`TenantFilter.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/03-multitenant-spring-webflux/src/main/kotlin/exposed/r2dbc/multitenant/webflux/tenant/TenantFilter.kt#L15-L69), [`TenantId.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/03-multitenant-spring-webflux/src/main/kotlin/exposed/r2dbc/multitenant/webflux/tenant/TenantId.kt#L15-L70), [`ActorControllerTest.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/03-multitenant-spring-webflux/src/test/kotlin/exposed/r2dbc/multitenant/webflux/controller/ActorControllerTest.kt#L94-L148) | Reactor context는 immutable subscriber scope다. Ktor와 같은 `current`/cleanup API로 합치면 fallback 정책이 바뀐다. |
| Connection-factory routing | `TenantContextKeys.TENANT_ID`를 Reactor context에 기록하고, `TenantTransactionExecutor`가 `Mono.deferContextual`에서 coroutine `ReactorContext`로 bridge한 뒤 routing DB를 연다. | [`TenantContextKeys.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/04-connection-factory-per-tenant-spring-webflux/src/main/kotlin/exposed/r2dbc/multitenant/connectionfactory/tenant/TenantContextKeys.kt#L3-L10), [`TenantTransactionExecutor.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/04-connection-factory-per-tenant-spring-webflux/src/main/kotlin/exposed/r2dbc/multitenant/connectionfactory/tenant/TenantTransactionExecutor.kt#L24-L43), [`TenantRoutingConnectionFactoryTest.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/04-connection-factory-per-tenant-spring-webflux/src/test/kotlin/exposed/r2dbc/multitenant/connectionfactory/tenant/TenantRoutingConnectionFactoryTest.kt#L24-L60) | context key와 connection selection은 저장소 어댑터 책임이다. 공통 core가 DB 정책을 소유하면 안 된다. |
| Security WebFlux | 인증된 tenant와 요청 tenant가 일치한 뒤에만 `TenantContextKeys.TENANT_ID`를 쓴다. 헤더를 먼저 context에 넣지 않는다. | [`AuthorizedTenantContextWebFilter.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/05-spring-security-tenant-authorization-spring-webflux/src/main/kotlin/exposed/r2dbc/multitenant/security/security/AuthorizedTenantContextWebFilter.kt#L18-L49), [`AuthorizedTenantContextWebFilterTest.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/05-spring-security-tenant-authorization-spring-webflux/src/test/kotlin/exposed/r2dbc/multitenant/security/security/AuthorizedTenantContextWebFilterTest.kt#L1-L180) | routing context와 authorization context를 같은 API로 제공하면 confused-deputy 위험이 생긴다. |
| Onboarding WebFlux | `[a-z][a-z0-9-]{1,30}` 규칙의 `@JvmInline value class TenantId`와 Reactor key를 사용하며, 명시 tenant 실행 경로도 별도로 제공한다. | [`TenantTypes.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/06-tenant-onboarding-spring-webflux/src/main/kotlin/exposed/r2dbc/multitenant/onboarding/tenant/TenantTypes.kt#L1-L40), [`TenantTransactionExecutor.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/06-tenant-onboarding-spring-webflux/src/main/kotlin/exposed/r2dbc/multitenant/onboarding/tenant/TenantTransactionExecutor.kt#L24-L55), [`TenantIdTest.kt`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/10-multi-tenant/06-tenant-onboarding-spring-webflux/src/test/kotlin/exposed/r2dbc/multitenant/onboarding/tenant/TenantIdTest.kt#L1-L28) | 동적 tenant lifecycle과 request routing을 하나의 mutable context에 넣지 않는다. |
| Servlet/virtual-thread | 이 issue train의 최신 workshop source에서 공통 `TenantContext` 또는 Servlet/virtual-thread contract를 확인하지 못했다. | projects/workshop/exposed-r2dbc source bounded scan과 현재 issue body 대조 | 수용 기준의 핵심 소비자가 아직 비어 있으므로 public bridge gate를 충족하지 못한다. |

Ktor 예제의 설계 문서도 두 carrier를 의도적으로 분리한다. [`Issue 33 설계`](https://github.com/bluetape4k/exposed-r2dbc-workshop/blob/develop/docs/superpowers/specs/2026-05-23-issue-33-ktor-multitenant-r2dbc-design.md#L90-L115)는 Ktor call attribute와 WebFlux `ReactorContext`를 각각 유지하고, `ThreadLocal`을 Ktor request path에서 금지한다. 이 결정을 거꾸로 되돌릴 공통 consumer proof는 아직 없다.

## 반복되는 최소 의미와 분리해야 할 의미

| 의미 | 공통 후보 | 현재 판정 |
| --- | --- | --- |
| tenant 식별자 | 비어 있지 않고 정규화된 opaque value 또는 value class | **후속 검토**. 정규화 규칙과 허용 문자/길이는 consumer별로 다르다. |
| 현재 값 조회 | `currentOrNull`과 명시적 `requireCurrent` | **후속 검토**. 현재 WebFlux의 default fallback은 fail-closed 계약과 충돌한다. |
| 중첩 범위 | `withTenant(tenant) { ... }`에서 이전 값을 복원 | **어댑터별 검증**. ThreadLocal adapter에만 필요한 cleanup이며 Reactor/Ktor carrier에는 동일한 복원 모델이 없다. |
| coroutine 전파 | `ThreadContextElement` 또는 `ReactorContext` bridge | **분리 유지**. blocking/Servlet과 WebFlux의 thread/context 수명이 다르다. |
| request 종료 정리 | ThreadLocal remove/restore, Reactor subscriber scope, Ktor call discard | **공통 API 금지**. 하나의 `clear()`가 모든 carrier에서 같은 의미를 갖지 않는다. |
| tenant routing | schema, connection factory, keyspace, cache prefix 선택 | **application 책임**. context core가 저장소나 인증 정책을 결정하지 않는다. |

## 후보 API 스케치

아래는 구현 제안이 아니라, 별도 consumer proof가 생겼을 때 검토할 최소 경계다.
현재 repository와 public API에는 추가하지 않았다.

```kotlin
@JvmInline
value class TenantId(val value: String)

interface TenantContextReader {
    fun currentOrNull(): TenantId?
    fun requireCurrent(): TenantId
}

suspend fun <T> withTenant(
    tenantId: TenantId,
    block: suspend () -> T,
): T
```

이 스케치는 다음을 전제로 한다.

- core는 tenant 값의 형식과 scope contract만 소유하고, HTTP header 해석·인증/인가·DB
  schema/connection 선택은 소유하지 않는다.
- 기본 tenant fallback을 public core의 기본 동작으로 만들지 않는다. fallback이
  필요한 direct example은 adapter가 명시적으로 선택한다.
- `withTenant`는 carrier별 adapter 계약을 별도로 갖는다. ThreadLocal adapter는
  `updateThreadContext`/`restoreThreadContext`와 예외 경로를 검증하고, Reactor
  adapter는 `contextWrite`/`ReactorContext` 전파와 subscriber scope를 검증한다.
- Ktor adapter는 call attribute를 사용하며 global mutable state를 만들지 않는다.

## 도입 게이트 판정

| 게이트 | 판정 | 증거와 남은 조건 |
| --- | --- | --- |
| 독립 production-shaped consumer 두 곳이 동일 semantic contract를 요구 | **미충족** | 현재 근거는 같은 R2DBC chapter의 teaching modules와 Ktor example이다. Servlet/virtual-thread consumer와 서로 다른 production domain의 공통 contract가 없다. |
| Ktor, Servlet/virtual-thread, WebFlux에서 동일 contract를 검증 | **미충족** | Ktor와 WebFlux만 확인했다. Servlet/virtual-thread fixture가 없다. |
| 동시 격리와 중첩/예외 cleanup을 carrier별로 증명 | **부분 충족** | Ktor overlapping request와 WebFlux tenant isolation은 green source/test contract가 있으나, ThreadLocal nested restore/leak test와 cross-carrier black-box suite가 없다. |
| routing과 authorization 경계를 보존 | **부분 충족** | security filter가 인증 후 context를 쓰는 현재 규칙은 확인했지만, 공통 API migration proof는 없다. |
| API ownership/dependency/migration/versioning | **미충족** | projects의 public module 위치, adapter dependency 방향, 기존 예제 migration 순서가 합의되지 않았다. |
| dispatcher/thread hop, Reactor cleanup, 성능·누수 비용 | **미충족** | context switch/restore overhead, ThreadLocal leak soak, adapter 간 allocation benchmark가 없다. |

모든 필수 게이트가 충족되지 않았으므로 library 구현이나 release 가능한 public
bridge PR을 만들지 않는다. #1320은 설계·평가 이슈로 열어 두고, 다음 단계에서
공통 fixture와 누락된 Servlet/virtual-thread proof를 먼저 추가한다.

## 최소 reusable test contract

추출을 다시 제안하기 전에, production repository나 framework entity를 공유하지 않는
black-box fixture를 각 adapter에 실행할 수 있어야 한다.

1. **현재 값과 빈 상태** — `currentOrNull()`은 carrier가 없을 때 `null`이고,
   `requireCurrent()`는 안정된 오류를 낸다. 암묵적인 default tenant는 fixture의
   기본값이 아니다.
2. **중첩 복원** — `A` 안에서 `B`를 설정한 뒤 `A`가 복원되고, 정상·예외 경로 모두
   request 종료 후 빈 상태가 된다.
3. **dispatcher/thread hop** — `Dispatchers.IO`, virtual thread, coroutine
   resumption을 섞어도 각 작업이 자기 tenant만 읽는다.
4. **동시 격리** — 최소 두 tenant의 병렬 작업에서 값 교차가 없고, 반복 교대 후
   이전 작업의 값이 남지 않는다.
5. **Reactor bridge** — `contextWrite`에서 시작한 값이 suspend controller와
   Exposed transaction까지 전달되고, subscriber 종료 뒤 다른 subscriber에 보이지
   않는다.
6. **Ktor lifecycle** — call attribute가 request 사이에 공유되지 않고, 인증/인가
   검증 전에는 routing tenant가 노출되지 않는다.
7. **보안/정규화** — raw header가 schema/connection 선택으로 직접 들어가지 않고,
   malformed/unknown/unauthorized tenant가 fail-closed 된다.
8. **비용 측정** — ThreadLocal, Reactor, Ktor adapter의 context switch와 allocation,
   leak soak 결과를 같은 JDK/fixture 조건에서 비교한다.

## 보안·운영·호환성 위험

- **보안:** 요청 header를 인증된 identity와 분리하면 tenant confusion이 발생한다.
  공통 core는 인증을 우회하는 `setCurrent` API를 공개하지 않아야 한다.
- **정리:** ThreadLocal을 coroutine context에 연결하면서 `remove()`/이전 값 복원을
  빠뜨리면 pooled thread와 virtual thread 모두에 잔존 값이 남을 수 있다.
- **Reactor:** subscriber context를 ThreadLocal처럼 지우려 하면 immutable context
  semantics와 충돌하고, 반대로 default fallback을 허용하면 누락 요청이 다른 tenant로
  라우팅될 수 있다.
- **저장소:** schema switch, connection-factory routing, keyspace/prefix 선택은
  transaction/resource boundary다. tenant core에 넣으면 dependency 방향과 migration
  책임이 뒤섞인다.
- **호환성:** 서로 다른 `TenantId` 검증 규칙을 하나로 통합하면 기존 API가 허용하거나
  거부하던 값이 바뀐다. value class 도입 전 serialization/logging/Java 호출 경계를
  별도로 확인해야 한다.
- **성능:** 모든 호출을 `ThreadContextElement`로 감싸는 비용과 Reactor bridge의
  context 재구성 비용은 현재 자료로 정량화하지 않았다.

## DoD와 후속 조건

- [x] 라이브 #1320, milestone/labels/assignee와 Epic #1423 train dependency를 확인했다.
- [x] 최신 `exposed-r2dbc-workshop`의 Ktor/WebFlux/connection-factory/security/onboarding carrier와 test/spec 근거를 대조했다.
- [x] 이슈 본문과 현재 source의 경로·구현 차이를 기록했다.
- [x] 공통 후보 API, adapter 경계, routing/auth 분리, cleanup 정책을 제시했다.
- [x] `extract now / defer pending proof / keep application-local` 중 `defer pending proof + keep application-local`을 선택했다.
- [ ] Servlet/virtual-thread consumer proof와 ThreadLocal nested/leak fixture — 후속 작업.
- [ ] cross-carrier compatibility suite와 adapter benchmark — 후속 작업.
- [ ] public module 구현/기존 예제 migration — 모든 도입 게이트 통과 전에는 생성하지 않음.

### 작성·검증 메모

- 문서 언어: 한국어 기술 문서. API 이름, 경로, 명령, URL, issue 번호는 원문을 보존했다.
- 범위: Type E 문서 변경이며 production behavior와 public API를 변경하지 않는다.
- 1인 개발자 lane에서는 CG-14 human-review를 N/A로 두되, source/read-back, diff,
  link, stale-token 검증과 live issue read-back은 별도로 수행한다.
