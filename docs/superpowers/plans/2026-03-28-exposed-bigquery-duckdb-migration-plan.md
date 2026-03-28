# exposed-bigquery / exposed-duckdb 이관 실행 계획

**날짜**: 2026-03-28
**Spec**: `docs/superpowers/specs/2026-03-28-exposed-bigquery-duckdb-migration-design.md`
**소스**: `bluetape4k-experimental/data/exposed-bigquery/`, `bluetape4k-experimental/data/exposed-duckdb/`
**대상**: `bluetape4k-projects/data/exposed-bigquery/`, `bluetape4k-projects/data/exposed-duckdb/`

---

## 사전 검증 결과

- **Exposed 버전**: 양쪽 모두 `1.1.1` -- 호환성 문제 없음
- **Import 패턴**: 양쪽 모두 `org.jetbrains.exposed.v1.*` -- 소스 변경 불필요
- **Libs.kt 추가 필요**: `duckdb_jdbc`, `google_api_services_bigquery` (projects Libs.kt에 미존재)
- **settings.gradle.kts**: `includeModules("data", ...)` 자동 등록 -- 별도 수정 불필요
- **패키지명**: `io.bluetape4k.exposed.bigquery.*`, `io.bluetape4k.exposed.duckdb.*` 유지 (변경 없음)
- **DuckDB 테스트**: testcontainers 미사용 확인 -- `bluetape4k-testcontainers` 불필요
- **BigQuery `exposed_jdbc`**: `Database.connect()`, `transaction()` 런타임 사용 확인 -- `implementation` 필수
- **BigQuery `exposed_java_time`**: `JavaInstantColumnType` 런타임 사용 확인 -- `implementation` 필수
- **DAO 미사용**: 양쪽 모두 `exposed.dao` import 없음 -- `exposed_dao` 제거

### Critic 검토 반영 요약

| # | 우선순위 | 항목 | 조치 |
|---|---------|------|------|
| 1 | 높음 | BigQuery `exposed_jdbc` → `implementation` | `Database.connect()`, `transaction()` 런타임 필수 |
| 2 | 중간 | BigQuery `exposed_java_time` → `implementation` | `JavaInstantColumnType` 런타임 필수 |
| 3 | 중간 | `exposed_dao` 제거 | 양쪽 모두 DAO 미사용 확인 |
| 4 | 낮음 | DuckDB `bluetape4k-testcontainers` 제거 | 인메모리 테스트만 사용 확인 |

---

## 태스크 목록

### Task 1: Libs.kt에 `duckdb_jdbc`, `google_api_services_bigquery` 상수 추가

- **complexity: low**
- **파일**: `buildSrc/src/main/kotlin/Libs.kt`
- **작업**: JDBC 드라이버 섹션 (`h2_v2`, `clickhouse_jdbc` 근처)에 2개 상수 추가
  ```kotlin
  const val duckdb_jdbc = "org.duckdb:duckdb_jdbc:1.1.3"            // https://mvnrepository.com/artifact/org.duckdb/duckdb_jdbc

  // BigQuery REST API 클라이언트
  // https://mvnrepository.com/artifact/com.google.apis/google-api-services-bigquery
  const val google_api_services_bigquery = "com.google.apis:google-api-services-bigquery:v2-rev20240919-2.0.0"
  ```
- **좌표 검증**: experimental Libs.kt 1364~1367행과 동일
- **검증**: Gradle sync 성공

### Task 2: `data/exposed-duckdb/` 디렉토리 구조 생성

- **complexity: low**
- **작업**: 디렉토리 생성
  ```
  data/exposed-duckdb/
  └── src/
      ├── main/kotlin/io/bluetape4k/exposed/duckdb/
      │   └── dialect/
      └── test/kotlin/io/bluetape4k/exposed/duckdb/
          ├── domain/
          ├── insert/
          └── query/
  ```
- **검증**: 디렉토리 존재 확인

### Task 3: `data/exposed-bigquery/` 디렉토리 구조 생성

- **complexity: low**
- **작업**: 디렉토리 생성
  ```
  data/exposed-bigquery/
  └── src/
      ├── main/kotlin/io/bluetape4k/exposed/bigquery/
      │   └── dialect/
      └── test/kotlin/io/bluetape4k/exposed/bigquery/
          ├── domain/
          ├── insert/
          └── query/
  ```
- **검증**: 디렉토리 존재 확인

### Task 4: exposed-duckdb `build.gradle.kts` 작성

