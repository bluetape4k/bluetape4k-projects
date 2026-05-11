# Bluetape4k Projects

[![CI](https://github.com/bluetape4k/bluetape4k-projects/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/bluetape4k-projects/actions/workflows/ci.yml)
[![Coverage](https://coveralls.io/repos/github/bluetape4k/bluetape4k-projects/badge.svg?branch=develop)](https://coveralls.io/github/bluetape4k/bluetape4k-projects)
[![Maven](https://badges.mvnrepository.com/badge/io.github.bluetape4k/bluetape4k-bom/badge.svg?label=Maven)](https://mvnrepository.com/artifact/io.github.bluetape4k/bluetape4k-bom)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

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
- **Spring Boot**: 4.0.0+
- **Kotlin Exposed**: 1.0.0+
- **Databases**: H2, PostgreSQL, MySQL

## Module Structure

Bluetape4k is a multi-module Gradle project organized by domain.

```mermaid
flowchart TB
    subgraph L5["Integration Layer"]
        SB["spring-boot/*"]
    end

    subgraph L4["Infrastructure Layer"]
        LETTUCE["infra/lettuce"]
        REDISSON["infra/redisson"]
        KAFKA["infra/kafka"]
        R4J["infra/resilience4j"]
        CACHE["cache/*"]
        OTEL["infra/opentelemetry"]
        BUCKET["infra/bucket4j"]
        MICRO["infra/micrometer"]
    end

    subgraph L3["Data Access Layer"]
        direction LR
        EXP["bluetape4k-exposed (별도 레포)"]
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
        TEXTS["bluetape4k-text ↗"]
        AWS["bluetape4k-aws ↗"]
        IMG["bluetape4k-image ↗"]
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
    class SB intLayer
    class JUNIT,TC,UTILS,TEXTS,AWS,IMG crossLayer
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
- ~~**crypto**~~: Encryption (Jasypt PBE, BouncyCastle) — **Deprecated**, use
  `bluetape4k-tink` instead

### AWS Modules → [bluetape4k-aws](https://github.com/bluetape4k/bluetape4k-aws)

> These modules have moved to the standalone **[bluetape4k-aws](https://github.com/bluetape4k/bluetape4k-aws)** repository.

Each service follows a **3-tier API** pattern: `sync` → `async (CompletableFuture)` → `coroutines (suspend)`

- **[aws](https://github.com/bluetape4k/bluetape4k-aws)**: AWS Java SDK v2 — DynamoDB, S3 (TransferManager), SES, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS with Coroutines extensions
- **[aws-kotlin](https://github.com/bluetape4k/bluetape4k-aws)**: AWS Kotlin SDK — native `suspend` functions; DynamoDB, S3, SES/SESv2, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS with DSL support (`metricDatum {}`, `stsClientOf {}`, etc.)

### Data Modules (`data/`)

#### Exposed Modules

> **이동됨**: Exposed ORM 관련 모듈(38개)은 독립 레포 **[bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)**로 분리됐습니다.
> 그룹 ID: `io.bluetape4k.exposed`, 버전: `1.8.0-SNAPSHOT`

#### Other Data Modules

- **[hibernate](./data/hibernate/README.md)/[hibernate-reactive](./data/hibernate-reactive/README.md)**: Hibernate ORM integration
- **[jdbc](./data/jdbc/README.md)**: JDBC utilities
- **[mongodb](./data/mongodb/README.md)**: MongoDB Kotlin Coroutine Driver extensions — `mongoClient {}` DSL, `findFirst`, `exists`, `upsert`, `findAsFlow`, `documentOf {}`, Aggregation Pipeline DSL
- **[r2dbc](./data/r2dbc/README.md)**: R2DBC support

### Infrastructure Modules (`infra/`)

- **[redis](./infra/redis/README.md)**: Lettuce/Redisson umbrella module (backward compatible)
    - **[lettuce](./infra/lettuce/README.md)**: Lettuce client, high-performance codecs (Jdk/Kryo/Fory × GZip/LZ4/Snappy/Zstd), `RedisFuture` → Coroutines adapters, distributed primitives (Lock, Semaphore, AtomicLong, Leader Election), `MapLoader`/`MapWriter`/`LettuceLoadedMap` (Read-through/Write-through/Write-behind), **BloomFilter/CuckooFilter** (Lua-script based, no RedisBloom extension needed), **HyperLogLog** (PFADD/PFCOUNT/PFMERGE)
    - **[redisson](./infra/redisson/README.md)**: Redisson client, Codec, Memoizer, NearCache (`RLocalCachedMap`), Leader Election (with Coroutines support)
- **[bucket4j](./infra/bucket4j/README.md)**: Rate limiting
- **[kafka](./infra/kafka/README.md)**: Kafka client
- **[kafka-logback](./infra/kafka-logback/README.md)**: Logback Kafka Appender (promoted from `x-obsoleted/logback-kafka`)
- **[micrometer](./infra/micrometer/README.md)**: Metrics
- **[opentelemetry](./infra/opentelemetry/README.md)**: Distributed tracing
- **[resilience4j](./infra/resilience4j/README.md)**: Resilience4j + Coroutines, Coroutines-native cache

#### Cache Modules (`cache/`)

A pluggable cache abstraction layer — swap backends without changing application code.

- **[cache-core](cache/cache-core/README.md)**: JCache abstraction + Caffeine/Cache2k/Ehcache local caches (merged from former `cache-local`) — `AsyncCache`, `SuspendCache`, `NearCache`, `SuspendNearCache`, Memoizer implementations, 6 abstract test fixtures
- **[cache-hazelcast](cache/cache-hazelcast/README.md)**: Hazelcast distributed cache + Caffeine 2-tier Near Cache (merged from former `cache-hazelcast-near`)
- **[cache-redisson](cache/cache-redisson/README.md)**: Redisson distributed cache + Caffeine 2-tier Near Cache (merged from former `cache-redisson-near`)
- **[cache-lettuce](cache/cache-lettuce/README.md)**: Lettuce (Redis) distributed cache — `LettuceNearCacheConfig`, automatic invalidation via RESP3 CLIENT TRACKING
- **[hibernate-cache-lettuce](cache/hibernate-cache-lettuce/README.md)**: Hibernate 2nd Level Cache + Lettuce NearCache (Caffeine L1 + Redis L2) — `LettuceNearCacheRegionFactory`, `LettuceNearCacheStorageAccess`, per-region TTL override, 15 codec variants

### Spring Boot Modules (`spring-boot/`)

Spring Boot 4.x is the only supported Spring Boot line in this repo. Former `spring-boot3/*`
modules were removed and former `spring-boot4/*` modules now publish as versionless
`spring-boot/*` modules.

> **BOM note**: Apply via `implementation(platform(...))` rather than `dependencyManagement { imports }` to avoid conflicts with KGP 2.3.x.

- **[core](./spring-boot/core/README.md)**: Spring Boot common utilities — WebFlux + Coroutines, RestClient DSL (
  `suspendGet`, `suspendPost`, etc.), Jackson 2 customizer, Retrofit2 integration, WebTestClient test utilities
- **[cassandra](./spring-boot/cassandra/README.md)**: Spring Data Cassandra with Coroutines extensions
- **[cassandra-demo](./spring-boot/cassandra-demo/README.md)**: Cassandra usage example
- **[data-redis](./spring-boot/redis/README.md)**: High-performance Spring Data Redis serialization —
  `RedisBinarySerializer`, `RedisCompressSerializer`, `redisSerializationContext {}` DSL
- **[hibernate-lettuce](./spring-boot/hibernate-lettuce/README.md)
  **: Hibernate 2nd Level Cache + Lettuce NearCache Spring Boot Auto-Configuration
- **[hibernate-lettuce-demo](./spring-boot/hibernate-lettuce-demo/README.md)
  **: Hibernate Lettuce NearCache + Spring MVC integration demo
- **[mongodb](./spring-boot/mongodb/README.md)**: Spring Data MongoDB Reactive with Coroutines extensions, Criteria/Query/Update infix DSL
- **[r2dbc](./spring-boot/r2dbc/README.md)**: Spring Data R2DBC with Coroutines extensions

### Text Processing → [bluetape4k-text](https://github.com/bluetape4k/bluetape4k-text)

> These modules have moved to the standalone **[bluetape4k-text](https://github.com/bluetape4k/bluetape4k-text)** repository.

- **[tokenizer-core](https://github.com/bluetape4k/bluetape4k-text)**: Tokenizer common interfaces — `TokenizeRequest/Response`, `BlockwordRequest/Response`, `DictionaryProvider`
- **[tokenizer-korean](https://github.com/bluetape4k/bluetape4k-text)**: Korean morphological analyzer (Open Korean Text)
- **[tokenizer-japanese](https://github.com/bluetape4k/bluetape4k-text)**: Japanese morphological analyzer (Kuromoji IPAdic 0.9.0)
- **[lingua](https://github.com/bluetape4k/bluetape4k-text)**: Language detection — Kotlin DSL wrapper over Lingua (75+ languages)
- **[text-search](https://github.com/bluetape4k/bluetape4k-text)**: Aho-Corasick multi-keyword search — blockword filter, highlight, Flow API

### Image Processing → [bluetape4k-image](https://github.com/bluetape4k/bluetape4k-image)

> These modules have moved to the standalone **[bluetape4k-image](https://github.com/bluetape4k/bluetape4k-image)** repository.

- **[images](https://github.com/bluetape4k/bluetape4k-image)**: Image processing utilities (scrimage — resize, crop, thumbnail, format conversion)
- **[images-vips-api](https://github.com/bluetape4k/bluetape4k-image)**: libvips binding-neutral API — `VipsImage`, `VipsRuntime`, `VipsEncodeOptions`
- **[images-vips-java21](https://github.com/bluetape4k/bluetape4k-image)**: Java 21 JVips/JNI binding — `JVipsRuntime`, `JVipsImage`, `NativeHandle` Cleaner leak guard
- **[images-vips-java25](https://github.com/bluetape4k/bluetape4k-image)**: Java 25 Panama FFM binding — `FfmVipsRuntime`, `FfmVipsImage`, `Arena` lifecycle

### Utility Modules (`utils/`)

> **Moved**: The leader-election module split into a standalone repo **[bluetape4k-leader](https://github.com/bluetape4k/bluetape4k-leader)** (blocking / async / coroutine / virtual-thread leader-election APIs with Redis backend).


- **[geo](./utils/geo/README.md)
  **: Geographic information — unified module covering geocode (Bing/Google), geohash, geoip2 (MaxMind) (merged from former
  `utils/geocode`, `utils/geohash`, `utils/geoip2`)
- **[idgenerators](./utils/idgenerators/README.md)**: ID generators — `Uuid` (V1–V7 unified API), `ULID`, `Ksuid` (Seconds/Millis), `Snowflakers` unified factory, `Flake`, `Hashids`, and more
- **[javatimes](./utils/javatimes/README.md)**: Date/time utilities
- **[jwt](./utils/jwt/README.md)**: JWT processing
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
- **[mock-web-server](./testing/mock-web-server/README.md)**: MVC mock HTTP server Docker image for integration tests
- **[mock-webflux-server](./testing/mock-webflux-server/README.md)
  **: WebFlux mock HTTP server Docker image for integration tests

When rebuilding mock server Docker images with Jib, always disable the Gradle configuration cache:

```bash
./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache
./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache
```

### Virtual Thread Modules (`virtualthread/`)

- **[virtualthread](./virtualthread/README.md)**: Java 21/25 Virtual Thread support
    - **[api](./virtualthread/api/README.md)**: Virtual Thread API with ServiceLoader-based runtime selection
    - **[jdk21](./virtualthread/jdk21/README.md)**: Java 21 Virtual Thread implementation
    - **[jdk25](./virtualthread/jdk25/README.md)**: Java 25 Virtual Thread implementation


### Example Modules (`examples/`)

Demonstration modules showing library usage. Not published to Maven.

- **[coroutines-demo](./examples/coroutines-demo/README.md)**: Kotlin Coroutines usage examples
- **[jpa-querydsl-demo](./examples/jpa-querydsl-demo/README.md)**: JPA + QueryDSL usage examples
- **[redisson-demo](./examples/redisson-demo/README.md)**: Redisson usage examples
- **[virtualthreads-demo](./examples/virtualthreads-demo/README.md)**: Java Virtual Thread usage examples

### Removed / Migrated Modules

The legacy `x-obsoleted/` directory was removed. The table below documents the final disposition of every former entry so older references stay traceable.

| Module                              | Status                                                                                         |
|-------------------------------------|------------------------------------------------------------------------------------------------|
| `logback-kafka`                     | **Promoted** → [`infra/kafka-logback`](./infra/kafka-logback/README.md)                        |
| `tokenizer`                         | **Migrated** → `bluetape4k-text` (tokenizer-core / tokenizer-korean / tokenizer-japanese)      |
| `ahocorasick`                       | **Migrated** → `bluetape4k-text/text-search`                                                   |
| `lingua`                            | **Migrated** → `bluetape4k-text/lingua`                                                        |
| `javers`                            | **Spun off** → standalone repo [bluetape4k-javers](https://github.com/bluetape4k/bluetape4k-javers) |
| `bloomfilter`                       | **Replaced** → `infra/lettuce` BloomFilter / CuckooFilter (Lua-based)                          |
| `vertx-coroutines` / `vertx-sqlclient` / `vertx-webclient` | **Merged** → `bluetape4k-vertx`                                         |
| `mapstruct`, `captcha`, `nats`, `naivebayes`, `mutiny-examples` | **Removed** (low usage)                                              |

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
baseVersion=1.7.0
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
signingKeyId=YOUR_LAST_8_HEX_DIGITS
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingPassword=YOUR_KEY_PASSPHRASE

# Maven Central Snapshots upload parallelism (default: 8)
centralSnapshotsParallelism=8
```

- `signingKeyId` must be the trailing 8 hex digits of the signing subkey ID, for example `5C6DF399`.
- If you accidentally provide a 16-digit long key ID such as `7CF28E155C6DF399`, the build normalizes it to `5C6DF399` and prints a warning.
- GitHub Actions secret `SIGNING_KEY_ID` should contain only the raw value, not `signingKeyId=...`.

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
./bin/repo-test-summary -- ./gradlew :bluetape4k-coroutines:test
```

The recommended workflow: **summarize first, then read raw output only for specific files or tasks.**
