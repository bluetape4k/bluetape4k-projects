# Spring Boot Integration

> 마지막 업데이트: 2026-04-07 | 관련 specs: 3개

## 개요

bluetape4k는 Spring Boot 3.x와 4.x를 동시에 지원한다. 두 버전은 완전히 독립된 모듈 셋으로 제공되며, 동일한 패키지 네임스페이스를 공유하여 마이그레이션 비용을 최소화한다.

```
spring-boot3/           spring-boot4/
├── core/               ├── core/
├── cassandra/          ├── cassandra/
├── mongodb/            ├── mongodb/
├── redis/              ├── redis/
├── r2dbc/              ├── r2dbc/
├── exposed-jdbc/       ├── exposed-jdbc/
├── exposed-r2dbc/      ├── exposed-r2dbc/
└── hibernate-lettuce/  └── hibernate-lettuce/
```

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| Spring Boot 4 BOM: `implementation(platform(...))` 방식 사용 | `dependencyManagement { imports }` 사용 시 `kotlinBuildToolsApiClasspath`가 오염되어 KGP 2.3.x 빌드 실패 | 2026-03-20 | spring-boot4-modules-design |
| Fork & Diverge (코드 복사 후 독립 진화) | BOM 충돌 방지, Spring 7 API 분기 대응, Boot 4 전용 기능 자유 추가 | 2026-03-20 | spring-boot4-modules-design |
| 동일 패키지 네임스페이스 (`io.bluetape4k.spring.*`) | Spring Boot 3 → 4 마이그레이션 시 패키지 변경 최소화. 별도 JAR 배포이므로 충돌 없음 | 2026-03-29 | spring-data-exposed-migration-design |
| jackson3 의존 (Spring Boot 4) | Spring Boot 4는 Jackson 3.x가 기본 | 2026-03-20 | spring-boot4-modules-design |
| hibernate-lettuce: 패키지 변경 없음 | Spring Boot 3/4 모두 `io.bluetape4k.spring.boot.autoconfigure.cache.lettuce` 유지 | 2026-03-29 | hibernate-lettuce-migration-design |

## 패턴 & 사용법

### Spring Boot 4 BOM 적용 (필수)

```kotlin
// spring-boot4 모듈의 build.gradle.kts
dependencies {
    // ✅ 올바른 방식
    implementation(platform(Libs.spring_boot4_dependencies))

    // ❌ 금지: KGP 2.3.x 빌드 실패
    // dependencyManagement { imports { mavenBom(Libs.spring_boot4_dependencies) } }
}
```

루트 `build.gradle.kts`의 `spring_boot3_dependencies` BOM은 **전체 프로젝트 공통 의존성** (jackson, slf4j, reactor 등)을 관리하므로 제거하지 않는다. Spring Boot 4 모듈만 per-module 오버라이드 방식을 사용한다.

### Spring Boot 3 vs 4 동시 지원 전략

| 항목 | Spring Boot 3 | Spring Boot 4 |
|------|--------------|--------------|
| 최소 JDK | 17 | 21 |
| HTTP 클라이언트 | WebClient + RestTemplate(deprecated) | RestClient(primary) + WebClient(reactive) |
| Virtual Threads | opt-in | 기본 활성화 |
| Servlet API | jakarta.servlet 6.x | jakarta.servlet 7.x |
| Jackson | 2.x | **3.x** (bluetape4k-jackson3 사용) |
| HibernatePropertiesCustomizer 패키지 | `org.springframework.boot.autoconfigure.orm.jpa` | `org.springframework.boot.hibernate.autoconfigure` |
| BOM 적용 | 없음 (루트 관리) | `implementation(platform(...))` |

### hibernate-lettuce NearCache 자동구성 패턴

**구조**: Caffeine L1 + Redis L2 2계층 NearCache

```yaml
# application.yml
bluetape4k:
  cache:
    lettuce:
      redis-uri: redis://localhost:6379
      default-ttl: 3600
      regions:
        io.bluetape4k.examples.Product:
          ttl: 1800
```

```kotlin
// AutoConfiguration 3개가 자동 등록됨
// 1. LettuceNearCacheHibernateAutoConfiguration  - HibernatePropertiesCustomizer 등록
// 2. LettuceNearCacheMetricsAutoConfiguration     - Micrometer Gauge 등록
// 3. LettuceNearCacheActuatorAutoConfiguration    - /actuator/nearcache endpoint
```

