# Spring Data Exposed Demo 모듈 이관 설계서

> 작성일: 2026-03-29
> 상태: 초안
> 대상: `bluetape4k-experimental` -> `bluetape4k-projects` 데모 모듈 승격

---

## 1. 개요

### 1.1 목적

`bluetape4k-experimental/examples/`에 있는 Exposed Spring Data 데모 애플리케이션 2개를 `bluetape4k-projects`의 `spring-boot3/`, `spring-boot4/` 하위에 각각 이관한다. 이를 통해:

- `bluetape4k-spring-boot3-exposed-jdbc` / `exposed-r2dbc` 라이브러리 모듈의 **실사용 데모**를 프로젝트 내에서 제공
- Spring Boot 3 / Spring Boot 4 각각에 대한 **검증된 예제** 확보
- experimental 모듈에서 본 프로젝트로의 코드 정리 진행

### 1.2 대상 모듈 (4개 생성)

| # | 소스 (experimental) | 대상 디렉토리 | Gradle 프로젝트명 |
|---|---------------------|--------------|-------------------|
| 1 | `exposed-jdbc-spring-data-mvc-demo` | `spring-boot3/exposed-jdbc-demo` | `bluetape4k-spring-boot3-exposed-jdbc-demo` |
| 2 | `exposed-r2dbc-spring-data-webflux-demo` | `spring-boot3/exposed-r2dbc-demo` | `bluetape4k-spring-boot3-exposed-r2dbc-demo` |
| 3 | (JDBC 소스 기반) | `spring-boot4/exposed-jdbc-demo` | `bluetape4k-spring-boot4-exposed-jdbc-demo` |
| 4 | (R2DBC 소스 기반) | `spring-boot4/exposed-r2dbc-demo` | `bluetape4k-spring-boot4-exposed-r2dbc-demo` |

### 1.3 전제 조건 (이미 존재하는 라이브러리 모듈)

| 모듈 | 설명 |
|------|------|
| `bluetape4k-spring-boot3-exposed-jdbc` | JDBC Repository, `@EnableExposedJdbcRepositories`, Spring 6 transaction |
| `bluetape4k-spring-boot3-exposed-r2dbc` | R2DBC Repository, `@EnableExposedR2dbcRepositories`, suspend/Flow |
| `bluetape4k-spring-boot4-exposed-jdbc` | Boot 4 JDBC, `exposed_spring7_transaction` |
| `bluetape4k-spring-boot4-exposed-r2dbc` | Boot 4 R2DBC, `exposed_spring7_transaction` 기반 |

---

## 2. 소스 구조 분석

### 2.1 JDBC MVC Demo (`exposed-jdbc-spring-data-mvc-demo`)

```
src/main/kotlin/io/bluetape4k/examples/exposed/mvc/
├── DemoApplication.kt              # @SpringBootApplication + @EnableExposedJdbcRepositories
├── config/
│   └── DataInitializer.kt          # ApplicationRunner, MigrationUtils로 DDL + seed data
├── controller/
│   └── ProductController.kt        # @RestController, CRUD (GET/POST/PUT/DELETE/search)
├── domain/
│   └── ProductEntity.kt            # Products (LongIdTable) + ProductEntity (LongEntity) + ProductDto
└── repository/
    └── ProductJdbcRepository.kt    # ExposedJdbcRepository<ProductEntity, Long>, findByName, findByPriceLessThan

src/main/resources/
└── application.yml                 # H2 인메모리 JDBC 설정

src/test/kotlin/io/bluetape4k/examples/exposed/mvc/
├── ProductJdbcRepositoryTest.kt    # 21 tests - @SpringBootTest + @Transactional, CRUD/Page/Sort/DSL
└── ProductControllerTest.kt        # 7 tests - RANDOM_PORT, RestClient 기반 E2E

src/test/resources/
├── junit-platform.properties       # parallel=false, per_class lifecycle
└── logback-test.xml                # DEBUG 로깅
```

