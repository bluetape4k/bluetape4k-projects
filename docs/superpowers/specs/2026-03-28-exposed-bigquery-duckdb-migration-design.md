# exposed-bigquery / exposed-duckdb 이관 설계 Spec

**날짜**: 2026-03-28
**소스**: `bluetape4k-experimental/data/exposed-bigquery/`, `bluetape4k-experimental/data/exposed-duckdb/`
**대상**: `bluetape4k-projects/data/exposed-bigquery/`, `bluetape4k-projects/data/exposed-duckdb/`
**참고 패턴**: `bluetape4k-projects/data/exposed-mysql8/`, `bluetape4k-projects/data/exposed-postgresql/`

---

## 1. 개요

bluetape4k-experimental 저장소의 `exposed-bigquery`와 `exposed-duckdb` 모듈을 bluetape4k-projects로 이관한다.
두 모듈 모두 Exposed DSL을 활용하되, 각각 고유한 접근 방식을 취한다:

- **exposed-bigquery**: JDBC 없이 BigQuery REST API로 실행. H2(PostgreSQL 모드)로 SQL 문자열만 생성
- **exposed-duckdb**: 표준 JDBC 방식이지만, DuckDB JDBC 드라이버의 제약(FK 캐싱 미지원 등)을 래퍼로 해결

---

## 2. 현재 상태

### 2.1 exposed-bigquery (main: 3파일 498줄, test: 6파일 ~622줄)

| 파일 | 역할 | LOC |
|------|------|-----|
| `BigQueryContext.kt` | H2 SQL 생성 + BigQuery REST 실행기, suspend/Flow API, 페이지네이션 자동 처리 | 364 |
| `BigQueryQueryExecutor.kt` | Query 결과 타입 변환기 + `BigQueryResultRow` 클래스 (타입 안전 컬럼 접근) | 97 |
| `dialect/BigQueryDialect.kt` | PostgreSQLDialect 상속 | 37 |

테스트:

| 파일 | 역할 |
|------|------|
| `AbstractBigQueryTest.kt` | BigQuery Emulator Testcontainer + 공통 설정 |
| `BigQueryEmulator.kt` | BigQuery Emulator Testcontainer 정의 |
| `domain/Events.kt` | 테스트용 테이블 정의 |
| `query/SelectQueryTest.kt` | BigQueryQueryExecutor 기반 SELECT 테스트 |
| `query/SelectTest.kt` | BigQueryContext 기반 SELECT 테스트 |
| `insert/InsertTest.kt` | INSERT 테스트 |

### 2.2 exposed-duckdb (main: 5파일 223줄, test: 5파일 ~390줄)

| 파일 | 역할 | LOC |
|------|------|-----|
| `DuckDBDatabase.kt` | 연결 팩토리 (`inMemory()`, `file()`) + 다이얼렉트 등록 | 91 |
| `DuckDBConnectionWrapper.kt` | JDBC 1.1.3 호환성 래퍼 | 27 |
| `DuckDBExtensions.kt` | `suspendTransaction`, `queryFlow` 확장 | 69 |
| `dialect/DuckDBDialect.kt` | PostgreSQLDialect 상속 | 16 |
| `dialect/DuckDBDialectMetadata.kt` | FK 캐싱 건너뜀 (getImportedKeys 미지원) | 20 |

테스트:

| 파일 | 역할 |
|------|------|
| `AbstractDuckDBTest.kt` | DuckDB 인메모리 공통 설정 |
| `DuckDBExtensionsTest.kt` | suspendTransaction/queryFlow 테스트 |
| `domain/Events.kt` | 테스트용 테이블 정의 |
| `insert/InsertTest.kt` | INSERT 테스트 |
| `query/SelectTest.kt` | SELECT 테스트 |

---

## 3. 설계 결정

### 3.1 패키지명

**결론: 기존 패키지 유지 (변경 없음)**

- `io.bluetape4k.exposed.bigquery.*` — 그대로 유지
- `io.bluetape4k.exposed.duckdb.*` — 그대로 유지

근거:
- exposed-postgresql(`io.bluetape4k.exposed.postgresql`), exposed-mysql8(`io.bluetape4k.exposed.mysql8`) 패턴과 동일
- 기존 코드의 import 변경 불필요 → 마이그레이션 비용 최소화

### 3.2 외부 의존성

#### exposed-bigquery

| 의존성 | Libs.kt (projects) 존재 여부 | 조치 |
|--------|--------------------------|------|
| `exposed_core` | O | `exposed_bom` platform 사용 |
| `exposed_dao` | O | `exposed_bom` platform 사용 |
| `exposed_jdbc` | O | `compileOnly`로 전환 (SQL 생성용 H2에만 필요) |
| `exposed_java_time` | O | `compileOnly`로 전환 |
| `google_api_services_bigquery` | **X — 추가 필요** | `com.google.apis:google-api-services-bigquery:v2-rev20240919-2.0.0` |
| `h2_v2` | O | `implementation` (내부 SQL 생성용) |
| `kotlinx_coroutines_core` | O | `api` |
| `testcontainers_gcloud` | O | `testImplementation` |

