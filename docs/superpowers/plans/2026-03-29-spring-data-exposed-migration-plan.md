# Spring Data Exposed 모듈 이관 실행 계획

**날짜**: 2026-03-29
**Spec**: `docs/superpowers/specs/2026-03-29-spring-data-exposed-migration-design.md`
**소스**: `bluetape4k-experimental/spring-data/exposed-jdbc-spring-data/`, `bluetape4k-experimental/spring-data/exposed-r2dbc-spring-data/`
**대상**: `bluetape4k-projects/spring-boot3/`, `bluetape4k-projects/spring-boot4/`

---

## 사전 검증 결과

- **Exposed 버전**: experimental/projects 동일 (`1.1.1`) -- 호환성 문제 없음
- **Import 패턴**: 양쪽 모두 `org.jetbrains.exposed.v1.*` -- 소스 변경 불필요
- **Spring 6 vs 7 차이**: `spring7.transaction.SpringTransactionManager` -> `spring.transaction.SpringTransactionManager` (JDBC 모듈 2개 파일, R2DBC는 해당 없음)
- **Spring Data 3.x vs 4.x 차이**: `getEntityInformation(Class<T>)` primary vs `getEntityInformation(RepositoryMetadata)` primary (Factory 파일 2개)
- **의존성 참조 방식**: 내부 모듈은 `project(":bluetape4k-*")` 사용 (`Libs.bluetape4k_*` 상수는 projects Libs.kt에 없음)
  - `Libs.bluetape4k_logging` → `project(":bluetape4k-logging")`
  - `Libs.bluetape4k_junit5` → `project(":bluetape4k-junit5")`
  - `Libs.bluetape4k_exposed_core` → `project(":bluetape4k-exposed-core")`
  - `Libs.bluetape4k_exposed_jdbc` → `project(":bluetape4k-exposed-jdbc")`
  - `Libs.bluetape4k_exposed_r2dbc` → `project(":bluetape4k-exposed-r2dbc")`
  - `Libs.bluetape4k_exposed_jdbc_tests` → `project(":bluetape4k-exposed-jdbc-tests")`
  - `Libs.bluetape4k_exposed_r2dbc_tests` → `project(":bluetape4k-exposed-r2dbc-tests")`
  - `Libs.bluetape4k_coroutines` → `project(":bluetape4k-coroutines")`
  - `Libs.bluetape4k_virtualthread_jdk21` → `project(":bluetape4k-virtualthread-jdk21")`
  - 외부 라이브러리(Exposed, Spring Data 등)는 여전히 `Libs.*` 사용
- **Libs.kt 추가**: 불필요 -- 기존 상수(`exposed_spring_transaction`, `exposed_spring7_transaction` 등) 모두 존재
- **settings.gradle.kts**: `includeModules("spring-boot3"/"spring-boot4", withBaseDir = true)` 자동 등록 -- 별도 수정 불필요
- **파일 수**: JDBC main 24개, test 7개 / R2DBC main 8개, test 5개

---

## 태스크 목록

### Phase 1: Spring Boot 4 모듈 (experimental 거의 그대로)

#### Task 1.1: `spring-boot4/exposed-jdbc-spring-data/` 디렉토리 생성 및 소스 복사

