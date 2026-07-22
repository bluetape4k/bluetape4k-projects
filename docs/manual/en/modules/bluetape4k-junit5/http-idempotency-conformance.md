---
title: Bounded-wait HTTP idempotency conformance
description: Select, test, and operate a bounded-wait HTTP idempotency policy without overstating durable guarantees.
manualId: bluetape4k-junit5
chapterId: http-idempotency-conformance
---

# Bounded-wait HTTP idempotency conformance

## Problem

A retry after a timeout may arrive while the original command is still running or after it committed without delivering
its response. A framework-neutral contract must distinguish first execution, bounded waiting, terminal replay, payload
conflict, capacity overflow, cancellation, and retention expiry without exposing another tenant's record.

`assertBoundedWaitHttpIdempotencyConformance` provides an opt-in, in-memory proof of those observable HTTP outcomes.
It does not install a production idempotency store.

## Policy selection

| Policy | Duplicate behavior | Best fit | Main cost |
| --- | --- | --- | --- |
| Immediate rejection | Return `idempotency_in_flight` immediately | Tight request budgets and clients that already back off | More ambiguous retries and polling pressure |
| Bounded wait | Admit at most `maxWaitersPerKey`, then return a terminal replay or a bounded error | Short commands whose duplicate fan-in fits the connection budget | Each waiter occupies bounded application capacity |
| `status-resource` | Return a durable operation identifier and let clients poll | SSE, WebSocket, long-running jobs, or work longer than the request deadline | Additional resource lifecycle and authorization design |

Choose bounded wait only when the wait improves client certainty without consuming the upstream deadline or global
connection budget.

## Suitability gate

Before adoption, answer all of these with measured or explicitly budgeted values:

| Gate | Acceptance rule |
| --- | --- |
| Latency | A representative high percentile plus `waitTimeout` remains below the smallest upstream/client deadline. |
| Duplicate fan-in | Observed or load-tested concurrent duplicates fit the per-key waiter budget. |
| Capacity | Per-key waiters fit tenant and global connection, coroutine/thread, and rate-limit budgets. |
| Retry horizon | `retention` covers the documented client retry horizon and clock-skew allowance. |

The shared proof accepts `maxWaitersPerKey <= 32` to keep its workload bounded. This is not a production recommendation.
Test any larger production limit with a representative adapter instance and a separate load test.

## Caller key lifecycle

| Situation | Caller action |
| --- | --- |
| Ambiguous/retriable response | Reuse the same key and canonical payload only within the documented retry horizon. |
| Terminal replay | Accept the replayed terminal response and stop retrying the command. |
| Changed-payload conflict | Treat `idempotency_key_reused` as a caller defect; do not mutate a payload behind one key. |
| Retention expiry | Expect the same key to become eligible for new ownership at the configured boundary. |
| New business intent | Generate a new key instead of recycling a previous command's key. |

Authenticate and authorize before idempotency lookup. Resolve tenant scope from authenticated server state, not a
caller-trusted tenant header. Never log raw keys, payloads, or tenant identifiers.

## Capacity and abuse

`maxWaitersPerKey` limits duplicate fan-in for one key only. It does not replace tenant/global connection limits, rate
limits, request-size limits, or admission control. Enforce those owning capacity layers before a request can consume a
waiter slot. A full waiter budget returns `429 idempotency_waiters_exceeded` with `Retry-After`; a timed-out waiter
returns `409 idempotency_in_flight` with `Retry-After` and releases its slot.

Persist only an explicit replay allowlist. `Authorization`, `Cookie`, credential-like headers, and hop-by-hop headers
are non-overridable denylist entries even when an adopter places them in the allowlist.

A blocking adapter must also budget executor threads for admitted requests. The shared three-key fan-in scenario needs
at least `3 * (maxWaitersPerKey + 1) + 1` threads so owners and waiters cannot occupy every thread before an overflow
probe runs. This is a test-harness lower bound, not a production sizing recommendation; size production pools from the
actual global and tenant fan-in, or use caller-owned virtual threads when the blocking boundary supports them.

## Signals and actions

| Signal increases | Owning capacity layer | Safe response |
| --- | --- | --- |
| Admitted waiters | Per-key concurrency and global connection budget | Compare fan-in with the configured budget; reduce upstream duplication before increasing capacity. |
| `idempotency_in_flight` timeouts | Command latency and caller deadline | Inspect latency percentiles and deadlines; reduce `waitTimeout` or move long work to a status resource. |
| `idempotency_waiters_exceeded` overflow | Tenant/global admission and rate limits | Throttle abusive callers, add jittered backoff, and keep `Retry-After` bounded. |
| `idempotency_key_reused` conflicts | Caller key lifecycle | Stop retries, alert on client defects, and verify canonical payload generation. |
| Owner abandon/disconnect before commit | Transaction owner and retry recovery | Confirm the slot and partial effect are reclaimed before allowing one retry owner. |
| Terminal replay rate | Client retry behavior and retention storage | Check timeout causes and retry horizon; do not treat replay volume as proof of duplicate side effects. |

## Transaction and crash proof

