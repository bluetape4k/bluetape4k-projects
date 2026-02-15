# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Bluetape4k은 Kotlin 언어로 JVM 환경에서 Backend 개발 시 사용하는 공용 라이브러리 모음입니다. Kotlin의 장점을 최대화하고, 기존 Java 라이브러리를 개선하며, Kotlin Coroutines 기반의 async/non-blocking 개발을 지원합니다.

## Development Guidelines

### Language and Documentation

- 주석과 설명은 KDoc 형식으로 **한국어**로 작성
- 커밋 메시지는 **한국어**로 작성하며, 머릿말(feat, fix, docs, style, refactor, perf, test, chore) 사용
- Kotlin 2.1.20 이상 사용
- 최대한 Kotlin extensions와 DSL 활용

### Technology Stack

- Java 21 (JVM Toolchain)
- Kotlin 2.3 (languageVersion & apiVersion)
- Spring Boot 3.4.0+
- Kotlin Exposed 1.0.0+
- 데이터베이스: H2, PostgreSQL, MySQL 주로 사용
- 최대한 내부 소스를 사용한다. (예: bluetape4k-core 의 RequireSupport.kt 등)

### Testing Standards

- JUnit 5 기반 테스트
- MockK를 Mock 라이브러리로 사용
- Kluent를 Assertions 라이브러리로 사용
- 예제는 간결하되 프로덕션 수준으로 작성하며, 실제 동작 가능해야 함

### Documentation

- Public Class, Interface, Extensions methods 에 대해서 핈수적으로 KDoc 을 작성한다
- KDoc 은 항상 한국어로 작성한다

## Build Commands

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

## Architecture

### Module Structure

프로젝트는 멀티 모듈 Gradle 프로젝트로, 기능별로 모듈이 분리되어 있습니다:

#### Core Modules (`bluetape4k/`)

- **core**: 핵심 유틸리티 (assertions, required, 압축 등)
- **coroutines**: Coroutines 관련 유틸리티 (DeferredValue, Flow extensions, AsyncFlow 등)
- **logging**: 로깅 관련 기능
- **bom**: Bill of Materials (dependency management)

#### I/O Modules (`io/`)

- **io**: 파일 I/O, 압축(LZ4, Zstd, Snappy), 직렬화(Kryo, Fory), Okio 통합
- **json**: JSON 처리
- **jackson**: Jackson 2.x 통합
- **jackson3**: Jackson 3.x 통합
- **jackson-binary/jackson-text**: Jackson 바이너리/텍스트 포맷
- **csv**: CSV 처리
- **avro**: Apache Avro
- **feign**: Feign HTTP 클라이언트 (Coroutines 지원)
- **retrofit2**: Retrofit2 (Coroutines 지원)
- **http**: HTTP 유틸리티
- **netty**: Netty 통합
- **grpc**: gRPC 지원
- **crypto**: 암호화 기능

#### AWS Modules (`aws/`, `aws-kotlin/`)

- **aws/core**: AWS Java SDK v2 공통 기능
- **aws/dynamodb**: DynamoDB (Java SDK, async/non-blocking)
- **aws/s3**: S3 (TransferManager, 대용량 파일 전송 최적화)
- **aws/ses, aws/sns, aws/sqs**: 이메일, 메시징 서비스
- **aws-kotlin/***: AWS Kotlin SDK 통합 (Coroutines 네이티브 지원)

#### Data Modules (`data/`)

- **exposed**: Kotlin Exposed ORM 확장
- **exposed-r2dbc**: Exposed + R2DBC (reactive)
- **exposed-redisson**: Exposed + Redisson (분산 락)
- **exposed-jackson/jackson3**: Exposed JSON 컬럼 지원
- **hibernate**: Hibernate 통합
- **hibernate-reactive**: Hibernate Reactive
- **jdbc**: JDBC 유틸리티
- **r2dbc**: R2DBC 지원
- **cassandra**: Cassandra 드라이버

#### Infrastructure Modules (`infra/`)

- **redis**: Lettuce/Redisson, 고성능 Codec, Near Cache
- **kafka**: Kafka 클라이언트
- **resilience4j**: Resilience4j + Coroutines, Coroutines Cache
- **cache**: 캐시 추상화
- **bucket4j**: Rate limiting
- **micrometer**: 메트릭
- **opentelemetry**: 분산 추적
- **nats**: NATS 메시징

#### Spring Modules (`spring/`)

- **core**: Spring 공통 기능
- **cassandra**: Spring Data Cassandra
- **r2dbc**: Spring Data R2DBC
- **retrofit2**: Spring + Retrofit2 통합
- **tests**: Spring 테스트 유틸리티

#### Vert.x Modules (`vertx/`)

- **core**: Vert.x 핵심 기능
- **sqlclient**: Vert.x SQL 클라이언트
- **resilience4j**: Vert.x + Resilience4j

#### Utilities (`utils/`)

- **units**: 단위 표현 value class (시간, 용량, 거리 등)
- **idgenerators**: ID 생성기
- **money**: Money API
- **jwt**: JWT 처리
- **images**: 이미지 처리
- **geocode, geohash, geoip2**: 지리 정보
- **lingua**: 언어 감지
- **ahocorasick**: 문자열 검색
- **mutiny**: Mutiny reactive 라이브러리 통합

#### Testing Modules (`testing/`)

- **junit5**: JUnit 5 확장 및 유틸리티
- **testcontainers**: Testcontainers 지원

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

### High-Performance Optimization

- **압축**: LZ4, Zstd 등 고성능 압축 알고리즘 적극 활용
- **직렬화**: Kryo, Fory 등 Java 기본 직렬화보다 빠른 방식 사용
- **Redis Codec**: 공식 Codec보다 성능이 우수한 커스텀 Codec 제공
- **S3 TransferManager**: 대용량 파일 전송 시 성능 최적화

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
projectGroup=io.bluetape4k
baseVersion=1.1.0
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

- `exposed-tests` 모듈은 Detekt disabled

### Jacoco (Currently Commented Out)

- 코드 커버리지 설정은 주석 처리되어 있음 (필요시 활성화)
