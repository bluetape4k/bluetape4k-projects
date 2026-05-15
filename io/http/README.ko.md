# Module bluetape4k-http

[English](./README.md) | 한국어

## 개요

`bluetape4k-http`는 다양한 HTTP 클라이언트 라이브러리를 Kotlin 확장 함수와 DSL로 통합하여 제공하는 모듈입니다.

Apache HttpComponents 5, OkHttp3, Vert.x HttpClient, Ktor Client 등을 일관된 방식으로 사용할 수 있으며, Kotlin Coroutines와 Virtual Threads를 기본 지원합니다.

## 아키텍처

### 전체 아키텍처: 다중 백엔드 HTTP 클라이언트

```mermaid
flowchart TD
    subgraph Application["애플리케이션"]
        APP[애플리케이션 코드]
        CO[Coroutines\nsuspend fun]
    end

    subgraph bluetape4k-http
        EXT[executeSuspending\n확장 함수]
        DSL[Builder DSL\nhttpAsyncClient / okhttp3Client / vertxHttpClientOf / ktorCioHttpClientOf]
    end

    subgraph Backends["HTTP 클라이언트 백엔드"]
        HC5A[HC5 Async\nhttpAsyncClient]
        HC5C[HC5 Classic\nhttpClient]
        HC5CA[HC5 캐싱\ncachingHttpAsyncClient]
        OKH[OkHttp3\nokhttp3Client]
        VTX[Vert.x HttpClient\nvertxHttpClientOf]
        KTOR[Ktor CIO\nktorCioHttpClientOf]
    end

    APP --> CO
    CO --> EXT
    EXT --> DSL
    DSL --> HC5A
    DSL --> HC5C
    DSL --> HC5CA
    DSL --> OKH
    DSL --> VTX
    DSL --> KTOR
    HC5A --> SERVER[(HTTP 서버)]
    HC5C --> SERVER
    HC5CA --> SERVER
    OKH --> SERVER
    VTX --> SERVER
    KTOR --> SERVER

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef utilStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F

    class APP coreStyle
    class CO asyncStyle
    class EXT,DSL utilStyle
    class HC5A,HC5C,HC5CA,OKH,VTX,KTOR serviceStyle
    class SERVER extStyle
```

### HTTP 클라이언트 계층 (HC5)

```mermaid
classDiagram
    class CloseableHttpAsyncClient {
        <<ApacheHC5>>
        +execute(request, callback) Future
        +start()
        +close()
    }

    class HttpAsyncClientCoroutines {
        <<확장함수>>
        +executeSuspending(request) SimpleHttpResponse
    }

    class CachingHttpAsyncClientBuilder {
        <<DSL빌더>>
        +setHttpCacheStorage(storage)
        +build() CloseableHttpAsyncClient
    }

    CachingHttpAsyncClientBuilder --> InMemoryHttpCacheStorage : 사용
    CachingHttpAsyncClientBuilder --> JavaCacheHttpCacheStorage : 사용
    CloseableHttpAsyncClient <.. HttpAsyncClientCoroutines : 확장

    style CloseableHttpAsyncClient fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style HttpAsyncClientCoroutines fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style CachingHttpAsyncClientBuilder fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style InMemoryHttpCacheStorage fill:#F57F17,stroke:#E65100,color:#000000
    style JavaCacheHttpCacheStorage fill:#F57F17,stroke:#E65100,color:#000000
```

### OkHttp3 클라이언트 계층

```mermaid
classDiagram
    class OkHttpClient {
        <<OkHttp3>>
        +newCall(request) Call
    }

    class LoggingInterceptor {
        +intercept(chain) Response
    }

    class CachingRequestInterceptor {
        +intercept(chain) Response
    }

    class CachingResponseInterceptor {
        +intercept(chain) Response
    }

    class OkHttpClientExtensionsCoroutines {
        <<확장함수>>
        +executeSuspending(request) Response
    }

    OkHttpClient --> LoggingInterceptor : addInterceptor
    OkHttpClient --> CachingRequestInterceptor : addInterceptor
    OkHttpClient --> CachingResponseInterceptor : addNetworkInterceptor
    OkHttpClient <.. OkHttpClientExtensionsCoroutines : 확장

    style OkHttpClient fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style LoggingInterceptor fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style CachingRequestInterceptor fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style CachingResponseInterceptor fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style OkHttpClientExtensionsCoroutines fill:#FFF3E0,stroke:#FFCC80,color:#E65100
```

