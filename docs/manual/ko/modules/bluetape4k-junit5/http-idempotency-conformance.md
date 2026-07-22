---
title: Bounded-wait HTTP idempotency conformance
description: durable guarantee를 과장하지 않고 bounded-wait HTTP idempotency 정책을 선택하고 검증하고 운영합니다.
manualId: bluetape4k-junit5
chapterId: http-idempotency-conformance
---

# Bounded-wait HTTP idempotency conformance

## 문제

timeout 뒤의 retry는 원래 command가 아직 실행 중일 때 도착할 수도 있고, response를 전달하지 못한 채 commit한
뒤에 도착할 수도 있습니다. framework-neutral 계약은 다른 tenant의 record를 노출하지 않으면서 첫 실행,
bounded wait, terminal replay, payload conflict, capacity overflow, cancellation, retention expiry를 구분해야 합니다.

`assertBoundedWaitHttpIdempotencyConformance`는 이 observable HTTP 결과를 검증하는 opt-in in-memory proof입니다.
production idempotency store를 설치하지 않습니다.

## 정책 선택

| 정책 | duplicate 처리 | 적합한 경우 | 주요 비용 |
| --- | --- | --- | --- |
| Immediate rejection | 즉시 `idempotency_in_flight` 반환 | request budget이 작고 client가 이미 backoff하는 경우 | ambiguous retry와 polling 압력 증가 |
| Bounded wait | `maxWaitersPerKey`까지만 허용하고 terminal replay 또는 bounded error 반환 | 짧은 command이고 duplicate fan-in이 connection budget 안에 드는 경우 | waiter마다 bounded application capacity 점유 |
| `status-resource` | durable operation identifier를 반환하고 client가 poll | SSE, WebSocket, long-running job 또는 request deadline보다 긴 작업 | 별도 resource lifecycle과 authorization 설계 |

wait가 client의 불확실성을 줄이면서 upstream deadline과 global connection budget을 넘지 않을 때만 bounded
wait를 선택합니다.

## 적합성 gate

도입 전에 다음 항목을 측정값 또는 명시적인 budget으로 판단합니다.

| Gate | 수용 기준 |
| --- | --- |
| Latency | 대표 high percentile과 `waitTimeout`의 합이 가장 작은 upstream/client deadline보다 짧습니다. |
| Duplicate fan-in | 관측하거나 load test한 concurrent duplicate가 per-key waiter budget 안에 듭니다. |
| Capacity | per-key waiter가 tenant/global connection, coroutine/thread, rate-limit budget 안에 듭니다. |
| Retry horizon | `retention`이 문서화한 client retry horizon과 clock-skew 여유를 포함합니다. |

shared proof는 workload를 bounded로 유지하기 위해 `maxWaitersPerKey <= 32`만 받습니다. production 권고값이
아닙니다. 더 큰 production limit은 대표 adapter instance와 별도 load test로 검증합니다.

## 호출자 key lifecycle

| 상황 | caller 대응 |
| --- | --- |
| Ambiguous/retriable response | 문서화한 retry horizon 안에서만 같은 key와 canonical payload로 다시 요청합니다. |
| Terminal replay | replay된 terminal response를 받아들이고 command retry를 끝냅니다. |
| Changed-payload conflict | `idempotency_key_reused`를 caller defect로 처리하고 한 key 뒤의 payload를 바꾸지 않습니다. |
| Retention expiry | 설정한 경계부터 같은 key가 새 ownership을 얻을 수 있다고 봅니다. |
| New business intent | 이전 command의 key를 재활용하지 않고 새 key를 생성합니다. |

idempotency lookup 전에 authenticate하고 authorize합니다. tenant scope는 caller가 보낸 tenant header가 아니라
인증된 server state에서 결정합니다. raw key, payload, tenant identifier를 로그에 남기지 않습니다.

## 용량과 악용

