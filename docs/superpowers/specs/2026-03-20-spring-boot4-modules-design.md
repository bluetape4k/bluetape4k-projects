# Spring Boot 4.x 지원 모듈 설계안

> 작성일: 2026-03-20
> 상태: Draft

## 1. 배경 및 목표

Spring Boot 4.0 (Spring Framework 7 기반)이 릴리즈됨에 따라, bluetape4k 프로젝트에서도
Spring Boot 4.x를 지원하는 독립 모듈 셋을 제공해야 한다.

### Spring Boot 4 주요 변경사항

| 항목 | Boot 3.x | Boot 4.x |
|------|----------|----------|
| Spring Framework | 6.x | 7.x |
| 최소 JDK | 17 | 21 |
| HTTP 클라이언트 기본 | WebClient + RestTemplate(deprecated) | **RestClient**(primary) + WebClient(reactive) |
| Virtual Threads | opt-in | **기본 활성화** |
| Servlet API | jakarta.servlet 6.x | jakarta.servlet 7.x |
| Reactor 버전 | 2024.x | 2025.x |
| Observability | Micrometer + Tracing | 통합 강화 |
| RestTemplate | deprecated | **삭제** |
| AOT / GraalVM | 실험적 | 프로덕션 지원 강화 |
| Exposed 연동 | `spring-boot-starter` | `spring-boot4-starter` (별도) |

### 목표

1. spring-boot3 모듈과 **동일 기능 수준** 제공
2. Spring Boot 4 전용 기능 추가 (RestClient DSL 강화, Virtual Thread 통합)
3. **독립 버전 사용** 가능 (boot3, boot4 동시 의존 불가, 각각 독립)
4. 코드 중복 최소화

## 2. 모듈 구조

```
spring-boot4/
├── core/               # Spring Boot 4 기반 핵심 통합 모듈
├── data-redis/         # Spring Data Redis 4.x 직렬화
├── mongodb/            # Spring Data MongoDB Reactive 4.x
├── r2dbc/              # Spring Data R2DBC 4.x
├── cassandra/          # Spring Data Cassandra 4.x
└── cassandra-demo/     # Cassandra 데모 (배포 제외)
```

### 모듈별 기능 요약

#### 2.1 core (`bluetape4k-spring-boot4-core`)

spring-boot3/core와 동일한 기능 영역 + Spring Boot 4 전용 기능:

| 패키지 | 기능 | Boot 4 변경점 |
|--------|------|--------------|
| `beans` | BeanFactory 확장, Annotation 유틸리티 | API 동일 (마이그레이션 불필요) |
| `config` | Profile 지원 | API 동일 |
| `core` | PropertyResolver, ToStringCreator, DataBuffer | DataBuffer 변경 가능성 확인 필요 |
| `data` | ExampleMatcher | API 동일 |
| `jackson` | ObjectMapperBuilder 커스터마이저 | **`bluetape4k-jackson3` 사용** (Spring Boot 4는 Jackson 3.x 기본) |
| `messaging` | MessageBuilder | API 동일 |
| `rest` | API 에러 응답, 예외 핸들러 | `ProblemDetail` 강화 활용 |
| `retrofit2` | Retrofit2 자동 구성 | API 동일 |
| `tests` | **HTTP 클라이언트 DSL** | **RestClient 기본, 신규 DSL 추가** |
| `ui` | Model/ModelMap 확장 | API 동일 |
| `util` | MemberUtils, StopWatch | API 동일 |
| `webflux` | WebClient, Controller, Filter | WebClient 유지 (reactive 전용) |
| **`http`** (신규) | **RestClient 기반 Coroutines HTTP DSL** | **Boot 4 전용 신규 기능** |
| **`virtualthread`** (신규) | **VT 기반 컨트롤러/Executor** | **Boot 4 기본 VT 활용** |

#### 2.2 data-redis (`bluetape4k-spring-boot4-data-redis`)

- spring-boot3/data-redis와 동일 구조
- Spring Data Redis 4.x BOM 적용

#### 2.3 mongodb (`bluetape4k-spring-boot4-mongodb`)

- spring-boot3/mongodb와 동일 구조
- Spring Data MongoDB 5.x BOM 적용

