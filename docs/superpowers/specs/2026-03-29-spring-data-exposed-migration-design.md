# Spring Data Exposed 모듈 이관 설계 Spec

- **날짜**: 2026-03-29
- **상태**: Draft
- **출처**: `bluetape4k-experimental/spring-data/` (exposed-jdbc-spring-data, exposed-r2dbc-spring-data)
- **대상**: `bluetape4k-projects/spring-boot3/`, `bluetape4k-projects/spring-boot4/`

---

## 1. 개요

`bluetape4k-experimental/spring-data/` 하위의 Exposed Spring Data Repository 모듈 2개를 `bluetape4k-projects`로 승격합니다. Spring Boot 3.5.x (Spring 6.x, Spring Data 3.x)와 Spring Boot 4.0.x (Spring 7.x, Spring Data 4.x) 각각에 대응하는 4개 모듈을 생성합니다.

### 이관 대상

| experimental 모듈 | 설명 |
|---|---|
| `exposed-jdbc-spring-data` | Exposed DAO Entity 기반 Spring Data JDBC Repository |
| `exposed-r2dbc-spring-data` | Exposed R2DBC DSL 기반 코루틴 Repository |

---

## 2. 모듈 배치 및 명명

### 2.1 디렉토리 구조

```
bluetape4k-projects/
  spring-boot3/
    exposed-jdbc-spring-data/     # NEW
    exposed-r2dbc-spring-data/    # NEW
  spring-boot4/
    exposed-jdbc-spring-data/     # NEW
    exposed-r2dbc-spring-data/    # NEW
```

### 2.2 Gradle 프로젝트명 (자동 생성)

`settings.gradle.kts`의 `includeModules("spring-boot3", withBaseDir = true)` 함수에 의해 자동 등록됩니다.

| 디렉토리 | Gradle 프로젝트명 |
|---|---|
| `spring-boot3/exposed-jdbc-spring-data` | `:bluetape4k-spring-boot3-exposed-jdbc-spring-data` |
| `spring-boot3/exposed-r2dbc-spring-data` | `:bluetape4k-spring-boot3-exposed-r2dbc-spring-data` |
| `spring-boot4/exposed-jdbc-spring-data` | `:bluetape4k-spring-boot4-exposed-jdbc-spring-data` |
| `spring-boot4/exposed-r2dbc-spring-data` | `:bluetape4k-spring-boot4-exposed-r2dbc-spring-data` |

> **참고**: `settings.gradle.kts` 변경 불필요. 디렉토리 생성만으로 자동 등록됩니다.

---

## 3. Spring Boot 버전별 차이점 분석

### 3.1 핵심 차이 (코드 변경 필요)

| 항목 | Spring Boot 3.5.x (Spring Data 3.x) | Spring Boot 4.0.x (Spring Data 4.x) |
|---|---|---|
| Exposed transaction | `exposed_spring_transaction` | `exposed_spring7_transaction` |
| SpringTransactionManager import | `org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager` | `org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager` |
| `getEntityInformation` | `getEntityInformation(Class<T>): EntityInformation<T, ID>` (primary) | `getEntityInformation(RepositoryMetadata): EntityInformation<*, *>` (primary, Class version deprecated) |
| BOM 적용 | `dependencyManagement { imports }` (기존 패턴) | `implementation(platform(Libs.spring_boot4_dependencies))` (KGP 충돌 방지) |
| Exposed Starter | `exposed_spring_boot_starter` (참고용, 직접 사용하지 않음) | `exposed_spring_boot4_starter` (참고용, 직접 사용하지 않음) |

### 3.2 공통 (변경 불필요)

- Spring Data `ListCrudRepository`, `ListPagingAndSortingRepository` -- Spring Data 3.x에도 존재 (2022년부터)
- Spring Data `CoroutineCrudRepository` -- 3.x/4.x 동일
- Spring Data `QueryByExampleExecutor` -- 3.x/4.x 동일
- `RepositoryFactorySupport`, `AbstractQueryCreator`, `PartTree` -- 핵심 API 동일
- `ValueExpressionDelegate` -- 3.x/4.x 동일 (Spring Data 3.3+에서 도입)
- Exposed DSL/DAO API -- 동일 (`org.jetbrains.exposed.v1.*`)
- 패키지 네임스페이스 -- `io.bluetape4k.spring.data.exposed.*` 유지

### 3.3 변경 대상 파일 목록

#### JDBC 모듈 (Spring Boot 3 적응)

