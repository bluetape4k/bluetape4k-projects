# Database Dialects

> 마지막 업데이트: 2026-04-07 | 관련 specs: 4개

## 개요

bluetape4k는 표준 Exposed ORM에 특수 DB 방언을 추가하는 모듈 시리즈를 제공한다. 모든 방언은 `PostgreSQLDialect`를 상속하고 각 DB의 제약에 맞게 메타데이터 및 연결 래퍼를 조정한다.

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| 모든 커스텀 Dialect: `PostgreSQLDialect` 상속 | Trino/DuckDB/BigQuery 모두 ANSI SQL 호환. PostgreSQL 방언이 가장 근접 | 2026-03-28 | exposed-bigquery-duckdb-migration-design |
| PostgreSQL: 단일 모듈 통합 (postgis+pgvector+tsrange) | 3가지 모두 PostgreSQL 전용. 독립 배포 필요성 없음. 패키지로 분리 | 2026-03-28 | postgresql-module-design |
| tsrange: H2 fallback 지원 | PostgreSQL 없이 단위 테스트 가능 | 2026-03-28 | postgresql-module-design |
| DuckDB: autocommit 전용 | DuckDB JDBC는 FK 캐싱 미지원 등 제약 존재. `DuckDBDialectMetadata`로 우회 | 2026-03-28 | exposed-bigquery-duckdb-migration-design |
| Trino: autocommit 전용, 트랜잭션 미지원 | Trino Memory connector는 BEGIN/COMMIT/ROLLBACK 미지원 | 2026-04-03 | exposed-trino-design |
| BigQuery: REST API via H2 SQL generation | BigQuery는 표준 JDBC 없음. H2(PostgreSQL 모드)로 SQL 문자열 생성 후 REST 실행 | 2026-03-28 | exposed-bigquery-duckdb-migration-design |
| MySQL8: 패키지 `mysql8.gis` 유지 | 향후 mysql8 아래 `fulltext`, `json` 서브패키지 추가 가능성 고려 | 2026-03-28 | exposed-mysql8-migration-design |

## 패턴 & 사용법

### PostgreSQL: PostGIS / pgvector / TSTZRANGE

모듈명: `bluetape4k-exposed-postgresql`  
패키지: `io.bluetape4k.exposed.postgresql.{postgis|pgvector|tsrange}`

#### PostGIS 공간 데이터

```kotlin
// 테이블 정의
object LocationTable : IntIdTable("locations") {
    val point = geoPoint("point")       // GEOMETRY(POINT, 4326)
    val area = geoPolygon("area")       // GEOMETRY(POLYGON, 4326)
}

// 공간 쿼리
LocationTable
    .select { LocationTable.area.stContains(targetPoint) }
    .toList()

LocationTable
    .select { LocationTable.point.stDWithin(center, 1000.0) }  // 1km 이내
```

**지원 ST_ 함수**: `stDistance`, `stDWithin`, `stWithin`, `stContains`, `stOverlaps`, `stIntersects`, `stDisjoint`, `stArea`

**테스트 이미지**: `postgis/postgis:16-3.4` (PostGIS 자동 설치됨)

#### pgvector 벡터 검색

```kotlin
object EmbeddingTable : LongIdTable("embeddings") {
    val vector = vector("embedding", dimension = 1536)  // VECTOR(1536)
}

// 벡터 유사도 검색
EmbeddingTable
    .select { EmbeddingTable.vector.cosineDistance(queryVector).less(0.1) }
    .orderBy(EmbeddingTable.vector.l2Distance(queryVector))
```

**지원 연산**: `cosineDistance`, `l2Distance`, `innerProduct`

**초기화 필요**:
```kotlin
// Connection 단위 1회 호출
registerVectorType(connection)
```

**테스트 이미지**: `pgvector/pgvector:pg16` + `CREATE EXTENSION IF NOT EXISTS vector`

#### TSTZRANGE 시간 범위

```kotlin
object EventTable : LongIdTable("events") {
    val period = tstzRange("period")  // TSTZRANGE
}

// 범위 쿼리
EventTable
    .select { EventTable.period.overlaps(searchRange) }    // &&
    .select { EventTable.period.contains(targetInstant) }  // @>
```