`maxWaitersPerKey`는 한 key의 duplicate fan-in만 제한합니다. tenant/global connection limit, rate limit,
request-size limit, admission control을 대신하지 않습니다. request가 waiter slot을 사용하기 전에 해당 capacity
layer에서 제한합니다. waiter budget이 가득 차면 `Retry-After`가 있는 `429 idempotency_waiters_exceeded`를,
waiter가 timeout되면 slot을 반환하고 `Retry-After`가 있는 `409 idempotency_in_flight`를 반환합니다.

명시적인 replay allowlist만 저장합니다. `Authorization`, `Cookie`, credential 계열 header와 hop-by-hop header는
adopter가 allowlist에 넣어도 해제할 수 없는 denylist입니다.

## 신호와 대응

| 증가한 신호 | 소유 capacity layer | 안전한 대응 |
| --- | --- | --- |
| 허용된 waiter | per-key concurrency와 global connection budget | fan-in을 설정 budget과 비교하고 capacity를 늘리기 전에 upstream duplicate를 줄입니다. |
| `idempotency_in_flight` timeout | command latency와 caller deadline | latency percentile과 deadline을 조사하고 `waitTimeout`을 줄이거나 긴 작업을 status resource로 옮깁니다. |
| `idempotency_waiters_exceeded` overflow | tenant/global admission과 rate limit | 악용 caller를 throttle하고 jitter backoff를 적용하며 `Retry-After`를 bounded로 유지합니다. |
| `idempotency_key_reused` conflict | caller key lifecycle | retry를 멈추고 client defect를 알리며 canonical payload 생성을 검증합니다. |
| commit 전 owner abandon/disconnect | transaction owner와 retry recovery | 한 retry owner를 허용하기 전에 slot과 partial effect가 정리됐는지 확인합니다. |
| Terminal replay 비율 | client retry 동작과 retention storage | timeout 원인과 retry horizon을 확인하고 replay 양을 duplicate side effect의 증거로 보지 않습니다. |

## transaction과 crash 증명

fixture PASS는 observable in-memory HTTP behavior를 증명합니다. production adapter에는 다음을 증명하는 durable
integration test가 여전히 필요합니다.

1. business result와 idempotency record가 atomic commit되거나 문서화한 recovery protocol로 reconcile됩니다.
2. restart/crash recovery가 해결되지 않은 command에 최대 한 owner만 선출합니다.
3. 만료된 record가 여러 새 owner로 갈라지지 않습니다.
4. replay snapshot이 bounded이고 `Authorization`, `Cookie` 및 금지 header를 제외합니다.
5. external provider의 idempotency key와 reconciliation protocol이 uncertain outcome을 처리합니다.

이 검증은 external `exactly-once` execution을 보장하지 않습니다. 각 integration test가 다루는 store,
transaction, provider boundary를 정확히 기록합니다.

## cancellation과 retention

waiter cancellation과 timeout은 waiter slot을 반환해야 합니다. commit 전 owner disconnect는 ownership을
abandon해 나중 retry가 owner가 되게 해야 하고, commit 뒤 disconnect는 terminal replay를 보존해야 합니다.
adapter reset hook은 scenario-owned work를 cancel 또는 join하고 active owner, waiter, child task가 모두 0임을
보고해야 합니다.

retention 경계는 정확합니다. expiry 전에는 terminal record를 replay하고, expiry 시점부터는 한 retry가 새
owner가 될 수 있습니다. `retention`은 fixture 예제가 아니라 client retry horizon, audit requirement, storage
capacity, privacy policy에서 결정합니다.

## 지원 입력

| 입력 또는 operation | 지원 판단 |
| --- | --- |
| canonical representation을 가진 bounded UTF-8 command | shared proof가 지원합니다. |
| Binary, large, multipart, streaming body | 지원하지 않습니다. domain-specific fingerprint와 integration proof를 사용합니다. |
| SSE, WebSocket, long-running operation | 지원하지 않습니다. `status-resource` 정책을 사용합니다. |
| External provider side effect | observable HTTP behavior만 포함합니다. provider idempotency와 reconciliation은 별도로 증명합니다. |