1. **`ExposedSpringDataAutoConfiguration.kt`** -- `spring7.transaction` → `spring.transaction` import
2. **`ExposedJdbcRepositoryFactory.kt`** -- `getEntityInformation(Class<T>)` 오버라이드 추가 (`@Deprecated` 없이 유지), `getEntityInformation(RepositoryMetadata)` 병행 오버라이드
3. **`AbstractExposedJdbcRepositoryTest.kt`** (테스트) -- `spring7.transaction` → `spring.transaction`
4. **`build.gradle.kts`** -- `exposed_spring7_transaction` → `exposed_spring_transaction`

#### R2DBC 모듈 (Spring Boot 3 적응)

5. **`ExposedR2dbcRepositoryFactory.kt`** -- `getEntityInformation(Class<T>)` 오버라이드 **반드시 추가** (Spring Data 3.x에서 abstract 메서드이므로 미구현 시 컴파일 오류 발생)
   ```kotlin
   // Spring Boot 3 전용 추가 오버라이드
   override fun <T : Any, ID : Any> getEntityInformation(domainClass: Class<T>): EntityInformation<T, ID> =
       StaticEntityInformation(domainClass, resolveIdType(domainClass)) as EntityInformation<T, ID>
   ```
6. **`ExposedR2dbcSpringDataAutoConfiguration.kt`** -- import 경로 변경 없음 (SpringTransactionManager 직접 사용 안 함)
7. **`build.gradle.kts`** -- `exposed_spring_transaction` 제거 (JDBC 모듈 전이 의존성으로 충분)

#### Spring Boot 4 모듈 (추가 변경)

8. **`build.gradle.kts`** -- `implementation(platform(Libs.spring_boot4_dependencies))` 추가
9. **`build.gradle.kts`** -- `bluetape4k_virtualthread_jdk25` → `bluetape4k_virtualthread_jdk21`

---

## 4. 코드 공유 전략

### 4.1 접근 방식: "Fork & Adapt" (복사 후 적응)

Spring Boot 3/4 간 코드 차이가 **3~5개 파일, 각 1~3줄 수준**으로 매우 작습니다. 공유 모듈(shared source set 등) 도입 시 빌드 복잡성이 증가하는 반면 실질적 이득이 미미합니다.

**결정: 각 모듈에 전체 소스를 복사하고, 버전별 차이만 적응합니다.**

### 4.2 공유 위치의 대안 (채택하지 않음)

| 방안 | 장점 | 단점 | 결정 |
|---|---|---|---|
| Gradle source set 공유 | 코드 중복 제거 | `includeModules` 함수와 충돌, 빌드 복잡성 | **불채택** |
| 별도 `-core` 모듈 | 깔끔한 분리 | 3개 모듈 추가 → 의존성 그래프 복잡 | **불채택** |
| 심볼릭 링크 | 간단 | OS 의존, Git 호환성 문제 | **불채택** |
| **Fork & Adapt** | **단순, 독립 빌드** | **3~5개 파일 중복** | **채택** |

### 4.3 중복 최소화 원칙

- R2DBC 모듈은 JDBC 모듈의 `ExposedMappingContext`, `ExposedEntityInformation`, `ExposedSortSupport` 등을 **모듈 내 의존성**(`api(project(":bluetape4k-spring-boot3-exposed-jdbc-spring-data"))`)으로 재사용
- 같은 Spring Boot 버전 내에서는 중복 없음
- Spring Boot 3 vs 4 간 차이 파일만 별도 유지

---

## 5. 모듈별 상세 설계

### 5.1 spring-boot3/exposed-jdbc-spring-data

#### build.gradle.kts

```kotlin
plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(Libs.springData("commons"))

    api(Libs.kotlin_reflect)
    api(Libs.bluetape4k_logging)
    api(Libs.exposed_core)
    api(Libs.exposed_dao)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)
    api(Libs.exposed_spring_transaction)           // *** Spring 6용 ***

    testImplementation(Libs.exposed_migration_jdbc)
    testImplementation(Libs.flyway_core)
    testImplementation(Libs.bluetape4k_junit5)
    testImplementation(Libs.bluetape4k_virtualthread_jdk21)   // *** JDK 21 ***

    api(Libs.bluetape4k_exposed_jdbc)
    testImplementation(Libs.bluetape4k_exposed_jdbc_tests)

    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBootStarter("data-jdbc"))
    testImplementation(Libs.springBootStarter("test"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)

    // Multi-DB 테스트용 JDBC 드라이버
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.postgresql_driver)
}
```

