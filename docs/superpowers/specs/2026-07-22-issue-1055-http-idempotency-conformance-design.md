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
- workshop [#532](https://github.com/bluetape4k/bluetape4k-workshop/issues/532),
  [#533](https://github.com/bluetape4k/bluetape4k-workshop/issues/533),
  [#534](https://github.com/bluetape4k/bluetape4k-workshop/issues/534)는 모두 닫혔으며,
  PostgreSQL을 terminal outcome의 권위로 두고 application-owned idempotency record를 사용한다.
- 세 workshop consumer는 저장소 구현을 공유하지 않지만 same-payload replay,
  different-payload conflict, bounded retention, retry/cancellation 경계가 필요하다는 점은 같다.
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

짧은 command가 대부분인 세 consumer에서 client 동작이 가장 단순해지고, timeout과 capacity를
명시하면 무제한 connection 점유도 막을 수 있다.

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
| request snapshot | `HttpIdempotencyRequest` | tenant, operation, resource identity, key와 canonical payload를 test request로 표현 |
| response snapshot | `HttpIdempotencyResponse` | status, body, replay-safe header와 stable problem code를 표현 |
| expectation/config | `HttpIdempotencyConformanceConfig` | `waitTimeout`, `maxWaitersPerKey`, `retention`, replay header allowlist를 명시 |
| framework boundary | `HttpIdempotencyConformanceAdapter` | request exchange와 deterministic test control을 제공 |
| runner | `assertHttpIdempotencyConformance` | 승인된 scenario 전체를 같은 순서와 assertion으로 실행 |

request/response/config처럼 value를 표현하는 public data class는 `Serializable`을 구현하고
`serialVersionUID`를 선언한다. 함수, lifecycle과 mutable control을 담는 adapter는 data class로
만들지 않는다. 모든 public declaration은 English KDoc, realistic usage example과 직접 test를
갖는다.

구체적인 internal control은 test application의 구현을 노출하지 않는 최소 기능만 가진다.

- 첫 business execution이 시작될 때까지 기다리기
- blocked execution의 terminal 또는 transient outcome 결정
- waiter deadline과 retention clock 전진
- scope별 실제 side-effect execution count 관찰

runner는 raw key, raw payload 또는 response body를 log하지 않는다. assertion failure는 scenario와
stable classification을 보여주되 secret-bearing 비교값을 출력하지 않는다.

## Scope와 fingerprint

idempotency scope는 다음 logical tuple이다.

```text
tenant + operation + resource identity + idempotency key digest
```

fingerprint는 HTTP method, canonical route identity, command 의미에 영향을 주는 canonical request
body와 precondition을 포함한다. header order, JSON field order나 transport encoding처럼 의미가
없는 차이는 application의 canonicalization 단계에서 제거한다. fixture는 특정 JSON library나 hash
algorithm을 강제하지 않고 동일/상이 fingerprint의 observable 결과만 검증한다.

raw idempotency key와 raw request payload는 persistence, operational log, metric label에 남기지
않는다. 인증, tenant resolution, key 길이 제한과 기본 request validation을 통과한 뒤 idempotency
lookup을 수행한다. 인증되지 않은 요청이 idempotency record 존재 여부를 탐색할 수 없어야 한다.

## In-flight 정책

선택한 정책은 bounded wait와 waiter limit다.

1. 최초 request가 scope ownership을 얻고 business operation을 한 번 실행한다.
2. 같은 scope와 fingerprint의 request는 기존 execution의 waiter로 등록한다.
3. owner가 `waitTimeout` 안에 끝나면 waiter도 같은 terminal response를 replay한다.
4. timeout되면 `409`와 stable code `idempotency_in_flight`, `Retry-After`를 반환한다.
5. `maxWaitersPerKey`를 초과하면 `429`와 `idempotency_waiters_exceeded`를 반환한다.
6. 같은 scope에 다른 fingerprint가 오면 기다리지 않고 `409 idempotency_key_reused`를 반환한다.

`waitTimeout`, `maxWaitersPerKey`와 retention은 adapter/application instance를 만들 때 제공하는
명시적 config다. client header나 process-wide hidden default로 바꾸지 않는다. 공통 fixture는
production 기본 시간값을 고정하지 않고 test가 작은 deterministic 값을 주입하게 한다.

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

replay 대상은 status, bounded body, content type과 application이 허용한 `Location`, `ETag` 같은
end-to-end header다. `Set-Cookie`, connection-specific hop-by-hop header와 secret-bearing header는
저장·재생하지 않는다.

## Transaction boundary와 ownership

durable application은 business mutation, audit/outbox와 terminal idempotency descriptor를 가능한
한 같은 database transaction에서 commit한다. 그래야 business mutation은 commit됐지만 replay
result가 없는 crash window를 피할 수 있다. fixture는 이 transaction을 만들거나 repository를
제공하지 않는다. application이 자신의 persistence model로 이 경계를 증명해야 한다.

외부 provider side effect는 database transaction에 자동으로 포함되지 않는다. response replay는
HTTP handler가 application record를 재사용한다는 뜻일 뿐 provider 호출, message delivery 또는
payment가 exactly once였다는 뜻이 아니다. provider request에도 별도 idempotency contract가
필요할 수 있다.

## Cancellation과 abandon

- waiter request cancellation은 그 waiter만 제거하고 owner execution을 취소하지 않는다.
- cancellation 후 waiter slot은 즉시 회수되어 waiter limit 누수가 없어야 한다.
- owner HTTP connection이 끊겨도 이미 commit된 transaction은 terminal descriptor로 finalize되어야 한다.
- commit 전에 execution이 취소·rollback되면 ownership을 abandon하고 같은 fingerprint retry를 허용한다.
- cleanup이 suspend cancellation 이후 필요하면 cleanup만 `NonCancellable`에 두고 원래 cancellation을
  다시 던진다.
- suspend boundary는 `CancellationException`을 broad exception으로 변환하거나 terminal failure로
  저장하지 않는다.

## Retention과 late retry

terminal response는 bounded retention 동안만 replay한다. retention이 끝난 key는 application이
기억하지 못하므로 새 request처럼 실행될 수 있다. fixture는 fake clock을 retention 이후로 전진해
새 execution과 `Idempotency-Replayed: false`를 검증한다.

문서는 client retry budget이 retention을 넘지 않아야 하며, retention 이후 같은 key가 중복 side
effect를 막아 준다고 기대해서는 안 된다고 명시한다. 더 긴 duplicate protection이 필요한 domain은
business unique constraint, tombstone 또는 별도 domain identity를 application이 소유한다.

## 공통 scenario

`assertHttpIdempotencyConformance`는 다음 scenario를 모두 실행한다. framework adapter는 일부를
skip하거나 expected result를 application별로 바꾸지 않는다.

1. 최초 request 성공과 `Idempotency-Replayed: false`
2. same-key/same-payload terminal replay
3. same-key/different-payload conflict
4. in-flight duplicate의 bounded wait와 동일 terminal result
5. wait timeout과 parse 가능한 `Retry-After`
6. waiter limit 초과와 owner execution 유지
7. waiter cancellation 후 slot 회수와 owner completion
8. transient failure 후 ownership abandon과 same-key retry
9. deterministic terminal failure replay
10. retention 만료 후 새로운 execution

각 scenario는 status/body/header뿐 아니라 scope별 side-effect count를 검증한다. raw `Boolean` 비교를
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
- test failure와 cancellation에서도 gate, waiter, coroutine과 executor를 정리한다.
- Spring/Ktor test를 서로 병렬 실행해야 한다는 가정을 두지 않는다.
- real database, Redis와 Testcontainers는 이번 fixture proof에 필요하지 않다.

## 문서 설계

### Module README

`testing/junit5/README.md`와 `README.ko.md`에 다음 내용을 locale parity로 추가한다.

- fixture 사용법과 Spring/Ktor adapter의 작은 예제
- replay/conflict/in-flight/timeout/waiter overflow/expiry 결과표
- `waitTimeout`, `maxWaitersPerKey`, retention 설정 위치
- raw key/payload logging 금지와 replay-safe header allowlist

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

root README에는 이 내용을 추가하지 않는다. module-scoped test fixture를 generic production HTTP
idempotency 기능으로 오해하지 않게 한다.

## 보안과 운영 경계

- key와 payload 크기는 HTTP ingress에서 제한한다.
- raw key, raw payload, tenant/principal identifier를 log나 metric label에 기록하지 않는다.
- waiter 수, timeout, conflict, overflow, replay와 abandon 결과는 bounded classification으로 관측한다.
- `Retry-After`를 제공하더라도 client retry budget과 jitter를 application API 문서에서 별도로 정한다.
- cross-tenant key existence를 유출하지 않도록 authentication/tenant resolution을 idempotency lookup보다
  먼저 수행한다.
- waiter limit은 per-key fan-in만 제한하며 global capacity protection을 대신하지 않는다.

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

## Acceptance criteria mapping

| Issue criterion | 설계 proof |
| --- | --- |
| replay | scenario 1, 2, 4, 9에서 response와 side-effect count 검증 |
| payload conflict | scenario 3에서 즉시 `409`, original replay 유지 검증 |
| in-flight behavior | bounded wait, timeout, waiter limit과 cancellation scenario |
| expired key | fake retention clock 이후 새 execution 검증 |
| failed terminal result | deterministic terminal failure replay와 transient abandon을 분리 검증 |
| two reference applications | Ktor `testApplication`과 Spring MockMvc가 같은 runner 실행 |
| durable transaction boundary | README/manual에 same-transaction 권고와 application ownership 명시 |
| persistence-neutral | shared API에 DB/cache/lock/repository type이 없음 |

## 검증 계획

구현 단계에서는 다음 순서로 fresh evidence를 수집한다.

1. `:bluetape4k-junit5:test`에서 shared runner unit/contract test
2. `:bluetape4k-ktor-testing:test`에서 Ktor reference adapter
3. `:bluetape4k-spring-boot-core:test`에서 Spring reference adapter
4. 세 affected module의 compile/test를 포함한 broader targeted build
5. affected Kotlin detekt/static analysis
6. README/manual locale parity와 source/test link 검증
7. `git diff --check`

실제 database나 Testcontainers 검증은 persistence implementation을 추가하지 않으므로 `N/A`다.

## Definition of Done

- 승인된 public surface와 공통 scenario가 `bluetape4k-junit5`에 구현된다.
- public data class가 `Serializable`과 `serialVersionUID`, English KDoc, 직접 test를 갖는다.
- Spring Boot와 Ktor reference test application이 같은 runner를 scenario 제외 없이 통과한다.
- bounded wait, timeout, waiter overflow, cancellation, retry, terminal failure와 expiry가 deterministic하게 검증된다.
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
