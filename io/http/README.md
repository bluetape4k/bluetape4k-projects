# Module bluetape4k-http

English | [한국어](./README.ko.md)

## Overview

`bluetape4k-http` integrates multiple HTTP client libraries through Kotlin extension functions and DSLs.

It provides a consistent interface for Apache HttpComponents 5, OkHttp3, Vert.x HttpClient, and Ktor Client, with built-in support for Kotlin Coroutines and Virtual Threads.

## Architecture

### Overall Architecture: Multi-Backend HTTP Client

![Overall Architecture: Multi-Backend HTTP Client diagram](../../docs/images/readme-diagrams/io-http-diagram-01.png)

### HTTP Client Hierarchy (HC5)

![HTTP Client Hierarchy (HC5) diagram](../../docs/images/readme-diagrams/io-http-diagram-02.png)

### OkHttp3 Client Hierarchy

![OkHttp3 Client Hierarchy diagram](../../docs/images/readme-diagrams/io-http-diagram-03.png)

### Async HTTP Request Flow (HC5 Async + Coroutines)

![Async HTTP Request Flow (HC5 Async + Coroutines) diagram](../../docs/images/readme-diagrams/io-http-sequence-01.png)

## Key Features

### 1. Apache HttpComponents 5 (HC5)

Wraps Apache HttpClient 5 with Kotlin DSL and Coroutines for both synchronous and asynchronous HTTP communication.

**Supported features:**

- Classic HttpClient (synchronous)
- Async HttpClient (asynchronous, Coroutines integration)
- HTTP/2 support (httpcore5-h2)
- Caching HttpClient (In-Memory, JCache)
- Connection pool management
- SSL/TLS configuration
- Fluent API

```kotlin
import io.bluetape4k.http.hc5.async.*

// Create an async HttpClient
val client = httpAsyncClient {
    setConnectionManager(cm)
    setMaxConnTotal(100)
    setMaxConnPerRoute(10)
}

// Async request in a Coroutines context
val request = SimpleHttpRequest.get("https://httpbin.org/get")
val response: SimpleHttpResponse = client.executeSuspending(request)
```

**Classic HttpClient:**

```kotlin
import io.bluetape4k.http.hc5.classic.*

// Create a classic HttpClient
val client = httpClient {
    setConnectionManager(poolingConnectionManager())
}

// Synchronous request
val response = client.execute(classicRequestOf(Method.GET, "https://httpbin.org/get"))
```

**Virtual Thread Classic HttpClient:**

```kotlin
import io.bluetape4k.http.hc5.classic.virtualThreadHttpClientOf

// HC5 Classic client backed by a Virtual Thread connection pool
val client = virtualThreadHttpClientOf(maxConnTotal = 200, maxConnPerRoute = 100)

client.use {
    val response = it.execute(classicRequestOf(Method.GET, "https://httpbin.org/get"))
    println(response.code)
}
```

**Production-tuned HttpClient:**

One-call factory that applies all recommended defaults: pooled connections, eviction of
expired/idle connections, keep-alive fallback for servers that omit the `Keep-Alive` header,
retry on transient failures, and conservative request timeouts.

```kotlin
import io.bluetape4k.http.hc5.classic.*
import io.bluetape4k.http.hc5.http.*

// All defaults: pool 200/100, eviction 60 s, keep-alive 60 s fallback, 3 retries, timeouts 5/10/30 s
val client = productionHttpClientOf()

// Custom pool size + longer response timeout
val client = productionHttpClientOf(
    maxConnTotal = 500,
    maxConnPerRoute = 200,
    requestConfig = productionRequestConfigOf(responseTimeout = Timeout.ofSeconds(60)),
)

// Virtual Thread variant (same tuning, virtual-thread connection pool)
val client = productionVirtualThreadHttpClientOf()
```

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxConnTotal` | 200 | Total pooled connections |
| `maxConnPerRoute` | 100 | Pooled connections per route |
| `connectionRequestTimeout` | 5 s | Wait for connection from pool |
| `connectTimeout` | 10 s | TCP connect handshake |
| `responseTimeout` | 30 s | First response byte deadline |
| `maxIdleTime` | 60 s | Idle connection eviction threshold |
| keep-alive fallback | 60 s | Used when server omits `Keep-Alive` |
| `maxRetries` | 3 | Retry count on transient failures |

**Async production-tuned client:**

```kotlin
import io.bluetape4k.http.hc5.async.*

val asyncClient = productionHttpAsyncClientOf()

