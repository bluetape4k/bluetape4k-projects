# Module bluetape4k-feign

English | [한국어](./README.ko.md)

## Overview

`bluetape4k-feign` extends [OpenFeign](https://github.com/OpenFeign/feign) with a Kotlin DSL and Coroutines support.

It allows REST API calls to be declared as interface methods, and supports pluggable HTTP transport layers including Apache HC5 and Vert.x.

## Architecture

### Overall Architecture: Feign + Coroutines Integration

```mermaid
flowchart TD
    subgraph Application
        APP[Application Code]
        API[Feign Interface\nsuspend fun / fun]
    end

    subgraph bluetape4k-feign
        FB[feignBuilderOf DSL]
        CFB[coroutineFeignBuilderOf DSL]
        PROXY[Feign Proxy\nCoroutineFeign]
    end

    subgraph Transport["HTTP Transport Layer"]
        AHC5[AsyncApacheHttp5Client]
        HC5[ApacheHttp5Client]
        VTX[VertxHttpClient / AsyncVertxHttpClient]
    end

    subgraph Codec
        JE[JacksonEncoder2]
        JD[JacksonDecoder2]
        FE[FeignFastjsonEncoder]
        FD[FeignFastjsonDecoder]
    end

    subgraph Resilience
        R4J[Resilience4jFeign\nCircuitBreaker / Retry]
    end

    APP --> API
    API --> PROXY
    FB --> PROXY
    CFB --> PROXY
    PROXY --> Transport
    PROXY --> Codec
    PROXY --> R4J
    AHC5 --> SERVER[(HTTP Server)]
    HC5 --> SERVER
    VTX --> SERVER

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef utilStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F

    class APP,API coreStyle
    class FB,CFB,PROXY serviceStyle
    class AHC5,HC5,VTX asyncStyle
    class JE,JD,FE,FD utilStyle
    class R4J extStyle
```

### Class Hierarchy: Feign + Coroutines

```mermaid
classDiagram
    class CoroutineFeign {
        <<feign_kotlin>>
    }

    class CoroutineBuilder {
        +client(asyncClient) CoroutineBuilder
        +encoder(encoder) CoroutineBuilder
        +decoder(decoder) CoroutineBuilder
        +options(options) CoroutineBuilder
        +logLevel(level) CoroutineBuilder
        +target(type, url) T
    }

    class AsyncClient {
        <<interface>>
    }

    class Encoder {
        <<interface>>
    }

    class Decoder {
        <<interface>>
    }

    class JacksonEncoder2 {
        -mapper: JsonMapper
        +encode(object, bodyType, template)
    }

    class JacksonDecoder2 {
        -mapper: JsonMapper
        +decode(response, type) Any?
    }

    class FeignFastjsonEncoder {
        +encode(object, bodyType, template)
    }

    class FeignFastjsonDecoder {
        +decode(response, type) Any?
    }

    CoroutineFeign *-- CoroutineBuilder
    CoroutineBuilder --> AsyncClient : configures
    CoroutineBuilder --> Encoder : configures
    CoroutineBuilder --> Decoder : configures
    Encoder <|.. JacksonEncoder2
    Decoder <|.. JacksonDecoder2
    Encoder <|.. FeignFastjsonEncoder
    Decoder <|.. FeignFastjsonDecoder

    style CoroutineFeign fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style CoroutineBuilder fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style AsyncClient fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style Encoder fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style Decoder fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style JacksonEncoder2 fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style JacksonDecoder2 fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style FeignFastjsonEncoder fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style FeignFastjsonDecoder fill:#E0F2F1,stroke:#80CBC4,color:#00695C
```

### HTTP Transport Layer Options

```mermaid
classDiagram
    class FeignClient {
        <<interface>>
        +target(type, url) T
    }
    class ApacheHttp5Client {
        +execute(request, options) Response
    }
    class AsyncApacheHttp5Client {
        +execute(request, options, callback)
    }
    class VertxHttpClient {
        +execute(request, options) Response
    }
    class AsyncVertxHttpClient {
        +execute(request, options, callback)
    }

    FeignClient --> ApacheHttp5Client : sync transport
    FeignClient --> AsyncApacheHttp5Client : async transport
    FeignClient --> VertxHttpClient : Vert.x sync
    FeignClient --> AsyncVertxHttpClient : Vert.x async

    style FeignClient fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style ApacheHttp5Client fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style AsyncApacheHttp5Client fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style VertxHttpClient fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style AsyncVertxHttpClient fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

### Suspend Function HTTP Request Flow

```mermaid
sequenceDiagram
        participant App as Application
        participant API as Feign interface (suspend fun)
        participant CB as CoroutineFeign.CoroutineBuilder
        participant AC as AsyncClient
        participant Codec as JacksonDecoder2
        participant Server as HTTP Server

    App->>CB: coroutineFeignBuilderOf().client<MyApi>(baseUrl)
    CB-->>App: Return MyApi proxy

    App->>API: suspend fun someApi()
    API->>AC: Async HTTP request
    AC->>Server: HTTP request
    Server-->>AC: HTTP response
    AC-->>API: Response
    API->>Codec: decode(response, type)
    Codec-->>API: Deserialized object
    API-->>App: Return result
```

## Key Features

### 1. Feign Builder DSL

Configure Feign clients easily using a Kotlin DSL.

```kotlin
import io.bluetape4k.feign.*

// DSL style
val api = feignBuilder {
    client(ApacheHttp5Client())
    encoder(JacksonEncoder())
    decoder(JacksonDecoder())
    logLevel(Logger.Level.BASIC)
}.client<GitHubApi>("https://api.github.com")

// Factory function style (basic usage)
val api = feignBuilderOf(
    client = ApacheHttp5Client(),
    encoder = JacksonEncoder(),
    decoder = JacksonDecoder(),
).client<GitHubApi>("https://api.github.com")

// Factory function + additional configuration (using builder lambda)
val api = feignBuilderOf(
    client = ApacheHttp5Client(),
    encoder = JacksonEncoder(),
    decoder = JacksonDecoder(),
) {
    // Apply additional Feign.Builder settings
    retryer(Retryer.Default())
    errorDecoder(MyErrorDecoder())
}.client<GitHubApi>("https://api.github.com")
```

`feignBuilderOf` contract:

- Applies `Encoder.Default()` and `Decoder.Default()` as defaults.
- The `builder: Feign.Builder.() -> Unit = {}` parameter allows inline additional configuration.
- Implemented as an `inline fun` to eliminate lambda call overhead.
- The previously misspelled `feingBuilderOf` has been removed. Use `feignBuilderOf`.

### 2. Coroutines Support

Supports async API calls via `suspend` functions using `CoroutineFeign`.

```kotlin
import io.bluetape4k.feign.coroutines.*

// Create a Coroutine Feign client
val api = coroutineFeignBuilderOf<Unit>(
    asyncClient = AsyncApacheHttp5Client(httpAsyncClientOf()),
    encoder = JacksonEncoder(),
    decoder = JacksonDecoder(),
).client<GitHubApi>("https://api.github.com")

// Call using suspend function
suspend fun getUser(username: String): User {
    return api.getUser(username)
}
```

**Dynamic URL support:**

```kotlin
interface GitHubApi {
    // Pass URI as the first argument to use a dynamic URL
    @RequestLine("GET /users/{username}")
    fun getUser(host: URI, @Param("username") username: String): User
}

val api = feignBuilderOf(client = ApacheHttp5Client()).client<GitHubApi>()
val user = api.getUser(URI("https://api.github.com"), "octocat")
```

`bodyAsReader()` contract:

- Throws `IllegalStateException("Response body is null.")` when the response body is absent.

### 3. HTTP Transport Layer Options

| Client                 | Characteristics               | Use Case                             |
|------------------------|-------------------------------|--------------------------------------|
| ApacheHttp5Client      | Stable, rich configuration    | Synchronous API calls                |
| AsyncApacheHttp5Client | Async, Coroutines integration | High-performance async communication |
| VertxHttpClient        | Event loop-based, lightweight | Vert.x ecosystem integration         |
| AsyncVertxHttpClient   | Vert.x async client           | Vert.x async communication           |

```kotlin
// Vert.x-based Feign client
val api = feignBuilderOf(
    client = VertxHttpClient(vertx),
).client<MyApi>("https://api.example.com")
```

### 4. Custom Encoder/Decoder

Use various serialization libraries such as Jackson and Fastjson2 as Encoder/Decoder.

```kotlin
// Jackson (recommended default)
val builder = feignBuilderOf(
    client = ApacheHttp5Client(),
    encoder = JacksonEncoder(),
    decoder = JacksonDecoder(),
)

// Fastjson2
val builder = feignBuilderOf(
    client = ApacheHttp5Client(),
    encoder = FeignFastjsonEncoder(),
    decoder = FeignFastjsonDecoder(),
)
```

### 5. Resilience4j Integration

Apply Resilience4j patterns such as Circuit Breaker and Retry to Feign clients.

```kotlin
import io.github.resilience4j.feign.Resilience4jFeign

val decoratedBuilder = Resilience4jFeign.builder(feignBuilderOf(
    client = ApacheHttp5Client(),
    encoder = JacksonEncoder(),
    decoder = JacksonDecoder(),
))
```

## Usage Examples

### API Definition

```kotlin
interface HttpbinApi {
    @RequestLine("GET /get")
    fun get(): HttpbinResponse

    @RequestLine("POST /post")
    @Headers("Content-Type: application/json")
    fun post(body: Map<String, Any>): HttpbinResponse

    @RequestLine("GET /status/{code}")
    fun status(@Param("code") code: Int): feign.Response
}

// Coroutine API
interface HttpbinCoroutineApi {
    @RequestLine("GET /get")
    suspend fun get(): HttpbinResponse

    @RequestLine("POST /post")
    @Headers("Content-Type: application/json")
    suspend fun post(body: Map<String, Any>): HttpbinResponse
}
```

## Module Structure

```
io.bluetape4k.feign
├── FeignBuilderSupport.kt           # Feign Builder DSL and factory functions
├── FeignRequestSupport.kt           # Request utilities
├── FeignResponseSupport.kt          # Response utilities
├── clients/                         # HTTP transport layer
│   └── vertx/                       # Vert.x-based clients
│       ├── VertxHttpClient.kt       # Sync Vert.x client
│       ├── AsyncVertxHttpClient.kt  # Async Vert.x client
│       └── VertxFeignSupport.kt     # Vert.x utilities
├── codec/                           # Encoder/Decoder
│   ├── JacksonEncoder2.kt           # Enhanced Jackson Encoder
│   ├── JacksonDecoder2.kt           # Enhanced Jackson Decoder
│   ├── FeignFastjsonEncoder.kt      # Fastjson2 Encoder
│   └── FeignFastjsonDecoder.kt      # Fastjson2 Decoder
└── coroutines/                      # Coroutines support
    └── FeignCoroutineBuilderSupport.kt  # CoroutineFeign Builder DSL
```

## Dependencies

```kotlin
dependencies {
    implementation(project(":bluetape4k-feign"))

    // Optional dependencies
    implementation("io.github.openfeign:feign-jackson")      // Jackson Encoder/Decoder
    implementation("io.github.openfeign:feign-hc5")          // Apache HC5 client
    implementation("io.github.resilience4j:resilience4j-feign") // Resilience4j integration
}
```

## Testing

```bash
# Run Feign module tests
./gradlew :bluetape4k-feign:test
```

## References

- [OpenFeign/feign](https://github.com/OpenFeign/feign)
- [Feign Kotlin](https://github.com/OpenFeign/feign/tree/master/kotlin)
- [Apache HttpComponents 5](https://hc.apache.org/httpcomponents-client-5.4.x/)
- [Resilience4j Feign](https://resilience4j.readme.io/docs/feign)
