# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

Bluetape4k is a shared Kotlin/JVM backend library collection. It maximizes Kotlin idioms, improves Java libraries, and supports Kotlin Coroutines-based async/non-blocking development.

## Development Guidelines

- **KDoc**: Required for all public classes, interfaces, and extension methods — written in **Korean**
- **Commit messages**: Korean, with prefix (`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`)
- **Kotlin**: 2.3+, use extensions and DSLs wherever possible
- **Stack**: Java 21 · Kotlin 2.3 · Spring Boot 3.4+ · Exposed 1.0+ · H2 / PostgreSQL / MySQL
- **Testing**: JUnit 5 + MockK + Kluent; examples must be production-quality and runnable

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
| `exposed-core` | Column types (compress/encrypt/serialize/inet/phone), `HasIdentifier`, `ExposedPage` |
| `exposed-dao` | DAO entities, custom IdTable (`KsuidTable`, `SnowflakeIdTable`, etc.) |
| `exposed-jdbc` | `ExposedRepository`, `SuspendedQuery`, `VirtualThreadTransaction` |
| `exposed-r2dbc` | Reactive `ExposedR2dbcRepository` |
| `exposed-jdbc-lettuce` | JDBC + Lettuce Redis cache (sync + suspend, Read/Write-through/behind) |
| `exposed-r2dbc-lettuce` | R2DBC + Lettuce Redis cache (suspend, no `runBlocking`) |
| `exposed-jdbc-redisson` / `exposed-r2dbc-redisson` | Same pattern with Redisson |
| `exposed-jackson` / `exposed-jackson3` / `exposed-fastjson2` | JSON column types |
| `exposed-jasypt` / `exposed-tink` | Encrypted column types |
| `exposed-measured` | Query timing via Micrometer |
| `exposed-postgresql` | PostGIS, pgvector, TSTZRANGE; H2 fallback |
| `exposed-mysql8` | GIS geometry types (8 kinds), JTS, spatial functions |
| `exposed-duckdb` | DuckDB dialect, `DuckDBDatabase` factory, `suspendTransaction`, `queryFlow` |
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
| `exposed-jdbc` (`bluetape4k-spring-boot3-exposed-jdbc`) | Exposed DAO Entity 기반 Spring Data JDBC Repository — PartTree 쿼리, QBE, Page/Sort 지원 |
| `exposed-r2dbc` (`bluetape4k-spring-boot3-exposed-r2dbc`) | Exposed R2DBC DSL 기반 코루틴 Spring Data Repository — suspend CRUD, Flow 지원 |
| `exposed-jdbc-demo` (`bluetape4k-spring-boot3-exposed-jdbc-demo`) | Exposed DAO + Spring Data JDBC + Spring MVC 통합 데모 |
| `exposed-r2dbc-demo` (`bluetape4k-spring-boot3-exposed-r2dbc-demo`) | Exposed R2DBC + suspend Repository + Spring WebFlux 통합 데모 |
| `hibernate-lettuce` (`bluetape4k-spring-boot3-hibernate-lettuce`) | Hibernate 2nd Level Cache Lettuce NearCache Auto-Configuration — Properties 바인딩, Micrometer Metrics, Actuator Endpoint |
| `hibernate-lettuce-demo` (`bluetape4k-spring-boot3-hibernate-lettuce-demo`) | Hibernate Lettuce NearCache + Spring MVC 통합 데모 |

### Spring Boot 4 (`spring-boot4/`)

Same package namespace (`io.bluetape4k.spring.*`) as Spring Boot 3 for minimal migration effort.

- `core`: Spring Boot 4 utilities, RestClient Coroutines DSL, Retrofit2, Jackson 2.x ObjectMapper customizer
- `cassandra` / `redis` / `mongodb` / `r2dbc`

| Module | Description |
|--------|-------------|
| `exposed-jdbc` (`bluetape4k-spring-boot4-exposed-jdbc`) | Exposed DAO Entity 기반 Spring Data JDBC Repository — PartTree 쿼리, QBE, Page/Sort 지원 |
| `exposed-r2dbc` (`bluetape4k-spring-boot4-exposed-r2dbc`) | Exposed R2DBC DSL 기반 코루틴 Spring Data Repository — suspend CRUD, Flow 지원 |
| `exposed-jdbc-demo` (`bluetape4k-spring-boot4-exposed-jdbc-demo`) | Exposed DAO + Spring Data JDBC + Spring MVC 통합 데모 (Spring Boot 4 BOM) |
| `exposed-r2dbc-demo` (`bluetape4k-spring-boot4-exposed-r2dbc-demo`) | Exposed R2DBC + suspend Repository + Spring WebFlux 통합 데모 (Spring Boot 4 BOM) |
| `hibernate-lettuce` (`bluetape4k-spring-boot4-hibernate-lettuce`) | Hibernate 2nd Level Cache Lettuce NearCache Auto-Configuration — Properties 바인딩, Micrometer Metrics, Actuator Endpoint |
| `hibernate-lettuce-demo` (`bluetape4k-spring-boot4-hibernate-lettuce-demo`) | Hibernate Lettuce NearCache + Spring MVC 통합 데모 (Spring Boot 4 BOM) |

> **Spring Boot 4 BOM**: Use `implementation(platform(Libs.spring_boot4_dependencies))` — **not** `dependencyManagement { imports }` (pollutes `kotlinBuildToolsApiClasspath`, breaks KGP 2.3.x).

### Utilities (`utils/`)

`geo` (geocode/geohash/geoip2), `idgenerators` (Ksuid/Snowflake/ULID/UUID), `images`, `javatimes`, `jwt`, `leader`, `math`, `measured` (type-safe units + measurements), `money`, `mutiny`.

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
-Xshare:off  -Xmx8G  -XX:+UseZGC  -XX:+UnlockExperimentalVMOptions  -XX:+EnableDynamicAgentLoading
```

## Key Design Patterns

### Coroutines-First

All async work uses Coroutines. Wrap blocking APIs with `withContext(Dispatchers.IO)`. Use internal utilities (e.g., `bluetape4k-core` `RequireSupport.kt`) over external alternatives.

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

## Version Management

```properties
# gradle.properties
projectGroup=io.github.bluetape4k
baseVersion=1.5.0
snapshotVersion=-SNAPSHOT   # empty for RELEASE
```

## Git Workflow

- Branch: `develop`
- Commits: Korean + prefix (`feat: ...`, `fix: ...`)

## Important Notes

- **Publishing**: GitHub Packages Maven; `workshop/` and `examples/` are excluded
- **atomicfu**: `kotlinx-atomicfu` plugin, `jvmVariant = "VH"` — use only as class-level properties (not method-local)
- **Detekt**: disabled for `exposed-jdbc-tests`
- **Jacoco**: commented out (enable when needed)
