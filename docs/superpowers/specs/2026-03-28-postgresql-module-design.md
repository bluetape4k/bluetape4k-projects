# bluetape4k-postgresql 모듈 설계 명세

## 1. 개요

`bluetape4k-experimental` 저장소의 PostgreSQL 전용 Exposed 확장 모듈 3개(exposed-postgis, exposed-pgvector, exposed-tsrange)를 `bluetape4k-projects`의 단일 모듈 `bluetape4k-postgresql`로 통합한다.

### 1.1 통합 근거

| 기준 | 판단 |
|------|------|
| 도메인 응집성 | 3개 모두 PostgreSQL 전용 Exposed 확장 |
| 선택적 의존성 | postgis-jdbc, pgvector는 `compileOnly`로 사용자가 필요한 것만 추가 |
| 모듈 최소화 정책 | bluetape4k-projects 기존 정책과 일치 |
| 독립 배포 필요성 | 없음 (항상 PostgreSQL + Exposed 조합으로 사용) |

### 1.2 모듈 위치

```
data/postgresql/
  src/main/kotlin/io/bluetape4k/exposed/postgresql/
  src/test/kotlin/io/bluetape4k/exposed/postgresql/
  build.gradle.kts
```

- `settings.gradle.kts`의 `includeModules("data", withBaseDir = false)` 규칙에 의해 자동으로 `:bluetape4k-postgresql` 모듈로 등록됨
- 별도의 settings.gradle.kts 수정 불필요

---

## 2. 패키지 구조

```
io.bluetape4k.exposed.postgresql/
    postgis/
        GeoPointColumnType.kt        -- PostGIS POINT 컬럼 타입
        GeoPolygonColumnType.kt      -- PostGIS POLYGON 컬럼 타입
        GeoExtensions.kt             -- Table.geoPoint(), 공간 연산 확장
    pgvector/
        VectorColumnType.kt          -- pgvector VECTOR(n) 컬럼 타입
        VectorExtensions.kt          -- Table.vector(), 거리 연산 확장
    tsrange/
        TimestampRange.kt            -- Instant 기반 범위 값 객체
        TstzRangeColumnType.kt       -- TSTZRANGE 컬럼 타입 (H2 fallback 포함)
        TstzRangeExtensions.kt       -- Table.tstzRange(), 범위 연산 확장
```

### 2.1 패키지 변경 매핑

| experimental 패키지 | bluetape4k-postgresql 패키지 |
|---------------------|------------------------------|
| `io.bluetape4k.exposed.postgis.*` | `io.bluetape4k.exposed.postgresql.postgis.*` |
| `io.bluetape4k.exposed.pgvector.*` | `io.bluetape4k.exposed.postgresql.pgvector.*` |
| `io.bluetape4k.exposed.tsrange.*` | `io.bluetape4k.exposed.postgresql.tsrange.*` |

공통 상위 패키지 `io.bluetape4k.exposed.postgresql`을 추가하여 모듈 소속을 명확히 한다.

---

## 3. 기능별 API 설계

### 3.1 PostGIS (postgis/)

#### GeoPointColumnType

```kotlin
/**
 * PostGIS GEOMETRY(POINT, 4326) 컬럼 타입.
 * JTS Geometry Point를 PostgreSQL PostGIS POINT로 매핑한다.
 */
class GeoPointColumnType : ColumnType<Point>() {
    override fun sqlType(): String = "GEOMETRY(POINT,4326)"
    override fun valueFromDB(value: Any): Point
    override fun notNullValueToDB(value: Point): Any
    override fun nonNullValueToString(value: Point): String
}
```

#### GeoPolygonColumnType

```kotlin
/**
 * PostGIS GEOMETRY(POLYGON, 4326) 컬럼 타입.
 */
class GeoPolygonColumnType : ColumnType<Polygon>() {
    override fun sqlType(): String = "GEOMETRY(POLYGON,4326)"
    override fun valueFromDB(value: Any): Polygon
    override fun notNullValueToDB(value: Polygon): Any
}
```

#### GeoExtensions