- **complexity: medium**
- **파일**: `data/exposed-duckdb/build.gradle.kts`
- **작업**: exposed-postgresql/exposed-mysql8 패턴 기반으로 작성
- **Critic 반영 사항**:
    - `exposed_dao` 제거 (DAO 미사용)
    - `exposed_jdbc` → `compileOnly` (표준 JDBC 방식이지만 소비자가 런타임에 추가)
    - `exposed_java_time` → `compileOnly`
    - `bluetape4k-testcontainers` 제거 (인메모리 테스트만 사용)
    - `testcontainers` 제거 (인메모리 테스트만 사용)
- **주요 구성**:
  ```kotlin
  tasks.test {
      jvmArgs("--enable-native-access=ALL-UNNAMED")
  }

  configurations {
      testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
  }

  dependencies {
      implementation(platform(Libs.exposed_bom))
      api(project(":bluetape4k-exposed-core"))
      compileOnly(Libs.exposed_jdbc)
      compileOnly(Libs.exposed_java_time)

      implementation(project(":bluetape4k-logging"))

      api(Libs.duckdb_jdbc)
      api(Libs.kotlinx_coroutines_core)

      testImplementation(project(":bluetape4k-junit5"))
      testImplementation(Libs.kotlinx_coroutines_test)

      testRuntimeOnly(Libs.hikaricp)
  }
  ```
- **검증**: Gradle sync 성공

### Task 5: exposed-bigquery `build.gradle.kts` 작성

- **complexity: high**
- **파일**: `data/exposed-bigquery/build.gradle.kts`
- **작업**: BigQuery는 JDBC가 아닌 REST API 방식이므로 다른 Exposed 모듈과 의존성 구조가 다름
- **Critic 반영 사항** (핵심):
    - `exposed_jdbc` → **`implementation`** (compileOnly 아님! `Database.connect()`, `transaction()` 런타임 필수)
    - `exposed_java_time` → **`implementation`** (`JavaInstantColumnType` 런타임 필수)
    - `exposed_dao` 제거 (DAO 미사용)
- **주요 구성**:
  ```kotlin
  configurations {
      testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
  }

  dependencies {
      implementation(platform(Libs.exposed_bom))
      api(project(":bluetape4k-exposed-core"))
      implementation(Libs.exposed_jdbc)        // CRITICAL: Database.connect(), transaction() 런타임 사용
      implementation(Libs.exposed_java_time)   // CRITICAL: JavaInstantColumnType 런타임 사용

      implementation(project(":bluetape4k-logging"))

      api(Libs.google_api_services_bigquery)
      api(Libs.kotlinx_coroutines_core)

      // H2 — BigQueryContext.create()가 내부 SQL 생성용 H2 DB를 사용
      implementation(Libs.h2_v2)

      testImplementation(project(":bluetape4k-junit5"))
      testImplementation(project(":bluetape4k-testcontainers"))
      testImplementation(Libs.kotlinx_coroutines_test)
      testImplementation(Libs.testcontainers_junit_jupiter)
      testImplementation(Libs.testcontainers_gcloud)
  }
  ```
- **참고**: 이 모듈은 JDBC로 직접 DB에 접속하지 않으므로 `exposed-jdbc-tests`, `hikaricp` 불필요
- **검증**: Gradle sync 성공

### Task 6: exposed-duckdb main 소스 5개 파일 복사

- **complexity: low**
- **소스**: `bluetape4k-experimental/data/exposed-duckdb/src/main/kotlin/io/bluetape4k/exposed/duckdb/`
- **대상**: `bluetape4k-projects/data/exposed-duckdb/src/main/kotlin/io/bluetape4k/exposed/duckdb/`
- **파일 목록**:
    1. `DuckDBDatabase.kt`
    2. `DuckDBConnectionWrapper.kt`
    3. `DuckDBExtensions.kt`
    4. `dialect/DuckDBDialect.kt`
    5. `dialect/DuckDBDialectMetadata.kt`
- **소스 변경**: 없음 (패키지명 동일, import 호환)
- **검증**: 파일 존재 + 패키지 선언 확인

### Task 7: exposed-duckdb test 소스 5개 파일 + resources 복사

- **complexity: low**
- **소스**: `bluetape4k-experimental/data/exposed-duckdb/src/test/`
- **대상**: `bluetape4k-projects/data/exposed-duckdb/src/test/`
- **kotlin 파일 목록**:
    1. `AbstractDuckDBTest.kt`
    2. `DuckDBExtensionsTest.kt`
    3. `domain/Events.kt`
    4. `insert/InsertTest.kt`
    5. `query/SelectTest.kt`
- **resources**: `test/resources/` 디렉토리가 있으면 함께 복사 (`logback-test.xml`, `junit-platform.properties` 등)
- **소스 변경**: 없음
- **검증**: 파일 존재 확인

