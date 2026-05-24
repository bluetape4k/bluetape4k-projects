# HC5 Production HTTP Client Tuning Defaults

**Date**: 2026-05-24
**Issue**: #582
**Branch**: feat/http-client-tuning-defaults-20260524

## Problem

`bluetape4k-http` exposed only low-level DSL builders for Apache HttpComponents 5 (HC5).
Callers had to wire connection pool sizing, eviction, keep-alive fallback, retry, and
request timeouts themselves — getting any one wrong led to connection leaks or silent hangs
in production.

## Solution

Added named-parameter factory functions with sensible defaults that apply all required
tuning in one call, while keeping every parameter individually overridable:

| Function | Layer |
|----------|-------|
| `productionRequestConfigOf()` | `hc5.http` — timeouts: 5 s pool-wait / 10 s connect / 30 s response |
| `defaultKeepAliveStrategy()` | `hc5.http` — 60 s fallback when server omits `Keep-Alive` header |
| `defaultRetryStrategy()` | `hc5.http` — 3 retries, 1 s interval |
| `productionHttpClientOf()` | `hc5.classic` — eviction + keep-alive + retry + timeouts |
| `productionVirtualThreadHttpClientOf()` | `hc5.classic` — delegates to productionHttpClientOf |
| `productionHttpAsyncClientOf()` | `hc5.async` — async equivalent |

## Key HC5 API Facts (verified via javap)

- `evictExpiredConnections()` and `evictIdleConnections(TimeValue)` are on
  **`HttpClientBuilder`** and **`HttpAsyncClientBuilder`**, NOT on
  `PoolingHttpClientConnectionManagerBuilder`.
- `DefaultConnectionKeepAliveStrategy.getKeepAliveDuration()` returns a **negative** `TimeValue`
  when the server omits the `Keep-Alive` header — check `duration.duration < 0` for fallback.
- `PoolingHttpClientConnectionManagerBuilder` in HC5 5.6.1 has **no `setThreadFactory` API**.
  "VirtualThread" in `productionVirtualThreadHttpClientOf` refers to the calling context only.

## Code Review Findings (Step 6-R)

| Severity | Finding | Resolution |
|----------|---------|------------|
| CRITICAL | `productionHttpAsyncClientOf` had zero tests | Added `ProductionHttpAsyncClientTest` (4 tests, all pass) |
| HIGH | `productionVirtualThreadHttpClientOf` doesn't wire VT internally | KDoc updated to document HC5 5.x limitation |

## Test Results

| Test class | Tests | Pass |
|-----------|-------|------|
| `ProductionRequestConfigTest` | 6 | 6 ✅ |
| `ProductionHttpClientTest` | 7 | 7 ✅ |
| `ProductionHttpAsyncClientTest` | 4 | 4 ✅ |
| **Total** | **17** | **17** |

## Lessons

1. **Always verify HC5 API placement with javap before implementing** — eviction is on the
   *client builder*, not the connection manager builder. Grep of existing code would not
   have caught this.
2. **Every new public function needs a test** — the code reviewer caught the missing async
   test. Add tests before submitting for review.
3. **"VirtualThread" naming needs careful documentation** — HC5 doesn't expose thread factory
   APIs; name it to avoid misleading callers.
