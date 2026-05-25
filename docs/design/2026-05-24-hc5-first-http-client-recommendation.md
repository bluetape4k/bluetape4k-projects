# Design Note: HC5-First HTTP Client Recommendation

**Date**: 2026-05-24
**Issue**: #586
**Status**: Accepted

---

## Context

`bluetape4k-http` wraps five HTTP client backends: Apache HttpComponents 5 (HC5), OkHttp3, JDK HttpClient, Vert.x HttpClient, and Ktor CIO. As of milestone 1.9.2, HC5 has received the deepest feature investment (production-tuned factories, caching DSL, cache metrics helpers, coroutine integration, virtual-thread support). This note evaluates whether HC5 should be promoted as the **primary recommended production HTTP client** and clarifies the role of the remaining backends.

---

## Decision

**Promote HC5 as the primary recommended production HTTP client in `bluetape4k-http`.**

The remaining backends remain fully supported as first-class options for their respective ecosystems, but the default recommendations in documentation will point to HC5.

---

## Recommendation by Use Case

| Scenario | Primary | Notes |
|----------|---------|-------|
| Sync backend calls (high-throughput) | **HC5 Classic + VirtualThread** | 7,247 ops/s (base); production-tuned defaults via `productionVirtualThreadHttpClientOf()` |
| Async backend calls (coroutine-first) | **HC5 Async + Coroutines** | `productionHttpAsyncClientOf()` + `executeSuspending()` |
| Repeated cacheable GETs (max throughput) | **HC5 CachingHttpClient (in-memory)** | 813,906 ops/s vs 682 ops/s no-cache (×1,233); `memoryCachingHttpClientOf()` |
| Cache persistence across restarts | OkHttp3 + DiskLruCache | `okhttp3ClientWithCache()`; 35,359 ops/s (×53 vs baseline) |
| General-purpose (no strong preference) | **OkHttp3 or HC5 Classic VT** | OkHttp3 easier for Android-compatible code |
| Ktor-based applications | Ktor CIO | Suspend-native, Ktor plugin ecosystem |
| Vert.x-based applications | Vert.x WebClient | Event-loop, reactive |
| Zero-dependency JVM services | JDK HttpClient | Standard library; feature set is more limited |

---

## HC5 as Primary — Rationale

### Performance evidence (2026-05-21 benchmark snapshot)

- **Base throughput** (`GET /ping`, 8 threads): HC5 Classic VirtualThread **7,247 ops/s** — top of the field; OkHttp3 VirtualThread 6,956 ops/s, JDK Sync 7,276 ops/s.
- **High-latency throughput** (`GET /delay/0.05`, 100 threads, theoretical ceiling 2,000 ops/s): all clients near-symmetric at ~1,860–1,902 ops/s. No single winner — confirmed that under real latency, thread model matters more than client library.
- **Cache throughput** (10 ms delay, `Cache-Control: public, max-age=3600`): HC5 in-memory cache **813,906 ops/s** — 23× faster than OkHttp3 disk cache. OkHttp disk is bottlenecked by `synchronized` DiskLruCache journal writes, not actual disk I/O.

### Feature completeness

HC5 provides the most complete set of production-ready features in `bluetape4k-http`:

| Feature | HC5 | OkHttp3 | JDK | Ktor | Vert.x |
|---------|-----|---------|-----|------|--------|
| Production-tuned factory | ✅ | ❌ | ❌ | ❌ | ❌ |
| Virtual Thread support | ✅ | ✅ | ✅ | ❌ | ❌ |
| Coroutine integration | ✅ | ✅ | ✅ | ✅ | ✅ |
| In-memory HTTP caching (RFC 7234) | ✅ | ❌ | ❌ | ❌ | ❌ |
| Disk caching | ✅ (via dir) | ✅ | ❌ | ❌ | ❌ |
| Cache config DSL | ✅ | ✅ (`okhttp3ClientWithCache`) | ❌ | ❌ | ❌ |
| HTTP/2 | ✅ | ✅ | ✅ | ❌ | ✅ |
| Connection eviction / keep-alive control | ✅ | ✅ | ❌ | ❌ | ❌ |
| Retry strategy | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## Rejected Alternatives as Primary

### OkHttp3

**Rejected as primary** for general backend use. Rationale:
- Excellent library but designed with Android as the primary target; disk-first caching model is suboptimal for JVM in-process caching.
- No built-in retry strategy.
- Cache hit throughput is 23× lower than HC5 in-memory for the same payload (OkHttp3: 35,359 ops/s vs HC5: 813,906 ops/s).
- **Retained as primary for**: use cases requiring cache persistence across restarts, Android-compatible code, and projects that already use OkHttp.

### JDK HttpClient

**Rejected as primary** for feature-rich production use. Rationale:
- No retry, no keep-alive fallback, no built-in cache.
- `CompletableFuture`-based async model is ergonomically inferior to HC5 + coroutines.
- **Retained as**: zero-dependency option for lightweight services where JDK is sufficient.

### Ktor CIO

**Rejected as primary** for general use. Rationale:
- Does not support HTTP/2.
- Base throughput 2,052 ops/s vs 7,247 ops/s for HC5 Classic VT on the same `/ping` benchmark — a 3.5× gap.
- **Retained as primary for**: Ktor-based application code and coroutine-first service libraries that already depend on Ktor.

### Vert.x WebClient

**Rejected as primary** for general use. Rationale:
- Event-loop model requires Vert.x Vertx instance; not composable outside Vert.x context.
- **Retained as primary for**: Vert.x ecosystem applications.

---

## Migration Risk

**No code is removed.** All existing backends remain available.

The only change is documentation: README and module docs now state which clients are **primary recommendations** and which are **ecosystem-specific or compatibility options**.

Existing code using OkHttp3, JDK, Ktor, or Vert.x backends is unaffected. No deprecation is introduced at this time.

Future deprecation of lower-priority backends would require:
1. A separate implementation issue with a concrete migration plan.
2. A measured reason (usage data, maintenance burden, duplication).
3. A multi-release deprecation cycle.

---

## Benchmark Evidence Gaps

The 2026-05-21 snapshot was collected on a local Colima Docker environment. The following gaps should be addressed before using the numbers for capacity planning:

- No CI-gated benchmark regression threshold exists yet (tracked in #585).
- High-latency benchmark measures throughput only; P95/P99 latency is not yet captured (tracked in #585).
- Benchmark uses a single-host setup; network variance on real infrastructure may differ.

---

## Follow-up Issues

| Issue | Description |
|-------|-------------|
| #584 | Evaluate `HttpClientLatencyBenchmark` fixture: `BluetapeWebfluxServer` vs `BluetapeHttpServer` |
| #585 | Add CPU and GC profiling to HTTP client benchmarks |
| #589 | Epic: benchmark-driven HTTP component performance improvement |
