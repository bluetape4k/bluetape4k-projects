# Bluetape4k Projects

Shared Kotlin/JVM library collection for backend development

English | [한국어](./README.ko.md)

![Blue Tape](./doc/bluetape4k.png)

KDoc writing guidelines: `doc/Kdoc_Instruction.md`

## Introduction

Bluetape4k was born out of real-world backend development with Kotlin — filling gaps that existing libraries leave open, especially around Coroutines, async I/O, and idiomatic Kotlin patterns.

1. **Idiomatic Kotlin coding style** — utilities that help you write better Kotlin.
    - Assertions and `required`-style helpers in `bluetape4k-core`
    - Composable unit types (`Units`) and measurements (`Measure`) in `bluetape4k-measured`

2. **Improved wrappers around Java libraries** — use proven libraries more effectively.
    - Enhanced LZ4, Zstd compression in `bluetape4k-core`
    - High-performance Lettuce/Redisson codecs in `bluetape4k-redis` (significantly faster than official codecs)

3. **Better testing infrastructure** — write more thorough, reliable tests.
    - `bluetape4k-junit5`: diverse testing techniques on top of JUnit 5
    - `bluetape4k-testcontainers`: Docker-based service containers for integration tests

4. **Async/Non-Blocking development with Kotlin Coroutines**.
    - `bluetape4k-coroutines`: utilities for writing coroutine-based code
    - `bluetape4k-feign`, `bluetape4k-retrofit2`: HTTP clients with native Coroutines support

5. **AWS SDK performance improvements**.
    - `bluetape4k-aws`: AWS Java SDK v2 — DynamoDB, S3, SES, SNS, SQS, KMS, CloudWatch, Kinesis, STS with async/non-blocking APIs
    - Optimized large file transfers via S3 TransferManager

6. **Ergonomic AWS Kotlin SDK wrappers**.
    - `bluetape4k-aws-kotlin`: native `suspend` functions built on the AWS Kotlin SDK — no `.await()` boilerplate needed

7. **Resilience4j with Coroutines support** — essential for microservices.
    - `bluetape4k-resilience4j`: full Coroutines integration for Resilience4j
    - Coroutines-native cache to store API call results in async contexts

8. **Redis at every level**.
    - `bluetape4k-redis`: high-performance codecs for Lettuce and Redisson
    - Coroutines-compatible distributed locking via Redisson
    - Near Cache support to boost throughput beyond simple distributed caching

Feel free to open an Issue if you need something that isn't here yet.

## Tech Stack

- **Java**: 21 (JVM Toolchain)
- **Kotlin**: 2.3 (Language & API Version)
- **Spring Boot**: 3.4.0+ / 4.0.0+
- **Kotlin Exposed**: 1.0.0+
- **Databases**: H2, PostgreSQL, MySQL

## Module Structure

Bluetape4k is a multi-module Gradle project organized by domain.