```kotlin
// 컬럼 등록
fun Table.geoPoint(name: String): Column<Point>
fun Table.geoPolygon(name: String): Column<Polygon>

// 공간 연산 (ST_ 함수)
fun Column<Point>.stDistance(other: Expression<Point>): Function<Double>
fun Column<Point>.stDWithin(other: Expression<Point>, distance: Double): Op<Boolean>
fun Column<Polygon>.stWithin(point: Expression<Point>): Op<Boolean>
fun Column<Polygon>.stContains(point: Expression<Point>): Op<Boolean>
fun Column<Polygon>.stContainsPoint(point: Expression<Point>): Op<Boolean>
fun Column<Polygon>.stOverlaps(other: Expression<Polygon>): Op<Boolean>
fun Column<Polygon>.stIntersects(other: Expression<Polygon>): Op<Boolean>
fun Column<Polygon>.stDisjoint(other: Expression<Polygon>): Op<Boolean>
fun Column<Polygon>.stArea(): Function<Double>
```

**변경사항**: 패키지 이동만. API 시그니처 변경 없음.

### 3.2 pgvector (pgvector/)

#### VectorColumnType

```kotlin
/**
 * pgvector VECTOR(n) 컬럼 타입.
 * FloatArray를 PostgreSQL vector 타입으로 매핑한다.
 *
 * @param dimension 벡터 차원 수
 */
class VectorColumnType(val dimension: Int) : ColumnType<FloatArray>() {
    override fun sqlType(): String = "VECTOR($dimension)"
    override fun valueFromDB(value: Any): FloatArray
    override fun notNullValueToDB(value: FloatArray): Any
    override fun nonNullValueToString(value: FloatArray): String
}
```

#### VectorExtensions

```kotlin
// 컬럼 등록
fun Table.vector(name: String, dimension: Int): Column<FloatArray>

// pgvector 타입 등록 (Connection 단위, 1회 호출)
fun registerVectorType(connection: Connection)

// 거리/유사도 연산
fun Column<FloatArray>.cosineDistance(other: Expression<FloatArray>): Function<Double>
fun Column<FloatArray>.l2Distance(other: Expression<FloatArray>): Function<Double>
fun Column<FloatArray>.innerProduct(other: Expression<FloatArray>): Function<Double>
```

**변경사항**:
- 패키지 이동
- `registerVectorType()`은 기존 그대로 유지 (pgvector JDBC 드라이버의 `PGvector.addVectorType()` 위임)

### 3.3 TSTZRANGE (tsrange/)

#### TimestampRange

원본 experimental API를 그대로 유지한다 (패키지 이동만).

```kotlin
/**
 * 시작/종료 [Instant]로 표현되는 시간 범위 값 객체.
 * PostgreSQL TSTZRANGE에 대응. 기본값: [start, end) (하한 포함, 상한 미포함).
 *
 * @property start 범위의 시작 시각
 * @property end 범위의 종료 시각
 * @property lowerInclusive 하한 포함 여부 (기본 true)
 * @property upperInclusive 상한 포함 여부 (기본 false)
 */
data class TimestampRange(
    val start: Instant,
    val end: Instant,
    val lowerInclusive: Boolean = true,
    val upperInclusive: Boolean = false,
) : Serializable {
    fun contains(instant: Instant): Boolean
    fun overlaps(other: TimestampRange): Boolean
}
```

**범위 의미론**: PostgreSQL TSTZRANGE 기본값 `[start, end)` 와 일치 (`lowerInclusive=true`, `upperInclusive=false`).

#### TstzRangeColumnType

```kotlin
/**
 * PostgreSQL TSTZRANGE 컬럼 타입.
 * H2 환경에서는 VARCHAR(120) fallback으로 동작한다.
 */
class TstzRangeColumnType : ColumnType<TimestampRange>() {
    override fun sqlType(): String  // PostgreSQL: "TSTZRANGE", H2: "VARCHAR(120)"
    override fun valueFromDB(value: Any): TimestampRange
    override fun notNullValueToDB(value: TimestampRange): Any
    override fun nonNullValueToString(value: TimestampRange): String
}
```

#### TstzRangeExtensions

```kotlin
// 컬럼 등록
fun Table.tstzRange(name: String): Column<TimestampRange>

// 범위 연산 (PostgreSQL 연산자)
fun Column<TimestampRange>.overlaps(other: Expression<TimestampRange>): Op<Boolean>        // &&
fun Column<TimestampRange>.contains(instant: Expression<Instant>): Op<Boolean>             // @>
fun Column<TimestampRange>.containsRange(other: Expression<TimestampRange>): Op<Boolean>   // @>
fun Column<TimestampRange>.isAdjacentTo(other: Expression<TimestampRange>): Op<Boolean>    // -|-
```

