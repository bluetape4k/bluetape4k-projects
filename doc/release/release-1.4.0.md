## [1.4.0] - 2026-03-12

### Added

#### infra/lettuce

- **`LettuceIntCodec`**: Int 값을 4바이트 big-endian으로 직렬화하는 `RedisCodec<String, Int>` (Redisson `IntegerCodec`과 바이너리 호환)
- **`LettuceLongCodec`**: Long 값을 8바이트 big-endian으로 직렬화하는 `RedisCodec<String, Long>` (Redisson `LongCodec`과 바이너리 호환)
- **`LettuceIntCodecTest`** / **`LettuceLongCodecTest`**: ByteBuffer round-trip, 경계값, Redis `hset`/
  `hget`, position 불변성 검증 테스트
- **`LettuceLeaderElection`** / **`LettuceSuspendLeaderElection`**: 분산 리더 선출
- **`LettuceLeaderGroupElection`** / **`LettuceSuspendLeaderGroupElection`**: 분산 그룹 리더 선출
- **`LeaderElectionOptions`** / **`LeaderGroupElectionOptions`**: `bluetape4k-leaders` 모듈 옵션 클래스
- Lettuce cache contracts 강화 및 Redis 8 테스트 추가

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
- cache 모듈별 **Factory object** 추가 및 JCache SPI 정리

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
- 분산 Primitive 리팩토링 및 동시성 테스트 강화

#### infra/redis (모듈 분리)

- `bluetape4k-redis` 단일 모듈 → `lettuce` + `redisson` + `spring-data-redis` 3개 모듈로 분리
- `awaitSuspending()`: 자체 구현 → `kotlinx.coroutines.future.await`로 대체

#### utils/leader

- leader local election flows 단순화 (불필요한 중간 state 제거)
- `LeaderElectionOptions` / `LeaderGroupElectionOptions` 옵션 클래스 lettuce/redisson 모듈에 적용

#### bluetape4k-core

- 핵심 유틸리티 코드 단순화 리팩토링

#### infra/cache-core

- **`SuspendNearCache.clear()`**: front/back cache 모두 clear 완료 후 info 로그 추가

#### infra/cache-lettuce

- **`ResilientLettuceSuspendNearCacheTest`**: write-behind 동기화 timeout 3초 → 5초로 증가 (테스트 안정성 개선)

#### infra/cache-redisson

- **`RedisSuspendNearCacheTest`**: Redisson `DEFAULT_EXPIRY_CHECK_PERIOD`(30s)를 고려한 clearAll timeout 30→40초 조정,
  `untilSuspending`에 `atMost` 명시

#### infra/nats

- **`ServerPoolExample`**: `@TestInstance(PER_CLASS)` + `@BeforeAll`/
  `@AfterAll` 방식으로 NATS 서버 관리 개선, 순차 기동으로 Docker 레이스 컨디션 방지

### Removed

#### infra/cache-ignite2

- **`cache-ignite2` 모듈 완전 제거**: cache umbrella에서 ignite2 의존성 삭제, 모듈 소스 및 테스트 전체 삭제
    - Apache Ignite 2.x 지원 종료 (Ignite 3.x `cache-ignite`로 대체)

#### examples

- vertx 관련 예제 제거 (미사용)

### Fixed

#### infra/lettuce

- **`LettuceIntCodec` / `LettuceLongCodec`**: `decodeValue`에서 absolute read →
  `bytes.duplicate()` 방식으로 수정 (caller position 불변)
- **`LettuceJCaching`**: 기본값 `localhost:6379` 에 자동 연결되는 문제 수정

#### infra/cache-lettuce

- **`LettuceAsyncMemoizer`**: `thenApply` 내부 blocking sync 호출 → `thenCompose` + `getAsync()`로 교체
- **`LettuceAsyncMemoizer.clear()`**: dangling future 방지를 위한 `inFlight.clear()` 제거

#### infra/cache-lettuce / cache-redisson

- `LettuceNearCache` / `RedissonNearCache` 기본 코덱을 LZ4 + Fory로 지정
- Write-behind `DEFAULT_BATCH_SIZE` 변경 (100 → 500)

#### infra/cache-hazelcast

- **`HazelcastJCaching`**: 기본 포트(5701)에 무한 재시도하는 문제 수정

#### infra/cache-ignite

- **`IgniteAsyncMemoizer`**: 비동기 API(`*Async()`)로 개선하여 ARM64 타임아웃 문제 해결
- `untilSuspending` 의 hang 방지 및 timeout 전파 수정 (root cause 보존)
- redisson 비동기 near cache 테스트를 `runSuspendIO`로 전환
- ignite 동적 cache readiness 회귀 테스트 추가

#### aws-kotlin (dynamodb / s3 / sqs / sts)

- `existsTable` 전체 페이지 순회 개선, s3 `exists/putAll/getAll` 보강
- sqs `receive` 기본 대기시간 결함 수정, sts `durationSeconds` 계약 검증 추가

#### testing/testcontainers

- Testcontainers 1.21.4 → 2.0.3 업그레이드 안정화 (2.x 좌표 체계 반영)

#### bluetape4k-coroutines

- Coroutines 함수 호출 버그 수정

#### 기타

- infra/nats: 관리 API 예외 계약 정리
- infra/bucket4j: probe 기반 잔여 토큰 계산 최적화
- infra/micrometer: Timer/Retrofit 계측 경로 회귀 보강
- infra/opentelemetry: Span/Coroutine helper 예외 처리 정리
- infra/resilience4j: 코드리뷰, KDoc 보강, README 최신화
- vertx/sqlclient: Transaction CancellationException 전파 보강, TupleMapper 리플렉션 캐시 도입
- utils/tokenizer: Korean/NounTokenizer topN 검증, fallback offset 보정 최적화
- utils/javatimes: ISO nullable 포맷 정렬, 캘린더 주 동작 계약 정렬 및 범위 계약 강화
- testing/junit5: 스트레스 테스터 실행 모델 개선 (대량 rounds 시 메모리 안정화)

#### infra/cache-lettuce

- **`ResilientLettuceSuspendNearCache.close()`**: 채널을 먼저 닫고 `consumerJob.join()`으로 write-behind 커맨드 소진 대기 후
  `scope.cancel()`하도록 종료 순서 개선 (커맨드 유실 방지)

#### infra/cache-redisson

- **`RedissonSuspendCache`**: 모든 `*Async()` 호출의 `awaitSuspending()` → `kotlinx.coroutines.future.await()`로 교체

#### testing/junit5

- **`untilSuspending`**: `coroutineScope` → `withContext(Dispatchers.IO)` 변경으로 hang 방지
- Awaitility 4.2+ 필드명 변경(`timeoutConstraint` → `waitConstraint`) 호환성 대응

#### testing/testcontainers

- **`NatsServer`**: NATS 이미지 태그 `2.10` → `2.12` 업그레이드
- **`LocalStackServerTest`**: custom network 테스트 `@Disabled` 처리 (Ryuk 레이스 컨디션, Docker Desktop macOS 이슈)

### Chores

- ConsulServer Docker 이미지 `hashicorp/consul 1.20`으로 업데이트

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