// Customised
val asyncClient = productionHttpAsyncClientOf(
    maxConnTotal = 500,
    retryStrategy = defaultRetryStrategy(maxRetries = 5),
)
```

**Caching HttpClient:**

```kotlin
import io.bluetape4k.http.hc5.cache.*

// HttpClient with in-memory cache
val cacheStorage = InMemoryHttpCacheStorage.createObjectCache()
val cachingClient = cachingHttpClient(cacheStorage)

// Async caching client (JCache-based)
val asyncCachingClient = cachingHttpAsyncClient {
    setHttpCacheStorage(JavaCacheHttpCacheStorage.createObjectCache(jcache))
}
```

### 2. OkHttp3

Square's OkHttp3 client made convenient with a Kotlin DSL.

**Supported features:**

- Virtual Thread-based Dispatcher by default
- Connection pool management
- Logging/caching interceptors
- MockWebServer utilities
- Coroutines extensions

**DSL Builder Functions (`OkHttp3Support.kt`):**

| Function | Description |
|----------|-------------|
| `okhttp3Client(connectionPool, dispatcher, block)` | Create an `OkHttpClient` with optional pool/dispatcher |
| `okHttp3ConnectionPool(maxIdleConnections, keepAliveDuration)` | Create a `ConnectionPool` |
| `okhttp3DispatcherWithVirtualThread(maxRequests, maxRequestsPerHost)` | Create a `Dispatcher` backed by Virtual Threads |
| `okhttp3DispatcherOf(executor, maxRequests, maxRequestsPerHost)` | Create a `Dispatcher` with a custom `ExecutorService` |
| `okhttp3ClientBuilderOf(connectionPool, dispatcher, block)` | Get a pre-configured `OkHttpClient.Builder` |
| `okhttp3RequestOf(url, block)` | Create an `okhttp3.Request` |
| `okhttp3CacheControl(block)` | Create a `CacheControl` via DSL |
| `okhttp3CacheControlOf(maxAge, maxStale, minFresh)` | Create a `CacheControl` with duration parameters |

```kotlin
import io.bluetape4k.http.okhttp3.*

// Connection pool + Virtual Thread Dispatcher
val pool = okHttp3ConnectionPool(maxIdleConnections = 50)
val dispatcher = okhttp3DispatcherWithVirtualThread(maxRequests = 200)

val client = okhttp3Client(
    connectionPool = pool,
    dispatcher = dispatcher,
) {
    addInterceptor(LoggingInterceptor(log))
    addNetworkInterceptor(CachingResponseInterceptor())
}

// Request DSL
val request = okhttp3RequestOf("https://httpbin.org/get") {
    get()
    header("Accept", "application/json")
}

// Sync call
client.newCall(request).execute().use { response ->
    println(response.body.string())
}

// Async call in a Coroutines context
val response = client.executeSuspending(request)
```

`executeSuspending` contract:

- When the coroutine is cancelled, the underlying OkHttp `Call` is also cancelled.
- Returns `Response` on success and propagates the cause exception on failure.

### 3. Vert.x HttpClient

Integrates Eclipse Vert.x's async HttpClient with Kotlin Coroutines.

```kotlin
import io.bluetape4k.http.vertx.*
import io.vertx.kotlin.core.http.httpClientOptionsOf

val options = httpClientOptionsOf(
    maxPoolSize = 20,
    keepAlive = true,
)
val vertxClient = vertxHttpClientOf(options)
```

## HTTP Client Comparison

| Client            | Protocol         | Characteristics                     | Use Case                     |
|-------------------|------------------|-------------------------------------|------------------------------|
| HC5 Classic       | HTTP/1.1         | Stable, rich configuration          | Synchronous API calls        |
| HC5 Async         | HTTP/1.1, HTTP/2 | Async, Coroutines integration       | High-performance async       |
| OkHttp3           | HTTP/1.1, HTTP/2 | Lightweight, Virtual Thread default | General-purpose HTTP client  |
| Vert.x HttpClient | HTTP/1.1, HTTP/2 | Event loop-based                    | Vert.x ecosystem integration |
| Ktor CIO          | HTTP/1.x         | Suspend-native, lightweight         | Ktor-based apps / coroutine-first calls |

## Performance Benchmark

Three JMH (Java Microbenchmark Harness) benchmarks compare client throughput.
All benchmarks target a separate Docker container server, isolating the server JVM from the client JVM.

```bash
# Run all benchmarks
./gradlew :bluetape4k-http:testBenchmark