Spring Boot 4에서 `HibernatePropertiesCustomizer`는 `spring-boot-hibernate` 모듈로 이동:

```kotlin
// Spring Boot 3
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer

// Spring Boot 4 (별도 의존성 필요)
// compileOnly(Libs.springBoot("hibernate"))
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
```

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일은 Spring Boot 3/4 모두 동일 형식 사용.

### Spring Data Exposed Repository (PartTree / QBE / Page / Sort)

```kotlin
// @EnableExposedJdbcRepositories 활성화
@EnableExposedJdbcRepositories
@Configuration
class ExposedDataConfig

// Repository 선언
interface CustomerRepository : ListCrudRepository<Customer, Long>,
    ListPagingAndSortingRepository<Customer, Long>,
    QueryByExampleExecutor<Customer> {

    // PartTree 쿼리 (메서드명 기반 자동 생성)
    fun findByNameAndEmail(name: String, email: String): List<Customer>
    fun findByAgeGreaterThan(age: Int, pageable: Pageable): Page<Customer>
}
```

AutoConfiguration 등록:

```
# JDBC 모듈
io.bluetape4k.spring.data.exposed.jdbc.config.ExposedSpringDataAutoConfiguration

# R2DBC 모듈
io.bluetape4k.spring.data.exposed.r2dbc.config.ExposedR2dbcSpringDataAutoConfiguration
```

### exposed-jdbc vs exposed-r2dbc 선택 기준

| 기준 | exposed-jdbc (Spring Data) | exposed-r2dbc (Spring Data) |
|------|---------------------------|-----------------------------|
| 실행 모델 | Virtual Thread / Blocking | 완전 비동기 (suspend/Flow) |
| Spring MVC 호환 | ✅ | ❌ (WebFlux만) |
| Spring WebFlux 호환 | ✅ (VT 기반) | ✅ |
| PartTree 쿼리 | ✅ | ✅ |
| QBE (QueryByExample) | ✅ | ❌ (미지원) |
| Transaction Manager | SpringTransactionManager (Spring 6/7) | R2DBC TransactionManager |
| Exposed Spring 의존성 | `exposed_spring_transaction` (Boot3) / `exposed_spring7_transaction` (Boot4) | JDBC 모듈 전이 의존 |

**Boot 3 vs Boot 4 Exposed Transaction 의존성**:

```kotlin
// Spring Boot 3 (Spring 6)
api(Libs.exposed_spring_transaction)

// Spring Boot 4 (Spring 7)
api(Libs.exposed_spring7_transaction)
```

### RestClient Coroutines DSL (Spring Boot 4 전용)

```kotlin
// suspend GET
val user: User = restClient.suspendGet("/users/1")

// suspend POST
val created: User = restClient.suspendPost("/users", body = newUser)

// RestClient DSL 빌더
val client = restClientOf("http://api.example.com") {
    defaultHeader("Authorization", "Bearer $token")
}
```

### testImplementation extendsFrom 패턴

모든 Spring Boot 모듈은 `compileOnly` 의존성이 테스트에서도 보이도록 다음 설정을 사용한다:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}
```

## 선택하지 않은 방식 / 트레이드오프

| 방식 | 채택하지 않은 이유 |
|------|-----------------|
| 공통 `shared` 모듈 추출 | Boot 3/4 BOM이 같은 좌표의 다른 버전을 강제 → 버전 충돌 발생. `io/jackson2` vs `io/jackson3` 분리 패턴과 동일 |
| `dependencyManagement { imports }` | KGP 2.3.x의 `kotlinBuildToolsApiClasspath` 오염 → 빌드 실패 |
| Spring Boot 4 모듈에서 `bootJar` 플러그인 | 라이브러리 모듈이므로 불필요. `dependencyManagement` 플러그인 + BOM만 사용 |
| `spring.factories` | Spring Boot 3.x/4.x 모두 `AutoConfiguration.imports` 사용 (spring.factories deprecated) |

## 관련 페이지

- [module-decisions.md](module-decisions.md) — includeModules 자동 등록 구조
- [database-dialects.md](database-dialects.md) — exposed-jdbc, exposed-r2dbc 패턴
- [infrastructure-patterns.md](infrastructure-patterns.md) — hibernate-lettuce NearCache 구조