**TimestampRange**: `data class TimestampRange(start: Instant, end: Instant, lowerInclusive: Boolean = true, upperInclusive: Boolean = false)`

**H2 fallback**: `VARCHAR(120)` (직렬화: `[2024-01-01T00:00:00Z,2024-12-31T23:59:59Z)`)

**테스트**: PostgreSQL(`postgres:16`) + H2 인메모리 두 가지 모두 검증

#### 의존성 선택

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-postgresql:$version")

    // 필요한 기능만 선택
    implementation("net.postgis:postgis-jdbc:2024.1.0")    // PostGIS 사용 시
    implementation("com.pgvector:pgvector:0.1.6")           // pgvector 사용 시
    runtimeOnly("org.postgresql:postgresql:42.7.x")         // 드라이버 필수
}
```

### MySQL8: GIS 공간 데이터

모듈명: `bluetape4k-exposed-mysql8`  
패키지: `io.bluetape4k.exposed.mysql8.gis`

```kotlin
object SpatialTable : LongIdTable("spatial") {
    val point = geoPoint("point")          // MySQL Internal Format
    val polygon = geoPolygon("polygon")
    val lineString = geoLineString("line")
    // + 5가지 추가 geometry 타입
}

// 공간 쿼리 (18개 확장 함수)
SpatialTable
    .select { SpatialTable.polygon.stContains(targetPoint) }
    .select { SpatialTable.point.stDistance(other).less(1000.0) }
```

**특이사항**: MySQL은 PostgreSQL과 달리 **WKB 내부 포맷**이 다름 — `MySqlWkbUtils`로 변환

```kotlin
// MySQL Internal Format <-> JTS Geometry 변환
val geometry: Geometry = MySqlWkbUtils.fromMySqlBytes(bytes)
val bytes: ByteArray = MySqlWkbUtils.toMySqlBytes(geometry, SRID_WGS84)
```

**JTS 의존성**: `org.locationtech.jts:jts-core:1.20.0`

**지원 spatial 함수 (18개)**: `stContains`, `stDistance`, `stDWithin`, `stWithin`, `stOverlaps`, `stIntersects`, `stDisjoint`, `stEquals`, `stTouches`, `stCrosses`, `stLength`, `stArea`, `stBuffer`, `stConvexHull`, `stCentroid`, `stBoundary`, `stEnvelope`, `stAsText`

**테스트 이미지**: `mysql:8.0` (Testcontainers MySQL)

### DuckDB: autocommit 전용

모듈명: `bluetape4k-exposed-duckdb`  
패키지: `io.bluetape4k.exposed.duckdb`

```kotlin
// 인메모리 DB
val db = DuckDBDatabase.inMemory()

// 파일 DB
val db = DuckDBDatabase.file("/path/to/db.duckdb")

// 표준 transaction 사용 (autocommit 세션)
transaction(db) {
    SchemaUtils.create(EventTable)
    EventTable.insert { it[name] = "test" }
    EventTable.selectAll().toList()
}

// suspend 트랜잭션
db.suspendTransaction {
    EventTable.selectAll().toList()
}

