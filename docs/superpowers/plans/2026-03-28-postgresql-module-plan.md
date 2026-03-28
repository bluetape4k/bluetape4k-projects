# bluetape4k-postgresql 모듈 구현 계획

**작성일**: 2026-03-28
**Spec 참조**: `docs/superpowers/specs/2026-03-28-postgresql-module-design.md`
**모듈 위치**: `data/postgresql/` (자동 등록: `:bluetape4k-postgresql`)

---

## Context

`bluetape4k-experimental` 저장소의 PostgreSQL 전용 Exposed 확장 모듈 3개(exposed-postgis, exposed-pgvector, exposed-tsrange)를
`bluetape4k-projects`의 단일 모듈 `bluetape4k-postgresql`로 통합한다.

소스는 패키지 변경만 수행하며 API 시그니처 변경은 없다. 테스트는 기능별 Docker 이미지를 분리하여 별도 컨테이너로 실행한다.

---

## Work Objectives

1. `Libs.kt`에 postgis-jdbc, pgvector 의존성 상수 추가
2. `data/postgresql/` 모듈 생성 및 빌드 설정
3. 3개 기능(PostGIS, pgvector, tsrange) 소스 마이그레이션 (패키지 변경)
4. 테스트 마이그레이션 (컨테이너별 분리)
5. README.md 작성 및 CLAUDE.md 업데이트

---

## Guardrails

### Must Have
- 모든 소스의 패키지를 `io.bluetape4k.exposed.postgresql.*` 하위로 변경
- `postgis-jdbc`, `pgvector`는 `compileOnly` 의존성 (사용자 선택)
- 테스트는 기능별 전용 Docker 이미지 사용 (postgis/postgis:16-3.4, pgvector/pgvector:pg16, postgres:16)
- KDoc 한국어 유지
- Detekt 정적 분석 통과

### Must NOT Have
- API 시그니처 변경 (패키지 이동만)
- 새로운 기능 추가 (향후 확장은 별도 태스크)
- `settings.gradle.kts` 수정 (자동 감지)

---

## Task Flow

```
Task 1 (Libs.kt)
    |
    v
Task 2 (build.gradle.kts)
    |
    v
Task 3a/3b/3c (소스 마이그레이션, 병렬 가능)
    |
    v
Task 4a/4b/4c (테스트 마이그레이션, 병렬 가능)
    |
    v
Task 5 (빌드 검증)
    |
    v
Task 6 (README + CLAUDE.md)
```

---

## Detailed TODOs

### Task 1: Libs.kt 의존성 상수 추가
**complexity: low**

`buildSrc/src/main/kotlin/Libs.kt`의 Database Drivers 섹션(`postgresql_driver` 근처)에 추가:

```kotlin
const val postgis_jdbc = "net.postgis:postgis-jdbc:2024.1.0"
const val pgvector = "com.pgvector:pgvector:0.1.6"
```

**Acceptance Criteria**:
- [ ] `Libs.postgis_jdbc`, `Libs.pgvector` 상수가 컴파일 가능
- [ ] 기존 빌드에 영향 없음

---

### Task 2: 모듈 디렉토리 및 build.gradle.kts 생성
**complexity: medium**

`data/postgresql/build.gradle.kts` 생성. `exposed-core/build.gradle.kts` 패턴 참조.

핵심 의존성 구조:
- `implementation(platform(Libs.exposed_bom))` -- Exposed 모듈 버전 정렬 (첫 번째 선언)
- `api(project(":bluetape4k-exposed-core"))` -- 컬럼 타입 기반
- `compileOnly(Libs.exposed_jdbc)` -- JDBC 연산에만 필요
- `compileOnly(Libs.exposed_java_time)` -- java.time 지원
- `compileOnly(Libs.postgis_jdbc)` -- PostGIS 사용 시만
- `compileOnly(Libs.pgvector)` -- pgvector 사용 시만
- `compileOnly(Libs.postgresql_driver)` -- PostgreSQL 드라이버
- `implementation(project(":bluetape4k-logging"))` -- 로깅
- `testImplementation(project(":bluetape4k-exposed-jdbc-tests"))` -- 테스트 인프라
- `testImplementation(project(":bluetape4k-junit5"))`
- `testImplementation(project(":bluetape4k-testcontainers"))`
- `testImplementation(Libs.testcontainers_junit_jupiter)`
- `testImplementation(Libs.testcontainers_postgresql)`
- `testRuntimeOnly(Libs.h2_v2)` -- tsrange H2 fallback
- `testRuntimeOnly(Libs.postgresql_driver)`
- `testRuntimeOnly(Libs.hikaricp)`
- `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }`

**Acceptance Criteria**:
- [ ] `./gradlew :bluetape4k-postgresql:dependencies` 정상 실행
- [ ] 모듈이 `settings.gradle.kts` 자동 감지로 등록됨

