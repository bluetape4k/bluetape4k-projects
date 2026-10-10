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
- Dedicated `WebClient` resource configuration through `AbstractWebClientConfig`

### RestClient Coroutines DSL

- `RestClient` coroutine extensions (`suspendGet`, `suspendPost`, `suspendPut`, `suspendPatch`, `suspendDelete`)

### Spring Boot Observability Helpers

- `ObservationRegistry.observeSpring` for service, HTTP handler, and event handler code paths
- `ObservationRegistry.observeSpringSuspending` for coroutine-aware observation scope propagation
- Low-cardinality and high-cardinality Micrometer key value grouping through `SpringObservationKeyValues`
- Prometheus and OpenTelemetry export remain application-owned Spring Boot Actuator configuration

### MDC Task Decorator

- `MdcTaskDecorator` propagates an immutable caller MDC snapshot across reusable Spring workers
- Empty caller context clears worker MDC for the task; the worker's previous complete map is restored after success or failure
- Executor creation, shutdown, and Spring bean lifecycle remain application-owned

### Test Utilities

- Integration test support based on Spring Boot Test
- `WebTestClient` test extensions (`httpGet`, `httpPost`, etc.)
- Testcontainers integration

## Diagrams

### Spring Boot Core Capability Map

![Spring Boot Core Capability Map diagram](../../docs/images/readme-diagrams/spring-boot-core-diagram-01.png)

### Spring WebFlux + Coroutines Request Flow

![Spring WebFlux + Coroutines Request Flow diagram](../../docs/images/readme-diagrams/spring-boot-core-diagram-02.png)

### RestClient Coroutines DSL Structure

![RestClient Coroutines DSL Structure diagram](../../docs/images/readme-diagrams/spring-boot-core-diagram-03.png)

### WebClient Dedicated Resource Configuration

![WebClient Dedicated Resource Configuration diagram](../../docs/images/readme-diagrams/spring-boot-core-diagram-04.png)

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

### Reified Spring Extensions

These optional Kotlin extensions supply Spring's `Class<T>` argument through a reified type parameter and delegate to the corresponding Spring API. Existing Java-style class-token calls remain available. The examples assume the receiver objects and application types are supplied by your Spring application or tests.

```kotlin
import io.bluetape4k.spring.boot.bind
import io.bluetape4k.spring.boot.bindOrCreate
import io.bluetape4k.spring.boot.get
import io.bluetape4k.spring.boot.getOrElse
import io.bluetape4k.spring.boot.getOrElseSupply
import io.bluetape4k.spring.boot.getOrElseThrow
import io.bluetape4k.spring.boot.isRegistered
import io.bluetape4k.spring.core.io.support.load
import io.bluetape4k.spring.context.annotation.register
import io.bluetape4k.spring.beans.getMergedRepeatableAnnotations
import io.bluetape4k.spring.beans.findAllMergedAnnotations
import io.bluetape4k.spring.core.typeDescriptorOf
import io.bluetape4k.spring.core.annotationMetadataOf
import io.bluetape4k.spring.expression.getValueAs
import io.bluetape4k.spring.tests.withUserConfiguration
import io.bluetape4k.spring.tests.withBean
import io.bluetape4k.spring.tests.autoConfigurationsOf
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.core.io.support.SpringFactoriesLoader
import java.util.function.Supplier
```