**핵심 특징:**
- Spring MVC (`spring-boot-starter-web`) 기반 동기 REST API
- Exposed DAO 패턴 (`LongEntity` + `LongEntityClass`)
- `transaction {}` 블록으로 트랜잭션 관리
- `MigrationUtils.statementsRequiredForDatabaseMigration()` 으로 DDL 자동 생성
- `exposed_spring_boot4_starter` 사용 (experimental 기준) -> Boot 3에서는 `exposed_spring_boot_starter`로 변경 필요

### 2.2 R2DBC WebFlux Demo (`exposed-r2dbc-spring-data-webflux-demo`)

```
src/main/kotlin/io/bluetape4k/examples/exposed/webflux/
├── WebfluxDemoApplication.kt       # @SpringBootApplication + @EnableExposedR2dbcRepositories
├── config/
│   ├── DataInitializer.kt          # CoroutineScope + ApplicationReadyEvent, suspendTransaction seed
│   └── ExposedR2dbcConfig.kt       # R2dbcDatabase.connect() Bean 등록
├── controller/
│   └── ProductController.kt        # @RestController, suspend CRUD (GET/POST/PUT/DELETE)
├── domain/
│   └── ProductEntity.kt            # Products (LongIdTable) + ProductDto (HasIdentifier<Long>)
└── repository/
    └── ProductR2dbcRepository.kt   # ExposedR2dbcRepository<ProductDto, Long>, toDomain/toPersistValues

src/main/resources/
└── application.yml                 # H2 JDBC + R2DBC 이중 설정

src/test/kotlin/io/bluetape4k/examples/exposed/webflux/
├── ProductR2dbcRepositoryTest.kt   # 16 tests - runTest/runSuspendIO, suspend CRUD/Flow/saveAll/streamAll
└── ProductControllerTest.kt        # 7 tests - RANDOM_PORT, WebTestClient 기반 E2E

src/test/resources/
├── junit-platform.properties
└── logback-test.xml
```

**핵심 특징:**
- Spring WebFlux (`spring-boot-starter-webflux`) 기반 비동기 suspend REST API
- Exposed DSL 패턴 (DAO 미사용, Table + data class DTO)
- `suspendTransaction {}` 블록으로 코루틴 트랜잭션 관리
- `R2dbcDatabase.connect()` 수동 Bean 등록
- `HasIdentifier<Long>` 인터페이스 구현 (bluetape4k-exposed-core 제공)
- `SchemaUtils.create()` (R2DBC용) 로 DDL 생성

---

## 3. 이관 전략

### 3.1 공통 원칙

