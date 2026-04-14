# Module bluetape4k-spring-boot4-core

English | [한국어](./README.ko.md)

A unified module providing common features for Spring Boot 4.x applications.

> Provides the same functionality as the Spring Boot 3 module (
`bluetape4k-spring-boot3`), adapted to the Spring Boot 4.x API. Both modules can be used independently.

## Features

### Spring Core Utilities

- BeanFactory extension functions
- Spring Boot AutoConfiguration support
- Jakarta Annotation API integration

### Spring WebFlux + Coroutines

- Coroutines-based WebFlux handler utilities
- `WebClient` extension functions (`httpGet`, `httpPost`, `httpPut`, `httpPatch`, `httpDelete`)
- `WebTestClient` extension functions
- Reactor ↔ Coroutines conversion support

### RestClient Coroutines DSL

- `RestClient` coroutine extensions (`suspendGet`, `suspendPost`, `suspendPut`, `suspendPatch`, `suspendDelete`)

### Jackson 2 Customizer

- `jacksonObjectMapperBuilderCustomizer` DSL
- Auto-registration of KotlinModule and JsonUuidModule
- Default serialization/deserialization configuration

> **Note**: Spring Boot 4 uses Jackson 2 (`com.fasterxml.jackson.*`) internally. Jackson 3 is not supported.

### Retrofit2 Integration

- Spring Boot + Retrofit2 auto-configuration
- OkHttp3 client integration
- Apache HttpClient5 integration
- Coroutines suspend function support

### Test Utilities

- Integration test support based on Spring Boot Test
- `WebTestClient` test extensions (`httpGet`, `httpPost`, etc.)
- Testcontainers integration

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot4-core:${bluetape4kVersion}")
}
```

## BOM Configuration Notes

The Spring Boot 4 BOM must be applied using `implementation(platform(...))`. Using
`dependencyManagement { imports { mavenBom() } }` conflicts with the Kotlin Gradle Plugin.

```kotlin
// ✅ Correct approach
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.x.x"))
}

// ❌ Incorrect approach (causes KGP build failures)
dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.x.x") }
}
```

## Usage Examples

### RestClient Coroutines DSL

```kotlin
import io.bluetape4k.spring4.http.*

val restClient = RestClient.create("https://api.example.com")

// Make HTTP requests using suspend functions
val user: User = restClient.suspendGet("/users/1")
val created: User = restClient.suspendPost("/users", newUser, MediaType.APPLICATION_JSON)
val updated: User = restClient.suspendPut("/users/1", updatedUser, MediaType.APPLICATION_JSON)
restClient.suspendDelete("/users/1")
```

### WebClient Extensions

```kotlin
import io.bluetape4k.spring4.tests.*

val webClient = WebClient.create("https://api.example.com")

// GET request
val response = webClient.httpGet("/users")
    .retrieve()
    .bodyToFlux(User::class.java)
    .asFlow()

// POST request
val created = webClient.httpPost("/users", newUser)
    .retrieve()
    .bodyToMono(User::class.java)
    .awaitSingle()
```

### WebFlux Controller (Coroutines)

```kotlin
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

### Jackson Customizer

```kotlin
@Configuration
class JacksonConfig {

    @Bean
    fun customizer(): Jackson2ObjectMapperBuilderCustomizer =
        jacksonObjectMapperBuilderCustomizer {
            // Additional customization
            featuresToEnable(SerializationFeature.INDENT_OUTPUT)
        }
}
```