### Task 8: exposed-duckdb 빌드 및 테스트 검증

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-exposed-duckdb:compileKotlin :bluetape4k-exposed-duckdb:compileTestKotlin
  ./gradlew :bluetape4k-exposed-duckdb:test
  ```
- **전제 조건**: 없음 (인메모리 테스트, Docker 불필요)
- **JVM 옵션**: `--enable-native-access=ALL-UNNAMED` 필수 (build.gradle.kts에 설정됨)
- **검증 기준**: 컴파일 성공 + 전체 테스트 통과
- **실패 시 대응**:
    - import 미해결 → Libs.kt 의존성 누락 확인
    - native access 에러 → `tasks.test { jvmArgs(...) }` 확인

### Task 9: exposed-bigquery main 소스 3개 파일 복사

- **complexity: low**
- **소스**: `bluetape4k-experimental/data/exposed-bigquery/src/main/kotlin/io/bluetape4k/exposed/bigquery/`
- **대상**: `bluetape4k-projects/data/exposed-bigquery/src/main/kotlin/io/bluetape4k/exposed/bigquery/`
- **파일 목록**:
    1. `BigQueryContext.kt`
    2. `BigQueryQueryExecutor.kt` (BigQueryResultRow 클래스 포함)
    3. `dialect/BigQueryDialect.kt`
- **소스 변경**: 없음
- **검증**: 파일 존재 + 패키지 선언 확인

### Task 10: exposed-bigquery test 소스 6개 파일 + resources 복사

- **complexity: low**
- **소스**: `bluetape4k-experimental/data/exposed-bigquery/src/test/`
- **대상**: `bluetape4k-projects/data/exposed-bigquery/src/test/`
- **kotlin 파일 목록**:
    1. `AbstractBigQueryTest.kt`
    2. `BigQueryEmulator.kt`
    3. `domain/Events.kt`
    4. `query/SelectQueryTest.kt`
    5. `query/SelectTest.kt`
    6. `insert/InsertTest.kt`
- **resources**: `test/resources/` 디렉토리가 있으면 함께 복사
- **소스 변경**: 없음
- **검증**: 파일 존재 확인

### Task 11: exposed-bigquery 빌드 및 테스트 검증

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-exposed-bigquery:compileKotlin :bluetape4k-exposed-bigquery:compileTestKotlin
  ./gradlew :bluetape4k-exposed-bigquery:test
  ```
- **전제 조건**: Docker 실행 중 (BigQuery Emulator Testcontainer)
- **검증 기준**: 컴파일 성공 + 전체 테스트 통과
- **실패 시 대응**:
    - `JavaInstantColumnType` 미발견 → `exposed_java_time`이 `implementation`인지 확인
    - `Database.connect()` 미발견 → `exposed_jdbc`가 `implementation`인지 확인
    - BigQuery Emulator 이미지 pull 실패 → Docker Hub/ghcr.io 접근 확인

### Task 12: README.md 복사 및 수정

- **complexity: low**
- **파일**: `data/exposed-bigquery/README.md`, `data/exposed-duckdb/README.md`
- **작업**:
    - experimental README.md 복사
    - 모듈명/artifact 참조를 `bluetape4k-exposed-bigquery`, `bluetape4k-exposed-duckdb`로 수정
    - 의존성 예시를 `project(":bluetape4k-exposed-bigquery")` 형태로 업데이트
    - 저장소 경로가 experimental을 가리키면 bluetape4k-projects로 변경
- **검증**: `experimental` 문자열 잔류 없는지 확인

### Task 13: CLAUDE.md 업데이트

- **complexity: low**
- **파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/CLAUDE.md`
- **작업**: `Architecture > Module Structure > Data Modules (data/)` 섹션에 추가
  ```markdown
  - **exposed-bigquery**: BigQuery 전용 Exposed 확장 — REST API 기반 실행 (JDBC 없음), H2로 SQL 생성, suspend/Flow API, 페이지네이션 자동 처리
  - **exposed-duckdb**: DuckDB 전용 Exposed 확장 — JDBC 래퍼, 커스텀 다이얼렉트, suspendTransaction/queryFlow 확장
  ```
- **위치**: `exposed-mysql8` 항목 아래
- **검증**: CLAUDE.md diff 확인

### Task 14: experimental 모듈 삭제

- **complexity: low**
- **대상**: `bluetape4k-experimental/data/exposed-bigquery/`, `bluetape4k-experimental/data/exposed-duckdb/`
- **작업**:
    1. 두 디렉토리 삭제 (`rm -rf`)
    2. experimental 저장소에서 커밋: `chore: exposed-bigquery, exposed-duckdb 모듈을 bluetape4k-projects로 이관 완료하여 삭제`
- **주의**: experimental settings.gradle.kts가 자동 등록 방식이면 디렉토리 삭제만으로 충분
- **검증**: experimental 빌드 성공 확인

### Task 15: 최종 빌드 검증

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-exposed-duckdb:build :bluetape4k-exposed-bigquery:build
  ./gradlew detekt
  ```
