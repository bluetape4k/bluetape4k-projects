# Retrofit HC5 / Vert.x cancel() propagation and tag() storage fix

**Date**: 2026-05-16
**Issues**: #484, #489
**PR**: #512

## Root Cause

### cancel() — incomplete propagation

Both `Hc5CallFactory.AsyncClientCall` and `VertxCallFactory.VertxCall` called `promise?.cancel(true)`,
which cancels the `CompletableFuture` but **not** the underlying network request.
The HC5 `Future<SimpleHttpResponse>` and the Vert.x `HttpClientRequest` continued running,
blocking the thread until the 30-second `callTimeout` expired.

`isCanceled()` was derived from `promise?.isCancelled ?: false`, which is also wrong:
it returns `false` until `cancel()` has already executed, so pre-execute reads always return false.

### tag() — silent no-ops

All four `tag()` overloads in both factories had no backing store.
`tag(type, computeIfAbsent)` computed and discarded the value every time;
`tag(type)` always returned null.

### #489 — premature @Disabled

`ApacheHc5HttpbinCoroutineJacksonTest` and `ApacheHc5HttpbinCoroutineFastjsonTest` both had
an empty `@Disabled` override of `get post's comments()`. The underlying parent test passes
correctly with `AsyncApacheHttp5Client` — the disable was not needed and was suppressing coverage.

## Fix

### Hc5CallFactory

- Added `@Volatile var hc5Future: Future<SimpleHttpResponse>? = null`.
  Assigned immediately after `asyncClient.execute()` returns.
- `cancel()` now calls `hc5Future?.cancel(true)` in addition to `promise?.cancel(true)`.
- Added `@Volatile var cancelled = false`; `isCanceled()` returns `cancelled` directly.
- Post-assignment race guard in `executeAsync()`: if `cancelled` is already true when
  `hc5Future` is assigned, cancel it immediately.
- Replaced tag no-ops with `ConcurrentHashMap<Class<*>, Any>` keyed by `type.java`.

### VertxCallFactory

- Added `@Volatile var vertxRequest: HttpClientRequest? = null`.
  Assigned inside the `onSuccess` handler before `req.send()`.
- `cancel()` calls `vertxRequest?.reset()` (Vert.x 5.x `reset()` is idempotent).
- Same `@Volatile cancelled` pattern and post-assignment race guard as HC5.
- Same `ConcurrentHashMap` tag storage.

### #489

Removed the `@Disabled` override and its empty body from both HC5 coroutine test classes.

## Thread Safety Analysis

The volatile read/write pair on `cancelled` + `hc5Future`/`vertxRequest` is sufficient under JMM:
- Path A: `cancel()` runs first → sets `cancelled=true`, reads `hc5Future=null` (no-op) →
  `executeAsync()` assigns `hc5Future`, checks `cancelled=true`, cancels immediately.
- Path B: `executeAsync()` assigns first → `cancel()` reads non-null `hc5Future`, cancels it.

Both paths are covered; no additional lock is needed.

## Verification

Regression tests added to `Hc5HttpClientTest` and `VertxHttpClientTest`:

1. `cancel sets isCanceled to true immediately before execute` — isCanceled() authority contract.
2. `cancel during enqueue propagates to underlying request and fires onFailure promptly` —
   `SocketPolicy.NO_RESPONSE` + 5 s `CountDownLatch`; without fix hangs 30 s and fails.
3. `tag computeIfAbsent caches and returns same instance on repeated calls` — `shouldBeSameInstanceAs`.
4. `tag read returns null when tag has not been set`.

Results: 24 tests (Hc5HttpClientTest), 24 tests (VertxHttpClientTest) — 0 failures, 0 skipped.
Feign: 232 tests — 0 failures, 0 skipped (2 previously disabled tests now pass).

## Future Guidance

- `CompletableFuture.cancel(true)` only transitions the future's state — it does NOT propagate
  to any underlying blocking operation. Always capture and cancel the real async handle.
- `isCanceled()` must reflect user intent (`cancelled` flag), not future state.
- `tag()` read and `tag(type, computeIfAbsent)` overloads must share a single backing map;
  use `ConcurrentHashMap<Class<*>, Any>` keyed by `type.java` for type-safe, concurrent access.
- Vert.x `HttpClientRequest.reset()` is idempotent in 5.x. Document this when relying on it.