#### 2.4 r2dbc (`bluetape4k-spring-boot4-r2dbc`)

- spring-boot3/r2dbc와 동일 구조
- Spring Data R2DBC 4.x BOM 적용

#### 2.5 cassandra (`bluetape4k-spring-boot4-cassandra`)

- spring-boot3/cassandra와 동일 구조
- Spring Data Cassandra 5.x BOM 적용

## 3. 코드 공유 전략

### 3.1 접근 방식: **복사 후 독립 진화 (Fork & Diverge)**

코드 공유를 위한 공통 모듈 추출은 **하지 않는다**. 이유:

1. **BOM 충돌 방지**: Spring Boot 3 BOM과 4 BOM은 같은 좌표(`org.springframework:spring-*`)의
   서로 다른 버전을 강제한다. 공통 모듈이 어느 쪽 BOM에도 속하면 버전 충돌이 발생한다.
2. **API 분기 가능성**: Spring Framework 7에서 API가 변경되면 공통 모듈이 양쪽 모두 깨진다.
3. **독립 진화**: Boot 4에서만 의미 있는 기능 (예: RestClient 네이티브 Coroutines)을 자유롭게 추가할 수 있다.
4. **프로젝트 관례**: 기존 `bluetape4k-jackson2` / `bluetape4k-jackson3` 도 동일 패턴으로 분리되어 있다.

### 3.2 패키지 네이밍

```
io.bluetape4k.spring4.beans
io.bluetape4k.spring4.config
io.bluetape4k.spring4.core
io.bluetape4k.spring4.http        # 신규: RestClient Coroutines DSL
io.bluetape4k.spring4.rest
io.bluetape4k.spring4.retrofit2
io.bluetape4k.spring4.tests
io.bluetape4k.spring4.webflux
io.bluetape4k.spring4.virtualthread  # 신규
```

- `io.bluetape4k.spring` (boot3) vs `io.bluetape4k.spring4` (boot4) 로 패키지를 분리하여
  같은 클래스패스에 양쪽이 공존해도 충돌하지 않는다.

## 4. Spring Boot 4 전용 신규 기능

### 4.1 RestClient Coroutines HTTP DSL (`http` 패키지)

Spring Boot 4에서 `RestClient`가 기본 HTTP 클라이언트가 되므로, Coroutines 네이티브 suspend DSL을 제공한다.

```kotlin
// RestClientCoroutinesDsl.kt
package io.bluetape4k.spring4.http

/**
 * RestClient 기반 suspend GET 요청을 전송합니다.
 *
 * ```kotlin
 * val user = restClient.suspendGet<User>("/users/1")
 * ```
 */
suspend inline fun <reified T: Any> RestClient.suspendGet(
    uri: String,
    accept: MediaType? = null,
): T = withContext(Dispatchers.IO) {
    httpGet(uri, accept).body(T::class.java)!!
}

/**
 * RestClient 기반 suspend POST 요청을 전송합니다.
 */
suspend inline fun <reified T: Any> RestClient.suspendPost(
    uri: String,
    body: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): T = withContext(Dispatchers.IO) {
    httpPost(uri, body, contentType, accept).body(T::class.java)!!
}

// suspendPut, suspendPatch, suspendDelete ...
```

기존 spring-boot3의 `RestClient.httpGet()` 등은 동기 `ResponseSpec`을 반환했으나,
Boot 4 모듈에서는 **suspend fun으로 직접 역직렬화된 객체를 반환**하는 상위 계층 DSL을 추가한다.

### 4.2 RestClient Builder DSL

```kotlin
// RestClientBuilderDsl.kt
package io.bluetape4k.spring4.http

/**
 * RestClient를 DSL로 구성합니다.
 *
 * ```kotlin
 * val client = restClientOf("http://api.example.com") {
 *     defaultHeader("Authorization", "Bearer $token")
 *     messageConverters { add(jacksonConverter) }
 * }
 * ```
 */
fun restClientOf(
    baseUrl: String,
    configure: RestClient.Builder.() -> Unit = {},
): RestClient =
    RestClient.builder()
        .baseUrl(baseUrl)
        .apply(configure)
        .build()
```

### 4.3 Virtual Thread 통합 (`virtualthread` 패키지)