- **검증 기준**:
    - [ ] `compileKotlin` 성공 (양쪽)
    - [ ] 전체 테스트 통과 (양쪽)
    - [ ] detekt 에러 없음
    - [ ] Libs.kt에 `duckdb_jdbc`, `google_api_services_bigquery` 추가 확인
    - [ ] CLAUDE.md에 `exposed-bigquery`, `exposed-duckdb` 항목 추가 확인
    - [ ] 기존 모듈 빌드 영향 없음 (`./gradlew build -x test` 전체 컴파일)

---

## 태스크 의존성 그래프

```
Task 1 (Libs.kt) ─────────────────────────────────────────────────────┐
                                                                      │
Task 2 (duckdb 디렉토리) ──┐                                           │
                           ├─> Task 4 (duckdb build.gradle.kts) ──┐   │
Task 6 (duckdb main 복사) ─┤                                       │   │
Task 7 (duckdb test 복사) ─┘                                       ├─> Task 8 (duckdb 빌드/테스트)
                                                                  │
Task 3 (bigquery 디렉토리) ──┐                                      │
                             ├─> Task 5 (bigquery build.gradle.kts) ──┐
Task 9 (bigquery main 복사) ─┤                                        ├─> Task 11 (bigquery 빌드/테스트)
Task 10 (bigquery test 복사) ┘                                        │
                                                                      │
Task 12 (README) ──── 독립 (디렉토리 생성 후 가능)                        │
Task 13 (CLAUDE.md) ── 독립 (Task 8 + Task 11 이후 권장)              │
Task 14 (삭제) ──────── Task 15 이후                                   │
Task 15 (최종 빌드) ─── Task 8 + Task 11 + Task 13 완료 후             ┘
```

## 병렬화 가능 그룹

| 그룹 | 태스크 | 설명 |
|------|--------|------|
| A (인프라 준비) | Task 1 + Task 2 + Task 3 | 동시 실행 가능 |
| B (DuckDB 파일) | Task 4 + Task 6 + Task 7 | A 완료 후 동시 실행 가능 |
| C (BigQuery 파일) | Task 5 + Task 9 + Task 10 | A 완료 후 동시 실행 가능 (B와도 병렬) |
| D (DuckDB 검증) | Task 8 | B 완료 후 (Docker 불필요, 먼저 실행) |
| E (BigQuery 검증) | Task 11 | C 완료 후 (Docker 필요) |
| F (문서) | Task 12 + Task 13 | D + E 완료 후 동시 실행 가능 |
| G (정리) | Task 15 → Task 14 | F 완료 후 순차 실행 |

---

## 위험 요소 및 완화 방안

| 위험 | 확률 | 완화 |
|------|------|------|
| Docker 미실행으로 BigQuery 테스트 실패 | 중간 | DuckDB 먼저 검증, BigQuery는 컴파일만으로 1차 검증 가능 |
| BigQuery Emulator 이미지 pull 지연 | 낮음 | 로컬 캐시 확인, 필요시 사전 pull |
| `exposed_jdbc` compileOnly 오류 (BigQuery) | **해소** | Critic 반영: `implementation`으로 변경 완료 |
| `exposed_java_time` 런타임 에러 (BigQuery) | **해소** | Critic 반영: `implementation`으로 변경 완료 |
| DuckDB native library 로딩 실패 | 낮음 | `--enable-native-access=ALL-UNNAMED` JVM 옵션 설정됨 |
| exposed-core/exposed-jdbc API 불일치 | 매우 낮음 | 양쪽 Exposed 1.1.1 동일, import 패턴 동일 확인 완료 |

---

## 요약

- **총 태스크**: 15개
- **complexity 분포**: high=1, medium=5, low=9
- **예상 시간**: 40분 ~ 1시간 (테스트 실행 포함)
- **위험도**: Low (패키지 변경 없음, Exposed 버전 동일, 의존성만 재구성)
- **핵심 결정** (Critic 반영):
    - BigQuery: `exposed_jdbc` = `implementation` (런타임 필수)
    - BigQuery: `exposed_java_time` = `implementation` (런타임 필수)
    - 양쪽: `exposed_dao` 제거 (미사용)
    - DuckDB: `bluetape4k-testcontainers` 제거 (인메모리 전용)
- **실행 순서**: DuckDB 먼저 (외부 의존성 없음) → BigQuery (Docker 필요)
