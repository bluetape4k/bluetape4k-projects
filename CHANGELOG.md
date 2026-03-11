# Changelog

모든 주요 변경 사항은 이 파일에 기록됩니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 따르며, 이 프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [1.3.1-SNAPSHOT] - Unreleased

### Added

#### infra/lettuce

- **`LettuceIntCodec`**: Int 값을 4바이트 big-endian으로 직렬화하는 `RedisCodec<String, Int>` (Redisson `IntegerCodec`과 바이너리 호환)
- **`LettuceLongCodec`**: Long 값을 8바이트 big-endian으로 직렬화하는 `RedisCodec<String, Long>` (Redisson `LongCodec`과 바이너리 호환)
- **`LettuceIntCodecTest`** / **`LettuceLongCodecTest`**: ByteBuffer round-trip, 경계값, Redis `hset`/
  `hget`, position 불변성 검증 테스트
- **`LettuceLeaderElection`** / **`LettuceSuspendLeaderElection`**: 분산 리더 선출
- **`LettuceLeaderGroupElection`** / **`LettuceSuspendLeaderGroupElection`**: 분산 그룹 리더 선출
- **`LeaderElectionOptions`** / **`LeaderGroupElectionOptions`**: `bluetape4k-leaders` 모듈 옵션 클래스

#### infra/cache-lettuce

- **`LettuceMemoizer<K, V>`**: `LettuceMap<V>` 기반 동기 메모이제이션 (`Memoizer<K,V>` 인터페이스)
- **`LettuceAsyncMemoizer<K, V>`**: `LettuceMap<V>` 기반 비동기 메모이제이션 (`AsyncMemoizer<K,V>` 인터페이스)
- **`LettuceSuspendMemoizer<K, V>`**: `LettuceMap<V>` 기반 suspend 메모이제이션 (`SuspendMemoizer<K,V>` 인터페이스)

#### infra/redisson

- **`RedissonMemoizer`** / **`AsyncRedissonMemoizer`** / **`RedissonSuspendMemoizer`**: `Memoizer<K,V>` 인터페이스 구현체 추가

#### cache-core (testFixtures)

- **`AbstractMemoizerTest`** / **`AbstractAsyncMemoizerTest`** / **`AbstractSuspendMemoizerTest`
  **: Memoizer 공통 테스트 기반 클래스

#### cache-hazelcast / cache-ignite

- `HazelcastMemoizer`, `IgniteMemoizer` 등 `Memoizer<K,V>` 인터페이스 구현 추가

#### io/protobuf (신규 모듈)

- `io/grpc`에서 Protobuf 유틸리티 분리 → `io/protobuf` 독립 모듈

#### io/okio (신규 모듈)

- `io/io` 모듈의 okio 패키지 → `io/okio` 독립 모듈로 분리

### Changed

#### infra/lettuce

- **분산 Primitive 클래스명 변경** (`Redis*` → `Lettuce*`):
  `RedisMap` → `LettuceMap`, `RedisSuspendMap` → `LettuceSuspendMap`,
  `RedisAtomicLong` → `LettuceAtomicLong`, `RedisSemaphore` → `LettuceSemaphore`,
  `RedisLock` → `LettuceLock` 등
- **`LettuceMap<V>`**: `syncCommands` 접근자 `protected` 변경, `putTtl` / `putAllTtl` 메서드 추가
- Memoizer 기능: `infra/lettuce` → `infra/cache-lettuce` 이동, `LettuceMemoizer<K:Any, V:Any>` Generic화

#### infra/redis (모듈 분리)

- `bluetape4k-redis` 단일 모듈 → `lettuce` + `redisson` + `spring-data-redis` 3개 모듈로 분리
- `awaitSuspending()`: 자체 구현 → `kotlinx.coroutines.future.await`로 대체

### Fixed

#### infra/lettuce

- **`LettuceIntCodec` / `LettuceLongCodec`**: `decodeValue`에서 absolute read →
  `bytes.duplicate()` 방식으로 수정 (caller position 불변)