**변경사항**: 패키지 이동만. H2 fallback 로직 유지.

---

## 4. build.gradle.kts 의존성 설계

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(project(":bluetape4k-exposed-core"))
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_java_time)

    // PostgreSQL 전용 라이브러리 (사용자가 필요한 것만 런타임에 추가)
    compileOnly(Libs.postgis_jdbc)       // PostGIS JDBC
    compileOnly(Libs.pgvector)           // pgvector JDBC

    // Logging
    implementation(project(":bluetape4k-logging"))

    // Database Drivers
    compileOnly(Libs.postgresql_driver)

    // Testing
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_postgresql)

    testRuntimeOnly(Libs.h2_v2)           // tsrange H2 fallback 테스트용
    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.hikaricp)
}
```

### 4.1 의존성 분류 근거

| 의존성 | 분류 | 근거 |
|--------|------|------|
| `bluetape4k-exposed-core` | `api` | 컬럼 타입의 기반 클래스 제공 |
| `exposed-jdbc` | `compileOnly` | JDBC 연산에만 필요, 사용자가 직접 추가 |
| `postgis-jdbc` | `compileOnly` | PostGIS 기능 사용 시에만 필요 |
| `pgvector` | `compileOnly` | pgvector 기능 사용 시에만 필요 |
| `postgresql_driver` | `compileOnly` | 런타임에 사용자가 추가 |

---

## 5. Libs.kt 추가 상수

```kotlin
// PostgreSQL Extensions (postgresql_driver 상수 근처에 추가)
const val postgis_jdbc = "net.postgis:postgis-jdbc:2024.1.0"
const val pgvector = "com.pgvector:pgvector:0.1.6"
```

**중요**: `Libs.kt` 수정은 구현 첫 번째 단계로 수행. build.gradle.kts 컴파일에 필요.

추가 위치: `Libs.kt`의 Database Drivers 섹션 (postgresql_driver 근처).

### 5.1 버전 선택 근거

| 라이브러리 | 버전 | 비고 |
|-----------|------|------|
| postgis-jdbc | 2024.1.0 | Maven Central 최신 안정 버전 (2024년 릴리즈) |
| pgvector | 0.1.6 | experimental과 동일, Maven Central 최신 |

---

## 6. 테스트 전략

### 6.1 테스트 인프라

기능별로 필요한 Docker 이미지가 다르므로 **별도 컨테이너** 전략을 사용한다. 각 테스트 클래스에서 해당 기능에 적합한 이미지를 사용:

| 기능 | Docker 이미지 | 필요 Extension |
|------|--------------|----------------|
| PostGIS | `postgis/postgis:16-3.4` | 자동 설치됨 |
| pgvector | `pgvector/pgvector:pg16` | `CREATE EXTENSION IF NOT EXISTS vector;` (initScript) |
| tsrange | `postgres:16` | 없음 (내장 타입) |

### 6.2 테스트 분류

```
src/test/kotlin/io/bluetape4k/exposed/postgresql/
    postgis/
        GeoPointColumnTypeTest.kt      -- POINT 읽기/쓰기/null
        GeoPolygonColumnTypeTest.kt    -- POLYGON 읽기/쓰기/null
        GeoExtensionsTest.kt           -- ST_ 공간 연산 통합 테스트
    pgvector/
        VectorColumnTypeTest.kt        -- VECTOR 읽기/쓰기/차원 검증
        VectorExtensionsTest.kt        -- cosine/l2/innerProduct 거리 연산
    tsrange/
        TimestampRangeTest.kt          -- 값 객체 단위 테스트 (DB 불필요)
        TstzRangeColumnTypeTest.kt     -- PostgreSQL TSTZRANGE + H2 fallback
        TstzRangeExtensionsTest.kt     -- 범위 연산 통합 테스트