#### exposed-duckdb

| 의존성 | Libs.kt (projects) 존재 여부 | 조치 |
|--------|--------------------------|------|
| `exposed_core` | O | `exposed_bom` platform 사용 |
| `exposed_dao` | O | `exposed_bom` platform 사용 |
| `exposed_jdbc` | O | `compileOnly`로 전환 |
| `exposed_java_time` | O | `compileOnly`로 전환 |
| `duckdb_jdbc` | **X — 추가 필요** | `org.duckdb:duckdb_jdbc:1.1.3` |
| `kotlinx_coroutines_core` | O | `api` |

### 3.3 Libs.kt 추가 항목

```kotlin
// JDBC Drivers (기존 h2_v2 근처)
const val duckdb_jdbc = "org.duckdb:duckdb_jdbc:1.1.3"            // https://mvnrepository.com/artifact/org.duckdb/duckdb_jdbc

// BigQuery REST API 클라이언트 (google 관련 섹션)
const val google_api_services_bigquery = "com.google.apis:google-api-services-bigquery:v2-rev20240919-2.0.0"  // https://mvnrepository.com/artifact/com.google.apis/google-api-services-bigquery
```

### 3.4 build.gradle.kts 설계

#### exposed-bigquery/build.gradle.kts

exposed-mysql8/exposed-postgresql 패턴을 따르되, BigQuery는 JDBC가 아닌 REST API 방식이므로 차이점이 있다:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed (SQL 생성용)
    implementation(platform(Libs.exposed_bom))
    api(project(":bluetape4k-exposed-core"))
    compileOnly(Libs.exposed_jdbc)        // H2 SQL 생성에 필요
    compileOnly(Libs.exposed_java_time)

    // Logging
    implementation(project(":bluetape4k-logging"))

    // BigQuery REST API 클라이언트
    api(Libs.google_api_services_bigquery)

    // Coroutines
    api(Libs.kotlinx_coroutines_core)

    // H2 — BigQueryContext.create()가 내부 SQL 생성용 H2 DB를 사용
    implementation(Libs.h2_v2)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_gcloud)
}
```

**참고**: exposed-bigquery는 JDBC로 직접 DB에 접속하지 않으므로 `exposed-jdbc-tests` 불필요.

#### exposed-duckdb/build.gradle.kts

```kotlin
tasks.test {
    // DuckDB JDBC uses System.load() for native library — required for Java 25+
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(project(":bluetape4k-exposed-core"))
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_java_time)

    // Logging
    implementation(project(":bluetape4k-logging"))

    // DuckDB JDBC 드라이버
    api(Libs.duckdb_jdbc)

    // Coroutines
    api(Libs.kotlinx_coroutines_core)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.testcontainers_junit_jupiter)

    testRuntimeOnly(Libs.hikaricp)
}
```

**참고**: DuckDB는 Testcontainer 없이 인메모리로 테스트 가능. `testcontainers_gcloud` 등 불필요.

### 3.5 이관 방식

**파일 복사 + 의존성 조정** 방식 (exposed-mysql8 이관과 동일):

1. experimental 소스를 그대로 복사 (패키지명 변경 없음)
2. `build.gradle.kts`는 projects 패턴에 맞게 재작성
3. `Libs.kt` 참조를 `project(":bluetape4k-xxx")` 형태로 변환
4. README.md 복사 후 필요시 경로 업데이트

### 3.6 experimental 저장소 처리

이관 완료 후:
- experimental의 `data/exposed-bigquery/`, `data/exposed-duckdb/` 디렉토리 삭제
- experimental의 `settings.gradle.kts`에서 해당 모듈 include 제거

---

## 4. 이관 파일 매핑

### 4.1 exposed-bigquery

| 소스 (experimental) | 대상 (projects) |
|---------------------|-----------------|
| `data/exposed-bigquery/src/main/kotlin/io/bluetape4k/exposed/bigquery/BigQueryContext.kt` | 동일 경로 |
| `data/exposed-bigquery/src/main/kotlin/io/bluetape4k/exposed/bigquery/BigQueryQueryExecutor.kt` | 동일 경로 |
| `data/exposed-bigquery/src/main/kotlin/io/bluetape4k/exposed/bigquery/dialect/BigQueryDialect.kt` | 동일 경로 |
| `data/exposed-bigquery/src/test/kotlin/...` (6파일) | 동일 경로 |
| `data/exposed-bigquery/README.md` | 동일 경로 |
| `data/exposed-bigquery/build.gradle.kts` | **재작성** (projects 패턴) |

### 4.2 exposed-duckdb

| 소스 (experimental) | 대상 (projects) |
|---------------------|-----------------|
| `data/exposed-duckdb/src/main/kotlin/io/bluetape4k/exposed/duckdb/DuckDBDatabase.kt` | 동일 경로 |
| `data/exposed-duckdb/src/main/kotlin/io/bluetape4k/exposed/duckdb/DuckDBConnectionWrapper.kt` | 동일 경로 |
| `data/exposed-duckdb/src/main/kotlin/io/bluetape4k/exposed/duckdb/DuckDBExtensions.kt` | 동일 경로 |
| `data/exposed-duckdb/src/main/kotlin/io/bluetape4k/exposed/duckdb/dialect/DuckDBDialect.kt` | 동일 경로 |
| `data/exposed-duckdb/src/main/kotlin/io/bluetape4k/exposed/duckdb/dialect/DuckDBDialectMetadata.kt` | 동일 경로 |
| `data/exposed-duckdb/src/test/kotlin/...` (5파일) | 동일 경로 |
| `data/exposed-duckdb/README.md` | 동일 경로 |
| `data/exposed-duckdb/build.gradle.kts` | **재작성** (projects 패턴) |

---

## 5. 빌드 설정 변경

### 5.1 `buildSrc/src/main/kotlin/Libs.kt` 변경

추가할 상수 2개:

```kotlin
const val duckdb_jdbc = "org.duckdb:duckdb_jdbc:1.1.3"
const val google_api_services_bigquery = "com.google.apis:google-api-services-bigquery:v2-rev20240919-2.0.0"
```

위치: JDBC 드라이버 섹션 (기존 `h2_v2`, `clickhouse_jdbc` 근처)

### 5.2 `settings.gradle.kts` 변경

**변경 불필요** — `data/` 하위 디렉토리는 `includeModules` 함수로 자동 등록됨.
`exposed-bigquery`, `exposed-duckdb` 디렉토리가 생기면 자동으로 `bluetape4k-exposed-bigquery`, `bluetape4k-exposed-duckdb` 모듈로 인식.

### 5.3 CLAUDE.md 업데이트

Architecture > Module Structure > Data Modules 섹션에 추가:

```markdown
- **exposed-bigquery**: BigQuery 전용 Exposed 확장 — REST API 기반 실행 (JDBC 없음), H2로 SQL 생성, suspend/Flow API, 페이지네이션 자동 처리
- **exposed-duckdb**: DuckDB 전용 Exposed 확장 — JDBC 래퍼, 커스텀 다이얼렉트, suspendTransaction/queryFlow 확장
```

---

## 6. 테스트 전략

### 6.1 exposed-bigquery 테스트

- **방식**: BigQuery Emulator Testcontainer (`testcontainers_gcloud`)
- **주의점**: BigQuery Emulator는 Docker 이미지 필요 (`ghcr.io/goccy/bigquery-emulator`)
- **검증 범위**: SELECT (단순/복합), INSERT, 페이지네이션, Flow 수집
- **실행**: `./gradlew :bluetape4k-exposed-bigquery:test`

### 6.2 exposed-duckdb 테스트

- **방식**: DuckDB 인메모리 DB (Testcontainer 불필요, 외부 서비스 의존성 없음)
- **주의점**: `--enable-native-access=ALL-UNNAMED` JVM 옵션 필수 (DuckDB native 라이브러리)
- **검증 범위**: 인메모리/파일 DB 연결, CRUD, suspendTransaction, queryFlow
- **실행**: `./gradlew :bluetape4k-exposed-duckdb:test`

### 6.3 이관 후 검증 순서

1. `./gradlew :bluetape4k-exposed-duckdb:test` — 외부 의존성 없으므로 먼저 검증
2. `./gradlew :bluetape4k-exposed-bigquery:test` — Docker 필요, BigQuery Emulator 이미지 pull 시간 고려
3. 전체 빌드 확인: `./gradlew build -x test` (컴파일 수준)

---

## 7. 태스크 목록

### Phase 1: Libs.kt 의존성 추가
- [ ] `Libs.kt`에 `duckdb_jdbc`, `google_api_services_bigquery` 상수 추가

### Phase 2: exposed-duckdb 이관 (외부 의존성 없음 → 먼저)
- [ ] `data/exposed-duckdb/` 디렉토리 생성
- [ ] main 소스 5파일 복사 (패키지 변경 없음)
- [ ] test 소스 5파일 복사
- [ ] README.md 복사
- [ ] `build.gradle.kts` 재작성 (projects 패턴)
- [ ] 빌드 확인: `./gradlew :bluetape4k-exposed-duckdb:build`
- [ ] 테스트 확인: `./gradlew :bluetape4k-exposed-duckdb:test`

### Phase 3: exposed-bigquery 이관
- [ ] `data/exposed-bigquery/` 디렉토리 생성
- [ ] main 소스 3파일 복사 (패키지 변경 없음)
- [ ] test 소스 6파일 복사
- [ ] README.md 복사
- [ ] `build.gradle.kts` 재작성 (projects 패턴)
- [ ] 빌드 확인: `./gradlew :bluetape4k-exposed-bigquery:build`
- [ ] 테스트 확인: `./gradlew :bluetape4k-exposed-bigquery:test`

### Phase 4: 프로젝트 문서 업데이트
- [ ] `CLAUDE.md` Architecture > Data Modules 섹션에 두 모듈 추가
- [ ] experimental 저장소에서 해당 모듈 삭제 (별도 커밋)

### Phase 5: 커밋
- [ ] `feat(data): bluetape4k-exposed-bigquery, bluetape4k-exposed-duckdb 모듈 추가 (experimental에서 이관)`