# Run a specific benchmark
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude="HttpClientBenchmark"
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude="HttpClientLatencyBenchmark"
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude="HttpClientCompressionCacheBenchmark"
```

### 1. HttpClientBenchmark — Base Throughput (`GET /ping`)

**Setup**: `BluetapeWebfluxServer` (Docker) · `@Threads(8)` · warmup 1×1s · measurement 1×1s

Lightweight `/ping` responses to measure pure connection throughput.

| Client | Mode | Notes |
|--------|------|-------|
| OkHttp3 Sync | sync | Platform thread |
| OkHttp3 VirtualThread | sync | Virtual Thread Dispatcher |
| OkHttp3 Coroutines | async | `Call.executeAsync()` (official okhttp-coroutines) |
| Java HttpClient Sync | sync | JDK built-in |
| Java HttpClient VirtualThread | sync | Virtual Thread executor |
| Java HttpClient H2 Sync | sync | HTTP/2 |
| HC5 Classic | sync | Apache HttpComponents 5 |
| HC5 Classic VirtualThread | sync | VT-based connection manager |
| HC5 Classic Coroutines | coroutine | `Dispatchers.IO` |
| HC5 Async Coroutines | async | `executeSuspending()` |
| Vert.x WebClient Coroutines | async | Event loop |
| Ktor CIO Coroutines | coroutine | CIO 3.5 opens dedicated HTTP/1 requests when pipelining is disabled |

> **Note**: With no simulated latency all modes produce similar throughput.
> Differences arise mainly from connection pool configuration and thread model.

### 2. HttpClientLatencyBenchmark — High-Latency Throughput (`GET /httpbin/delay/0.05`)

**Setup**: `BluetapeWebfluxServer` (Docker, 50 ms delay) · `@Threads(100)` · warmup 1×1s · measurement 1×1s

**Theoretical sync ceiling**: 100 threads × (1000 ms / 50 ms) = **2,000 ops/s**
Async / coroutine modes can exceed this ceiling without blocking threads.

### 2026-05-21 HTTP client benchmark snapshot

Environment: local Colima Docker, `bluetape4k/mock-webflux-server:latest`, Docker server 29.2.1, JMH via `:bluetape4k-http:testBenchmark`.
See [the benchmark report](../../docs/benchmarks/2026-05-21-io-http-client-benchmark.md) for commands, rejected approaches, and raw evidence notes.

The snapshot uses the same JMH thread count for every row in each benchmark.
Ktor CIO is no longer a one-thread exception, but the whole benchmark uses a short equal-thread window because CIO 3.5 opens dedicated HTTP/1 connections unless its pipeline path is enabled.

#### Base throughput snapshot

| Benchmark row | ops/s |
|---------------|------:|
| `HttpClientBenchmark.javaHttpSync` | 7,276.492 |
| `HttpClientBenchmark.hc5ClassicVirtualThread` | 7,246.690 |
| `HttpClientBenchmark.okhttp3VirtualThread` | 6,955.796 |
| `HttpClientBenchmark.javaHttpVirtualThread` | 6,562.497 |
| `HttpClientBenchmark.hc5Classic` | 6,490.422 |
| `HttpClientBenchmark.javaHttpH2VirtualThread` | 6,275.262 |
| `HttpClientBenchmark.hc5ClassicCoroutines` | 6,230.735 |
| `HttpClientBenchmark.vertxWebClientCoroutines` | 6,043.906 |
| `HttpClientBenchmark.javaHttpH2Sync` | 6,027.618 |
| `HttpClientBenchmark.okhttp3Sync` | 5,771.310 |
| `HttpClientBenchmark.okhttp3Coroutines` | 5,752.350 |
| `HttpClientBenchmark.hc5AsyncCoroutines` | 5,520.183 |
| `HttpClientBenchmark.javaHttpH2Coroutines` | 5,481.592 |
| `HttpClientBenchmark.javaHttpCoroutines` | 4,739.894 |
| `HttpClientBenchmark.ktorCioCoroutines` | 2,052.281 |

#### High-latency snapshot

| Benchmark row | ops/s |
|---------------|------:|
| `HttpClientLatencyBenchmark.okhttp3VirtualThread` | 1,902.171 |
| `HttpClientLatencyBenchmark.hc5ClassicVirtualThread` | 1,888.018 |
| `HttpClientLatencyBenchmark.javaHttpVirtualThread` | 1,883.634 |
| `HttpClientLatencyBenchmark.hc5Classic` | 1,880.023 |
| `HttpClientLatencyBenchmark.okhttp3Sync` | 1,870.124 |
| `HttpClientLatencyBenchmark.javaHttpSync` | 1,865.997 |
| `HttpClientLatencyBenchmark.javaHttpCoroutines` | 1,863.948 |
| `HttpClientLatencyBenchmark.hc5AsyncCoroutines` | 1,860.655 |
| `HttpClientLatencyBenchmark.vertxWebClientCoroutines` | 1,859.003 |
| `HttpClientLatencyBenchmark.okhttp3Coroutines` | 1,856.895 |
| `HttpClientLatencyBenchmark.ktorCioCoroutines` | 1,515.026 |
| `HttpClientLatencyBenchmark.hc5ClassicCoroutines` | 1,216.306 |

![HTTP client high-latency benchmark chart](../../docs/images/readme-diagrams/io-http-chart-02.svg)

**Notes**:
- The previous Vert.x result mainly measured the Vert.x 5 default HTTP/1 pool cap. The benchmark now configures `PoolOptions` to match peer clients.
- Ktor CIO's default path remains slower on `/ping` because it uses dedicated HTTP/1 connections. Forcing CIO pipelining produced EOFs or hangs against the mock fixtures, so the comparable run keeps default CIO behavior and shortens the window for every row.
- Base `/ping` measurements have high variance on this local Docker setup. Treat the high-latency table as the stronger comparison signal.

### 3. HttpClientCompressionCacheBenchmark — Cache + gzip Effect

**Setup**: `WireMockServer` (Docker, 10 ms fixed delay) · gzip 1 KB response · `Cache-Control: public, max-age=3600` · `@Threads(8)` · warmup 2×3s · measurement 3×5s

**Theoretical baseline (no cache)**: 8 threads × (1000 ms / 10 ms) = **800 ops/s**

| Client | Cache | ops/s | vs baseline |
|--------|-------|------:|-------------|
| HC5 Classic + InMemoryCache | In-memory (Heap) | **813,906** | ×1,233 |
| OkHttp3 + DiskLruCache | Disk (OS page cache) | **35,359** | ×53 |
| HC5 Classic (no cache) | — | 682 | ×1 |
| HC5 Classic VirtualThread (no cache) | — | 668 | — |
| OkHttp3 (no cache) | — | 661 | — |

![HTTP Cache Benchmark Throughput chart](../../docs/images/readme-charts/io-http-cache-throughput-chart-01.png)

**Key Insights**:
- **Cache effect**: Eliminating a 10 ms network RTT alone achieves 35K–813K ops/s
- **HC5 MemCache vs OkHttp DiskCache (23× gap)**:
  - HC5: `ConcurrentHashMap` direct lookup → ~1–10 μs/op
  - OkHttp: `DiskLruCache` `synchronized` + journal write + per-hit gzip decompression → ~200–230 μs/op
  - The 1 KB cache file fits in a single 4 KB OS page, so after warmup reads are purely from page cache (RAM), not real disk I/O — but the filesystem call overhead remains
- **OkHttp DiskCache at 35K ops/s is correct**: test-verified with `networkResponse == null` and `cacheResponse != null` on every cache hit

**Recommended client by use case**:

| Scenario | Recommendation |
|----------|----------------|
| Repeated GET + maximum cache throughput | HC5 CachingHttpClient (MemCache) |
| Cache persistence across restarts | OkHttp3 + DiskLruCache |
| General high-throughput (no caching needed) | HC5 Classic VirtualThread or OkHttp3 |
| High-latency async bulk requests | HC5 Async Coroutines or Vert.x WebClient |
| Ktor-based apps / coroutine-first calls | Ktor CIO |

## Backend Comparison

| Client | Protocol | Characteristics | Use case |
|--------|----------|-----------------|----------|
| HC5 Async | HTTP/1.x, HTTP/2 | Full-featured, caching, SSL, Virtual Thread | Enterprise backend, high-throughput |
| HC5 Classic | HTTP/1.x, HTTP/2 | Synchronous, VirtualThread support | Legacy code, blocking I/O |
| OkHttp3 | HTTP/1.x, HTTP/2 | Interceptors, DiskLruCache, MockWebServer | General-purpose, Android-compatible |
| JDK | HTTP/1.x, HTTP/2 | Standard library, no extra dependency | Minimal footprint, Java-native |
| Vert.x | HTTP/1.x, HTTP/2 | Event-loop, reactive, ALPN | Vert.x-based applications |
| Ktor CIO | HTTP/1.x | Suspend-native, Ktor plugin ecosystem, lightweight | Ktor-based apps/libraries and coroutine-first calls |

> **Note**: Ktor CIO does not support HTTP/2. For HTTP/2 use cases, prefer HC5 Async, JDK, or OkHttp3.

## Coroutines Support

All async HTTP clients support natural use in Coroutines contexts via the `executeSuspending` extension function.

```kotlin
import kotlinx.coroutines.*

