# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

Bluetape4k is a shared Kotlin/JVM backend library collection. It maximizes Kotlin idioms, improves Java libraries, and supports Kotlin Coroutines-based async/non-blocking development.

## Development Guidelines

- [ ] **README 다이어그램**: 모든 모듈 README에 Mermaid UML 다이어그램(class/sequence/flowchart) 포함
- [ ] **KDoc**: 모든 public 클래스·인터페이스·확장 함수에 **한국어** KDoc 필수
- [ ] **커밋 메시지**: 한국어 + 접두사 (`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`)
- [ ] **Kotlin**: 2.3+, extension·DSL 최대 활용
- [ ] **테스트**: JUnit 5 + MockK + Kluent; 예제는 실행 가능한 production 품질
- [ ] **포맷**: IntelliJ IDEA formatter + `.editorconfig` — **ktlint 사용 금지** (프로젝트 스타일 충돌)

## Build Commands

```bash
# Repo summary (prefer over raw git commands)
./bin/repo-status          # git status summary
./bin/repo-diff            # per-file change count
./bin/repo-test-summary -- ./gradlew :module:test   # condensed test output

# Build
./gradlew clean build
./gradlew :bluetape4k-coroutines:build
./gradlew build -x test

# Test
./gradlew test
./gradlew :bluetape4k-io:test
./gradlew test --tests "io.bluetape4k.io.CompressorTest"

# Quality
./gradlew detekt
./gradlew formatKotlin

# Publish
./gradlew publishBluetape4kPublicationToBluetape4kRepository           # SNAPSHOT
./gradlew publishBluetape4kPublicationToBluetape4kRepository -PsnapshotVersion=  # RELEASE
```

## 코드 변경 후

- [ ] 컴파일 + 테스트 실행
- [ ] `docs/testlogs/YYYY-MM.md` 현재 달 파일 맨 위에 결과 기록 (doc-only 변경 제외)
- [ ] 모듈 변경 시: `README.md` **및** `README.ko.md` 동기화 업데이트
- [ ] superpowers 작업 완료 시: `docs/superpowers/index/YYYY-MM.md` 항목 추가 → `docs/superpowers/INDEX.md` 건수 갱신
- [ ] spec/plan 새로 작성 시: `/wiki-update` 실행

## Token-Efficient Workflow

Prefer `./bin/repo-*` scripts over raw git/gradle output. Summarize first; read raw output only for specific files or tasks.

## Architecture

Multi-module Gradle project. `settings.gradle.kts` auto-registers subdirectories as `bluetape4k-{dirname}`.

### Core (`bluetape4k/`)

| Module | Description |
|--------|-------------|
| `core` | Core utilities (assertions, compression) |
| `coroutines` | Coroutines utilities (DeferredValue, Flow extensions, AsyncFlow) |
| `logging` | Logging utilities |
| `bom` | Bill of Materials |

### I/O (`io/`)

| Module | Description |
|--------|-------------|
| `io` | File I/O, compression (LZ4/Zstd/Snappy), serialization (Kryo/Fory) |
| `okio` | Okio Buffer/Sink/Source extensions, Jasypt/Tink encrypt Sink/Source |
| `jackson` / `jackson3` | Jackson 2.x / 3.x integration (all formats: CBOR, CSV, YAML, TOML…) |
| `fastjson2` | FastJSON2 |
| `feign` / `retrofit2` | HTTP clients with Coroutines support |
| `protobuf` / `grpc` | Protobuf utilities + gRPC server/client abstraction |
| `tink` | Google Tink AEAD/DAEAD/MAC encryption (`TinkEncryptor`) |
| `vertx` | Vert.x unified module (core + SQL client + Resilience4j) |
| `netty` / `http` / `avro` / `csv` | Netty, HTTP utils, Avro, CSV |
| ~~`crypto`~~ | Deprecated → use `tink` |

### AWS (`aws/`, `aws-kotlin/`)

3-tier API per service: `sync` → `async (CompletableFuture)` → `coroutines (suspend)`.

- `bluetape4k-aws`: Java SDK v2, services via `compileOnly`, coroutines via `.await()` wrappers
- `bluetape4k-aws-kotlin`: Kotlin SDK, native `suspend` functions, no wrapping needed

### Data (`data/`)

#### Exposed