#### 코드 변경 사항 (experimental 대비)

| 파일 | 변경 내용 |
|---|---|
| `ExposedSpringDataAutoConfiguration.kt` | `import ...spring7.transaction...` -> `import ...spring.transaction...` |
| `ExposedJdbcRepositoryFactory.kt` | `getEntityInformation(RepositoryMetadata)` 유지 + `getEntityInformation(Class<T>)` primary로 복원 (deprecated 제거) |
| `SimpleExposedJdbcRepository.kt` | 변경 없음 (Spring Data 3.x에도 `ListCrudRepository` 존재) |
| `AbstractExposedJdbcRepositoryTest.kt` | `import ...spring7.transaction...` -> `import ...spring.transaction...`, JDK 25 -> JDK 21 |
| `build.gradle.kts` | `exposed_spring7_transaction` -> `exposed_spring_transaction` |

#### Spring Data 3.x `RepositoryFactorySupport` 적응

```kotlin
// Spring Data 3.x: getEntityInformation(Class<T>) 가 primary
// Spring Data 4.x: getEntityInformation(RepositoryMetadata) 가 primary (Class 버전 deprecated)

// Spring Boot 3 버전:
override fun <T : Any, ID : Any> getEntityInformation(domainClass: Class<T>): EntityInformation<T, ID> =
    ExposedEntityInformationImpl(domainClass as Class<Entity<Any>>) as EntityInformation<T, ID>

// getEntityInformation(RepositoryMetadata) 도 오버라이드 (Spring Data 3.3+에서 도입)
override fun getEntityInformation(metadata: RepositoryMetadata): EntityInformation<*, *> =
    exposedEntityInformation(metadata.domainType)
```

> **참고**: `getEntityInformation(RepositoryMetadata)`는 Spring Data 3.3에서 추가되었으므로 Spring Boot 3.5.x에서 사용 가능합니다. 두 메서드 모두 오버라이드하되, Spring Boot 3에서는 `Class<T>` 버전에 `@Deprecated` 없이 유지합니다.

#### `ValueExpressionDelegate` 호환성

`ValueExpressionDelegate`는 Spring Data 3.3+에서 도입되었으므로 Spring Boot 3.5.x에서 사용 가능합니다. `ExposedQueryLookupStrategy`의 시그니처 변경 불필요합니다.

### 5.2 spring-boot4/exposed-jdbc-spring-data

experimental 코드를 거의 그대로 복사합니다.

#### build.gradle.kts 변경

```kotlin
dependencies {
    // Spring Boot 4 BOM
    implementation(platform(Libs.spring_boot4_dependencies))

    // ... (나머지 동일)
    api(Libs.exposed_spring7_transaction)  // *** Spring 7용 (experimental과 동일) ***
    testImplementation(Libs.bluetape4k_virtualthread_jdk21)  // JDK 21 (프로젝트 표준)
}
```

> **참고**: `bluetape4k-projects`의 JVM Toolchain은 Java 21이므로, experimental에서 사용하던 `bluetape4k_virtualthread_jdk25`를 `bluetape4k_virtualthread_jdk21`로 변경합니다. JDK 25 Virtual Thread 테스트는 experimental에서만 수행합니다.

### 5.3 spring-boot3/exposed-r2dbc-spring-data

#### build.gradle.kts

```kotlin
plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(Libs.springData("commons"))

    // JDBC 모듈의 EntityInformation, MappingContext 재사용
    api(project(":bluetape4k-spring-boot3-exposed-jdbc-spring-data"))

    api(Libs.kotlin_reflect)
    api(Libs.exposed_core)
    api(Libs.exposed_r2dbc)
    api(Libs.exposed_java_time)
    // exposed_spring_transaction은 JDBC 모듈(api 의존)에서 전이됨 → 중복 추가 불필요

    testImplementation(Libs.exposed_migration_r2dbc)
    testImplementation(Libs.flyway_core)
    testImplementation(Libs.bluetape4k_junit5)
    testImplementation(Libs.bluetape4k_virtualthread_jdk21)

    api(Libs.bluetape4k_exposed_core)
    api(Libs.bluetape4k_exposed_r2dbc)
    testImplementation(Libs.bluetape4k_exposed_r2dbc_tests)

    api(Libs.bluetape4k_coroutines)
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    compileOnly(Libs.springBoot("autoconfigure"))
    testImplementation(Libs.springBootStarter("test"))

    testImplementation(Libs.h2_v2)
    testImplementation(Libs.r2dbc_h2)
    testImplementation(Libs.hikaricp)

    // Multi-DB R2DBC 드라이버
    testImplementation(Libs.r2dbc_mysql)
    testImplementation(Libs.r2dbc_mariadb)
    testImplementation(Libs.r2dbc_postgresql)

    // Multi-DB JDBC 드라이버 (Testcontainers 연결용)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.postgresql_driver)
}
```

