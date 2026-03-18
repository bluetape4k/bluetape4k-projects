# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Bluetape4k은 Kotlin 언어로 JVM 환경에서 Backend 개발 시 사용하는 공용 라이브러리 모음입니다. Kotlin의 장점을 최대화하고, 기존 Java 라이브러리를 개선하며, Kotlin Coroutines 기반의 async/non-blocking 개발을 지원합니다.

## Claude 작업 지침

- Rate Limit 이 다가오면 Memory.md 에 현재까지의 Context 를 저장해서 토큰을 절약할 것
- 작업 시 Python 코드 제작은 자제하고, Read/Edit 방식으로 작업할 것
- Think Before Coding — 모르면 추측하지 말고 물어봐
- Simplicity First — 요청한 것만 만들어. 200줄이 50줄로 되면 다시 써
- Surgical Changes — 옆 코드 "개선"하지 마. 변경된 모든 줄이 요청으로 추적 가능해야 함
- Goal-Driven Execution — "버그 고쳐" 대신 "버그 재현 테스트 쓰고 통과시켜"

### CLI 도구 사용 규칙 (Rust 기반 현대 도구 우선)

- **파일 탐색**: `find` 대신 `fd` 사용 (예: `fd -e kt -t f`)
- **텍스트 검색**: `grep` 대신 `rg` (ripgrep) 사용 (예: `rg "패턴" --type kotlin`)
- **파일 내용 확인**: `cat` 대신 `bat` 사용 (예: `bat src/Foo.kt`)
- **디렉토리 목록**: `ls` 대신 `eza` 사용 (예: `eza -la --git`)
- **코드 구조 검색/리팩토링**: `ast-grep` 적극 활용 (예: `ast-grep -p 'fun $NAME($$$)' -l kotlin`)
- **JSON 파싱**: `jq` 사용 (예: `curl ... | jq '.data[]'`)
- **YAML 파싱**: `yq` 사용 (예: `yq '.dependencies' build.gradle.yaml`)
- **GitHub 작업**: `gh` CLI를 비대화형 모드로 사용 (예: `gh pr list --json number,title`,
  `gh issue create --title "..." --body "..."`)
- **Python 린팅/포매팅**: `ruff` 사용 (예: `ruff check .`, `ruff format .`)
- **모든 외부 CLI 명령**: 비대화형 플래그(`--yes`, `--quiet`, `--no-input`) 및 JSON 출력(`--format json`, `--json`) 강제 적용

## Development Guidelines

### Language and Documentation

- 주석과 설명은 KDoc 형식으로 **한국어**로 작성
- 커밋 메시지는 **한국어**로 작성하며, 머릿말(feat, fix, docs, style, refactor, perf, test, chore) 사용
- Kotlin 2.3 이상 사용
- 최대한 Kotlin extensions와 DSL 활용

### Technology Stack

- Java 21 (JVM Toolchain)
- Kotlin 2.3 (languageVersion & apiVersion)
- Spring Boot 3.4.0+
- Kotlin Exposed 1.0.0+
- 데이터베이스: H2, PostgreSQL, MySQL 주로 사용

### Testing Standards

- JUnit 5 기반 테스트
- MockK를 Mock 라이브러리로 사용
- Kluent를 Assertions 라이브러리로 사용
- 예제는 간결하되 프로덕션 수준으로 작성하며, 실제 동작 가능해야 함

### Documentation

- Public Class, Interface, Extensions methods 에 대해서 필수적으로 KDoc 을 작성한다
- KDoc 은 항상 한국어로 작성한다

## Build Commands

### Git

```bash
# 저장소 상태 요약
./bin/repo-status

# diff 요약
./bin/repo-diff

# 테스트/Gradle 출력 요약
./bin/repo-test-summary -- ./gradlew :bluetape4k-coroutines:test
```

### Basic Build