1. **코드 이관만 수행**: 기능 추가/리팩토링 없이 소스 그대로 복사 (`.kt` 파일 변경 없음, `build.gradle.kts`만 신규 작성)
2. **패키지명 유지**: `io.bluetape4k.examples.exposed.mvc` / `io.bluetape4k.examples.exposed.webflux`
3. **`id(Plugins.spring_boot)` 플러그인 미사용**: `cassandra-demo` 등 기존 데모 모듈 패턴 준수. `bootJar` 태스크 불필요.
4. **settings.gradle.kts 변경 불필요**: `includeModules("spring-boot3/4")` 가 자동으로 하위 디렉토리 등록
5. **`configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 패턴 필수**

### 3.2 Spring Boot 3 이관

- **의존성 참조**: `project(":bluetape4k-spring-boot3-exposed-jdbc")` / `project(":bluetape4k-spring-boot3-exposed-r2dbc")`
- **Spring Transaction**: `exposed_spring_transaction` (Spring 6 / `spring-transaction` 아티팩트)
- **Boot Starter**: `exposed_spring_boot_starter` (Exposed 공식 Spring Boot starter)
- **소스 그대로 복사** (experimental 코드 기준)

### 3.3 Spring Boot 4 이관

- **BOM 적용**: `implementation(platform(Libs.spring_boot4_dependencies))` (첫 줄)
- **의존성 참조**: `project(":bluetape4k-spring-boot4-exposed-jdbc")` / `project(":bluetape4k-spring-boot4-exposed-r2dbc")`
- **Spring Transaction**: `exposed_spring7_transaction` (Spring 7 / `spring7-transaction` 아티팩트)
- **Boot Starter**: `exposed_spring_boot4_starter` (Exposed 공식 Spring Boot 4 starter)
- **소스 동일 복사**: Spring Boot 4의 API는 Boot 3과 동일한 패키지 네임스페이스이므로 Kotlin 소스 변경 없음

### 3.4 JDBC Demo vs R2DBC Demo 차이

| 항목 | JDBC Demo (MVC) | R2DBC Demo (WebFlux) |
|------|----------------|---------------------|
| Web 스택 | `spring-boot-starter-web` | `spring-boot-starter-webflux` |
| 트랜잭션 | `transaction {}` (동기) | `suspendTransaction {}` (코루틴) |
| Entity 패턴 | DAO (`LongEntity`) | DSL + DTO (`data class`) |
| Repository | `ExposedJdbcRepository` | `ExposedR2dbcRepository` |
| DB 설정 | DataSource (JDBC) only | DataSource (JDBC) + R2DBC 이중 설정 |
| DDL 생성 | `MigrationUtils` (JDBC) | `SchemaUtils.create()` (R2DBC) |
| 테스트 클라이언트 | `RestClient` | `WebTestClient` |
| Coroutines | 불필요 | 필수 (`kotlinx-coroutines-core/reactor/test`) |

---

## 4. build.gradle.kts 상세

### 4.1 `spring-boot3/exposed-jdbc-demo/build.gradle.kts`

```kotlin
plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":bluetape4k-spring-boot3-exposed-jdbc"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.exposed_spring_boot_starter)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_migration_jdbc)
    implementation(Libs.exposed_java_time)
    runtimeOnly(Libs.h2_v2)

    testImplementation(Libs.springBootStarter("test"))
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}
```

### 4.2 `spring-boot3/exposed-r2dbc-demo/build.gradle.kts`

```kotlin
plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":bluetape4k-spring-boot3-exposed-r2dbc"))

    implementation(Libs.exposed_r2dbc)
    implementation(Libs.exposed_java_time)

    runtimeOnly(Libs.r2dbc_h2)
    runtimeOnly(Libs.h2_v2)   // JDBC DataSource (DataInitializer + SchemaUtils에 필요)

    implementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test"))

    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}
```

### 4.3 `spring-boot4/exposed-jdbc-demo/build.gradle.kts`

```kotlin
plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(platform(Libs.spring_boot4_dependencies))

    implementation(project(":bluetape4k-spring-boot4-exposed-jdbc"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.exposed_spring_boot4_starter)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_migration_jdbc)
    implementation(Libs.exposed_java_time)
    runtimeOnly(Libs.h2_v2)

    testImplementation(Libs.springBootStarter("test"))
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}
```

### 4.4 `spring-boot4/exposed-r2dbc-demo/build.gradle.kts`

```kotlin
plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(platform(Libs.spring_boot4_dependencies))

    implementation(project(":bluetape4k-spring-boot4-exposed-r2dbc"))

    implementation(Libs.exposed_r2dbc)
    implementation(Libs.exposed_java_time)

    runtimeOnly(Libs.r2dbc_h2)
    runtimeOnly(Libs.h2_v2)   // JDBC DataSource (DataInitializer + SchemaUtils에 필요)

    implementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test"))

    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}