suspend fun fetchData() = coroutineScope {
    val client = httpAsyncClient { /* configuration */ }

    // Parallel requests
    val response1 = async { client.executeSuspending(request1) }
    val response2 = async { client.executeSuspending(request2) }

    val results = awaitAll(response1, response2)
}
```

## Module Structure

```
io.bluetape4k.http
├── hc5/                    # Apache HttpComponents 5
│   ├── async/              # Async client, Coroutines integration
│   ├── cache/              # Caching client (In-Memory, JCache)
│   ├── classic/            # Sync client
│   ├── entity/             # Entity/Multipart builders
│   ├── fluent/             # Fluent API extensions
│   ├── http/               # Request/Response builder, config
│   ├── http2/              # HTTP/2 configuration
│   ├── protocol/           # HttpClientContext extensions
│   ├── reactor/            # IOReactor configuration
│   ├── routing/            # Routing utilities
│   └── ssl/                # SSL/TLS configuration
├── okhttp3/                # OkHttp3
│   ├── OkHttp3Support.kt   # Client/Request/Response DSL
│   ├── LoggingInterceptor.kt
│   ├── CachingRequestInterceptor.kt
│   ├── CachingResponseInterceptor.kt
│   └── mock/               # MockWebServer utilities
├── vertx/                  # Vert.x HttpClient
│   └── VertxHttpClientSupport.kt
└── ktor/                   # Ktor Client (optional, suspend-native)
    └── KtorHttpClientSupport.kt