| Module | Use When |
|--------|----------|
| `exposed` *(umbrella)* | Keep existing code unchanged |
| `exposed-core` | Column types (compress/encrypt/serialize/inet/phone), `HasIdentifier`, `ExposedPage`, `Auditable` + `UserContext` + `AuditableIdTable` (감사 추적) |
| `exposed-dao` | DAO entities, custom IdTable (`KsuidTable`, `SnowflakeIdTable`, etc.), `AuditableEntity` + `AuditableEntityClass` (감사 추적) |
| `exposed-jdbc` | `ExposedRepository`, `SuspendedQuery`, `VirtualThreadTransaction`, `AuditableJdbcRepository` (감사 추적) |
| `exposed-r2dbc` | Reactive `ExposedR2dbcRepository` |
| `exposed-cache` | 공통 캐시 인터페이스 (`JdbcCacheRepository`, `SuspendedJdbcCacheRepository`, `R2dbcCacheRepository`, `JdbcRedisRepository`, `SuspendJdbcRedisRepository`, `R2dbcRedisRepository`) + `LocalCacheConfig` + testFixtures 시나리오 |
| `exposed-jdbc-caffeine` | JDBC + Caffeine 로컬 캐시 (sync + suspend, Read/Write-through/behind, H2 only) |
| `exposed-r2dbc-caffeine` | R2DBC + Caffeine AsyncCache (suspend, Read/Write-through/behind, H2 only) |
| `exposed-jdbc-lettuce` | JDBC + Lettuce Redis cache (sync + suspend, Read/Write-through/behind) |
| `exposed-r2dbc-lettuce` | R2DBC + Lettuce Redis cache (suspend, no `runBlocking`) |
| `exposed-jdbc-redisson` / `exposed-r2dbc-redisson` | Same pattern with Redisson |
| `exposed-jackson` / `exposed-jackson3` / `exposed-fastjson2` | JSON column types |
| `exposed-jasypt` / `exposed-tink` | Encrypted column types |
| `exposed-measured` | Query timing via Micrometer |
| `exposed-postgresql` | PostGIS, pgvector, TSTZRANGE; H2 fallback |
| `exposed-mysql8` | GIS geometry types (8 kinds), JTS, spatial functions |
| `exposed-duckdb` | DuckDB dialect, `DuckDBDatabase` factory, `suspendTransaction`, `queryFlow` |
| `exposed-trino` | Trino JDBC Dialect, TrinoDatabase, suspendTransaction/queryFlow; autocommit 전용 (트랜잭션 미지원) |
| `exposed-bigquery` | BigQuery REST API via H2 SQL generation; `BigQueryContext`, suspend/Flow API |
| `exposed-jdbc-tests` / `exposed-r2dbc-tests` | Shared test infrastructure |

#### Other Data

- `hibernate` / `hibernate-reactive`: Hibernate + Hibernate Reactive
- `hibernate-cache-lettuce`: Hibernate 2nd Level Cache via `LettuceNearCacheRegionFactory` (Caffeine L1 + Redis L2, 15 codecs)
- `mongodb`: MongoDB Kotlin Coroutine Driver — `mongoClient {}` DSL, `findAsFlow`, `pipeline {}` Aggregation DSL
- `jdbc` / `r2dbc` / `cassandra`: JDBC utils, R2DBC, Cassandra driver

### Infrastructure (`infra/`)

| Module | Description |
|--------|-------------|
| `lettuce` | Lettuce Redis client, high-perf codec, distributed primitives (Lock, Semaphore, AtomicLong, Leader Election, Memoizer), sync/async/suspend 3-tier; `MapLoader`/`MapWriter`/`LettuceLoadedMap`; suspend variants without `runBlocking` |
| `redisson` | Redisson client, Codec, Cache, Leader Election, Memoizer, NearCache, Coroutines |
| `redis` *(umbrella)* | lettuce + redisson + spring-data-redis |
| `kafka` | Kafka client |
| `resilience4j` | Resilience4j + Coroutines cache |
| `bucket4j` | Rate limiting |
| `micrometer` | Metrics |
| `opentelemetry` | Distributed tracing |

#### Cache (`infra/cache-*`)

Pluggable NearCache abstraction — swap backends without changing code.