```

### 6.3 테스트 컨테이너 전략

| 기능 | 필요 컨테이너 | 이미지 |
|------|-------------|--------|
| PostGIS | PostgreSQL + PostGIS | `postgis/postgis:16-3.4` |
| pgvector | PostgreSQL + pgvector | `pgvector/pgvector:pg16` |
| tsrange | PostgreSQL (기본) + H2 | `postgres:16` + H2 in-memory |

PostGIS와 pgvector 테스트는 서로 다른 Docker 이미지를 사용하므로 테스트 클래스에서 별도의 컨테이너를 정의한다.

### 6.4 테스트 패턴

```kotlin
// Testcontainers 기반 PostgreSQL 테스트 패턴 (기존 exposed-jdbc-tests 활용)
@Testcontainers
class GeoExtensionsTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgis/postgis:16-3.4")
            .withDatabaseName("testdb")
            .withInitScript("postgis-init.sql")  // CREATE EXTENSION IF NOT EXISTS postgis;
    }

    // 테스트 메서드들...
}
```

### 6.5 H2 Fallback 테스트 (tsrange)

`TstzRangeColumnType`은 H2에서 `VARCHAR(120)` fallback을 지원하므로, H2 in-memory DB로도 기본 CRUD 테스트를 수행한다.

---

## 7. README 작성 계획

`data/postgresql/README.md` 파일을 모듈과 함께 생성한다.

### 7.1 README 구조

```markdown
# bluetape4k-postgresql

PostgreSQL 전용 Exposed 확장 모듈. PostGIS 공간 데이터, pgvector 벡터 검색,
TSTZRANGE 시간 범위 컬럼 타입을 제공합니다.

## 기능

### PostGIS (공간 데이터)
- POINT, POLYGON 컬럼 타입
- ST_Distance, ST_DWithin, ST_Contains 등 공간 연산

### pgvector (벡터 검색)
- VECTOR(n) 컬럼 타입
- Cosine Distance, L2 Distance, Inner Product 연산

### TSTZRANGE (시간 범위)
- TSTZRANGE 컬럼 타입 (H2 fallback 지원)
- 범위 겹침, 포함, 인접 연산

## 의존성

(Gradle 설정 예시)

## 사용 예제

(각 기능별 간단한 코드 예제)
```

---

## 8. 마이그레이션 체크리스트

- [ ] `data/postgresql/` 디렉토리 생성
- [ ] `build.gradle.kts` 작성
- [ ] `Libs.kt`에 `postgis_jdbc`, `pgvector` 상수 추가
- [ ] experimental 소스를 새 패키지 구조로 복사 및 패키지명 변경
- [ ] KDoc 한국어 작성/검토
- [ ] 테스트 마이그레이션 및 Testcontainers 설정
- [ ] H2 fallback 테스트 (tsrange)
- [ ] PostGIS 공간 연산 통합 테스트
- [ ] pgvector 거리 연산 통합 테스트
- [ ] Detekt 정적 분석 통과
- [ ] `data/postgresql/README.md` 작성
- [ ] CLAUDE.md Architecture > Module Structure 섹션에 `bluetape4k-postgresql` 추가

---

## 9. 사용자 가이드 (의존성 선택)

모듈 사용 시 필요한 기능에 따라 런타임 의존성을 추가한다:

```kotlin
dependencies {
    // 기본 (tsrange만 사용)
    implementation("io.github.bluetape4k:bluetape4k-postgresql:$version")

    // PostGIS 사용 시
    implementation("net.postgis:postgis-jdbc:2023.1.0")

    // pgvector 사용 시
    implementation("com.pgvector:pgvector:0.1.6")

    // PostgreSQL 드라이버 (필수)
    runtimeOnly("org.postgresql:postgresql:42.7.10")
}
```

---

## 10. 향후 확장 고려사항

- **PostgreSQL JSONB 연산**: Exposed의 기본 JSON 지원과 별도로 PostgreSQL 전용 JSONB 연산자(`@>`, `?`, `?|`, `?&`) 추가 가능
- **PostgreSQL ARRAY 타입**: `text[]`, `int[]` 등 PostgreSQL 배열 컬럼 타입
- **Full Text Search**: `tsvector`, `tsquery` 기반 전문 검색 컬럼 타입
- **LTREE**: 계층 구조 경로 컬럼 타입
- 위 기능들은 동일 모듈(`bluetape4k-postgresql`)에 패키지를 추가하는 방식으로 확장한다