#### infra/cache-lettuce

- **`LettuceAsyncMemoizer`**: `thenApply` 내부 blocking sync 호출 → `thenCompose` + `getAsync()`로 교체
- **`LettuceAsyncMemoizer.clear()`**: dangling future 방지를 위한 `inFlight.clear()` 제거

#### infra/cache-lettuce / cache-redisson

- `LettuceNearCache` / `RedissonNearCache` 기본 코덱을 LZ4 + Fory로 지정
- Write-behind `DEFAULT_BATCH_SIZE` 변경 (100 → 500)

#### aws-kotlin (dynamodb / s3 / sqs / sts)

- `existsTable` 전체 페이지 순회 개선, s3 `exists/putAll/getAll` 보강
- sqs `receive` 기본 대기시간 결함 수정, sts `durationSeconds` 계약 검증 추가

#### testing/testcontainers

- Testcontainers 1.21.4 → 2.0.3 업그레이드 안정화 (2.x 좌표 체계 반영)

#### 기타

- infra/nats: 관리 API 예외 계약 정리
- infra/bucket4j: probe 기반 잔여 토큰 계산 최적화
- infra/micrometer: Timer/Retrofit 계측 경로 회귀 보강
- infra/opentelemetry: Span/Coroutine helper 예외 처리 정리
- infra/resilience4j: 코드리뷰, KDoc 보강, README 최신화
- vertx/sqlclient: Transaction CancellationException 전파 보강, TupleMapper 리플렉션 캐시 도입
- utils/tokenizer: Korean/NounTokenizer topN 검증, fallback offset 보정 최적화
- utils/javatimes: 캘린더 주 동작 계약 정렬 및 범위 계약 강화
- testing/junit5: 스트레스 테스터 실행 모델 개선 (대량 rounds 시 메모리 안정화)

---

## [1.3.0] - 2026-03-06

### Added

#### io/tink (신규 모듈)

- **`bluetape4k-tink`**: Google Tink 기반 현대적 암호화 모듈
    - AEAD: AES-GCM, ChaCha20-Poly1305, XChaCha20-Poly1305
    - DAEAD: AES-SIV (결정적 암호화)
    - MAC: HMAC-SHA256/SHA512
    - `TinkEncryptSink` / `TinkDecryptSource` (Okio 스트리밍 암복호화)

#### io/jackson, io/jackson3

- Google Tink 기반 JSON 필드 암호화 지원 (`@EncryptedJsonField`)

#### data/exposed

- **`bluetape4k-exposed-tink`**: Google Tink 기반 Exposed 암호화 컬럼 (AEAD/DAEAD)
- `kotlin.uuid.Uuid` 타입 지원 (exposed-core)

#### cache-core / cache-*

- **Resilient NearCache** (write-behind + retry + graceful degradation):
  `ResilientLettuceNearCache`, `ResilientLettuceSuspendNearCache`,
  `ResilientRedissonNearCache`, `ResilientRedissonSuspendNearCache`

#### io/serialization

- **`ForyBinarySerializer` / `KryoBinarySerializer`**: 보안 모드 (`secureFory` / `secure`) 추가

### Changed

#### cache-core / cache-* (모듈 통합)

- **10개 → 5개(+umbrella)로 통합**:
    - `cache-local` → `cache-core`에 병합
    - `cache-hazelcast-near` → `cache-hazelcast`에 병합
    - `cache-ignite-near` → `cache-ignite`에 병합
    - `cache-redisson-near` → `cache-redisson`에 병합
- `JCaching`의 Redisson/Hazelcast/Ignite 객체를 각 모듈로 분리

#### data/exposed

- Exposed 암호화 컬럼: `bluetape4k-crypto` → `bluetape4k-tink` 전환
- `AbstractValueObject.equalProperties/hashCode` → `abstract`으로 변경

#### build

- Gradle 9.3.1 → 9.4.0 업그레이드
- 전역 싱글톤 `unsafeLazy` → `lazy(SYNCHRONIZED)` 일괄 변경