```

## Dependencies

```kotlin
dependencies {
    implementation(project(":bluetape4k-http"))

    // Add compileOnly for each backend you use.
    // Change to implementation in application projects where runtime availability is required.
    compileOnly("org.apache.httpcomponents.client5:httpclient5") // HC5
    compileOnly("com.squareup.okhttp3:okhttp")                   // OkHttp3
    compileOnly("io.vertx:vertx-core")                           // Vert.x
    compileOnly("io.ktor:ktor-client-core")                      // Ktor Client (any engine)
    compileOnly("io.ktor:ktor-client-cio")                       // Ktor CIO engine (HTTP/1.x)
}
```

## Testing

```bash
# Run HTTP module tests
./gradlew :bluetape4k-http:test

# Check line coverage
./gradlew :bluetape4k-http:koverLog
```

### Test Coverage

Line coverage: **72%** (target: ≥ 70%)

Covered packages:

| Package | Coverage | Tests |
|---------|----------|-------|
| `hc5/async` | ✅ | `AsyncHttpClientTest`, `AsyncHttpClientCoroutinesTest`, `MinimalHttpAsyncClientTest` |
| `hc5/async/methods` | ✅ | `SimpleHttpRequestTest`, `SimpleHttpResponseTest`, `AsyncMethodsTest` |
| `hc5/cache` | ✅ | `CachingHttpClientBuilderTest`, `CachingHttpAsyncClientBuilderTest` |
| `hc5/classic` | ✅ | `ClassicHttpClientTest`, `MinimalAndVirtualThreadHttpClientTest` |
| `hc5/fluent` | ✅ | `RequestTest` |
| `hc5/http` | ✅ | `ContextBuilderTest`, `CookieSpecSupportTest`, `PoolingHttpClientConnectionManagerBuilderTest`, `BasicRequestProducerTest` |
| `hc5/protocol` | ✅ | `HttpClientContextTest` |
| `hc5/routing` | ✅ | `RoutingSupportTest` |
| `hc5/ssl` | ✅ | `SslSupportTest` |
| `jdk` | ✅ | `JdkHttpClientSupportTest`, `JdkHttpClientCoroutinesTest` |
| `okhttp3` | ✅ | Multiple tests |
| `ktor` | ✅ | `KtorHttpClientSupportTest` |

## References

- [Apache HttpComponents 5](https://hc.apache.org/httpcomponents-client-5.4.x/)
- [OkHttp](https://square.github.io/okhttp/)
- [Vert.x HttpClient](https://vertx.io/docs/vertx-core/kotlin/)
- [Ktor Client](https://ktor.io/docs/client-create-and-configure.html)
- [httpbin.org](https://httpbin.org/) - HTTP testing API