```mermaid
flowchart TB
    subgraph L5["Integration Layer"]
        SB3["spring-boot3/*"]
        SB4["spring-boot4/*"]
    end

    subgraph L4["Infrastructure Layer"]
        LETTUCE["infra/lettuce"]
        REDISSON["infra/redisson"]
        KAFKA["infra/kafka"]
        R4J["infra/resilience4j"]
        CACHE["infra/cache-*"]
        OTEL["infra/opentelemetry"]
        BUCKET["infra/bucket4j"]
        MICRO["infra/micrometer"]
    end

    subgraph L3["Data Access Layer"]
        direction LR
        EXP["data/exposed-*"]
        HIB["data/hibernate*"]
        MONGO["data/mongodb"]
        CASS["data/cassandra"]
        JDBC["data/jdbc"]
        R2DBC["data/r2dbc"]
    end

    subgraph L2["I/O & Serialization Layer"]
        direction LR
        IO["io/io"]
        JACKSON["io/jackson2·3"]
        FEIGN["io/feign"]
        RETRO["io/retrofit2"]
        GRPC["io/grpc"]
        OKIO["io/okio"]
        TINK["io/tink"]
        VERTX["io/vertx"]
    end

    subgraph L1["Core Extensions Layer"]
        COROU["bluetape4k-coroutines"]
        VT["virtualthread-api"]
    end

    subgraph L0["Foundation Layer"]
        CORE["bluetape4k-core"]
        LOG["bluetape4k-logging"]
        BOM["bluetape4k-bom"]
    end

    subgraph CROSS["Cross-cutting"]
        direction LR
        JUNIT["testing/junit5"]
        TC["testing/testcontainers"]
        UTILS["utils/*"]
        AWS["aws/*"]
    end

    L5 --> L4
    L5 --> L3
    L4 --> L2
    L3 --> L2
    L2 --> L1
    L1 --> L0
    CROSS -.-> L0
    CROSS -.-> L1

    classDef foundation fill:#E8F5E9,stroke:#4CAF50,color:#1B5E20
    classDef coreExt fill:#E3F2FD,stroke:#42A5F5,color:#0D47A1
    classDef ioLayer fill:#FFF3E0,stroke:#FF9800,color:#E65100
    classDef dataLayer fill:#F3E5F5,stroke:#AB47BC,color:#4A148C
    classDef infraLayer fill:#E0F2F1,stroke:#26A69A,color:#004D40
    classDef intLayer fill:#FCE4EC,stroke:#EC407A,color:#880E4F
    classDef crossLayer fill:#FFF9C4,stroke:#FDD835,color:#F57F17

    class CORE,LOG,BOM foundation
    class COROU,VT coreExt
    class IO,JACKSON,FEIGN,RETRO,GRPC,OKIO,TINK,VERTX ioLayer
    class EXP,HIB,MONGO,CASS,JDBC,R2DBC dataLayer
    class LETTUCE,REDISSON,KAFKA,R4J,CACHE,OTEL,BUCKET,MICRO infraLayer
    class SB3,SB4 intLayer
    class JUNIT,TC,UTILS,AWS crossLayer
```

### Core Modules (`bluetape4k/`)

- **[core](./bluetape4k/core/README.md)**: Core utilities — assertions, required helpers, collections (BoundedStack, RingBuffer, PaginatedList, Permutation), wildcard pattern matching, XXHasher, and more
- **[coroutines](./bluetape4k/coroutines/README.md)**: Kotlin Coroutines extensions — DeferredValue, Flow extensions, AsyncFlow
- **[logging](./bluetape4k/logging/README.md)**: Logging utilities
- **bom**: Bill of Materials for dependency management

### I/O Modules (`io/`)

- **[avro](./io/avro/README.md)**: Apache Avro support
- **[csv](./io/csv/README.md)**: CSV processing utilities
- **[fastjson2](./io/fastjson2/README.md)**: FastJSON2 integration
- **[feign](./io/feign/README.md)**: Feign HTTP client with Coroutines support
- **[grpc](./io/grpc/README.md)**: gRPC server/client abstractions (includes `bluetape4k-protobuf`)
- **[http](./io/http/README.md)**: HTTP utilities
- **[io](./io/io/README.md)
  **: File I/O, compression (LZ4, Zstd, Snappy, Zip), serialization (Kryo, Fory), ZIP builder/utilities
- **[jackson2](./io/jackson2/README.md)/[jackson3](./io/jackson3/README.md)
  **: Jackson 2.x/3.x integration — binary (CBOR, Ion, Smile) and text (CSV, YAML, TOML) formats (merged from former
  `jackson-binary/text` and `jackson3-binary/text` modules)
- **[json](./io/json/README.md)**: JSON processing utilities
- **[netty](./io/netty/README.md)**: Netty integration
- **[okio](./io/okio/README.md)
  **: Okio-based I/O extensions — Buffer/Sink/Source utilities, Base64, Channel, Cipher, Compress, Coroutines, Jasypt/Tink encrypt Sink/Source
- **[protobuf](./io/protobuf/README.md)**: Protobuf utilities — Timestamp/Duration/Money conversions, ProtobufSerializer
- **[retrofit2](./io/retrofit2/README.md)**: Retrofit2 HTTP client with Coroutines support
- **[tink](./io/tink/README.md)**: Modern encryption via Google Tink — AEAD, Deterministic AEAD, MAC, Digest, unified
  `TinkEncryptor`, Okio `TinkEncryptSink`/`TinkDecryptSource`
- **[vertx](./io/vertx/README.md)**: Vert.x unified module — core, SQL client, Resilience4j integration (merged from former `vertx/core`, `vertx/sqlclient`, `vertx/resilience4j`)
- ~~**[crypto](./io/crypto/README.md)**~~: Encryption (Jasypt PBE, BouncyCastle) — **Deprecated**, use
  `bluetape4k-tink` instead

### AWS Modules (`aws/`)