### Fixed

- 암호화 모듈에서 민감 정보 로깅 제거 (보안)
- io/avro: codec 기본값·Snappy 매핑 정합화, reflect 안정성 개선
- redis memorizer: in-flight dedup 로직 추가로 동시 호출 중복 계산 방지
- CloudWatch Logs: `"logs"` 서비스 지정 누락 수정
- while-delay 구문 → `await-untilSuspending` 전환

---

## [1.2.3] - 2026-03-03

### Added

#### data/mongodb (신규 모듈)

- MongoDB Kotlin Coroutine Driver 기반 확장 모듈
    - `mongoClient {}` DSL, `MongoClientProvider`
    - `findFirst`, `exists`, `upsert`, `findAsFlow` 확장 함수
    - `documentOf {}` DSL, Aggregation Pipeline DSL (`pipeline {}`)

#### spring/mongodb (신규 모듈)

- Spring Data MongoDB Reactive 확장 (코루틴 기반)

#### aws / aws-kotlin

- **CloudWatch Metrics / Logs**: 메트릭 발행/조회, 로그 그룹/스트림/이벤트 (Java SDK + Kotlin SDK)
- **Kinesis**: 스트림 관리, 레코드 전송/조회 (Java SDK + Kotlin SDK)
- **STS**: GetCallerIdentity, AssumeRole, 세션 토큰 발급 (Java SDK + Kotlin SDK)

#### utils/leader

- **`LeaderGroupElection`** / **`SuspendLeaderGroupElection`**: 분산 그룹 리더 선출

#### data/exposed

- **`bluetape4k-exposed-jackson3`**: Jackson 3.x 기반 Exposed JSON 컬럼 지원

#### utils/measured

- Angle, Area, Volume, Temperature, Pressure, Storage, Frequency, Energy/Power, BinarySize, GraphicsLength 단위 추가

#### cache

- **Ignite3 NearCache JCache SPI** 구현 (CachingProvider, CacheManager)

### Changed

- KDoc 표준화 보강 (spring/aws/kafka/opentelemetry/micrometer/bucket4j/cache/redis/nats/resilience4j/jackson/feign/retrofit2 등 전 모듈)

---

## [1.2.2] - 2026-03-01

### Fixed

- Snapshot 배포(publish) 버그 수정

---

## [1.2.1] - 2026-02-28

### Changed

- Maven Central 배포 설정 변경

---

## [1.2.0] - 2026-02-28

### Added

#### aws / aws-kotlin

- **`bluetape4k-aws-kms`**: AWS KMS 암호화 키 관리 (Java SDK v2)
- **`bluetape4k-aws-kotlin-kms`**: AWS KMS Kotlin SDK 확장

#### infra/cache

- **Lettuce `SuspendCache`** 구현
- **cache-redisson-near / cache-hazelcast-near / cache-ignite-near** 전용 near cache 모듈 신설
- **Hazelcast/Ignite2 `SuspendCache`** 서버 연동 구현

#### data/exposed

- **`bluetape4k-exposed-measured`**: Exposed Custom ColumnType 및 DB 방언 매트릭스 테스트
- **`bluetape4k-measured`** 코어 모듈: Units/Measure 기반 단위 조합 연산 (Length/Time/Mass)
- Measured 확장: Angle, Area, Storage, Pressure, Volume, Temperature

### Changed

- cache 모듈을 core/provider 구조로 재편:
    - `bluetape4k-cache-core` (공통 API, NearCache, SuspendCache 추상화)
    - `cache-local/redisson/hazelcast/ignite` provider 분리
- `cache-core` testFixtures 도입 (Abstract 테스트 공통 재사용)
- Virtual Threads 예제 구조화 (part1/part2/part3)

---

## [1.1.0] - 2026-02-22

### Added

#### utils/virtualthread (신규 모듈)