```

---

## 5. 테스트 전략

### 5.1 공통

- **H2 인메모리 DB**: 외부 인프라 없이 단독 실행 가능
- **`@SpringBootTest`**: 전체 ApplicationContext 로딩
- **Testcontainers 미사용**: 데모 단순성 유지

### 5.2 JDBC Demo 테스트

| 테스트 클래스 | 테스트 수 | 방식 |
|-------------|---------|------|
| `ProductJdbcRepositoryTest` | 21 | `@SpringBootTest` + `@Transactional`, `transaction {}` 내 CRUD/Page/Sort/DSL |
| `ProductControllerTest` | 7 | `RANDOM_PORT` + `RestClient`, E2E HTTP 호출 |

### 5.3 R2DBC Demo 테스트

| 테스트 클래스 | 테스트 수 | 방식 |
|-------------|---------|------|
| `ProductR2dbcRepositoryTest` | 16 | `@SpringBootTest(NONE)` + `runTest`/`runSuspendIO`, suspend CRUD/Flow |
| `ProductControllerTest` | 7 | `RANDOM_PORT` + `WebTestClient`, E2E HTTP 호출 |

### 5.4 검증 방법

각 모듈별로 Gradle 테스트 실행:

```bash
./gradlew :bluetape4k-spring-boot3-exposed-jdbc-demo:test
./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-demo:test
./gradlew :bluetape4k-spring-boot4-exposed-jdbc-demo:test
./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-demo:test
```

---

## 6. 파일 이관 목록

### 6.1 `spring-boot3/exposed-jdbc-demo` (JDBC MVC, Boot 3)

| 파일 | 작업 |
|------|------|
| `build.gradle.kts` | **신규 작성** (섹션 4.1) |
| `src/main/kotlin/.../mvc/DemoApplication.kt` | 복사 (그대로) |
| `src/main/kotlin/.../mvc/config/DataInitializer.kt` | 복사 (그대로) |
| `src/main/kotlin/.../mvc/controller/ProductController.kt` | 복사 (그대로) |
| `src/main/kotlin/.../mvc/domain/ProductEntity.kt` | 복사 (그대로) |
| `src/main/kotlin/.../mvc/repository/ProductJdbcRepository.kt` | 복사 (그대로) |
| `src/main/resources/application.yml` | 복사 (그대로) |
| `src/test/kotlin/.../mvc/ProductJdbcRepositoryTest.kt` | 복사 (그대로) |
| `src/test/kotlin/.../mvc/ProductControllerTest.kt` | 복사 (그대로) |
| `src/test/resources/junit-platform.properties` | 복사 (그대로) |
| `src/test/resources/logback-test.xml` | 복사 (그대로) |

### 6.2 `spring-boot3/exposed-r2dbc-demo` (R2DBC WebFlux, Boot 3)

| 파일 | 작업 |
|------|------|
| `build.gradle.kts` | **신규 작성** (섹션 4.2) |
| `src/main/kotlin/.../webflux/WebfluxDemoApplication.kt` | 복사 (그대로) |
| `src/main/kotlin/.../webflux/config/DataInitializer.kt` | 복사 (그대로) |
| `src/main/kotlin/.../webflux/config/ExposedR2dbcConfig.kt` | 복사 (그대로) |
| `src/main/kotlin/.../webflux/controller/ProductController.kt` | 복사 (그대로) |
| `src/main/kotlin/.../webflux/domain/ProductEntity.kt` | 복사 (그대로) |
| `src/main/kotlin/.../webflux/repository/ProductR2dbcRepository.kt` | 복사 (그대로) |
| `src/main/resources/application.yml` | 복사 (그대로) |
| `src/test/kotlin/.../webflux/ProductR2dbcRepositoryTest.kt` | 복사 (그대로) |
| `src/test/kotlin/.../webflux/ProductControllerTest.kt` | 복사 (그대로) |
| `src/test/resources/junit-platform.properties` | 복사 (그대로) |
| `src/test/resources/logback-test.xml` | 복사 (그대로) |

### 6.3 `spring-boot4/exposed-jdbc-demo` (JDBC MVC, Boot 4)

- **Boot 3 JDBC Demo와 동일한 소스 파일 구조**
- `build.gradle.kts`만 **Boot 4 전용** (섹션 4.3)

### 6.4 `spring-boot4/exposed-r2dbc-demo` (R2DBC WebFlux, Boot 4)

- **Boot 3 R2DBC Demo와 동일한 소스 파일 구조**
- `build.gradle.kts`만 **Boot 4 전용** (섹션 4.4)

---

## 7. 주의사항

### 7.1 Spring Boot 4 BOM 적용

```kotlin
// 반드시 이 방식 사용 (KGP 2.3 kotlinBuildToolsApiClasspath 충돌 방지)
implementation(platform(Libs.spring_boot4_dependencies))