### 비동기 HTTP 요청 흐름 (HC5 Async + Coroutines)

```mermaid
sequenceDiagram
        participant App as 애플리케이션
        participant Ext as executeSuspending()
        participant HC5 as CloseableHttpAsyncClient
        participant Server as HTTP 서버

    App->>Ext: suspend fun executeSuspending(request)
    Ext->>HC5: execute(request, FutureCallback)
    HC5->>Server: HTTP 요청 (비동기)
    Server-->>HC5: HTTP 응답
    HC5-->>Ext: FutureCallback.completed(response)
    Ext-->>App: SimpleHttpResponse
```

## 주요 기능

### 1. Apache HttpComponents 5 (HC5)

Apache HttpClient 5를 Kotlin DSL과 Coroutines로 래핑하여 동기/비동기 HTTP 통신을 지원합니다.

**지원 기능:**

- Classic HttpClient (동기 방식)
- Async HttpClient (비동기, Coroutines 통합)
- HTTP/2 지원 (httpcore5-h2)
- 캐싱 HttpClient (In-Memory, JCache)
- Connection Pool 관리
- SSL/TLS 설정
- Fluent API

```kotlin
import io.bluetape4k.http.hc5.async.*

// Async HttpClient 생성
val client = httpAsyncClient {
    setConnectionManager(cm)
    setMaxConnTotal(100)
    setMaxConnPerRoute(10)
}

// Coroutines 환경에서 비동기 요청
val request = SimpleHttpRequest.get("https://httpbin.org/get")
val response: SimpleHttpResponse = client.executeSuspending(request)
```

**Classic HttpClient:**

```kotlin
import io.bluetape4k.http.hc5.classic.*

// Classic HttpClient 생성
val client = httpClient {
    setConnectionManager(poolingConnectionManager())
}

// 동기 요청
val response = client.execute(classicRequestOf(Method.GET, "https://httpbin.org/get"))
```

**Virtual Thread Classic HttpClient:**

```kotlin
import io.bluetape4k.http.hc5.classic.virtualThreadHttpClientOf

// Virtual Thread 기반 커넥션 풀을 사용하는 HC5 Classic 클라이언트
val client = virtualThreadHttpClientOf(maxConnTotal = 200, maxConnPerRoute = 100)

client.use {
    val response = it.execute(classicRequestOf(Method.GET, "https://httpbin.org/get"))
    println(response.code)
}
```

**캐싱 HttpClient:**

```kotlin
import io.bluetape4k.http.hc5.cache.*

// In-Memory 캐시를 사용하는 HttpClient
val cacheStorage = InMemoryHttpCacheStorage.createObjectCache()
val cachingClient = cachingHttpClient(cacheStorage)

// Async 캐싱 클라이언트 (JCache 기반)
val asyncCachingClient = cachingHttpAsyncClient {
    setHttpCacheStorage(JavaCacheHttpCacheStorage.createObjectCache(jcache))
}
```

### 2. OkHttp3

Square의 OkHttp3 클라이언트를 Kotlin DSL로 간편하게 생성하고 사용할 수 있습니다.

**지원 기능:**

- Virtual Thread 기반 Dispatcher 기본 사용
- Connection Pool 관리
- 로깅/캐싱 Interceptor
- MockWebServer 유틸리티
- Coroutines 확장

**DSL 빌더 함수 (`OkHttp3Support.kt`):**