- **Java 21/25 Virtual Threads** 지원 모듈 분리
    - `VirtualThreadExecutor`, `VirtualThreadDispatcher`
    - `StructuredTaskScope` API 추상화 (Jdk21/Jdk25 공용)

#### data/exposed (모듈 분리)

- `bluetape4k-exposed` → `exposed-core` + `exposed-dao` + `exposed-jdbc` 3개 모듈 분리
    - `exposed-core`: JDBC 불필요 핵심 (컬럼 타입, ID 확장, HasIdentifier, ExposedPage)
    - `exposed-dao`: DAO 엔티티 확장, 커스텀 IdTable
    - `exposed-jdbc`: JDBC Repository, SuspendedQuery, VirtualThreadTransaction
    - `exposed`: umbrella (하위 호환)

#### spring

- **WebClient** 설정 개선 및 테스트 보강
- **spring-tests**: HTTP 클라이언트 확장 함수 추가

### Changed

- `CloseableCoroutineScope` 도입 및 Scope 구현체 리팩토링
- Coroutines MDC 컨텍스트 처리 개선 (`logging` 모듈 의존성 변경)
- R2DBC 코루틴 확장 네이밍 정리
- Cassandra 코루틴 함수명 정리
- 리소스 종료(shutdown) 로직 안전성 개선

### Fixed

- `Ksuid`, `KsuidMillis`에서 사용하는 `BytesBase62` 버그 수정
- `javatimes` range/period 로직 및 테스트 보강
- images 스트림 유틸 개선

---

## [1.0.0] - 2026-02-03

### Added

- **Eclipse Collections** 적용 및 컬렉션 처리 전반 최적화
- **spring-tests**: `RestClient` / `WebClient` / `WebTestClient` 확장 함수 추가

### Changed

- Atomic 관련 구조: `kotlinx.atomicfu` 기반으로 통일
- UUID 대신 Base58로 키 생성 방식 변경
- `SuspendRingBuffer` 및 Parallel 코드 개선

---

## [0.1.7] - 2026-01-26

### Changed

- `ExposedRepository.batchUpdate` → `batchUpsert`로 변경 및 기능 개선
- `kotlinx-atomicfu` → Java standard atomics로 교체
- Exposed Entity `toStringBuilder` 명칭 변경

### Fixed

- `TimebasedUuid` Deprecated 메시지 오타 수정

---

## [0.1.6] - 2026-01-23

### Changed

- **`KLogging` → `KLoggingChannel`** 전환 (전 모듈, 코루틴 환경 로깅 개선)
- `TimebasedUuid` 생성 방식 변경 (Reordered 사용)
- Kotlin `Enum.entries` 활용으로 변경
- `CoLeaderElection` 인터페이스 제거 (사용 중단됨)

---

## [0.1.5] - 2026-01-09

의존성 업그레이드 및 내부 안정성 개선.

---

## [0.1.4] - 2026-01-09

### Added

#### io/jackson3 (신규 모듈)

- **`bluetape4k-jackson3`**: Jackson 3.x 기반 JSON 처리 및 확장
- **`bluetape4k-jackson3-binary`**: CBOR, Ion, Smile 포맷 직렬화
- **`bluetape4k-jackson3-text`**: CSV, Properties, YAML, TOML 포맷 지원

#### io/jackson

- **`@JsonMasker`**: JSON 필드 마스킹 직렬화 지원
- **`@JsonEncrypt`**: JSON 필드 암호화/복호화 직렬화 지원

#### data/exposed

- **`bluetape4k-exposed-jackson3`**: Jackson 3.x 기반 Exposed JSON 컬럼 지원

---

## [0.1.3] - 2025-09-27

의존성 업그레이드 및 내부 안정성 개선.

---

## [0.1.2] - 2025-09-26

### Changed

- **Fory Codec** 관련 Serializer 추가
- **Fury Serializer** Deprecated 처리 시작

---

## [0.1.0] - 2025-09-26

### Changed