```bash
# Clean and build all modules
./gradlew clean build

# Build specific module
./gradlew :bluetape4k-coroutines:build

# Build without tests
./gradlew build -x test
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :bluetape4k-io:test

# Run specific test class
./gradlew test --tests "io.bluetape4k.io.CompressorTest"

# Run tests with detailed logging
./gradlew test --info
```

### Code Quality

```bash
# Run Detekt (static analysis)
./gradlew detekt

# Format code (if formatter is configured)
./gradlew formatKotlin
```

### Publishing

```bash
# Publish SNAPSHOT to Maven repository
./gradlew publishBluetape4kPublicationToBluetape4kRepository

# Publish RELEASE (remove snapshot version)
./gradlew publishBluetape4kPublicationToBluetape4kRepository -PsnapshotVersion=
```

## Token-Efficient Workflow

- 세션 컨텍스트 절약을 위해 `git status` 대신 `./bin/repo-status`를 우선 사용합니다.
- 전체 patch가 필요하기 전에는 `git diff` 대신 `./bin/repo-diff`로 파일별 변경량만 먼저 확인합니다.
- Gradle 테스트/빌드 로그는 `./bin/repo-test-summary -- ./gradlew ...` 형태로 요약해서 확인합니다.
- 기본 흐름은 "요약 먼저, 원본 출력은 필요한 파일이나 태스크에 한해 2차로 확인"입니다.

## Architecture

### Module Structure

프로젝트는 멀티 모듈 Gradle 프로젝트로, 기능별로 모듈이 분리되어 있습니다:

#### Core Modules (`bluetape4k/`)

- **core**: 핵심 유틸리티 (assertions, required, 압축 등)
- **coroutines**: Coroutines 관련 유틸리티 (DeferredValue, Flow extensions, AsyncFlow 등)
- **logging**: 로깅 관련 기능
- **bom**: Bill of Materials (dependency management)

#### I/O Modules (`io/`)

- **io**: 파일 I/O, 압축(LZ4, Zstd, Snappy), 직렬화(Kryo, Fory)
- **okio**: Okio 기반 I/O 확장 — Buffer/Sink/Source 유틸리티, Base64, Channel, Cipher, Compress, Coroutines(Suspended I/O), Jasypt/Tink 암호화 Sink/Source
- **json**: JSON 처리
- **jackson**: Jackson 2.x 통합
- **jackson3**: Jackson 3.x 통합
- ~~**jackson-binary/jackson-text**~~: **`bluetape4k-jackson2`에 통합됨** (CBOR, Ion, Smile, CSV, YAML, TOML 등 모든 포맷 포함)
- ~~**jackson3-binary/jackson3-text**~~: **`bluetape4k-jackson3`에 통합됨**
- **fastjson2**: FastJSON2
- **csv**: CSV 처리
- **avro**: Apache Avro
- **feign**: Feign HTTP 클라이언트 (Coroutines 지원)
- **retrofit2**: Retrofit2 (Coroutines 지원)
- **http**: HTTP 유틸리티
- **netty**: Netty 통합
- **protobuf**: Protobuf 유틸리티 — 타입 별칭(`ProtoMessage`, `ProtoAny` 등), Timestamp/Duration/DateTime/Money 변환, `ProtobufSerializer`
- **grpc**: gRPC 서버/클라이언트 추상화 (`bluetape4k-protobuf` 포함)
- ~~**crypto**~~: 암호화 기능 (Jasypt 기반 PBE, BouncyCastle) — **Deprecated** (`bluetape4k-tink`로 대체)
- **tink**: Google Tink 기반 현대적 암호화 — AEAD (AES-GCM, ChaCha20-Poly1305), Deterministic AEAD (AES-SIV), MAC (HMAC), Digest (SHA-256 등), 통합 Encryptor (`TinkEncryptor`); Okio 암호화 Sink/Source (`TinkEncryptSink`/`TinkDecryptSource`)는 `io/io` 모듈에 위치

#### AWS Modules (`aws/`, `aws-kotlin/`)

각 서비스마다 **3단계 API** 패턴을 제공합니다: `sync` → `async (CompletableFuture)` → `coroutines (suspend)`