### 5.4 spring-boot4/exposed-r2dbc-spring-data

```kotlin
dependencies {
    implementation(platform(Libs.spring_boot4_dependencies))

    api(project(":bluetape4k-spring-boot4-exposed-jdbc-spring-data"))  // Boot 4 JDBC 모듈 참조

    // ... (나머지 동일, exposed_spring7_transaction 사용)
}
```

---

## 6. 패키지 구조

**Spring Boot 3과 4 모두 동일한 패키지를 사용합니다.** (기존 `spring-boot3/core`와 `spring-boot4/core`가 동일한 `io.bluetape4k.spring.*` 패키지를 사용하는 패턴 준수)

```
io.bluetape4k.spring.data.exposed.jdbc/
  annotation/
    ExposedEntity.kt
    Query.kt
  config/
    ExposedSpringDataAutoConfiguration.kt
  mapping/
    ExposedMappingContext.kt
    ExposedPersistentEntity.kt
    ExposedPersistentProperty.kt
    DefaultExposedPersistentEntity.kt
    DefaultExposedPersistentProperty.kt
  repository/
    ExposedJdbcRepository.kt
    config/
      EnableExposedJdbcRepositories.kt
      ExposedJdbcRepositoriesRegistrar.kt
      ExposedJdbcRepositoryConfigurationExtension.kt
    query/
      DeclaredExposedQuery.kt
      ExposedQueryCreator.kt
      ExposedQueryLookupStrategy.kt
      ExposedQueryMethod.kt
      ParameterMetadataProvider.kt
      PartTreeExposedQuery.kt
    support/
      ExposedEntityInformation.kt
      ExposedEntityInformationImpl.kt
      ExposedJdbcRepositoryFactory.kt
      ExposedJdbcRepositoryFactoryBean.kt
      ExposedSortSupport.kt
      SimpleExposedJdbcRepository.kt

io.bluetape4k.spring.data.exposed.r2dbc/
  config/
    ExposedR2dbcSpringDataAutoConfiguration.kt
  repository/
    ExposedR2dbcRepository.kt
    config/
      EnableExposedR2dbcRepositories.kt
      ExposedR2dbcRepositoriesRegistrar.kt
      ExposedSuspendRepositoryConfigurationExtension.kt
    support/
      ExposedR2dbcRepositoryFactory.kt
      ExposedR2dbcRepositoryFactoryBean.kt
      SimpleExposedR2dbcRepository.kt
```

---

## 7. AutoConfiguration 등록

### 7.1 파일 경로

```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

> Spring Boot 3.x / 4.x 모두 `AutoConfiguration.imports` 파일을 사용합니다. `spring.factories`는 사용하지 않습니다.

### 7.2 JDBC 모듈

```
io.bluetape4k.spring.data.exposed.jdbc.config.ExposedSpringDataAutoConfiguration
```

### 7.3 R2DBC 모듈

```
io.bluetape4k.spring.data.exposed.r2dbc.config.ExposedR2dbcSpringDataAutoConfiguration
```

---

## 8. 테스트 전략

### 8.1 테스트 구조

각 모듈에 동일한 테스트 셋을 유지합니다:

| 테스트 클래스 | 대상 | DB |
|---|---|---|
| `SimpleExposedJdbcRepositoryTest` | 기본 CRUD, Page, Sort | H2 인메모리 |
| `PartTreeExposedJdbcQueryTest` | 메서드명 기반 쿼리 (PartTree) | H2 인메모리 |
| `QueryByExampleTestJdbc` | QBE (Example, ExampleMatcher) | H2 인메모리 |
| `MultiDbExposedJdbcRepositoryTest` | 멀티 DB 호환성 | MySQL, MariaDB, PostgreSQL (Testcontainers) |
| `SimpleExposedR2dbcRepositoryTest` | R2DBC CRUD, Page, Flow | H2 R2DBC 인메모리 |
| `MultiDbExposedR2dbcRepositoryTest` | R2DBC 멀티 DB | MySQL, PostgreSQL R2DBC (Testcontainers) |

### 8.2 테스트 의존성

- `bluetape4k-junit5` -- JUnit 5 확장
- `bluetape4k-exposed-jdbc-tests` / `bluetape4k-exposed-r2dbc-tests` -- Exposed 테스트 인프라
- `bluetape4k-testcontainers` -- Testcontainers 지원
- `bluetape4k-virtualthread-jdk21` -- Virtual Thread 테스트 (experimental의 JDK 25 대신)

### 8.3 테스트 실행 명령

```bash
# Spring Boot 3 JDBC
./gradlew :bluetape4k-spring-boot3-exposed-jdbc-spring-data:test

