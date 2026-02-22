# Module bluetape4k-spring-core

Spring Framework의 기본 지원 기능에 대해 Kotlin 언어로 확장된 기능을 제공합니다.

## 주요 기능

- **PropertyResolver 확장**: 속성 값을 Kotlin 스타일로 조회
- **BeanUtils 확장**: 인스턴스 생성, 메소드 검색, 프로퍼티 복사
- **API 예외 처리**: REST API 용 예외 클래스 및 글로벌 핸들러
- **Jackson 설정**: ObjectMapper 커스터마이징
- **StopWatch 확장**: 실행 시간 측정 유틸리티
- **Model 확장**: MVC Model 속성 추가 헬퍼
- **Profile 어노테이션**: 환경별 프로필 어노테이션

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-spring-core:${version}")
}
```

## 주요 기능 상세

### 1. PropertyResolver 확장

PropertyResolver에서 속성 값을 Kotlin 스타일로 조회합니다.

```kotlin
import io.bluetape4k.spring.core.*

// 기본 조회
val value: String? = propertyResolver["app.name"]
val valueWithDefault: String = propertyResolver["app.name", "default"]

// 타입 변환 조회
val port: Int? = propertyResolver["server.port", Int::class]
val enabled: Boolean = propertyResolver["feature.enabled", Boolean::class, false]

// reified 타입 조회
val timeout: Long? = propertyResolver.getAs<Long>("request.timeout")
val retries: Int = propertyResolver.getAs("max.retries", 3)

// 필수 속성 조회
val dbUrl: String = propertyResolver.getRequiredProperty("database.url", String::class)
val poolSize: Int = propertyResolver.getRequiredPropertyAs<Int>("pool.size")
```

---

### 2. BeanUtils 확장

Spring BeanUtils를 Kotlin 스타일로 사용합니다.

#### 인스턴스 생성

```kotlin
import io.bluetape4k.spring.beans.*

// 기본 인스턴스 생성
val instance = UserService::class.java.instantiateClass()

// 상위 타입으로 캐스팅하여 생성
val service = UserServiceImpl::class.java.instantiateClass(UserService::class.java)

// 생성자 인자와 함께 생성
val user = User::class.java.constructors.first().instantiateClass("John", 25)
```

#### 메소드 검색

```kotlin
// 메소드 찾기
val method = UserService::class.java.findMethod("findById", Long::class.java)
val declaredMethod = UserService::class.java.findDeclaredMethod("internalMethod")

// 최소 파라미터 메소드 찾기
val minimalMethod = UserService::class.java.findMethodWithMinimalParameters("save")

// 시그니처로 메소드 찾기
val method = UserService::class.java.resolveSignature("findById(long)")
```

#### 프로퍼티 관련

```kotlin
// 프로퍼티 디스크립터 조회
val descriptors = User::class.java.getPropertyDescriptors()
val nameDescriptor = User::class.java.getPropertyDescriptor("name")

// 프로퍼티 복사
source.copyProperties(target)
source.copyProperties(target, "id", "createdAt")  // 특정 속성 제외
source.copyProperties(target, User::class.java)   // 특정 타입만 복사
```

---

### 3. API 예외 처리

REST API 용 예외 클래스와 글로벌 예외 핸들러를 제공합니다.

#### 예외 클래스

```kotlin
import io.bluetape4k.spring.rest.exceptions.*

// 400 Bad Request
throw ApiBadRequestException("Invalid parameter")

// 401 Unauthorized
throw ApiUnauthorizedException("Authentication required")

// 403 Forbidden
throw ApiForbiddenException("Access denied")

// 404 Not Found
throw ApiEntityNotFoundException("User not found with id: $id")

// 429 Too Many Requests
throw ApiTooManyRequestsException("Rate limit exceeded")

// 500 Internal Server Error
throw ApiInternalServerErrorException("Unexpected error occurred")