##### Java SDK v2 기반 (`bluetape4k-aws`)

단일 통합 모듈. AWS 핵심 기능은 `api()`로 제공하고, 각 서비스 SDK는 `compileOnly()`로 선언하여 사용자가 필요한 서비스만 런타임에 추가합니다.

- DynamoDB, S3(TransferManager), SES, SNS, SQS, KMS, CloudWatch/CloudWatchLogs, Kinesis, STS
- 각 서비스별 coroutines 확장: `XxxAsyncClientCoroutinesExtensions.kt` (`.await()` 래핑)

##### AWS Kotlin SDK 기반 (`bluetape4k-aws-kotlin`)

네이티브 `suspend` 함수를 기본 제공하는 단일 통합 모듈. `.await()` 변환 없이 코루틴에서 직접 사용 가능합니다.

- DynamoDB, S3, SES/SESv2, SNS, SQS, KMS, CloudWatch/CloudWatchLogs, Kinesis, STS
- DSL 지원: `metricDatum {}`, `inputLogEvent {}`, `putRecordRequestOf {}`, `stsClientOf {}` 등

##### AWS 모듈 패턴

| SDK | 모듈 | Coroutines |
|-----|------|------------|
| Java SDK v2 | `bluetape4k-aws` | `XxxAsyncClientCoroutinesExtensions.kt` (`.await()` 래핑) |
| Kotlin SDK | `bluetape4k-aws-kotlin` | 기본 제공 (별도 래핑 불필요) |

#### Data Modules (`data/`)

Exposed 모듈은 기능별로 분리되어 있습니다 (하위 호환 umbrella 포함):

- **exposed** *(umbrella)*: `exposed-core` + `exposed-dao` + `exposed-jdbc`를 묶는 하위 호환 모듈. 기존 코드는 변경 없이 사용 가능
- **exposed-core**: JDBC 불필요한 핵심 기능 — 압축/암호화/직렬화 컬럼 타입, 클라이언트 ID 생성 확장(`timebasedGenerated`, `snowflakeGenerated`, `ksuidGenerated`), `HasIdentifier`, `ExposedPage`
- **exposed-dao**: DAO 엔티티 확장 — `EntityExtensions`, `StringEntity`, 커스텀 IdTable(`KsuidTable`, `SnowflakeIdTable`, `TimebasedUUIDTable`, `SoftDeletedIdTable` 등)
- **exposed-jdbc**: JDBC 전용 — `ExposedRepository`, `SoftDeletedRepository`, `SuspendedQuery`, `VirtualThreadTransaction`, `TableExtensions`
- **exposed-r2dbc**: Exposed + R2DBC (reactive, `ExposedR2dbcRepository`)
- **exposed-jdbc-lettuce**: Exposed JDBC + Lettuce Redis 캐시 (Read-through / Write-through / Write-behind) —
  `AbstractJdbcLettuceRepository`, `ExposedEntityMapLoader`, `ExposedEntityMapWriter`;
  `AbstractSuspendedJdbcLettuceRepository`, `SuspendedExposedEntityMapLoader/Writer` (코루틴 네이티브 버전,
  `LettuceSuspendedLoadedMap` 사용)
- **exposed-r2dbc-lettuce**: Exposed R2DBC + Lettuce Redis 캐시 (코루틴 네이티브 Read-through / Write-through / Write-behind) —
  `AbstractR2dbcLettuceRepository`, `R2dbcExposedEntityMapLoader`, `R2dbcExposedEntityMapWriter`,
  `LettuceSuspendedLoadedMap`;
  `runBlocking` 없이 `suspendTransaction` 기반으로 동작
- **exposed-jdbc-redisson**: Exposed JDBC + Redisson (Read-through / Write-through / Write-behind 캐시) —
  `JdbcRedissonRepository<ID: Any, E: Any>`, `AbstractJdbcRedissonRepository`,
  `SuspendedJdbcRedissonRepository`, `AbstractSuspendedJdbcRedissonRepository`;
  `extractId(entity): ID` 패턴으로 엔티티 ID 추출 (Lettuce Repository와 동일 패턴)
