# Bluetape4k Projects

Kotlin 언어로 JVM 환경에서 개발할 때 사용할 공용 라이브러리

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

7. Redis 를 댜양한 방식에서 사용할 수 있도록 지원합니다.
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

### Core 모듈 (`bluetape4k/`)

- **[core](./bluetape4k/core/README.md)**: 핵심 유틸리티 (assertions, 압축, required 등)
- **[coroutines](./bluetape4k/coroutines/README.md)**: Kotlin Coroutines 확장 (DeferredValue, Flow extensions, AsyncFlow)
- **[logging](./bluetape4k/logging/README.md)**: 로깅 관련 기능
- **bom**: Bill of Materials (의존성 관리)

### I/O 모듈 (`io/`)

- **[io](./io/io/README.md)**: 파일 I/O, 압축(LZ4, Zstd, Snappy), 직렬화(Kryo, Fory), Okio 통합 (Tink 기반 암호화 Sink/Source 포함)
- **[okio](./io/okio/README.md)**: Okio 기반 I/O 확장 — Buffer/Sink/Source 유틸리티, Base64, Channel, Cipher, Compress, Coroutines, Jasypt/Tink 암호화 Sink/Source
- **[jackson2](./io/jackson2/README.md)/[jackson3](./io/jackson3/README.md)
  **: Jackson 2.x/3.x 통합 — 바이너리(CBOR, Ion, Smile) 및 텍스트(CSV, YAML, TOML) 포맷 포함 (구 `jackson-binary/text`,
  `jackson3-binary/text` 통합됨)
- **[json](./io/json/README.md)**: JSON 처리
- **[csv](./io/csv/README.md)**: CSV 처리
- **[feign](./io/feign/README.md)**: Feign HTTP 클라이언트 (Coroutines 지원)
- **[retrofit2](./io/retrofit2/README.md)**: Retrofit2 HTTP 클라이언트 (Coroutines 지원)
- **[protobuf](./io/protobuf/README.md)**: Protobuf 유틸리티 (Timestamp/Duration/Money 변환, ProtobufSerializer)
- **[grpc](./io/grpc/README.md)**: gRPC 서버/클라이언트 추상화 (`bluetape4k-protobuf` 포함)
- ~~**[crypto](./io/crypto/README.md)**~~: 암호화 기능 (Jasypt 기반 PBE, BouncyCastle) — **Deprecated** (`bluetape4k-tink`로 대체)
- **[tink](./io/tink/README.md)**: Google Tink 기반 현대적 암호화 — AEAD, Deterministic AEAD, MAC, Digest, 통합 Encryptor (`TinkEncryptor`), Okio `TinkEncryptSink`/`TinkDecryptSource`
- **[http](./io/http/README.md)**: HTTP 유틸리티
- **[netty](./io/netty/README.md)**: Netty 통합
- **[avro](./io/avro/README.md)**: Apache Avro
- **[fastjson2](./io/fastjson2/README.md)**: FastJSON2
- **[vertx](./io/vertx/README.md)**: Vert.x 단일 통합 모듈 — 핵심 기능, SQL 클라이언트, Resilience4j 통합 포함 (구 `vertx/core`,
  `vertx/sqlclient`, `vertx/resilience4j` 통합됨)

### AWS 모듈 (`aws/`)

각 서비스마다 **3단계 API** 패턴 제공: `sync` → `async (CompletableFuture)` → `coroutines (suspend)`

- **bluetape4k-aws**: AWS Java SDK v2 기반 단일 통합 모듈 — DynamoDB, S3(TransferManager), SES, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS 포함. 각 서비스의 coroutines 확장 (`XxxAsyncClientCoroutinesExtensions.kt`) 제공
- **bluetape4k-aws-kotlin**: AWS Kotlin SDK 기반 단일 통합 모듈 — native `suspend` 함수 기본 제공 (`.await()` 변환 불필요). DynamoDB, S3, SES/SESv2, SNS, SQS, KMS, CloudWatch/Logs, Kinesis, STS 포함. DSL 지원 (`metricDatum {}`, `inputLogEvent {}`, `stsClientOf {}` 등)