- **complexity: medium**
- **작업**:
  1. 디렉토리 구조 생성:
     ```
     spring-boot4/exposed-jdbc-spring-data/
     └── src/
         ├── main/kotlin/io/bluetape4k/spring/data/exposed/jdbc/
         ├── main/resources/META-INF/spring/
         ├── test/kotlin/io/bluetape4k/spring/data/exposed/jdbc/
         └── test/resources/
     ```
  2. `build.gradle.kts` 작성 (experimental 기반, 변경사항):
     - `implementation(platform(Libs.spring_boot4_dependencies))` 추가 (KGP 충돌 방지)
     - `exposed_spring7_transaction` 유지 (Spring 7용)
     - `Libs.bluetape4k_*` 참조 전체를 `project(":bluetape4k-*")` 로 변경 (projects Libs.kt에 미존재)
     - `Libs.bluetape4k_virtualthread_jdk25` → `project(":bluetape4k-virtualthread-jdk21")` (프로젝트 표준 JDK 21)
     - `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 추가
       (**이유**: `compileOnly(Libs.springBoot("autoconfigure"))` 등 compileOnly 의존성을 테스트 컴파일 시에도 접근하기 위함)
  3. `src/main/kotlin/` 전체 복사 (24개 파일, 변경 없음)
  4. `src/main/resources/META-INF/spring/AutoConfiguration.imports` 복사
  5. `src/test/kotlin/` 전체 복사 (7개 파일)
     - `AbstractExposedJdbcRepositoryTest.kt`: `jdk25` import 없으면 변경 없음 (test 의존성에서 처리)
  6. `src/test/resources/` 복사 (`application.yml`, `junit-platform.properties`, `logback-test.xml`)
- **검증**: 디렉토리 구조 및 파일 존재 확인

#### Task 1.2: `spring-boot4/exposed-r2dbc-spring-data/` 디렉토리 생성 및 소스 복사

- **complexity: medium**
- **작업**:
  1. 디렉토리 구조 생성:
     ```
     spring-boot4/exposed-r2dbc-spring-data/
     └── src/
         ├── main/kotlin/io/bluetape4k/spring/data/exposed/r2dbc/
         ├── main/resources/META-INF/spring/
         ├── test/kotlin/io/bluetape4k/spring/data/exposed/r2dbc/
         └── test/resources/
     ```
  2. `build.gradle.kts` 작성 (experimental 기반, 변경사항):
     - `implementation(platform(Libs.spring_boot4_dependencies))` 추가
     - `api(project(":exposed-jdbc-spring-data"))` -> `api(project(":bluetape4k-spring-boot4-exposed-jdbc-spring-data"))`
     - `bluetape4k_virtualthread_jdk25` -> `bluetape4k_virtualthread_jdk21`
     - `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 추가
  3. `src/main/kotlin/` 전체 복사 (8개 파일, 변경 없음)
  4. `src/main/resources/META-INF/spring/AutoConfiguration.imports` 복사
  5. `src/test/kotlin/` 전체 복사 (5개 파일, 변경 없음)
  6. `src/test/resources/` 복사 (`junit-platform.properties`, `logback-test.xml`)
- **검증**: 디렉토리 구조 및 파일 존재 확인

#### Task 1.3: Spring Boot 4 컴파일 검증

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-spring-boot4-exposed-jdbc-spring-data:compileKotlin :bluetape4k-spring-boot4-exposed-jdbc-spring-data:compileTestKotlin
  ./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-spring-data:compileKotlin :bluetape4k-spring-boot4-exposed-r2dbc-spring-data:compileTestKotlin
  ```
- **검증 기준**: 컴파일 성공, 에러 0
- **실패 시 대응**:
  - Spring Boot 4 BOM 충돌 -> `platform()` 적용 방식 확인
  - Spring Data 4.x API 시그니처 -> `getEntityInformation(RepositoryMetadata)` primary 확인
  - R2DBC 모듈 -> JDBC 모듈 프로젝트 참조 경로 확인

#### Task 1.4: Spring Boot 4 테스트 실행

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-spring-boot4-exposed-jdbc-spring-data:test
  ./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-spring-data:test
  ```
- **전제 조건**: Docker 실행 중 (MultiDb 테스트용 Testcontainers)
- **검증 기준**: 전체 테스트 통과
- **실패 시 대응**:
  - H2 인메모리 테스트 실패 -> AutoConfiguration 등록 확인 (`AutoConfiguration.imports`)
  - `SpringTransactionManager` 빈 충돌 -> `@ConditionalOnMissingBean` 조건 확인
  - MultiDb 테스트 실패 -> Docker 상태, Testcontainers JDBC/R2DBC 드라이버 확인

---

### Phase 2: Spring Boot 3 모듈 (적응 필요)

#### Task 2.1: `spring-boot3/exposed-jdbc-spring-data/` 디렉토리 생성 및 소스 복사 + 적응

