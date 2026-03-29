# Spring Data Exposed Demo 모듈 이관 실행 계획

**날짜**: 2026-03-29
**Spec**: `docs/superpowers/specs/2026-03-29-spring-data-demo-migration-design.md`
**소스**: `bluetape4k-experimental/examples/exposed-jdbc-spring-data-mvc-demo/`, `bluetape4k-experimental/examples/exposed-r2dbc-spring-data-webflux-demo/`
**대상**: `bluetape4k-projects/spring-boot3/`, `bluetape4k-projects/spring-boot4/`

---

## 사전 검증 결과

- **experimental JDBC 소스 경로**: `/Users/debop/work/bluetape4k/bluetape4k-experimental/examples/exposed-jdbc-spring-data-mvc-demo/`
  - main 소스 5개: `DemoApplication.kt`, `DataInitializer.kt`, `ProductController.kt`, `ProductEntity.kt`, `ProductJdbcRepository.kt`
  - test 소스 2개: `ProductJdbcRepositoryTest.kt`, `ProductControllerTest.kt`
  - resources: `application.yml`, `junit-platform.properties`, `logback-test.xml`
- **experimental R2DBC 소스 경로**: `/Users/debop/work/bluetape4k/bluetape4k-experimental/examples/exposed-r2dbc-spring-data-webflux-demo/`
  - main 소스 6개: `WebfluxDemoApplication.kt`, `DataInitializer.kt`, `ExposedR2dbcConfig.kt`, `ProductController.kt`, `ProductEntity.kt`, `ProductR2dbcRepository.kt`
  - test 소스 2개: `ProductR2dbcRepositoryTest.kt`, `ProductControllerTest.kt`
  - resources: `application.yml`, `junit-platform.properties`, `logback-test.xml`