Each service follows a **3-tier API** pattern: `sync` → `async (CompletableFuture)` → `coroutines (suspend)`

- **[aws](./aws/aws/README.md)
  **: AWS Java SDK v2 — unified module covering DynamoDB, S3 (TransferManager), SES, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS with per-service Coroutines extensions (
  `XxxAsyncClientCoroutinesExtensions.kt`)
- **[aws-kotlin](./aws/aws-kotlin/README.md)**: AWS Kotlin SDK — native `suspend` functions, no
  `.await()` wrappers needed; covers DynamoDB, S3, SES/SESv2, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS with DSL support (
  `metricDatum {}`, `inputLogEvent {}`, `stsClientOf {}`, etc.)

### Data Modules (`data/`)

#### Exposed Modules (split by function)

- **[exposed](./data/exposed/README.md)**: Umbrella module — bundles `exposed-core` + `exposed-dao` + `exposed-jdbc` for backward compatibility
- **[exposed-core](./data/exposed-core/README.md)**: Core features without JDBC — compressed/encrypted/serialized column types, ID generation extensions, `HasIdentifier`, `ExposedPage`
- **[exposed-dao](./data/exposed-dao/README.md)**: DAO entity extensions — `EntityExtensions`, `StringEntity`, custom IdTables (`KsuidTable`, `SnowflakeIdTable`, `SoftDeletedIdTable`, etc.)
- **[exposed-fastjson2](./data/exposed-fastjson2/README.md)**: FastJSON2 JSON column support for Exposed
- **[exposed-jackson2](./data/exposed-jackson2/README.md)/[jackson3](./data/exposed-jackson3/README.md)
  **: JSON column support for Exposed (Jackson 2.x/3.x)
- **[exposed-jasypt](./data/exposed-jasypt/README.md)**: Jasypt-encrypted columns for Exposed
- **[exposed-jdbc](./data/exposed-jdbc/README.md)**: JDBC-specific — `ExposedRepository`, `SoftDeletedRepository`,
  `SuspendedQuery`, `VirtualThreadTransaction`
- **[exposed-jdbc-redisson](./data/exposed-jdbc-redisson/README.md)**: Exposed JDBC + Redisson distributed locking
- **[exposed-jdbc-tests](./data/exposed-jdbc-tests/README.md)**: Shared test infrastructure for JDBC-based modules
- **[exposed-measured](./data/exposed-measured/README.md)**: Query execution time measurement via Micrometer
- **[exposed-r2dbc](./data/exposed-r2dbc/README.md)**: Exposed + R2DBC reactive support (`ExposedR2dbcRepository`)
- **[exposed-r2dbc-redisson](./data/exposed-r2dbc-redisson/README.md)**: Exposed R2DBC + Redisson distributed locking
- **[exposed-r2dbc-tests](./data/exposed-r2dbc-tests/README.md)**: Shared test infrastructure for R2DBC-based modules
- **[exposed-tink](./data/exposed-tink/README.md)**: Google Tink encrypted columns (AEAD/Deterministic AEAD)

#### Other Data Modules

- **[cassandra](./data/cassandra/README.md)**: Cassandra driver
- **[exposed-bigquery](./data/exposed-bigquery/README.md)**: Google BigQuery REST API integration — SQL generated via H2 (PostgreSQL mode) then executed on BigQuery REST; `BigQueryContext` (SELECT/INSERT/UPDATE/DELETE/DDL), `BigQueryResultRow` (type-safe column access), suspend/Flow API
- **[exposed-duckdb](./data/exposed-duckdb/README.md)**: DuckDB JDBC integration —
  `DuckDBDialect` (extends PostgreSQL dialect), `DuckDBDatabase` factory (in-memory/file/read-only),
  `suspendTransaction`, `queryFlow`
- **[exposed-jdbc-lettuce](./data/exposed-jdbc-lettuce/README.md)**: Exposed JDBC + Lettuce Redis cache — Read-through/Write-through/Write-behind; `AbstractJdbcLettuceRepository`, coroutine-native `AbstractSuspendedJdbcLettuceRepository`
- **[exposed-mysql8](./data/exposed-mysql8/README.md)
  **: MySQL 8.0-specific Exposed extensions — 8 GIS geometry types, JTS-based geometry columns, spatial functions (
  `ST_Contains`, `ST_Distance`, etc.); MySQL Internal Format WKB conversion