### 데이터 모듈 (`data/`)

#### Exposed 모듈 (기능별 분리)

- **[exposed](./data/exposed/README.md)**: umbrella 모듈 — `exposed-core` + `exposed-dao` + `exposed-jdbc` 묶음 (하위 호환)
- **[exposed-core](./data/exposed-core/README.md)**: JDBC 없이 사용 가능한 핵심 기능 — 압축/암호화/직렬화 컬럼 타입, ID 생성 확장, `HasIdentifier`, `ExposedPage`
- **[exposed-dao](./data/exposed-dao/README.md)**: DAO 엔티티 확장 — `EntityExtensions`, `StringEntity`, 커스텀 IdTable (`KsuidTable`, `SnowflakeIdTable`, `SoftDeletedIdTable` 등)
- **[exposed-jdbc](./data/exposed-jdbc/README.md)**: JDBC 전용 — `ExposedRepository`, `SoftDeletedRepository`, `SuspendedQuery`, `VirtualThreadTransaction`
- **[exposed-r2dbc](./data/exposed-r2dbc/README.md)**: Exposed + R2DBC (reactive, `ExposedR2dbcRepository`)
- **[exposed-jdbc-redisson](./data/exposed-jdbc-redisson/README.md)**: Exposed JDBC + Redisson (분산 락)
- **[exposed-r2dbc-redisson](./data/exposed-r2dbc-redisson/README.md)**: Exposed R2DBC + Redisson (분산 락)
- **[exposed-jackson2](./data/exposed-jackson2/README.md)/[jackson3](./data/exposed-jackson3/README.md)**: Exposed JSON 컬럼 지원 (Jackson 2.x/3.x)
- **[exposed-fastjson2](./data/exposed-fastjson2/README.md)**: Exposed FastJSON2 JSON 컬럼 지원
- **[exposed-jasypt](./data/exposed-jasypt/README.md)**: Exposed Jasypt 암호화 컬럼
- **[exposed-tink](./data/exposed-tink/README.md)**: Exposed 암호화 컬럼 (Google Tink AEAD/Deterministic AEAD)
- **[exposed-measured](./data/exposed-measured/README.md)**: Exposed 쿼리 실행 시간 측정 (Micrometer 통합)
- **[exposed-jdbc-tests](./data/exposed-jdbc-tests/README.md)**: JDBC 기반 테스트 공통 인프라
- **[exposed-r2dbc-tests](./data/exposed-r2dbc-tests/README.md)**: R2DBC 기반 테스트 공통 인프라

#### 기타 데이터 모듈

- **[hibernate](./data/hibernate/README.md)/[hibernate-reactive](./data/hibernate-reactive/README.md)**: Hibernate ORM 통합
- **[mongodb](./data/mongodb/README.md)**: MongoDB Kotlin Coroutine Driver 확장 — `mongoClient {}` DSL, `findFirst`, `exists`, `upsert`, `findAsFlow`, `documentOf {}`, Aggregation Pipeline DSL
- **[r2dbc](./data/r2dbc/README.md)**: R2DBC 지원
- **[cassandra](./data/cassandra/README.md)**: Cassandra 드라이버
- **[jdbc](./data/jdbc/README.md)**: JDBC 유틸리티

### 인프라 모듈 (`infra/`)

- **[redis](./infra/redis/README.md)**: Lettuce/Redisson umbrella 모듈 (하위 호환)
    - **[lettuce](./infra/lettuce/README.md)**: Lettuce 클라이언트, 고성능 Codec (Jdk/Kryo/Fory × GZip/LZ4/Snappy/Zstd),
      `RedisFuture` → Coroutines 어댑터
    - **[redisson](./infra/redisson/README.md)**: Redisson 클라이언트, Codec, Memorizer, NearCache (
      `RLocalCachedMap`), Leader Election (Coroutines 지원)