| Module | Description |
|--------|-------------|
| `cache` *(umbrella)* | cache-core + hazelcast + redisson + lettuce |
| `cache-core` | JCache abstraction, Caffeine/Cache2k/Ehcache; `NearCacheOperations<V>`, `SuspendNearCacheOperations<V>`, `ResilientNearCacheDecorator` |
| `cache-hazelcast` | Hazelcast `HazelcastNearCache<V>` / `HazelcastSuspendNearCache<V>` |
| `cache-redisson` | Redisson `RedissonNearCache<V>` (RLocalCachedMap) / suspend variant |
| `cache-lettuce` | Lettuce RESP3 CLIENT TRACKING `LettuceNearCache<V>` / suspend variant |

### Spring Boot 3 (`spring-boot3/`)

`bluetape4k-spring-boot3`: Unified module — Spring core utilities, WebFlux + Coroutines, Retrofit2, test utilities (WebTestClient, Testcontainers).

Other: `cassandra`, `mongodb` (ReactiveMongoOperations coroutines DSL), `redis` (serialization), `r2dbc`.

| Module | Description |
|--------|-------------|
| `exposed-jdbc` (`bluetape4k-spring-boot3-exposed-jdbc`) | Exposed DAO Entity-based Spring Data JDBC Repository — PartTree queries, QBE, Page/Sort support |
| `exposed-r2dbc` (`bluetape4k-spring-boot3-exposed-r2dbc`) | Exposed R2DBC DSL coroutine Spring Data Repository — suspend CRUD, Flow support |
| `exposed-jdbc-demo` (`bluetape4k-spring-boot3-exposed-jdbc-demo`) | Exposed DAO + Spring Data JDBC + Spring MVC integration demo |
| `exposed-r2dbc-demo` (`bluetape4k-spring-boot3-exposed-r2dbc-demo`) | Exposed R2DBC + suspend Repository + Spring WebFlux integration demo |
| `hibernate-lettuce` (`bluetape4k-spring-boot3-hibernate-lettuce`) | Hibernate 2nd Level Cache Lettuce NearCache Auto-Configuration — Spring properties binding, Micrometer Metrics, Actuator Endpoint |
| `hibernate-lettuce-demo` (`bluetape4k-spring-boot3-hibernate-lettuce-demo`) | Hibernate Lettuce NearCache + Spring MVC integration demo |
| `batch-exposed` (`bluetape4k-spring-boot3-batch-exposed`) | Spring Batch + Exposed 통합 — ExposedKeysetItemReader(keyset 페이징), ExposedItemWriter/UpdateItemWriter/UpsertItemWriter, ExposedRangePartitioner(VirtualThread 파티션), ExposedBatchAutoConfiguration |

### Spring Boot 4 (`spring-boot4/`)

Same package namespace (`io.bluetape4k.spring.*`) as Spring Boot 3 for minimal migration effort.

- `core`: Spring Boot 4 utilities, RestClient Coroutines DSL, Retrofit2, Jackson 2.x ObjectMapper customizer
- `cassandra` / `redis` / `mongodb` / `r2dbc`

| Module                                                                      | Description                                                                                                                                                                                         |
|-----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `exposed-jdbc` (`bluetape4k-spring-boot4-exposed-jdbc`)                     | Exposed DAO Entity-based Spring Data JDBC Repository — PartTree queries, QBE, Page/Sort support                                                                                                     |
| `exposed-r2dbc` (`bluetape4k-spring-boot4-exposed-r2dbc`)                   | Exposed R2DBC DSL coroutine Spring Data Repository — suspend CRUD, Flow support                                                                                                                     |
| `exposed-jdbc-demo` (`bluetape4k-spring-boot4-exposed-jdbc-demo`)           | Exposed DAO + Spring Data JDBC + Spring MVC integration demo (Spring Boot 4 BOM)                                                                                                                    |
| `exposed-r2dbc-demo` (`bluetape4k-spring-boot4-exposed-r2dbc-demo`)         | Exposed R2DBC + suspend Repository + Spring WebFlux integration demo (Spring Boot 4 BOM)                                                                                                            |
| `hibernate-lettuce` (`bluetape4k-spring-boot4-hibernate-lettuce`)           | Hibernate 2nd Level Cache Lettuce NearCache Auto-Configuration — Spring properties binding, Micrometer Metrics, Actuator Endpoint                                                                   |
| `hibernate-lettuce-demo` (`bluetape4k-spring-boot4-hibernate-lettuce-demo`) | Hibernate Lettuce NearCache + Spring MVC integration demo (Spring Boot 4 BOM)                                                                                                                       |
| `batch-exposed` (`bluetape4k-spring-boot4-batch-exposed`)                   | Spring Batch 6.x + Exposed 통합 — ExposedKeysetItemReader(keyset 페이징), ExposedItemWriter/UpdateItemWriter/UpsertItemWriter, ExposedRangePartitioner(VirtualThread 파티션), ExposedBatchAutoConfiguration |