| 함수 | 설명 |
|-----|------|
| `okhttp3Client(connectionPool, dispatcher, block)` | `OkHttpClient` 생성 (pool/dispatcher 선택 설정) |
| `okHttp3ConnectionPool(maxIdleConnections, keepAliveDuration)` | `ConnectionPool` 생성 |
| `okhttp3DispatcherWithVirtualThread(maxRequests, maxRequestsPerHost)` | Virtual Thread 기반 `Dispatcher` 생성 |
| `okhttp3DispatcherOf(executor, maxRequests, maxRequestsPerHost)` | 커스텀 `ExecutorService` 기반 `Dispatcher` 생성 |
| `okhttp3ClientBuilderOf(connectionPool, dispatcher, block)` | 사전 구성된 `OkHttpClient.Builder` 반환 |
| `okhttp3RequestOf(url, block)` | `okhttp3.Request` DSL 생성 |
| `okhttp3CacheControl(block)` | `CacheControl` DSL 생성 |
| `okhttp3CacheControlOf(maxAge, maxStale, minFresh)` | 기간 파라미터 기반 `CacheControl` 생성 |

```kotlin
import io.bluetape4k.http.okhttp3.*

// Connection Pool + Virtual Thread Dispatcher
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

// 동기 호출
client.newCall(request).execute().use { response ->
    println(response.body.string())
}

// Coroutines 환경에서 비동기 요청
val response = client.executeSuspending(request)
```

`executeSuspending` 계약:

- 코루틴이 취소되면 내부 OkHttp `Call`도 함께 취소됩니다.
- 성공 시 `Response`를 그대로 반환하고, 실패 시 원인 예외를 전파합니다.

### 3. Vert.x HttpClient

Eclipse Vert.x의 비동기 HttpClient를 Kotlin Coroutines와 통합합니다.

```kotlin
import io.bluetape4k.http.vertx.*
import io.vertx.kotlin.core.http.httpClientOptionsOf

val options = httpClientOptionsOf(
    maxPoolSize = 20,
    keepAlive = true,
)
val vertxClient = vertxHttpClientOf(options)
```

## HTTP 클라이언트 비교

| 클라이언트             | 프로토콜             | 특성                    | 용도            |
|-------------------|------------------|-----------------------|---------------|
| HC5 Classic       | HTTP/1.1         | 안정적, 풍부한 설정           | 동기 API 호출     |
| HC5 Async         | HTTP/1.1, HTTP/2 | 비동기, Coroutines 통합    | 고성능 비동기 통신    |
| OkHttp3           | HTTP/1.1, HTTP/2 | 경량, Virtual Thread 기본 | 범용 HTTP 클라이언트 |
| Vert.x HttpClient | HTTP/1.1, HTTP/2 | 이벤트 루프 기반             | Vert.x 생태계 통합 |
| Ktor CIO          | HTTP/1.x         | suspend-native, 경량, Ktor 플러그인 생태계 | Ktor 기반 앱 / 코루틴 우선 호출 |

> **참고**: Ktor CIO는 HTTP/2를 지원하지 않습니다. HTTP/2가 필요한 경우 HC5 Async, JDK, 또는 OkHttp3를 사용하세요.

## 성능 벤치마크

JMH(Java Microbenchmark Harness) 기반 벤치마크 3종으로 클라이언트별 처리량을 측정합니다.
모든 벤치마크는 별도의 Docker 컨테이너 서버에 요청하여 서버 JVM과 클라이언트 JVM을 분리합니다.

```bash
# 전체 벤치마크 실행
./gradlew :bluetape4k-http:testBenchmark

# 특정 벤치마크만 실행
./gradlew :bluetape4k-http:testBenchmark -Pbenchmark.include="HttpClientBenchmark"
./gradlew :bluetape4k-http:testBenchmark -Pbenchmark.include="HttpClientLatencyBenchmark"
./gradlew :bluetape4k-http:testBenchmark -Pbenchmark.include="HttpClientCompressionCacheBenchmark"
```

### 1. HttpClientBenchmark — 기본 처리량 (`GET /ping`)

**환경**: `BluetapeHttpServer` (Docker) · `@Threads(8)` · warmup 1×2s · measurement 3×3s

경량 `/ping` 응답으로 순수 연결 처리량을 측정합니다.