- **complexity: high**
- **작업**:
  1. Spring Boot 4 JDBC 모듈(Task 1.1)을 기반으로 복사
  2. `build.gradle.kts` 변경:
     - `implementation(platform(Libs.spring_boot4_dependencies))` **제거** (Spring Boot 3은 루트 BOM 적용)
     - `exposed_spring7_transaction` -> `exposed_spring_transaction` (Spring 6용)
  3. `ExposedSpringDataAutoConfiguration.kt` 변경:
     - `import org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager`
     - -> `import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager`
  4. `ExposedJdbcRepositoryFactory.kt` 변경:
     - `getEntityInformation(Class<T>)`에서 `@Deprecated` 어노테이션 제거 (Spring Data 3.x에서 primary)
     - `getEntityInformation(RepositoryMetadata)` 오버라이드 유지 (Spring Data 3.3+에서 존재)
  5. `AbstractExposedJdbcRepositoryTest.kt` 변경:
     - `import org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager`
     - -> `import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager`
- **변경 파일 목록**:
  | 파일 | 변경 내용 |
  |---|---|
  | `build.gradle.kts` | BOM 제거, `exposed_spring_transaction` 사용 |
  | `ExposedSpringDataAutoConfiguration.kt` | `spring7.transaction` -> `spring.transaction` import |
  | `ExposedJdbcRepositoryFactory.kt` | `@Deprecated` 제거 |
  | `AbstractExposedJdbcRepositoryTest.kt` | `spring7.transaction` -> `spring.transaction` import |
- **검증**: 변경 파일 4개의 diff 확인

#### Task 2.2: `spring-boot3/exposed-r2dbc-spring-data/` 디렉토리 생성 및 소스 복사 + 적응

- **complexity: high**
- **작업**:
  1. Spring Boot 4 R2DBC 모듈(Task 1.2)을 기반으로 복사
  2. `build.gradle.kts` 변경:
     - `implementation(platform(Libs.spring_boot4_dependencies))` **제거**
     - `api(project(":bluetape4k-spring-boot4-exposed-jdbc-spring-data"))` -> `api(project(":bluetape4k-spring-boot3-exposed-jdbc-spring-data"))`
     - `exposed_spring7_transaction` 직접 의존 있으면 제거 (JDBC 모듈에서 전이)
  3. `ExposedR2dbcRepositoryFactory.kt` 변경 -- **핵심 적응**:
     - Spring Data 3.x에서 `getEntityInformation(Class<T>)` 가 abstract이므로 **반드시 추가**:
       ```kotlin
       override fun <T : Any, ID : Any> getEntityInformation(domainClass: Class<T>): EntityInformation<T, ID> =
           StaticEntityInformation(domainClass, resolveIdType(domainClass)) as EntityInformation<T, ID>
       ```
     - `resolveIdType()` 헬퍼 메서드 추가 (RepositoryMetadata 없이 Class에서 ID 타입 추론)
     - 또는 간단하게 `Any::class.java as Class<ID>` 사용 (StaticEntityInformation 패턴)
- **변경 파일 목록**:
  | 파일 | 변경 내용 |
  |---|---|
  | `build.gradle.kts` | BOM 제거, JDBC 모듈 참조 Boot 3으로 변경 |
  | `ExposedR2dbcRepositoryFactory.kt` | `getEntityInformation(Class<T>)` abstract 오버라이드 추가 |
- **검증**: 변경 파일 2개의 diff 확인

#### Task 2.3: Spring Data 3.x API 호환성 검증 (컴파일)

- **complexity: high**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-spring-boot3-exposed-jdbc-spring-data:compileKotlin :bluetape4k-spring-boot3-exposed-jdbc-spring-data:compileTestKotlin
  ./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-spring-data:compileKotlin :bluetape4k-spring-boot3-exposed-r2dbc-spring-data:compileTestKotlin
  ```
- **검증 항목**:
  - [ ] `ValueExpressionDelegate` 존재 확인 (Spring Data 3.3+)
  - [ ] `RepositoryFactorySupport.getEntityInformation(RepositoryMetadata)` 존재 확인 (Spring Data 3.3+)
  - [ ] `AbstractQueryCreator.and(Part, S, Iterator)` 3-param 시그니처 확인
  - [ ] `ExposedR2dbcRepositoryFactory.getEntityInformation(Class<T>)` 컴파일 성공
- **실패 시 대응**:
  - `ValueExpressionDelegate` 미존재 -> `SpelExpressionParser` 기반 fallback (가능성 매우 낮음)
  - `getEntityInformation(RepositoryMetadata)` 미존재 -> Spring Data 3.3 미만일 경우 제거 (가능성 낮음, Boot 3.5.x는 Data 3.4+ 포함)
  - R2DBC Factory `getEntityInformation(Class<T>)` 컴파일 실패 -> `StaticEntityInformation` 생성자 파라미터 조정

#### Task 2.4: Spring Boot 3 테스트 실행

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-spring-boot3-exposed-jdbc-spring-data:test
  ./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-spring-data:test
  ```