> **Spring Boot 4 BOM**: Use `implementation(platform(Libs.spring_boot4_dependencies))` — **not** `dependencyManagement { imports }` (pollutes `kotlinBuildToolsApiClasspath`, breaks KGP 2.3.x).

### Utilities (`utils/`)

| Module | Description |
|--------|-------------|
| `geo` | Geocode (Google Maps/Bing), GeoHash, GeoIP2 (MaxMind) |
| `science` | GIS 좌표계 (BoundingBox/UTM/DMS), Shapefile (GeoTools LGPL), PostGIS DB 적재 파이프라인 |
| `idgenerators` | Ksuid, Snowflake, ULID, UUID |
| `images` | Image processing utilities |
| `javatimes` | Java Time extensions |
| `jwt` | JWT token utilities |
| `leader` | Leader election |
| `math` | Math utilities |
| `measured` | Type-safe units + measurements |
| `money` | Money/currency types |
| `mutiny` | Mutiny reactive extensions |
| `states` | Kotlin DSL FSM — 동기(AtomicReference CAS) + 코루틴(Mutex+StateFlow), Guard 조건, clinic-appointment 패턴 |
| `workflow` | Kotlin DSL Workflow — Sequential/Parallel/Conditional/Repeat/Retry 플로우, 동기(Virtual Threads) + 코루틴(suspend/Flow), ABORTED(break)/CANCELLED/PartialSuccess 지원 |

### Testing (`testing/`)

`junit5` — JUnit 5 extensions · `testcontainers` — Docker-based test infrastructure.

### Virtual Threads (`virtualthread/`)

`api` (ServiceLoader-based runtime selection) · `jdk21` · `jdk25`.

### Other

`timefold/` — Timefold Solver · `examples/` — Usage examples (not published).

## Build Configuration

- **JVM Toolchain**: Java 21 · **Kotlin**: 2.3 · **Gradle**: ZGC daemon, 4–8 GB heap, parallel build (disabled during test), build cache
- `build.gradle.kts` — root config · `settings.gradle.kts` — `includeModules` · `gradle.properties` — versions · `buildSrc/Libs.kt` — dependency versions

### Key Kotlin Compiler Flags

```
-Xjsr305=strict  -jvm-default=enable  -Xinline-classes
-Xcontext-parameters  -Xannotation-default-target=param-property
-opt-in=kotlin.ExperimentalStdlibApi
-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi
```

### Test JVM Options

```
-Xshare:off  -Xmx4G  -XX:+UseG1GC  -XX:+UnlockExperimentalVMOptions  -XX:+EnableDynamicAgentLoading
```

## Kotlin Edit Workflow (MANDATORY)

`.kt` 파일 편집 시 **예외 없이** 아래 순서를 따른다.

### 클래스 수정 전
- [ ] `ide_find_references` 또는 `get_impact_radius_tool` 로 영향받는 파일 파악

### 모든 `.kt` 편집 후
- [ ] `ide_diagnostics` — import 오류·`@Deprecated` 경고 즉시 확인
- [ ] import 오류 → `ide_optimize_imports` 로 수정
- [ ] `@Deprecated` 경고 → `lsp_code_actions` Quick Fix 적용 — 절대 deprecated 사용 방치 금지
- [ ] 위 단계 통과 후에만 빌드/컴파일 실행

## Key Design Patterns

### Coroutines-First

All async work uses Coroutines. Wrap blocking APIs with `withContext(Dispatchers.IO)`. Use internal utilities (e.g., `bluetape4k-core` `RequireSupport.kt`) over external alternatives.

### Record / Model Data Class Pattern

DB Row를 담는 data class (`*Record`, `*Info`, `*Model`)는 반드시 다음 규칙을 따릅니다:

```kotlin
// ✅ 분산 캐시(Lettuce/Redisson) 저장을 위해 Serializable 필수
data class SpatialLayerRecord(
    val id: Long = 0L,
    val name: String,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
```

- [ ] `Serializable` 구현 필수 (Lettuce/Redisson 캐시 직렬화)
- [ ] `companion object : KLogging()` + `private const val serialVersionUID = 1L` 필수
- [ ] `exposed.model` 패키지에 위치

### Repository Generic Pattern

```kotlin
// All repositories: <ID: Any, E: Any> — no table type generic
class MyRepository : AbstractJdbcRepository<Long, MyEntity>() {
    override val table = MyTable
    override fun extractId(entity: MyEntity) = entity.id
}
```
`SoftDeleted*` repositories retain `T` for `table.isDeleted` access.

### NearCache Unified Interface

```kotlin
val cache = lettuceNearCacheOf<MyValue>(redisClient, codec, config)
val resilient = cache.withResilience { retryMaxAttempts = 5 }
```

Key interfaces: `NearCacheOperations<V>` (blocking), `SuspendNearCacheOperations<V>` (suspend), `NearCacheStatistics`.

### High-Performance Optimization

Compression: LZ4/Zstd · Serialization: Kryo/Fory · Custom Redis codecs (faster than official).

### Auditable Pattern (감사 추적)

모든 Exposed 테이블의 생성자, 생성 시간, 수정자, 수정 시간을 자동으로 추적합니다.

**3계층 구조**:
1. **exposed-core**: `Auditable` 인터페이스 + `UserContext` (ScopedValue/ThreadLocal 듀얼 전략) + `AuditableIdTable` (테이블 베이스)
2. **exposed-dao**: `AuditableEntity` (flush() 오버라이드로 createdBy/updatedBy 자동 설정) + `AuditableEntityClass` (DAO)
3. **exposed-jdbc**: `AuditableJdbcRepository` (auditedUpdateById/auditedUpdateAll 메서드로 updatedAt/updatedBy DB CURRENT_TIMESTAMP 자동 설정)

**사용 예시**:
```kotlin
object ArticleTable : AuditableLongIdTable("articles") {
    val title = varchar("title", 255)
}

class Article(id: EntityID<Long>) : AuditableLongEntity(id) {
    companion object : AuditableLongEntityClass<Article>(ArticleTable)
    var title by ArticleTable.title
    override var createdBy by ArticleTable.createdBy
    override var createdAt by ArticleTable.createdAt
    override var updatedBy by ArticleTable.updatedBy
    override var updatedAt by ArticleTable.updatedAt
}

class ArticleRepository : LongAuditableJdbcRepository<ArticleRecord, ArticleTable> { ... }

transaction {
    UserContext.withUser("alice") {
        Article.new { title = "Hello" }  // createdBy="alice", createdAt=DB시각 자동설정
    }
    UserContext.withUser("bob") {
        repo.auditedUpdateById(1L) { it[title] = "Updated" }  // updatedBy="bob", updatedAt=DB시각 자동설정
    }
}
```

**중요**: UPDATE 시에는 반드시 `auditedUpdateById()` 또는 `auditedUpdateAll()`을 사용하세요.

## Version Management

```properties
# gradle.properties
projectGroup=io.github.bluetape4k
baseVersion=1.5.0
snapshotVersion=-SNAPSHOT   # empty for RELEASE
```

## Git Workflow

- [ ] Branch: `develop`
- [ ] Commits: 한국어 + 접두사 (`feat: ...`, `fix: ...`)

## Important Notes

- [ ] **jar 소스 추출**: `.claude/lib-sources/<라이브러리명>/` 사용 — 프로젝트 소스 트리나 `/tmp/` 추출 금지
- [ ] **Publishing**: GitHub Packages Maven; `workshop/`·`examples/` 제외
- [ ] **atomicfu**: 클래스 프로퍼티에서만 사용 — 메서드 로컬 사용 금지
- [ ] **Detekt**: `exposed-jdbc-tests` 에서 비활성화됨
- [ ] **virtualthread-api 변경**: `virtualthread/api` 인터페이스 추가·변경 시 `jdk21` + `jdk25` 동시 수정 필수 — 하나만 수정 금지