```kotlin
// Binder: 기본 호출, handler 호출, nullable handler, bindOrCreate 두 형태
binder.bind<ServerProperties>("server")
binder.bind<ServerProperties>("server", bindHandler)
binder.bind<ServerProperties>("server", bindHandler = null)
binder.bindOrCreate<ServerProperties>("server")
binder.bindOrCreate<ServerProperties>("server", bindHandler)

// BootstrapContext: 조회, 기본값, 지연 기본값, 지연 예외, 등록 확인
val defaultSupplier = Supplier<ServerProperties?> { ServerProperties() }
val exceptionSupplier = Supplier<Throwable> { IllegalStateException("missing") }
bootstrapContext.get<ServerProperties>()
bootstrapContext.getOrElse<ServerProperties>(ServerProperties())
bootstrapContext.getOrElseSupply<ServerProperties>(defaultSupplier)
bootstrapContext.getOrElseThrow<ServerProperties>(exceptionSupplier)
bootstrapContext.isRegistered<ServerProperties>()

// SpringFactoriesLoader: 생략, resolver, failure handler, 둘 다, 명시적 null
val resolver = SpringFactoriesLoader.ArgumentResolver.from { _ -> null }
val failureHandler = SpringFactoriesLoader.FailureHandler { _, _, _ -> }
loader.load<TestFactory>()
loader.load<TestFactory>(argumentResolver = resolver)
loader.load<TestFactory>(failureHandler = failureHandler)
loader.load<TestFactory>(argumentResolver = resolver, failureHandler = failureHandler)
loader.load<TestFactory>(argumentResolver = null)
loader.load<TestFactory>(failureHandler = null)

// Context registry와 repeatable annotation 조회 의미
registry.register<SampleConfiguration>()
annotationElement.getMergedRepeatableAnnotations<Marker>()
annotationElement.findAllMergedAnnotations<Marker>() // existing find semantics

// Core type metadata와 SpEL의 네 typed overload
typeDescriptorOf<String>()
annotationMetadataOf<SampleConfiguration>()
expression.getValueAs<String>()
expression.getValueAs<String>(rootObject)
expression.getValueAs<String>(evaluationContext)
expression.getValueAs<String>(evaluationContext, rootObject)
expression.getValueAs<String>(rootObject = null)

// 세 concrete runner의 설정, 무명 bean, 이름 있는 bean, nullable 이름
ApplicationContextRunner().withUserConfiguration<SampleRunnerConfiguration>()
ApplicationContextRunner().withBean<ServiceBean> { ServiceBean() }
ApplicationContextRunner().withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
ApplicationContextRunner().withBean<ServiceBean>(name = null) { ServiceBean() }

WebApplicationContextRunner().withUserConfiguration<SampleRunnerConfiguration>()
WebApplicationContextRunner().withBean<ServiceBean> { ServiceBean() }
WebApplicationContextRunner().withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
WebApplicationContextRunner().withBean<ServiceBean>(name = null) { ServiceBean() }

ReactiveWebApplicationContextRunner().withUserConfiguration<SampleRunnerConfiguration>()
ReactiveWebApplicationContextRunner().withBean<ServiceBean> { ServiceBean() }
ReactiveWebApplicationContextRunner().withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
ReactiveWebApplicationContextRunner().withBean<ServiceBean>(name = null) { ServiceBean() }

// 단일 auto-configuration은 reified helper, 여러 타입은 Spring vararg API
ApplicationContextRunner().withConfiguration(autoConfigurationsOf<SampleAutoConfiguration>())
AutoConfigurations.of(FirstAutoConfiguration::class.java, SecondAutoConfiguration::class.java)
```

`getMergedRepeatableAnnotations<A>()` follows get semantics, while the existing `findAllMergedAnnotations<A>()` searches through the element hierarchy. Choose the function whose search behavior matches the call site. `typeDescriptorOf<T>()` describes the raw Java class and does not retain generic type arguments. `autoConfigurationsOf<T>()` accepts one class; use Spring's `AutoConfigurations.of(Class<?>...)` for multiple auto-configurations. Nullable Java-overload arguments use named Kotlin arguments such as `bindHandler = null`, `argumentResolver = null`, `rootObject = null`, and `name = null`.

The extensions target Spring Boot 4.1.1 and Spring Framework 7.0.9. Add the artifact that supplies each API when it is not already provided by an existing starter. The consumer dependency table under Key Dependency Structure does not add dependencies to this module. Let the Spring Boot BOM or parent manage versions, and do not add an artifact again when your selected starter already provides it.

### RestClient Coroutines DSL

The DSL runs blocking `RestClient` calls with `runInterruptible(Dispatchers.IO)`, so coroutine cancellation can interrupt the waiting client thread when the underlying request factory honors interruption.

```kotlin
import io.bluetape4k.spring.http.*

val restClient = RestClient.create("https://api.example.com")

// Make HTTP requests using suspend functions
val user: User = restClient.suspendGet("/users/1")
val created: User = restClient.suspendPost("/users", newUser, MediaType.APPLICATION_JSON)
val updated: User = restClient.suspendPut("/users/1", updatedUser, MediaType.APPLICATION_JSON)
restClient.suspendDelete("/users/1")
```

### WebClient Extensions

```kotlin
import io.bluetape4k.spring.tests.*

val webClient = WebClient.create("https://api.example.com")

// GET request
val response = webClient.httpGet("/users")
    .bodyToFlux(User::class.java)
    .asFlow()

// POST request
val created = webClient.httpPost("/users", newUser)
    .bodyToMono(User::class.java)
    .awaitSingle()
```

