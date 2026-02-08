# Bluetape4k Projects

Kotlin 언어로 JVM 환경에서 개발할 때 사용할 공용 라이브러리

![Blue Tape](./doc/bluetape4k.png)

## 소개

Kotlin 언어를 배우고, 사용하면서, Backend 개발에 자주 사용하는 기술, Coroutines 등 기존 라이브러리가 제공하지 않는 기능 들을 개발해 왔습니다.

1. Kotlin 의 장점을 최대화 할 수 있는 추천할 만한 코딩 스타일을 제공할 수 있는 기능을 제공합니다.
    - `bluetape4k-core` 의 assertions, required 같은 기능
    - `bluetape4k-units` 의 단위를 표현하는 value class 제공

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
    - `bluetape4k-aws-xxxx` AWS Java SDK 사용 시 Async/Non-Blocking 방식으로 사용할 수 있도록 합니다.
    - `bluetape4k-aws-s3` 는 S3 사용 시 Async/Non-Blocking 방식, TransferManager 를 사용하여, 대용량 파일 전송 시 성능을 향상할 수 있습니다.

5. AWS Kotlin SDK 를 사용을 편리하게 하기 위한 기능을 제공합니다.
    - `bluetape4k-aws-kotlin-xxxx` AWS Kotlin SDK 를 사용할 때 유용하고, 고성능을 지원할 수 있도록 Coroutines 를 활용하는 기능 및 예제를 제공합니다.

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
- **Spring Boot**: 3.4.0+
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

- **[io](./io/io/README.md)**: 파일 I/O, 압축(LZ4, Zstd, Snappy), 직렬화(Kryo, Fory), Okio 통합
- **[jackson](./io/jackson/README.md)/[jackson3](./io/jackson3/README.md)
  **: Jackson 2.x/3.x 통합 및 [바이너리](./io/jackson-binary/README.md)/[텍스트](./io/jackson-text/README.md) 포맷 지원
- **[json](./io/json/README.md)**: JSON 처리
- **[csv](./io/csv/README.md)**: CSV 처리
- **[feign](./io/feign/README.md)**: Feign HTTP 클라이언트 (Coroutines 지원)
- **[retrofit2](./io/retrofit2/README.md)**: Retrofit2 HTTP 클라이언트 (Coroutines 지원)
- **[grpc](./io/grpc/README.md)**: gRPC 지원
- **[crypto](./io/crypto/README.md)**: 암호화 기능
- **[http](./io/http/README.md)**: HTTP 유틸리티
- **[netty](./io/netty/README.md)**: Netty 통합
- **[avro](./io/avro/README.md)**: Apache Avro
- **[fastjson2](./io/fastjson2/README.md)**: FastJSON2

### AWS 모듈 (`aws/`, `aws-kotlin/`)