- **내부 모듈 참조**: `project(":bluetape4k-spring-boot3-exposed-jdbc")` 방식 사용 (`Libs.bluetape4k_*` 상수 없음)
- **Spring Boot 4 BOM**: `implementation(platform(Libs.spring_boot4_dependencies))` (절대 `dependencyManagement` 방식 금지)
- **`id(Plugins.spring_boot)` 미사용**: `cassandra-demo` 패턴 준수. `bootJar`/`jar` 태스크 설정 불필요
- **`configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 패턴 필수**
- **settings.gradle.kts 변경 불필요**: `includeModules("spring-boot3"/"spring-boot4")` 자동 등록
- **Libs.kt 상수 확인 완료**: `exposed_spring_boot_starter`, `exposed_spring_boot4_starter`, `exposed_spring_transaction`, `exposed_spring7_transaction`, `exposed_migration_jdbc` 모두 존재
- **experimental build.gradle.kts 차이점**:
  - JDBC: `id(Plugins.spring_boot)` 사용 + `project(":exposed-jdbc-spring-data")` -> 제거/변경 필요
  - R2DBC: `id(Plugins.spring_boot)` 사용 + `project(":exposed-r2dbc-spring-data")` + `Libs.bluetape4k_r2dbc` + `Libs.bluetape4k_coroutines` -> 변경 필요
- **패키지명 유지**: `io.bluetape4k.examples.exposed.mvc` (JDBC), `io.bluetape4k.examples.exposed.webflux` (R2DBC)

---

## 태스크 목록

### Phase 1: Boot 3 JDBC Demo

#### T1.1: `spring-boot3/exposed-jdbc-demo/build.gradle.kts` 작성

- **complexity: medium**
- **목표**: Boot 3 JDBC MVC 데모의 Gradle 빌드 파일 신규 작성
- **입력**: experimental `exposed-jdbc-spring-data-mvc-demo/build.gradle.kts` (참조만), spec 섹션 4.1
- **출력**: `spring-boot3/exposed-jdbc-demo/build.gradle.kts`
- **수행 방법**:
  1. `spring-boot3/exposed-jdbc-demo/` 디렉토리 생성
  2. spec 섹션 4.1 기준으로 `build.gradle.kts` 작성:
     ```kotlin
     plugins {
         kotlin("plugin.spring")
     }

     configurations {
         testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
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
     ```
  3. `id(Plugins.spring_boot)` 절대 미사용 확인
  4. `exposed_spring_boot_starter` (Boot 3용) 사용 확인 (`exposed_spring_boot4_starter` 아님)

#### T1.2: `spring-boot3/exposed-jdbc-demo/` 소스 복사

- **complexity: low**
- **목표**: experimental JDBC demo 소스를 Boot 3 대상 디렉토리에 그대로 복사
- **입력**: `bluetape4k-experimental/examples/exposed-jdbc-spring-data-mvc-demo/src/`
- **출력**: `spring-boot3/exposed-jdbc-demo/src/` (전체 구조)
- **수행 방법**:
  1. 디렉토리 구조 생성:
     ```
     spring-boot3/exposed-jdbc-demo/src/
     ├── main/kotlin/io/bluetape4k/examples/exposed/mvc/
     │   ├── DemoApplication.kt
     │   ├── config/DataInitializer.kt
     │   ├── controller/ProductController.kt
     │   ├── domain/ProductEntity.kt
     │   └── repository/ProductJdbcRepository.kt
     ├── main/resources/application.yml
     ├── test/kotlin/io/bluetape4k/examples/exposed/mvc/
     │   ├── ProductJdbcRepositoryTest.kt
     │   └── ProductControllerTest.kt
     └── test/resources/
         ├── junit-platform.properties
         └── logback-test.xml
     ```
  2. `cp -r` 로 전체 소스 복사 (`.kt` 파일 변경 없음)
  3. 패키지명 `io.bluetape4k.examples.exposed.mvc` 유지 확인
- **검증**: 파일 10개 존재 확인 (main 5 + resources 1 + test 2 + test resources 2)

#### T1.3: Boot 3 JDBC Demo 빌드/테스트 검증

- **complexity: medium**
- **목표**: Boot 3 JDBC 데모 컴파일 및 테스트 통과 확인
- **입력**: T1.1 + T1.2 결과물
- **출력**: 빌드/테스트 결과
- **수행 방법**:
  1. 컴파일 검증:
     ```bash
     ./gradlew :bluetape4k-spring-boot3-exposed-jdbc-demo:compileKotlin
     ```
  2. 테스트 실행:
     ```bash
     ./gradlew :bluetape4k-spring-boot3-exposed-jdbc-demo:test
     ```
  3. 예상 테스트 수: 28개 (Repository 21 + Controller 7)
- **autoconfiguration 호환성 검증** (추가 사전 검증):
  - `.kt`, `.yml` 파일에서 `org.jetbrains.exposed.spring.autoconfigure` 패키지 직접 참조 여부 grep
  - 직접 참조 없으면 `exposed_spring_boot_starter` 사용 가능
  - `exposed_spring_transaction`은 `bluetape4k-spring-boot3-exposed-jdbc`가 `api()`로 transitive 제공하므로 명시 불필요
- **실패 시 대응**:
  - `exposed_spring_boot_starter` 버전 호환성 확인
  - `DemoApplication.kt`에서 `exposed_spring_boot4_starter` 클래스 참조 시 -> `exposed_spring_boot_starter`로 변경
  - `MigrationUtils` import 확인 (`exposed_migration_jdbc` 의존성)

---

### Phase 2: Boot 3 R2DBC Demo

#### T2.1: `spring-boot3/exposed-r2dbc-demo/build.gradle.kts` 작성

- **complexity: medium**
- **목표**: Boot 3 R2DBC WebFlux 데모의 Gradle 빌드 파일 신규 작성
- **입력**: experimental `exposed-r2dbc-spring-data-webflux-demo/build.gradle.kts` (참조만), spec 섹션 4.2
- **출력**: `spring-boot3/exposed-r2dbc-demo/build.gradle.kts`
- **수행 방법**:
  1. `spring-boot3/exposed-r2dbc-demo/` 디렉토리 생성
  2. spec 섹션 4.2 기준으로 `build.gradle.kts` 작성:
     ```kotlin
     plugins {
         kotlin("plugin.spring")
     }

     configurations {
         testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
     }

     dependencies {
         implementation(project(":bluetape4k-spring-boot3-exposed-r2dbc"))

         implementation(Libs.exposed_r2dbc)
         implementation(Libs.exposed_java_time)

         runtimeOnly(Libs.r2dbc_h2)
         runtimeOnly(Libs.h2_v2)

         implementation(Libs.springBootStarter("webflux"))
         testImplementation(Libs.springBootStarter("test"))

         implementation(project(":bluetape4k-coroutines"))
         implementation(Libs.kotlinx_coroutines_core)
         implementation(Libs.kotlinx_coroutines_reactor)
         testImplementation(Libs.kotlinx_coroutines_test)
     }
     ```
  3. experimental의 `Libs.bluetape4k_r2dbc` -> `project(":bluetape4k-spring-boot3-exposed-r2dbc")` transitive로 대체 확인
  4. `Libs.bluetape4k_coroutines` -> `project(":bluetape4k-coroutines")` 변경 확인

#### T2.2: `spring-boot3/exposed-r2dbc-demo/` 소스 복사

- **complexity: low**
- **목표**: experimental R2DBC demo 소스를 Boot 3 대상 디렉토리에 그대로 복사
- **입력**: `bluetape4k-experimental/examples/exposed-r2dbc-spring-data-webflux-demo/src/`
- **출력**: `spring-boot3/exposed-r2dbc-demo/src/` (전체 구조)
- **수행 방법**:
  1. 디렉토리 구조 생성:
     ```
     spring-boot3/exposed-r2dbc-demo/src/
     ├── main/kotlin/io/bluetape4k/examples/exposed/webflux/
     │   ├── WebfluxDemoApplication.kt
     │   ├── config/DataInitializer.kt
     │   ├── config/ExposedR2dbcConfig.kt
     │   ├── controller/ProductController.kt
     │   ├── domain/ProductEntity.kt
     │   └── repository/ProductR2dbcRepository.kt
     ├── main/resources/application.yml
     ├── test/kotlin/io/bluetape4k/examples/exposed/webflux/
     │   ├── ProductR2dbcRepositoryTest.kt
     │   └── ProductControllerTest.kt
     └── test/resources/
         ├── junit-platform.properties
         └── logback-test.xml
     ```
  2. `cp -r` 로 전체 소스 복사 (`.kt` 파일 변경 없음)
  3. 패키지명 `io.bluetape4k.examples.exposed.webflux` 유지 확인
- **검증**: 파일 11개 존재 확인 (main 6 + resources 1 + test 2 + test resources 2)

#### T2.3: Boot 3 R2DBC Demo 빌드/테스트 검증

- **complexity: medium**
- **목표**: Boot 3 R2DBC 데모 컴파일 및 테스트 통과 확인
- **입력**: T2.1 + T2.2 결과물
- **출력**: 빌드/테스트 결과
- **수행 방법**:
  1. 컴파일 검증:
     ```bash
     ./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-demo:compileKotlin
     ```
  2. 테스트 실행:
     ```bash
     ./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-demo:test
     ```
  3. 예상 테스트 수: 23개 (Repository 16 + Controller 7)
- **실패 시 대응**:
  - `R2dbcDatabase.connect()` Bean 등록 확인 (`ExposedR2dbcConfig.kt`)
  - `HasIdentifier<Long>` import 확인 (bluetape4k-exposed-core transitive)
  - `suspendTransaction` import 확인 (`exposed_r2dbc` 의존성)
  - `WebTestClient` 미발견 -> `springBootStarter("test")` + WebFlux 자동 포함 확인

---

### Phase 3: Boot 4 JDBC Demo

#### T3.1: `spring-boot4/exposed-jdbc-demo/build.gradle.kts` 작성

- **complexity: medium**
- **목표**: Boot 4 JDBC MVC 데모의 Gradle 빌드 파일 신규 작성
- **입력**: T1.1 결과물 기반, spec 섹션 4.3
- **출력**: `spring-boot4/exposed-jdbc-demo/build.gradle.kts`
- **수행 방법**:
  1. `spring-boot4/exposed-jdbc-demo/` 디렉토리 생성
  2. spec 섹션 4.3 기준으로 `build.gradle.kts` 작성:
     ```kotlin
     plugins {
         kotlin("plugin.spring")
     }

     configurations {
         testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
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
     ```
  3. **핵심 차이**: `implementation(platform(Libs.spring_boot4_dependencies))` 첫 줄 + `exposed_spring_boot4_starter`
  4. `project(":bluetape4k-spring-boot4-exposed-jdbc")` 참조 확인 (Boot 3은 `-boot3-`)

#### T3.2: `spring-boot4/exposed-jdbc-demo/` 소스 복사

- **complexity: low**
- **목표**: Boot 3 JDBC demo와 동일한 소스를 Boot 4 대상 디렉토리에 복사
- **입력**: `bluetape4k-experimental/examples/exposed-jdbc-spring-data-mvc-demo/src/` (또는 T1.2 결과물)
- **출력**: `spring-boot4/exposed-jdbc-demo/src/` (T1.2와 동일 구조)
- **수행 방법**:
  1. T1.2와 동일한 디렉토리 구조 생성
  2. `cp -r` 로 전체 소스 복사 (`.kt` 파일 변경 없음)
  3. Spring Boot 4의 API는 Boot 3과 동일 패키지 네임스페이스이므로 Kotlin 소스 변경 불필요
- **검증**: 파일 10개 존재 확인

#### T3.3: Boot 4 JDBC Demo 빌드/테스트 검증

- **complexity: medium**
- **목표**: Boot 4 JDBC 데모 컴파일 및 테스트 통과 확인
- **입력**: T3.1 + T3.2 결과물
- **출력**: 빌드/테스트 결과
- **수행 방법**:
  1. 컴파일 검증:
     ```bash
     ./gradlew :bluetape4k-spring-boot4-exposed-jdbc-demo:compileKotlin
     ```
  2. 테스트 실행:
     ```bash
     ./gradlew :bluetape4k-spring-boot4-exposed-jdbc-demo:test
     ```
  3. 예상 테스트 수: 28개 (Repository 21 + Controller 7)
- **실패 시 대응**:
  - Boot 4 BOM 충돌 -> `platform()` 적용 방식 재확인
  - `exposed_spring_boot4_starter` AutoConfiguration 클래스명 확인
  - `MigrationUtils` Boot 4 호환성 확인

---

### Phase 4: Boot 4 R2DBC Demo

#### T4.1: `spring-boot4/exposed-r2dbc-demo/build.gradle.kts` 작성

- **complexity: medium**
- **목표**: Boot 4 R2DBC WebFlux 데모의 Gradle 빌드 파일 신규 작성
- **입력**: T2.1 결과물 기반, spec 섹션 4.4
- **출력**: `spring-boot4/exposed-r2dbc-demo/build.gradle.kts`
- **수행 방법**:
  1. `spring-boot4/exposed-r2dbc-demo/` 디렉토리 생성
  2. spec 섹션 4.4 기준으로 `build.gradle.kts` 작성:
     ```kotlin
     plugins {
         kotlin("plugin.spring")
     }

     configurations {
         testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
     }

     dependencies {
         implementation(platform(Libs.spring_boot4_dependencies))

         implementation(project(":bluetape4k-spring-boot4-exposed-r2dbc"))

         implementation(Libs.exposed_r2dbc)
         implementation(Libs.exposed_java_time)

         runtimeOnly(Libs.r2dbc_h2)
         runtimeOnly(Libs.h2_v2)

         implementation(Libs.springBootStarter("webflux"))
         testImplementation(Libs.springBootStarter("test"))

         implementation(project(":bluetape4k-coroutines"))
         implementation(Libs.kotlinx_coroutines_core)
         implementation(Libs.kotlinx_coroutines_reactor)
         testImplementation(Libs.kotlinx_coroutines_test)
     }
     ```
  3. **핵심 차이**: `implementation(platform(Libs.spring_boot4_dependencies))` + `project(":bluetape4k-spring-boot4-exposed-r2dbc")`

#### T4.2: `spring-boot4/exposed-r2dbc-demo/` 소스 복사

- **complexity: low**
- **목표**: Boot 3 R2DBC demo와 동일한 소스를 Boot 4 대상 디렉토리에 복사
- **입력**: `bluetape4k-experimental/examples/exposed-r2dbc-spring-data-webflux-demo/src/` (또는 T2.2 결과물)
- **출력**: `spring-boot4/exposed-r2dbc-demo/src/` (T2.2와 동일 구조)
- **수행 방법**:
  1. T2.2와 동일한 디렉토리 구조 생성
  2. `cp -r` 로 전체 소스 복사 (`.kt` 파일 변경 없음)
  3. Spring Boot 4의 API는 Boot 3과 동일 패키지 네임스페이스이므로 Kotlin 소스 변경 불필요
- **검증**: 파일 11개 존재 확인

#### T4.3: Boot 4 R2DBC Demo 빌드/테스트 검증

- **complexity: medium**
- **목표**: Boot 4 R2DBC 데모 컴파일 및 테스트 통과 확인
- **입력**: T4.1 + T4.2 결과물
- **출력**: 빌드/테스트 결과
- **수행 방법**:
  1. 컴파일 검증:
     ```bash
     ./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-demo:compileKotlin
     ```
  2. 테스트 실행:
     ```bash
     ./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-demo:test
     ```
  3. 예상 테스트 수: 23개 (Repository 16 + Controller 7)
- **실패 시 대응**:
  - Boot 4 BOM R2DBC 버전 충돌 -> `r2dbc_h2` 버전 확인
  - `ExposedR2dbcConfig.kt` Bean 등록 호환성 확인
  - `WebTestClient` Boot 4 호환성 확인

---

### Phase 5: 정리

#### T5.1: README.md 작성 (4개)

- **complexity: low**
- **목표**: 각 데모 모듈의 사용법 문서 작성
- **입력**: spec 전체
- **출력**:
  - `spring-boot3/exposed-jdbc-demo/README.md`
  - `spring-boot3/exposed-r2dbc-demo/README.md`
  - `spring-boot4/exposed-jdbc-demo/README.md`
  - `spring-boot4/exposed-r2dbc-demo/README.md`
- **수행 방법**:
  1. 각 README에 포함할 내용:
     - 모듈명, 설명 (JDBC MVC / R2DBC WebFlux)
     - Spring Boot 버전 (3.x / 4.x)
     - 사용하는 라이브러리 모듈 (`bluetape4k-spring-boot3/4-exposed-jdbc/r2dbc`)
     - 빌드/테스트 명령어
     - 주요 API 엔드포인트 (CRUD)
     - 프로젝트 구조 설명
  2. experimental README 참고 (있으면)

#### T5.2: CLAUDE.md Architecture 섹션 업데이트

- **complexity: low**
- **목표**: 루트 CLAUDE.md에 데모 모듈 4개 추가 반영
- **입력**: 현재 CLAUDE.md
- **출력**: 업데이트된 CLAUDE.md
- **수행 방법**:
  1. `Spring Boot 3 (spring-boot3/)` 섹션에 추가:
     - `exposed-jdbc-demo`: Exposed JDBC MVC 데모 (Spring Data Repository CRUD)
     - `exposed-r2dbc-demo`: Exposed R2DBC WebFlux 데모 (suspend/Flow CRUD)
  2. `Spring Boot 4 (spring-boot4/)` 섹션에 동일하게 추가

#### T5.3: 전체 4개 모듈 최종 빌드 확인

- **complexity: medium**
- **목표**: 모든 데모 모듈의 빌드 + 테스트 일괄 검증
- **입력**: Phase 1~4 결과물
- **출력**: 최종 빌드/테스트 결과
- **수행 방법**:
  ```bash
  ./gradlew :bluetape4k-spring-boot3-exposed-jdbc-demo:build \
            :bluetape4k-spring-boot3-exposed-r2dbc-demo:build \
            :bluetape4k-spring-boot4-exposed-jdbc-demo:build \
            :bluetape4k-spring-boot4-exposed-r2dbc-demo:build
  ```
- **검증 기준**:
  - [ ] 4개 모듈 모두 컴파일 성공
  - [ ] 4개 모듈 모두 테스트 통과 (JDBC 28 + R2DBC 23) x 2 = 102개
  - [ ] CLAUDE.md 업데이트 완료
  - [ ] README.md 4개 모두 존재

---

## 태스크 의존성 그래프

```
Phase 1 (Boot 3 JDBC)              Phase 2 (Boot 3 R2DBC)
T1.1 (build.gradle.kts) ─┐         T2.1 (build.gradle.kts) ─┐
T1.2 (소스 복사)         ─┴─> T1.3  T2.2 (소스 복사)         ─┴─> T2.3

Phase 3 (Boot 4 JDBC)              Phase 4 (Boot 4 R2DBC)
T3.1 (build.gradle.kts) ─┐         T4.1 (build.gradle.kts) ─┐
T3.2 (소스 복사)         ─┴─> T3.3  T4.2 (소스 복사)         ─┴─> T4.3

Phase 5 (정리)
T1.3 + T2.3 + T3.3 + T4.3 ─> T5.1 (README) ─┐
                              T5.2 (CLAUDE.md) ┴─> T5.3 (최종 검증)
```

## 병렬화 가능 그룹

| 그룹 | 태스크 | 설명 |
|------|--------|------|
| A (Build 파일) | T1.1 + T2.1 + T3.1 + T4.1 | 4개 build.gradle.kts 동시 작성 가능 |
| B (소스 복사) | T1.2 + T2.2 + T3.2 + T4.2 | 4개 디렉토리 동시 복사 가능 |
| C (빌드 검증) | T1.3 + T2.3 + T3.3 + T4.3 | A+B 완료 후 동시 검증 가능 |
| D (문서) | T5.1 + T5.2 | C 완료 후 동시 작성 가능 |
| E (최종) | T5.3 | D 완료 후 실행 |

---

## 위험 요소 및 완화 방안

| 위험 | 확률 | 영향 | 완화 |
|------|------|------|------|
| experimental 소스에서 `exposed_spring_boot4_starter` 참조 (Boot 3에서 사용 불가) | 중간 | 컴파일 실패 | T1.3/T2.3에서 확인, Boot 3용 `exposed_spring_boot_starter`로 변경 |
| `Libs.bluetape4k_r2dbc` 참조 불가 (projects Libs.kt에 없음) | 낮음 (build.gradle.kts에서 제거 완료) | 빌드 실패 | spec 4.2/4.4에서 이미 제거됨, transitive로 해결 |
| Spring Boot 4 BOM `dependencyManagement` 방식 실수 | 높음 (미대응 시 KGP 충돌) | 빌드 실패 | `implementation(platform(...))` 방식 엄수 |
| R2DBC Demo의 H2 JDBC DataSource 누락 | 중간 | 런타임 에러 (DDL 생성 실패) | `runtimeOnly(Libs.h2_v2)` 명시 (JDBC + R2DBC 이중 설정) |
| logback-test.xml 패키지명 불일치 | 낮음 | 로그 미출력 (기능 무관) | 복사 후 패키지명 확인 |

---

## 요약

- **총 태스크**: 15개 (Phase 1: 3, Phase 2: 3, Phase 3: 3, Phase 4: 3, Phase 5: 3)
- **complexity 분포**: medium=9, low=6
- **생성 모듈**: 4개
  - `bluetape4k-spring-boot3-exposed-jdbc-demo`
  - `bluetape4k-spring-boot3-exposed-r2dbc-demo`
  - `bluetape4k-spring-boot4-exposed-jdbc-demo`
  - `bluetape4k-spring-boot4-exposed-r2dbc-demo`
- **신규 파일**: ~48개 (4 build.gradle.kts + 4 README.md + 40 소스/테스트/리소스)
- **소스 변경**: 없음 (`.kt` 파일 그대로 복사, `build.gradle.kts`만 신규 작성)
- **예상 시간**: 30분 ~ 1시간 (소스 복사 + 빌드 검증, API 호환성 이슈 없음)
- **위험도**: Low (데모 모듈이므로 소스 변경 없이 빌드 설정만 작성)
- **커밋 전략**: 4개 모듈 모두 완료 후 단일 커밋 (`feat(spring-data-demo): ...`)
- **핵심 원칙**:
  - 코드 이관만 수행 (기능 추가/리팩토링 없음)
  - `id(Plugins.spring_boot)` 미사용
  - Boot 4는 `implementation(platform(Libs.spring_boot4_dependencies))` 필수
  - `configurations { testImplementation.get().extendsFrom(...) }` 패턴 필수
  - `exposed_spring_transaction`은 `bluetape4k-spring-boot3-exposed-jdbc`가 `api()`로 transitive 제공