- **[kafka](./infra/kafka/README.md)**: Kafka 클라이언트
- **[resilience4j](./infra/resilience4j/README.md)**: Resilience4j + Coroutines, Coroutines Cache
- **[bucket4j](./infra/bucket4j/README.md)**: Rate limiting
- **[micrometer](./infra/micrometer/README.md)**: 메트릭
- **[opentelemetry](./infra/opentelemetry/README.md)**: 분산 추적

#### 캐시 모듈 (`infra/cache-*`)

플러그인 방식으로 백엔드를 교체할 수 있는 캐시 추상화 레이어입니다.

- **[cache](./infra/cache/README.md)**: umbrella 모듈 (cache-core + hazelcast + redisson + lettuce)
- **[cache-core](./infra/cache-core/README.md)**: JCache 추상화 + Caffeine/Cache2k/Ehcache 로컬 캐시 (구 `cache-local` 병합) — `AsyncCache`, `SuspendCache`, `NearCache`, `SuspendNearCache`, Memorizer 구현체, testFixtures 6종 추상 테스트
- **[cache-hazelcast](./infra/cache-hazelcast/README.md)**: Hazelcast 분산 캐시 + Caffeine 2-Tier Near Cache (구 `cache-hazelcast-near` 병합)
- **[cache-redisson](./infra/cache-redisson/README.md)**: Redisson 분산 캐시 + Caffeine 2-Tier Near Cache (구 `cache-redisson-near` 병합)
- **[cache-lettuce](./infra/cache-lettuce/README.md)**: Lettuce(Redis) 기반 분산 캐시 — `LettuceNearCacheConfig`, RESP3 CLIENT TRACKING 기반 자동 invalidation

### Spring Boot 3 모듈 (`spring-boot3/`)

- **[bluetape4k-spring-boot3](./spring-boot3/core/README.md)** (
  `spring-boot3/core`): Spring Boot 3 기반 공통 기능 통합 모듈 — Spring core 유틸리티, WebFlux + Coroutines, Retrofit2 통합, 테스트 유틸리티 포함 (구
  `spring/core`, `spring/webflux`, `spring/retrofit2`, `spring/tests` 통합됨)
- **[data-redis](./spring-boot3/data-redis/README.md)**: Spring Data Redis 고성능 직렬화 — `RedisBinarySerializer`,
  `RedisCompressSerializer`, `redisSerializationContext {}` DSL
- **[cassandra](./spring-boot3/cassandra/README.md)**: Spring Data Cassandra
- **[mongodb](./spring-boot3/mongodb/README.md)**: Spring Data MongoDB Reactive —
  `ReactiveMongoOperations` 코루틴 확장, Criteria/Query/Update infix DSL
- **[r2dbc](./spring-boot3/r2dbc/README.md)**: Spring Data R2DBC

> Spring Data JPA는 `data/hibernate` 모듈로 이동했습니다.

### Spring Boot 4 모듈 (`spring-boot4/`)

Spring Boot 4.x 전용 모듈. Spring Boot 3 모듈과 독립적으로 사용 가능합니다.

> **BOM 적용 주의**: `dependencyManagement { imports }` 대신 `implementation(platform(...))` 방식으로 적용해야 KGP 2.3.x와 충돌 없이 빌드됩니다.