- **aws/**: AWS Java SDK v2 기반
   - **[core](./aws/core/README.md)**: AWS SDK 공통 기능
   - **[dynamodb](./aws/dynamodb/README.md)**: DynamoDB (async/non-blocking)
   - **[s3](./aws/s3/README.md)**: S3 (TransferManager, 대용량 파일 전송 최적화)
   - **[ses](./aws/ses/README.md)**: Simple Email Service
   - **[sns](./aws/sns/README.md)**: Simple Notification Service
   - **[sqs](./aws/sqs/README.md)**: Simple Queue Service
- **aws-kotlin/**: AWS Kotlin SDK 기반 (Coroutines 네이티브 지원)
   - **[core](./aws-kotlin/core/README.md)**: AWS Kotlin SDK 공통 기능
   - **[dynamodb](./aws-kotlin/dynamodb/README.md)**: DynamoDB
   - **[s3](./aws-kotlin/s3/README.md)**: S3
   - **[ses](./aws-kotlin/ses/README.md)/[sesv2](./aws-kotlin/sesv2/README.md)**: Simple Email Service
   - **[sns](./aws-kotlin/sns/README.md)**: Simple Notification Service
   - **[sqs](./aws-kotlin/sqs/README.md)**: Simple Queue Service

### 데이터 모듈 (`data/`)

- **[exposed](./data/exposed/README.md)**: Kotlin Exposed ORM 확장
- **[exposed-r2dbc](./data/exposed-r2dbc/README.md)**: Exposed + R2DBC (reactive)
- **[exposed-redisson](./data/exposed-redisson/README.md)**: Exposed + Redisson (분산 락)
- **[exposed-r2dbc-redisson](./data/exposed-r2dbc-redisson/README.md)**: Exposed + R2DBC + Redisson
- **[exposed-jackson](./data/exposed-jackson/README.md)/[jackson3](./data/exposed-jackson3/README.md)
  **: Exposed JSON 컬럼 지원
- **[exposed-fastjson2](./data/exposed-fastjson2/README.md)**: Exposed FastJSON2 통합
- **[exposed-jasypt](./data/exposed-jasypt/README.md)**: Exposed Jasypt 암호화
- **[hibernate](./data/hibernate/README.md)/[hibernate-reactive](./data/hibernate-reactive/README.md)
  **: Hibernate ORM 통합
- **[r2dbc](./data/r2dbc/README.md)**: R2DBC 지원
- **[cassandra](./data/cassandra/README.md)**: Cassandra 드라이버
- **[jdbc](./data/jdbc/README.md)**: JDBC 유틸리티

### 인프라 모듈 (`infra/`)

- **[redis](./infra/redis/README.md)**: Lettuce/Redisson 통합, 고성능 Codec, Near Cache
- **[kafka](./infra/kafka/README.md)**: Kafka 클라이언트
- **[resilience4j](./infra/resilience4j/README.md)**: Resilience4j + Coroutines, Coroutines Cache
- **[cache](./infra/cache/README.md)**: 캐시 추상화
- **[bucket4j](./infra/bucket4j/README.md)**: Rate limiting
- **[micrometer](./infra/micrometer/README.md)**: 메트릭
- **[opentelemetry](./infra/opentelemetry/README.md)**: 분산 추적
- **[nats](./infra/nats/README.md)**: NATS 메시징

### Spring 모듈 (`spring/`)

- **[core](./spring/core/README.md)**: Spring Boot 공통 기능
- **[cassandra](./spring/cassandra/README.md)**: Spring Data Cassandra
- **[r2dbc](./spring/r2dbc/README.md)**: Spring Data R2DBC
- **[jpa](./spring/jpa/README.md)**: Spring Data JPA
- **[webflux](./spring/webflux/README.md)**: Spring WebFlux
- **[retrofit2](./spring/retrofit2/README.md)**: Spring + Retrofit2 통합
- **[modulith-events-exposed](./spring/modulith-events-exposed/README.md)**: Spring Modulith Events + Exposed
- **[tests](./spring/tests/README.md)**: Spring 테스트 유틸리티

### Vert.x 모듈 (`vertx/`)

- **[core](./vertx/core/README.md)**: Vert.x 핵심 기능
- **[sqlclient](./vertx/sqlclient/README.md)**: Vert.x SQL 클라이언트
- **[resilience4j](./vertx/resilience4j/README.md)**: Vert.x + Resilience4j

### 유틸리티 모듈 (`utils/`)

- **[units](./utils/units/README.md)**: 단위 표현 value class (시간, 용량, 거리 등)
- **[idgenerators](./utils/idgenerators/README.md)**: ID 생성기 (Ksuid, Snowflake, ULID, UUID 등)
- **[money](./utils/money/README.md)**: Money API
- **[jwt](./utils/jwt/README.md)**: JWT 처리
- **[geocode](./utils/geocode/README.md)/[geohash](./utils/geohash/README.md)/[geoip2](./utils/geoip2/README.md)
  **: 지리 정보 처리
- **[lingua](./utils/lingua/README.md)**: 언어 감지
- **[images](./utils/images/README.md)**: 이미지 처리
- **[ahocorasick](./utils/ahocorasick/README.md)**: 문자열 검색
- **[bloomfilter](./utils/bloomfilter/README.md)**: Bloom Filter
- **[captcha](./utils/captcha/README.md)**: CAPTCHA 생성
- **[javatimes](./utils/javatimes/README.md)**: 날짜/시간 유틸리티
- **[leader](./utils/leader/README.md)**: Leader 선출
- **[logback-kafka](./utils/logback-kafka/README.md)**: Logback Kafka Appender
- **[math](./utils/math/README.md)**: 수학 유틸리티
- **[mutiny](./utils/mutiny/README.md)**: Mutiny reactive 통합
- **[naivebayes](./utils/naivebayes/README.md)**: Naive Bayes 분류기

### 테스트 모듈 (`testing/`)

- **[junit5](./testing/junit5/README.md)**: JUnit 5 확장 및 유틸리티
- **[testcontainers](./testing/testcontainers/README.md)**: Testcontainers 지원 (Redis, Kafka, DB 등)

### 기타 모듈

- **[javers](./javers/README.md)**: JaVers 감사 로그
   - **[core](./javers/core/README.md)**: JaVers 핵심 기능
   - **[persistence-kafka](./javers/persistence-kafka/README.md)**: Kafka 영속화
   - **[persistence-redis](./javers/persistence-redis/README.md)**: Redis 영속화
- **[tokenizer](./tokenizer/core/README.md)**: 형태소 분석기
   - **[korean](./tokenizer/korean/README.md)**: 한국어 형태소 분석
   - **[japanese](./tokenizer/japanese/README.md)**: 일본어 형태소 분석
- **[timefold](./timefold/solver-persistence-exposed/README.md)**: Timefold Solver + Exposed 통합
- **examples/**: 라이브러리 사용 예제

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
projectGroup=io.bluetape4k
baseVersion=1.1.0
snapshotVersion=-SNAPSHOT
```

### SNAPSHOT 배포

```bash
# GitHub Packages Maven에 SNAPSHOT 배포
./gradlew publishBluetape4kPublicationToBluetape4kRepository
```

### RELEASE 배포

```bash
# snapshotVersion을 제거하고 RELEASE 배포
./gradlew publishBluetape4kPublicationToBluetape4kRepository -PsnapshotVersion=
```

**참고**: GitHub Packages 배포를 위해서는 `~/.gradle/gradle.properties`에 다음 설정이 필요합니다:

```properties
gpr.user=your-github-username
gpr.publish.key=your-github-token
```
