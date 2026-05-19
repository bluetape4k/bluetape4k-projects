# Module bluetape4k-spring-boot-core

English | [한국어](./README.ko.md)

A unified module providing common features for Spring Boot 4.x applications.

> This is the versionless Spring Boot 4 implementation.

## Features

### Spring Core Utilities

- BeanFactory extension functions
- Kotlin reified annotation lookup extensions for `AnnotatedElementUtils` and `AnnotationUtils`
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
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-core:${bluetape4kVersion}")
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

### Annotation Lookup Extensions

```kotlin
import io.bluetape4k.spring.beans.findMergedAnnotationOrNull
import io.bluetape4k.spring.beans.hasMergedAnnotation

val mapping = method.findMergedAnnotationOrNull<RequestMapping>()
val hasMapping = method.hasMergedAnnotation<RequestMapping>()
```

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

![Core Class Structure 1](../../docs/images/readme-diagrams/spring-boot-core-diagram-01.svg)

### Spring WebFlux + Coroutines Request Flow

![Spring WebFlux + Coroutines Request Flow 2](../../docs/images/readme-diagrams/spring-boot-core-diagram-02.svg)

### RestClient Coroutines DSL Structure

![RestClient Coroutines DSL Structure 3](../../docs/images/readme-diagrams/spring-boot-core-diagram-03.svg)

### Retrofit2 Integration Structure

![Retrofit2 Integration Structure 4](../../docs/images/readme-diagrams/spring-boot-core-diagram-04.svg)

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot-core:test
```