- **exposed-r2dbc-redisson**: Exposed R2DBC + Redisson (코루틴 네이티브 Read-through / Write-through / Write-behind 캐시) —
  `R2dbcRedissonRepository<ID: Any, E: Any>`, `AbstractR2dbcRedissonRepository`;
  `extractId(entity): ID` 패턴, `suspendTransaction` 기반
- **exposed-jackson/jackson3**: Exposed JSON 컬럼 지원 (Jackson 2.x / 3.x)
- **exposed-fastjson2**: Exposed JSON 컬럼 지원 (Fastjson2)
- **exposed-jasypt**: Exposed 암호화 컬럼 (Jasypt)
- **exposed-tink**: Exposed 암호화 컬럼 (Google Tink AEAD/Deterministic AEAD)
- **exposed-measured**: Exposed 쿼리 실행 시간 측정 (Micrometer 통합)
- **exposed-jdbc-tests**: JDBC 기반 테스트 공통 인프라
- **exposed-r2dbc-tests**: R2DBC 기반 테스트 공통 인프라
- **hibernate**: Hibernate 통합
- **hibernate-reactive**: Hibernate Reactive
- **jdbc**: JDBC 유틸리티
- **mongodb**: MongoDB Kotlin Coroutine Driver 확장 — `mongoClient {}` DSL, `MongoClientProvider`, `findFirst`, `exists`, `upsert`, `findAsFlow`, `documentOf {}`, Aggregation Pipeline DSL (`pipeline {}`)
- **r2dbc**: R2DBC 지원
- **cassandra**: Cassandra 드라이버

##### Exposed 모듈 의존성 선택 가이드

| 사용 목적 | 권장 모듈 |
|-----------|-----------|
| Jackson/암호화/압축 컬럼 타입, R2DBC와 함께 사용 | `bluetape4k-exposed-core` |
| DAO Entity, 커스텀 IdTable | `bluetape4k-exposed-dao` |
| JDBC Repository, 쿼리, 트랜잭션 | `bluetape4k-exposed-jdbc` |
| 쿼리 실행 시간 측정 (Micrometer) | `bluetape4k-exposed-measured` |
| 기존 코드 그대로 유지 | `bluetape4k-exposed` (umbrella) |

#### Infrastructure Modules (`infra/`)

- **redis** *(umbrella)*: `lettuce` + `redisson` + `spring-data-redis`를 묶는 하위 호환 모듈
- **lettuce
  **: Lettuce Redis 클라이언트, 고성능 Codec, Future→Coroutine 어댑터, 분산 Primitive (Lock, Semaphore, AtomicLong, Map, Leader Election, Memorizer) — sync/async/suspend 3-tier API;
  `MapLoader`/`MapWriter`/`LettuceLoadedMap` (동기 Read-through/Write-through/Write-behind 추상화);
  `SuspendedMapLoader`/`SuspendedMapWriter`/`LettuceSuspendedLoadedMap` (코루틴 네이티브 suspend 버전, `runBlocking` 없음)
- **redisson**: Redisson Redis 클라이언트, Codec, Cache, Leader Election, Memorizer, NearCache, Coroutines
- **kafka**: Kafka 클라이언트
- **resilience4j**: Resilience4j + Coroutines, Coroutines Cache
- **bucket4j**: Rate limiting
- **micrometer**: 메트릭
- **opentelemetry**: 분산 추적
- **nats**: NATS 메시징

##### 캐시 모듈 (`infra/cache-*`)

플러그인 방식으로 백엔드를 교체할 수 있는 캐시 추상화 레이어입니다.