| 클라이언트 | 방식 | 특징 |
|-----------|------|------|
| OkHttp3 Sync | 동기 | 플랫폼 스레드 |
| OkHttp3 VirtualThread | 동기 | Virtual Thread Dispatcher |
| OkHttp3 Coroutines | 비동기 | `Call.executeAsync()` (공식 okhttp-coroutines) |
| Java HttpClient Sync | 동기 | JDK 내장 |
| Java HttpClient VirtualThread | 동기 | Virtual Thread executor |
| Java HttpClient H2 Sync | 동기 | HTTP/2 |
| HC5 Classic | 동기 | Apache HttpComponents 5 |
| HC5 Classic VirtualThread | 동기 | VT 기반 커넥션 매니저 |
| HC5 Classic Coroutines | 코루틴 | `Dispatchers.IO` |
| HC5 Async Coroutines | 비동기 | `executeSuspending()` |
| Vert.x WebClient Coroutines | 비동기 | 이벤트 루프 |

> **참고**: 지연 없는 경량 응답이므로 동기/비동기 방식 모두 유사한 처리량을 냅니다.
> 차이는 주로 커넥션 풀 설정과 스레드 모델에서 발생합니다.

### 2. HttpClientLatencyBenchmark — 고지연 환경 처리량 (`GET /httpbin/delay/0.05`)

**환경**: `BluetapeHttpServer` (Docker, 50ms 지연) · `@Threads(100)` · warmup 1×3s · measurement 3×5s

**이론값**: 100 threads × (1000ms / 50ms) = **2,000 ops/s** (동기 상한)
비동기/코루틴 방식은 스레드 블로킹 없이 이 상한을 초과합니다.

| 클라이언트 | 방식 | 비고 |
|-----------|------|------|
| OkHttp3 Sync | 동기 | 100 플랫폼 스레드 차단 |
| OkHttp3 VirtualThread | 동기 | VT로 차단 비용 감소 |
| OkHttp3 Coroutines | 비동기 | `Dispatchers.IO` + `executeAsync()` |
| Java HttpClient Sync | 동기 | — |
| Java HttpClient VirtualThread | 동기 | — |
| Java HttpClient Coroutines | 비동기 | `sendAwait()` |
| HC5 Classic | 동기 | — |
| HC5 Classic VirtualThread | 동기 | — |
| HC5 Classic Coroutines | 코루틴 | `Dispatchers.IO` |
| HC5 Async Coroutines | 비동기 | `executeSuspending()` |
| Vert.x WebClient Coroutines | 비동기 | — |

### 3. HttpClientCompressionCacheBenchmark — 캐시 + gzip 효과

**환경**: `WireMockServer` (Docker, 10ms 고정 지연) · gzip 1KB 응답 · `Cache-Control: public, max-age=3600` · `@Threads(8)` · warmup 2×3s · measurement 3×5s

**이론값(캐시 없음)**: 8 threads × (1000ms / 10ms) = **800 ops/s**

| 클라이언트 | 캐시 | ops/s | 배율 |
|-----------|------|------:|------|
| HC5 Classic + InMemoryCache | 인메모리 (Heap) | **813,906** | ×1,233 |
| OkHttp3 + DiskLruCache | 디스크 (OS 페이지 캐시) | **35,359** | ×53 |
| HC5 Classic (캐시 없음) | — | 682 | ×1 |
| HC5 Classic VirtualThread (캐시 없음) | — | 668 | — |
| OkHttp3 (캐시 없음) | — | 661 | — |

**인사이트**:
- **캐시 효과**: 10ms 네트워크 지연 제거만으로 35K–813K ops/s 달성
- **HC5 MemCache vs OkHttp DiskCache (23배 차이)**:
  - HC5: `ConcurrentHashMap` 직접 조회 → ~1–10 μs/op
  - OkHttp: `DiskLruCache` `synchronized` + journal write + gzip 재해제 → ~200–230 μs/op
  - OkHttp 캐시 파일(1KB)은 워밍업 후 OS 페이지 캐시(RAM)에 올라가므로 실제 디스크 I/O는 없으나, 파일 시스템 계층 오버헤드가 남음