- **[exposed-postgresql](./data/exposed-postgresql/README.md)
  **: PostgreSQL-specific Exposed extensions — PostGIS spatial data (`POINT`/`POLYGON`), pgvector vector search (
  `VECTOR(n)`), TSTZRANGE time-range column types; H2 fallback support
- **[exposed-r2dbc-lettuce](./data/exposed-r2dbc-lettuce/README.md)**: Exposed R2DBC + Lettuce Redis cache — coroutine-native Read-through/Write-through/Write-behind; `AbstractR2dbcLettuceRepository`
- **[exposed-trino](./data/exposed-trino/README.md)**: Trino JDBC integration —
  `TrinoDialect`, catalog/schema-aware connection support, coroutine-friendly query helpers, and distributed SQL query workflows for analytics use cases
- **[hibernate](./data/hibernate/README.md)/[hibernate-reactive](./data/hibernate-reactive/README.md)**: Hibernate ORM integration
- **[hibernate-cache-lettuce](./data/hibernate-cache-lettuce/README.md)**: Hibernate 2nd Level Cache + Lettuce NearCache (Caffeine L1 + Redis L2) — `LettuceNearCacheRegionFactory`, `LettuceNearCacheStorageAccess`, per-region TTL override, 15 codec variants
- **[jdbc](./data/jdbc/README.md)**: JDBC utilities
- **[mongodb](./data/mongodb/README.md)**: MongoDB Kotlin Coroutine Driver extensions — `mongoClient {}` DSL, `findFirst`, `exists`, `upsert`, `findAsFlow`, `documentOf {}`, Aggregation Pipeline DSL
- **[r2dbc](./data/r2dbc/README.md)**: R2DBC support

### Infrastructure Modules (`infra/`)

- **[redis](./infra/redis/README.md)**: Lettuce/Redisson umbrella module (backward compatible)
    - **[lettuce](./infra/lettuce/README.md)**: Lettuce client, high-performance codecs (Jdk/Kryo/Fory × GZip/LZ4/Snappy/Zstd), `RedisFuture` → Coroutines adapters, distributed primitives (Lock, Semaphore, AtomicLong, Leader Election), `MapLoader`/`MapWriter`/`LettuceLoadedMap` (Read-through/Write-through/Write-behind), **BloomFilter/CuckooFilter** (Lua-script based, no RedisBloom extension needed), **HyperLogLog** (PFADD/PFCOUNT/PFMERGE)
    - **[redisson](./infra/redisson/README.md)**: Redisson client, Codec, Memoizer, NearCache (`RLocalCachedMap`), Leader Election (with Coroutines support)
- **[bucket4j](./infra/bucket4j/README.md)**: Rate limiting
- **[kafka](./infra/kafka/README.md)**: Kafka client
- **[micrometer](./infra/micrometer/README.md)**: Metrics
- **[opentelemetry](./infra/opentelemetry/README.md)**: Distributed tracing
- **[resilience4j](./infra/resilience4j/README.md)**: Resilience4j + Coroutines, Coroutines-native cache

#### Cache Modules (`infra/cache-*`)

A pluggable cache abstraction layer — swap backends without changing application code.

- **[cache](./infra/cache/README.md)**: Umbrella module (cache-core + hazelcast + redisson + lettuce)
- **[cache-core](./infra/cache-core/README.md)**: JCache abstraction + Caffeine/Cache2k/Ehcache local caches (merged from former `cache-local`) — `AsyncCache`, `SuspendCache`, `NearCache`, `SuspendNearCache`, Memoizer implementations, 6 abstract test fixtures
- **[cache-hazelcast](./infra/cache-hazelcast/README.md)**: Hazelcast distributed cache + Caffeine 2-tier Near Cache (merged from former `cache-hazelcast-near`)
- **[cache-redisson](./infra/cache-redisson/README.md)**: Redisson distributed cache + Caffeine 2-tier Near Cache (merged from former `cache-redisson-near`)
- **[cache-lettuce](./infra/cache-lettuce/README.md)**: Lettuce (Redis) distributed cache — `LettuceNearCacheConfig`, automatic invalidation via RESP3 CLIENT TRACKING

### Spring Boot 3 Modules (`spring-boot3/`)

- **[core](./spring-boot3/core/README.md)
  **: Spring Boot 3 unified module — Spring core utilities, WebFlux + Coroutines, Retrofit2 integration, test utilities (merged from former
  `spring/core`, `spring/webflux`, `spring/retrofit2`, `spring/tests`)