- **cache**: 캐시 추상화 umbrella 모듈 (cache-core + hazelcast + redisson + lettuce)
- **cache-core**: JCache 추상화 + Caffeine/Cache2k/Ehcache 로컬 캐시 — `NearCacheOperations<V>`, `SuspendNearCacheOperations<V>` 공통 인터페이스, `ResilientNearCacheDecorator` (retry + failure strategy), `JCacheNearCache<V>`, `NearCacheStatistics`, Memorizer 구현체
- **cache-hazelcast**: Hazelcast 분산 캐시 + `HazelcastNearCache<V>: NearCacheOperations<V>`, `HazelcastSuspendNearCache<V>: SuspendNearCacheOperations<V>`
- **cache-redisson**: Redisson 분산 캐시 + `RedissonNearCache<V>: NearCacheOperations<V>` (RLocalCachedMap 기반), `RedissonSuspendNearCache<V>: SuspendNearCacheOperations<V>`
- **cache-lettuce**: Lettuce(Redis) 기반 분산 캐시 + `LettuceNearCache<V>: NearCacheOperations<V>` (RESP3 CLIENT TRACKING), `LettuceSuspendNearCache<V>: SuspendNearCacheOperations<V>`

#### Spring Modules (`spring/`)

- **bluetape4k-spring-boot3** *(통합 모듈)*: Spring Boot 3 기반 공통 기능 통합
  - Spring core 유틸리티 (BeanFactory 확장, ToStringCreator 지원 등)
  - Spring WebFlux + Coroutines 지원 (WebTestClient 확장 포함)
  - Spring + Retrofit2 통합 (OkHttp3, Apache HttpClient5 포함)
  - Spring 테스트 유틸리티 (WebTestClient, Testcontainers 통합)
  - ~~spring/core, spring/webflux, spring/retrofit2, spring/tests 통합됨~~
- **spring/jpa** → `data/hibernate`로 이동: JPA 관련 Spring 통합은 `bluetape4k-hibernate` 모듈에 위치
- **cassandra**: Spring Data Cassandra
- **mongodb**: Spring Data MongoDB Reactive — `ReactiveMongoOperations` 코루틴 확장 (`findAsFlow`, `insertSuspending` 등), Criteria/Query/Update infix DSL
- **data-redis**: Spring Data Redis 직렬화 (BinarySerializer, CompressSerializer, SerializationContext DSL)
- **r2dbc**: Spring Data R2DBC

#### Vert.x Modules (`vertx/`)

- **bluetape4k-vertx** *(통합 모듈)*: Vert.x 핵심 기능 + SQL 클라이언트 + Resilience4j 통합
  - ~~vertx/core, vertx/sqlclient, vertx/resilience4j 통합됨~~

#### Utilities (`utils/`)

- **ahocorasick**: 문자열 검색 (Aho-Corasick 알고리즘)
- **bloomfilter**: Bloom Filter
- **captcha**: CAPTCHA 생성
- **bluetape4k-geo** *(통합 모듈)*: 지리 정보 처리 — geocode(Bing/Google), geohash, geoip2(MaxMind) 통합
  - ~~utils/geocode, utils/geohash, utils/geoip2 통합됨~~
- **idgenerators**: ID 생성기 (Ksuid, Snowflake, ULID, UUID 등)
- **images**: 이미지 처리
- **javatimes**: 날짜/시간 유틸리티
- **jwt**: JWT 처리
- **leader**: Leader 선출
- **lingua**: 언어 감지
- **logback-kafka**: Logback Kafka Appender
- **math**: 수학 유틸리티
- **measured**: 조합 가능한 단위 타입(`Units`)과 측정값(`Measure`) 기반으로, 복합 단위(`m/s`, `kg*m/s^2`)를 타입 안전하게 표현
- **money**: Money API
- **mutiny**: Mutiny reactive 라이브러리 통합
- **naivebayes**: Naive Bayes 분류기
- ~~**units**~~: 단위 표현 value class — **Deprecated** (`bluetape4k-measured`의 기능으로 통합)

#### Testing Modules (`testing/`)

- **junit5**: JUnit 5 확장 및 유틸리티
- **testcontainers**: Testcontainers 지원