// 절대 금지:
// dependencyManagement { imports { mavenBom(...) } }
```

### 7.2 모듈명 자동 등록

`settings.gradle.kts`의 `includeModules("spring-boot3", withProjectName=true, withBaseDir=true)` 로직에 의해:
- 디렉토리 `spring-boot3/exposed-jdbc-demo/` -> 프로젝트명 `bluetape4k-spring-boot3-exposed-jdbc-demo`
- **별도 settings 수정 불필요**

### 7.3 experimental 소스의 의존성 차이

experimental 소스에서 사용하는 의존성명이 다를 수 있음:
- `project(":exposed-jdbc-spring-data")` -> `project(":bluetape4k-spring-boot3-exposed-jdbc")` 로 변경
- `project(":exposed-r2dbc-spring-data")` -> `project(":bluetape4k-spring-boot3-exposed-r2dbc")` 로 변경
- `Libs.bluetape4k_r2dbc` (experimental) -> `project(":bluetape4k-r2dbc")` 또는 transitive로 해결

### 7.4 Exposed Spring Boot Starter 구분

| Boot 버전 | Starter | Transaction |
|----------|---------|------------|
| Boot 3 (Spring 6) | `Libs.exposed_spring_boot_starter` | `Libs.exposed_spring_transaction` |
| Boot 4 (Spring 7) | `Libs.exposed_spring_boot4_starter` | `Libs.exposed_spring7_transaction` |

### 7.5 R2DBC Demo의 `bluetape4k_r2dbc` 참조

experimental에서는 `Libs.bluetape4k_r2dbc`를 사용하나, 이 의존성은 `bluetape4k-spring-boot3-exposed-r2dbc` / `bluetape4k-exposed-r2dbc`가 transitive로 제공한다. 명시적 의존성 추가 불필요할 수 있으며, 빌드 시 확인 필요.

### 7.6 테스트 리소스 logback-test.xml 패키지명 조정

logback-test.xml 내 logger 패키지명이 소스 패키지와 일치하는지 확인:
- JDBC: `io.bluetape4k.examples.exposed.mvc`
- R2DBC: `io.bluetape4k.examples.exposed.webflux`

---

## 8. 태스크 목록

### Phase 1: Boot 3 모듈 생성 (JDBC + R2DBC)

- [ ] T1. `spring-boot3/exposed-jdbc-demo/build.gradle.kts` 작성
- [ ] T2. `spring-boot3/exposed-jdbc-demo/` 소스 복사 (5 main + 2 test + 3 resources)
- [ ] T3. `spring-boot3/exposed-r2dbc-demo/build.gradle.kts` 작성
- [ ] T4. `spring-boot3/exposed-r2dbc-demo/` 소스 복사 (6 main + 2 test + 3 resources)
- [ ] T5. Boot 3 JDBC 데모 빌드 + 테스트 검증
- [ ] T6. Boot 3 R2DBC 데모 빌드 + 테스트 검증

### Phase 2: Boot 4 모듈 생성 (JDBC + R2DBC)

- [ ] T7. `spring-boot4/exposed-jdbc-demo/build.gradle.kts` 작성 (Boot 4 BOM)
- [ ] T8. `spring-boot4/exposed-jdbc-demo/` 소스 복사 (Boot 3와 동일)
- [ ] T9. `spring-boot4/exposed-r2dbc-demo/build.gradle.kts` 작성 (Boot 4 BOM)
- [ ] T10. `spring-boot4/exposed-r2dbc-demo/` 소스 복사 (Boot 3와 동일)
- [ ] T11. Boot 4 JDBC 데모 빌드 + 테스트 검증
- [ ] T12. Boot 4 R2DBC 데모 빌드 + 테스트 검증

### Phase 3: 정리

- [ ] T13. 각 모듈 README.md 작성 (4개 — 빌드/실행 방법, 예시 포함)
- [ ] T14. CLAUDE.md Architecture 섹션 업데이트 (Spring Boot 3/4 데모 모듈 추가)
- [ ] T15. 전체 4개 모듈 최종 빌드 확인

---

## 변경 영향 범위

| 항목 | 영향 |
|------|------|
| `settings.gradle.kts` | 변경 없음 (자동 등록) |
| 기존 모듈 | 변경 없음 |
| 신규 디렉토리 | 4개 (`spring-boot3/exposed-jdbc-demo`, `spring-boot3/exposed-r2dbc-demo`, `spring-boot4/exposed-jdbc-demo`, `spring-boot4/exposed-r2dbc-demo`) |
| 신규 파일 | ~44개 (4 build.gradle.kts + 40 소스/테스트/리소스) |
