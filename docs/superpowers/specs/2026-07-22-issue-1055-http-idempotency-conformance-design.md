# Issue #1055 HTTP Idempotency Conformance 설계

Date: 2026-07-22
Repo: `bluetape4k-projects`
Issue: [#1055](https://github.com/bluetape4k/bluetape4k-projects/issues/1055)
Target milestone: `1.12.0`

## 문제

Order Lifecycle, Reservation Control Plane, Promotion & Voucher reference application은
모두 HTTP command 재시도에서 같은 문제를 해결한다. 같은 tenant와 operation에서 같은
`Idempotency-Key`와 같은 request가 다시 오면 이미 얻은 결과를 재생해야 하고, 같은 key에
다른 request가 오면 거절해야 한다. 첫 요청이 아직 실행 중이거나, 실패·취소·retention 만료
경계에 있을 때의 동작도 framework와 application마다 달라지면 안 된다.

현재 `bluetape4k-projects`에는 generic production idempotency adapter가 없다. 이를 먼저
만들면 PostgreSQL, Redis, distributed lock 또는 repository API를 성급하게 공통 계약으로
고정하게 된다. 이번 작업은 application 구현을 추출하지 않고, 여러 HTTP stack이 같은
동작을 증명할 수 있는 test contract와 conformance fixture만 제공한다.

## 현재 근거

- `testing/junit5/.../HttpOperationObservabilityConformance.kt`는 framework-neutral snapshot과
  assertion을 `bluetape4k-junit5`에 두고 Spring Boot/Ktor 테스트가 각 framework 결과를
  변환하는 선례다.
- `spring-boot/core/.../SpringObservationSupportTest.kt`와
  `ktor/observability/.../Bluetape4kKtorObservabilityTest.kt`는 같은 observability fixture를
  framework별 test application에서 실행한다.
- issue #1055가 consumer로 고정한 workshop [#532](https://github.com/bluetape4k/bluetape4k-workshop/issues/532),
  [#533](https://github.com/bluetape4k/bluetape4k-workshop/issues/533),
  [#534](https://github.com/bluetape4k/bluetape4k-workshop/issues/534)는 persistence 구현을
  공유시키지 말고 replay, payload conflict, in-flight, expiry와 failed terminal result를 같은
  black-box contract로 검증하라고 요구한다. 이 설계는 변할 수 있는 consumer 구현 세부가 아니라
  issue #1055의 고정된 scope와 acceptance criteria를 근거로 삼는다.
- `testing/junit5`는 이미 `bluetape4k-assertions`, JUnit 5, coroutine core와 deterministic test
  helper를 제공한다. 신규 module이나 dependency가 필요하지 않다.

외부 SDK나 version-sensitive API를 새로 선택하지 않으므로 이번 설계에는 별도 web dependency
research가 필요하지 않다. Spring Boot와 Ktor adapter는 현재 repository의 실제 test API를
기준으로 작성한다.

## 목표

1. 가장 작은 framework-neutral HTTP idempotency contract를 정의한다.
2. same-key/same-payload replay와 same-key/different-payload conflict를 실제 HTTP 요청으로 검증한다.
3. 명시적인 in-flight 정책으로 bounded wait와 waiter limit를 검증한다.
4. timeout, cancellation, abandon/retry, terminal failure, retention 만료를 deterministic하게 검증한다.
5. Spring Boot와 Ktor test application이 scenario 제외 없이 같은 runner를 실행하게 한다.
6. application이 transaction, persistence, rate limit, authentication과 lifecycle을 계속 소유하게 한다.
7. README와 manual에 정책 선택 기준과 exactly-once 비보장 범위를 설명한다.

## 비목표

- generic production idempotency store, filter, plugin, interceptor 또는 repository API를 만들지 않는다.
- PostgreSQL, Redis, distributed lock, pub/sub 또는 특정 wake-up mechanism을 기본값으로 선택하지 않는다.
- workshop repository의 세 application을 이번 PR에서 변경하지 않는다.
- payment, reservation, voucher domain model이나 deterministic provider를 fixture에 넣지 않는다.
- HTTP response replay가 외부 side effect의 exactly-once 실행을 보장한다고 주장하지 않는다.
- 장기 operation을 위한 operation resource와 status 조회 API를 만들지 않는다.
- streaming request/response, multipart/file upload, SSE, WebSocket, protocol upgrade와
  non-repeatable payload를 지원하지 않는다.
- tenant rate limiter, authentication, authorization 또는 abuse detection을 idempotency fixture로 대체하지 않는다.

## 선택지

### 선택지 A: in-flight 요청 즉시 거절

동일 fingerprint의 요청이 실행 중이면 `409`를 즉시 반환하고 client가 나중에 재시도한다.

장점은 connection과 waiter 상태를 유지하지 않아 구현이 단순하다는 것이다. 단점은 모든 client가
backoff, retry budget과 jitter를 구현해야 하고, network retry가 이미 발생한 상황에서 추가 retry
storm을 만들 수 있다는 것이다.

결정: 거절.

### 선택지 B: bounded wait와 waiter limit

동일 fingerprint의 요청은 기존 실행에 합류해 설정된 시간까지만 기다린다. 완료되면 같은 terminal
결과를 받고, timeout되면 명시적인 in-flight 응답을 받는다. key별 waiter 수도 제한한다.

operation latency, upstream deadline, expected duplicate fan-in과 connection budget이 아래 suitability
gate를 만족하는 consumer에서 client 동작을 단순하게 하며, timeout과 capacity를 명시해 무제한
connection 점유를 막는다. 각 adopter가 이 조건을 입증해야 한다.

결정: 채택.

### 선택지 C: `202 Accepted`와 status resource

첫 요청이 operation resource를 만들고 client가 `Location`을 조회한다. 장기 실행에는 적합하지만
operation ID, 조회 endpoint, retention, authorization과 상태 모델이 새로운 public HTTP contract가
된다. 이번 이슈의 최소 contract 범위를 넘는다.

결정: 거절. 장기 workflow가 실제 consumer 요구로 확인되면 별도 이슈에서 검토한다.

## Fixture 구조 선택

### 채택: 공통 active scenario runner와 얇은 framework adapter

`bluetape4k-junit5`에 suspend 기반 runner, HTTP request/response snapshot, deterministic control
계약을 둔다. runner가 중복 HTTP request를 직접 조합하고 동시 실행해 observable behavior를
검증한다. Spring Boot와 Ktor 테스트는 request 전송, fake clock, 실행 gate와 side-effect count를
연결하는 얇은 test adapter만 제공한다.

driver의 clock/gate/probe는 test application을 deterministic하게 제어하기 위한 test surface다.
production persistence, lock 또는 wake-up API가 아니다. test server, registry, coroutine scope와
executor lifecycle은 각 framework test가 소유하고 닫는다.

### 거절: snapshot assertion만 제공

observability fixture와 같은 snapshot-only 형태는 terminal result 비교에는 충분하지만 in-flight
wait, waiter overflow, cancellation과 side-effect single execution을 증명하지 못한다.

### 거절: framework별 base class

Spring/Ktor base test class는 설정이 쉬워 보여도 상속 구조와 lifecycle hook을 강제한다. 다른
HTTP stack이 fixture를 사용하기 어렵고 framework별 scenario 제외가 생길 가능성이 높다.

## Package와 public surface

공통 코드는 `io.bluetape4k.junit5.http.idempotency` package에 둔다. 구현 계획에서 실제 source
영향을 다시 확인하되 다음 역할을 유지한다.

| 역할 | 설계 이름 | 책임 |
| --- | --- | --- |
| request snapshot | `HttpIdempotencyRequest` | operation, resource identity, key header values와 bounded request body를 test request로 표현 |
| response snapshot | `HttpIdempotencyResponse` | status, body, replay-safe header와 stable problem code를 표현 |
| expectation/config | `BoundedWaitHttpIdempotencyConformanceConfig` | timeout, waiter limit, retention, retry hint와 replay bounds를 명시 |
| framework boundary | `BoundedWaitHttpIdempotencyAdapter` | request exchange와 deterministic test control을 제공 |
| runner | `assertBoundedWaitHttpIdempotencyConformance` | bounded-wait scenario 전체를 같은 순서와 assertion으로 실행 |

이름에 `BoundedWait`를 포함해 이 runner가 모든 idempotency 정책의 보편 판정기가 아님을 드러낸다.
즉시 거절과 `202` status resource 구현은 이 profile 밖에 있으며, 그 이유만으로 비준수라고 판정하지
않는다. 구현 계획에서 유지할 공개 declaration 모양은 다음과 같다.

```kotlin
class HttpIdempotencyRequest(
    val authenticationProfile: String,
    val operation: String,
    val resourceIdentity: String,
    idempotencyKeys: List<String>,
    val requestBody: String,
): Serializable {
    val idempotencyKeys: List<String>
}

class HttpIdempotencyResponse(
    val statusCode: Int,
    val body: String,
    headers: Map<String, List<String>>,
    val problemCode: String? = null,
): Serializable {
    val headers: Map<String, List<String>>
}

class BoundedWaitHttpIdempotencyConformanceConfig(
    val waitTimeout: Duration,
    val scenarioTimeout: Duration,
    val maxWaitersPerKey: Int,
    val retention: Duration,
    val inFlightRetryAfter: Duration,
    val overflowRetryAfter: Duration,
    val maxIdempotencyKeyBytes: Int,
    val maxRequestBodyBytes: Int,
    val maxReplayBodyBytes: Int,
    val maxReplayHeaderNames: Int,
    val maxReplayValuesPerHeader: Int,
    val maxReplayHeaderValueBytes: Int,
    val maxReplayHeaderBytes: Int,
    replayHeaderAllowlist: Set<String> = emptySet(),
): Serializable {
    val replayHeaderAllowlist: Set<String>
}

data class HttpIdempotencyQuiescence(
    val activeWaiters: Int,
    val openGates: Int,
    val activeChildTasks: Int,
): Serializable

interface BoundedWaitHttpIdempotencyAdapter {
    suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse
    suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest)
    suspend fun awaitWaiterCount(request: HttpIdempotencyRequest, expected: Int)
    suspend fun completeOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse)
    suspend fun holdOwnerResponseDelivery(request: HttpIdempotencyRequest)
    suspend fun releaseOwnerResponseDelivery(request: HttpIdempotencyRequest)
    suspend fun abandonOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse)
    suspend fun advanceTimeBy(duration: Duration)
    suspend fun resetScenario()
    fun sideEffectCount(request: HttpIdempotencyRequest): Int
    fun quiescence(): HttpIdempotencyQuiescence
}

suspend fun assertBoundedWaitHttpIdempotencyConformance(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
)
```

`holdOwnerResponseDelivery`는 request owner가 시작되기 전에 arm한다. terminal outcome은 정상적으로
commit되지만 response delivery는 release, owner cancellation 또는 scenario reset까지 보류된다.
cancellation/reset cleanup은 delivery hold를 정확히 한 번 회수하며 terminal record를 abandon하지 않는다.

여기서 `Duration`은 `java.time.Duration`이다. public signature에는 Spring, Ktor, repository,
lock, persistence, production clock, dispatcher 또는 executor type을 넣지 않는다.
`authenticationProfile`은 reference adapter가 인증된 test principal/tenant를 선택하는 opaque test
handle이며 HTTP에서 caller가 tenant를 직접 지정하게 하는 header가 아니다. cross-tenant scenario는
서로 다른 profile이 서로 다른 server-resolved tenant로 mapping됨을 adapter setup에서 고정한다. runner는 global scope나
hidden dispatcher를 만들지 않고 caller의 structured coroutine scope에서 실행한다. MockMvc adapter는
자신이 소유한 bounded executor에서 blocking exchange만 격리하며 runner contract를 바꾸지 않는다.

collection-bearing request/response/config의 public constructor는 collection parameter를 property로 직접
보관하지 않고 nested collection까지 canonical deep copy한 immutable property를 만든다. 명시적인
`copy`-equivalent member도 public constructor를 다시 호출하며 equality/hash와 serialization은
canonical content 기준이다.

config는 constructor와 copy-equivalent 생성 경로에서 다음 불변식을 검증한다.

- duration은 positive, non-zero이며 millisecond 또는 second 단위 변환이 overflow하지 않는다.
  `waitTimeout`은 60초 이하, `retention`은 365일 이하, test-only real-time watchdog인
  `scenarioTimeout`은 1..60초이다.
- `maxWaitersPerKey`는 `1..10_000`, `maxIdempotencyKeyBytes`는 `1..8_191`,
  `maxRequestBodyBytes`는 `1..16_777_215`, `maxReplayBodyBytes`는 `1..16_777_216`,
  `maxReplayHeaderNames`는 `0..100`, `maxReplayValuesPerHeader`는 `1..100`,
  `maxReplayHeaderValueBytes`는 `1..65_536`, `maxReplayHeaderBytes`는 `1..1_048_576`이다.
- 두 `Retry-After` 값은 positive whole seconds이고 86,400초 이하이다.
- header name은 nonblank ASCII HTTP token이고 case-insensitive하게 중복되지 않는다.
- collection은 canonical lower-case name으로 정렬·방어 복사하며 외부 mutation이 snapshot을 바꾸지 않는다.

request/response constructor와 deserialization은 config가 없어도 검증 가능한 intrinsic invariant를
검증한다. identity field는 nonblank이고 `authenticationProfile`은 512 UTF-8 bytes,
`operation`/`resourceIdentity`는 1,024 UTF-8 bytes 이하이다. `idempotencyKeys`는 normal request 한 개와
duplicate-header negative vector 두 개만 표현하도록 `1..2` values를 deep-copy한다. 복사는 input
order와 multiplicity를 그대로 보존하고 equality/hash/serialization도 ordered list 기준이다. adapter는
각 원소를 별도 반복 `Idempotency-Key` header value로 전송하며 merge하거나 deduplicate하지 않는다.
각 key/body/header는 config가 허용할 수 있는
위 absolute ceiling을 넘지 않는다. response status는 `100..599`, body는 valid UTF-8이고
`problemCode`는 nullable lower snake case다. response header key는 lower-case로 normalize하고 value
순서를 보존한 immutable map으로 canonicalize한다. request/response equality와 hash는
canonical string, header와 body content를 기준으로 하며 source collection mutation의 영향을 받지 않는다.

runner는 정상 scenario의 single key/request-body fixture가 configured bound 이내인지 확인한다. duplicate
negative vector만 두 key values를 사용한다. oversized
negative scenario는 configured limit보다 정확히 1 byte 크고 각 intrinsic 8,192/16,777,216-byte
ceiling 이내인 key/body를 `adapter.exchange`로 실제 전송한다. reference application/adapter가 같은
config를 HTTP ingress에 적용해 거절하므로 shared runner가 black-box 결과를 관찰할 수 있다. runner는 exchange 뒤 replay
snapshot을 저장·비교하기 전에 body, header name count, values per name, individual value와 aggregate
header bytes를 같은 config로 검증한다. Java deserialization의 `readResolve`는 intrinsic grammar와
absolute ceiling을 복구하고, 더 작은 instance-specific limit은 정상 fixture validation과 reference
application ingress가 적용한다.

위반은 cause나 입력값을 노출하지 않는 stable `IllegalArgumentException`으로 거절한다. Java
deserialization은 private `readResolve`에서 같은 public constructor/factory를 다시 통과시키고 실패를
redacted `InvalidObjectException`으로 바꾼다. serialization round-trip, crafted invalid state와 mutable
collection isolation을 직접 test한다.

request/response/config/quiescence처럼 value를 표현하는 public class는 모두 `Serializable`을 구현하고
`serialVersionUID`를 선언한다. secret-bearing request/response는 equality/hash semantics를 유지하되
`toString()`에서 raw tenant, principal, key, payload, body와 모든 header value를 redaction하고,
sensitive/denylisted header name도 출력하지 않는다. 함수, lifecycle과 mutable
control을 담는 adapter는 data class로 만들지 않는다. 모든 public declaration은 English KDoc,
realistic usage example과 직접 test를 갖는다.

구체적인 internal control은 test application의 구현을 노출하지 않는 최소 기능만 가진다.

- 첫 business execution이 시작될 때까지 기다리기
- blocked execution의 terminal 또는 transient outcome 결정
- waiter deadline과 retention clock 전진
- scope별 실제 side-effect execution count 관찰

runner는 raw key, raw payload 또는 response body를 log하지 않는다. assertion failure, exception,
serialization failure와 object/nested collection rendering은 scenario와 stable classification만 보여주고
sentinel secret를 출력하지 않는다. reference test는 raw key/body뿐 아니라 authorization와 cookie
header name/value sentinel가 이 네 경로에 나타나지 않음을 검증한다.

## Scope와 fingerprint

idempotency scope는 server가 인증·인가 결과에서 만든 다음 logical tuple이다.

```text
security scope + operation + resource identity + idempotency key digest
```

`security scope`는 기본적으로 server-resolved tenant다. 같은 tenant 안에서도 결과 가시성이 principal,
role 또는 authorization partition보다 좁으면 application은 그 context까지 scope에 포함한다. 인증과
해당 operation/resource에 대한 인가는 lookup, conflict 판정과 replay보다 먼저 끝나야 한다. caller가
보낸 tenant header나 body field를 trusted scope로 사용하지 않는다.

fingerprint는 HTTP method, canonical route identity, command 의미에 영향을 주는 canonical request
body와 precondition을 포함한다. header order, JSON field order나 transport encoding처럼 의미가
없는 차이는 application의 canonicalization 단계에서 제거한다. fixture는 특정 JSON library나 hash
algorithm을 강제하지 않고 동일/상이 fingerprint의 observable 결과만 검증한다.

raw idempotency key와 raw request payload는 persistence, operational log, metric label에 남기지
않는다. ingress는 canonicalization과 lookup 전에 buffered request body의 configured byte limit을
적용한다. 그 다음 idempotency header가 정확히 하나인지 확인하고, nonblank visible ASCII,
configured byte limit 이내인지 검증하며 control character, invalid octet와 duplicate header를 거절한다.
scope tuple과 fingerprint의 digest input은 domain separator와 각 component의 byte length를 포함해
concatenation ambiguity가 없어야 한다. digest는 algorithm을 API에 노출하지 않되 최소 128-bit collision
security를 제공하는 collision-resistant primitive를 사용한다. fixture는 oversized request,
duplicate/malformed key, ambiguous tuple과 canonical payload negative vector를 검증한다.

인증되지 않았거나 인가되지 않은 요청은 foreign record의 존재 여부와 무관하게 동일한 외부 결과를
받아야 한다. replay/conflict/waiter signal을 노출하거나 side effect를 실행해서는 안 된다.

## In-flight 정책

선택한 정책은 bounded wait와 waiter limit다.

1. 최초 request가 scope ownership을 얻고 business operation을 한 번 실행한다.
2. 같은 scope와 fingerprint의 request는 기존 execution의 waiter로 등록한다.
3. waiter deadline은 waiter registration이 성공한 instant에서 시작한다. owner가 deadline보다 엄격히
   먼저 terminal 상태가 되면 waiter도 같은 terminal response를 replay한다.
4. timeout되면 `409`와 stable code `idempotency_in_flight`, `Retry-After`를 반환한다.
5. `maxWaitersPerKey`를 초과하면 `429`와 `idempotency_waiters_exceeded`를 반환한다.
6. 같은 scope에 다른 fingerprint가 오면 기다리지 않고 `409 idempotency_key_reused`를 반환한다.

`waitTimeout`, `maxWaitersPerKey`와 retention은 adapter/application instance를 만들 때 제공하는
명시적 config다. client header나 process-wide hidden default로 바꾸지 않는다. 공통 fixture는
production 기본 시간값을 고정하지 않고 test가 작은 deterministic 값을 주입하게 한다.

`now >= deadline`이면 timeout이 이긴다. completion과 timeout은 하나의 atomic transition에서 한 결과만
선택하며 exact deadline에는 timeout을 반환한다. timeout, cancellation, owner terminal completion과
transient abandon은 waiter를 exactly once deregister하고 slot을 즉시 회수한다. different fingerprint는
waiter로 등록하지 않으며 `maxWaitersPerKey`는 owner를 제외한 같은 scope/fingerprint waiter만 센다.

`Retry-After`는 HTTP-date가 아닌 positive delta-seconds 하나만 사용한다. `409`는
`inFlightRetryAfter`, `429`는 `overflowRetryAfter`를 그대로 반환한다. 값은 각 상태의 완료 시간을
보장하지 않는 earliest retry hint이며 client retry budget이나 jitter를 대신하지 않는다.

waiter limit은 같은 key의 connection fan-in을 제한한다. tenant 또는 principal 전체 rate limit,
gateway quota와 abuse detection은 별도 application concern이다. waiter limit만으로 공격 방어가
완료된다고 문서화하지 않는다.

## Response와 error contract

| 상황 | HTTP 결과 | stable code/header | side effect |
| --- | --- | --- | --- |
| 최초 성공 | 원래 terminal status/body | `Idempotency-Replayed: false` | 1회 |
| 같은 request terminal replay | 저장한 status/body | `Idempotency-Replayed: true` | 추가 실행 없음 |
| 같은 request in-flight 완료 | owner와 같은 terminal result | `Idempotency-Replayed: true` | 추가 실행 없음 |
| 같은 request wait timeout | `409` | `idempotency_in_flight`, `Retry-After` | owner 상태 유지 |
| waiter limit 초과 | `429` | `idempotency_waiters_exceeded`, `Retry-After` | owner 상태 유지 |
| 다른 fingerprint | `409` | `idempotency_key_reused` | 추가 실행 없음 |
| deterministic terminal failure | application이 정한 terminal status/body | 첫 응답 false, replay true | 추가 실행 없음 |
| transient failure/rollback | application이 정한 transient response | terminal replay header 없음 | ownership abandon, retry 가능 |
| retention 만료 | 새 request로 실행 | `Idempotency-Replayed: false` | 새 side effect 가능 |

success response와 application이 명시적으로 terminal로 분류한 deterministic failure만 저장한다.
status code만 보고 모든 `4xx`를 terminal로 추론하지 않는다. state revision이나 policy version이
바뀌면 결과 의미도 달라지는 conflict는 application이 replay 가능 여부를 결정한다. dependency
timeout, deadlock, rollback과 같은 transient failure는 terminal result로 저장하지 않고 ownership을
abandon해 같은 fingerprint retry를 허용한다.

replay 대상은 status, configured UTF-8 body bound, content type과 application이 명시적으로 허용한
end-to-end header다. default allowlist는 empty다. `Set-Cookie`, `Cookie`, `Authorization`,
`Proxy-Authorization`, `WWW-Authenticate`, connection-specific hop-by-hop header와 credential-bearing
header는 대소문자와 무관하게 non-overridable denylist로 저장·재생하지 않는다. header value도 configured
byte bound를 적용한다. signed URL이나 credential query를 포함할 수 있는 `Location`은 application이
value safety를 증명한 경우에만 allowlist에 넣는다.

oversized body/header 또는 streaming response는 terminal replay snapshot으로 finalize할 수 없다.
application은 business commit 전에 compact replay descriptor나 replay 가능한 resource reference로
변환해야 한다. 그렇지 못한 operation은 이 bounded buffered profile을 사용하지 않고 status resource
contract를 선택한다.

## Transaction boundary와 ownership

durable application은 business mutation, audit/outbox와 terminal idempotency descriptor를 같은
database transaction에서 commit해야 한다. terminal retention은 이 atomic finalization commit
instant부터 시작한다. business mutation commit 뒤 별도 "finalize"를 수행하는 split transaction은
이 contract의 durable proof가 아니다. fixture는 transaction이나 repository를 제공하지 않으며
in-memory HTTP behavior만 증명한다.

production adopter는 별도 integration test로 business mutation과 terminal descriptor의 atomic commit,
process restart/crash 뒤 replay, rollback 뒤 ownership abandon, concurrent recovery와 external side-effect
경계를 증명해야 durable idempotency를 주장할 수 있다. runner 성공 메시지와 문서는 이 proof boundary를
명시한다.

외부 provider side effect는 database transaction에 자동으로 포함되지 않는다. response replay는
HTTP handler가 application record를 재사용한다는 뜻일 뿐 provider 호출, message delivery 또는
payment가 exactly once였다는 뜻이 아니다. provider request에도 별도 idempotency contract가
필요할 수 있다.

## Cancellation과 abandon

- waiter request cancellation은 그 waiter만 제거하고 owner execution을 취소하지 않는다.
- cancellation 후 waiter slot은 즉시 회수되어 waiter limit 누수가 없어야 한다.
- owner HTTP connection이 끊겨도 이미 같은 transaction에서 commit된 terminal descriptor는 replay된다.
- owner disconnect/cancellation이 commit보다 먼저 이기면 transaction을 rollback하고 ownership을
  atomically abandon한다. 등록된 waiter는 stable transient response를 받고 exactly once 제거되며,
  뒤따른 같은 fingerprint 요청 중 하나만 새 owner가 된다.
- cleanup이 suspend cancellation 이후 필요하면 cleanup만 `NonCancellable`에 두고 원래 cancellation을
  다시 던진다.
- suspend boundary는 `CancellationException`을 broad exception으로 변환하거나 terminal failure로
  저장하지 않는다.

## Retention과 late retry

terminal response는 atomic finalization commit instant부터 bounded retention 동안만 replay한다.
active owner에는 terminal retention을 적용하지 않는다. `now >= expiresAt`이면 expired이며, concurrent
retry 중 하나만 expired record를 교체하고 새 owner가 된다. fixture는 exact expiry 전/시/후와 동시
retry를 검증한다.

문서는 client retry budget이 retention을 넘지 않아야 하며, retention 이후 같은 key가 중복 side
effect를 막아 준다고 기대해서는 안 된다고 명시한다. 더 긴 duplicate protection이 필요한 domain은
business unique constraint, tombstone 또는 별도 domain identity를 application이 소유한다.

## 공통 scenario

`assertBoundedWaitHttpIdempotencyConformance`는 다음 scenario를 모두 실행한다. framework adapter는
일부를 skip하거나 expected result를 application별로 바꾸지 않는다.

1. 최초 request 성공과 `Idempotency-Replayed: false`
2. same-key/same-payload terminal replay
3. same-key/different-payload의 owner release/clock advance 전 즉시 conflict와 original replay 보존
4. 같은 key의 cross-tenant isolation
5. unauthenticated/unauthorized request의 record-present/absent indistinguishability와 zero side effect
6. in-flight duplicate의 bounded wait와 동일 terminal result
7. completion이 deadline 직전, exact deadline, 직후인 세 ordering과 atomic single response
8. wait timeout의 positive delta-seconds `Retry-After`, slot 회수와 replacement waiter admission
9. waiter limit 초과의 즉시 `429`, positive delta-seconds `Retry-After`와 zero registration
10. waiter cancellation 후 slot 회수와 owner completion
11. transient failure의 atomic abandon, 기존 waiter 종료와 concurrent retry 중 single new owner
12. owner disconnect의 pre-commit rollback/abandon과 post-commit replay
13. deterministic terminal failure replay
14. retention exact expiry 전/시/후와 concurrent retry 중 single new owner
15. oversized request body/key, duplicate/malformed key, ambiguous scope tuple/canonical payload의
    ownership 획득 전 거절과 zero waiter/side effect
16. replay allowlist/denylist의 mixed-case secret header, many names/repeated values, individual/aggregate
    header overflow와 oversized body 거절
17. 여러 key의 barrier-aligned repeated fan-in/overflow stress와 key 간 독립성

각 scenario는 status/body/header뿐 아니라 scope별 side-effect count를 검증한다. conflict와 overflow는
owner release나 clock advance 전에 응답하고 waiter count와 execution count를 바꾸지 않아야 한다.
stress scenario는 key마다 정확히 한 owner, 정확한 admitted/overflow 수, cross-key contention 부재,
slot 회수와 bounded termination을 여러 round에서 검증한다. suspend fan-in에는 기존
`SuspendedJobTester`를 우선 사용하고, 필요한 barrier semantics를 제공하지 못해 custom harness를 쓰면
그 이유를 test KDoc과 review evidence에 남긴다. raw `Boolean` 비교를
`shouldBeTrue()`로 감싸기보다 `bluetape4k-assertions`의 `shouldBeEqualTo`, `shouldHaveSize`,
`shouldContain`, `shouldNotBeEqualTo` 등 의미를 직접 드러내는 assertion을 우선한다. 적합한 helper가
없을 때만 raw assertion을 쓰고 이유를 review evidence에 남긴다.

## Framework reference test application

### Ktor

`ktor/testing` test source에 `testApplication` 기반 adapter를 둔다. request header와 body를 실제
Ktor client로 보내고 fake idempotency application의 response를 `HttpIdempotencyResponse`로
변환한다. application, client, coroutine scope와 gate lifecycle은 test가 소유한다.

### Spring Boot

`spring-boot/core` test source에 MockMvc 기반 adapter를 둔다. 같은 request와 response contract를
Spring controller/filter 경계로 실행한다. Spring context와 executor는 test가 소유하고 닫는다.

두 reference test application은 같은 fixture entrypoint를 그대로 호출하고 scenario 제외나
framework-specific expectation override를 사용하지 않는다. 이들은 production idempotency adapter가
아니며, published fixture가 Spring Boot와 Ktor에서 실행 가능하다는 executable proof다.

workshop consumer는 이번 PR에서 변경하지 않는다. published version을 사용할 수 있게 된 뒤 세
application이 동일 fixture를 직접 실행하는 후속 adoption을 검토한다.

## Determinism과 resource lifecycle

- `Thread.sleep`, 임의 polling interval이나 실제 wall-clock 경과로 timeout/retention을 증명하지 않는다.
- fake clock, controlled execution gate와 explicit completion signal을 사용한다.
- owner start, waiter registration과 completion 순서를 barrier로 고정한다.
- timeout/completion/expiry는 virtual clock으로 exact boundary 양쪽과 boundary instant를 실행한다.
- runner는 entry에서 single-thread monotonic watchdog scheduler를 만들고 모든 control wait와 scenario를
  behavioral virtual clock과 분리된 real-time `scenarioTimeout`으로 감싼다. 이 scheduler는 coroutine
  dispatcher나 global scope가 아니며 behavioral virtual clock을 전진시키지 않는다. timeout 시 redacted
  scenario diagnostic을 내고 child job을 취소하며, runner outer `finally`에서 scheduler를 shutdown하고
  termination을 검증한다.
- 각 scenario는 unique scope 또는 `resetScenario`를 사용한다. `finally`에서 reset/cleanup 후
  `quiescence()`의 active waiter, gate와 child task가 모두 0인지 검증한다.
- framework-owned application context와 executor는 scenario마다 닫지 않고 runner가 끝난 뒤 reference
  test의 outer `finally`에서 한 번 닫고 종료를 검증한다.
- test failure와 cancellation에서도 gate, waiter, coroutine과 executor를 정리한다.
- Spring/Ktor test를 서로 병렬 실행해야 한다는 가정을 두지 않는다.
- real database, Redis와 Testcontainers는 이번 fixture proof에 필요하지 않다.

## Caller contract와 지원 범위

| 결과 | caller action |
| --- | --- |
| ambiguous network failure, `idempotency_in_flight`, `idempotency_waiters_exceeded`, transient failure | 같은 logical command의 같은 key와 같은 payload를 retry budget 안에서 재사용하고 `Retry-After`와 jitter를 따른다. |
| terminal replay | 저장된 결과를 원래 결과로 받아들이고 새 side effect를 기대하지 않는다. |
| `idempotency_key_reused` | payload를 바꾸어 retry하지 않는다. caller bug 또는 key lifecycle 위반으로 종료한다. |
| retention 만료 가능성 | 같은 key가 duplicate protection을 보장하지 않음을 받아들이거나 domain identity/unique constraint로 보호한다. |
| 새 business intent | 이전 key를 재사용하지 않고 새 key를 발급한다. |

client는 logical command마다 하나의 key를 만들고 결과가 모호하거나 retriable한 동안 key/payload를 함께
유지한다. payload를 변경해야 한다면 기존 retry가 아니라 새 intent로 취급한다. `Retry-After`는 earliest
retry hint일 뿐 성공 보장이나 server-side reservation이 아니다.

| 입력/operation 형태 | bounded-wait profile |
| --- | --- |
| 짧은 buffered command, bounded UTF-8 request/response, deterministic canonicalization | 지원 |
| binary/large/streaming body, multipart/file upload, non-repeatable payload | 미지원; compact resource reference 또는 별도 contract 사용 |
| SSE, WebSocket, protocol upgrade, long-running operation | 미지원; `202` operation/status resource 사용 |
| external provider side effect | HTTP replay만 검증; provider 자체 idempotency 또는 reconciliation 필요 |

operation latency의 높은 percentile과 waiter timeout 합이 upstream HTTP deadline보다 충분히 작고,
예상 duplicate fan-in이 per-key 및 global connection budget 안에 있으며, client retry behavior를 통제할
수 있을 때 bounded wait를 선택한다. 이 조건을 만족하지 않으면 즉시 거절 profile이나 status resource가
더 안전하다. 정책 전환은 status/code/retry semantics가 바뀌는 public client contract 변경이므로 API
versioning과 migration 안내가 필요하다.

## 문서 설계

### Module README

`testing/junit5/README.md`와 `README.ko.md`에 다음 내용을 locale parity로 추가한다.

- fixture 사용법과 Spring/Ktor adapter의 작은 예제
- replay/conflict/in-flight/timeout/waiter overflow/expiry 결과표
- `waitTimeout`, `maxWaitersPerKey`, retention 설정 위치
- raw key/payload logging 금지와 replay-safe header allowlist
- logical command별 key 생성·재사용 규칙과 결과별 caller action
- 이 fixture가 in-memory HTTP behavior만 증명하며 durable production proof가 아니라는 경계

두 framework 예제는 compile-checked KDoc/test source로 제공한다. 각 예제는 config 생성, adapter와
test application/executor setup, runner 호출, structured scope와 cleanup ownership을 끝까지 보여준다.

### Manual

다음 bilingual manual chapter를 추가하고 module landing page에서 연결한다.

- `docs/manual/en/modules/bluetape4k-junit5/http-idempotency-conformance.md`
- `docs/manual/ko/modules/bluetape4k-junit5/http-idempotency-conformance.md`

manual은 즉시 거절, bounded wait, status lookup을 비교하고 다음 판단 기준을 설명한다.

- 짧은 synchronous command에는 bounded wait가 적합한 이유
- 장기 operation에는 status resource가 더 적합한 이유
- timeout, waiter limit, tenant rate limit과 retry storm의 관계
- transaction commit, external side effect와 exactly-once 비보장
- cancellation, retention과 late retry 위험
- fixture와 production adapter의 책임 경계
- production adoption 전 atomic commit, restart/crash recovery, rollback, external-side-effect integration
  test checklist
- latency/deadline, duplicate fan-in, connection budget과 retry horizon을 이용한 suitability gate
- bounded wait 철회 시 즉시 거절 또는 status resource로 이동하는 migration/versioning 주의점

root README에는 이 내용을 추가하지 않는다. module-scoped test fixture를 generic production HTTP
idempotency 기능으로 오해하지 않게 한다.

## 보안과 운영 경계

- key와 payload 크기는 HTTP ingress에서 제한한다.
- raw key, raw payload, tenant/principal identifier를 log나 metric label에 기록하지 않는다.
- application은 waiter 수, timeout, conflict, overflow, replay와 abandon을 raw identifier 없는 bounded
  classification으로 관측해야 한다. 이 HTTP conformance fixture는 production telemetry API나 metric
  cardinality를 검증하지 않는다.
- `Retry-After`를 제공하더라도 client retry budget과 jitter를 application API 문서에서 별도로 정한다.
- cross-tenant key existence를 유출하지 않도록 authentication/tenant resolution을 idempotency lookup보다
  먼저 수행한다.
- waiter limit은 per-key fan-in만 제한하며 global capacity protection을 대신하지 않는다.

request-size 검증은 application ingress에서 canonicalization/lookup보다 먼저 수행해야 한다. shared
runner의 black-box proof는 HTTP 거절, zero waiter와 zero side effect까지이며 internal canonicalization/
lookup count를 노출하는 test hook은 요구하지 않는다. adopter integration checklist가 실제 ingress
pipeline ordering을 별도로 검증한다.

| 관측 증가 | 가능한 원인 | owning capacity layer | 안전한 대응 |
| --- | --- | --- | --- |
| waiter | client retry burst, slow owner | application/gateway connection budget | latency 원인과 retry 정책을 먼저 확인한다. waiter limit만 올리지 않는다. |
| timeout | owner latency가 deadline 초과 | operation timeout, dependency budget | owner latency와 upstream deadline을 조정하거나 status resource로 전환한다. |
| overflow | hot key 또는 abuse | per-key limit + tenant/global limiter | offender를 격리하고 quota/rate limit을 적용한다. |
| conflict | key lifecycle 위반 | client SDK/API contract | payload mutation과 key reuse를 수정한다. |
| abandon | rollback, cancellation, transient dependency failure | transaction/dependency policy | rollback 원인을 해결하고 bounded retry를 유지한다. |
| replay 급증 | retry storm 또는 network instability | client/gateway retry budget | retry budget, jitter와 network error를 점검한다. |

## Failure modes

1. **waiter leak:** cancelled waiter가 slot을 반환하지 않으면 공격자가 한 key를 영구 고갈시킬 수 있다.
   cancellation scenario에서 slot 회수 후 새 waiter가 들어오는지 검증한다.
2. **timeout/completion race:** timeout과 owner completion이 동시에 발생하면 한 request가 서로 다른
   terminal response를 볼 수 있다. adapter는 하나의 atomic decision만 관찰하게 해야 한다.
3. **fingerprint overwrite:** 다른 payload가 original record를 덮으면 첫 request replay가 손상된다.
   conflict 뒤 original payload replay를 다시 검증한다.
4. **commit/finalize gap:** business mutation만 commit되고 idempotency descriptor가 남지 않으면 retry가
   side effect를 반복할 수 있다. durable application은 같은 transaction 경계를 문서화한다.
5. **late retry after expiry:** retention 이후 동일 key가 새 execution이 될 수 있다. README/manual에
   retry budget과 domain uniqueness 책임을 명시한다.
6. **unsafe response replay:** cookie나 secret-bearing header를 저장하면 다른 HTTP session에 credential이
   재생될 수 있다. allowlist 밖 header가 snapshot에 들어가지 않는 negative test를 둔다.
7. **framework exception drift:** Spring만 특정 scenario를 skip하거나 Ktor만 다른 status를 기대하면
   공통 contract가 유명무실해진다. 두 adapter가 동일 runner를 그대로 호출하게 한다.

## Compatibility와 migration

새 API는 `bluetape4k-junit5` test fixture이므로 existing production runtime 동작을 바꾸지 않는다.
신규 dependency, Gradle project registration, BOM/catalog 변경도 없다. 기존 observability fixture와
package를 분리해 symbol 충돌을 피한다.

public fixture는 `1.12.0`에서 처음 추가된다. 이후 production adapter를 제안하려면 최소 두
application이 같은 persistence/wake-up abstraction을 반복 사용한다는 별도 evidence가 필요하다.
이번 test contract를 이유로 application-owned repository를 성급히 공용 module로 이동하지 않는다.

runtime 또는 data migration은 `N/A`다. adoption은 test dependency 추가, application-owned adapter 작성,
fixture 실행, durable integration checklist 검증, client 문서 공개 순서의 opt-in이다. 철회는 fixture
호출을 제거하거나 이전 library version을 pin하면 되며 production data rollback은 없다. 공개 API는
binary/API compatibility validation과 `1.12.0` release note에 추가 사실과 proof boundary를 기록한다.

## Acceptance criteria mapping

| Issue criterion | 설계 proof |
| --- | --- |
| replay | scenario 1, 2, 6, 13에서 response와 side-effect count 검증 |
| payload conflict | scenario 3에서 즉시 `409`, original replay 유지 검증 |
| tenant/key scope | scenario 4, 5, 15에서 isolation, authorization-before-lookup과 key validation 검증 |
| in-flight behavior | scenario 6-12에서 bounded wait, exact deadline, waiter limit, cleanup과 cancellation 검증 |
| expired key | scenario 14에서 exact expiry와 concurrent single-owner election 검증 |
| failed terminal result | scenario 11, 13에서 transient abandon과 deterministic terminal replay 분리 검증 |
| two reference applications | Ktor `testApplication`과 Spring MockMvc가 같은 runner 실행 |
| durable transaction boundary | README/manual에 same-transaction 권고와 application ownership 명시 |
| persistence-neutral | shared API에 DB/cache/lock/repository type이 없음 |

## 검증 계획

구현 단계에서는 다음 순서로 fresh evidence를 수집한다.

1. `:bluetape4k-junit5:test`에서 shared runner unit/contract, serialization/redaction과 repeated stress test
2. `:bluetape4k-ktor-testing:test`에서 Ktor reference adapter
3. `:bluetape4k-spring-boot-core:test`에서 Spring reference adapter
4. 세 affected module의 compile/test를 포함한 broader targeted build
5. affected Kotlin detekt/static analysis
6. public API/binary compatibility와 compile-checked KDoc example 검증
7. README/manual locale parity와 source/test link 검증
8. `git diff --check`

실제 database나 Testcontainers 검증은 persistence implementation을 추가하지 않으므로 `N/A`다.

## Definition of Done

- 승인된 bounded-wait public surface와 공통 scenario가 `bluetape4k-junit5`에 구현된다.
- public data class가 `Serializable`, `serialVersionUID`, constructor/deserialization invariant,
  redacted rendering, English KDoc와 직접 test를 갖는다.
- Spring Boot와 Ktor reference test application이 같은 runner를 scenario 제외 없이 통과한다.
- bounded wait, exact timeout race, waiter overflow/slot recovery, cancellation, authorization isolation,
  retry, terminal failure와 exact expiry가 deterministic하게 검증된다.
- `bluetape4k-assertions`를 우선하고 의미를 잃는 Boolean assertion을 남기지 않는다.
- English/Korean README와 manual chapter가 정책 선택 기준과 ownership을 같은 의미로 설명한다.
- 신규 dependency, production store/filter/plugin과 workshop 변경이 없다.
- Type A spec/plan/code review의 최신 결과가 P0=0, P1=0이다.
- targeted test, static analysis와 `git diff --check`가 통과한다.
- issue-linked PR은 exact head, CI와 review convergence를 증명하고 별도 merge 승인을 기다린다.

## 승인 기록

- In-flight policy: bounded wait + waiter limit, timeout과 `Retry-After` 포함 — 승인.
- Fixture architecture: active scenario runner + framework-specific thin test adapter — 승인.
- Scope/fingerprint, error, terminal/transient, expiry/cancellation contract — 승인.
- Shared scenarios, Spring/Ktor proof, bilingual README/manual 범위 — 승인.
- Type A spec review: Performance, Stability, Security, Operator/Ops, Developer/API, User/caller
  최신 재검토 결과 P0=0, P1=0.
