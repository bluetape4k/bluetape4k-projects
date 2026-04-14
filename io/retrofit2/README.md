# Module bluetape4k-retrofit2

English | [한국어](./README.ko.md)

## Overview

`bluetape4k-retrofit2` is a module that extends [Retrofit2](https://square.github.io/retrofit/) with Kotlin DSL and Coroutines support.

Beyond the default OkHttp transport, it supports multiple HTTP backends including Apache HC5, Vert.x, and AsyncHttpClient. It also provides error handling via Kotlin's
`Result` type and automatically detects and registers Reactive Streams adapters.

## Architecture

### Overall Architecture: Retrofit2 + Coroutines + Result Pattern

```mermaid
flowchart TD
    subgraph Application["Application"]
        APP[Application Code]
        API[Retrofit Interface\nsuspend fun / Result~T~]
    end

    subgraph bluetape4k-retrofit2
        RB[retrofitOf DSL]
        RCA[ResultCallAdapterFactory]
        RC[ResultCall]
        RX[Reactive Adapters\nRxJava2/3 / Reactor]
    end

    subgraph CallFactory["Call.Factory (HTTP Backend)"]
        OKH[OkHttpClient\ndefault]
        HC5[Hc5CallFactory\nApache HC5]
        VTX[VertxCallFactory\nVert.x]
        AHC[AhcCallFactory\nAsyncHttpClient]
    end

    subgraph Converter["Converter Factory"]
        JCF[jacksonConverterFactoryOf\nJSON]
        SCF[defaultScalarsConverterFactory\nScalars]
    end

    APP --> API
    API --> RB
    RB --> RCA
    RCA --> RC
    RB --> CallFactory
    RB --> Converter
    RB --> RX
    OKH --> SERVER[(HTTP Server)]
    HC5 --> SERVER
    VTX --> SERVER
    AHC --> SERVER

    classDef coreStyle fill:#1B5E20,stroke:#1B5E20,color:#FFFFFF,font-weight:bold
    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef utilStyle fill:#E65100,stroke:#E65100,color:#FFFFFF
    classDef asyncStyle fill:#6A1B9A,stroke:#6A1B9A,color:#FFFFFF

    class APP,API coreStyle
    class RB,RCA,RC serviceStyle
    class OKH,HC5,VTX,AHC asyncStyle
    class JCF,SCF utilStyle
    class RX utilStyle
```

### Retrofit2 + Result Pattern Integration

```mermaid
classDiagram
    class Retrofit {
        <<Retrofit2>>
        +create(serviceClass) T
        +baseUrl() HttpUrl
    }

    class CallAdapter {
        <<interface>>
        +responseType() Type
        +adapt(call) T
    }

    class ResultCallAdapterFactory {
        +get(returnType, annotations, retrofit) CallAdapter?
    }

    class ResultCall {
        -delegate: Call~T~
        +execute() Response~Result~T~~
        +enqueue(callback)
        +clone() Call~Result~T~~
    }

    class Hc5CallFactory {
        -asyncClient: CloseableHttpAsyncClient
        +newCall(request) Call
        +close()
    }

    class VertxCallFactory {
        +newCall(request) Call
    }

    class AhcCallFactory {
        +newCall(request) Call
    }

    CallAdapter <|.. ResultCallAdapterFactory
    ResultCallAdapterFactory ..> ResultCall : creates
    Retrofit --> ResultCallAdapterFactory : addCallAdapterFactory
    Retrofit --> Hc5CallFactory : callFactory
    Retrofit --> VertxCallFactory : callFactory
    Retrofit --> AhcCallFactory : callFactory

    style Retrofit fill:#37474F,stroke:#263238,color:#FFFFFF
    style CallAdapter fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style ResultCallAdapterFactory fill:#00897B,stroke:#00695C,color:#FFFFFF
    style ResultCall fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    style Hc5CallFactory fill:#00897B,stroke:#00695C,color:#FFFFFF
    style VertxCallFactory fill:#00897B,stroke:#00695C,color:#FFFFFF
    style AhcCallFactory fill:#00897B,stroke:#00695C,color:#FFFFFF
```

### Suspend Function HTTP Request Flow (Result Pattern)

```mermaid
sequenceDiagram
    box rgb(232, 245, 233) Application
        participant App as Application
    end
    box rgb(237, 231, 246) Retrofit
        participant API as Retrofit Interface (suspend fun)
        participant RC as ResultCall
        participant CF as Call.Factory (e.g. Hc5CallFactory)
    end
    box rgb(227, 242, 253) Server
        participant Server as HTTP Server
    end

    App->>API: suspend fun getUser(): Result~User~
    API->>RC: enqueue(callback)
    RC->>CF: delegate.enqueue(resultCallback)
    CF->>Server: HTTP request (async)
    Server-->>CF: HTTP response
    alt 2xx success
        CF-->>RC: onResponse (body != null)
        RC-->>API: Result.success(body)
    else 4xx/5xx failure
        CF-->>RC: onResponse (isSuccessful == false)
        RC-->>API: Result.failure(HttpException)
    else network error
        CF-->>RC: onFailure(throwable)
        RC-->>API: Result.failure(IOException)
    end
    API-->>App: Result~User~
```

## Key Features

### 1. Retrofit Builder DSL

Build a Retrofit instance concisely using Kotlin DSL.

```kotlin
import io.bluetape4k.retrofit2.*

// DSL style
val retrofit = retrofit("https://api.github.com", defaultJsonConverterFactory) {
    callFactory(okhttp3Client())
    addCallAdapterFactory(ResultCallAdapterFactory())
}

// Factory function style (auto-detects CallAdapters)
val retrofit = retrofitOf(
    baseUrl = "https://api.github.com",
    callFactory = okhttp3Client(),
    converterFactory = defaultJsonConverterFactory,
)

// Create a service interface
val api = retrofit.service<GitHubApi>()
```

### 2. Result Pattern Support

`ResultCallAdapterFactory` wraps API responses safely in Kotlin's `Result` type.

```kotlin
interface GitHubApi {
    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): Result<User>

    @GET("users/{username}/repos")
    suspend fun getUserRepos(@Path("username") username: String): Result<List<Repo>>
}

// Error handling with the Result pattern
val result = api.getUser("octocat")
result.onSuccess { user ->
    println("User: ${user.name}")
}.onFailure { error ->
    println("Error: ${error.message}")
}
```

### 3. Coroutines Support

Declare suspend functions and async requests are automatically made in a coroutines context.

```kotlin
interface HttpbinApi {
    @GET("get")
    suspend fun get(): HttpbinResponse

    @POST("post")
    suspend fun post(@Body body: Map<String, Any>): HttpbinResponse
}

// Parallel requests in a coroutines context
suspend fun fetchMultiple(api: HttpbinApi) = coroutineScope {
    val response1 = async { api.get() }
    val response2 = async { api.get() }
    awaitAll(response1, response2)
}
```

Recommended usage:

- For new API designs, prefer `suspend fun` or `suspend fun ...: Result<T>` wherever possible.
- Use `Call<T>` +
  `executeAsync()` only when compatibility with existing Java callers or explicit cancellation/callback bridging is needed.
- When using Resilience4j `Retry`, use this module's `executeAsync(retry)` / `suspendExecute(retry)` — they retry with a
  `clone()`d `Call` internally.
- `ResultCallAdapterFactory` normalizes HTTP errors to
  `Result.failure(HttpException)`, making it especially useful when composing the business layer around
  `Result` instead of exceptions.

### 4. Multiple HTTP Backends (CallFactory)

Use HTTP clients other than OkHttp3 as a `Call.Factory`.

| CallFactory            | Underlying Library      | Characteristics                             |
|------------------------|-------------------------|---------------------------------------------|
| OkHttpClient (default) | OkHttp3                 | Lightweight, HTTP/2, general purpose        |
| Hc5CallFactory         | Apache HttpComponents 5 | Rich configuration, enterprise environments |
| VertxCallFactory       | Vert.x                  | Event-loop based, high performance          |
| AhcCallFactory         | AsyncHttpClient         | Netty-based, high-volume async requests     |

```kotlin
// Retrofit with Apache HC5
val retrofit = retrofitOf(
    baseUrl = "https://api.example.com",
    callFactory = Hc5CallFactory(httpClient),
)

// Retrofit with Vert.x
val retrofit = retrofitOf(
    baseUrl = "https://api.example.com",
    callFactory = VertxCallFactory(vertxClient),
)
```

### 5. Reactive Streams Adapter Auto-Detection

Automatically registers adapters for Reactive libraries found on the classpath.

- **RxJava2**: `RxJava2CallAdapterFactory`
- **RxJava3**: `RxJava3CallAdapterFactory`
- **Reactor**: `ReactorCallAdapterFactory`

```kotlin
// RxJava3 API
interface GitHubRxApi {
    @GET("users/{username}")
    fun getUser(@Path("username") username: String): Single<User>

    @GET("users/{username}/repos")
    fun getUserRepos(@Path("username") username: String): Flowable<List<Repo>>
}

// Reactor API
interface GitHubReactorApi {
    @GET("users/{username}")
    fun getUser(@Path("username") username: String): Mono<User>
}
```

### 6. Converter Factory

Provides Jackson-based JSON conversion out of the box, with Scalars conversion also supported.

```kotlin
// Default Jackson Converter (based on bluetape4k-jackson2)
val jsonFactory = defaultJsonConverterFactory

// Custom ObjectMapper
val customFactory = jacksonConverterFactoryOf(customObjectMapper)

// Scalars Converter (String, primitive types)
val scalarsFactory = defaultScalarsConverterFactory
```

## Usage Examples

```kotlin
interface HttpbinApi {
    // Synchronous call
    @GET("get")
    fun get(): Call<HttpbinResponse>

    // Coroutines
    @GET("get")
    suspend fun getSuspend(): HttpbinResponse

    // Result pattern
    @GET("get")
    suspend fun getResult(): Result<HttpbinResponse>

    // Path / Query parameters
    @GET("anything/{path}")
    suspend fun anything(
        @Path("path") path: String,
        @Query("key") key: String,
    ): HttpbinAnythingResponse

    // POST with Body
    @POST("post")
    suspend fun post(@Body body: Map<String, Any>): HttpbinResponse
}
```

## Module Structure

```
io.bluetape4k.retrofit2
├── RetrofitSupport.kt               # Retrofit Builder DSL and factory functions
├── RetrofitCallSupport.kt           # Call extension functions
├── SuspendRetrofitCallSupport.kt    # Suspend Call extension functions
├── ExceptionSupport.kt              # Exception handling utilities
├── result/                          # Result pattern
│   ├── ResultCall.kt                # Result-wrapping Call implementation
│   └── ResultCallAdapterFactory.kt  # Result CallAdapter factory
└── clients/                         # HTTP transport backends
    ├── hc5/                         # Apache HC5 CallFactory
    │   ├── Hc5CallFactory.kt
    │   └── Hc5OkHttp3Support.kt
    ├── vertx/                       # Vert.x CallFactory
    │   ├── VertxCallFactory.kt
    │   └── VertxOkHttp3Support.kt
    └── ahc/                         # AsyncHttpClient CallFactory
        └── AhcCallFactorySupport.kt
```

## Dependencies

```kotlin
dependencies {
    implementation(project(":bluetape4k-retrofit2"))

    // Optional dependencies
    implementation("com.squareup.retrofit2:converter-jackson")       // Jackson conversion
    implementation("com.squareup.retrofit2:converter-scalars")       // Scalars conversion
    implementation("com.squareup.retrofit2:adapter-rxjava3")         // RxJava3 adapter
    implementation("com.jakewharton.retrofit:retrofit2-reactor-adapter") // Reactor adapter
}
```

## Testing

```bash
# Run Retrofit2 module tests
./gradlew :bluetape4k-retrofit2:test
```

## References

- [Retrofit](https://square.github.io/retrofit/)
- [OkHttp](https://square.github.io/okhttp/)
- [Jackson](https://github.com/FasterXML/jackson)
- [RxJava3](https://github.com/ReactiveX/RxJava)
- [Project Reactor](https://projectreactor.io/)