- **검증 기준**: 전체 테스트 통과
- **실패 시 대응**:
  - `SpringTransactionManager` import 오류 -> `spring.transaction` vs `spring7.transaction` 확인
  - Exposed AutoConfiguration 충돌 -> `excludeName` 설정 확인 (Spring Boot 3 Exposed Starter 클래스명 차이)

---

### Phase 3: 문서 및 마무리

#### Task 3.1: 각 모듈 README.md 작성 (4개)

- **complexity: low**
- **파일**:
  - `spring-boot3/exposed-jdbc-spring-data/README.md`
  - `spring-boot3/exposed-r2dbc-spring-data/README.md`
  - `spring-boot4/exposed-jdbc-spring-data/README.md`
  - `spring-boot4/exposed-r2dbc-spring-data/README.md`
- **작업**: experimental README 기반, 모듈명/의존성/AutoConfiguration 설명, Spring Boot 버전 명시
- **검증**: README 내용 적절성 확인

#### Task 3.2: CLAUDE.md 업데이트

- **complexity: low**
- **파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/CLAUDE.md`
- **작업**: `Architecture > Module Structure` 섹션 업데이트
  - `Spring Modules (spring-boot3/)` 섹션에 추가:
    ```markdown
    - **exposed-jdbc-spring-data** (`bluetape4k-spring-boot3-exposed-jdbc-spring-data`): Exposed DAO Entity 기반 Spring Data JDBC Repository — PartTree 쿼리, QBE, Page/Sort 지원
    - **exposed-r2dbc-spring-data** (`bluetape4k-spring-boot3-exposed-r2dbc-spring-data`): Exposed R2DBC DSL 기반 코루틴 Spring Data Repository — suspend CRUD, Flow 지원
    ```
  - `Spring Boot 4 Modules (spring-boot4/)` 섹션에 동일하게 추가
- **검증**: CLAUDE.md diff 확인

#### Task 3.3: MultiDb 테스트 검증 (Testcontainers, 4개 모듈 전체)

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-spring-boot3-exposed-jdbc-spring-data:test --tests "*MultiDb*"
  ./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-spring-data:test --tests "*MultiDb*"
  ./gradlew :bluetape4k-spring-boot4-exposed-jdbc-spring-data:test --tests "*MultiDb*"
  ./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-spring-data:test --tests "*MultiDb*"
  ```
- **전제 조건**: Docker 실행 중
- **검증 기준**: MySQL, MariaDB, PostgreSQL JDBC + MySQL, PostgreSQL R2DBC 전체 통과

#### Task 3.4: 최종 빌드 검증

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-spring-boot3-exposed-jdbc-spring-data:build
  ./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-spring-data:build
  ./gradlew :bluetape4k-spring-boot4-exposed-jdbc-spring-data:build
  ./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-spring-data:build
  ```
- **검증 기준**:
  - [ ] 4개 모듈 모두 컴파일 성공
  - [ ] 4개 모듈 모두 테스트 통과
  - [ ] CLAUDE.md 업데이트 완료
  - [ ] README.md 4개 모두 존재

---

### Phase 4 (선택): Experimental 정리

#### Task 4.1: experimental 모듈 정리

- **complexity: low**
- **대상**: `bluetape4k-experimental/spring-data/exposed-jdbc-spring-data/`, `bluetape4k-experimental/spring-data/exposed-r2dbc-spring-data/`
- **작업**: deprecated 마킹 또는 디렉토리 삭제, experimental 커밋
- **주의**: experimental settings.gradle.kts 자동 등록이면 디렉토리 삭제만으로 충분
- **검증**: experimental 빌드 성공

---

## 태스크 의존성 그래프

```
Task 1.1 (Boot4 JDBC) ──┐
                         ├─> Task 1.3 (Boot4 컴파일) ──> Task 1.4 (Boot4 테스트)