- **[cassandra](./spring-boot3/cassandra/README.md)**: Spring Data Cassandra
- **[cassandra-demo](./spring-boot3/cassandra-demo/README.md)**: Cassandra usage example with Spring Boot 3
- **[data-redis](./spring-boot3/redis/README.md)**: High-performance Spring Data Redis serialization —
  `RedisBinarySerializer`, `RedisCompressSerializer`, `redisSerializationContext {}` DSL
- **[exposed-jdbc](./spring-boot3/exposed-jdbc/README.md)
  **: Exposed DAO entity-based Spring Data JDBC Repository — PartTree queries, QBE, Page/Sort support
- **[exposed-jdbc-demo](./spring-boot3/exposed-jdbc-demo/README.md)
  **: Exposed DAO + Spring Data JDBC + Spring MVC integration demo
- **[exposed-r2dbc](./spring-boot3/exposed-r2dbc/README.md)
  **: Exposed R2DBC DSL-based coroutine Spring Data Repository — suspend CRUD, Flow support
- **[exposed-r2dbc-demo](./spring-boot3/exposed-r2dbc-demo/README.md)
  **: Exposed R2DBC + suspend Repository + Spring WebFlux integration demo
- **[hibernate-lettuce](./spring-boot3/hibernate-lettuce/README.md)
  **: Hibernate 2nd Level Cache + Lettuce NearCache Spring Boot Auto-Configuration — properties binding, Micrometer Metrics, Actuator Endpoint
- **[hibernate-lettuce-demo](./spring-boot3/hibernate-lettuce-demo/README.md)
  **: Hibernate Lettuce NearCache + Spring MVC integration demo
- **[mongodb](./spring-boot3/mongodb/README.md)**: Spring Data MongoDB Reactive — `ReactiveMongoOperations` Coroutines extensions, Criteria/Query/Update infix DSL
- **[r2dbc](./spring-boot3/r2dbc/README.md)**: Spring Data R2DBC

> Spring Data JPA has moved to the `data/hibernate` module.

### Spring Boot 4 Modules (`spring-boot4/`)

Dedicated Spring Boot 4.x modules. Can be used independently from Spring Boot 3 modules.

> **BOM note**: Apply via `implementation(platform(...))` rather than `dependencyManagement { imports }` to avoid conflicts with KGP 2.3.x.

- **[core](./spring-boot4/core/README.md)**: Spring Boot 4 common utilities — WebFlux + Coroutines, RestClient DSL (
  `suspendGet`, `suspendPost`, etc.), Jackson 2 customizer, Retrofit2 integration, WebTestClient test utilities
- **[cassandra](./spring-boot4/cassandra/README.md)**: Spring Data Cassandra with Coroutines extensions
- **[cassandra-demo](./spring-boot4/cassandra-demo/README.md)**: Cassandra usage example
- **[data-redis](./spring-boot4/redis/README.md)**: High-performance Spring Data Redis serialization —
  `RedisBinarySerializer`, `RedisCompressSerializer`, `redisSerializationContext {}` DSL
- **[exposed-jdbc](./spring-boot4/exposed-jdbc/README.md)
  **: Exposed DAO entity-based Spring Data JDBC Repository — PartTree queries, QBE, Page/Sort support (Spring Boot 4 BOM)
- **[exposed-jdbc-demo](./spring-boot4/exposed-jdbc-demo/README.md)
  **: Exposed DAO + Spring Data JDBC + Spring MVC integration demo (Spring Boot 4 BOM)
- **[exposed-r2dbc](./spring-boot4/exposed-r2dbc/README.md)
  **: Exposed R2DBC DSL-based coroutine Spring Data Repository — suspend CRUD, Flow support (Spring Boot 4 BOM)
- **[exposed-r2dbc-demo](./spring-boot4/exposed-r2dbc-demo/README.md)
  **: Exposed R2DBC + suspend Repository + Spring WebFlux integration demo (Spring Boot 4 BOM)
- **[hibernate-lettuce](./spring-boot4/hibernate-lettuce/README.md)
  **: Hibernate 2nd Level Cache + Lettuce NearCache Spring Boot Auto-Configuration (Spring Boot 4 BOM)
- **[hibernate-lettuce-demo](./spring-boot4/hibernate-lettuce-demo/README.md)
  **: Hibernate Lettuce NearCache + Spring MVC integration demo (Spring Boot 4 BOM)