```kotlin
// VirtualThreadController.kt
package io.bluetape4k.spring4.virtualthread

/**
 * Virtual Thread Executor를 사용하는 컨트롤러 베이스 클래스.
 * Spring Boot 4에서는 기본으로 VT가 활성화되므로,
 * 명시적 VT Executor 구성이 필요한 경우에 사용합니다.
 */
abstract class AbstractVirtualThreadController {
    companion object {
        val virtualThreadExecutor: ExecutorService =
            Executors.newVirtualThreadPerTaskExecutor()
    }
}

/**
 * Virtual Thread 기반 TaskExecutor 자동 구성.
 */
@Configuration
class VirtualThreadAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun virtualThreadTaskExecutor(): AsyncTaskExecutor =
        SimpleAsyncTaskExecutor().apply {
            setVirtualThreads(true)
        }
}
```

### 4.4 Observability 통합 (향후 확장)

Spring Boot 4의 강화된 Observability API를 활용한 자동 메트릭 수집 (core 모듈 내 구현 예정).

## 5. 빌드 설정

### 5.1 BOM 분리 (핵심)

#### 루트 BOM 유지 이유

루트 `build.gradle.kts`의 `spring_boot3_dependencies` BOM은 **spring-boot3 모듈 전용이 아니다**.
전체 프로젝트에서 공통으로 사용하는 jackson, slf4j, logback, reactor 등의 버전도 이 BOM이 관리한다.
루트에서 제거하면 비spring 모듈들의 의존성 버전 관리가 깨지므로 **루트에 유지**한다.

> spring-boot3 모듈들도 루트 BOM에 의존하며, 각 서브모듈에 별도 선언이 없다.
> 이 관례를 유지하고, **boot4 모듈만 per-module 오버라이드** 방식을 사용한다.

#### spring-boot4 모듈의 BOM 오버라이드

각 spring-boot4 서브모듈의 `build.gradle.kts`에서 Boot 4 BOM을 **추가 선언**한다.
`dependencyManagement` 블록은 **나중에 선언된 BOM이 우선**하므로, 루트 Boot 3 BOM을 자연스럽게 오버라이드한다.

```kotlin
// spring-boot4/core/build.gradle.kts
dependencyManagement {
    imports {
        mavenBom(Libs.spring_boot4_dependencies)  // Boot 3 BOM 오버라이드
    }
}
```

### 5.2 settings.gradle.kts

이미 `includeModules("spring-boot4", withBaseDir = true)` 가 선언되어 있어 변경 불필요.
모듈명 자동 생성 규칙: `bluetape4k-spring-boot4-{dirname}`

### 5.3 Libs.kt 변경

이미 `spring_boot4` 버전(4.0.3)과 `spring_boot4_dependencies` BOM이 정의되어 있다.
추가 필요 항목:

```kotlin
// Libs.kt 에 추가
fun springBoot4Starter(module: String) =
    "org.springframework.boot:spring-boot-starter-$module:${Versions.spring_boot4}"

// Spring Framework 7 전용 함수 (필요 시)
fun spring7(module: String) = "org.springframework:spring-$module"  // BOM에서 버전 관리
```

> `springBoot4Starter()` 는 BOM 오버라이드 시 불필요할 수 있으나,
> 명시적 버전 고정이 필요한 상황에서 유용하다.

### 5.4 Gradle Plugin

spring-boot4 모듈에서는 `org.springframework.boot` Gradle 플러그인 4.x를 사용하지 **않는다**
(라이브러리 모듈이므로). `dependencyManagement` 플러그인 + BOM import만 사용한다.

## 6. 의존성 그래프

