# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

Bluetape4k is a shared Kotlin/JVM backend library collection. It maximizes Kotlin idioms, improves Java libraries, and supports Kotlin Coroutines-based async/non-blocking development.

## Development Guidelines

- [ ] **README Diagrams**: Include Mermaid UML diagrams (class/sequence/flowchart) in every module README
- [ ] **KDoc**: Required on all public classes, interfaces, and extension functions (Korean KDoc is acceptable)
- [ ] **Commit Messages**: Korean + prefix (`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`)
- [ ] **Kotlin**: 2.3+, maximize use of extensions and DSL
- [ ] **Tests**: JUnit 5 + MockK + Kluent; examples must be runnable, production-quality
- [ ] **Format**: IntelliJ IDEA formatter + `.editorconfig` — **no ktlint** (conflicts with project style)

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

## After Code Changes

- [ ] Run compile + tests
- [ ] Record result at the top of Obsidian `wiki/testlogs/YYYY-MM.md` for current month (skip for doc-only changes)
- [ ] When changing a module: sync-update both `README.md` **and** `README.ko.md`
- [ ] After superpowers work: add entry to `docs/superpowers/index/YYYY-MM.md` → update count in `docs/superpowers/INDEX.md`
- [ ] When creating a new spec/plan: run `/wiki-update`

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

### AWS (`aws/`, `aws-kotlin/`)

3-tier API per service: `sync` → `async (CompletableFuture)` → `coroutines (suspend)`.

- `bluetape4k-aws`: Java SDK v2, services via `compileOnly`, coroutines via `.await()` wrappers
- `bluetape4k-aws-kotlin`: Kotlin SDK, native `suspend` functions, no wrapping needed

### Data (`data/`)

#### Exposed

| Module | Use When |
|--------|----------|
| `exposed` *(umbrella)* | Keep existing code unchanged |
| `exposed-core` | Column types (compress/encrypt/serialize/inet/phone), `HasIdentifier`, `ExposedPage`, `Auditable` + `UserContext` + `AuditableIdTable` (audit tracking) |
| `exposed-dao` | DAO entities, custom IdTable (`KsuidTable`, `SnowflakeIdTable`, etc.), `AuditableEntity` + `AuditableEntityClass` (audit tracking) |
| `exposed-jdbc` | `ExposedRepository`, `SuspendedQuery`, `VirtualThreadTransaction`, `AuditableJdbcRepository` (audit tracking) |
| `exposed-r2dbc` | Reactive `ExposedR2dbcRepository` |
| `exposed-cache` | Common cache interfaces (`JdbcCacheRepository`, `SuspendedJdbcCacheRepository`, `R2dbcCacheRepository`, `JdbcRedisRepository`, `SuspendJdbcRedisRepository`, `R2dbcRedisRepository`) + `LocalCacheConfig` + testFixtures scenarios |
| `exposed-jdbc-caffeine` | JDBC + Caffeine local cache (sync + suspend, Read/Write-through/behind, H2 only) |
| `exposed-r2dbc-caffeine` | R2DBC + Caffeine AsyncCache (suspend, Read/Write-through/behind, H2 only) |
| `exposed-jdbc-lettuce` | JDBC + Lettuce Redis cache (sync + suspend, Read/Write-through/behind) |
| `exposed-r2dbc-lettuce` | R2DBC + Lettuce Redis cache (suspend, no `runBlocking`) |
| `exposed-jdbc-redisson` / `exposed-r2dbc-redisson` | Same pattern with Redisson |
| `exposed-jackson` / `exposed-jackson3` / `exposed-fastjson2` | JSON column types |
| `exposed-tink` | Encrypted column types (AEAD/DAEAD via Google Tink) |
| `exposed-measured` | Query timing via Micrometer |
| `exposed-postgresql` | PostGIS, pgvector, TSTZRANGE; H2 fallback |
| `exposed-mysql8` | GIS geometry types (8 kinds), JTS, spatial functions |
| `exposed-duckdb` | DuckDB dialect, `DuckDBDatabase` factory, `suspendTransaction`, `queryFlow` |
| `exposed-trino` | Trino JDBC Dialect, TrinoDatabase, suspendTransaction/queryFlow; autocommit only (no transaction support) |
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
| `batch-exposed` (`bluetape4k-spring-boot3-batch-exposed`) | Spring Batch + Exposed integration — ExposedKeysetItemReader (keyset paging), ExposedItemWriter/UpdateItemWriter/UpsertItemWriter, ExposedRangePartitioner (VirtualThread partition), ExposedBatchAutoConfiguration |

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
| `batch-exposed` (`bluetape4k-spring-boot4-batch-exposed`)                   | Spring Batch 6.x + Exposed integration — ExposedKeysetItemReader (keyset paging), ExposedItemWriter/UpdateItemWriter/UpsertItemWriter, ExposedRangePartitioner (VirtualThread partition), ExposedBatchAutoConfiguration |