// 503 Service Unavailable
throw ApiServiceUnavailableException("Service temporarily unavailable")
```

#### 예외 핸들러 등록

```kotlin
// ApiExceptionHandler는 @RestControllerAdvice로 자동 등록됩니다
// 다음과 같은 응답을 반환합니다:
{
    "errorCode": null,
    "timestamp": "2024-10-14T10:30:00Z",
    "message": "User not found with id: 123",
    "stackTraces": [...]
}
```

#### 커스텀 에러 응답

```kotlin
import io.bluetape4k.spring.rest.*

val errorResponse = apiErrorResponseEntityOf(
    statusCode = HttpStatus.BAD_REQUEST.value(),
    errorCode = "INVALID_PARAMETER",
    message = "Parameter 'name' must not be empty",
    stackTraces = emptyList()
)
```

---

### 4. Jackson 설정 커스터마이징

Spring Boot의 ObjectMapper를 커스터마이징합니다.

```kotlin
import io.bluetape4k.spring.jackson.*

@Configuration
class JsonConfiguration {

    @Bean
    fun jackson2ObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return jackson2ObjectMapperBuilderCustomizer { builder ->
            // 추가 설정
            builder.timeZone(TimeZone.getTimeZone("Asia/Seoul"))
        }
    }
}
```

#### 기본 설정 내용

- Kotlin 모듈 자동 등록
- `JsonUuidModule` 등록 (UUID 직렬화)
- NULL 값 직렬 제외 (`JsonInclude.Include.NON_NULL`)
- 알 수 없는 프로퍼티 무시
- BigDecimal 평문으로 출력
- 빈 문자열을 NULL로 처리

---

### 5. StopWatch 확장

Spring StopWatch를 이용한 실행 시간 측정 유틸리티입니다.

#### withStopWatch

```kotlin
import io.bluetape4k.spring.util.*

// 블록 실행 시간 측정
val stopWatch = withStopWatch("dataProcessing") {
    processData()
    saveToDatabase()
}
println(stopWatch.prettyPrint())
```

#### 코루틴 지원

```kotlin
// suspend 함수 실행 시간 측정
val stopWatch = withSuspendStopWatch("asyncOperation") {
    delay(100)
    fetchRemoteData()
}
```

#### 태스크 단위 측정

```kotlin
val stopWatch = StopWatch("multiTask")

// 개별 태스크 측정
val result1 = stopWatch.task("task1") {
    performTask1()
}
val result2 = stopWatch.task("task2") {
    performTask2()
}

// suspend 태스크 측정
val result3 = stopWatch.suspendTask("asyncTask") {
    performAsyncTask()
}

println(stopWatch.prettyPrint())
```

---

### 6. Model 확장

MVC Model에 속성을 추가하는 헬퍼 함수입니다.

```kotlin
import io.bluetape4k.spring.ui.*

@Controller
class UserController {

    @GetMapping("/users")
    fun listUsers(model: Model): String {
        // vararg로 여러 속성 추가
        model.addAttributes(
            "users" to userService.findAll(),
            "totalCount" to userService.count(),
            "currentPage" to 1
        )

        // 기존 속성과 병합
        model.mergeAttributes(
            "title" to "User List",
            "activeMenu" to "users"
        )

        return "users/list"
    }
}
```

---

### 7. Profile 어노테이션

환경별 프로필을 지정하는 커스텀 어노테이션입니다.

```kotlin
import io.bluetape4k.spring.config.*

@Configuration
@LocalProfile
class LocalConfig {
    // spring.profiles.active=local 일 때만 적용
}

@Configuration
@DevelopProfile
class DevelopmentConfig {
    // spring.profiles.active=dev|develop|development 일 때만 적용
}

@Configuration
@ProductionProfile
class ProductionConfig {
    // spring.profiles.active=prod|product|production 일 때만 적용
}
```

#### 제공되는 Profile 어노테이션

| 어노테이션                | 활성 프로필                    |
|----------------------|---------------------------|
| `@LocalProfile`      | local                     |
| `@DevelopProfile`    | dev, develop, development |
| `@FeatureProfile`    | feature                   |
| `@TestProfile`       | test, testing             |
| `@QaProfile`         | qa                        |
| `@StageProfile`      | stage, staging            |
| `@ProductionProfile` | prod, product, production |

---

## 테스트

```bash
./gradlew :spring:core:test
```