# Spring Boot 3 R2DBC
./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-spring-data:test

# Spring Boot 4 JDBC
./gradlew :bluetape4k-spring-boot4-exposed-jdbc-spring-data:test

# Spring Boot 4 R2DBC
./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-spring-data:test
```

---

## 9. Libs.kt 의존성 참조

### 9.1 기존 사용 가능한 의존성 (추가 불필요)

```kotlin
// Libs.kt 에 이미 존재
val exposed_spring_transaction       // Spring 6용
val exposed_spring7_transaction      // Spring 7용
val exposed_spring_boot_starter      // Spring Boot 3용
val exposed_spring_boot4_starter     // Spring Boot 4용
val exposed_core
val exposed_dao
val exposed_jdbc
val exposed_r2dbc
val exposed_java_time
val exposed_migration_jdbc
val exposed_migration_r2dbc
val bluetape4k_exposed_core
val bluetape4k_exposed_jdbc
val bluetape4k_exposed_r2dbc
val bluetape4k_exposed_jdbc_tests
val bluetape4k_exposed_r2dbc_tests
```

### 9.2 추가 필요 여부

추가 의존성 없음. 기존 `Libs.kt`에 정의된 항목으로 충분합니다.

---

## 10. 위험 요소 및 완화 방안

### 10.1 Spring Data 3.x API 호환성

| 위험 | 영향 | 완화 |
|---|---|---|
| `ValueExpressionDelegate` 3.x 미존재 가능성 | 컴파일 실패 | Spring Data 3.3+에서 도입 확인, Spring Boot 3.5.x는 3.4+ 포함 -- **위험 낮음** |
| `RepositoryComposition.RepositoryFragments` 시그니처 변경 | R2DBC Factory 컴파일 실패 | Spring Data 3.x/4.x 모두 `getRepositoryFragments(RepositoryMetadata): RepositoryFragments` -- **안전** |
| `ExposedR2dbcRepositoryFactory.getEntityInformation(Class<T>)` 미구현 | Spring Boot 3 컴파일 실패 | Section 3.3에서 명시적 추가 필요 (Spring Data 4.x에서는 default 메서드로 위임됨) -- **수정 완료** |
| `AbstractQueryCreator` `and(Part, S, Iterator)` 3-param 시그니처 | PartTree 쿼리 컴파일 실패 | Spring Data 3.x에서도 3-param 버전 존재 -- **위험 낮음** |

### 10.2 Spring Boot 4 BOM 적용

Spring Boot 4 모듈에서는 반드시 `implementation(platform(...))` 방식을 사용합니다. `dependencyManagement { imports { mavenBom() } }` 사용 시 `kotlinBuildToolsApiClasspath`에 영향을 주어 KGP 2.3.x 빌드가 실패합니다.

### 10.3 Exposed Transaction Manager 빈 이름 충돌

`ExposedSpringDataAutoConfiguration`이 `springTransactionManager` 이름으로 `SpringTransactionManager` 빈을 등록합니다. Exposed Spring Boot Starter가 같은 이름의 빈을 등록하므로 `@ConditionalOnMissingBean(name = ["springTransactionManager"])` 조건으로 중복 방지합니다.

---

## 11. 구현 태스크 목록

### Phase 1: Spring Boot 4 모듈 (experimental 거의 그대로)

- [ ] **T1.1** `spring-boot4/exposed-jdbc-spring-data/` 디렉토리 생성 및 experimental 소스 복사
  - `build.gradle.kts` 작성 (`platform(Libs.spring_boot4_dependencies)` 추가, `jdk25` -> `jdk21`)
  - `src/main/kotlin/` 전체 복사 (변경 없음)
  - `src/main/resources/META-INF/spring/` AutoConfiguration.imports 복사
  - `src/test/kotlin/` 전체 복사 (`jdk25` -> `jdk21`)

- [ ] **T1.2** `spring-boot4/exposed-r2dbc-spring-data/` 디렉토리 생성 및 experimental 소스 복사
  - `build.gradle.kts` 작성 (R2DBC가 JDBC 모듈 참조: `project(":bluetape4k-spring-boot4-exposed-jdbc-spring-data")`)
  - `src/main/kotlin/` 전체 복사 (변경 없음)
  - `src/main/resources/META-INF/spring/` AutoConfiguration.imports 복사
  - `src/test/kotlin/` 전체 복사

- [ ] **T1.3** Spring Boot 4 빌드 검증
  - `./gradlew :bluetape4k-spring-boot4-exposed-jdbc-spring-data:build`
  - `./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-spring-data:build`

- [ ] **T1.4** Spring Boot 4 테스트 실행 (H2 인메모리)
  - `./gradlew :bluetape4k-spring-boot4-exposed-jdbc-spring-data:test`
  - `./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-spring-data:test`

### Phase 2: Spring Boot 3 모듈 (적응 필요)

- [ ] **T2.1** `spring-boot3/exposed-jdbc-spring-data/` 디렉토리 생성 및 소스 복사
  - `build.gradle.kts` 작성 (`exposed_spring_transaction` 사용, BOM 적용 없음)
  - Spring 6 import 적응 (`spring7.transaction` -> `spring.transaction`)
  - `ExposedJdbcRepositoryFactory.kt` -- `getEntityInformation(Class<T>)`에서 `@Deprecated` 제거

- [ ] **T2.2** `spring-boot3/exposed-r2dbc-spring-data/` 디렉토리 생성 및 소스 복사
  - `build.gradle.kts` 작성 (`project(":bluetape4k-spring-boot3-exposed-jdbc-spring-data")` 참조)
  - Spring 6 import 적응

- [ ] **T2.3** Spring Data 3.x API 호환성 검증
  - `ValueExpressionDelegate` 존재 확인
  - `RepositoryFactorySupport.getEntityInformation(RepositoryMetadata)` 존재 확인
  - `AbstractQueryCreator.and(Part, S, Iterator)` 3-param 시그니처 확인
  - 컴파일 실패 시 API fallback 적용

- [ ] **T2.4** Spring Boot 3 빌드 검증
  - `./gradlew :bluetape4k-spring-boot3-exposed-jdbc-spring-data:build`
  - `./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-spring-data:build`

- [ ] **T2.5** Spring Boot 3 테스트 실행 (H2 인메모리)

### Phase 3: 멀티 DB 테스트 및 마무리

- [ ] **T3.1** MultiDb 테스트 검증 (Testcontainers, Spring Boot 3/4 모두)
  - MySQL, MariaDB, PostgreSQL JDBC 테스트
  - MySQL, PostgreSQL R2DBC 테스트

- [ ] **T3.2** CLAUDE.md 업데이트
  - `spring-boot3/` 및 `spring-boot4/` 모듈 목록에 추가
  - Module Structure 섹션 업데이트

- [ ] **T3.3** 각 모듈 README.md 작성
  - 사용법, 의존성, AutoConfiguration 설명

### Phase 4 (선택): Experimental 정리

- [ ] **T4.1** experimental 모듈에서 `bluetape4k-projects`로 이관 완료 표시
  - experimental 모듈 deprecated 마킹 또는 삭제

---

## 12. 예상 작업량

| Phase | 태스크 수 | 예상 시간 |
|---|---|---|
| Phase 1 (Boot 4) | 4 | 30분 |
| Phase 2 (Boot 3) | 5 | 1시간 (API 호환성 검증 포함) |
| Phase 3 (마무리) | 3 | 30분 |
| Phase 4 (정리) | 1 | 10분 |
| **합계** | **13** | **~2시간** |

---

## 13. 의사결정 기록

| 결정 | 이유 |
|---|---|
| Fork & Adapt (코드 복사) | 차이 3~5개 파일, 공유 모듈 대비 빌드 단순성 우선 |
| 동일 패키지 (`io.bluetape4k.spring.*`) | 기존 spring-boot3/core, spring-boot4/core 패턴 준수 |
| JDK 21 Virtual Thread | `bluetape4k-projects` 표준 JVM Toolchain (JDK 25는 experimental 전용) |
| `configurations { testImplementation.get().extendsFrom(...) }` | `spring-boot3/core` 패턴 준수 |
| AutoConfiguration.imports 사용 | Spring Boot 3.x/4.x 모두 지원, `spring.factories` deprecated |