> **Spring Boot 4 BOM**: Use `implementation(platform(Libs.spring_boot4_dependencies))` — **not** `dependencyManagement { imports }` (pollutes `kotlinBuildToolsApiClasspath`, breaks KGP 2.3.x).

### Utilities (`utils/`)

| Module | Description |
|--------|-------------|
| `geo` | Geocode (Google Maps/Bing), GeoHash, GeoIP2 (MaxMind) |
| `science` | GIS coordinate systems (BoundingBox/UTM/DMS), Shapefile (GeoTools LGPL), PostGIS DB loading pipeline |
| `idgenerators` | Ksuid, Snowflake, ULID, UUID |
| `images` | Image processing utilities |
| `javatimes` | Java Time extensions |
| `jwt` | JWT token utilities |
| `leader` | Leader election |
| `math` | Math utilities |
| `measured` | Type-safe units + measurements |
| `batch` | Coroutine-native batch framework — `BatchJob/Step/Runner`, `BatchReader/Writer/Processor`, `SkipPolicy`, `InMemoryBatchJobRepository`, `ExposedJdbcBatchJobRepository`, `ExposedR2dbcBatchJobRepository`, keyset paging Reader/Writer, checkpoint restart, Workflow integration |
| `money` | Money/currency types |
| `mutiny` | Mutiny reactive extensions |
| `states` | Kotlin DSL FSM — sync (AtomicReference CAS) + coroutines (Mutex+StateFlow), guard conditions, clinic-appointment pattern |
| `workflow` | Kotlin DSL Workflow — Sequential/Parallel/Conditional/Repeat/Retry flows, sync (Virtual Threads) + coroutines (suspend/Flow), ABORTED(break)/CANCELLED/PartialSuccess support |

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

Follow this order **without exception** when editing `.kt` files.

### Before Modifying a Class
- [ ] Use `ide_find_references` or `get_impact_radius_tool` to identify affected files

### After Every `.kt` Edit
- [ ] `ide_diagnostics` — check import errors and `@Deprecated` warnings immediately
- [ ] Import errors → fix with `ide_optimize_imports`
- [ ] `@Deprecated` warnings → apply Quick Fix via `lsp_code_actions` — never leave deprecated usage unresolved
- [ ] Only run build/compile after passing the above steps

## Key Design Patterns

### Coroutines-First

All async work uses Coroutines. Wrap blocking APIs with `withContext(Dispatchers.IO)`. Use internal utilities (e.g., `bluetape4k-core` `RequireSupport.kt`) over external alternatives.

### Record / Model Data Class Pattern

Data classes holding DB rows (`*Record`, `*Info`, `*Model`) must follow these rules:

```kotlin
// ✅ Serializable required for distributed cache (Lettuce/Redisson) storage
data class SpatialLayerRecord(
    val id: Long = 0L,
    val name: String,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
```

- [ ] Must implement `Serializable` (for Lettuce/Redisson cache serialization)
- [ ] Must have `companion object : KLogging()` + `private const val serialVersionUID = 1L`
- [ ] Place in `exposed.model` package

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

### Auditable Pattern (Audit Tracking)

Automatically tracks creator, creation time, modifier, and modification time on all Exposed tables.

**3-layer structure**:
1. **exposed-core**: `Auditable` interface + `UserContext` (ScopedValue/ThreadLocal dual strategy) + `AuditableIdTable` (table base)
2. **exposed-dao**: `AuditableEntity` (overrides flush() to auto-set createdBy/updatedBy) + `AuditableEntityClass` (DAO)
3. **exposed-jdbc**: `AuditableJdbcRepository` (auditedUpdateById/auditedUpdateAll auto-sets updatedAt/updatedBy via DB CURRENT_TIMESTAMP)

**Usage example**:
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
        Article.new { title = "Hello" }  // createdBy="alice", createdAt=auto-set by DB
    }
    UserContext.withUser("bob") {
        repo.auditedUpdateById(1L) { it[title] = "Updated" }  // updatedBy="bob", updatedAt=auto-set by DB
    }
}
```

**Important**: Always use `auditedUpdateById()` or `auditedUpdateAll()` for UPDATE operations.

## Version Management

```properties
# gradle.properties
projectGroup=io.github.bluetape4k
baseVersion=1.5.0
snapshotVersion=-SNAPSHOT   # empty for RELEASE
```

## Git Workflow

- [ ] Branch: `develop`
- [ ] Commits: Korean + prefix (`feat: ...`, `fix: ...`)

## Important Notes

- [ ] **jar source extraction**: Use `.claude/lib-sources/<library-name>/` — never extract into project source tree or `/tmp/`
- [ ] **Publishing**: GitHub Packages Maven; exclude `workshop/` and `examples/`
- [ ] **atomicfu**: Use only at class property level — never as method-local variables
- [ ] **Detekt**: Disabled in `exposed-jdbc-tests`
- [ ] **virtualthread-api changes**: When adding/modifying interfaces in `virtualthread/api`, always update both `jdk21` and `jdk25` — never modify just one