Fixture PASS proves observable, in-memory HTTP behavior. A production adapter still needs durable integration tests that
prove:

1. the business result and idempotency record commit atomically, or that a documented recovery protocol reconciles them;
2. restart/crash recovery elects at most one owner for an unresolved command;
3. expired records cannot race into multiple new owners;
4. replay snapshots remain bounded and omit `Authorization`, `Cookie`, and other prohibited headers;
5. an external provider's idempotency key and reconciliation protocol handle uncertain outcomes.

These checks do not establish external `exactly-once` execution. State precisely which store, transaction, and provider
boundary each integration test covers.

## Cancellation and retention

Waiter cancellation and timeout must reclaim the waiter slot. Owner disconnect before commit must abandon ownership so a
later retry can become owner; disconnect after commit must preserve the terminal replay. The adapter's reset hook must
cancel or join scenario-owned work and report zero active owners, waiters, and child tasks.

Retention is exact: before expiry, replay the terminal record; at or after expiry, one retry may become the new owner.
Set `retention` from the client retry horizon, audit requirements, storage capacity, and privacy policy rather than from
the fixture example.

## Supported inputs

| Input or operation | Support decision |
| --- | --- |
| Bounded UTF-8 command with a canonical representation | Supported by the shared proof. |
| Binary, large, multipart, or streaming body | Unsupported; use a domain-specific fingerprint and integration proof. |
| SSE, WebSocket, or long-running operation | Unsupported; use a `status-resource` policy. |
| External provider side effect | Observable HTTP behavior only; prove provider idempotency and reconciliation separately. |

The fixture also checks bounded key/body ingress, canonical JSON equivalence, malformed input rejection, replay body and
header limits, tenant isolation, and authorization-before-lookup behavior.

## Adoption and rollback

Adopt in this order:

1. add the JUnit 5 test dependency;
2. build an application adapter over the real framework test surface;
3. run `assertBoundedWaitHttpIdempotencyConformance` with instance-scoped limits;
4. add durable transaction, restart/crash, and external-side-effect integration checks;
5. document key lifecycle, `Retry-After`, retention, and conflict handling for clients.

Adoption is opt-in. Rollback by removing the fixture call or pinning the previous library version; there is no production
data rollback because the fixture does not own production data. If the public policy changes, version the API and publish
client migration guidance before changing server behavior.

## Ktor example

The compile-checked
[`KtorHttpIdempotencyConformanceTest`](../../../../../ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency/KtorHttpIdempotencyConformanceTest.kt)
uses the real Ktor `testApplication` request path, verifies declared and streaming ingress bounds, and maps cancellation
back to the exact exchange. The test owns `testApplication`; the fixture owns only its watchdog and adapter reset calls.

```kotlin
testApplication {
    val config = conformanceConfig()
    val fakeApplication = KtorFakeIdempotencyApplication(config)
    application { fakeApplication.installRoutes(this) }

    val adapter = KtorBoundedWaitHttpIdempotencyAdapter(client, fakeApplication, config)
    assertBoundedWaitHttpIdempotencyConformance(adapter, config)
}
```

## Spring example

The compile-checked
[`SpringHttpIdempotencyConformanceTest`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency/SpringHttpIdempotencyConformanceTest.kt)
uses MockMvc, bounds declared and unknown-length bodies before controller lookup, and wraps blocking execution with
`runInterruptible`. The caller creates and closes the bounded executor/dispatcher. For the documented
`maxWaitersPerKey = 8`, the shared fan-in formula requires at least 28 threads; the example rounds that up to 32.

```kotlin
Executors.newFixedThreadPool(32).asCoroutineDispatcher().use { dispatcher ->
    val adapter = SpringBoundedWaitHttpIdempotencyAdapter(mockMvc, application, dispatcher, config)
    assertBoundedWaitHttpIdempotencyConformance(adapter, config)
}
```

## Limitations

The fixture is not a store, middleware package, distributed lock, rate limiter, or transaction coordinator. It does not
prove database isolation, cross-node failover, restart recovery, network partition behavior, external `exactly-once`
effects, or performance at production limits. It tests a synthetic bounded UTF-8 command profile and requires adopters
to prove every omitted production boundary. `scenarioTimeout` is a cooperative watchdog: adapters must suspend
cooperatively, and blocking framework calls must use a caller-owned interruptible dispatcher. It cannot safely
force-stop code that ignores cancellation or thread interruption.

## Verification

The public runner executes 17 scenarios covering terminal outcomes, bounded wait, cancellation, retention, ingress and
replay bounds, and repeated fan-in. Start from the public source and framework references:

- [`assertBoundedWaitHttpIdempotencyConformance`](../../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyConformance.kt)
- [`BoundedWaitHttpIdempotencyConformanceConfig`](../../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyValues.kt)
- [`KtorHttpIdempotencyConformanceTest`](../../../../../ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency/KtorHttpIdempotencyConformanceTest.kt)
- [`SpringHttpIdempotencyConformanceTest`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency/SpringHttpIdempotencyConformanceTest.kt)

Run the module and framework reference tests before relying on the contract. A green result is the start of durable
integration proof, not a replacement for it.