- **[mongodb](./spring-boot4/mongodb/README.md)**: Spring Data MongoDB Reactive with Coroutines extensions, Criteria/Query/Update infix DSL
- **[r2dbc](./spring-boot4/r2dbc/README.md)**: Spring Data R2DBC with Coroutines extensions

### Utility Modules (`utils/`)

- **[geo](./utils/geo/README.md)
  **: Geographic information — unified module covering geocode (Bing/Google), geohash, geoip2 (MaxMind) (merged from former
  `utils/geocode`, `utils/geohash`, `utils/geoip2`)
- **[idgenerators](./utils/idgenerators/README.md)**: ID generators — `Uuid` (V1–V7 unified API), `ULID`, `Ksuid` (Seconds/Millis), `Snowflakers` unified factory, `Flake`, `Hashids`, and more
- **[images](./utils/images/README.md)**: Image processing utilities
- **[javatimes](./utils/javatimes/README.md)**: Date/time utilities
- **[jwt](./utils/jwt/README.md)**: JWT processing
- **[leader](./utils/leader/README.md)**: Leader election
- **[math](./utils/math/README.md)**: Math utilities
- **[measured](./utils/measured/README.md)**: Composable unit types (`Units`) and measurements (`Measure`) — express composite units (`m/s`, `kg*m/s^2`) with full type safety
- **[money](./utils/money/README.md)**: Money/currency API
- **[mutiny](./utils/mutiny/README.md)**: Mutiny reactive integration
- **[rule-engine](./utils/rule-engine/README.md)
  **: Lightweight Kotlin rule engine — DSL rules, annotation-based rules, script engines, and coroutine execution
- **[science](./utils/science/README.md)
  **: GIS spatial data processing — coordinate system conversions (BoundingBox/UTM/DMS, Proj4J), Shapefile reading (GeoTools 31.6 LGPL), JTS-based spatial geometry operations, PostGIS DB ingestion pipeline (SpatialLayerTable/SpatialFeatureTable/PoiTable)
- **[states](./utils/states/README.md)**: Kotlin DSL-based finite state machine library — sync/coroutine FSMs, guards, and `StateFlow` observation
- **[workflow](./utils/workflow/README.md)
  **: Kotlin DSL workflow orchestration — Sequential/Parallel/Conditional/Repeat/Retry flows, sync (Virtual Threads) + coroutine (suspend/Flow), ABORTED/CANCELLED/PartialSuccess support
- ~~**units**~~: Unit value classes — **Deprecated**, merged into `measured`

### Testing Modules (`testing/`)

- **[junit5](./testing/junit5/README.md)**: JUnit 5 extensions and utilities
- **[testcontainers](./testing/testcontainers/README.md)**: Testcontainers support (Redis, Kafka, databases, etc.)

### Virtual Thread Modules (`virtualthread/`)

- **[virtualthread](./virtualthread/README.md)**: Java 21/25 Virtual Thread support
    - **[api](./virtualthread/api/README.md)**: Virtual Thread API with ServiceLoader-based runtime selection
    - **[jdk21](./virtualthread/jdk21/README.md)**: Java 21 Virtual Thread implementation
    - **[jdk25](./virtualthread/jdk25/README.md)**: Java 25 Virtual Thread implementation

### Other Modules

- **[timefold](./timefold/solver-persistence-exposed/README.md)**: Timefold Solver + Exposed integration

### Example Modules (`examples/`)

Demonstration modules showing library usage. Not published to Maven.

- **[coroutines-demo](./examples/coroutines-demo/README.md)**: Kotlin Coroutines usage examples
- **[jpa-querydsl-demo](./examples/jpa-querydsl-demo/README.md)**: JPA + QueryDSL usage examples
- **[redisson-demo](./examples/redisson-demo/README.md)**: Redisson usage examples
- **[virtualthreads-demo](./examples/virtualthreads-demo/README.md)**: Java Virtual Thread usage examples

### Obsolete Modules (`x-obsoleted/`)

No longer maintained. Excluded from the build and scheduled for removal.