#### Virtual Thread Modules (`virtualthread/`)

- **api**: Virtual Thread API 및 ServiceLoader 기반 런타임 선택
- **jdk21**: Java 21 Virtual Thread 구현체
- **jdk25**: Java 25 Virtual Thread 구현체

#### Other Modules

- **javers/**: JaVers 감사 로그
- **tokenizer/**: 한국어/일본어 토크나이저
- **timefold/**: Timefold Solver
- **examples/**: 라이브러리 사용 예제

### Build Configuration

#### Gradle Settings

- **JVM Toolchain**: Java 21
- **Kotlin Version**: 2.3 (API & Language)
- **Gradle Daemon**: ZGC, 4-8GB heap
- **Parallel Build**: enabled (단 test 시에는 비활성화)
- **Build Cache**: enabled

#### Key Gradle Files

- `build.gradle.kts`: 루트 빌드 설정, 공통 dependencies, 플러그인 구성
- `settings.gradle.kts`: 모듈 include 로직 (`includeModules` 함수)
- `gradle.properties`: 버전 정보, JVM 옵션
- `buildSrc/src/main/kotlin/Libs.kt`: 의존성 버전 중앙 관리

#### Dependency Management

- Spring Boot, Spring Cloud BOM 사용
- AWS SDK, Jackson, Micrometer, OpenTelemetry, Testcontainers 등 주요 BOM 적용
- `dependencyManagement.setApplyMavenExclusions(false)` 설정으로 빌드 속도 개선

### Kotlin Compiler Options

```kotlin
kotlinOptions {
    jvmTarget = "21"
    languageVersion = "2.3"
    apiVersion = "2.3"
    freeCompilerArgs = listOf(
        "-Xjsr305=strict",
        "-jvm-default=enable",
        "-Xinline-classes",
        "-Xstring-concat=indy",
        "-Xcontext-parameters",
        "-Xannotation-default-target=param-property",
        // Opt-in annotations
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlin.ExperimentalStdlibApi",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        ...
    )
}
```

### Test Configuration

테스트는 다음과 같은 JVM 옵션으로 실행됩니다:

```kotlin
test {
    jvmArgs(
        "-Xshare:off",
        "-Xmx8G",
        "-XX:+UseZGC",
        "-XX:-MaxFDLimit",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+EnableDynamicAgentLoading",
    )
}
```

Quarkus 모듈의 경우 추가로:

```kotlin
systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
```

## Key Design Patterns

### Coroutines-First Approach

- 모든 비동기 작업은 Coroutines 기반으로 설계
- Reactor, RxJava 등 다른 reactive 라이브러리는 Coroutines와 통합하여 사용
- AWS SDK, HTTP 클라이언트 등 blocking API를 Coroutines로 래핑
- 최대한 내부 소스를 사용한다. (예: bluetape4k-core 의 RequireSupport.kt 등)

### High-Performance Optimization

- **압축**: LZ4, Zstd 등 고성능 압축 알고리즘 적극 활용
- **직렬화**: Kryo, Fory 등 Java 기본 직렬화보다 빠른 방식 사용
- **Redis Codec**: 공식 Codec보다 성능이 우수한 커스텀 Codec 제공
- **S3 TransferManager**: 대용량 파일 전송 시 성능 최적화

### Repository Generic Pattern

모든 Exposed Repository 인터페이스는 통일된 제네릭 패턴을 사용합니다:

- `JdbcRepository<ID: Any, E: Any>` — T(테이블 타입) 제네릭 제거, `val table: IdTable<ID>` 사용
- `R2dbcRepository<ID: Any, E: Any>` — 동일 패턴
- Redisson/Lettuce 캐시 Repository도 동일: `<ID: Any, E: Any>` + `extractId(entity): ID`
- `SoftDeletedJdbcRepository`/`SoftDeletedR2dbcRepository`만 `table.isDeleted` 접근을 위해 T 유지
- MapWriter의 writeThrough/writeBehind에서는 `Map<ID, E>`의 entry key로 ID 접근 (HasIdentifier 의존 없음)

### NearCache Unified Interface Pattern

모든 NearCache 백엔드(Lettuce, Hazelcast, Redisson, JCache)는 공통 인터페이스로 통일:

- `NearCacheOperations<V: Any>: AutoCloseable` — blocking 인터페이스 (키는 String 고정)
- `SuspendNearCacheOperations<V: Any>` — suspend 인터페이스 (`suspend fun close()`)
- `NearCacheStatistics` — 로컬/백엔드 hit/miss 통계
- `ResilientNearCacheDecorator` — retry + failure strategy Decorator (`.withResilience {}`)
- 팩토리 함수: `lettuceNearCacheOf()`, `hazelcastNearCacheOf()`, `redissonNearCacheOf()`, `jcacheNearCacheOf()`

```kotlin
// 기본 사용
val cache = lettuceNearCacheOf<MyValue>(redisClient, codec, config)
cache.put("key", value)

// Resilience 래핑
val resilient = lettuceNearCacheOf<MyValue>(redisClient, codec, config)
    .withResilience { retryMaxAttempts = 5 }
```

### Extension Functions

- Kotlin의 extension function을 적극 활용하여 기존 라이브러리의 사용성 개선
- 예: `Flow.chunked()`, `Flow.windowed()`, `File.copyToAsync()`, `Deferred.zipWith()` 등

### Result Pattern

- 최근 추가된 패턴으로, Result 타입을 사용한 에러 핸들링 (예: `io/io` 모듈의 파일 I/O)

### Testcontainers Integration

- 테스트 환경에서 Docker 기반 외부 서비스(Redis, Kafka, DB 등) 자동 구성

## Common Patterns

### Module Dependencies

각 모듈은 필요한 core 모듈을 의존성으로 가짐:

```kotlin
dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    // 모듈별 추가 의존성
}
```

### Codec Implementation

Redis, Jackson 등 직렬화가 필요한 곳에서는 고성능 Codec 구현:

- `bluetape4k-io`의 `BinarySerializer` 활용
- LZ4/Zstd 압축 + Kryo/Fory 직렬화 조합

### Coroutines Extensions

기존 라이브러리의 blocking API를 suspend function으로 변환:

```kotlin
suspend fun <T> blockingOperation(): T = withContext(Dispatchers.IO) {
    // blocking call
}
```

### Flow Operators

복잡한 Flow 처리를 위한 커스텀 연산자 제공:

- `Flow.chunked()`, `Flow.windowed()`: 배치 처리
- `AsyncFlow`: 비동기 매핑을 순서 보장하며 처리

## Version Management

프로젝트 버전은 `gradle.properties`에서 관리:

```properties
projectGroup=io.github.bluetape4k
baseVersion=1.5.0
snapshotVersion=-SNAPSHOT
```

- SNAPSHOT 빌드: `snapshotVersion=-SNAPSHOT`
- RELEASE 빌드: `snapshotVersion=` (빈 값)

## Git Workflow

- Main branch: `develop`
- 커밋 메시지는 한국어로, prefix 사용 (feat, fix, docs, refactor, test, chore 등)
- 예: `feat: Result 패턴 기반 파일 I/O 유틸리티 추가`

## Important Notes

### Publishing

- 이 프로젝트는 GitHub Packages Maven에 배포됩니다
- `workshop/` 및 `examples/` 하위 모듈은 배포되지 않음
- GitHub 토큰 설정 필요: `~/.gradle/gradle.properties` 또는 환경변수로 설정

### Atomicfu

- `kotlinx-atomicfu` 플러그인 사용
- VarHandle 기반 atomic 연산 (`jvmVariant = "VH"`)

### Detekt

- `exposed-jdbc-tests` 모듈은 Detekt disabled

### Jacoco (Currently Commented Out)

- 코드 커버리지 설정은 주석 처리되어 있음 (필요시 활성화)