```
bluetape4k-spring-boot4-core
├── api: bluetape4k-core
├── api: bluetape4k-coroutines
├── api: bluetape4k-netty
├── api: bluetape4k-retrofit2
├── api: bluetape4k-micrometer
├── api: bluetape4k-jackson3          # Spring Boot 4는 Jackson 3.x 기본 사용
├── api: spring-boot-starter-webflux (4.x BOM)
├── impl: kotlinx-coroutines-reactor
├── impl: reactor-core
├── compileOnly: spring-boot-starter-web (4.x BOM)
└── compileOnly: spring-boot-starter-test (4.x BOM)

bluetape4k-spring-boot4-data-redis
├── api: bluetape4k-core
├── api: bluetape4k-io
└── api: spring-boot-starter-data-redis (4.x BOM)

bluetape4k-spring-boot4-mongodb
├── api: bluetape4k-spring-boot4-core
├── api: bluetape4k-coroutines
└── api: spring-boot-starter-data-mongodb-reactive (4.x BOM)

bluetape4k-spring-boot4-r2dbc
├── api: bluetape4k-r2dbc
├── api: bluetape4k-spring-boot4-core
└── api: spring-boot-starter-data-r2dbc (4.x BOM)

bluetape4k-spring-boot4-cassandra
├── api: bluetape4k-cassandra
├── api: bluetape4k-spring-boot4-core
└── api: spring-boot-starter-data-cassandra (4.x BOM)
```

## 7. 마이그레이션 가이드

| spring-boot3 | spring-boot4 | 변경 필요 |
|--------------|--------------|-----------|
| `bluetape4k-spring-boot3-core` | `bluetape4k-spring-boot4-core` | 의존성 교체 |
| `bluetape4k-jackson2` | `bluetape4k-jackson3` | Spring Boot 4 Jackson 3.x 기본 |
| `io.bluetape4k.spring.*` | `io.bluetape4k.spring4.*` | import 변경 |
| `WebClient.httpGet()` | `WebClient.httpGet()` (동일 API) | 패키지만 변경 |
| `RestClient.httpGet()` | `RestClient.httpGet()` + `suspendGet()` | 신규 suspend DSL 사용 가능 |
| N/A | `restClientOf()` DSL | 신규 기능 |
| N/A | `VirtualThreadAutoConfiguration` | 신규 기능 |

## 8. 구현 우선순위

| 순서 | 모듈 | 복잡도 | 비고 |
|------|------|--------|------|
| 1 | core | **high** | 가장 큰 모듈. BOM 오버라이드 + 신규 기능 설계 포함 |
| 2 | data-redis | **low** | boot3 복사 + BOM 변경 |
| 3 | r2dbc | **low** | boot3 복사 + BOM 변경 + core 의존성 변경 |
| 4 | mongodb | **medium** | boot3 복사 + BOM 변경 + MongoDB driver 호환성 확인 |
| 5 | cassandra | **medium** | boot3 복사 + BOM 변경 + Cassandra driver 호환성 확인 |
| 6 | cassandra-demo | **low** | 데모, 배포 제외 |

## 9. 리스크 및 고려사항

### 9.1 Spring Boot 4.0 안정성

- Spring Boot 4.0.3이 GA이긴 하나, 아직 초기 릴리즈 단계.
- 일부 서드파티 라이브러리 (Retrofit2, AsyncHttpClient 등)가 Spring Framework 7과 호환되지 않을 수 있음.
- **대응**: `compileOnly` 의존성으로 선언하고, 호환되지 않는 기능은 제외 또는 래퍼 제공.

### 9.2 Reactor 버전 충돌

- Boot 3의 Reactor 2024.x와 Boot 4의 Reactor 2025.x는 바이너리 호환되지 않을 수 있음.
- **대응**: BOM 오버라이드로 모듈별 정확한 버전 고정.

### 9.3 Exposed Spring Boot 4 Starter

- `Libs.exposed_spring_boot4_starter`가 이미 정의되어 있음.
- data 모듈 (exposed-jdbc, exposed-r2dbc)에서 Boot 4 Starter 전환이 필요함.
- **대응**: spring-boot4 모듈과 별도로 exposed 모듈에서 처리.

### 9.4 테스트 인프라

- `spring-boot-starter-test` 4.x가 JUnit 5 + MockK 조합과 호환되는지 확인 필요.
- Testcontainers 호환성도 검증 필요.

## 10. 향후 확장

- **spring-boot4/actuator**: Actuator 커스텀 엔드포인트 유틸리티
- **spring-boot4/security**: Spring Security 7.x 확장
- **spring-boot4/graphql**: Spring GraphQL 통합
- **spring-boot4/grpc**: gRPC + Spring Boot 4 자동 구성