Task 1.2 (Boot4 R2DBC) ─┘                                      │
                                                                v
Task 1.1 복사본 ─> Task 2.1 (Boot3 JDBC 적응) ──┐               │
                                                ├─> Task 2.3 (Boot3 컴파일) ──> Task 2.4 (Boot3 테스트)
Task 1.2 복사본 ─> Task 2.2 (Boot3 R2DBC 적응) ─┘                                      │
                                                                                       v
Task 3.1 (README) ────── Task 1.1 이후 병렬 가능                                         │
Task 3.2 (CLAUDE.md) ─── Task 2.4 이후 권장                                              │
                                                                                       v
                                                              Task 3.3 (MultiDb) ──> Task 3.4 (최종)
                                                                                       │
                                                                                       v
                                                                              Task 4.1 (정리)
```

## 병렬화 가능 그룹

| 그룹 | 태스크 | 설명 |
|------|--------|------|
| A (Boot 4 생성) | Task 1.1 + Task 1.2 | 동시 실행 가능 |
| B (Boot 4 검증) | Task 1.3 -> Task 1.4 | 순차 실행 |
| C (Boot 3 적응) | Task 2.1 + Task 2.2 | A 기반, 동시 실행 가능 |
| D (Boot 3 검증) | Task 2.3 -> Task 2.4 | 순차 실행 |
| E (문서) | Task 3.1 + Task 3.2 | B/D 완료 후 동시 실행 가능 |
| F (최종) | Task 3.3 -> Task 3.4 -> Task 4.1 | E 완료 후 순차 실행 |

---

## 위험 요소 및 완화 방안

| 위험 | 확률 | 영향 | 완화 |
|------|------|------|------|
| Spring Data 3.x `getEntityInformation(Class<T>)` abstract 미구현 (R2DBC) | 높음 (미대응 시 확실한 컴파일 오류) | 컴파일 실패 | Task 2.2에서 명시적 오버라이드 추가 |
| Spring Boot 4 BOM `dependencyManagement` 방식 적용 시 KGP 충돌 | 높음 (미대응 시 확실한 빌드 실패) | 빌드 실패 | `implementation(platform(...))` 방식 사용 |
| `ValueExpressionDelegate` Spring Data 3.x 미존재 | 매우 낮음 (3.3+에서 도입, Boot 3.5.x는 3.4+ 포함) | 컴파일 실패 | Task 2.3에서 검증, fallback 준비 |
| `spring.transaction` vs `spring7.transaction` import 누락 | 중간 | 런타임 ClassNotFoundException | Task 2.1 변경 파일 목록 체크리스트로 누락 방지 |
| Docker 미실행으로 MultiDb 테스트 실패 | 낮음 | 테스트 실패 (기능에 영향 없음) | H2 인메모리 테스트로 1차 검증 후 MultiDb는 별도 태스크 |
| Exposed AutoConfiguration 클래스명 Boot 3 vs 4 차이 | 낮음 | 테스트 시 빈 충돌 | `excludeName` 설정으로 Exposed 자체 AutoConfig 제외 |

---

## 요약

- **총 태스크**: 13개 (Phase 1: 4, Phase 2: 4, Phase 3: 4, Phase 4: 1)
- **complexity 분포**: high=3, medium=6, low=4
- **생성 모듈**: 4개
  - `bluetape4k-spring-boot3-exposed-jdbc-spring-data`
  - `bluetape4k-spring-boot3-exposed-r2dbc-spring-data`
  - `bluetape4k-spring-boot4-exposed-jdbc-spring-data`
  - `bluetape4k-spring-boot4-exposed-r2dbc-spring-data`
- **파일 수**: main 32개 (JDBC 24 + R2DBC 8) x 2 (Boot 3/4) = 64개 + build/test/resources
- **예상 시간**: 1.5 ~ 2.5시간 (API 호환성 검증 + MultiDb 테스트 포함)
- **위험도**: Medium (Spring Data 3.x API 적응이 핵심 리스크)
- **핵심 결정**:
  - Fork & Adapt (코드 복사) 방식 채택 (차이 3~5개 파일)
  - Boot 4 먼저 (experimental 거의 그대로), Boot 3은 Boot 4 기반 적응
  - 동일 패키지 `io.bluetape4k.spring.data.exposed.*` 유지