- ~~**vertx-coroutines**~~: Vert.x + Coroutines — merged into `bluetape4k-vertx`
- ~~**vertx-sqlclient**~~: Vert.x SQL Client — merged into `bluetape4k-vertx`
- ~~**vertx-webclient**~~: Vert.x Web Client — merged into `bluetape4k-vertx`
- ~~**mapstruct**~~: MapStruct integration — dropped, unused
- ~~**bloomfilter**~~: Bloom Filter — dropped, low usage
- ~~**captcha**~~: CAPTCHA generation — dropped, low usage
- ~~**logback-kafka**~~: Logback Kafka Appender — dropped, low usage
- ~~**nats**~~: NATS messaging — dropped, low usage
- ~~**javers**~~: JaVers audit log — dropped, low usage
- ~~**tokenizer**~~: Korean/Japanese morphological analyzer — dropped, low usage
- ~~**ahocorasick**~~: Aho-Corasick string search — dropped, low usage
- ~~**lingua**~~: Language detection — dropped, low usage
- ~~**naivebayes**~~: Naive Bayes classifier — dropped, low usage
- ~~**mutiny-examples**~~: Mutiny usage examples — dropped

## Building and Testing

### Build the Project

```bash
# Full project build
./gradlew clean build

# Build a specific module
./gradlew :bluetape4k-coroutines:build

# Build without running tests
./gradlew build -x test
```

### Run Tests

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :bluetape4k-io:test

# Run a specific test class
./gradlew test --tests "io.bluetape4k.io.CompressorTest"

# Run with verbose output
./gradlew test --info
```

### Code Quality

```bash
# Run Detekt static analysis
./gradlew detekt
```

## Publishing

Check `gradle.properties` for the current version:

```properties
projectGroup=io.github.bluetape4k
baseVersion=1.5.0
snapshotVersion=-SNAPSHOT
```

### Maven Central SNAPSHOT

```bash
# Publish a SNAPSHOT with default parallelism (centralSnapshotsParallelism=8)
./gradlew publishAggregationToCentralSnapshots

# Reduce parallelism to lower server load
./gradlew -PcentralSnapshotsParallelism=4 publishAggregationToCentralSnapshots
```

- The root aggregation task is `publishAggregationToCentralSnapshots`.
- Unlike a RELEASE, SNAPSHOTs are uploaded file-by-file (not as a single ZIP), so many `PUT` requests are expected for large module counts.
- Publishable targets exclude `workshop/**`, `examples/**`, and `-demo` modules.
- Snapshot repository: `https://central.sonatype.com/repository/maven-snapshots/`
- Adjust parallelism via the `centralSnapshotsParallelism` property (default: `8`).

### Maven Central RELEASE

```bash
# Remove snapshotVersion to publish a RELEASE
./gradlew publishAggregationToCentralPortal -PsnapshotVersion= --no-daemon --no-configuration-cache
```

- The root aggregation task is `publishAggregationToCentralPortal`.
- RELEASE publishing creates an NMCP aggregation ZIP and uploads it via the Central Portal Publisher API — far fewer requests than a SNAPSHOT.
- Publishable targets exclude `workshop/**`, `examples/**`, and `-demo` modules.
- The same RELEASE version cannot be republished; bump `baseVersion` before retrying after a failure.

### Required Configuration (`~/.gradle/gradle.properties`)

```properties
# Sonatype Central Portal credentials
central.user=your-central-portal-username
central.password=your-central-portal-password

# Recommended: in-memory PGP signing
signingUseGpgCmd=false
signingKeyId=YOUR_KEY_ID
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingPassword=YOUR_KEY_PASSPHRASE

# Maven Central Snapshots upload parallelism (default: 8)
centralSnapshotsParallelism=8
```

### Notes

- `publishAggregationToCentralPortalSnapshots` is a deprecated alias — prefer `publishAggregationToCentralSnapshots`.
- Use the root aggregation task rather than running individual tasks like `publishAllPublicationsToCentralPortalSnapshots` or `publishAllPublicationsToCentralSnapshots` directly.
- If SNAPSHOTs are slow or generating too many requests, tune `centralSnapshotsParallelism` in the range of `4`–`12`.
- RELEASE uses an aggregation ZIP upload path, so its behavior differs from SNAPSHOT.

### Token-Efficient Summary Commands

Before opening raw `git`/Gradle output in AI agent sessions or long terminal sessions, use these summary commands first:

```bash
# Repository status summary
./bin/repo-status

# Per-file diff change count summary
./bin/repo-diff

# Condensed Gradle test/build log
./bin/repo-test-summary -- ./gradlew :05-exposed-dml:01-dml:test
```

The recommended workflow: **summarize first, then read raw output only for specific files or tasks.**
