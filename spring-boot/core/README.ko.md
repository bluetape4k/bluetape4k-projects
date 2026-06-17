# Module bluetape4k-spring-boot-core

[English](./README.md) | 한국어

Spring Boot 4.x 기반 공통 기능 통합 모듈입니다.

> Spring Boot 4 기반 versionless 표준 구현입니다.

## 제공 기능

### Spring Core 유틸리티

- BeanFactory 확장 함수
- `AnnotatedElementUtils`와 `AnnotationUtils`를 위한 Kotlin reified 애너테이션 조회 확장
- Spring Boot AutoConfiguration 지원
- Jakarta Annotation API 통합

### Spring WebFlux + Coroutines

- Coroutines 기반 WebFlux 핸들러 유틸리티
- `WebClient` 확장 함수 (`httpGet`, `httpPost`, `httpPut`, `httpPatch`, `httpDelete`)
- `WebTestClient` 확장 함수
- Reactor ↔ Coroutines 변환 지원
- `AbstractWebClientConfig` 기반 전용 `WebClient` 리소스 구성

### RestClient Coroutines DSL

- `RestClient` 코루틴 확장 (`suspendGet`, `suspendPost`, `suspendPut`, `suspendPatch`, `suspendDelete`)

### Spring Boot Observability 헬퍼

- 서비스, HTTP 핸들러, 이벤트 핸들러 코드 경로를 위한 `ObservationRegistry.observeSpring`
- 코루틴 Observation scope 전파와 정리를 위한 `ObservationRegistry.observeSpringSuspending`
- `SpringObservationKeyValues`를 통한 Micrometer low-cardinality/high-cardinality key value 그룹화
- Prometheus와 OpenTelemetry export는 애플리케이션 소유의 Spring Boot Actuator 설정으로 유지

### 테스트 유틸리티

- Spring Boot Test 기반 통합 테스트 지원
- `WebTestClient` 테스트 확장 (`httpGet`, `httpPost` 등)
- Testcontainers 통합

## 다이어그램

### Spring Boot Core 기능 맵

![Spring Boot Core 기능 맵 다이어그램](../../docs/images/readme-diagrams/spring-boot-core-diagram-01.png)

### Spring WebFlux + Coroutines 요청 흐름

![Spring WebFlux + Coroutines diagram](../../docs/images/readme-diagrams/spring-boot-core-diagram-02.png)

### RestClient Coroutines DSL 구조

![RestClient Coroutines DSL diagram](../../docs/images/readme-diagrams/spring-boot-core-diagram-03.png)

### WebClient 전용 리소스 구성

![WebClient 전용 리소스 구성 다이어그램](../../docs/images/readme-diagrams/spring-boot-core-diagram-04.png)

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-core:${bluetape4kVersion}")
}
```

## BOM 적용 주의사항

Spring Boot 4 BOM은 반드시 `implementation(platform(...))` 방식으로 적용해야 합니다.
`dependencyManagement { imports { mavenBom() } }` 방식은 Kotlin Gradle Plugin과 충돌합니다.

```kotlin
// ✅ 올바른 방식
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.x.x"))
}

// ❌ 잘못된 방식 (KGP 빌드 실패 유발)
dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.x.x") }
}
```

## 사용 예시

### 애너테이션 조회 확장

```kotlin
import io.bluetape4k.spring.beans.findMergedAnnotationOrNull
import io.bluetape4k.spring.beans.hasMergedAnnotation

val mapping = method.findMergedAnnotationOrNull<RequestMapping>()
val hasMapping = method.hasMergedAnnotation<RequestMapping>()
```

### RestClient Coroutines DSL

이 DSL은 blocking `RestClient` 호출을 `runInterruptible(Dispatchers.IO)` 로 실행한다.
따라서 요청 factory가 thread interrupt를 존중하면 coroutine cancellation 이 대기 중인
client thread 를 interrupt 할 수 있다.

```kotlin
import io.bluetape4k.spring.http.*

val restClient = RestClient.create("https://api.example.com")

// suspend 함수로 HTTP 요청
val user: User = restClient.suspendGet("/users/1")
val created: User = restClient.suspendPost("/users", newUser, MediaType.APPLICATION_JSON)
val updated: User = restClient.suspendPut("/users/1", updatedUser, MediaType.APPLICATION_JSON)
restClient.suspendDelete("/users/1")
```

### WebClient 확장

```kotlin
import io.bluetape4k.spring.tests.*

val webClient = WebClient.create("https://api.example.com")

// GET 요청
val response = webClient.httpGet("/users")
    .retrieve()
    .bodyToFlux(User::class.java)
    .asFlow()

// POST 요청
val created = webClient.httpPost("/users", newUser)
    .retrieve()
    .bodyToMono(User::class.java)
    .awaitSingle()
```

### WebFlux 컨트롤러 (Coroutines)

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

### 서비스, HTTP 핸들러, 이벤트 코드 관측

Spring Boot가 애플리케이션에 주입한 `ObservationRegistry`를 사용합니다. 이 헬퍼는 Micrometer Observation
라이프사이클과 코루틴 scope 정리만 담당하며 exporter를 설치하거나 전역 OpenTelemetry SDK 상태를 바꾸지 않습니다.

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

Prometheus는 custom endpoint를 만들지 않고 Spring Boot Actuator endpoint로 노출합니다.

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

Tracing/export backend는 애플리케이션이 소유한 Spring Boot와 Micrometer Tracing 설정으로 연결합니다.

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

### WebTestClient 테스트

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest(@Autowired val client: WebTestClient) {

    @Test
    fun `사용자 목록 조회`() = runTest {
        client.httpGet("/users")
            .expectStatus().isOk
            .expectBodyList(User::class.java)
            .hasSize(10)
    }
}
```

## 주요 의존성 구조

| 범주                            | 의존 방식         | 설명                      |
|-------------------------------|---------------|-------------------------|
| `spring-boot-starter-webflux` | `compileOnly` | WebFlux + Coroutines 필수 |
| `bluetape4k-coroutines`       | `compileOnly` | Coroutines 지원           |
| `micrometer-observation`      | `compileOnly` | Observation 헬퍼 지원      |
| `spring-boot-starter-web`     | `compileOnly` | 선택적 서블릿 지원              |
| `resilience4j-*`              | `compileOnly` | 선택적 Resilience4j        |

## 빌드 및 테스트

```bash
./gradlew :bluetape4k-spring-boot-core:test
```