```mermaid
bar
    title HTTP 캐시 효과 (ops/s, @Threads=8, WireMock 10ms 지연)
    "HC5 + MemCache" : 813906
    "OkHttp + DiskCache" : 35359
    "NoCache 기준" : 682
```

**권장 선택**:

| 상황 | 권장 |
|------|------|
| 반복 GET + 캐시 최우선 | HC5 CachingHttpClient (MemCache) |
| 재시작 후 캐시 유지 필요 | OkHttp3 + DiskLruCache |
| 범용 고성능 (캐시 불필요) | HC5 Classic VirtualThread 또는 OkHttp3 |
| 고지연 비동기 대량 요청 | HC5 Async Coroutines 또는 Vert.x WebClient |

## Coroutines 지원

모든 비동기 HTTP 클라이언트는 `executeSuspending` 확장 함수를 통해 Coroutines 환경에서 자연스럽게 사용할 수 있습니다.

```kotlin
import kotlinx.coroutines.*

suspend fun fetchData() = coroutineScope {
    val client = httpAsyncClient { /* 설정 */ }

    // 병렬 요청
    val response1 = async { client.executeSuspending(request1) }
    val response2 = async { client.executeSuspending(request2) }

    val results = awaitAll(response1, response2)
}
```

## 모듈 구조

```
io.bluetape4k.http
├── hc5/                    # Apache HttpComponents 5
│   ├── async/              # 비동기 클라이언트, Coroutines 통합
│   ├── cache/              # 캐싱 클라이언트 (In-Memory, JCache)
│   ├── classic/            # 동기 클라이언트
│   ├── entity/             # Entity/Multipart 빌더
│   ├── fluent/             # Fluent API 확장
│   ├── http/               # Request/Response 빌더, Config
│   ├── http2/              # HTTP/2 설정
│   ├── protocol/           # HttpClientContext 확장
│   ├── reactor/            # IOReactor 설정
│   ├── routing/            # 라우팅 유틸리티
│   └── ssl/                # SSL/TLS 설정
├── okhttp3/                # OkHttp3
│   ├── OkHttp3Support.kt   # 클라이언트/Request/Response DSL
│   ├── LoggingInterceptor.kt
│   ├── CachingRequestInterceptor.kt
│   ├── CachingResponseInterceptor.kt
│   └── mock/               # MockWebServer 유틸리티
├── vertx/                  # Vert.x HttpClient
│   └── VertxHttpClientSupport.kt
└── ktor/                   # Ktor Client (선택적, suspend-native)
    └── KtorHttpClientSupport.kt
```

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-http"))

    // Apache HttpComponents 5 (기본 백엔드)
    implementation("org.apache.httpcomponents.client5:httpclient5")

    // 선택적 백엔드 — 사용할 백엔드만 compileOnly로 추가
    compileOnly("com.squareup.okhttp3:okhttp")               // OkHttp3
    compileOnly("io.vertx:vertx-core")                       // Vert.x
    compileOnly("io.ktor:ktor-client-core")                  // Ktor Client (엔진 선택)
    compileOnly("io.ktor:ktor-client-cio")                   // Ktor CIO 엔진 (HTTP/1.x)
}
```

## 테스트

```bash
# HTTP 모듈 테스트 실행
./gradlew :bluetape4k-http:test

# 라인 커버리지 확인
./gradlew :bluetape4k-http:koverLog
```

### 테스트 커버리지

라인 커버리지: **72%** (목표: ≥ 70%)

| 패키지 | 커버리지 | 테스트 클래스 |
|--------|----------|---------------|
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
| `okhttp3` | ✅ | 다수의 테스트 |
| `ktor` | ✅ | `KtorHttpClientSupportTest` |

## 참고

- [Apache HttpComponents 5](https://hc.apache.org/httpcomponents-client-5.4.x/)
- [OkHttp](https://square.github.io/okhttp/)
- [Vert.x HttpClient](https://vertx.io/docs/vertx-core/kotlin/)
- [Ktor Client](https://ktor.io/docs/client-create-and-configure.html)
- [httpbin.org](https://httpbin.org/) - HTTP 테스트용 API