- **Fury → Fory** 전체 교체 (`io.fury.*` → `org.apache.fory.*`)
- 대규모 의존성 버전 업그레이드 (Kotlin, Spring Boot, Exposed, Hibernate, Vert.x, AWS 등)
- Elasticsearch 8.18.x → 9.1.x 업그레이드
- `suspendedTransactionAsync`를 사용하여 DB 트랜잭션 처리 개선
- Spring DataBuffer 관련 확장 함수 추가

---

## [0.0.10] - 2025-06-11

### Changed

- `runSuspendTest` → `runSuspendIO` 리네이밍
- suspend 함수 리네이밍 전반 (`coXxx` → `xxxSuspending` 패턴 적용)
- 코루틴 기반 테스트 로직 개선
- Bloom Filter suspend 기반 구현 추가
- 코루틴 기반 Writer/City/Country 테스트 보강

---

## [0.0.9] - 2025-05-28

의존성 업그레이드 및 내부 안정성 개선.

---

## [0.0.8] - 2025-05-28

### Added

- **Exposed-R2DBC 확장**: `TableExtensions`, `QueryExtensions`, `ReadableExtensions`
- `BatchInsertOnConflictDoNothing` 지원
- R2DBC 기반 Redisson 캐싱 및 테스트

### Changed

- Exposed v1.0.0-beta-2 업그레이드 및 API 마이그레이션

### Fixed

- `findAll()` 함수 `List<T>` 반환 수정
- `findLastOrNull()` 함수 수정

---

## [0.0.7] - 2025-05-19

의존성 업그레이드.

---

## [0.0.6] - 2025-05-19

### Changed

- **`KLogging` → `KLoggingChannel`** 전환 시작 (코루틴 환경 로깅 개선)
- Fury 0.10.2 업그레이드 (Javers 예외 해결)

---

## [0.0.5] - 2025-03-25

### Added

- **`bluetape4k-fastjson2`**: Fastjson2 기반 직렬화 모듈
- **`bluetape4k-exposed-fastjson2`**: Exposed Fastjson2 JSON 컬럼 지원

### Fixed

- Pulsar, RabbitMQ, ZipKin 버전 다운그레이드 (안정성)
- Entity `equals`에 type 비교 추가
- `idEquals` 에서 backReferencedOn 관계 테이블 참조 수정

---

## [0.0.4] - 2025-03-11

### Added

- **`bluetape4k-exposed-jasypt`**: Jasypt 기반 Exposed 암호화 컬럼 지원
- **`bluetape4k-exposed-tests`**: Exposed 공통 테스트 인프라
- **`ExposedRepository`** / **`ExposedCoroutineRepository`**: 범용 Exposed Repository 패턴 구현
- Exposed Jackson 컬럼 (`jackson`, `jacksonb`) 구현
- `KsuidMillisTable` 추가

### Changed

- Gradle 8.13, Spring Boot 3.4.3, Exposed 0.60.0 업그레이드

---

## [0.0.2] - 2024-11-28

### Added

- **`bluetape4k-spring-r2dbc`**: Spring Data R2DBC 모듈
- **`bluetape4k-spring-tests`**: Spring 테스트 유틸리티 모듈
- **`Measurable` / `MeasurableUnit`** 인터페이스 도입 (단위 추상화 기반)
- Temperature, Angle 단위 추가

### Changed

- Kotlin 2.1.0 업그레이드
- Gradle 8.11.1 업그레이드
- Coroutines 용 Controller 정의

---

## [0.0.1] - 2024-11-22

### Added

초기 릴리즈. 다음 모듈 포함:

- **`bluetape4k-aws-kotlin`**: AWS Kotlin SDK 기반 (core, dynamodb, s3, ses, sns, sqs)
- **`bluetape4k-logback-kafka`**: Logback Kafka Appender
- **`bluetape4k-coroutines`**: Kotlin Coroutines 유틸리티
- Examples: Cassandra, JPA+QueryDSL, Coroutines, MongoDB, Spring Webflux, Vert.x, Redisson, Mutiny, MapStruct
