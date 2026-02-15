# Module bluetape4k-http

## 개요

`bluetape4k-http`는 다양한 HTTP 클라이언트 라이브러리를 Kotlin 확장 함수와 DSL로 통합하여 제공하는 모듈입니다.

Apache HttpComponents 5, OkHttp3, Vert.x HttpClient, AsyncHttpClient 등을 일관된 방식으로 사용할 수 있으며, Kotlin Coroutines와 Virtual Threads를 기본 지원합니다.

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

**캐싱 HttpClient:**

```kotlin
import io.bluetape4k.http.hc5.cache.*

// In-Memory 캐시를 사용하는 HttpClient
val cachingClient = cachingHttpClient {
    setCacheStorage(InMemoryHttpCacheStorage())
}

// Async 캐싱 클라이언트
val asyncCachingClient = cachingHttpAsyncClient {
    setCacheStorage(JavaCacheHttpCacheStorage(jcache))
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

```kotlin
import io.bluetape4k.http.okhttp3.*

// Virtual Thread 기반 OkHttpClient 생성
val client = okhttp3Client {
    addInterceptor(LoggingInterceptor())
    addInterceptor(CachingResponseInterceptor())
}

// Request DSL
val request = okhttp3RequestOf("https://httpbin.org/get") {
    get()
    header("Accept", "application/json")
}

// Coroutines 환경에서 비동기 요청
val response = client.executeSuspending(request)
```

### 3. Vert.x HttpClient

Eclipse Vert.x의 비동기 HttpClient를 Kotlin Coroutines와 통합합니다.

```kotlin
import io.bluetape4k.http.vertx.*

val vertxClient = vertxHttpClient {
    setMaxPoolSize(20)
    setKeepAlive(true)
}
```

### 4. AsyncHttpClient (AHC)

Netty 기반의 AsyncHttpClient를 Kotlin Coroutines로 래핑합니다.

```kotlin
import io.bluetape4k.http.ahc.*

val client = asyncHttpClient {
    setMaxConnections(100)
    setMaxConnectionsPerHost(10)
}

// Coroutines 환경에서 비동기 요청
val response = client.executeSuspending {
    prepareGet("https://httpbin.org/get")
}
```

## HTTP 클라이언트 비교

| 클라이언트             | 프로토콜             | 특성                    | 용도            |
|-------------------|------------------|-----------------------|---------------|
| HC5 Classic       | HTTP/1.1         | 안정적, 풍부한 설정           | 동기 API 호출     |
| HC5 Async         | HTTP/1.1, HTTP/2 | 비동기, Coroutines 통합    | 고성능 비동기 통신    |
| OkHttp3           | HTTP/1.1, HTTP/2 | 경량, Virtual Thread 기본 | 범용 HTTP 클라이언트 |
| Vert.x HttpClient | HTTP/1.1, HTTP/2 | 이벤트 루프 기반             | Vert.x 생태계 통합 |
| AsyncHttpClient   | HTTP/1.1, HTTP/2 | Netty 기반, 고성능         | 대량 비동기 요청     |

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

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-http"))

    // 선택적 의존성 (필요한 것만 추가)
    implementation("com.squareup.okhttp3:okhttp")           // OkHttp3
    implementation("org.asynchttpclient:async-http-client")  // AsyncHttpClient
    implementation("io.vertx:vertx-core")                    // Vert.x
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
├── ahc/                    # AsyncHttpClient
│   ├── AsyncHttpClientSupport.kt
│   └── CoroutineSupport.kt
└── vertx/                  # Vert.x HttpClient
    └── VertxHttpClientSupport.kt
```

## 테스트

```bash
# HTTP 모듈 테스트 실행
./gradlew :bluetape4k-http:test
```

## 참고

- [Apache HttpComponents 5](https://hc.apache.org/httpcomponents-client-5.4.x/)
- [OkHttp](https://square.github.io/okhttp/)
- [Vert.x HttpClient](https://vertx.io/docs/vertx-core/kotlin/)
- [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client)
- [httpbin.org](https://httpbin.org/) - HTTP 테스트용 API
