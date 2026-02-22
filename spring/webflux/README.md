# Module bluetape4k-spring-webflux

Spring WebFlux를 사용하는 프로젝트에서 사용할 수 있는 Kotlin 확장 기능을 제공합니다.

## 주요 기능

- **WebClient 설정**: 커스텀 이벤트 루프를 사용하는 WebClient 빌더
- **요청 컨텍스트 필터**: 요청 정보를 Reactor Context에 저장/조회
- **리다이렉트 필터**: 루트 경로 리다이렉트를 위한 WebFilter
- **코루틴 컨트롤러 베이스**: CoroutineScope가 포함된 추상 컨트롤러

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-spring-webflux:${version}")
}
```

## 주요 기능 상세

### 1. WebClient 설정

기본 WebClient는 WebFlux 서버의 스레드 풀을 공유합니다. 대량의 외부 API 호출 시 별도의 스레드 풀을 사용하도록 설정합니다.

#### 기본 설정 상속

```kotlin
import io.bluetape4k.spring.webflux.config.AbstractWebClientConfig
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class CustomWebClientConfig: AbstractWebClientConfig() {

    // WebClient 전용 이벤트 루프 스레드 수 (기본: 4 * CPU 코어)
    override val threadCount: Int = 8

    // 커넥션 타임아웃 (기본: 5000ms)
    override val connectTimeoutMillis: Int = 3000

    // 응답 타임아웃 (기본: 3초)
    override val responseTimeout: Duration = Duration.ofSeconds(10)

    // Codec 메모리 최대 크기 (기본: 16MB)
    override val maxInMemorySize: Int = 32 * 1024 * 1024
}
```

#### 제공되는 빈

| 빈 | 설명 |
|---|------|
| `LoopResources` | 커스텀 이벤트 루프 리소스 |
| `ReactorResourceFactory` | Reactor 리소스 팩토리 |
| `ReactorClientHttpConnector` | HTTP 커넥터 (SSL 설정 포함) |
| `ExchangeStrategies` | Codec 설정 (메모리 크기) |
| `WebClient` | 최종 WebClient 빈 |

#### SSL 설정

기본적으로 모든 인증서를 신뢰합니다. 보안 정책을 변경하려면 `sslContext()`를 오버라이드하세요.

```kotlin
@Configuration
class SecureWebClientConfig: AbstractWebClientConfig() {

    override fun sslContext(): SslContext {
        return SslContextBuilder
            .forClient()
            .trustManager(CustomTrustManager())  // 커스텀 TrustManager 사용
            .build()
    }
}
```

---

### 2. 요청 컨텍스트 필터

HTTP 요청 정보를 Reactor Context에 저장하여, 비동기 흐름 어디서든 접근할 수 있게 합니다.

#### 필터 등록

```kotlin
import io.bluetape4k.spring.webflux.filter.HttpRequestCapturer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebFilterConfig {

    @Bean
    fun httpRequestCapturer(): HttpRequestCapturer {
        return HttpRequestCapturer()
    }
}
```

#### 요청 정보 조회

```kotlin
import io.bluetape4k.spring.webflux.filter.HttpRequestHolder

@Service
class AuditService {

    suspend fun logRequest() {
        // Reactor Context에서 요청 정보 조회
        val request = HttpRequestHolder.getHttpRequest().awaitSingleOrNull()

        request?.let {
            println("Request Path: ${it.path}")
            println("Request Method: ${it.method}")
            println("Request Headers: ${it.headers}")
            println("Query Params: ${it.queryParams}")
        }
    }
}
```

#### 코루틴에서 사용

```kotlin
@RestController
class UserController {

    @GetMapping("/users/{id}")
    suspend fun getUser(@PathVariable id: Long): User {
        // 요청 정보 로깅
        val request = HttpRequestHolder.getHttpRequest().awaitSingleOrNull()
        log.info { "Request from: ${request?.remoteAddress}" }

        return userService.findById(id)
    }
}
```

---

### 3. 리다이렉트 필터

특정 경로로 요청을 리다이렉트하는 WebFilter입니다. 주로 루트 경로를 Swagger UI로 리다이렉트할 때 사용합니다.

#### Swagger 리다이렉트

```kotlin
import io.bluetape4k.spring.webflux.filter.AbstractRedirectWebFilter
import org.springframework.stereotype.Component