### MDC TaskDecorator

Connect `MdcTaskDecorator` to an executor owned by the application. It captures the caller MDC when `decorate` is called, replaces the worker MDC while the task runs, and restores the worker's previous map in `finally`.

```kotlin
import io.bluetape4k.spring.task.MdcTaskDecorator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration(proxyBeanMethods = false)
class TaskExecutionConfiguration {
    @Bean
    fun applicationTaskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 8
            setTaskDecorator(MdcTaskDecorator())
            initialize()
        }
}
```

The decorator does not create or close the executor and does not register an automatic bean. An empty caller MDC hides stale worker values for the task, and task-local keys are removed when the worker's previous context is restored.

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

### Observing Service, HTTP Handler, and Event Code

Use the `ObservationRegistry` that Spring Boot wires for the application. The helpers only manage Micrometer Observation lifecycle and coroutine scope cleanup; they do not install exporters or mutate global OpenTelemetry SDK state.

```kotlin
import io.bluetape4k.spring.observability.SpringObservationKeyValues
import io.bluetape4k.spring.observability.observeSpring
import io.bluetape4k.spring.observability.observeSpringSuspending
import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues
import io.micrometer.observation.ObservationRegistry
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

class OrderService(
    private val observationRegistry: ObservationRegistry,
) {
    fun load(orderId: String): Order =
        observationRegistry.observeSpring(
            name = "order.service.load",
            keyValues = SpringObservationKeyValues(
                lowCardinality = KeyValues.of(KeyValue.of("component", "order-service")),
            ),
        ) { context ->
            context.addLowCardinalityKeyValue(KeyValue.of("outcome", "success"))
            repository.find(orderId)
        }

    suspend fun handleCreated(event: OrderCreated): Unit =
        observationRegistry.observeSpringSuspending("order.events.consume") { context ->
            context.addLowCardinalityKeyValue(KeyValue.of("event.name", "order.created"))
            eventHandler.handle(event)
        }
}

class OrderHandler(
    private val observationRegistry: ObservationRegistry,
    private val orderService: OrderService,
) {
    suspend fun get(request: ServerRequest): ServerResponse =
        observationRegistry.observeSpringSuspending("order.http.get") { context ->
            context.addLowCardinalityKeyValue(KeyValue.of("http.route", "/orders/{id}"))
            val order = orderService.load(request.pathVariable("id"))
            ServerResponse.ok().bodyValueAndAwait(order)
        }
}
```

Expose Prometheus through Spring Boot Actuator instead of registering a custom endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    prometheus:
      access: read_only
```

Configure tracing/export backends with Spring Boot and Micrometer Tracing properties owned by the application:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
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

| Category                      | Scope            | Description                       |
|-------------------------------|------------------|-----------------------------------|
| `spring-boot-starter-webflux` | `compileOnly`    | Required for WebFlux + Coroutines |
| `bluetape4k-logging`          | `implementation` | MDC TaskDecorator support         |
| `bluetape4k-coroutines`       | `compileOnly`    | Coroutines support                |
| `micrometer-observation`      | `compileOnly`    | Observation helper support        |
| `spring-boot-starter-web`     | `compileOnly`    | Optional servlet support          |
| `resilience4j-*`              | `compileOnly`    | Optional Resilience4j             |

The reified Spring extensions use the following consumer artifacts when they are not already supplied by a starter:

| Artifact | API families | Consumer scope |
|---|---|---|
| `org.springframework.boot:spring-boot` | `Binder`, `BootstrapContext` | `implementation`, or `compileOnly` if supplied at runtime |
| `org.springframework:spring-core` | `SpringFactoriesLoader`, repeatable annotations, `TypeDescriptor`, `AnnotationMetadata` | `implementation`, or `compileOnly` if supplied at runtime |
| `org.springframework:spring-context` | `AnnotationConfigRegistry` | `implementation`, or `compileOnly` if supplied at runtime |
| `org.springframework:spring-expression` | `Expression` | `implementation`, or `compileOnly` if supplied at runtime |
| `org.springframework.boot:spring-boot-test` | Context runner extensions | `testImplementation` |
| `org.springframework.boot:spring-boot-autoconfigure` | `autoConfigurationsOf<T>()` | `implementation`, or `compileOnly` if supplied at runtime |

Use the Spring Boot BOM or parent to align versions. Existing starters may already supply these artifacts; do not declare duplicates. No dependency or version change is required by these README examples.

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot-core:test
```
