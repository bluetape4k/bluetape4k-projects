# #1562 공통 TenantContext core와 carrier adapter 설계

- Epic: [#1320](https://github.com/bluetape4k/bluetape4k-projects/issues/1320)
- 선행 fixture: [#1552](https://github.com/bluetape4k/bluetape4k-projects/issues/1552)
- 선행 ownership 설계: [#1553](https://github.com/bluetape4k/bluetape4k-projects/issues/1553)
- 구현 이슈: [#1562](https://github.com/bluetape4k/bluetape4k-projects/issues/1562)
- catalog/BOM 이슈: [bluetape4k-dependencies #213](https://github.com/bluetape4k/bluetape4k-dependencies/issues/213)
- MVC·virtual-thread consumer: [exposed-workshop #255](https://github.com/bluetape4k/exposed-workshop/issues/255)
- Reactor·Ktor consumer: [exposed-r2dbc-workshop #215](https://github.com/bluetape4k/exposed-r2dbc-workshop/issues/215)
- 기준 브랜치: `develop`의 `18472064c594ab2dee835cff6695cd6ef9538ea5`
- 작업 브랜치: `feat/issue-1562-tenant-context`
- 지원 기준: Java/JVM 25, Kotlin 2.4, Spring Boot 4.x, stable release 제외

## 결정 요약

이 문서에서 **tenant 값**은 검증이 끝난 `TenantId`, **carrier**는 tenant 값을 현재 실행
범위에 보관하는 구현, **binding**은 그 값을 carrier에 연결하는 동작, **consumer**는 공개
artifact를 사용하는 application 예제를 뜻한다. **unbound**는 현재 실행 범위에 tenant 값이
연결되지 않은 상태다.

공통 tenant 값과 no-default 조회 계약을 `bluetape4k-tenant`에 두고, 외부 타입을
공개 API에 노출하는 Reactor와 Ktor 연동은 각각 `bluetape4k-tenant-reactor`,
`bluetape4k-ktor-tenant`로 분리한다.

`bluetape4k-tenant`는 JDK 25 전용 모듈로 다음을 제공한다.

- 정규화된 tenant 식별자를 담는 `TenantId`
- `currentOrNull`, `requireCurrent`, `withTenant`의 no-default 계약
- platform thread와 Spring MVC에서 사용할 `ThreadLocalTenantContext`
- virtual thread의 동적 범위에서 사용할 `ScopedValueTenantContext`

Reactor adapter는 immutable subscriber `Context`를 파생하고 `ContextView`에서 읽는다.
Ktor adapter는 `ApplicationCall.attributes`에 값을 저장하고 읽는다. 두 adapter 모두
header parsing, tenant 존재 확인, 인증·인가, schema/database/connection routing,
persistence를 수행하지 않는다.

Servlet filter, Spring `WebFilter`, Ktor plugin은 애플리케이션 경계에 남긴다. 각
consumer는 요청 값을 검증한 뒤 adapter에 `TenantId`만 전달한다. 이 구분으로 공통
artifact는 transport와 보안 정책을 소유하지 않으면서 carrier 수명주기와 복원 규칙만
일관되게 제공한다.

## 문제와 현재 증거

멀티테넌트 예제는 같은 책임을 서로 다른 방식으로 반복 구현한다.

- `exposed-workshop` MVC 예제는 `ThreadLocal`에 값을 저장하고 `finally`에서
  `remove()`한다. 동일 servlet thread의 순차 요청, 중첩, downstream 예외 뒤 cleanup을
  이미 테스트한다.
- `exposed-workshop` virtual-thread 예제는 `ScopedValue`를 사용하지만 context가 없을 때
  default tenant로 돌아가고 `withTenant`의 tenant 인자에도 default가 있다. 이는 #1551의
  no-default 결정과 맞지 않는다.
- `exposed-r2dbc-workshop` WebFlux 예제는 문자열 key로 Reactor Context에 tenant를 넣는다.
  PR #214의 deterministic fixture는 interleaved subscription, nested success/failure,
  cancellation, 외부 context 비오염을 검증한다.
- 같은 PR의 Ktor fixture는 `ApplicationCall.attributes`가 dispatcher hop 뒤에도 요청별
  값을 보존하고 새 요청에 값을 남기지 않는지 검증한다. test-local 중첩 helper는 공통
  production API의 one-call/one-tenant 계약으로 대체한다.
- `bluetape4k-projects`에는 tenant 전용 production artifact가 없다. 기존
  `bluetape4k-core`, `bluetape4k-coroutines`, `bluetape4k-virtualthread-api`의 소유권을
  tenant 기능 때문에 넓히지 않는다는 #1553 결정이 존재한다.

따라서 이번 변경은 application-local 코드를 그대로 복사하는 작업이 아니다. carrier별
수명주기를 공통 no-default 의미로 묶고, 기존 deterministic fixture를 실제 독립 consumer
증거로 전환하는 production API 승격이다.

## 목표

1. 모든 carrier에서 context 부재를 `null` 또는 명시적 예외로 표현하고 default tenant를
   제공하지 않으며, carrier 수명주기에 맞는 명시적 binding만 허용한다.
2. ThreadLocal과 ScopedValue의 중첩·예외 복원을 공통 block API로 고정한다.
3. Reactor Context와 Ktor attributes의 carrier 고유 수명주기를 보존하면서 같은
   `TenantId`와 조회 의미를 사용한다.
4. framework 의존성을 선택 artifact에 격리한다.
5. 기존 네 consumer의 routing·authorization 동작을 보존하면서 application-local
   carrier 구현을 제거하거나 얇은 application boundary로 축소한다.
6. 새 artifact를 generated BOM, central SNAPSHOT, 중앙 catalog/aggregator 순서로 공개하고
   실제 consumer가 `2.0.0-SNAPSHOT`을 해석하도록 증명한다.

## 비목표

- default tenant 선택 또는 fallback 정책
- tenant header 이름, trim/case 규칙, 중복 header 처리
- tenant 존재 확인과 registry 조회
- 인증·인가, schema/database/connection 선택, transaction 또는 persistence
- Servlet filter, Spring `WebFilter`, Ktor plugin의 범용 구현
- `ThreadLocal`을 `ScopedValue`로 전면 대체하는 것
- coroutine suspension을 `ScopedValue`가 자동으로 가로지른다고 보장하는 것
- JDK 21 compatibility island에 tenant 모듈을 추가하는 것
- stable `2.0.0` 발행 또는 Maven Central release closeout

## 선택지와 결정

| 선택지 | 장점 | 문제 | 결정 |
| --- | --- | --- | --- |
| A. JDK core + Reactor + Ktor의 3-artifact 구조 | 외부 의존성을 격리하고 JDK carrier는 한 계약으로 테스트 가능 | 세 모듈·문서·catalog 등록이 필요 | **선택** |
| B. core, ThreadLocal, ScopedValue, Reactor, Ktor를 모두 별도 artifact로 분리 | carrier 소유권이 가장 명시적 | JDK 의존성만 가진 작은 artifact가 과도하게 늘고 consumer 설정이 복잡 | 제외 |
| C. 하나의 artifact에 Reactor/Ktor를 `compileOnly`로 추가 | coordinate가 하나 | 공개 signature의 외부 타입을 소비자가 해석할 수 없고 선택 의존성 실패가 런타임으로 이동 | 제외 |
| D. 기존 core/coroutines/virtualthread-api/ktor-core에 기능을 분산 | 새 artifact 수가 적음 | #1553의 dedicated owner 결정과 JDK 21 compatibility island를 훼손 | 제외 |

선택안 A에서 JDK carrier를 같은 artifact에 두는 이유는 두 구현이 외부 framework를
요구하지 않고 저장소 기본 JVM 25 target을 공유하기 때문이다. Reactor와 Ktor는 공개
signature가 각각 `reactor.util.context.*`, `ApplicationCall`을 사용하므로 별도 artifact의
`api` 의존성으로 분리한다.

## 모듈과 의존성 경계

### `bluetape4k/tenant` → `bluetape4k-tenant`

- package: `io.bluetape4k.tenant`
- 외부 framework 의존성 없음
- 저장소 기본 Java/JVM 25 target 사용
- `TenantId`, missing-context 예외, `TenantContext` 계약
- `ThreadLocalTenantContext`, `ScopedValueTenantContext`

### `bluetape4k/tenant-reactor` → `bluetape4k-tenant-reactor`

- package: `io.bluetape4k.tenant.reactor`
- `api(project(":bluetape4k-tenant"))`
- `api(libs.reactor.core)`
- 이번 범위는 coroutine Reactor context bridge를 제공하지 않으므로
  `kotlinx-coroutines-reactor`를 사용하거나 의존성에 추가하지 않는다.

### `ktor/tenant` → `bluetape4k-ktor-tenant`

- package: `io.bluetape4k.ktor.tenant`
- `api(project(":bluetape4k-tenant"))`
- `api(libs.ktor.server.core)`
- header parser나 installable plugin은 포함하지 않는다.

`settings.gradle.kts`의 기존 `includeModules("bluetape4k", ...)`와
`includeModules("ktor", withBaseDir = true)` 자동 등록을 사용한다. 수동 `include`를
추가하지 않는다. BOM은 `isPublishableLibraryProject()`가 반환하는 모든 publishable
subproject를 동적으로 constraint에 포함하므로 module registration과 generated POM을
함께 검증한다.

## core API 계약

공개 type과 함수 이름은 다음과 같이 고정한다. 구현 전에 작성하는 API compile test가
signature와 missing-context 예외 계약을 잠근다.

```kotlin
@JvmInline
value class TenantId(val value: String) {
    init {
        require(value.isNotBlank()) { "TenantId must not be blank" }
    }
}

interface TenantContext {
    fun currentOrNull(): TenantId?
    fun requireCurrent(): TenantId
    fun <T> withTenant(tenantId: TenantId, block: () -> T): T
}

class MissingTenantContextException : IllegalStateException("Tenant context is not bound")

val threadLocal: TenantContext = ThreadLocalTenantContext()
val scopedValue: TenantContext = ScopedValueTenantContext()
```

- `TenantId`는 생성 시 blank 값을 `IllegalArgumentException`과 정확한 메시지
  `TenantId must not be blank`로 거부하지만 trim, case conversion, tenant existence 확인은
  하지 않는다. `TenantId(" clinic-a ")`처럼 non-blank 원문은 그대로 보존하므로 원시 요청
  값의 정규화는 application boundary가 수행한다.
- `currentOrNull()`은 context가 없으면 `null`이다.
- `requireCurrent()`는 context가 없으면 정확히 `MissingTenantContextException`을 던진다.
  예외 메시지 `Tenant context is not bound`도 API compile/behavior test로 고정한다.
- `withTenant()`는 tenant 인자를 반드시 요구한다. default 인자와 fallback은 없다.
- 중첩 block은 outer tenant를 복원한다.
- outer tenant가 없던 block은 성공·예외 모두 종료 후 다시 비어 있어야 한다.
- carrier instance는 tenant 값을 log하거나 전역 registry에 기록하지 않는다.
- 두 concrete carrier는 public zero-argument constructor로 application과 test가 독립 instance를
  만들 수 있다. application configuration이 instance를 한 번 생성해 filter와 downstream에
  같은 identity로 주입하며, 서로 다른 instance는 값을 공유하지 않는다는 identity test를
  둔다. global object/default instance는 제공하지 않는다.

`ThreadLocalTenantContext`는 application singleton으로 생성해 filter와 downstream이 같은
instance를 사용한다. top-level block 종료 시 `ThreadLocal.remove()`를 호출한다. 이전
값이 있으면 `set(previous)`로 복원한다. 값의 공개 `set`/`clear` API는 제공하지 않아
application이 `finally`를 빼먹을 수 있는 표면을 줄인다.

`ScopedValueTenantContext`는 instance별 private key를 소유하고
`ScopedValue.where(key, tenantId).call/run`의 동적 범위를 사용한다. `ScopedValue.get()`을
unbound 상태에서 직접 호출하지 않고 먼저 `isBound`를 검사한다. 새로운 coroutine
dispatcher hop 전파를 약속하지 않으며, servlet virtual thread 안의 동기 호출과 JDK가
보장하는 structured inheritance 범위만 문서화한다.

## Reactor adapter 계약

Reactor Context는 immutable subscriber-local 값이므로 ThreadLocal처럼 set/clear하지 않는다.

```kotlin
object ReactorTenantContext {
    fun currentOrNull(context: ContextView): TenantId?
    fun requireCurrent(context: ContextView): TenantId
    fun withTenant(context: Context, tenantId: TenantId): Context
}
```

- key는 adapter가 소유하는 collision-resistant private object다. application 문자열 key를
  public contract로 사용하지 않는다.
- `withTenant`는 입력 `Context`를 수정하지 않고 tenant를 가진 새 `Context`를 반환한다.
- nested binding은 inner derived context에서만 값을 바꾸고 outer context는 그대로 둔다.
- subscription cancellation 뒤 외부 subscriber context에 tenant가 나타나지 않아야 한다.
- `Mono.deferContextual`/`Flux.deferContextual`이 같은 key와 no-default 의미를 사용한다.
- `requireCurrent`는 core의 `MissingTenantContextException`과 정확한 메시지를 그대로
  사용하며 Reactor 고유 예외로 감싸지 않는다.
- coroutine `ReactorContext` bridge는 이번 범위에서 제공하지 않는다. 별도 ambient coroutine
  API 없이 subscriber `ContextView`를 명시적으로 받는 것이 carrier 경계를 보존한다.
- adapter는 `Hooks`, automatic context propagation 또는 global hook을 설치하지 않는다.

## Ktor adapter 계약

Ktor adapter는 요청에 이미 존재하는 `ApplicationCall`을 명시적으로 받는다.

```kotlin
class TenantAlreadyBoundException :
    IllegalStateException("Tenant context is already bound to this call")

object KtorTenantContext {
    fun currentOrNull(call: ApplicationCall): TenantId?
    fun requireCurrent(call: ApplicationCall): TenantId
    fun bindTenant(call: ApplicationCall, tenantId: TenantId)
}
```

- adapter는 private `TenantBinding` holder type과 fully qualified versioned name
  `io.bluetape4k.ktor.tenant.binding.v1`을 가진 `AttributeKey<TenantBinding>`을 소유한다.
  외부 code가 같은 holder type을 참조할 수 없으므로 이름/공개 `TenantId` type만 같은 key와
  충돌하지 않는다. 회귀 test는 같은 이름의 외부 `AttributeKey<TenantId>`가 값을 가로채지
  못함을 검증한다.
- application plugin은 header를 검증하고 tenant 존재·권한을 확인한 뒤 request pipeline
  시작 전에 `bindTenant`를 정확히 한 번 호출한다.
- 이미 tenant가 bound된 call에 두 번째 `bindTenant`를 호출하면 실패한다. 같은 call에서
  tenant를 nested rebind하거나 concurrent coroutine마다 바꾸는 API는 제공하지 않는다.
- `bindTenant`는 non-suspending 함수이며 `ApplicationCall.attributes`를 하나의 monitor로
  동기화한 check-and-put으로 선형화한다. 먼저 성공한 호출만 값을 기록하고 나머지는 정확히
  `TenantAlreadyBoundException("Tenant context is already bound to this call")`을 받는다.
  이 예외는 잘못 구성된 pipeline을 뜻하므로 application의 일반 `500` 처리로 전달하며
  `400` 또는 `409`로 바꾸거나 기존 값을 덮어쓰지 않는다.
- dispatcher hop은 같은 `ApplicationCall`을 전달하는 Ktor의 요청 경계를 따른다.
- `requireCurrent`는 core의 `MissingTenantContextException`과 정확한 메시지를 그대로
  사용하며 Ktor 고유 예외로 감싸지 않는다.
- exception·cancellation 뒤 attribute를 별도로 global cleanup할 필요는 없다. 값의 owner가
  request-local `ApplicationCall`이고 call 자체가 요청 종료와 함께 폐기되기 때문이다. 새
  call 격리와 cancelled request retention test로 이 전제를 검증한다.
- adapter는 `createApplicationPlugin`, status mapping, response body를 제공하지 않는다.

따라서 carrier 공통 의미는 no-default 조회와 명시적 binding이지 동일한 mutable block
형태를 강제하는 것이 아니다. `withTenant`는 lexical JDK carrier와 immutable Reactor
Context 파생에 사용하고, Ktor는 one-call/one-tenant `bindTenant`만 제공한다.

## application 경계와 데이터 흐름

모든 reference consumer는 context binding 전에 다음 fail-closed 순서를 적용한다.

1. 인증이 있는 consumer는 먼저 principal을 인증한다.
2. transport boundary가 정확히 하나의 논리 tenant 값을 읽고, 길이 제한을 적용하고,
   conflicting/blank 값을 거부한다.
3. 각 application이 문서화한 trim, case, Unicode, 허용 문자 규칙으로 canonical ID를
   만든다. 공통 `TenantId`는 이 정규화를 대신하지 않는다.
4. registry/enum에서 tenant 존재를 확인한다.
5. 인증이 있는 consumer는 principal이 canonical tenant에 접근할 권한이 있는지 확인한다.
6. 위 검증을 모두 통과한 canonical ID로만 `TenantId`를 생성하고 carrier에 binding한다.

미인증은 기존 security chain의 `401`, 권한 불일치는 `403`, missing/blank/conflicting/unknown
tenant는 기존 reference consumer 계약의 `400`으로 응답한다. boundary 이후
`MissingTenantContextException`이 발생하면 application wiring 오류이므로 `500`으로
종료하며 default tenant로 복구하지 않는다. 인증 기능이 없는 Ktor/WebFlux 예제는
인증·인가 단계가 N/A임을 README에 명시하되 unknown tenant를 binding 전에 거부한다.

### Spring MVC / platform thread

1. filter가 header와 authentication을 검증한다.
2. authenticated tenant 권한을 확인한 canonical application enum/domain tenant를 공통
   `TenantId`로 변환한다.
3. application singleton `tenantContext: ThreadLocalTenantContext`를 filter에 주입하고
   `tenantContext.withTenant(tenantId) { chain.doFilter(request, response) }` 안에서 chain을
   실행한다.
4. adapter가 nested 복원과 top-level `remove()`를 보장한다.

### Spring MVC / virtual thread

1. filter가 header를 canonicalize하고 tenant 존재를 확인하며 default fallback 없이
   실패시킨다.
2. application singleton `tenantContext: ScopedValueTenantContext`를 filter에 주입하고
   `tenantContext.withTenant(tenantId) { chain.doFilter(request, response) }` 안에서 chain을
   실행한다.
3. block 밖에서는 unbound 상태다.

### WebFlux / Reactor

1. `WebFilter`가 header를 canonicalize하고 tenant 존재와 적용 가능한 권한을 검증한다.
2. `chain.filter(exchange).contextWrite { ReactorTenantContext.withTenant(it, tenantId) }`로
   subscriber context를 파생한다.
3. routing connection factory는 `deferContextual`에서 `requireCurrent(contextView)`를
   사용한다.

### Ktor

1. application plugin이 header 길이·중복·blank·unknown tenant를 검증한다.
2. 검증된 값만 `TenantId`로 만들어 `bindTenant`로 call attribute에 한 번 바인딩한다.
3. repository/routing은 `requireCurrent(call)`로 값을 읽고 application enum에 매핑한다.

### carrier 선택표

| application 실행 경계 | 선택할 carrier | 지원 범위 | 지원하지 않는 사용 |
| --- | --- | --- | --- |
| Servlet MVC / platform thread | singleton `ThreadLocalTenantContext` | 동일 thread의 동기 filter-chain block | suspend, dispatcher hop, 비동기 callback 자동 전파 |
| Servlet MVC / virtual thread | singleton `ScopedValueTenantContext` | virtual thread의 동기 lexical block과 JDK structured inheritance | 임의 coroutine suspension·dispatcher hop 자동 전파 |
| Spring WebFlux | `ReactorTenantContext` | subscription-local Reactor `Context` | ThreadLocal bridge, global hook, coroutine 자동 전파 |
| Ktor | `KtorTenantContext` | one-call/one-tenant `ApplicationCall` attribute | nested 또는 concurrent rebind |

모든 consumer는 raw header를 직접 `TenantId`로 감싸지 않는다. 예를 들어 application이
소유한 `TenantKey` enum/domain registry가 다음 순서를 수행한 뒤 공통 API로 넘긴다.

```kotlin
val canonical = rawHeader.trim().lowercase(Locale.ROOT)
val tenantKey = TenantKey.entries.singleOrNull { it.externalId == canonical }
    ?: throw UnknownTenantException()
authorize(principal, tenantKey)
val tenantId = TenantId(tenantKey.externalId)
```

MVC는 이 `tenantId`를 `withTenant` block에, WebFlux는 `contextWrite`에, Ktor는 최초
`bindTenant`에 전달한다. 예제의 `UnknownTenantException`도 raw 값이나 tenant 값을 메시지,
로그, metric tag에 기록하지 않는 application-local 예외로 구현한다.

## 호환성과 migration

- 새 artifact 추가이므로 기존 Projects 소비자의 binary/source contract는 바꾸지 않는다.
- 세 artifact는 모두 JDK 25 전용이다. `bluetape4k-virtualthread-api`와
  `bluetape4k-virtualthread-jdk21`은 변경하지 않는다.
- workshop migration은 consumer별 additive 순서로 진행한다. 한 consumer의 local
  carrier를 제거하기 전에 동일 테스트가 SNAPSHOT artifact를 사용해 통과해야 한다.
- application-local header/auth/routing 코드는 유지하므로 persistence 또는 data migration은
  없다.
- rollback은 dependency와 adapter 사용을 제거하고 직전 local carrier를 복원하는 방식이다.
  SNAPSHOT artifact가 다른 consumer에 배포됐더라도 각 consumer는 독립적으로 rollback할
  수 있다.
- stable version을 요구하는 consumer에는 이번 SNAPSHOT train을 적용하지 않는다.

## 4개 저장소 delivery DAG

| 순서 | 저장소·이슈 | base | head | 산출물 | 다음 단계 gate |
| --- | --- | --- | --- | --- | --- |
| 1 | `bluetape4k-projects` #1562 | `develop` | `feat/issue-1562-tenant-context` | 3개 artifact와 generated BOM constraint | exact-head PR merge 후 SNAPSHOT dispatch |
| 2 | `bluetape4k-dependencies` #213 | `develop` | `feat/issue-213-tenant-context-catalog` | 3개 alias와 aggregator catalog | Projects public SNAPSHOT POM read-back |
| 3 | `exposed-workshop` #255 | `develop` | `feat/issue-255-tenant-context-consumer` | MVC ThreadLocal·virtual-thread ScopedValue reference consumer | Dependencies public SNAPSHOT read-back |
| 4 | `exposed-r2dbc-workshop` #215 | `develop` | `feat/issue-215-tenant-context-consumer` | Reactor·Ktor reference consumer | Dependencies public SNAPSHOT read-back |

3과 4는 서로 독립이며 같은 verified Dependencies SNAPSHOT build를 기준으로 병렬 진행할 수 있다.
각 저장소의 실제 branch는 시작 직전에 최신 `origin/develop`에서 생성하고 표의 base/head를
PR read-back으로 다시 확인한다.

## SNAPSHOT publish gate

1. Projects PR의 exact head, required CI, review, mergeability를 재확인하고 fresh merge 승인을
   받는다.
2. merge SHA에서 성공한 required CI run ID를 고정한다. 수동 publish workflow 입력은
   `expected_head_sha`와 `verified_ci_run_id`를 필수로 받고, CI conclusion이 `success`이며
   CI의 `head_sha`와 현재 `develop` head가 모두 `expected_head_sha`와 같을 때만 해당 SHA를
   checkout한다. branch 이름만 checkout하거나 현재 head를 암묵적으로 사용하면 실패한다.
3. 그 SHA에서 SNAPSHOT workflow의 version/target/credentials/dispatch hold를 새로 읽고,
   별도 승인된 workflow dispatch의 publication run ID를 기록한다. Dependencies의 현재
   `workflow_dispatch`/`workflow_run` checkout도 이 exact-head 계약으로 보강하기 전에는
   중앙 SNAPSHOT을 발행하지 않는다.
4. `nmcpPublishAggregationToCentralPortalSnapshots`가 성공한 뒤 공개
   `bluetape4k-bom:2.0.0-SNAPSHOT` POM에서 세 artifact constraint를 확인한다.
5. Projects handoff receipt에 repository merge SHA, required CI run ID, publication run ID,
   Maven metadata의 timestamp/build number와 `lastUpdated`, 공개 BOM POM 및 세 artifact
   POM/JAR SHA-256을 기록한다. SNAPSHOT coordinate 자체를 immutable로 간주하지 않는다.
6. Dependencies가 이 immutable handoff receipt로 식별한 Projects BOM build를 참조하도록
   alias와 generated catalog를 갱신하고 별도 PR/merge/SNAPSHOT gate를 통과한다.
7. Dependencies handoff에도 merge SHA, required CI run ID, publication run ID, catalog commit
   SHA, timestamp/build number, `lastUpdated`, 공개 POM/catalog SHA-256을 기록한다.
8. 공개 `bluetape4k-dependencies:2.0.0-SNAPSHOT` POM/catalog가 세 alias를 해석하는지
   확인한 뒤에만 workshop dependency version을 `2.0.0-SNAPSHOT`으로 전환한다.

handoff receipt의 schema는 `bluetape.snapshot-handoff/v1`로 고정하고 publish workflow가
`tenant-context-handoff.json`을 생성한다. 필수 필드는 `repository`, `merge_sha`,
`verified_ci_run_id`, `publication_run_id`, `group`, `artifact`, `base_version`, `timestamp`,
`build_number`, `last_updated`, `resources[{url,sha256}]`, `catalog_commit_sha`, `created_at`,
`status`다. 적용되지 않는 `catalog_commit_sha`는 `null`이고 `status`는 최초 `verified`,
실패한 downstream이 있으면 새 append-only receipt의 `supersedes`로 연결해 `rejected`로
표시한다. 기존 receipt 파일은 수정하지 않는다.

publish workflow는 receipt를 GitHub Actions v4 immutable artifact
`tenant-context-handoff-<merge_sha>-<timestamp>-<build_number>`로 90일 보존하고 artifact ID와
digest를 job summary 및 연결된 GitHub issue에 기록한다. release coordinator가 owner이며,
downstream workflow는 repository/run/artifact ID로 receipt를 다운로드하고 artifact digest,
schema, exact SHA, 모든 resource SHA-256을 검증한다. 90일을 넘겨 재현해야 하면 같은 mutable
coordinate를 신뢰하지 않고 새 exact-head CI/publish gate로 새 receipt를 만든다.

consumer build는 `--refresh-dependencies`와 changing-module cache 0을 사용한다. build 시작과
종료에 Maven metadata를 다시 읽어 receipt의 `timestamp`/`build_number`/`last_updated`와
같은지 확인하고, 실제 다운로드한 BOM/POM/JAR resource의 SHA-256을 receipt와 비교한다.
중간에 metadata가 바뀌거나 하나라도 다르면 build를 실패시킨다. 즉 Gradle 선언은
`2.0.0-SNAPSHOT`을 유지하지만 성공 증거는 receipt가 고정한 timestamped resource다.

local Maven publish, Gradle 성공, 이전 SNAPSHOT build 또는 HTTP metadata 존재만으로 다음
저장소를 시작하지 않는다. 공개 POM/catalog의 실제 constraint와 immutable source SHA가
handoff 증거다.

각 저장소 작업을 시작하기 전에 `last-good-manifest.json`에 현재 base SHA, dependency
coordinate, resolved timestamp/build number, catalog commit SHA, resource checksum, 통과한
test command/run ID를 기록한다. downstream 검증이 실패하면 열차를 즉시 중단하고 실패
receipt를 `rejected`로 supersede한 뒤, 아직 merge 전이면 branch에서 dependency 변경을
되돌리고 merge 후면 별도 revert PR을 만든다. 그 다음 manifest가 고정한 catalog/dependency로
consumer를 복원해 같은 테스트를 재실행한다. 복원 증거가 없으면 train을 재개하지 않는다.
upstream 수정은 새 merge SHA와 새 timestamped SNAPSHOT receipt를 만들고 실패한 단계부터
다시 시작한다.

## 테스트 계약

### `bluetape4k-tenant`

1. `TenantId`가 blank를 거부하고 application 정규화를 대신하지 않는다.
2. ThreadLocal `currentOrNull`은 미설정 시 `null`, `requireCurrent`는 명시적 예외다.
3. ThreadLocal nested success/failure 뒤 outer tenant가 복원된다.
4. top-level success/failure 뒤 동일 platform thread에서 값이 없다.
5. 고정 thread pool의 교차 tenant 순차/병렬 실행에서 값이 섞이지 않는다.
6. ScopedValue unbound 조회, nested success/failure, block 종료 비오염을 검증한다.
7. virtual-thread request-shaped 실행에서 각 tenant가 자기 값만 읽는다.
8. ScopedValue가 coroutine suspension 자동 전파를 보장하지 않는다는 문서와 test 범위를
   일치시킨다.

### `bluetape4k-tenant-reactor`

1. 서로 다른 tenant의 두 subscription을 single-thread scheduler에서 의도적으로
   interleave한다.
2. nested derived context success/failure 뒤 outer context가 유지된다.
3. cancellation 뒤 외부 context의 `currentOrNull`이 `null`이다.
4. missing context의 `requireCurrent`가 즉시 실패한다.
5. 원본 `Context`가 `withTenant` 호출로 변경되지 않는다.
6. 한 subscription당 boundary binding은 한 번만 수행하며 signal마다 `Context.put`하지
   않는다.

### `bluetape4k-ktor-tenant`

1. `ApplicationCall.attributes` 미설정 조회와 require failure를 검증한다.
2. 같은 call의 두 번째 binding을 거부한다. concurrent fixture는 start barrier 뒤 서로 다른
   tenant로 동시에 bind해 정확히 한 호출만 성공하고 나머지는 모두 정확한
   `TenantAlreadyBoundException`을 받으며, 이후 조회가 승자의 값이고 overwrite가 없음을
   검증한다.
3. dispatcher hop을 포함한 overlapping test에서 expected/observed tenant가 일치한다.
4. exception·cancellation 뒤 새 call은 이전 요청의 tenant를 볼 수 없다.
5. cancelled call이 adapter 또는 global registry에 retained되지 않는다.

### reference consumer

- exposed-workshop의 기존 MVC 동일-thread, downstream failure, nested test를 보존한다.
- virtual-thread 예제의 default fallback을 제거하고 missing tenant failure를 추가한다.
- exposed-r2dbc-workshop PR #214의 Reactor 35/35, Ktor 11/11 deterministic fixture를
  SNAPSHOT artifact API로 재실행한다. test-local Ktor nested rebind helper는 production
  adapter API로 승격하지 않고 one-call/one-tenant 계약에 맞게 조정한다.
- routing/authorization 결과와 HTTP error semantics가 migration 전과 동일한지 검증한다.

## 문서 계약

- 세 module에 `bluetape4k/tenant/README{,.ko}.md`,
  `bluetape4k/tenant-reactor/README{,.ko}.md`, `ktor/tenant/README{,.ko}.md`를 두고 정확한
  group `io.bluetape4k`, version `2.0.0-SNAPSHOT`, JDK 25 requirement, no-default,
  lifecycle, unsupported boundary, 최소 사용 예제를 맞춘다.
- 각 README는 `platform("io.bluetape4k:bluetape4k-bom:2.0.0-SNAPSHOT")`과 해당 artifact
  `bluetape4k-tenant`, `bluetape4k-tenant-reactor`, `bluetape4k-ktor-tenant`의 Gradle Kotlin
  DSL 및 Maven 예시를 제공한다. SNAPSHOT 소비 예시는 Maven Central snapshots repository와
  changing-module cache 정책이 필요하다는 조건을 함께 표시한다.
- reader-facing KDoc은 한국어로 작성하고 API/identifier는 원문을 유지한다.
- root README/README.ko의 module 목록에 세 artifact를 같은 의미로 등록한다.
- ThreadLocal README는 application singleton과 `withTenant` 사용을 강조하고 raw set/clear
  예제를 제공하지 않는다.
- ScopedValue README는 coroutine suspension 자동 전파가 아님을 명시한다.
- Reactor README는 immutable derived context와 global hook 미설치를 명시한다.
- Ktor README는 header/auth/plugin이 application 소유임을 명시한다.
- production log, exception, MDC, metric tag에는 raw header, token, tenant 값을 기록하지
  않는다. synthetic fixture trace만 expected/observed tenant를 사용할 수 있다. 운영 진단은
  carrier 종류, bound 여부, 실패 단계처럼 값이 아닌 상태만 남긴다.
- library는 logging/metric backend를 추가하지 않는다. reference consumer가 기존
  observability stack으로 `tenant_context_binding_failures_total{carrier,stage}`를 선택적으로
  기록할 때 `carrier`와 `stage`는 문서화한 enum만 사용하고 tenant 값은 label에 넣지 않는다.
  기존 correlation/trace ID만 log에 연결한다. boundary 이후 missing 또는 duplicate binding이
  5분 구간에 한 건이라도 있으면 application wiring alert 대상으로 삼고, 각 workshop
  maintainer가 owner, release coordinator가 SNAPSHOT train 동안 확인 owner다.

## 성능·retention 계약

- `ThreadLocalTenantContext`와 `ScopedValueTenantContext`는 application singleton으로
  생성한다. request마다 context instance나 key를 만들지 않는다.
- Reactor binding은 subscription boundary에서 한 번만 수행하며 signal마다 `Context.put`을
  호출하지 않는다.
- fixed 8-thread platform executor와 100개 canonical tenant를 round-robin하는 10,000개
  virtual-thread task의 overlap/retention stress를 60초 timeout으로 실행한다. 모든 task가
  자기 tenant만 보고 block 종료 직후 unbound인지 확인한다.
- retention stress는 intern하지 않은 unique tenant 문자열의 `WeakReference`/`ReferenceQueue`를
  사용한다. task와 executor 종료 후 최대 10초 동안 bounded allocation pressure와 GC를
  수행해 모든 sentinel이 enqueue되는지 확인한다. 결과와 JUnit XML은 각 module의
  `build/reports/tests/tenantRetentionStress/`와 `build/test-results/tenantRetentionStress/`에
  보존한다. 기능 test의 remove/unbound 증거가 우선이며 이 GC 기반 test가 환경상 불안정하면
  통과로 간주하지 않고 test log와 heap/class histogram을 첨부해 별도 blocker로 판정한다.
- generic Reactor/Ktor carrier에 저장되는 `TenantId` boxing과 immutable `Context` 파생은
  boundary당 한 번 허용한다.
- 이번 PR은 새 benchmark module이나 CI latency threshold를 추가하지 않는다. 작업이 요청
  경계당 한 번이고 안정적인 기존 baseline이 없어 noisy microbenchmark를 release gate로
  사용하지 않는다. implementation inspection과 bounded stress에서 per-signal write 또는
  request별 key 생성이 발견되면 별도 benchmark issue를 등록한다.

## 실패 모드와 대응

| 실패 모드 | 탐지 | 대응 |
| --- | --- | --- |
| ThreadLocal이 top-level 종료 뒤 값을 남김 | 동일 thread 순차 요청·예외 test | public set/clear를 숨기고 `finally`에서 `remove()` |
| nested block이 outer tenant를 지움 | nested success/failure test | 이전 값 기록 후 restore/remove 분기 |
| ScopedValue unbound `get()`이 실패 | unbound currentOrNull test | `isBound` 확인 뒤 조회 |
| virtual-thread 지원을 coroutine 전파로 오해 | dispatcher-hop negative scope·README audit | 지원 범위를 동기 servlet/structured JDK scope로 제한 |
| Reactor 문자열 key가 다른 library와 충돌 | key identity test | adapter private object key 사용 |
| Reactor binding이 원본 Context를 오염 | identity/outer-context test | immutable derived `Context`만 반환 |
| cancellation 후 tenant가 외부 subscriber에 노출 | deterministic cancel fixture | global hook과 ThreadLocal bridge를 설치하지 않음 |
| 같은 Ktor call을 concurrent rebind해 confused tenant가 보임 | duplicate/concurrent bind negative test | one-call/one-tenant를 강제하고 두 번째 bind 실패 |
| 검증 전에 tenant가 binding됨 | unauthenticated/unknown/unauthorized negative test | canonicalize·existence·authorization 뒤에만 `TenantId` 생성/binding |
| raw tenant/header/token이 로그로 노출됨 | source/log capture review | production logging 금지, synthetic test trace만 허용 |
| adapter가 header/auth/routing 정책을 소유 | dependency/source review | filter/plugin을 consumer에 유지하고 adapter API를 carrier 동작으로 제한 |
| core가 Reactor/Ktor를 transitively 끌어옴 | outgoing dependency/POM audit | 외부 타입 adapter를 별도 artifact로 격리 |
| 새 module이 자동 등록·Kover·BOM에서 누락 | `projects`, Kover graph, generated POM inventory audit | 기존 auto-registration과 publishable-project predicate 사용 |
| stale SNAPSHOT으로 workshop이 우연히 성공 | public POM/catalog SHA·timestamp read-back | Projects → Dependencies → consumers 순서와 immutable handoff receipt 고정 |
| local enum과 공통 TenantId 변환에서 의미가 바뀜 | migration parity test | normalization/existence mapping은 application boundary에 유지 |

## 검증 순서

구현 plan에서 정확한 task 존재를 다시 확인한 뒤 다음 순서로 검증한다.

```bash
./gradlew :bluetape4k-tenant:test \
  :bluetape4k-tenant-reactor:test \
  :bluetape4k-ktor-tenant:test \
  --no-daemon --no-configuration-cache --no-build-cache

./gradlew :bluetape4k-tenant:check \
  :bluetape4k-tenant-reactor:check \
  :bluetape4k-ktor-tenant:check \
  --no-daemon --no-configuration-cache --no-build-cache

./gradlew projects --no-daemon --no-configuration-cache --no-build-cache

./gradlew -p buildSrc test \
  --no-daemon --no-configuration-cache --no-build-cache

./gradlew :bluetape4k-bom:generatePomFileForBluetape4kPublication \
  :bluetape4k-bom:generateMetadataFileForBluetape4kPublication \
  -PsnapshotVersion=-SNAPSHOT \
  --no-daemon --no-configuration-cache --no-build-cache

ruby scripts/publication/validate_poms.rb
ruby scripts/publication/validate_module_metadata.rb
git diff --check
```

repository-wide `detekt`, disabled-test gate, CI path filter, root README generation/audit와
SNAPSHOT workflow policy 검사는 implementation plan에서 module addition hazard로 배정한다.
Testcontainers나 Docker는 세 Projects module unit test에 필요하지 않는다. workshop의 기존
database fixture는 각 저장소 규칙에 따라 순차 실행한다.

## 수용 기준

- [ ] 세 artifact가 정확한 project path와 dependency direction으로 자동 등록된다.
- [ ] public API가 모든 carrier에서 `currentOrNull`/`requireCurrent`의 no-default 의미를
      유지하고, JDK/Reactor는 `withTenant`, Ktor는 one-call/one-tenant `bindTenant`를
      제공한다.
- [ ] ThreadLocal 동일-thread 순차 요청, nested, downstream exception 뒤 누수가 없다.
- [ ] JDK 25 ScopedValue가 dynamic scope와 nested 복원을 보장한다.
- [ ] Reactor interleave/nested/cancel과 Ktor dispatcher/duplicate/concurrent/new-call fixture가
      통과한다.
- [ ] core artifact POM에 Reactor/Ktor/Servlet/Spring 의존성이 없다.
- [ ] root/module README와 KDoc이 lifecycle, unsupported boundary, JDK 25를 설명한다.
- [ ] generated BOM/POM/module metadata가 세 artifact를 포함한다.
- [ ] 공개 `bluetape4k-bom:2.0.0-SNAPSHOT`에 세 constraint가 존재한다.
- [ ] 공개 `bluetape4k-dependencies:2.0.0-SNAPSHOT` catalog/aggregator가 세 alias를 제공한다.
- [ ] 두 workshop reference consumer가 실제 SNAPSHOT artifact로 기존 acceptance를 통과한다.
- [ ] #1552의 남은 ThreadLocal/Servlet/virtual-thread proof와 #1320의 production consumer
      acceptance가 live issue에 reconciliation된다.

## DoD

- [ ] spec과 implementation plan이 독립 6-perspective review에서 P0=0/P1=0이다.
- [ ] TDD RED/GREEN 증거와 module별 fresh test count를 기록한다.
- [ ] module registration, detekt, Kover, README, CI, publication metadata hazard가 모두
      PASS 또는 근거 있는 N/A다.
- [ ] Projects PR은 exact head와 required CI를 확인하고 fresh merge 승인 전에는 병합하지
      않는다.
- [ ] 두 SNAPSHOT dispatch는 각각 최신 workflow/target/credential/hold를 확인한 뒤에만
      실행한다.
- [ ] Dependencies와 두 workshop PR도 base/head/CI/review를 독립적으로 검증한다.
- [ ] stable release는 실행하지 않는다.
- [ ] #1562, #1552, #1320과 세 cross-repo child issue의 live 상태를 acceptance evidence와
      맞춰 closeout한다.

## 추적성

| 요구 | 설계 위치 | 검증 증거 |
| --- | --- | --- |
| no-default core | core API 계약 | missing/blank/require test |
| MVC ThreadLocal 누수 방지 | application 경계, core test | same-thread·nested·exception fixture |
| JDK 25 virtual thread | core API, 비목표 | ScopedValue dynamic-scope fixture |
| WebFlux Reactor | Reactor adapter 계약 | interleave·nested·cancel fixture |
| Ktor call context | Ktor adapter 계약 | dispatcher·duplicate·concurrent·new-call fixture |
| auth/routing application ownership | application 경계 | consumer source review·parity test |
| BOM SNAPSHOT | SNAPSHOT publish gate | public BOM POM constraint read-back |
| central catalog | delivery DAG | Dependencies generated catalog와 public SNAPSHOT |
| independent consumers | reference consumer | exposed-workshop #255, r2dbc #215 CI/test evidence |
| Epic closeout | 수용 기준·DoD | #1552/#1320 live reconciliation comment |

## source ledger

- 2026-08-28 live GitHub: #1320, #1552, #1553, #1562와 cross-repo #213/#255/#215
- Projects `develop`: `18472064c594ab2dee835cff6695cd6ef9538ea5`
- Dependencies `develop`: `df64293753a9491b337852a158f89d4a93a1734a`
- exposed-workshop `develop`: `2728f0543a8077e3b6ced4fbc384d45a21471dea`
- exposed-r2dbc-workshop `develop`: `a32f9cd47a33dfc857baf32e9e8f53d4534570b2`
- PR #214 merge SHA: `a32f9cd47a33dfc857baf32e9e8f53d4534570b2`
- Projects source: `settings.gradle.kts`, root Java 21 compatibility list,
  `bluetape4k/bom/build.gradle.kts`, `TaskContext`, Reactor/Ktor context examples
- Workshop source: MVC ThreadLocal/ScopedValue context와 filter, Reactor/Ktor deterministic fixture
- public SNAPSHOT metadata: `bluetape4k-bom:2.0.0-SNAPSHOT`와
  `bluetape4k-dependencies:2.0.0-SNAPSHOT`; 현재 BOM에는 tenant constraint 없음

## 독립 검토 결과

| 관점 | 1차 결과 | 반영 결과 |
| --- | --- | --- |
| 성능·retention | P0=0, P1=0, P2=3 | singleton·subscription당 1회 binding·10,000 task/60초와 retention evidence를 고정했다. |
| 안정성 | P0=0, P1=3, P2=2 | Ktor one-call 원자성, 공통 예외/API, coroutine 비보장, immutable SNAPSHOT receipt를 고정했다. |
| 보안 | P0=0, P1=2, P2=3 | canonicalization·인증·인가 순서, HTTP 상태, duplicate binding, 민감 값 비기록을 고정했다. |
| 개발자 API | P0=0, P1=1, P2=5 | `TenantId` 검증, carrier 생성·identity, Ktor private holder, 공통 예외, dependency/검증 명령을 고정했다. |
| 운영·release | P0=0, P1=4, P2=3 | exact-head CI/publish, append-only receipt schema, resolved checksum, rollback manifest, 운영 owner를 고정했다. |
| 사용자·caller | P0=0, P1=3, P2=3, P3=1 | 정확한 block 호출, carrier 선택표, application mapping 예시, README 좌표와 용어를 고정했다. |

모든 1차 P1과 actionable P2/P3를 본문에 반영했다. main integration readback 결과는
P0=0/P1=0이며 유예한 finding은 없다. 보안·안정성·성능 재검토 lane은 수정본 검토를 시작했으나
5분 command deadline 안에 결과를 반환하지 못해 회수했으며, 이를 finding closure 증거로
사용하지 않는다.

## 작성·검토 게이트

- SPW-01 source ledger: live issue/branch SHA, Projects module/publish source, 네 consumer와
  deterministic fixture를 확인했다.
- SPW-02 spec structure: 문제, 목표, 비목표, 대안, 선택, API, lifecycle, migration,
  failure mode, test, SNAPSHOT DAG, acceptance와 DoD를 포함한다.
- SPW-03 Korean naturalness: reader-facing prose는 한국어로 쓰고 API, artifact, 명령,
  URL과 exact identifier는 원문을 보존한다.
- SPW-04 traceability: #1562 수용 기준과 cross-repo handoff를 설계·검증 표로 연결했다.
- SPW-05 readback: 독립 review 수정 뒤 전체 파일을 다시 읽고 placeholder, stale SHA,
  unchecked 설계 결정과 SNAPSHOT 순서 모순이 없는지 확인한다.