@Component
class RedirectToSwaggerWebFilter: AbstractRedirectWebFilter(
    redirectPath = "/swagger-ui.html",
    requestPath = "/"
) {
    companion object {
        const val ROOT_PATH = "/"
        const val SWAGGER_PATH = "/swagger-ui.html"
    }
}
```

#### 커스텀 리다이렉트

```kotlin
@Component
class RedirectToApiDocsWebFilter: AbstractRedirectWebFilter(
    redirectPath = "/v3/api-docs",
    requestPath = "/api-docs"
)
```

---

### 4. 코루틴 컨트롤러 베이스

CoroutineScope가 내장된 추상 컨트롤러를 제공합니다.

#### AbstractCoroutineIOController

IO 작업에 적합한 `Dispatchers.IO`를 사용합니다.

```kotlin
import io.bluetape4k.spring.webflux.controller.AbstractCoroutineIOController
import kotlinx.coroutines.launch

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
): AbstractCoroutineIOController() {

    @GetMapping("/{id}")
    suspend fun getUser(@PathVariable id: Long): User {
        // Dispatchers.IO 환경에서 실행
        return userService.findById(id)
    }

    @PostMapping("/batch")
    suspend fun batchCreate(@RequestBody requests: List<CreateUserRequest>): BatchResult {
        var success = 0
        var failed = 0

        // 컨트롤러의 CoroutineScope 사용
        requests.map { request ->
            async {
                try {
                    userService.create(request)
                    success++
                } catch (e: Exception) {
                    failed++
                }
            }
        }.awaitAll()

        return BatchResult(success, failed)
    }
}
```

#### AbstractCoroutineDefaultController

CPU 집약적 작업에 적합한 `Dispatchers.Default`를 사용합니다.

```kotlin
import io.bluetape4k.spring.webflux.controller.AbstractCoroutineDefaultController

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService
): AbstractCoroutineDefaultController() {

    @GetMapping("/generate")
    suspend fun generateReport(): Report {
        // Dispatchers.Default 환경에서 실행
        return reportService.generateHeavyReport()
    }
}
```

#### AbstractCoroutineVTController

Java 21+ Virtual Thread를 사용합니다.

```kotlin
import io.bluetape4k.spring.webflux.controller.AbstractCoroutineVTController

@RestController
@RequestMapping("/api/external")
class ExternalApiController(
    private val externalService: ExternalService
): AbstractCoroutineVTController() {

    @GetMapping("/fetch")
    suspend fun fetchData(): ExternalData {
        // Virtual Thread 환경에서 실행
        // 블로킹 I/O 작업에 적합
        return externalService.fetchFromExternalApi()
    }
}
```

#### 컨트롤러 타입 선택 가이드

| 컨트롤러                                 | Dispatcher          | 적합한 작업             |
|--------------------------------------|---------------------|--------------------|
| `AbstractCoroutineIOController`      | Dispatchers.IO      | 파일 I/O, DB, 네트워크   |
| `AbstractCoroutineDefaultController` | Dispatchers.Default | CPU 연산, 계산         |
| `AbstractCoroutineVTController`      | Dispatchers.VT      | 블로킹 I/O (Java 21+) |

---

### 5. 전체 설정 예시

```kotlin
@Configuration
class WebFluxConfig {

    // WebClient 설정
    @Bean
    fun customWebClientConfig(): CustomWebClientConfig {
        return object: CustomWebClientConfig() {
            override val threadCount = 8
            override val responseTimeout = Duration.ofSeconds(10)
        }
    }

    // 요청 캡처 필터
    @Bean
    fun httpRequestCapturer(): HttpRequestCapturer {
        return HttpRequestCapturer()
    }

    // Swagger 리다이렉트
    @Bean
    fun redirectToSwaggerFilter(): RedirectToSwaggerWebFilter {
        return RedirectToSwaggerWebFilter()
    }
}
```

```kotlin
@RestController
@RequestMapping("/api")
class ApiController(
    private val externalService: ExternalService,
    private val auditService: AuditService
): AbstractCoroutineIOController() {

    @GetMapping("/data")
    suspend fun getData(): Data {
        // 요청 정보 로깅
        auditService.logRequest()

        // 외부 API 호출
        return externalService.fetch()
    }

    @GetMapping("/parallel")
    suspend fun getParallel(): List<Data> {
        // 병렬 처리
        return (1..10).map { id ->
            async { externalService.fetchById(id) }
        }.awaitAll()
    }
}
```

---

## 테스트

```bash
./gradlew :spring:webflux:test
```

## 참고

- [Spring WebFlux Reference](https://docs.spring.io/spring-framework/reference/web-reactive.html)
- [Kotlin Coroutines Support](https://docs.spring.io/spring-framework/reference/languages/kotlin/coroutines.html)
- [Project Reactor](https://projectreactor.io/)
