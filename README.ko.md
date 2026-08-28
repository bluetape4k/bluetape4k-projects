# Bluetape4k Projects

[![CI](https://github.com/bluetape4k/bluetape4k-projects/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/bluetape4k-projects/actions/workflows/ci.yml)
[![Coverage](https://coveralls.io/repos/github/bluetape4k/bluetape4k-projects/badge.svg?branch=develop)](https://coveralls.io/github/bluetape4k/bluetape4k-projects)
[![Maven](https://badges.mvnrepository.com/badge/io.github.bluetape4k/bluetape4k-bom/badge.svg?label=Maven)](https://mvnrepository.com/artifact/io.github.bluetape4k/bluetape4k-bom)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-25-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

JVM 환경에서 Kotlin 언어로 개발할 때 사용하는 공용 라이브러리

[English](./README.md) | 한국어

![bluetape4k Projects 모듈형 백엔드 작업대 일러스트](./docs/assets/projects-workbench.png)

## 소개

Kotlin 언어를 배우고, 사용하면서, Backend 개발에 자주 사용하는 기술, Coroutines 등 기존 라이브러리가 제공하지 않는 기능 들을 개발해 왔습니다.

1. Kotlin 의 장점을 최대화 할 수 있는 추천할 만한 코딩 스타일을 제공할 수 있는 기능을 제공합니다.
    - `bluetape4k-core` 의 assertions, required 같은 기능
    - `bluetape4k-measured` 의 조합 가능한 단위 타입(`Units`)과 측정값(`Measure`) 제공

2. 기존 Java 라이브러리를 무지성으로 사용하지 않고, 좀 더 효과적으로 사용할 수 있도록 개선한 기능을 제공합니다.
    - `bluetape4k-io` 의 LZ4, Zstd, Snappy, Zip 등 압축 기능 개선
    - `bluetape4k-redis`, `bluetape4k-lettuce`, `bluetape4k-redisson` 의 고성능 Lettuce/Redisson Codec 제공

3. 테스트를 좀 더 완성도 있게 하기 위한 기능을 제공합니다.
    - `bluetape4k-junit5` 다양한 테스트 기법을 Junit5 기반으로 제공합니다.
    - `bluetape4k-testcontainers` 다양한 서비스들을 테스트 환경에서 사용할 수 있도록 합니다.

4. Kotlin Coroutines 등 Async/Non-Blocking 방식의 개발을 지원하는 기능을 제공합니다.
    - `bluetape4k-coroutines` Coroutine 을 사용할 때 유용한 기능을 제공합니다.
    - `bluetape4k-feign`, `bluetape4k-retrofit2` 등은 HTTP 통신 시 async/non-blocking을 위해 Coroutines 을 사용하도록 합니다

5. 독립 저장소로 분리된 bluetape4k 생태계 모듈을 함께 관리합니다.
    - AWS, image, text, leader-election, JaVers, Exposed 통합은 독립 bluetape4k 저장소로 분리되었습니다.
    - 이 저장소는 공유 Kotlin/JVM 기반, I/O, data, infra, Spring Boot, testing, utility, example 모듈을 유지합니다.

6. MSA의 필수인 Resilience4j 에 대한 Kotlin Coroutines 지원을 강화했습니다.
    - `bluetape4k-resilience4j` Resilience4j 를 사용할 때 Kotlin Coroutines 를 사용할 수 있도록 지원합니다.
    - 또한 Coroutines 용 Cache를 추가하여, Coroutines 환경에서도 API 호출 결과를 캐싱할 수 있도록 지원합니다.

7. Redis 를 다양한 방식에서 사용할 수 있도록 지원합니다.
    - `bluetape4k-redis`는 Lettuce, Redisson 용 고성능 Codec 을 제공합니다.
    - Redisson의 다양한 Lock 기능을 Coroutines 환경에서도 사용할 수 있도록 지원합니다.
    - Redis를 분산 캐시로만 사용하는 것이 아니라, Near Cache로 사용할 수 있도록 하여 더욱 성능을 높힐 수 있도록 합니다.

그 외 현업에서 마주쳤던 많은 문제를 해결하는 과정에서 필요로 하는 기능 들을 제공합니다.

앞으로도 필요한 기능들이 있다면 Issue 에 제안 주시기 바랍니다.

<!-- README_VISUAL_OVERVIEW:START -->
## Overview Diagram

![Bluetape4k framework overview diagram](docs/images/readme-diagrams/root-readme-overview-01.png)

## Module Composition Chart

![Bluetape4k framework module composition chart](docs/images/readme-charts/root-readme-module-chart-01.png)
<!-- README_VISUAL_OVERVIEW:END -->

## 기술 스택

- **Java**: 25 (기본 JVM toolchain 및 `.java-version`); `virtualthread/jdk21`과 최소 `virtualthread-api`/`logging`/testing dependency closure는 Java 21 호환성을 명시적으로 유지합니다.
- **Kotlin**: 2.4 (기본 Language, API 및 JVM 25 target)
- **Gradle**: 저장소에 포함된 Wrapper 기준 9.7.0
- **Spring Boot**: 4.x 전용. 과거 계획서와 리뷰 문서에는 은퇴한 Spring Boot 3 모듈 언급이 남아 있을 수 있지만, 현재 사용자 대상 모듈과 예제는 Spring Boot 4.x를 기준으로 합니다.
- **JetBrains Exposed**: 1.2.x (외부 `bluetape4k-exposed` artifact는 독립 레포에서 별도 릴리즈됨)
- **데이터베이스**: H2, PostgreSQL, MySQL

<!-- issue-1335-java25-semver:start -->
### JVM 호환성과 2.0.0

`2.0.0`부터 일반 Bluetape4k artifact는 Java 25 runtime이 필요합니다.
Java 21 호환성 섬은 `bluetape4k-assertions`, `bluetape4k-junit5`,
`bluetape4k-logging`, `bluetape4k-virtualthread-api`,
`bluetape4k-virtualthread-jdk21`의 다섯 모듈로 한정되며, 모든 artifact가
Java 21과 호환된다는 뜻이 아닙니다.

Java 21–24에서 일반 artifact를 사용 중이면 `2.0.0`을 위해 Java 25로
이동하거나 `1.13.x`를 유지해야 합니다. Java 21을 유지해야 한다면
호환성 섬 모듈만 선택하고 Java 25 target artifact를 같은 classpath에
섞지 않아야 합니다.
<!-- issue-1335-java25-semver:end -->

## 모듈 구조

Bluetape4k는 기능별로 분리된 멀티 모듈 Gradle 프로젝트입니다.

![projects Architecture diagram](docs/images/readme-diagrams/root-readme-en-diagram-01.png)

### Core 모듈 (`bluetape4k/`)

- **[annotations](./bluetape4k/annotations/README.ko.md)**: API 성숙도 annotation (experimental, beta, internal, delicate, obsolete, implementation-only opt-in 마커)
- **[core](./bluetape4k/core/README.ko.md)**: 핵심 유틸리티 (assertions, required, 컬렉션(BoundedStack, RingBuffer, PaginatedList, Permutation), Wildcard 패턴 매칭, XXHasher 등)
- **[coroutines](./bluetape4k/coroutines/README.ko.md)**: Kotlin Coroutines 확장 (DeferredValue, Flow extensions, AsyncFlow)
- **[logging](./bluetape4k/logging/README.ko.md)**: 로깅 관련 기능
- **[tenant](./bluetape4k/tenant/README.ko.md)**: JDK 25 `ThreadLocal`/`ScopedValue` 기반 no-default tenant context
- **[tenant-reactor](./bluetape4k/tenant-reactor/README.ko.md)**: immutable Reactor subscriber `Context` tenant adapter
- **[bom](./bluetape4k/bom/README.ko.md)**: Bill of Materials (의존성 관리)

### I/O 모듈 (`io/`)

- **[avro](./io/avro/README.ko.md)**: Apache Avro
- **[csv](./io/csv/README.ko.md)**: CSV 처리
- **[fastjson2](./io/fastjson2/README.ko.md)**: FastJSON2
- **[feign](./io/feign/README.ko.md)**: Feign HTTP 클라이언트 (Coroutines 지원)
- **[grpc](./io/grpc/README.ko.md)**: gRPC 서버/클라이언트 추상화 (`bluetape4k-protobuf` 포함)
- **[http](./io/http/README.ko.md)**: HTTP 유틸리티
- **[io](./io/io/README.ko.md)**: 파일 I/O, 압축(LZ4, Zstd, Snappy, Zip), 직렬화(Kryo, Fory), ZIP 빌더/유틸리티
- **[jackson2](./io/jackson2/README.ko.md)/[jackson3](./io/jackson3/README.ko.md)**: Jackson 2.x/3.x 통합 — 바이너리(CBOR, Ion, Smile) 및 텍스트(CSV, YAML, TOML) 포맷 포함 (구 `jackson-binary/text`,
  `jackson3-binary/text` 통합됨)
- **[json](./io/json/README.ko.md)**: JSON 처리
- **[netty](./io/netty/README.ko.md)**: Netty 통합
- **[okio](./io/okio/README.ko.md)**: Okio 기반 I/O 확장 — Buffer/Sink/Source 유틸리티, Base64, Channel, Cipher, Compress, Coroutines, Jasypt/Tink 암호화 Sink/Source
- **[protobuf](./io/protobuf/README.ko.md)**: Protobuf 유틸리티 (Timestamp/Duration/Money 변환, ProtobufSerializer)
- **[retrofit2](./io/retrofit2/README.ko.md)**: Retrofit2 HTTP 클라이언트 (Coroutines 지원)
- **[tink](./io/tink/README.ko.md)**: Google Tink 기반 현대적 암호화 — AEAD, Deterministic AEAD, MAC, Digest, 통합 Encryptor (
  `TinkEncryptor`), Okio `TinkEncryptSink`/`TinkDecryptSource`
- **[vertx](./io/vertx/README.ko.md)**: Vert.x 단일 통합 모듈 — 핵심 기능, SQL 클라이언트, Resilience4j 통합 포함 (구 `vertx/core`, `vertx/sqlclient`, `vertx/resilience4j` 통합됨)
- ~~**crypto**~~: 암호화 기능 (Jasypt 기반 PBE, BouncyCastle) — **Deprecated** (`tink`으로 대체)

### AWS 모듈 → [bluetape4k-aws](https://github.com/bluetape4k/bluetape4k-aws)

> 이 모듈들은 독립 저장소 **[bluetape4k-aws](https://github.com/bluetape4k/bluetape4k-aws)**로 분리되었습니다.

각 서비스마다 **3단계 API** 패턴 제공: `sync` → `async (CompletableFuture)` → `coroutines (suspend)`

- **[aws](https://github.com/bluetape4k/bluetape4k-aws)**: AWS Java SDK v2 — DynamoDB, S3(TransferManager), SES, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS (Coroutines 확장 포함)
- **[aws-kotlin](https://github.com/bluetape4k/bluetape4k-aws)**: AWS Kotlin SDK — native `suspend` 함수 기본 제공; DynamoDB, S3, SES/SESv2, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS DSL 지원

### 데이터 모듈 (`data/`)

#### Exposed 모듈

> **이동됨**: Exposed ORM 관련 모듈(38개)은 독립 레포 **[bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)**로 분리됐습니다.
> 그룹 ID: `io.bluetape4k.exposed`; 독립 레포의 최신 릴리즈 버전을 사용하세요.

#### 기타 데이터 모듈

- **[cassandra](./data/cassandra/README.ko.md)**: Cassandra 드라이버
- **[hibernate](./data/hibernate/README.ko.md)/[hibernate-reactive](./data/hibernate-reactive/README.ko.md)**: Hibernate ORM 통합
- **[jdbc](./data/jdbc/README.ko.md)**: JDBC 유틸리티
- **[mongodb](./data/mongodb/README.ko.md)**: MongoDB Kotlin Coroutine Driver 확장 — `mongoClient {}` DSL, `findFirst`, `exists`, `upsert`, `findAsFlow`, `documentOf {}`, Aggregation Pipeline DSL
- **[r2dbc](./data/r2dbc/README.ko.md)**: R2DBC 지원

### 인프라 모듈 (`infra/`)

- **[redis](./infra/redis/README.ko.md)**: Lettuce/Redisson umbrella 모듈 (하위 호환)
    - **[lettuce](./infra/lettuce/README.ko.md)**: Lettuce 클라이언트, 고성능 Codec (Jdk/Kryo/Fory × GZip/LZ4/Snappy/Zstd),
      `RedisFuture` → Coroutines 어댑터, 분산 Primitive (Lock, Semaphore, AtomicLong, Leader Election),
      `MapLoader`/`MapWriter`/`LettuceLoadedMap` (Read-through/Write-through/Write-behind),
      **BloomFilter/CuckooFilter** (Lua 스크립트 기반, RedisBloom 불필요), **HyperLogLog** (PFADD/PFCOUNT/PFMERGE)
    - **[redisson](./infra/redisson/README.ko.md)**: Redisson 클라이언트, Codec, Memoizer, NearCache (`RLocalCachedMap`), Leader Election (Coroutines 지원)
- **[bucket4j](./infra/bucket4j/README.ko.md)**: Rate limiting
- **[elasticsearch](./infra/elasticsearch/README.ko.md)**: Elasticsearch Java API client DSL 및 Coroutines 지원
- **[kafka](./infra/kafka/README.ko.md)**: Kafka 클라이언트
- **[kafka4](./infra/kafka4/README.ko.md)**: Kafka 4.x / Spring Kafka 4.x 라인
- **[kafka-logback](./infra/kafka-logback/README.ko.md)**: Logback Kafka Appender (구 `x-obsoleted/logback-kafka` 에서 승격)
- **[micrometer](./infra/micrometer/README.ko.md)**: 메트릭
- **[nats](./infra/nats/README.ko.md)**: NATS Java client DSL 및 Coroutines 지원
- **[opentelemetry](./infra/opentelemetry/README.ko.md)**: 분산 추적
- **[pulsar](./infra/pulsar/README.ko.md)**: Apache Pulsar client 확장, Coroutines 및 schema helper 지원
- **[resilience4j](./infra/resilience4j/README.ko.md)**: Resilience4j + Coroutines, Coroutines Cache

#### 캐시 모듈 (`cache/`)

플러그인 방식으로 백엔드를 교체할 수 있는 캐시 추상화 레이어입니다.

- **[cache-core](cache/cache-core/README.ko.md)**: JCache 추상화 + Caffeine/Cache2k/Ehcache 로컬 캐시 (구 `cache-local` 병합) — `AsyncCache`, `SuspendCache`, `NearCache`, `SuspendNearCache`, Memoizer 구현체, testFixtures 6종 추상 테스트
- **[cache-hazelcast](cache/cache-hazelcast/README.ko.md)**: Hazelcast 분산 캐시 + Caffeine 2-Tier Near Cache (구 `cache-hazelcast-near` 병합)
- **[cache-redisson](cache/cache-redisson/README.ko.md)**: Redisson 분산 캐시 + Caffeine 2-Tier Near Cache (구 `cache-redisson-near` 병합)
- **[cache-lettuce](cache/cache-lettuce/README.ko.md)**: Lettuce(Redis) 기반 분산 캐시 — `LettuceNearCacheConfig`, RESP3 CLIENT TRACKING 기반 자동 invalidation
- **[hibernate-cache-lettuce](cache/hibernate-cache-lettuce/README.ko.md)**: Hibernate 2nd Level Cache + Lettuce NearCache (Caffeine L1 + Redis L2) — `LettuceNearCacheRegionFactory`, `LettuceNearCacheStorageAccess`, region별 TTL 오버라이드, 15가지 코덱 지원

### Spring Boot 모듈 (`spring-boot/`)

이 저장소는 Spring Boot 4.x만 지원합니다. versionless `spring-boot/*` 모듈은
현재 Spring Boot 4 아티팩트로 게시합니다.

> **BOM 적용 주의**: `dependencyManagement { imports }` 대신 `implementation(platform(...))` 방식으로 적용해야 KGP 2.3.x와 충돌 없이 빌드됩니다.

- **[core](./spring-boot/core/README.ko.md)**: Spring Boot 기반 공통 기능 — WebFlux + Coroutines, RestClient DSL (
  `suspendGet`, `suspendPost` 등), Jackson 2 커스터마이저, Retrofit2 통합, WebTestClient 테스트 유틸리티
- **[cassandra](./spring-boot/cassandra/README.ko.md)**: Spring Data Cassandra 코루틴 확장
- **[cassandra-demo](./spring-boot/cassandra-demo/README.ko.md)**: Cassandra 사용 예제
- **[data-redis](./spring-boot/redis/README.ko.md)**: Spring Data Redis 고성능 직렬화 — `RedisBinarySerializer`, `RedisCompressSerializer`, `redisSerializationContext {}` DSL
- **[hibernate-lettuce](./spring-boot/hibernate-lettuce/README.ko.md)**: Hibernate 2nd Level Cache + Lettuce NearCache Spring Boot Auto-Configuration
- **[hibernate-lettuce-demo](./spring-boot/hibernate-lettuce-demo/README.ko.md)**: Hibernate Lettuce NearCache + Spring MVC 통합 데모
- **[idgenerator-spring-boot-demo](./examples/spring-boot/idgenerator-spring-boot-demo/README.ko.md)**: `bluetape4k-idgenerators` Spring Boot REST 예제
- **[observability-spring-boot-demo](./examples/spring-boot/observability-spring-boot-demo/README.ko.md)**: Spring Boot 4 Actuator Prometheus와 OTLP observability 예제
- **[mongodb](./spring-boot/mongodb/README.ko.md)**: Spring Data MongoDB Reactive 코루틴 확장, Criteria/Query/Update infix DSL
- **[r2dbc](./spring-boot/r2dbc/README.ko.md)**: Spring Data R2DBC 코루틴 확장

### Ktor 모듈 (`ktor/`)

서버 측 Ktor 3.x 기반 공통 모듈입니다. Plugin 설치는 명시적으로 유지하고, auth helper는 서버 확장 지점이 검증될 때까지 backlog로 둡니다.

- **[core](./ktor/core/README.ko.md)**: JSON, 오류 응답, health/readiness route를 위한 기본 Ktor 서버 helper
- **[observability](./ktor/observability/README.ko.md)**: Call logging, correlation-id, metrics, Prometheus route helper
- **[openapi](./ktor/openapi/README.ko.md)**: 명시적인 Ktor OpenAPI 및 Swagger UI 문서 route helper
- **[resilience4j](./ktor/resilience4j/README.ko.md)**: Route 범위 Resilience4j retry, circuit breaker, rate limiter, timeout helper
- **[tenant](./ktor/tenant/README.ko.md)**: `ApplicationCall` one-call/one-tenant context adapter
- **[testing](./ktor/testing/README.ko.md)**: Ktor `testApplication` 및 JSON client 테스트 helper

### 텍스트 처리 → [bluetape4k-text](https://github.com/bluetape4k/bluetape4k-text)

> 이 모듈들은 독립 저장소 **[bluetape4k-text](https://github.com/bluetape4k/bluetape4k-text)**로 분리되었습니다.

- **[tokenizer-core](https://github.com/bluetape4k/bluetape4k-text)**: 토크나이저 공통 인터페이스 — `TokenizeRequest/Response`, `BlockwordRequest/Response`, `DictionaryProvider`
- **[tokenizer-korean](https://github.com/bluetape4k/bluetape4k-text)**: 한국어 형태소 분석기 (Open Korean Text 기반)
- **[tokenizer-japanese](https://github.com/bluetape4k/bluetape4k-text)**: 일본어 형태소 분석기 (Kuromoji IPAdic 0.9.0)
- **[lingua](https://github.com/bluetape4k/bluetape4k-text)**: 언어 감지 — Lingua 기반 Kotlin DSL 래퍼 (75+ 언어)
- **[text-search](https://github.com/bluetape4k/bluetape4k-text)**: Aho-Corasick 다중 키워드 검색 — 금칙어 필터, 하이라이팅, Flow API

### 이미지 처리 → [bluetape4k-image](https://github.com/bluetape4k/bluetape4k-image)

> 이 모듈들은 독립 저장소 **[bluetape4k-image](https://github.com/bluetape4k/bluetape4k-image)**로 분리되었습니다.

- **[images](https://github.com/bluetape4k/bluetape4k-image)**: 이미지 처리 유틸리티 (scrimage — 리사이즈, 크롭, 썸네일, 포맷 변환)
- **[images-vips-api](https://github.com/bluetape4k/bluetape4k-image)**: libvips 바인딩 중립 API — `VipsImage`, `VipsRuntime`, `VipsEncodeOptions`
- **[images-vips-java21](https://github.com/bluetape4k/bluetape4k-image)**: Java 21 JVips/JNI 바인딩 — `JVipsRuntime`, `JVipsImage`, `NativeHandle` Cleaner leak guard
- **[images-vips-java25](https://github.com/bluetape4k/bluetape4k-image)**: Java 25 Panama FFM 바인딩 — `FfmVipsRuntime`, `FfmVipsImage`, `Arena` 라이프사이클 관리

### 유틸리티 모듈 (`utils/`)

> **이동됨**: 리더 선출(leader election) 모듈은 독립 레포 **[bluetape4k-leader](https://github.com/bluetape4k/bluetape4k-leader)** 로 분리됐습니다. blocking / async / coroutine / virtual-thread API와 Redis 백엔드를 제공합니다.

- **[geo](./utils/geo/README.ko.md)**: 지리 정보 처리 단일 통합 모듈 — geocode(Bing/Google), geohash, geoip2(MaxMind) 포함 (구
  `utils/geocode`, `utils/geohash`, `utils/geoip2` 통합됨)
- **[idgenerators](./utils/idgenerators/README.ko.md)**: ID 생성기 — `Uuid`(V1~V7 통일 API), `ULID`, `Ksuid`(Seconds/Millis), `Snowflakers` 통일 팩토리, `Flake`, `Hashids` 등 다양한 ID 생성 알고리즘 제공
- **[javatimes](./utils/javatimes/README.ko.md)**: 날짜/시간 유틸리티
- **[jwt](./utils/jwt/README.ko.md)**: JWT 처리
- **[math](./utils/math/README.ko.md)**: 수학 유틸리티
- **[measured](./utils/measured/README.ko.md)**: 조합 가능한 단위 타입(`Units`)과 측정값(`Measure`) 기반으로, 복합 단위(`m/s`, `kg*m/s^2`)를 타입 안전하게 표현
- **[money](./utils/money/README.ko.md)**: Money API
- **[mutiny](./utils/mutiny/README.ko.md)**: Mutiny reactive 통합
- **[probabilistic](./utils/probabilistic/README.ko.md)**: Bloom filter 등 의존성 없는 확률적 자료구조
- **[rule-engine](./utils/rule-engine/README.ko.md)**: 경량 Kotlin Rule Engine — DSL 규칙, 어노테이션 기반 규칙, 스크립트 엔진, 코루틴 실행 지원
- **[science](./utils/science/README.ko.md)**: GIS 공간 데이터 처리 — 좌표계 변환(BoundingBox/UTM/DMS, Proj4J), Shapefile 읽기(GeoTools 31.6 LGPL), JTS 기반 공간 기하학 연산, PostGIS DB 적재 파이프라인(SpatialLayerTable/SpatialFeatureTable/PoiTable)
- **[states](./utils/states/README.ko.md)**: Kotlin DSL 기반 유한 상태 머신 라이브러리 — 동기/코루틴 FSM, Guard 조건, `StateFlow` 상태 관찰 지원
- **[workflow](./utils/workflow/README.ko.md)**: Kotlin DSL 워크플로우 오케스트레이션 — Sequential/Parallel/Conditional/Repeat/Retry 플로우, 동기(Virtual Threads) + 코루틴(suspend/Flow), ABORTED/CANCELLED/PartialSuccess 지원
- ~~**units**~~: 단위 표현 value class — **Deprecated** (`measured`로 통합)

### 테스트 모듈 (`testing/`)

- **[assertions](./testing/assertions/README.ko.md)**: 테스트용 bluetape4k assertion DSL 기반
- **[junit5](./testing/junit5/README.ko.md)**: JUnit 5 확장 및 유틸리티
- **[testcontainers](./testing/testcontainers/README.ko.md)**: Testcontainers 지원 (Redis, Kafka, DB 등)
- **[testcontainers-spring](./testing/testcontainers-spring/README.ko.md)**: Testcontainers 프로퍼티를 Spring `DynamicPropertyRegistry`에 연결하는 선택 모듈
- **[mock-web-server](./testing/mock-web-server/README.ko.md)**: 통합 테스트용 MVC Mock HTTP Server Docker 이미지
- **[mock-webflux-server](./testing/mock-webflux-server/README.ko.md)**: 통합 테스트용 WebFlux Mock HTTP Server Docker 이미지

Jib으로 Mock Server Docker 이미지를 다시 빌드할 때는 Gradle configuration cache를 항상 비활성화해야 합니다:

```bash
./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache
./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache
```

### Virtual Thread 모듈 (`virtualthread/`)

- **[virtualthread](./virtualthread/README.ko.md)**: Java 21/25 Virtual Thread 지원
    - **[api](./virtualthread/api/README.ko.md)**: Virtual Thread API 및 ServiceLoader 기반 런타임 선택
    - **[jdk21](./virtualthread/jdk21/README.ko.md)**: Java 21 Virtual Thread 구현체
    - **[jdk25](./virtualthread/jdk25/README.ko.md)**: Java 25 Virtual Thread 구현체


### 예제 모듈 (`examples/`)

라이브러리 사용 방법을 보여주는 예제 모듈입니다. 배포되지 않습니다.

- **[coroutines-demo](./examples/coroutines-demo/README.ko.md)**: Kotlin Coroutines 사용 예제
- **[jpa-blazepersistence-demo](./examples/jpa-blazepersistence-demo/README.ko.md)**: JPA + Blaze Persistence 사용 예제
- **[jpa-querydsl-demo](./examples/jpa-querydsl-demo/README.ko.md)**: JPA + QueryDSL 사용 예제
- **[idgenerator-ktor-demo](./examples/ktor/idgenerator-ktor-demo/README.ko.md)**: `bluetape4k-idgenerators` Ktor HTTP 예제
- **[observability-ktor-demo](./examples/ktor/observability-ktor-demo/README.ko.md)**: Ktor Prometheus `/metrics`와 opt-in OpenTelemetry tracing 예제
- **[observability-spring-boot-demo](./examples/spring-boot/observability-spring-boot-demo/README.ko.md)**: Spring Boot 4 Actuator Prometheus와 OTLP observability 예제
- **[redisson-demo](./examples/redisson-demo/README.ko.md)**: Redisson 사용 예제
- **[virtualthreads-demo](./examples/virtualthreads-demo/README.ko.md)**: Java Virtual Thread 사용 예제

### 제거 / 이전된 모듈

레거시 `x-obsoleted/` 디렉토리는 제거되었습니다. 이전 항목들의 최종 처분 내역을 표로 보존하여 과거 참조가 추적 가능하도록 유지합니다.

| 모듈                                | 상태                                                                                  |
|-------------------------------------|---------------------------------------------------------------------------------------|
| `logback-kafka`                     | **승격** → [`infra/kafka-logback`](./infra/kafka-logback/README.ko.md)                |
| `tokenizer`                         | **이전** → `bluetape4k-text` (tokenizer-core / tokenizer-korean / tokenizer-japanese) |
| `ahocorasick`                       | **이전** → `bluetape4k-text/text-search`                                              |
| `lingua`                            | **이전** → `bluetape4k-text/lingua`                                                   |
| `javers`                            | **분리** → 독립 레포 [bluetape4k-javers](https://github.com/bluetape4k/bluetape4k-javers) |
| `bloomfilter`                       | **대체** → `infra/lettuce` BloomFilter / CuckooFilter (Lua 기반)                      |
| `vertx-coroutines` / `vertx-sqlclient` / `vertx-webclient` | **통합** → `bluetape4k-vertx`                                  |
| `mapstruct`, `captcha`, `naivebayes`, `mutiny-examples` | **삭제** (사용 빈도 낮음)                                  |

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

이 명령은 분석 대상 library subproject의 Kotlin 소스를 검사하며, 포함된
프로젝트의 소스가 비어 있으면 실패합니다. 분석 모듈/파일 영수증은
`build/reports/detekt/source-coverage.md`에, 병합된 Checkstyle 결과는
`build/reports/detekt/merged.xml`에 생성됩니다. examples·demo·benchmark·workshop
소스, 메타데이터 전용 프로젝트와 문서화된 `exposed-jdbc-tests` 예외는 영수증에
명시적인 제외 항목으로 기록됩니다.

## 배포 방법

버전 확인은 `gradle.properties` 파일에서 확인

```properties
projectGroup=io.github.bluetape4k
baseVersion=2.0.0
snapshotVersion=
```

### Maven Central SNAPSHOT 배포

```bash
# 기본 병렬도(centralSnapshotsParallelism=8)로 SNAPSHOT 배포
./gradlew nmcpPublishAggregationToCentralPortalSnapshots -PsnapshotVersion=-SNAPSHOT --no-configuration-cache

# 병렬도를 낮춰 서버 부담을 줄이고 싶을 때
./gradlew -PcentralSnapshotsParallelism=4 nmcpPublishAggregationToCentralPortalSnapshots -PsnapshotVersion=-SNAPSHOT --no-configuration-cache
```

- 루트 집계 task는 `nmcpPublishAggregationToCentralPortalSnapshots` 입니다.
- SNAPSHOT 배포는 release 와 달리 ZIP 1회 업로드가 아니라 file-by-file 업로드를 수행합니다.
- 따라서 모듈 수가 많을수록 `PUT` 요청이 많이 발생하는 것이 정상입니다.
- 업로드 대상은 `workshop/**`, `examples/**`, `-demo` 모듈을 제외한 publishable modules 입니다.
- Snapshot 저장소는 `https://central.sonatype.com/repository/maven-snapshots/` 입니다.
- 병렬도는 `centralSnapshotsParallelism` property 로 조절할 수 있습니다. 기본값은 `8` 입니다.

### Maven Central RELEASE 배포

```bash
# snapshotVersion이 비어 있는 커밋에서 RELEASE 배포
./gradlew nmcpPublishAggregationToCentralPortal --no-daemon --no-configuration-cache
```

- 루트 집계 task는 `nmcpPublishAggregationToCentralPortal` 입니다.
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
signingKeyId=YOUR_LAST_8_HEX_DIGITS
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingPassword=YOUR_KEY_PASSPHRASE

# Maven Central Snapshots 업로드 병렬도 (기본값: 8)
centralSnapshotsParallelism=8
```

- `signingKeyId`에는 signing subkey ID의 **뒤 8자리 hex 값**만 넣어야 합니다. 예: `5C6DF399`
- `7CF28E155C6DF399` 같은 16자리 long key ID를 넣으면 빌드가 `5C6DF399`로 정규화하고 경고를 출력합니다.
- GitHub Actions secret `SIGNING_KEY_ID`에는 `signingKeyId=...`가 아니라 값만 넣어야 합니다.

### 참고

- `publishAggregationToCentralSnapshots`, `publishAggregationToCentralPortal`, `publishAggregationToCentralPortalSnapshots` 같은 하위 호환 task가 `./gradlew tasks`에 남아 있을 수 있지만, 위의 `nmcpPublishAggregation*` 루트 task 사용을 권장합니다.
- `nmcpPublishAllPublicationsToCentralPortalSnapshots`, `publishAllPublicationsToCentralPortalSnapshots`, `publishAllPublicationsToCentralSnapshots` 같은 개별 task 직접 실행 대신 루트 집계 task 사용을 권장합니다.
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
./bin/repo-test-summary -- ./gradlew :bluetape4k-coroutines:test
```

기본 흐름은 "요약 먼저, 필요한 파일이나 태스크만 원본 출력 확인"입니다.