fixture는 bounded key/body ingress, canonical JSON equivalence, malformed input 거부, replay body/header limit,
tenant isolation, authorization-before-lookup 동작도 검증합니다.

## 도입과 철회

다음 순서로 도입합니다.

1. JUnit 5 test dependency를 추가합니다.
2. 실제 framework test surface 위에 application adapter를 만듭니다.
3. instance-scoped limit으로 `assertBoundedWaitHttpIdempotencyConformance`를 실행합니다.
4. durable transaction, restart/crash, external-side-effect integration check를 추가합니다.
5. client에게 key lifecycle, `Retry-After`, retention, conflict 처리를 문서화합니다.

도입은 opt-in입니다. fixture 호출을 제거하거나 이전 library version을 pin해 rollback합니다. fixture가
production data를 소유하지 않으므로 production data rollback은 없습니다. public 정책을 바꾼다면 server
동작을 바꾸기 전에 API를 versioning하고 client migration 안내를 배포합니다.

## Ktor 예제

compile-checked
[`KtorHttpIdempotencyConformanceTest`](../../../../../ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency/KtorHttpIdempotencyConformanceTest.kt)는
실제 Ktor `testApplication` request path를 사용하고 declared/streaming ingress bound를 검증하며 cancellation을
정확한 exchange로 전달합니다. test가 `testApplication`을 소유하고 fixture는 자체 watchdog과 adapter reset
호출만 소유합니다.

```kotlin
testApplication {
    val config = conformanceConfig()
    val fakeApplication = KtorFakeIdempotencyApplication(config)
    application { fakeApplication.installRoutes(this) }

    val adapter = KtorBoundedWaitHttpIdempotencyAdapter(client, fakeApplication, config)
    assertBoundedWaitHttpIdempotencyConformance(adapter, config)
}
```

## Spring 예제

compile-checked
[`SpringHttpIdempotencyConformanceTest`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency/SpringHttpIdempotencyConformanceTest.kt)는
MockMvc를 사용하고 controller lookup 전에 declared/unknown-length body를 제한하며 blocking execution을
`runInterruptible`로 감쌉니다. caller가 bounded executor/dispatcher를 만들고 닫습니다.

```kotlin
Executors.newFixedThreadPool(8).asCoroutineDispatcher().use { dispatcher ->
    val adapter = SpringBoundedWaitHttpIdempotencyAdapter(mockMvc, application, dispatcher, config)
    assertBoundedWaitHttpIdempotencyConformance(adapter, config)
}
```

## 제한

fixture는 store, middleware package, distributed lock, rate limiter, transaction coordinator가 아닙니다.
database isolation, cross-node failover, restart recovery, network partition 동작, external `exactly-once` effect,
production limit의 성능을 증명하지 않습니다. synthetic bounded UTF-8 command profile을 검증하며 빠진 모든
production boundary는 adopter가 증명해야 합니다.

## 검증

public runner는 terminal outcome, bounded wait, cancellation, retention, ingress/replay bound, repeated fan-in을
포함한 17개 scenario를 실행합니다. public source와 framework reference에서 시작합니다.

- [`assertBoundedWaitHttpIdempotencyConformance`](../../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyConformance.kt)
- [`BoundedWaitHttpIdempotencyConformanceConfig`](../../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyValues.kt)
- [`KtorHttpIdempotencyConformanceTest`](../../../../../ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency/KtorHttpIdempotencyConformanceTest.kt)
- [`SpringHttpIdempotencyConformanceTest`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency/SpringHttpIdempotencyConformanceTest.kt)

계약을 사용하기 전에 module test와 framework reference test를 실행합니다. green 결과는 durable integration
proof의 시작점이지 대체물이 아닙니다.