---

### Task 3a: PostGIS 소스 마이그레이션
**complexity: medium**

experimental 소스 2개 파일을 복사하고 패키지 변경:
- `GeoColumnTypes.kt` -> `io.bluetape4k.exposed.postgresql.postgis`
- `GeoExtensions.kt` -> `io.bluetape4k.exposed.postgresql.postgis`

변경 내용:
- `package io.bluetape4k.exposed.postgis` -> `package io.bluetape4k.exposed.postgresql.postgis`
- API 시그니처 변경 없음

파일 목록 (src/main/kotlin/io/bluetape4k/exposed/postgresql/postgis/):
- `GeoColumnTypes.kt` (GeoPointColumnType, GeoPolygonColumnType, SRID_WGS84 상수)
- `GeoExtensions.kt` (geoPoint, geoPolygon, stDistance, stDWithin, stWithin, stContains, stContainsPoint, stOverlaps, stIntersects, stDisjoint, stArea + 모든 Op/Expr 클래스)

**Acceptance Criteria**:
- [ ] 패키지 `io.bluetape4k.exposed.postgresql.postgis` 하위에 모든 클래스 존재
- [ ] KDoc 한국어 유지
- [ ] `import net.postgis.jdbc.*` 유지

---

### Task 3b: pgvector 소스 마이그레이션
**complexity: medium**

experimental 소스 2개 파일을 복사하고 패키지 변경:
- `VectorColumnType.kt` -> `io.bluetape4k.exposed.postgresql.pgvector`
- `VectorExtensions.kt` -> `io.bluetape4k.exposed.postgresql.pgvector`

변경 내용:
- `package io.bluetape4k.exposed.pgvector` -> `package io.bluetape4k.exposed.postgresql.pgvector`
- API 시그니처 변경 없음

파일 목록 (src/main/kotlin/io/bluetape4k/exposed/postgresql/pgvector/):
- `VectorColumnType.kt` (VectorColumnType)
- `VectorExtensions.kt` (vector, registerVectorType, cosineDistance, l2Distance, innerProduct, VectorDistanceOp)

**Acceptance Criteria**:
- [ ] 패키지 `io.bluetape4k.exposed.postgresql.pgvector` 하위에 모든 클래스 존재
- [ ] `import com.pgvector.PGvector` 유지
- [ ] `import io.bluetape4k.support.requirePositiveNumber` 유지 (bluetape4k-core 의존)

---

### Task 3c: tsrange 소스 마이그레이션
**complexity: medium**

experimental 소스 3개 파일을 복사하고 패키지 변경:
- `TimestampRange.kt` -> `io.bluetape4k.exposed.postgresql.tsrange`
- `TstzRangeColumnType.kt` -> `io.bluetape4k.exposed.postgresql.tsrange`
- `TstzRangeExtensions.kt` -> `io.bluetape4k.exposed.postgresql.tsrange`

변경 내용:
- `package io.bluetape4k.exposed.tsrange` -> `package io.bluetape4k.exposed.postgresql.tsrange`
- API 시그니처 변경 없음

파일 목록 (src/main/kotlin/io/bluetape4k/exposed/postgresql/tsrange/):
- `TimestampRange.kt` (TimestampRange data class, Serializable)
- `TstzRangeColumnType.kt` (TstzRangeColumnType, PG_TIMESTAMP_FORMATTER)
- `TstzRangeExtensions.kt` (tstzRange, TstzRangeOverlapsOp, TstzRangeContainsInstantOp, TstzRangeContainsRangeOp, TstzRangeAdjacentOp, overlaps, contains, containsRange, isAdjacentTo)

**Acceptance Criteria**:
- [ ] 패키지 `io.bluetape4k.exposed.postgresql.tsrange` 하위에 모든 클래스 존재
- [ ] H2 fallback 로직 (VARCHAR(120)) 유지
- [ ] `parameterMarker()` 의 `?::tstzrange` 캐스트 유지

---

### Task 4a: PostGIS 테스트 마이그레이션
**complexity: high**

`GeoColumnTypeTest.kt`를 복사하고 패키지 변경. PostGIS 전용 컨테이너 사용.

src/test/kotlin/io/bluetape4k/exposed/postgresql/postgis/:
- `GeoColumnTypeTest.kt`

핵심 테스트 컨테이너:
- Docker 이미지: `postgis/postgis:16-3.4`
- 초기화: `CREATE EXTENSION IF NOT EXISTS postgis`

테스트 패턴 변경:
- `package io.bluetape4k.exposed.postgis` -> `package io.bluetape4k.exposed.postgresql.postgis`
- `import io.bluetape4k.exposed.postgis.*` -> `import io.bluetape4k.exposed.postgresql.postgis.*`
- 기존 `AbstractExposedTest` 상속 유지
- `ShutdownQueue.register(this)` 패턴 유지