- **[core](./spring-boot4/core/README.md)** (`bluetape4k-spring-boot4-core`): Spring Boot 4 기반 공통 기능 — WebFlux + Coroutines, RestClient DSL (`suspendGet`, `suspendPost` 등), Jackson 2 커스터마이저, Retrofit2 통합, WebTestClient 테스트 유틸리티
- **[data-redis](./spring-boot4/data-redis/README.md)**: Spring Data Redis 고성능 직렬화 — `RedisBinarySerializer`, `RedisCompressSerializer`, `redisSerializationContext {}` DSL
- **[cassandra](./spring-boot4/cassandra/README.md)**: Spring Data Cassandra 코루틴 확장
- **[mongodb](./spring-boot4/mongodb/README.md)**: Spring Data MongoDB Reactive 코루틴 확장, Criteria/Query/Update infix DSL
- **[r2dbc](./spring-boot4/r2dbc/README.md)**: Spring Data R2DBC 코루틴 확장
- **[cassandra-demo](./spring-boot4/cassandra-demo/README.md)**: Cassandra 사용 예제

### 유틸리티 모듈 (`utils/`)

- **[bluetape4k-geo](./utils/geo/README.md)** (
  `utils/geo`): 지리 정보 처리 단일 통합 모듈 — geocode(Bing/Google), geohash, geoip2(MaxMind) 포함 (구 `utils/geocode`,
  `utils/geohash`, `utils/geoip2` 통합됨)
- **[idgenerators](./utils/idgenerators/README.md)**: ID 생성기 — `Uuid`(V1~V7 통일 API), `ULID`, `Ksuid`(Seconds/Millis), `Snowflakers` 통일 팩토리, `Flake`, `Hashids` 등 다양한 ID 생성 알고리즘 제공
- **[images](./utils/images/README.md)**: 이미지 처리
- **[javatimes](./utils/javatimes/README.md)**: 날짜/시간 유틸리티
- **[jwt](./utils/jwt/README.md)**: JWT 처리
- **[leader](./utils/leader/README.md)**: Leader 선출
- **[math](./utils/math/README.md)**: 수학 유틸리티
- **[measured](./utils/measured/README.md)**: 조합 가능한 단위 타입(`Units`)과 측정값(`Measure`) 기반으로,
  복합 단위(`m/s`, `kg*m/s^2`)를 타입 안전하게 표현
- **[money](./utils/money/README.md)**: Money API
- **[mutiny](./utils/mutiny/README.md)**: Mutiny reactive 통합
- ~~**units**~~: 단위 표현 value class — **Deprecated** (`bluetape4k-measured`의 기능으로 통합)

### 테스트 모듈 (`testing/`)

- **[junit5](./testing/junit5/README.md)**: JUnit 5 확장 및 유틸리티
- **[testcontainers](./testing/testcontainers/README.md)**: Testcontainers 지원 (Redis, Kafka, DB 등)

### Virtual Thread 모듈 (`virtualthread/`)

- **[virtualthread](./virtualthread/README.md)**: Java 21/25 Virtual Thread 지원
    - **[api](./virtualthread/api/README.md)**: Virtual Thread API 및 ServiceLoader 기반 런타임 선택
    - **[jdk21](./virtualthread/jdk21/README.md)**: Java 21 Virtual Thread 구현체
    - **[jdk25](./virtualthread/jdk25/README.md)**: Java 25 Virtual Thread 구현체

### 기타 모듈

- **[timefold](./timefold/solver-persistence-exposed/README.md)**: Timefold Solver + Exposed 통합

### 예제 모듈 (`examples/`)

라이브러리 사용 방법을 보여주는 예제 모듈입니다. 배포되지 않습니다.

- **[coroutines-demo](./examples/coroutines-demo/README.md)**: Kotlin Coroutines 사용 예제
- **[jpa-querydsl-demo](./examples/jpa-querydsl-demo/README.md)**: JPA + QueryDSL 사용 예제
- **[redisson-demo](./examples/redisson-demo/README.md)**: Redisson 사용 예제
- **[virtualthreads-demo](./examples/virtualthreads-demo/README.md)**: Java Virtual Thread 사용 예제

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
./gradlew publishAggregationToCentralSnapshots --no-daemon --no-configuration-cache
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