### WebTestClient Test

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest(@Autowired val client: WebTestClient) {

    @Test
    fun `fetch user list`() = runTest {
        client.httpGet("/users")
            .expectStatus().isOk
            .expectBodyList(User::class.java)
            .hasSize(10)
    }
}
```

## Key Dependency Structure

| Category                      | Scope         | Description                       |
|-------------------------------|---------------|-----------------------------------|
| `spring-boot-starter-webflux` | `api`         | Required for WebFlux + Coroutines |
| `bluetape4k-retrofit2`        | `api`         | Retrofit2 integration             |
| `bluetape4k-coroutines`       | `api`         | Coroutines support                |
| `bluetape4k-jackson2`         | `compileOnly` | Jackson 2 support                 |
| `spring-boot-starter-web`     | `compileOnly` | Optional servlet support          |
| `resilience4j-*`              | `compileOnly` | Optional Resilience4j             |

## Architecture Diagrams

### Core Class Structure

```mermaid
classDiagram
    class UserController {
        -service: UserService
        +getUsers(): Flow~User~
        +getUser(id): User
        +createUser(request): ResponseEntity~User~
    }
    class UserService {
        -restClient: RestClient
        +findAllAsFlow(): Flow~User~
        +findById(id): User
        +create(user): User
    }
    class RestClientDsl {
        <<extension>>
        +suspendGet(uri): T
        +suspendPost(uri, body): T
        +suspendPut(uri, body): T
        +suspendDelete(uri)
    }
    class WebTestClientExt {
        <<extension>>
        +httpGet(uri): ResponseSpec
        +httpPost(uri, body): ResponseSpec
    }
    class Retrofit2Config {
        +retrofit(): Retrofit
        +okHttpClient(): OkHttpClient
        +jacksonConverterFactory(): JacksonConverterFactory
    }

    UserController --> UserService
    UserService --> RestClientDsl
    Retrofit2Config --> UserService : inject
    WebTestClientExt --> UserController : test

    style UserController fill:#37474F,stroke:#263238,color:#FFFFFF
    style UserService fill:#00ACC1,stroke:#00838F,color:#FFFFFF
    style RestClientDsl fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style WebTestClientExt fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    style Retrofit2Config fill:#00897B,stroke:#00695C,color:#FFFFFF
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

    classDef clientStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef serverStyle fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    classDef handlerStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef serviceStyle fill:#00ACC1,stroke:#00838F,color:#FFFFFF
    classDef dataStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF

    class Client clientStyle
    class Netty,WebFlux serverStyle
    class Handler handlerStyle
    class Service serviceStyle
    class DB dataStyle
```

### RestClient Coroutines DSL Structure

```mermaid
flowchart TD
    App["Application Code"] --> DSL["RestClient Coroutines DSL<br/>suspendGet / suspendPost<br/>suspendPut / suspendPatch / suspendDelete"]
    DSL --> RestClient["Spring RestClient"]
    RestClient --> HTTP["HTTP Request"]
    HTTP --> ExternalAPI["External REST API"]
    ExternalAPI --> RestClient
    RestClient --> DSL
    DSL --> App

    classDef appStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef dslStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef httpStyle fill:#00897B,stroke:#00695C,color:#FFFFFF
    classDef externalStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF

    class App appStyle
    class DSL dslStyle
    class RestClient,HTTP httpStyle
    class ExternalAPI externalStyle
```

### Retrofit2 Integration Structure

```mermaid
flowchart TD
    App["Application"] --> RetrofitBean["Retrofit Bean<br/>(@Bean retrofit.create<T>())"]
    RetrofitBean --> Retrofit2["Retrofit2"]
    Retrofit2 --> OkHttp["OkHttp3 Client"]
    Retrofit2 --> HttpClient5["Apache HttpClient5"]
    Retrofit2 --> Jackson["Jackson 2 Serialization/Deserialization"]
    Retrofit2 --> CoroutinesAdapter["Coroutines Adapter<br/>(suspend function support)"]
    OkHttp --> ExternalAPI["External REST API"]
    HttpClient5 --> ExternalAPI

    classDef appStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef retrofitStyle fill:#00897B,stroke:#00695C,color:#FFFFFF
    classDef adapterStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef serdeStyle fill:#F57F17,stroke:#E65100,color:#000000
    classDef externalStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF

    class App appStyle
    class RetrofitBean,Retrofit2,OkHttp,HttpClient5 retrofitStyle
    class CoroutinesAdapter adapterStyle
    class Jackson serdeStyle
    class ExternalAPI externalStyle
```

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot4-core:test
```