포함 테스트 (17개):
- POINT CRUD, POLYGON CRUD, SRID 보정
- ST_Distance, ST_DWithin, ST_Within, ST_Contains (Polygon-Polygon, Polygon-Point)
- ST_Overlaps, ST_Intersects, ST_Disjoint, ST_Area
- 혼합 시나리오 (포함/겹침/분리)

**Acceptance Criteria**:
- [ ] `postgis/postgis:16-3.4` 컨테이너로 모든 테스트 통과
- [ ] `./gradlew :bluetape4k-postgresql:test --tests "*GeoColumnTypeTest*"` 성공

---

### Task 4b: pgvector 테스트 마이그레이션
**complexity: high**

`VectorColumnTypeTest.kt`를 복사하고 패키지 변경. pgvector 전용 컨테이너 사용.

src/test/kotlin/io/bluetape4k/exposed/postgresql/pgvector/:
- `VectorColumnTypeTest.kt`

핵심 테스트 컨테이너:
- Docker 이미지: `pgvector/pgvector:pg16`
- 초기화: `CREATE EXTENSION IF NOT EXISTS vector` + `PGvector.addVectorType()`

테스트 패턴 변경:
- `package io.bluetape4k.exposed.pgvector` -> `package io.bluetape4k.exposed.postgresql.pgvector`
- `import io.bluetape4k.exposed.pgvector.*` -> `import io.bluetape4k.exposed.postgresql.pgvector.*`

포함 테스트 (9개):
- 벡터 CRUD, 코사인 거리, L2 거리
- dimension 검증 (0, 음수, 불일치)
- VectorDistanceOp select expr 조회

**Acceptance Criteria**:
- [ ] `pgvector/pgvector:pg16` 컨테이너로 모든 테스트 통과
- [ ] `./gradlew :bluetape4k-postgresql:test --tests "*VectorColumnTypeTest*"` 성공

---

### Task 4c: tsrange 테스트 마이그레이션
**complexity: high**

3개 테스트 파일을 복사/생성하고 패키지 변경.

src/test/kotlin/io/bluetape4k/exposed/postgresql/tsrange/:
- `TimestampRangeTest.kt` (순수 값 객체 단위 테스트 — DB 불필요)
- `TstzRangeColumnTypeTest.kt` (H2 + PostgreSQL 멀티 dialect 테스트)
- `TstzRangePostgresTest.kt` (PostgreSQL 전용 네이티브 연산자 테스트)

핵심 테스트 컨테이너:
- Docker 이미지: `postgres:16` (기본 PostgreSQL)
- H2 in-memory (fallback 테스트용, testRuntimeOnly)

테스트 패턴 변경:
- `package io.bluetape4k.exposed.tsrange` -> `package io.bluetape4k.exposed.postgresql.tsrange`
- `import io.bluetape4k.exposed.tsrange.*` -> `import io.bluetape4k.exposed.postgresql.tsrange.*`
- `AbstractExposedTest` + `TestDB` + `withTables` 패턴 유지

포함 테스트:
- `TimestampRangeTest` (6개): contains/overlaps 로직, lowerInclusive/upperInclusive 조합, 경계 케이스
- `TstzRangeColumnTypeTest` (10개): 범위 CRUD (기본/양쪽 포함/양쪽 미포함), contains, overlaps, PG JDBC literal 파싱, dialect별 sqlType/parameterMarker
- `TstzRangePostgresTest` (7개): 네이티브 TSTZRANGE CRUD, overlaps/containsRange/adjacent 연산자, 복수 범위

**Acceptance Criteria**:
- [ ] `TimestampRangeTest` 단위 테스트 통과 (DB 불필요)
- [ ] H2 fallback 테스트 (VARCHAR(120)) 통과
- [ ] PostgreSQL 네이티브 TSTZRANGE 테스트 통과
- [ ] `./gradlew :bluetape4k-postgresql:test --tests "*Tstz*"` 성공

---

### Task 5: 빌드 검증 및 bluetape4k-patterns 체크리스트
**complexity: low**

```bash
./gradlew :bluetape4k-postgresql:build
./gradlew :bluetape4k-postgresql:detekt
./gradlew :bluetape4k-postgresql:test
```

**bluetape4k-patterns 체크리스트 (모든 신규 소스 파일 검토)**:
- [ ] 인자 검증: `requirePositiveNumber`, `requireNotBlank` 등 적절히 사용 (VectorColumnType.dimension 등)
- [ ] 로깅: companion object에 `KLogging()` 사용
- [ ] value class 적절성 검토 (TimestampRange는 두 필드라 data class 적합)
- [ ] KDoc 한국어 작성 여부 (public class/interface/extension 전수 확인)
- [ ] PostgreSQL 전용 연산에 `check(currentDialect is PostgreSQLDialect)` guard 존재

