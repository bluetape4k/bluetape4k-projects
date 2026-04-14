# Module bluetape4k-spring-boot3

English | [한국어](./README.ko.md)

An integrated module for common functionality based on Spring Boot 3.

> The former `spring/core`, `spring/webflux`, `spring/retrofit2`, and
`spring/tests` modules have been consolidated into this single module.

## Features

### Spring Core Utilities (formerly `spring/core`)

- BeanFactory extension functions
- `ToStringCreator` support utilities
- Spring Boot AutoConfiguration support
- Jakarta Annotation API integration

### Spring WebFlux + Coroutines (formerly `spring/webflux`)

- Coroutines-based WebFlux handler utilities
- `WebTestClient` extension functions
- Reactor ↔ Coroutines conversion support
- Netty-based HTTP server integration

### Retrofit2 Integration (formerly `spring/retrofit2`)

- Spring Boot + Retrofit2 auto-configuration
- OkHttp3 client integration
- Apache HttpClient5 integration
- Coroutines suspend function support (`retrofit2-adapter-java8`)
- Jackson serialization/deserialization converters

### Test Utilities (formerly `spring/tests`)

- Spring Boot Test-based integration test support
- `WebTestClient` test extensions
- Testcontainers integration

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot3:${bluetape4kVersion}")
}
```

Optional service-specific dependencies:

```kotlin
dependencies {
    // For Retrofit2 usage
    implementation("io.github.bluetape4k:bluetape4k-spring-boot3:${bluetape4kVersion}")
    runtimeOnly(Libs.retrofit2)

    // For Resilience4j usage (declared compileOnly, so add at runtime)
    implementation(Libs.resilience4j_all)
}
```

## Key Dependency Structure

| Category                      | Scope         | Description                     |
|-------------------------------|---------------|---------------------------------|
| `spring-boot-starter-webflux` | `api`         | WebFlux + Coroutines (required) |
| `bluetape4k-retrofit2`        | `api`         | Retrofit2 integration           |
| `bluetape4k-coroutines`       | `api`         | Coroutines support              |
| `bluetape4k-netty`            | `api`         | Netty integration               |
| `bluetape4k-micrometer`       | `api`         | Metrics                         |
| `spring-boot-starter-web`     | `compileOnly` | Optional servlet support        |
| `resilience4j-*`              | `compileOnly` | Optional Resilience4j           |

## Usage Examples

## Architecture Diagrams

### Core Component Class Diagram

```mermaid
classDiagram
    class DispatcherHandler {
        +handle(exchange): Mono~Void~
    }
    class WebFluxHandler {
        +handle(): Flow~T~
        +handleSuspend(): T
    }
    class UserService {
        +findAllAsFlow(): Flow~User~
        +findById(id): User
    }
    class UserRepository {
        +findAll(): Flow~User~
        +findById(id): User?
    }
    class Retrofit2Client {
        +create(): ApiClient
    }
    class WebTestClientExt {
        +httpGet(uri): ResponseSpec
        +httpPost(uri, body): ResponseSpec
    }

    DispatcherHandler --> WebFluxHandler
    WebFluxHandler --> UserService
    UserService --> UserRepository
    Retrofit2Client --> WebFluxHandler : inject
    WebTestClientExt --> DispatcherHandler : test

    style DispatcherHandler fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style WebFluxHandler fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style UserService fill:#E0F7FA,stroke:#80DEEA,color:#00695C
    style UserRepository fill:#E0F7FA,stroke:#80DEEA,color:#00695C
    style Retrofit2Client fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style WebTestClientExt fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
```

### Spring WebFlux + Coroutines Request Flow

```mermaid
flowchart LR
    Client["HTTP Client"] --> Netty["Netty HTTP Server"]
    Netty --> WebFlux["Spring WebFlux<br/>DispatcherHandler"]
    WebFlux --> Handler["Coroutines Handler<br/>suspend fun / Flow"]
    Handler --> Service["Service Layer"]
    Service --> DB[("Database / External API")]
    DB --> Service
    Service --> Handler
    Handler --> WebFlux
    WebFlux --> Netty
    Netty --> Client

    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef springStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef dataStyle fill:#F57F17,stroke:#E65100,color:#000000

    class Client extStyle
    class Netty extStyle
    class WebFlux springStyle
    class Handler asyncStyle
    class Service serviceStyle
    class DB dataStyle
```

### Retrofit2 Integration Structure

```mermaid
flowchart TD
    App["Application"] --> RetrofitBean["Retrofit Bean<br/>@Bean retrofit.create<T>()"]
    RetrofitBean --> Retrofit2["Retrofit2"]
    Retrofit2 --> OkHttp["OkHttp3 Client"]
    Retrofit2 --> HttpClient5["Apache HttpClient5"]
    Retrofit2 --> Jackson["Jackson Serialization"]
    Retrofit2 --> CoroutinesAdapter["Coroutines Adapter<br/>suspend function support"]
    OkHttp --> ExternalAPI["External REST API"]
    HttpClient5 --> ExternalAPI

    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef springStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A

    class App serviceStyle
    class RetrofitBean springStyle
    class Retrofit2 springStyle
    class OkHttp extStyle
    class HttpClient5 extStyle
    class Jackson extStyle
    class CoroutinesAdapter asyncStyle
    class ExternalAPI extStyle
```

### WebTestClient Test Structure

```mermaid
flowchart LR
    Test["@SpringBootTest<br/>Integration Test"] --> WTC["WebTestClient"]
    WTC --> Controller["REST Controller<br/>Coroutines"]
    Controller --> Service["Service Layer"]
    TC["Testcontainers<br/>DB / Redis etc."] -.->|"@DynamicPropertySource"| Test

    classDef testStyle fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    classDef springStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0

    class Test testStyle
    class TC testStyle
    class WTC springStyle
    class Controller asyncStyle
    class Service serviceStyle
```

### WebFlux Controller (Coroutines)

```kotlin
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.flow.Flow

@RestController
@RequestMapping("/users")
class UserController(private val service: UserService) {

    @GetMapping
    fun getUsers(): Flow<User> = service.findAllAsFlow()

    @GetMapping("/{id}")
    suspend fun getUser(@PathVariable id: Long): User =
        service.findById(id)
}
```

### Registering a Retrofit2 Client

```kotlin
import retrofit2.Retrofit
import retrofit2.create

@Configuration
class RetrofitConfig {

    @Bean
    fun userApiClient(retrofit: Retrofit): UserApiClient =
        retrofit.create()
}
```

### WebTestClient Test

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest(@Autowired val client: WebTestClient) {

    @Test
    fun `fetch user list`() {
        client.get().uri("/users")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(User::class.java)
            .hasSize(10)
    }
}
```