// Flow 스트리밍
db.queryFlow {
    EventTable.selectAll()
}.collect { row -> ... }
```

**DuckDBConnectionWrapper**: FK 캐싱 우회 (`getImportedKeys` 미지원 대응)

**DuckDBDialectMetadata**: `fillConstraintCacheForTables` no-op

**JVM 옵션 필수** (native library):
```kotlin
tasks.test {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
```

**테스트**: Testcontainer 불필요 — 인메모리로 로컬 테스트 가능

### Trino: autocommit 전용 (트랜잭션 미지원)

모듈명: `bluetape4k-exposed-trino`  
패키지: `io.bluetape4k.exposed.trino`

```kotlin
// 연결
val db = TrinoDatabase.connect(
    host = "localhost",
    port = 8080,
    catalog = "memory",
    schema = "default"
)

// 또는 JDBC URL
val db = TrinoDatabase.connect("jdbc:trino://localhost:8080/memory/default")

// transaction {} 사용 (실질적으로 autocommit 세션)
transaction(db) {
    SchemaUtils.create(EventTable)
    EventTable.insert { it[name] = "test" }
}

// suspend 트랜잭션
db.suspendTransaction {
    EventTable.selectAll().toList()
}
```

**중요 주의사항**:

```
⚠️ Trino는 트랜잭션을 지원하지 않습니다.
transaction {} 블록은 autocommit 모드로 실행됩니다:
- 원자성(Atomicity) 보장 없음
- 블록 중간 실패 시 앞선 DML은 롤백되지 않음
- Nested transaction / Savepoint 미지원
```

**TrinoConnectionWrapper**:
- `getAutoCommit()` → `true` 강제
- `commit()`, `rollback()` → no-op (Exposed 프레임워크 호환 어댑터)
- `prepareStatement(sql, autoGeneratedKeys)` → `prepareStatement(sql)` 위임

**TrinoDialectMetadata**: `fillConstraintCacheForTables` no-op (FK 메타데이터 미지원)

**테스트 이미지**: `trinodb/trino:475` + `TrinoServer.Launcher.shared`

### BigQuery: REST API via H2 SQL generation

모듈명: `bluetape4k-exposed-bigquery`  
패키지: `io.bluetape4k.exposed.bigquery`

```kotlin
// BigQueryContext: H2로 SQL 생성 → BigQuery REST API 실행
val ctx = BigQueryContext.create(
    projectId = "my-project",
    datasetId = "my-dataset",
    credentials = GoogleCredentials.getApplicationDefault()
)

// suspend API
val rows: List<BigQueryResultRow> = ctx.suspendQuery {
    EventTable.selectAll().where { EventTable.name eq "test" }
}

// Flow 스트리밍 (페이지네이션 자동 처리)
ctx.queryFlow {
    EventTable.selectAll()
}.collect { row -> ... }

// 타입 안전 접근
val name: String = row[EventTable.name]
val count: Long = row[EventTable.count]
```

**동작 원리**:
1. H2(PostgreSQL 모드)로 Exposed DSL → SQL 문자열 생성
2. `BigQueryDialect`가 BigQuery 호환 SQL로 조정
3. Google BigQuery REST API로 실행
4. `BigQueryResultRow`로 타입 안전 결과 반환

**테스트**: BigQuery Emulator Testcontainer (`testcontainers_gcloud` — `ghcr.io/goccy/bigquery-emulator`)

### 방언별 비교 요약

| DB | JDBC | 트랜잭션 | 테스트 방식 | 특수 사항 |
|----|------|---------|-----------|---------|
| PostgreSQL | ✅ | ✅ | Testcontainers `postgres:16` | PostGIS/pgvector extension 필요 |
| MySQL8 | ✅ | ✅ | Testcontainers `mysql:8.0` | WKB 내부 포맷 변환 |
| DuckDB | ✅ (JDBC) | ❌ (autocommit) | 인메모리 (Docker 불필요) | native library, JVM 옵션 필요 |
| Trino | ✅ (JDBC) | ❌ (autocommit) | Testcontainers `trinodb/trino:475` | Memory connector 기준 |
| BigQuery | ❌ (REST API) | ❌ | BigQuery Emulator | H2로 SQL 생성 |

## 선택하지 않은 방식 / 트레이드오프

| 방식 | 채택하지 않은 이유 |
|------|-----------------|
| PostgreSQL 기능별 독립 모듈 | 항상 함께 사용됨. 통합이 자연스러움 |
| Trino에서 transaction 래핑 제거 | Exposed 프레임워크와의 호환성 유지. `transaction {}` API 일관성 |
| BigQuery JDBC 드라이버 사용 | 공식 BigQuery JDBC 드라이버가 제한적. REST API가 더 안정적 |
| DuckDB 별도 dialect 없이 PostgreSQL 그대로 | `getImportedKeys` 등 JDBC 메타데이터 호환성 문제 발생 |
| exposed-bigquery에 Trino 통합 | 모듈 책임 분리. `exposed-bigquery-trino` 별도 모듈로 Phase 2에서 제공 |

## 관련 페이지

- [module-decisions.md](module-decisions.md) — includeModules 자동 등록, compileOnly 전략
- [exposed-patterns.md](exposed-patterns.md) — Exposed 기본 패턴
- [infrastructure-patterns.md](infrastructure-patterns.md) — Testcontainers 서버
