# Bluetape4k Projects

[![Maven](https://badges.mvnrepository.com/badge/io.github.bluetape4k/bluetape4k-bom/badge.svg?label=Maven)](https://mvnrepository.com/artifact/io.github.bluetape4k/bluetape4k-bom)

JVM 환경에서 Kotlin 언어로 개발할 때 사용하는 공용 라이브러리

[English](./README.md) | 한국어

![Blue Tape](./doc/bluetape4k.png)

KDoc 작성 지침: `doc/Kdoc_Instruction.md`

## 소개

Kotlin 언어를 배우고, 사용하면서, Backend 개발에 자주 사용하는 기술, Coroutines 등 기존 라이브러리가 제공하지 않는 기능 들을 개발해 왔습니다.

1. Kotlin 의 장점을 최대화 할 수 있는 추천할 만한 코딩 스타일을 제공할 수 있는 기능을 제공합니다.
    - `bluetape4k-core` 의 assertions, required 같은 기능
    - `bluetape4k-measured` 의 조합 가능한 단위 타입(`Units`)과 측정값(`Measure`) 제공

2. 기존 Java 라이브러리를 무지성으로 사용하지 않고, 좀 더 효과적으로 사용할 수 있도록 개선한 기능을 제공합니다.
    - `bluetape4k-core` 의 LZ4, Zstd 등 압축 기능 개선
    - `bluetape4k-redis` 의 lettuce, redisson 용 Codec 제공 (공식 Codec 보다 성능이 월등함)

3. 테스트를 좀 더 완성도 있게 하기 위한 기능을 제공합니다.
    - `bluetape4k-junit5` 다양한 테스트 기법을 Junit5 기반으로 제공합니다.
    - `bluetape4k-testcontainers` 다양한 서비스들을 테스트 환경에서 사용할 수 있도록 합니다.

3. Kotlin Coroutines 등 Async/Non-Blocking 방식의 개발을 지원하는 기능을 제공합니다.
    - `bluetape4k-coroutines` Coroutine 을 사용할 때 유용한 기능을 제공합니다.
    - `bluetape4k-feigh`, `bluetape4k-retrofit2` 등은 HTTP 통신 시 async/non-blocking을 위해 Coroutines 을 사용하도록 합니다

4. AWS SDK 사용 시 성능을 위해 개선한 기능을 제공합니다.
    - `bluetape4k-aws` AWS Java SDK v2 기반으로 DynamoDB, S3, SES, SNS, SQS, KMS, CloudWatch, Kinesis, STS 등을 Async/Non-Blocking 방식으로 사용할 수 있도록 합니다.
    - S3 TransferManager를 활용한 대용량 파일 전송 성능 최적화를 제공합니다.

5. AWS Kotlin SDK 를 사용을 편리하게 하기 위한 기능을 제공합니다.
    - `bluetape4k-aws-kotlin` AWS Kotlin SDK 기반으로 native `suspend` 함수를 기본 제공하여 Coroutines 환경에서 편리하게 사용할 수 있습니다.

6. MSA의 필수인 Resilience4j 에 대한 Kotlin Coroutines 지원을 강화했습니다.
    - `bluetape4k-resilience4j` Resilience4j 를 사용할 때 Kotlin Coroutines 를 사용할 수 있도록 지원합니다.
    - 또한 Coroutines 용 Cache를 추가하여, Coroutines 환경에서도 API 호출 결과를 캐싱할 수 있도록 지원합니다.

7. Redis 를 다양한 방식에서 사용할 수 있도록 지원합니다.
    - `bluetape4k-redis`는 Lettuce, Redisson 용 고성능 Codec 을 제공합니다.
    - Redisson의 다양한 Lock 기능을 Coroutines 환경에서도 사용할 수 있도록 지원합니다.
    - Redis를 분산 캐시로만 사용하는 것이 아니라, Near Cache로 사용할 수 있도록 하여 더욱 성능을 높힐 수 있도록 합니다.

그 외 현업에서 마주쳤던 많은 문제를 해결하는 과정에서 필요로 하는 기능 들을 제공합니다.

앞으로도 필요한 기능들이 있다면 Issue 에 제안 주시기 바랍니다.

## 기술 스택

- **Java**: 21 (JVM Toolchain)
- **Kotlin**: 2.3 (Language & API Version)
- **Spring Boot**: 3.4.0+ / 4.0.0+
- **Kotlin Exposed**: 1.0.0+
- **데이터베이스**: H2, PostgreSQL, MySQL

## 모듈 구조

Bluetape4k는 기능별로 분리된 멀티 모듈 Gradle 프로젝트입니다.

```mermaid
flowchart TB
    subgraph L5["통합 레이어 — Integration"]
        SB3["spring-boot3/*"]
        SB4["spring-boot4/*"]
    end

    subgraph L4["인프라 레이어 — Infrastructure"]
        LETTUCE["infra/lettuce"]
        REDISSON["infra/redisson"]
        KAFKA["infra/kafka"]
        R4J["infra/resilience4j"]
        CACHE["infra/cache-*"]
        OTEL["infra/opentelemetry"]
        BUCKET["infra/bucket4j"]
        MICRO["infra/micrometer"]
    end

    subgraph L3["데이터 접근 레이어 — Data Access"]
        direction LR
        EXP["data/exposed-*"]
        HIB["data/hibernate*"]
        MONGO["data/mongodb"]
        CASS["data/cassandra"]
        JDBC["data/jdbc"]
        R2DBC["data/r2dbc"]
    end

    subgraph L2["I/O 및 직렬화 레이어 — I/O & Serialization"]
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

    subgraph L1["코어 확장 레이어 — Core Extensions"]
        COROU["bluetape4k-coroutines"]
        VT["virtualthread-api"]
    end

    subgraph L0["기반 레이어 — Foundation"]
        CORE["bluetape4k-core"]
        LOG["bluetape4k-logging"]
        BOM["bluetape4k-bom"]
    end

    subgraph CROSS["횡단 관심사 — Cross-cutting"]
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

### Core 모듈 (`bluetape4k/`)

- **[core](./bluetape4k/core/README.ko.md)**: 핵심 유틸리티 (assertions, required, 컬렉션(BoundedStack, RingBuffer, PaginatedList, Permutation), Wildcard 패턴 매칭, XXHasher 등)
- **[coroutines](./bluetape4k/coroutines/README.ko.md)**: Kotlin Coroutines 확장 (DeferredValue, Flow extensions, AsyncFlow)
- **[logging](./bluetape4k/logging/README.ko.md)**: 로깅 관련 기능
- **bom**: Bill of Materials (의존성 관리)

### I/O 모듈 (`io/`)

- **[avro](./io/avro/README.ko.md)**: Apache Avro
- **[csv](./io/csv/README.ko.md)**: CSV 처리
- **[fastjson2](./io/fastjson2/README.ko.md)**: FastJSON2
- **[feign](./io/feign/README.ko.md)**: Feign HTTP 클라이언트 (Coroutines 지원)
- **[grpc](./io/grpc/README.ko.md)**: gRPC 서버/클라이언트 추상화 (`bluetape4k-protobuf` 포함)
- **[http](./io/http/README.ko.md)**: HTTP 유틸리티
- **[io](./io/io/README.ko.md)**: 파일 I/O, 압축(LZ4, Zstd, Snappy, Zip), 직렬화(Kryo, Fory), ZIP 빌더/유틸리티
- **[jackson2](./io/jackson2/README.ko.md)/[jackson3](./io/jackson3/README.ko.md)
  **: Jackson 2.x/3.x 통합 — 바이너리(CBOR, Ion, Smile) 및 텍스트(CSV, YAML, TOML) 포맷 포함 (구 `jackson-binary/text`,
  `jackson3-binary/text` 통합됨)
- **[json](./io/json/README.ko.md)**: JSON 처리
- **[netty](./io/netty/README.ko.md)**: Netty 통합
- **[okio](./io/okio/README.ko.md)
  **: Okio 기반 I/O 확장 — Buffer/Sink/Source 유틸리티, Base64, Channel, Cipher, Compress, Coroutines, Jasypt/Tink 암호화 Sink/Source
- **[protobuf](./io/protobuf/README.ko.md)**: Protobuf 유틸리티 (Timestamp/Duration/Money 변환, ProtobufSerializer)
- **[retrofit2](./io/retrofit2/README.ko.md)**: Retrofit2 HTTP 클라이언트 (Coroutines 지원)
- **[tink](./io/tink/README.ko.md)**: Google Tink 기반 현대적 암호화 — AEAD, Deterministic AEAD, MAC, Digest, 통합 Encryptor (
  `TinkEncryptor`), Okio `TinkEncryptSink`/`TinkDecryptSource`
- **[vertx](./io/vertx/README.ko.md)**: Vert.x 단일 통합 모듈 — 핵심 기능, SQL 클라이언트, Resilience4j 통합 포함 (구 `vertx/core`, `vertx/sqlclient`, `vertx/resilience4j` 통합됨)
- ~~**[crypto](./io/crypto/README.ko.md)**~~: 암호화 기능 (Jasypt 기반 PBE, BouncyCastle) — **Deprecated** (`tink`으로 대체)

### AWS 모듈 (`aws/`)

각 서비스마다 **3단계 API** 패턴 제공: `sync` → `async (CompletableFuture)` → `coroutines (suspend)`

- **[aws](./aws/aws/README.ko.md)
  **: AWS Java SDK v2 기반 단일 통합 모듈 — DynamoDB, S3(TransferManager), SES, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS 포함. 각 서비스의 coroutines 확장 (
  `XxxAsyncClientCoroutinesExtensions.kt`) 제공
- **[aws-kotlin](./aws/aws-kotlin/README.ko.md)**: AWS Kotlin SDK 기반 단일 통합 모듈 — native `suspend` 함수 기본 제공 (
  `.await()` 변환 불필요). DynamoDB, S3, SES/SESv2, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS 포함. DSL 지원 (
  `metricDatum {}`, `inputLogEvent {}`, `stsClientOf {}` 등)

### 데이터 모듈 (`data/`)

#### Exposed 모듈 (기능별 분리)

- **[exposed](./data/exposed/README.ko.md)**: umbrella 모듈 — `exposed-core` + `exposed-dao` + `exposed-jdbc` 묶음 (하위 호환)
- **[exposed-core](./data/exposed-core/README.ko.md)**: JDBC 없이 사용 가능한 핵심 기능 — 압축/암호화/직렬화 컬럼 타입, ID 생성 확장, `HasIdentifier`, `ExposedPage`
- **[exposed-dao](./data/exposed-dao/README.ko.md)**: DAO 엔티티 확장 — `EntityExtensions`, `StringEntity`, 커스텀 IdTable (`KsuidTable`, `SnowflakeIdTable`, `SoftDeletedIdTable` 등)
- **[exposed-fastjson2](./data/exposed-fastjson2/README.ko.md)**: Exposed FastJSON2 JSON 컬럼 지원
- **[exposed-jackson2](./data/exposed-jackson2/README.ko.md)/[jackson3](./data/exposed-jackson3/README.ko.md)
  **: Exposed JSON 컬럼 지원 (Jackson 2.x/3.x)
- **[exposed-jasypt](./data/exposed-jasypt/README.ko.md)**: Exposed Jasypt 암호화 컬럼
- **[exposed-jdbc](./data/exposed-jdbc/README.ko.md)**: JDBC 전용 — `ExposedRepository`, `SoftDeletedRepository`, `SuspendedQuery`, `VirtualThreadTransaction`
- **[exposed-jdbc-redisson](./data/exposed-jdbc-redisson/README.ko.md)**: Exposed JDBC + Redisson (분산 락)
- **[exposed-jdbc-tests](./data/exposed-jdbc-tests/README.ko.md)**: JDBC 기반 테스트 공통 인프라
- **[exposed-measured](./data/exposed-measured/README.ko.md)**: Exposed 쿼리 실행 시간 측정 (Micrometer 통합)
- **[exposed-r2dbc](./data/exposed-r2dbc/README.ko.md)**: Exposed + R2DBC (reactive, `ExposedR2dbcRepository`)
- **[exposed-r2dbc-redisson](./data/exposed-r2dbc-redisson/README.ko.md)**: Exposed R2DBC + Redisson (분산 락)
- **[exposed-r2dbc-tests](./data/exposed-r2dbc-tests/README.ko.md)**: R2DBC 기반 테스트 공통 인프라
- **[exposed-tink](./data/exposed-tink/README.ko.md)**: Exposed 암호화 컬럼 (Google Tink AEAD/Deterministic AEAD)

#### 기타 데이터 모듈

- **[cassandra](./data/cassandra/README.ko.md)**: Cassandra 드라이버
- **[exposed-bigquery](./data/exposed-bigquery/README.ko.md)**: Google BigQuery REST API 통합 — H2(PostgreSQL 모드)로 SQL 생성 후 BigQuery REST 실행, `BigQueryContext`(SELECT/INSERT/UPDATE/DELETE/DDL), `BigQueryResultRow`(Column 참조 타입 안전 접근), suspend/Flow API
- **[exposed-duckdb](./data/exposed-duckdb/README.ko.md)**: DuckDB JDBC 통합 — `DuckDBDialect`(PostgreSQL 상속),
  `DuckDBDatabase` 팩토리(인메모리/파일/읽기전용), `suspendTransaction`, `queryFlow`
- **[exposed-jdbc-lettuce](./data/exposed-jdbc-lettuce/README.ko.md)**: Exposed JDBC + Lettuce Redis 캐시 — Read-through/Write-through/Write-behind, `AbstractJdbcLettuceRepository`, 코루틴 네이티브 `AbstractSuspendedJdbcLettuceRepository`
- **[exposed-mysql8](./data/exposed-mysql8/README.ko.md)
  **: MySQL 8.0 전용 Exposed 확장 — GIS 공간 데이터(8종), JTS 기반 Geometry 컬럼, `ST_Contains`/
  `ST_Distance` 등 공간 함수; MySQL Internal Format WKB 변환
- **[exposed-postgresql](./data/exposed-postgresql/README.ko.md)**: PostgreSQL 전용 Exposed 확장 — PostGIS 공간 데이터(`POINT`/
  `POLYGON`), pgvector 벡터 검색(`VECTOR(n)`), TSTZRANGE 시간 범위 컬럼 타입; H2 fallback 지원
- **[exposed-r2dbc-lettuce](./data/exposed-r2dbc-lettuce/README.ko.md)**: Exposed R2DBC + Lettuce Redis 캐시 — 코루틴 네이티브 Read-through/Write-through/Write-behind, `AbstractR2dbcLettuceRepository`
- **[exposed-trino](./data/exposed-trino/README.ko.md)**: Trino JDBC 통합 —
  `TrinoDialect`, catalog/schema 인식 연결 지원, 코루틴 친화적 쿼리 헬퍼, 분산 SQL 분석 워크로드 지원
- **[hibernate](./data/hibernate/README.ko.md)/[hibernate-reactive](./data/hibernate-reactive/README.ko.md)**: Hibernate ORM 통합
- **[hibernate-cache-lettuce](./data/hibernate-cache-lettuce/README.ko.md)**: Hibernate 2nd Level Cache + Lettuce NearCache (Caffeine L1 + Redis L2) — `LettuceNearCacheRegionFactory`, `LettuceNearCacheStorageAccess`, region별 TTL 오버라이드, 15가지 코덱 지원
- **[jdbc](./data/jdbc/README.ko.md)**: JDBC 유틸리티
- **[mongodb](./data/mongodb/README.ko.md)**: MongoDB Kotlin Coroutine Driver 확장 — `mongoClient {}` DSL, `findFirst`, `exists`, `upsert`, `findAsFlow`, `documentOf {}`, Aggregation Pipeline DSL
- **[r2dbc](./data/r2dbc/README.ko.md)**: R2DBC 지원

### 인프라 모듈 (`infra/`)

- **[redis](./infra/redis/README.ko.md)**: Lettuce/Redisson umbrella 모듈 (하위 호환)
    - **[lettuce](./infra/lettuce/README.ko.md)**: Lettuce 클라이언트, 고성능 Codec (Jdk/Kryo/Fory × GZip/LZ4/Snappy/Zstd),
      `RedisFuture` → Coroutines 어댑터, 분산 Primitive (Lock, Semaphore, AtomicLong, Leader Election),
      `MapLoader`/`MapWriter`/`LettuceLoadedMap` (Read-through/Write-through/Write-behind),
      **BloomFilter/CuckooFilter** (Lua 스크립트 기반, RedisBloom 불필요), **HyperLogLog** (PFADD/PFCOUNT/PFMERGE)
    - **[redisson](./infra/redisson/README.ko.md)**: Redisson 클라이언트, Codec, Memorizer, NearCache (`RLocalCachedMap`), Leader Election (Coroutines 지원)
- **[bucket4j](./infra/bucket4j/README.ko.md)**: Rate limiting
- **[kafka](./infra/kafka/README.ko.md)**: Kafka 클라이언트
- **[micrometer](./infra/micrometer/README.ko.md)**: 메트릭
- **[opentelemetry](./infra/opentelemetry/README.ko.md)**: 분산 추적
- **[resilience4j](./infra/resilience4j/README.ko.md)**: Resilience4j + Coroutines, Coroutines Cache

#### 캐시 모듈 (`infra/cache-*`)

플러그인 방식으로 백엔드를 교체할 수 있는 캐시 추상화 레이어입니다.

- **[cache](./infra/cache/README.ko.md)**: umbrella 모듈 (cache-core + hazelcast + redisson + lettuce)
- **[cache-core](./infra/cache-core/README.ko.md)**: JCache 추상화 + Caffeine/Cache2k/Ehcache 로컬 캐시 (구 `cache-local` 병합) — `AsyncCache`, `SuspendCache`, `NearCache`, `SuspendNearCache`, Memorizer 구현체, testFixtures 6종 추상 테스트
- **[cache-hazelcast](./infra/cache-hazelcast/README.ko.md)**: Hazelcast 분산 캐시 + Caffeine 2-Tier Near Cache (구 `cache-hazelcast-near` 병합)
- **[cache-redisson](./infra/cache-redisson/README.ko.md)**: Redisson 분산 캐시 + Caffeine 2-Tier Near Cache (구 `cache-redisson-near` 병합)
- **[cache-lettuce](./infra/cache-lettuce/README.ko.md)**: Lettuce(Redis) 기반 분산 캐시 — `LettuceNearCacheConfig`, RESP3 CLIENT TRACKING 기반 자동 invalidation

### Spring Boot 3 모듈 (`spring-boot3/`)

- **[core](./spring-boot3/core/README.ko.md)
  **: Spring Boot 3 기반 공통 기능 통합 모듈 — Spring core 유틸리티, WebFlux + Coroutines, Retrofit2 통합, 테스트 유틸리티 포함 (구 `spring/core`,
  `spring/webflux`, `spring/retrofit2`, `spring/tests` 통합됨)
- **[cassandra](./spring-boot3/cassandra/README.ko.md)**: Spring Data Cassandra
- **[cassandra-demo](./spring-boot3/cassandra-demo/README.ko.md)**: Spring Boot 3 기반 Cassandra 사용 예제
- **[data-redis](./spring-boot3/redis/README.ko.md)**: Spring Data Redis 고성능 직렬화 — `RedisBinarySerializer`,
  `RedisCompressSerializer`, `redisSerializationContext {}` DSL
- **[exposed-jdbc](./spring-boot3/exposed-jdbc/README.ko.md)
  **: Exposed DAO 엔티티 기반 Spring Data JDBC Repository — PartTree 쿼리, QBE, Page/Sort 지원
- **[exposed-jdbc-demo](./spring-boot3/exposed-jdbc-demo/README.ko.md)
  **: Exposed DAO + Spring Data JDBC + Spring MVC 통합 데모
- **[exposed-r2dbc](./spring-boot3/exposed-r2dbc/README.ko.md)
  **: Exposed R2DBC DSL 기반 코루틴 Spring Data Repository — suspend CRUD, Flow 지원
- **[exposed-r2dbc-demo](./spring-boot3/exposed-r2dbc-demo/README.ko.md)
  **: Exposed R2DBC + suspend Repository + Spring WebFlux 통합 데모
- **[hibernate-lettuce](./spring-boot3/hibernate-lettuce/README.ko.md)
  **: Hibernate 2nd Level Cache + Lettuce NearCache Spring Boot Auto-Configuration — Properties 바인딩, Micrometer Metrics, Actuator Endpoint
- **[hibernate-lettuce-demo](./spring-boot3/hibernate-lettuce-demo/README.ko.md)
  **: Hibernate Lettuce NearCache + Spring MVC 통합 데모
- **[mongodb](./spring-boot3/mongodb/README.ko.md)**: Spring Data MongoDB Reactive — `ReactiveMongoOperations` 코루틴 확장, Criteria/Query/Update infix DSL
- **[r2dbc](./spring-boot3/r2dbc/README.ko.md)**: Spring Data R2DBC

> Spring Data JPA는 `data/hibernate` 모듈로 이동했습니다.

### Spring Boot 4 모듈 (`spring-boot4/`)

Spring Boot 4.x 전용 모듈. Spring Boot 3 모듈과 독립적으로 사용 가능합니다.

> **BOM 적용 주의**: `dependencyManagement { imports }` 대신 `implementation(platform(...))` 방식으로 적용해야 KGP 2.3.x와 충돌 없이 빌드됩니다.

- **[core](./spring-boot4/core/README.ko.md)**: Spring Boot 4 기반 공통 기능 — WebFlux + Coroutines, RestClient DSL (
  `suspendGet`, `suspendPost` 등), Jackson 2 커스터마이저, Retrofit2 통합, WebTestClient 테스트 유틸리티
- **[cassandra](./spring-boot4/cassandra/README.ko.md)**: Spring Data Cassandra 코루틴 확장
- **[cassandra-demo](./spring-boot4/cassandra-demo/README.ko.md)**: Cassandra 사용 예제
- **[data-redis](./spring-boot4/redis/README.ko.md)**: Spring Data Redis 고성능 직렬화 — `RedisBinarySerializer`, `RedisCompressSerializer`, `redisSerializationContext {}` DSL
- **[exposed-jdbc](./spring-boot4/exposed-jdbc/README.ko.md)
  **: Exposed DAO 엔티티 기반 Spring Data JDBC Repository — PartTree 쿼리, QBE, Page/Sort 지원 (Spring Boot 4 BOM)
- **[exposed-jdbc-demo](./spring-boot4/exposed-jdbc-demo/README.ko.md)
  **: Exposed DAO + Spring Data JDBC + Spring MVC 통합 데모 (Spring Boot 4 BOM)
- **[exposed-r2dbc](./spring-boot4/exposed-r2dbc/README.ko.md)
  **: Exposed R2DBC DSL 기반 코루틴 Spring Data Repository — suspend CRUD, Flow 지원 (Spring Boot 4 BOM)
- **[exposed-r2dbc-demo](./spring-boot4/exposed-r2dbc-demo/README.ko.md)
  **: Exposed R2DBC + suspend Repository + Spring WebFlux 통합 데모 (Spring Boot 4 BOM)
- **[hibernate-lettuce](./spring-boot4/hibernate-lettuce/README.ko.md)
  **: Hibernate 2nd Level Cache + Lettuce NearCache Spring Boot Auto-Configuration (Spring Boot 4 BOM)
- **[hibernate-lettuce-demo](./spring-boot4/hibernate-lettuce-demo/README.ko.md)
  **: Hibernate Lettuce NearCache + Spring MVC 통합 데모 (Spring Boot 4 BOM)
- **[mongodb](./spring-boot4/mongodb/README.ko.md)**: Spring Data MongoDB Reactive 코루틴 확장, Criteria/Query/Update infix DSL
- **[r2dbc](./spring-boot4/r2dbc/README.ko.md)**: Spring Data R2DBC 코루틴 확장

### 유틸리티 모듈 (`utils/`)

- **[geo](./utils/geo/README.ko.md)**: 지리 정보 처리 단일 통합 모듈 — geocode(Bing/Google), geohash, geoip2(MaxMind) 포함 (구
  `utils/geocode`, `utils/geohash`, `utils/geoip2` 통합됨)
- **[idgenerators](./utils/idgenerators/README.ko.md)**: ID 생성기 — `Uuid`(V1~V7 통일 API), `ULID`, `Ksuid`(Seconds/Millis), `Snowflakers` 통일 팩토리, `Flake`, `Hashids` 등 다양한 ID 생성 알고리즘 제공
- **[images](./utils/images/README.ko.md)**: 이미지 처리
- **[javatimes](./utils/javatimes/README.ko.md)**: 날짜/시간 유틸리티
- **[jwt](./utils/jwt/README.ko.md)**: JWT 처리
- **[leader](./utils/leader/README.ko.md)**: Leader 선출
- **[math](./utils/math/README.ko.md)**: 수학 유틸리티
- **[measured](./utils/measured/README.ko.md)**: 조합 가능한 단위 타입(`Units`)과 측정값(`Measure`) 기반으로, 복합 단위(`m/s`, `kg*m/s^2`)를 타입 안전하게 표현
- **[money](./utils/money/README.ko.md)**: Money API
- **[mutiny](./utils/mutiny/README.ko.md)**: Mutiny reactive 통합
- **[rule-engine](./utils/rule-engine/README.ko.md)**: 경량 Kotlin Rule Engine — DSL 규칙, 어노테이션 기반 규칙, 스크립트 엔진, 코루틴 실행 지원
- **[science](./utils/science/README.ko.md)
  **: GIS 공간 데이터 처리 — 좌표계 변환(BoundingBox/UTM/DMS, Proj4J), Shapefile 읽기(GeoTools 31.6 LGPL), JTS 기반 공간 기하학 연산, PostGIS DB 적재 파이프라인(SpatialLayerTable/SpatialFeatureTable/PoiTable)
- **[states](./utils/states/README.ko.md)**: Kotlin DSL 기반 유한 상태 머신 라이브러리 — 동기/코루틴 FSM, Guard 조건, `StateFlow` 상태 관찰 지원
- **[workflow](./utils/workflow/README.ko.md)
  **: Kotlin DSL 워크플로우 오케스트레이션 — Sequential/Parallel/Conditional/Repeat/Retry 플로우, 동기(Virtual Threads) + 코루틴(suspend/Flow), ABORTED/CANCELLED/PartialSuccess 지원
- ~~**units**~~: 단위 표현 value class — **Deprecated** (`measured`로 통합)

### 테스트 모듈 (`testing/`)

- **[junit5](./testing/junit5/README.ko.md)**: JUnit 5 확장 및 유틸리티
- **[testcontainers](./testing/testcontainers/README.ko.md)**: Testcontainers 지원 (Redis, Kafka, DB 등)

### Virtual Thread 모듈 (`virtualthread/`)

- **[virtualthread](./virtualthread/README.ko.md)**: Java 21/25 Virtual Thread 지원
    - **[api](./virtualthread/api/README.ko.md)**: Virtual Thread API 및 ServiceLoader 기반 런타임 선택
    - **[jdk21](./virtualthread/jdk21/README.ko.md)**: Java 21 Virtual Thread 구현체
    - **[jdk25](./virtualthread/jdk25/README.ko.md)**: Java 25 Virtual Thread 구현체

### 기타 모듈

- **[timefold](./timefold/solver-persistence-exposed/README.ko.md)**: Timefold Solver + Exposed 통합

### 예제 모듈 (`examples/`)

라이브러리 사용 방법을 보여주는 예제 모듈입니다. 배포되지 않습니다.

- **[coroutines-demo](./examples/coroutines-demo/README.ko.md)**: Kotlin Coroutines 사용 예제
- **[jpa-querydsl-demo](./examples/jpa-querydsl-demo/README.ko.md)**: JPA + QueryDSL 사용 예제
- **[redisson-demo](./examples/redisson-demo/README.ko.md)**: Redisson 사용 예제
- **[virtualthreads-demo](./examples/virtualthreads-demo/README.ko.md)**: Java Virtual Thread 사용 예제

### 폐기된 모듈 (`x-obsoleted/`)

더 이상 유지보수되지 않는 모듈입니다. 빌드에서 제외되었으며 삭제될 예정입니다.

- ~~**vertx-coroutines**~~: Vert.x + Coroutines — `bluetape4k-vertx`로 통합됨
- ~~**vertx-sqlclient**~~: Vert.x SQL Client — `bluetape4k-vertx`로 통합됨
- ~~**vertx-webclient**~~: Vert.x Web Client — `bluetape4k-vertx`로 통합됨
- ~~**mapstruct**~~: MapStruct 통합 — 미사용으로 폐기
- ~~**bloomfilter**~~: Bloom Filter — 사용 빈도 낮아 폐기
- ~~**captcha**~~: CAPTCHA 생성 — 사용 빈도 낮아 폐기
- ~~**logback-kafka**~~: Logback Kafka Appender — 사용 빈도 낮아 폐기
- ~~**nats**~~: NATS 메시징 — 사용 빈도 낮아 폐기
- ~~**javers**~~: JaVers 감사 로그 — 사용 빈도 낮아 폐기
- ~~**tokenizer**~~: 한국어/일본어 형태소 분석기 — 사용 빈도 낮아 폐기
- ~~**ahocorasick**~~: 문자열 검색 (Aho-Corasick) — 사용 빈도 낮아 폐기
- ~~**lingua**~~: 언어 감지 — 사용 빈도 낮아 폐기
- ~~**naivebayes**~~: Naive Bayes 분류기 — 사용 빈도 낮아 폐기
- ~~**mutiny-examples**~~: Mutiny 사용 예제 — 폐기

## 빌드 및 테스트

### 프로젝트 빌드

```bash
# 전체 프로젝트 빌드
./gradlew clean build

# 특정 모듈만 빌드
./gradlew :bluetape4k-coroutines:build

# 테스트 제외하고 빌드
./gradlew build -x test
```

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 모듈 테스트
./gradlew :bluetape4k-io:test

# 특정 테스트 클래스 실행
./gradlew test --tests "io.bluetape4k.io.CompressorTest"

# 상세 로그와 함께 테스트
./gradlew test --info
```

### 코드 품질 검사

```bash
# Detekt 정적 분석 실행
./gradlew detekt
```

## 배포 방법

버전 확인은 `gradle.properties` 파일에서 확인

```properties
projectGroup=io.github.bluetape4k
baseVersion=1.5.0
snapshotVersion=-SNAPSHOT
```

### Maven Central SNAPSHOT 배포

```bash
# 기본 병렬도(centralSnapshotsParallelism=8)로 SNAPSHOT 배포
./gradlew publishAggregationToCentralSnapshots

# 병렬도를 낮춰 서버 부담을 줄이고 싶을 때
./gradlew -PcentralSnapshotsParallelism=4 publishAggregationToCentralSnapshots
```

- 루트 집계 task는 `publishAggregationToCentralSnapshots` 입니다.
- SNAPSHOT 배포는 release 와 달리 ZIP 1회 업로드가 아니라 file-by-file 업로드를 수행합니다.
- 따라서 모듈 수가 많을수록 `PUT` 요청이 많이 발생하는 것이 정상입니다.
- 업로드 대상은 `workshop/**`, `examples/**`, `-demo` 모듈을 제외한 publishable modules 입니다.
- Snapshot 저장소는 `https://central.sonatype.com/repository/maven-snapshots/` 입니다.
- 병렬도는 `centralSnapshotsParallelism` property 로 조절할 수 있습니다. 기본값은 `8` 입니다.

### Maven Central RELEASE 배포

```bash
# snapshotVersion을 제거하고 RELEASE 배포
./gradlew publishAggregationToCentralPortal -PsnapshotVersion= --no-daemon --no-configuration-cache
```

- 루트 집계 task는 `publishAggregationToCentralPortal` 입니다.
- RELEASE 배포는 NMCP aggregation ZIP 을 만들어 Central Portal Publisher API 로 업로드합니다.
- SNAPSHOT과 달리 artifact 파일들을 개별 `PUT` 하지 않으므로 요청 수가 훨씬 적습니다.
- 업로드 대상은 `workshop/**`, `examples/**`, `-demo` 모듈을 제외한 publishable modules 입니다.
- 동일 RELEASE 버전은 재배포할 수 없으므로 실패 시 `baseVersion`을 올려야 합니다.

### 필수 설정 (`~/.gradle/gradle.properties`)

```properties
# Sonatype Central Portal 계정
central.user=your-central-portal-username
central.password=your-central-portal-password

# 권장: In-memory PGP signing
signingUseGpgCmd=false
signingKeyId=YOUR_KEY_ID
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingPassword=YOUR_KEY_PASSPHRASE

# Maven Central Snapshots 업로드 병렬도 (기본값: 8)
centralSnapshotsParallelism=8
```

### 참고

- 기존 `publishAggregationToCentralPortalSnapshots` 는 deprecated alias 이며,
  `publishAggregationToCentralSnapshots` 사용을 권장합니다.
- `publishAllPublicationsToCentralPortalSnapshots` / `publishAllPublicationsToCentralSnapshots` 같은 개별 task 직접 실행 대신 루트 집계 task 사용을 권장합니다.
- SNAPSHOT이 느리거나 요청이 과도해 보이면 `centralSnapshotsParallelism` 값을 `4`, `8`, `12` 정도 범위에서 조절해 보세요.
- RELEASE는 aggregation ZIP 업로드 경로를 사용하므로 SNAPSHOT과 동작 방식이 다릅니다.

### 토큰 절약형 요약 명령

AI 에이전트나 긴 터미널 세션에서 원시 `git`/Gradle 출력을 바로 열기 전에, 아래 요약 명령을 먼저 사용하는 것을 권장합니다.

```bash
# 저장소 상태 요약
./bin/repo-status

# 파일별 diff 변경량 요약
./bin/repo-diff

# Gradle 테스트/빌드 로그 요약
./bin/repo-test-summary -- ./gradlew :05-exposed-dml:01-dml:test
```

기본 흐름은 "요약 먼저, 필요한 파일이나 태스크만 원본 출력 확인"입니다.