**Acceptance Criteria**:
- [ ] 빌드 성공 (컴파일 에러 없음)
- [ ] Detekt 정적 분석 통과
- [ ] 전체 테스트 통과 (PostGIS + pgvector + tsrange)
- [ ] bluetape4k-patterns 체크리스트 전 항목 충족

---

### Task 6: README.md 작성 및 CLAUDE.md 업데이트
**complexity: low**

#### 6a: `data/postgresql/README.md` 작성
- 모듈 개요 (PostgreSQL 전용 Exposed 확장)
- 3개 기능 설명 (PostGIS, pgvector, tsrange)
- 의존성 설정 가이드 (필요한 기능에 따라 런타임 의존성 추가)
- 사용 예제 (각 기능별)

#### 6b: CLAUDE.md 업데이트
- Architecture > Module Structure > Data Modules 섹션에 추가:
  ```
  - **postgresql**: PostgreSQL 전용 Exposed 확장 -- PostGIS 공간 데이터, pgvector 벡터 검색, TSTZRANGE 시간 범위
  ```

**Acceptance Criteria**:
- [ ] `data/postgresql/README.md` 존재
- [ ] CLAUDE.md Data Modules 섹션에 postgresql 모듈 설명 포함

---

## Success Criteria

1. `./gradlew :bluetape4k-postgresql:build` 성공
2. `./gradlew :bluetape4k-postgresql:test` 전체 통과 (PostGIS, pgvector, tsrange)
3. `./gradlew :bluetape4k-postgresql:detekt` 통과
4. 패키지 구조가 `io.bluetape4k.exposed.postgresql.{postgis,pgvector,tsrange}` 규칙 준수
5. `compileOnly` 의존성으로 사용자 선택적 기능 분리 유지
6. README.md, CLAUDE.md 문서 최신화

---

## File Inventory

### 신규 생성 파일 (12개)

| 파일 | 원본 (experimental) |
|------|---------------------|
| `data/postgresql/build.gradle.kts` | 신규 |
| `data/postgresql/src/main/kotlin/.../postgis/GeoColumnTypes.kt` | `exposed-postgis/.../GeoColumnTypes.kt` |
| `data/postgresql/src/main/kotlin/.../postgis/GeoExtensions.kt` | `exposed-postgis/.../GeoExtensions.kt` |
| `data/postgresql/src/main/kotlin/.../pgvector/VectorColumnType.kt` | `exposed-pgvector/.../VectorColumnType.kt` |
| `data/postgresql/src/main/kotlin/.../pgvector/VectorExtensions.kt` | `exposed-pgvector/.../VectorExtensions.kt` |
| `data/postgresql/src/main/kotlin/.../tsrange/TimestampRange.kt` | `exposed-tsrange/.../TimestampRange.kt` |
| `data/postgresql/src/main/kotlin/.../tsrange/TstzRangeColumnType.kt` | `exposed-tsrange/.../TstzRangeColumnType.kt` |
| `data/postgresql/src/main/kotlin/.../tsrange/TstzRangeExtensions.kt` | `exposed-tsrange/.../TstzRangeExtensions.kt` |
| `data/postgresql/src/test/kotlin/.../postgis/GeoColumnTypeTest.kt` | `exposed-postgis/.../GeoColumnTypeTest.kt` |
| `data/postgresql/src/test/kotlin/.../pgvector/VectorColumnTypeTest.kt` | `exposed-pgvector/.../VectorColumnTypeTest.kt` |
| `data/postgresql/src/test/kotlin/.../tsrange/TimestampRangeTest.kt` | 신규 (단위 테스트) |
| `data/postgresql/src/test/kotlin/.../tsrange/TstzRangeColumnTypeTest.kt` | `exposed-tsrange/.../TstzRangeColumnTypeTest.kt` |
| `data/postgresql/src/test/kotlin/.../tsrange/TstzRangePostgresTest.kt` | `exposed-tsrange/.../TstzRangePostgresTest.kt` |
| `data/postgresql/src/test/resources/postgis-init.sql` | 신규 (`CREATE EXTENSION IF NOT EXISTS postgis;`) |
| `data/postgresql/src/test/resources/pgvector-init.sql` | 신규 (`CREATE EXTENSION IF NOT EXISTS vector;`) |

### 수정 파일 (2개)

| 파일 | 변경 내용 |
|------|----------|
| `buildSrc/src/main/kotlin/Libs.kt` | `postgis_jdbc`, `pgvector` 상수 추가 |
| `CLAUDE.md` | Data Modules 섹션에 postgresql 추가 |

### 신규 문서 (1개)

| 파일 |
|------|
| `data/postgresql/README.md` |
