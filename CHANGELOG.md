# Changelog

모든 주요 변경 사항은 이 파일에 기록됩니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 따르며, 이 프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [1.6.1] - 2026-04-16

### Added

#### testing/mock-server — `bluetape4k-mock-server` 신규 모듈 ([`a340e49b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/a340e49b4))

Spring Boot 4 + Java 25 + Virtual Threads 기반의 자체 내장 Mock HTTP 서버.
기존 외부 의존(`httpbin.org`, `jsonplaceholder.typicode.com`)을 컨테이너화된 로컬 서버로 대체합니다.

- **httpbin 시뮬레이터**: `GET /anything`, `POST /anything`, `/status/{code}`, `/delay/{n}`, `/headers`, `/ip`, `/uuid`, `/gzip`, `/stream/{n}`, `/image/{type}` 등 지원
- **jsonplaceholder 시뮬레이터**: posts / comments / albums / photos / todos / users CRUD API (인메모리 상태)
- **웹 컨텐츠 시뮬레이터**: naver / google / home / login / article HTML 페이지
- `/ping` → `"pong"` 반환 (Testcontainers wait strategy 호환)
- Jib 빌드: `arm64` / `amd64` 호스트 아키텍처 자동 감지
- Jackson 3 (`tools.jackson.*`) 사용 — Spring Boot 4 호환

#### testing/testcontainers — `BluetapeHttpServer` 추가, `HttpbinServer` 대체 ([`a340e49b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/a340e49b4))

- `BluetapeHttpServer`: `bluetape4k/mock-server` Docker 이미지를 기반으로 하는 Testcontainers 래퍼
- `httpbinUrl`, `jsonplaceholderUrl`, `pingUrl` 프로퍼티 제공
- 기존 `HttpbinServer`, `HttpbinHttp2Server` 및 관련 테스트 제거

### Changed

#### testing/mock-server — 전체 모듈 `BluetapeHttpServer` 마이그레이션 ([`22986785c`](https://github.com/bluetape4k/bluetape4k-projects/commit/22986785c))

io/feign, io/retrofit2, io/http, infra/micrometer, spring-boot3/4 등 외부 httpbin에 의존하던 테스트를 모두 `BluetapeHttpServer`로 전환하였습니다.

#### spring-boot3/4 cassandra — `ReactiveSession.executeSuspending` SimpleStatement 강제 경유 ([`6376544ae`](https://github.com/bluetape4k/bluetape4k-projects/commit/6376544ae))

Kotlin 오버로드 해석 문제(`execute(String, Map)` → `execute(String, Object...)` vararg로 잘못 dispatch)로 인해 Map 전체가 단일 BIGINT 값으로 직렬화되던 버그를 수정했습니다.

- `executeSuspending(query, vararg args)` → `execute(SimpleStatement.newInstance(query, *args)).awaitSingle()`
- `executeSuspending(query, Map)` → `execute(SimpleStatement.newInstance(query, args)).awaitSingle()`
- spring-boot3 / spring-boot4 cassandra 모듈 동시 적용

#### 빌드 — SB3 BOM 루트 제거, 각 모듈 명시적 선언으로 전환 ([`a340e49b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/a340e49b4))

루트 `build.gradle.kts`에서 Spring Boot 3 / Spring Cloud / Spring Integration BOM을 제거하고 SB3 모듈(29개)에서 `implementation(platform(Libs.spring_boot3_dependencies))`를 직접 선언합니다.
SB4 모듈이 SB3 BOM에 오염되던 문제를 해소합니다.

### Docs

#### 전체 README — Mermaid UML 다이어그램 스타일 가이드 적용 ([`5e5ae1963`](https://github.com/bluetape4k/bluetape4k-projects/commit/5e5ae1963))

모든 모듈 README의 Mermaid 다이어그램(classDiagram / sequenceDiagram / flowchart)에 색상 테마 및 레이아웃 가이드를 일관되게 적용했습니다.

---

## [1.6.0] - 2026-04-14

### Added

#### spring-boot3/batch-exposed — Spring Batch 5.x + Exposed JDBC Partitioned Step 모듈 추가 ([`853c140bc`](https://github.com/bluetape4k/bluetape4k-projects/commit/853c140bc))

- `Partitioned Step + VirtualThread Parallel Query` 전략의 배치 모듈 추가
- `ExposedRangePartitioner`: auto-increment PK를 ID 범위로 분할하는 파티셔너
- `ExposedKeysetItemReader`: keyset 페이징 + 재시작 지원 `ItemStreamReader`
- `ExposedItemWriter` / `ExposedUpdateItemWriter` / `ExposedUpsertItemWriter` 제공
- `virtualThreadPartitionTaskExecutor`, `partitionedBatchJob`, `ExposedBatchAutoConfiguration` 포함
- H2/PostgreSQL/MySQL 기반 테스트 및 파티션 벤치마크 추가

#### spring-boot4/batch-exposed — Spring Boot 4 + Exposed 통합 배치 모듈 추가 ([`a02e9201e`](https://github.com/bluetape4k/bluetape4k-projects/commit/a02e9201e))

- `ExposedKeysetItemReader`, `ExposedRangePartitioner`, `ExposedItemWriter` 계열을 Spring Boot 4 환경에 맞춰 제공
- `ExposedBatchAutoConfiguration` 및 배치 DSL 포함
- README/README.ko와 기본 통합 테스트 세트 함께 추가

#### utils/batch — Kotlin Coroutine 네이티브 경량 배치 프레임워크 모듈 추가 ([`0e9ae3096`](https://github.com/bluetape4k/bluetape4k-projects/commit/0e9ae3096))

- `No Spring`, `No runBlocking` 원칙의 코루틴 배치 실행 모델 제공
- `BatchReader` / `BatchProcessor` / `BatchWriter` chunk 파이프라인과 `BatchJob` / `BatchStep` / `BatchStepRunner` 코어 엔진 제공
- `BatchJobRepository` 기반 체크포인트 재시작, `SkipPolicy` / `RetryPolicy`, `batchJob {}` / `step {}` DSL 포함
- `BatchJob`의 `SuspendWork` 구현으로 `utils/workflow`와 직접 통합
- `ExposedJdbcBatchJobRepository` / `ExposedR2dbcBatchJobRepository` 및 JDBC/R2DBC Reader/Writer 제공

#### utils/rule-engine — Janino/Groovy 스크립트 엔진 추가 ([`833e31a39`](https://github.com/bluetape4k/bluetape4k-projects/commit/833e31a39))

기존 Kotlin Script / MVEL / SpEL 기반 규칙 실행기에 Janino와 Groovy 엔진 선택지를 추가했습니다.

- `io.bluetape4k.rule.engines.janino.*`: `ExpressionEvaluator` / `ScriptEvaluator` 기반 컴파일형 Java 표현식 엔진
- `io.bluetape4k.rule.engines.groovy.*`: `GroovyShell` + `NullSafeBinding` 기반 동적 스크립트 엔진
- 엔진별 `Condition` / `Action` / `Rule` / `Support` 타입 추가
- Janino/Groovy 예제·회귀 테스트 추가 및 README/README.ko 선택 가이드 보강

#### data/exposed-cache — 공통 캐시 인터페이스 모듈 추가 (구 `exposed-redis-api`) ([`d969a78e0`](https://github.com/bluetape4k/bluetape4k-projects/commit/d969a78e0))

`exposed-redis-api`를 `exposed-cache`로 리네이밍하고, DB + 캐시 조합의 공통 인터페이스를 정비한 모듈입니다.

**공통 캐시 인터페이스**
- `JdbcCacheRepository` / `SuspendedJdbcCacheRepository`: JDBC 기반 동기·코루틴 캐시 레포지토리
- `R2dbcCacheRepository`: R2DBC 기반 리액티브 캐시 레포지토리
- `JdbcRedisRepository` / `SuspendJdbcRedisRepository` / `R2dbcRedisRepository`: Redis 백엔드 전용 서브인터페이스 (`invalidateByPattern`)
- `LocalCacheConfig`: Caffeine 로컬 캐시 설정 data class (TTL, 최대 크기)
- `testFixtures`: Read/Write-Through/Behind 시나리오 공유 테스트 인프라

#### data/exposed-jdbc-caffeine — JDBC + Caffeine 로컬 캐시 모듈 추가 ([`d969a78e0`](https://github.com/bluetape4k/bluetape4k-projects/commit/d969a78e0))

- `JdbcCaffeineRepository`: JDBC + Caffeine 동기 로컬 캐시 (Read-Through / Write-Through / Write-Behind)
- `SuspendedJdbcCaffeineRepository`: 위와 동일, suspend 코루틴 버전
- Write-Behind: Channel 기반 비동기 쓰기 큐, `CoroutineScope` 생명주기 관리
- H2 인메모리 DB 기반 테스트 36개 (2 skipped — AutoInc Write-Behind)

#### data/exposed-r2dbc-caffeine — R2DBC + Caffeine AsyncCache 모듈 추가 ([`d969a78e0`](https://github.com/bluetape4k/bluetape4k-projects/commit/d969a78e0))

- `R2dbcCaffeineRepository`: Caffeine `AsyncCache` 기반 suspend 로컬 캐시 (R2DBC, runBlocking 없음)
- Read-Through / Write-Through / Write-Behind 전략 지원
- H2 인메모리 R2DBC 기반 테스트 18개 (1 skipped — AutoInc Write-Behind)

#### data/exposed-r2dbc — R2DBC 커넥션 풀 DSL 추가 ([`db91dd6af`](https://github.com/bluetape4k/bluetape4k-projects/commit/db91dd6af))

`io.bluetape4k.exposed.r2dbc.pool` 패키지에 커넥션 풀 구성을 위한 Kotlin DSL을 제공합니다.

- `R2dbcPoolConfig`: 커넥션 풀 설정 data class (스마트 기본값: CPU×8, 최소 100, TTL·타임아웃 등)
- `R2dbcConnectionConfig`: 커넥션 옵션 DSL 빌더 — 표준 r2dbc-spi 옵션 타입-안전 프로퍼티 + `option()` 드라이버 확장
- `connectionPoolOf(options) { }` / `connectionPoolOf(factory) { }` / `ConnectionFactoryOptions.toConnectionPool { }`
- `connectionFactoryOptionsOf { }` / `connectionFactoryOf { }` — DSL 람다 방식
- `connectionFactoryOptionsOf(url)` / `connectionFactoryOf(url)` — R2DBC URL 파싱 방식
- `r2dbcConnectionPool { connection { } pool { } }` — 연결·풀 설정 통합 DSL
- `r2dbcConnectionPool(url) { }` — URL + 풀 설정 간결 방식
- `R2dbcPoolConfig.toConnectionPoolConfiguration(factory)` 변환 유틸

### Changed

#### io/feign — 기본 구현 클래스와 상수 이름 정리 ([`11ec8881a`](https://github.com/bluetape4k/bluetape4k-projects/commit/11ec8881a))

- Feign 기본 구현 인스턴스 생성을 `Encoder.Default()` / `Decoder.Default()` / `Retryer.Default()`에서 `DefaultEncoder()` / `DefaultDecoder()` / `DefaultRetryer()`로 정리
- 잘못된 상수명 `JAVA_CLASS_VERION` → `JAVA_CLASS_VERSION`, `MILLIS_IN_DAY` → `MillisPerDay` 수정
- 관련 회귀 테스트와 오타성 테스트 코드 함께 정리

#### testing/testcontainers — Pulsar client API 및 의존성 버전 갱신 ([`75a1f84db`](https://github.com/bluetape4k/bluetape4k-projects/commit/75a1f84db))

- `testing/testcontainers`에 `pulsar-client-api` 의존성 추가
- Pulsar 3.3.9, Elasticsearch 9.3.3, CockroachDB 25.4.8로 관련 버전 업데이트
- `PulsarServer`, `ElasticsearchServer`, `CockroachServer` 연관 빌드 설정 동기화

#### spring-boot4/batch-exposed — Spring Batch 6.0 deprecated API 제거 ([`0c0bc8412`](https://github.com/bluetape4k/bluetape4k-projects/commit/0c0bc8412))

- `JobLauncherTestUtils` → `JobOperatorTestUtils`, `launchJob()` → `startJob()`로 테스트 유틸 정리
- `chunk(n, tm)` 호출을 `chunk(n).transactionManager(tm)` 패턴으로 전환
- Boot 3 대비 누락되었던 integration/benchmark 테스트 4종 추가

#### utils/idgenerators — kotlinx-benchmark 성능 측정 추가 ([`cb38152b0`](https://github.com/bluetape4k/bluetape4k-projects/commit/cb38152b0))

- `SingleThreadIdGeneratorBenchmark`, `ConcurrentIdGeneratorBenchmark` 추가
- `Benchmark.md`에 7개 ID 생성기 성능 비교와 컬러 바 차트 문서화

#### data/exposed-cache — 4개 Redis 캐시 모듈 인터페이스 통일 ([`b7311fccb`](https://github.com/bluetape4k/bluetape4k-projects/commit/b7311fccb), [`1385f8c41`](https://github.com/bluetape4k/bluetape4k-projects/commit/1385f8c41))

- `exposed-jdbc-lettuce` / `exposed-r2dbc-lettuce` / `exposed-jdbc-redisson` / `exposed-r2dbc-redisson` 4개 모듈 인터페이스 및 테스트 구조 표준화
- Lettuce fat 인터페이스 → `JdbcRedisRepository` / `R2dbcRedisRepository` 슬림 인터페이스로 분리
- `invalidateByPattern()`: Redis 전용 서브인터페이스로 분리 (공통 캐시 인터페이스에서 제거)
- `RedissonCacheConfig.name` 프로퍼티 추가

#### bluetape4k-core — `HasIdentifier` Deprecate ([`ac61e2864`](https://github.com/bluetape4k/bluetape4k-projects/commit/ac61e2864))

- `HasIdentifier` 인터페이스 `@Deprecated` 처리 — `java.io.Serializable` 직접 구현 권장
- 분산 캐시 직렬화는 `Serializable` + `serialVersionUID` 패턴으로 통일

#### bluetape4k-coroutines — `Deferred` 확장 함수 추가 ([`a52702e75`](https://github.com/bluetape4k/bluetape4k-projects/commit/a52702e75))

- `Deferred.zip(other)`: 두 `Deferred` 결과를 `Pair`로 결합
- `Deferred.zipWith(other, transform)`: 두 결과를 변환 함수로 합성

### Fixed

#### utils/workflow — suspend 워크플로 취소 전파 보강 ([`514b8c4d8`](https://github.com/bluetape4k/bluetape4k-projects/commit/514b8c4d8))

- `SuspendSequentialFlow`, `SuspendParallelFlow`, `SuspendRepeatFlow`, `SuspendRetryFlow`, `SuspendConditionalFlow`에서 `CancellationException` 전파를 일관되게 보장
- 실행 모델 비교 benchmark 및 README 설명 보강

#### utils/states — 종료 상태 조회 일관성 수정 ([`6a66dc0e7`](https://github.com/bluetape4k/bluetape4k-projects/commit/6a66dc0e7))

- 종료 상태에서도 `canTransition()` / `allowedEvents()`가 전이 가능해 보이던 불일치 수정
- 동기/코루틴 상태 머신 회귀 테스트 추가

#### utils/rule-engine — suspend 규칙 엔진 취소 전파 보강 ([`be865e94b`](https://github.com/bluetape4k/bluetape4k-projects/commit/be865e94b))

- `DefaultSuspendRuleEngine`의 `fire()` / `check()` 경로에서 코루틴 취소를 정상 전파하도록 수정
- 회귀 테스트 및 README 설명 갱신

#### bluetape4k-coroutines — `Flow.log()` / `AsyncFlow.log()` 로그 미출력 버그 수정 ([`f56b00a27`](https://github.com/bluetape4k/bluetape4k-projects/commit/f56b00a27), [`8c9a96681`](https://github.com/bluetape4k/bluetape4k-projects/commit/8c9a96681))

- `Flow.log()` 연산자가 실제 로그를 출력하지 않던 문제 수정
- `AsyncFlow.log()` 동일 버그 수정

#### data/exposed-cache — R2DBC Write-Behind 타이밍·UNIQUE 제약 위반 수정 ([`86fa30e83`](https://github.com/bluetape4k/bluetape4k-projects/commit/86fa30e83))

- Write-Behind 비동기 큐 플러시 타이밍 경쟁 조건 수정
- UUID 기반 테스트 테이블 추가로 AutoInc UNIQUE 제약 위반 방지

#### infra/redisson — `RedissonCacheConfig` bluetape4k-patterns 위반 수정 ([`4cffbcc2b`](https://github.com/bluetape4k/bluetape4k-projects/commit/4cffbcc2b), [`5d6da26af`](https://github.com/bluetape4k/bluetape4k-projects/commit/5d6da26af))

- stdlib `require()` → `requirePositiveNumber` / `requireGe` 등 bluetape4k 확장함수로 교체
- `validateUnsupportedMapSettings` 내부 상태 검증은 `check()` 유지 (패턴 원칙에 맞게 복원)

---

## [1.5.0] - 2026-04-05

### Added

#### utils/workflow — Kotlin DSL 워크플로 모듈 추가 ([`685e25a4d`](https://github.com/bluetape4k/bluetape4k-projects/commit/685e25a4d))

j-easy/easy-flows에서 영감을 받아 Kotlin 2.3 + 코루틴 + Virtual Threads로 완전 재작성한 워크플로 엔진입니다.

**API**
- `WorkStatus`: `COMPLETED` / `FAILED` / `PARTIAL` / `CANCELLED` / `ABORTED` (5종)
- `WorkReport` sealed interface: `Success` / `Failure` / `PartialSuccess` / `Cancelled` / `Aborted`
- `WorkContext`: `ConcurrentHashMap` 기반, `compute()` 원자적 read-modify-write 지원
- `RetryPolicy`: 지수 백오프, `maxAttempts`(총 시도 횟수), `maxRetries` 편의 프로퍼티
- `ParallelPolicy`: `ALL`(ShutdownOnFailure) / `ANY`(ShutdownOnSuccess)
- `ErrorStrategy`: `STOP`(return) / `CONTINUE`(continue) — `ABORTED`는 ErrorStrategy 무관 break

**동기 플로우 5종 (Virtual Threads)**
- `SequentialWorkFlow`: 순차 실행, `CONTINUE` 전략 시 `PartialSuccess` 반환
- `ParallelWorkFlow`: `StructuredTaskScopes` 기반, `joinUntil(deadline)` 타임아웃, ALL/ANY 정책
- `ConditionalWorkFlow`: predicate 기반 then/otherwise 분기
- `RepeatWorkFlow`: `repeatWhile` / `until` 조건 + `maxIterations` 상한
- `RetryWorkFlow`: 지수 백오프 재시도

**코루틴 플로우 5종 (suspend)**
- `SuspendSequentialFlow` / `SuspendParallelFlow` / `SuspendConditionalFlow` / `SuspendRepeatFlow` / `SuspendRetryFlow`
- `SuspendParallelFlow` ANY 정책: Channel 기반 첫 성공 즉시 반환
- `workReportFlow()` / `executeAsFlow()`: Flow 스트리밍 지원

**DSL**
- `sequentialFlow {}` / `parallelFlow {}` / `conditionalFlow {}` / `repeatFlow {}` / `retryFlow {}`
- `suspendSequentialFlow {}` 등 코루틴 변형 전체 제공
- `parallelAllFlow {}` / `parallelAnyFlow {}` + nested `parallelAll` / `parallelAny`
- 단일 루트 강제: `require(rootWork == null)`

**테스트**: 173개 전체 통과, 주문처리 실무 예제 2개 (동기/코루틴)

#### utils/states — 코루틴 기반 유한 상태 머신(FSM) 모듈 추가 ([`3e9be7d25`](https://github.com/bluetape4k/bluetape4k-projects/commit/3e9be7d25))

- `BaseStateMachine` / `StateMachine` / `SuspendStateMachineInterface` 3계층 인터페이스 (시그니처 충돌 방지)
- `DefaultStateMachine`: `AtomicReference` CAS 기반 Thread-safe 동기 FSM
- `SuspendStateMachine`: `Mutex` + `MutableStateFlow` 기반 코루틴 FSM + 상태 관찰
- `stateMachine {}` / `suspendStateMachine {}` DSL + `on<E>()` 헬퍼 + Guard 조건 지원
- `TransitionBuilder`: Guard 조건 람다 DSL
- clinic-appointment Map 기반 O(1) 전이 패턴 채택
- 테스트 35개 (Turnstile, Order, Appointment, GuardedTransition)

#### utils/rule-engine — Kotlin DSL 기반 규칙 엔진 모듈 추가 ([`69de83742`](https://github.com/bluetape4k/bluetape4k-projects/commit/69de83742))

- `Rule` / `SuspendRule` / `Facts`(`ConcurrentHashMap`) / `RuleEngine` / `SuspendRuleEngine` 핵심 인터페이스
- `rule {}` / `suspendRule {}` DSL + `@Rule` / `@Condition` / `@Action` 어노테이션 + `RuleProxy` 변환
- `CompositeRule`: `ActivationRuleGroup` / `ConditionalRuleGroup` / `UnitRuleGroup`
- `InferenceRuleEngine`: 전방 추론(Forward Chaining) 지원
- Expression Engine: MVEL2 / SpEL / Kotlin Script (`BasicJvmScriptingHost`, 컴파일 캐시)
- Rule Reader: YAML / JSON / HOCON 파일에서 규칙 로드
- 테스트 72개 통과

#### testing/testcontainers — 신규 서버 8종 추가 ([`3b0e5af8`](https://github.com/bluetape4k/bluetape4k-projects/commit/3b0e5af8))

- `Neo4jServer`: Neo4j 그래프 DB, Bolt/HTTP 포트, `bolt-url` 프로퍼티 export
- `MemgraphServer`: Memgraph 그래프 DB, `bolt-port`/`log-port`/`bolt-url` export
- `PostgreSQLAgeServer`: PostgreSQL + Apache AGE 그래프 확장
- `ToxiproxyServer`: 카오스 테스트용 네트워크 프록시, `control-port`/`control-url` export, latency·bandwidth toxic 주입 테스트 구현
- `TrinoServer`: 분산 SQL 쿼리 엔진
- `WireMockServer`: HTTP stub/mock 서버, stale 커넥션 자동 재시도 (`resetAll`)
- `KeycloakServer`: Keycloak 인증 서버, `auth-url`/`admin-username`/`admin-password` export
- `InfluxDBServer`: InfluxDB 2.x 시계열 DB, `admin-token`/`organization`/`bucket` export

#### testing/testcontainers — `PropertyExportingServer` 계약 강화 ([`cc0d7204`](https://github.com/bluetape4k/bluetape4k-projects/commit/cc0d7204))

- `PropertyExportingServer` 인터페이스: `propertyKeys()` / `properties()` / `registerSystemProperties()` / `writeToSystemProperties()` 통일
- 프로퍼티 키 명명 규칙을 **kebab-case 소문자**로 통일 (`bootstrapServers` → `bootstrap-servers`, `bolt.url` → `bolt-url` 등)
- `withCompatKeys()`: kebab-case 키 추가 시 구 camelCase 키도 병행 등록 (하위 호환)

#### data/exposed-trino — Trino JDBC Dialect 모듈 추가 ([`28dab07f`](https://github.com/bluetape4k/bluetape4k-projects/commit/28dab07f), [`7816a3ca`](https://github.com/bluetape4k/bluetape4k-projects/commit/7816a3ca))

- `TrinoDatabase`: Trino JDBC 연결 팩토리 (`jdbc:trino://`)
- `suspendTransaction` / `queryFlow`: 코루틴 기반 Trino 쿼리 API
- autocommit 전용 (Trino는 트랜잭션 미지원)
- `testcontainers`의 `TrinoServer`와 연동 테스트 포함

### Changed

#### data/exposed-trino, exposed-bigquery, exposed-duckdb — Codex 개선 적용 ([`4ce6750b`](https://github.com/bluetape4k/bluetape4k-projects/commit/4ce6750b), [`c50998bf`](https://github.com/bluetape4k/bluetape4k-projects/commit/c50998bf))

- API 일관성·KDoc·테스트 코드 정리

### Fixed

- `virtualthread/api` — `StructuredTaskScopeAll.joinUntil(Instant)` 메서드 추가, `Jdk21AllScope` 구현: `ParallelWorkFlow` 타임아웃이 실제로 동작하지 않던 문제 수정 ([`685e25a4d`](https://github.com/bluetape4k/bluetape4k-projects/commit/685e25a4d))
- `virtualthread/jdk25` — `Jdk25AllScope.joinUntil(Instant)` 구현: JDK 25 `StructuredTaskScope`에 `joinUntil` 없음 → 스케줄러로 스레드 인터럽트 후 `TimeoutException` 변환
- `WireMockServer.resetAll()`: Apache HttpClient 5 stale 커넥션으로 인한 `NoHttpResponseException` 발생 시 클라이언트 재생성 후 1회 재시도 ([`c4adae7d`](https://github.com/bluetape4k/bluetape4k-projects/commit/c4adae7d))
- `ZooKeeperServer`: Curator 연결 타임아웃 안정화 — `RetryOneTime(1000)` + `blockUntilConnected(10s)` 추가 (IPv6→IPv4 폴백 대응) ([`0d05542d`](https://github.com/bluetape4k/bluetape4k-projects/commit/0d05542d))
- `ToxiproxyServer`: `useDefaultPort` 시 `exposeCustomPorts()` 누락 수정, KDoc 프로퍼티 키 `control.port` → `control-port` ([`a46226b8`](https://github.com/bluetape4k/bluetape4k-projects/commit/a46226b8))

---

## [1.5.0-RC1] - 2026-04-01

### Added

#### utils/science — GIS 공간 데이터 처리 모듈 신규 추가 ([`a32d243b`](https://github.com/bluetape4k/bluetape4k-projects/commit/a32d243b))

debop4k-science를 Kotlin 2.3 + 최신 라이브러리로 완전 재작성한 `bluetape4k-science` 모듈입니다.

**coords 패키지 — 좌표계 및 변환**
- `GeoLocation(latitude, longitude)`: Haversine 거리 계산 (`distanceTo`)
- `BoundingBox`: 경계 박스 + `relationTo(other)` (DISJOINT/INTERSECTS/CONTAINS/WITHIN), JTS `Envelope` 변환
- `DM` / `DMS`: 도분/도분초 data class + `CoordConverters` (Degree ↔ DM ↔ DMS 왕복 변환)
- `UtmZoneSupport`: UTM Zone/Band 결정(`utmZoneOf`), 위도 밴드 `I·O` 제외 로직, `UtmZone.boundingBox()`

**geometry 패키지 — JTS 기반 공간 연산**
- `GeometryOperations`: 두 점 간 각도·거리, 선분 교차점, 위경도 유효성 검증
- `PolygonExtensions`: JTS `Polygon` 넓이(㎡), 무게중심, `BoundingBox` 변환

**projection 패키지 — Proj4J 기반 좌표계 변환**
- `Projections`: `utmToWgs84()`, `wgs84ToUtm()`, `transform(sourceCrs, targetCrs, coord)` — 임의 EPSG CRS 변환
- `CrsRegistry` (internal): EPSG 코드 기반 CRS 캐시 (`ConcurrentHashMap`)

**shapefile 패키지 — GeoTools 31.6 기반 Shapefile 처리**
- `ShapefileReader`: `loadShape(file)` (동기), `loadShapeAsync(file)` (`withContext(Dispatchers.IO)` 래핑)
- `ShapeModels`: `ShapeHeader`, `ShapeAttribute`, `ShapeRecord`, `Shape` — GeoTools 타입을 public API에 노출하지 않음
- `ShapefileExtensions`: `toGeoLocations()`, `filterByBoundingBox()`, `filterByAttribute()`, `computeBoundingBox()`

**exposed 패키지 — PostGIS DB 적재 파이프라인**
- `SpatialLayerTable` / `SpatialFeatureTable`: `AuditableLongIdTable` 상속, `geoGeometry()` + `jacksonb<Map<String,Any?>>()` JSONB
- `PoiTable`: POI 지점 저장 (`geoPoint()` + `jacksonb<Map<String,Any?>>()`)
- `NetCdfFileTable` / `NetCdfGridValueTable`: 테이블 DDL (UCAR 구현은 Phase 4 보류)
- `SpatialLayerRepository` / `SpatialFeatureRepository`: `LongJdbcRepository` 기반 CRUD
- `ShapefileImportService.importShapefile()`: Shapefile → PostGIS 배치 적재 (1,000건/트랜잭션, `ensureActive()`, JTS→WKT→PostGIS 변환)
- `NetCdfFileRepository` / `NetCdfCatalogService`: 구조 정의 (NetCDF 읽기는 Phase 4 후 구현)

> **[!NOTE]**
> GeoTools는 **LGPL** 라이선스로 `compileOnly`로만 선언됩니다. 빌드 스크립트에 OsGeo Maven 저장소 추가 필요:
> ```kotlin
> maven("https://repo.osgeo.org/repository/release/")
> ```

#### data/exposed-postgresql — GeoGeometryColumnType 추가

- `GeoGeometryColumnType`: 모든 PostGIS geometry 타입(Point/Polygon/LineString/MultiPolygon 등)을 수용하는 generic 컬럼 타입 ([`a32d243b`](https://github.com/bluetape4k/bluetape4k-projects/commit/a32d243b))
- `Table.geoGeometry(name)` 확장함수
- `ST_Distance(geography)`, `ST_DWithin(geography)`, `ST_Intersects`, `ST_Contains`, `ST_Within` Expression 클래스 추가

### Changed

#### aws-kotlin — 클라이언트 생성/해제 패턴 통일 ([`af247f65`](https://github.com/bluetape4k/bluetape4k-projects/commit/af247f65))

- 모든 서비스에 `xxxClientOf` + `withXxxClient` 팩토리 함수 쌍을 `*Support.kt`로 분리
  - 신규 파일: `KinesisClientSupport`, `SesClientSupport`, `SesV2ClientSupport`, `SnsClientSupport`, `SqsClientSupport`
- `withXxxClient`를 `xxxClientOf(...).useSafe { }` 패턴으로 통일 — 코루틴 취소·예외 시 자동 `close()` 보장
- `httpClient` 기본값을 `HttpClientEngineProvider.defaultHttpEngine`(CRT)으로 전체 통일
- `*Extensions.kt`에서 팩토리/with 함수 제거 — 확장 함수만 유지

> **[!NOTE]**
> AWS Kotlin SDK 클라이언트는 내부 HTTP 커넥션 풀·스레드를 보유합니다.
> 사용 후 반드시 `close()`를 호출하거나, **`withXxxClient { }` 블록을 사용하면 자동으로 리소스가 해제**됩니다.

### Fixed

- mutiny 테스트 병렬 실행 비활성화 ([`e0082a82`](https://github.com/bluetape4k/bluetape4k-projects/commit/e0082a82))
- JUnit Jupiter 병렬 실행 비활성화 ([`70cd469e`](https://github.com/bluetape4k/bluetape4k-projects/commit/70cd469e))

---

## [1.5.0-Beta3] - 2026-03-31

### Added

#### spring-boot3/4 — Exposed Spring Data JDBC/R2DBC Repository 이관

- **`bluetape4k-spring-boot3-exposed-jdbc`** experimental → projects 이관 ([`08c6711a`](https://github.com/bluetape4k/bluetape4k-projects/commit/08c6711a))
  - `ExposedJdbcRepository<E, ID>`: PartTree 쿼리 자동 생성, QBE(Query By Example), Pageable/Sort 지원
  - `SimpleExposedJdbcRepository`: `findAll`, `findById`, `save`, `saveAll`, `deleteById`, `count`, `exists`
  - `ExposedQueryCreator`: 메서드명 기반 WHERE 절 자동 생성 (`findByName`, `findByPriceLessThan` 등)
  - `@EnableExposedJdbcRepositories`: Auto-Configuration 진입점
- **`bluetape4k-spring-boot4-exposed-jdbc`**: Spring Boot 4 BOM(`platform()`) 적용 동일 기능 제공

- **`bluetape4k-spring-boot3-exposed-r2dbc`** experimental → projects 이관 ([`08c6711a`](https://github.com/bluetape4k/bluetape4k-projects/commit/08c6711a))
  - `ExposedR2dbcRepository<T, ID>`: suspend CRUD (`findAll`, `findByIdOrNull`, `save`, `deleteById`), Flow 지원
  - `SimpleExposedR2dbcRepository`: `toDomain`, `toPersistValues`, `extractId` 오버라이드 패턴
  - `@EnableExposedR2dbcRepositories`: Auto-Configuration 진입점
- **`bluetape4k-spring-boot4-exposed-r2dbc`**: Spring Boot 4 BOM 적용 동일 기능 제공

#### spring-boot3/4 — Exposed Spring Data 데모 앱 추가

- **`bluetape4k-spring-boot3-exposed-jdbc-demo`** / **`bluetape4k-spring-boot4-exposed-jdbc-demo`** ([`3b3b2729`](https://github.com/bluetape4k/bluetape4k-projects/commit/3b3b2729))
  - Exposed DAO + Spring Data JDBC + Spring MVC CRUD 데모 (H2 in-memory, 검색 API 포함)
- **`bluetape4k-spring-boot3-exposed-r2dbc-demo`** / **`bluetape4k-spring-boot4-exposed-r2dbc-demo`** ([`3b3b2729`](https://github.com/bluetape4k/bluetape4k-projects/commit/3b3b2729))
  - Exposed R2DBC + suspend Repository + Spring WebFlux CRUD 데모 (H2 R2DBC in-memory)

#### spring-boot3/4 — Hibernate Lettuce NearCache Auto-Configuration 이관

- **`bluetape4k-spring-boot3-hibernate-lettuce`** experimental → projects 이관 ([`1f0eae55`](https://github.com/bluetape4k/bluetape4k-projects/commit/1f0eae55))
  - `LettuceNearCacheHibernateAutoConfiguration`: YAML 프로퍼티 → `HibernatePropertiesCustomizer` 자동 변환
  - `LettuceNearCacheSpringProperties`: `bluetape4k.cache.lettuce-near.*` 바인딩 — codec, useResp3, local(Caffeine), redisTtl(region별 오버라이드)
  - `LettuceNearCacheMetricsAutoConfiguration`: Micrometer Gauge — `lettuce.nearcache.active.regions`, `lettuce.nearcache.total.local.size`
  - `LettuceNearCacheActuatorAutoConfiguration`: `GET /actuator/nearcache`, `GET /actuator/nearcache/{region}` 엔드포인트
  - 13개 테스트 케이스 전체 통과 (ApplicationContextRunner 단위 + Testcontainers Redis 통합)
- **`bluetape4k-spring-boot4-hibernate-lettuce`**: Spring Boot 4 BOM 적용 (HibernatePropertiesCustomizer 패키지 `org.springframework.boot.hibernate.autoconfigure`로 변경) ([`93d9d10e`](https://github.com/bluetape4k/bluetape4k-projects/commit/93d9d10e))

#### spring-boot3/4 — Hibernate Lettuce NearCache 데모 앱 추가

- **`bluetape4k-spring-boot3-hibernate-lettuce-demo`** / **`bluetape4k-spring-boot4-hibernate-lettuce-demo`** ([`0c85c9e5`](https://github.com/bluetape4k/bluetape4k-projects/commit/0c85c9e5), [`1ebdaa3c`](https://github.com/bluetape4k/bluetape4k-projects/commit/1ebdaa3c))
  - `Product` JPA 엔티티 (`@Cacheable`, `@Cache(NONSTRICT_READ_WRITE)`) + Spring Data JPA
  - `ProductController`: CRUD REST API (`/api/products`)
  - `CacheController`: L1 캐시 통계 조회/evict API (`/api/cache/stats`, `/api/cache/evict`)
  - 6개 통합 테스트 (Testcontainers Redis + H2)

#### data/hibernate-cache-lettuce — Hibernate 2nd Level Cache + Lettuce NearCache 신규 추가

- **`bluetape4k-hibernate-cache-lettuce`** experimental → projects 이관 ([`de7cf96d`](https://github.com/bluetape4k/bluetape4k-projects/commit/de7cf96d))
  - `LettuceNearCacheRegionFactory`: `RegionFactoryTemplate` 상속, Redis 클라이언트/코덱 초기화, region별 `StorageAccess` 생성
  - `LettuceNearCacheStorageAccess`: `DomainDataStorageAccess` 구현, Caffeine(L1) + Redis(L2) 2-tier 캐시 브릿지, 복합키/NaturalId 키 정규화
  - `LettuceNearCacheProperties`: Hibernate properties 파싱, 15가지 코덱 지원(Fory/Kryo × 5가지 압축), region별 TTL 오버라이드
  - 14개 테스트 클래스, 58개 테스트 케이스 전체 통과 (Testcontainers Redis + H2)

#### data/exposed-bigquery — Google BigQuery REST API 통합 신규 추가

- **`bluetape4k-exposed-bigquery`** experimental → projects 이관 ([`d7acd494`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7acd494))
  - `BigQueryContext`: Exposed DSL → H2(PostgreSQL 모드) SQL 생성 후 BigQuery REST API 실행. SELECT/INSERT/UPDATE/DELETE/DDL 지원
  - `BigQueryQueryExecutor`: `toList()`, `toListSuspending()`, `toFlow()`, 페이지네이션 자동 처리
  - `BigQueryResultRow`: Column 참조 기반 타입 안전 행 접근 (Long, BigDecimal, Instant 등)
  - `BigQueryDialect`: `PostgreSQLDialect` 상속 BigQuery 전용 다이얼렉트
  - `BigQueryEmulator`: 로컬 에뮬레이터(포트 9050) 자동 감지, 없으면 Testcontainers 자동 기동

#### data/exposed-duckdb — DuckDB JDBC 통합 신규 추가

- **`bluetape4k-exposed-duckdb`** experimental → projects 이관 ([`d7acd494`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7acd494))
  - `DuckDBDialect`: `PostgreSQLDialect` 상속 DuckDB 전용 다이얼렉트 + `DuckDBDialectMetadata` (FK 제약 캐싱 no-op)
  - `DuckDBDatabase`: 인메모리/파일/읽기전용 연결 팩토리 (`object`)
  - `DuckDBConnectionWrapper`: JDBC 1.1.3 `prepareStatement` 오버로드 호환 래퍼
  - `suspendTransaction`: `Dispatchers.IO` 기반 suspend 트랜잭션 확장 함수
  - `queryFlow`: 대용량 결과셋 `Flow<T>` 스트리밍 확장 함수

#### data/exposed-postgresql — PostgreSQL 전용 Exposed 확장 신규 추가

- **`bluetape4k-exposed-postgresql`** 신규 추가 ([`06c5087f`](https://github.com/bluetape4k/bluetape4k-projects/commit/06c5087f))
  - PostGIS 공간 데이터 컬럼 타입 — `POINT`, `POLYGON` (H2 fallback 지원)
  - pgvector 벡터 검색 컬럼 타입 — `VECTOR(n)`, 유사도 검색 (`<->`, `<#>`, `<=>`)
  - TSTZRANGE 시간 범위 컬럼 타입

#### data/exposed-mysql8 — MySQL 8.0 GIS 전용 Exposed 확장 신규 추가

- **`bluetape4k-exposed-mysql8`** experimental → projects 이관 ([`5a9e32d7`](https://github.com/bluetape4k/bluetape4k-projects/commit/5a9e32d7))
  - GIS 공간 데이터 컬럼 타입 8종: `POINT`, `LINESTRING`, `POLYGON`, `MULTIPOINT`, `MULTILINESTRING`, `MULTIPOLYGON`, `GEOMETRYCOLLECTION`, `GEOMETRY`
  - JTS(Java Topology Suite) 기반 `Geometry` 컬럼 타입
  - 공간 함수: `ST_Contains`, `ST_Distance`, `ST_AsText`, `ST_GeomFromText` 등
  - MySQL Internal Format WKB 자동 변환

#### data/exposed-core — inet/phone 컬럼 타입 통합

- **`exposed-inet`, `exposed-phone`** 모듈을 `exposed-core`로 이관 ([`dd520942`](https://github.com/bluetape4k/bluetape4k-projects/commit/dd520942))
  - `inetAddress`, `cidr` 컬럼 타입 + PostgreSQL `<<` 네트워크 포함 연산자
  - `phoneNumber`, `phoneNumberString` 컬럼 타입 (libphonenumber opt-in)
  - 기존 별도 모듈 의존성 → `bluetape4k-exposed-core`로 통합

#### infra/lettuce — 확률적 자료구조 추가

- **BloomFilter / CuckooFilter**: Redis Lua 스크립트 기반 구현 (RedisBloom 서버 확장 불필요) ([`4a3d0fb9`](https://github.com/bluetape4k/bluetape4k-projects/commit/4a3d0fb9))
  - `BloomFilter<E>`: `add`, `mightContain`, `clear` — 지정 오류율·최대 항목 수 기반 비트배열 크기 자동 계산
  - `CuckooFilter<E>`: `add`, `mightContain`, `delete` 지원 — BloomFilter 대비 삭제 연산 지원
- **HyperLogLog**: PFADD/PFCOUNT/PFMERGE 래핑 ([`4a3d0fb9`](https://github.com/bluetape4k/bluetape4k-projects/commit/4a3d0fb9))
  - `HyperLogLog<E>`: `add`, `count`, `merge` — 대용량 카디널리티 근사 계산 (표준오차 ≈ 0.81%)

#### data/exposed — Auditable 감사 추적 패턴 추가

- **`exposed-core`**: `Auditable` 인터페이스 + `UserContext` (ScopedValue/ThreadLocal 듀얼 전략) + `AuditableIdTable` 베이스 테이블 추가 ([`207bcbca`](https://github.com/bluetape4k/bluetape4k-projects/commit/207bcbca))
- **`exposed-dao`**: `AuditableEntity` (`flush()` 오버라이드로 createdBy/updatedBy 자동 설정) + `AuditableEntityClass` DAO 추가 ([`207bcbca`](https://github.com/bluetape4k/bluetape4k-projects/commit/207bcbca))
- **`exposed-jdbc`**: `AuditableJdbcRepository` (`auditedUpdateById`/`auditedUpdateAll` — updatedAt/updatedBy DB CURRENT_TIMESTAMP 자동 설정) 추가 ([`207bcbca`](https://github.com/bluetape4k/bluetape4k-projects/commit/207bcbca))
- **`exposed-core/dao`**: ULID 커스텀 ID 지원 추가 (`UlidIdTable`, `UlidEntity`) ([`cd345b11`](https://github.com/bluetape4k/bluetape4k-projects/commit/cd345b11))

#### data/hibernate — Hibernate 6.6 NaturalId 확장

- **`Session`/`EntityManager`** 용 `bySimpleNaturalId`, 복합 NaturalId helper 추가 ([`5e7e7f00`](https://github.com/bluetape4k/bluetape4k-projects/commit/5e7e7f00))
- `ConcreteProxy`, embeddable inheritance 매핑 회귀 테스트 추가

#### io/tink — Redis Key Rotation 지원

- Versioned Keyset + Lettuce/Redisson 기반 Redis 키셋 저장소 추가 ([`02fe8621`](https://github.com/bluetape4k/bluetape4k-projects/commit/02fe8621))
- `TinkJsonProtoKeysetFormat` 기반 키셋 직렬화로 deprecated API 제거

#### io/http — MockWebServer 헤더 지연 헬퍼 추가

- **`enqueueBodyWithHeadersDelay`** 확장 함수 추가 — headers delay 기반 취소 테스트 안정화 ([`2a15d515`](https://github.com/bluetape4k/bluetape4k-projects/commit/2a15d515))

#### utils/idgenerators — Uuid 생성기 교체

- 테스트 코드 전반의 `TimebasedUuid` → `Uuid` 클래스 교체 완료 ([`db55831f`](https://github.com/bluetape4k/bluetape4k-projects/commit/db55831f))
  - `Uuid.V7` (EpochTimebased), `Uuid.V1` (DefaultTimebased), `Uuid.V6` (Reordered), `Uuid.V4` (Random), `Uuid.V5` (Namebased)

### Fixed

#### core/coroutines

- **`startCollectOn`**: upstream `launch` 의 `CancellationException` 을 error 로 변환하지 않도록 수정 ([`03c396e6`](https://github.com/bluetape4k/bluetape4k-projects/commit/03c396e6))
- **`DeferredValue.value`** / **`SuspendRingBuffer.iterator`** deprecated 처리 — blocking 계약 명확화 ([`712d7cbf`](https://github.com/bluetape4k/bluetape4k-projects/commit/712d7cbf))
- **`PublishSubject.emitError(null)`** 종료 계약 정리 ([`712d7cbf`](https://github.com/bluetape4k/bluetape4k-projects/commit/712d7cbf))
- **`firstCompleted`**: 첫 완료 기준으로 의미를 정렬하고 `firstSucceeded` 분리 ([`b9c55f5d`](https://github.com/bluetape4k/bluetape4k-projects/commit/b9c55f5d))

#### io/jackson

- **`JsonNode` 변환 로직**: `stringNode` 호출 수정 + `treeToValueOrNull()` `TreeNode` 캐스팅 명시 ([`97a8280e`](https://github.com/bluetape4k/bluetape4k-projects/commit/97a8280e))

#### io/retrofit2

- **Retry Call 재사용 버그**: 매 시도마다 `clone()` 된 Call 사용하도록 수정 ([`cbe6eeca`](https://github.com/bluetape4k/bluetape4k-projects/commit/cbe6eeca))

#### infra/resilience4j

- **Coroutine 예외 계약**: `CompletionStage recover` null cause 및 동기 예외 복구 정리, `CancellationException` 전파 보장 ([`d2b32b60`](https://github.com/bluetape4k/bluetape4k-projects/commit/d2b32b60))

#### infra/bucket4j

- **`RateLimitResult`**: 음수 값 불변식 추가 + error 결과 진단 메시지 보존 강화 ([`62b3d39d`](https://github.com/bluetape4k/bluetape4k-projects/commit/62b3d39d))

#### infra/kafka

- **`SuspendKafkaConsumerTemplate`**: subscribe/assign/commit/seek 관리 기능 추가, 종료 시 `CoroutineScope` 취소 보장 ([`aedc6e44`](https://github.com/bluetape4k/bluetape4k-projects/commit/aedc6e44))

#### infra/cache

- **`NearCacheResilienceConfig`** / **`HazelcastNearCacheConfig`**: 입력 제약 추가 ([`188163b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/188163b4))
- **`RedissonNearCacheConfig`**: TTL/idle 입력 제약 추가 ([`79db098f`](https://github.com/bluetape4k/bluetape4k-projects/commit/79db098f))
- **`LettuceCacheConfig`**: 생성 시점 입력 제약 검증 추가 ([`d4cbd1dd`](https://github.com/bluetape4k/bluetape4k-projects/commit/d4cbd1dd))

#### data/hibernate-cache-lettuce

- **`LettuceNearCacheProperties`**: `Serializable` 처리 + 미지원 codec 즉시 검증 ([`3a061b72`](https://github.com/bluetape4k/bluetape4k-projects/commit/3a061b72))

#### data/exposed

- **`exposed-r2dbc`**: `ON CONFLICT DO NOTHING` PostgreSQL 표준 SQL로 일반화 ([`b9c0b838`](https://github.com/bluetape4k/bluetape4k-projects/commit/b9c0b838))
- **`exposed-r2dbc`**: 페이징 계약 강화 ([`e53024c1`](https://github.com/bluetape4k/bluetape4k-projects/commit/e53024c1))
- **`exposed-jdbc`**: 배치 조회 시 쿼리 조건 보존 수정 ([`b0a73d13`](https://github.com/bluetape4k/bluetape4k-projects/commit/b0a73d13))
- **`exposed lettuce loader/writer`**: `loadAllKeys` PK 오름차순 고정 + `chunkSize` 입력 검증 추가 ([`5fb405e0`](https://github.com/bluetape4k/bluetape4k-projects/commit/5fb405e0))
- **`exposed-postgresql`**: TSTZRANGE fractional seconds 파싱 수정 ([`d7607df2`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7607df2))
- **`exposed-mysql8`**: Geometry literal 경로를 표준 WKB 기반으로 수정 ([`d7607df2`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7607df2))

#### utils/geo

- **`GeoHashCircleQuery`**: 음수 반경 즉시 거부 검증 추가 ([`a4d555cc`](https://github.com/bluetape4k/bluetape4k-projects/commit/a4d555cc))

#### utils/idgenerators

- **UUID 시퀀스 API**: Base62 시퀀스 `size` 검증 일관성 정렬 ([`17f509b8`](https://github.com/bluetape4k/bluetape4k-projects/commit/17f509b8))

### Changed

#### 의존성 버전 업데이트

- `vertx`: 4.5.25 → 4.5.26
- `aws2`: 2.42.15 → 2.42.23
- `aws2_crt`: 0.43.8 → 0.44.0
- `aws_kotlin`: 1.6.18 → 1.6.46
- `aws_smithy_kotlin`: 1.6.2 → 1.6.7
([`42600939`](https://github.com/bluetape4k/bluetape4k-projects/commit/42600939))

#### infra/redisson

- **`RedissonCodecs`** deprecated 객체 제거 ([`52d84d11`](https://github.com/bluetape4k/bluetape4k-projects/commit/52d84d11))

---

## [1.5.0-Beta2] - 2026-03-21

### Added

#### utils/idgenerators — ULID 통합

- **`bluetape4k-ulid` 실험 모듈 → `bluetape4k-idgenerators`에 통합** ([`a40a3392`](https://github.com/bluetape4k/bluetape4k-projects/commit/a40a3392))
  - `ULID` interface + `ULIDFactory` / `ULIDMonotonic` / `ULIDStatefulMonotonic` 구현체 마이그레이션 (`io.bluetape4k.ulid` → `io.bluetape4k.idgenerators.ulid`)
  - `UlidGenerator`: `ULID.StatefulMonotonic` 기반 `IdGenerator<String>` 어댑터 추가
  - `JavaUUIDSupport` / `KotlinUuidSupport` 확장 함수 포함
  - 동시성 테스트 추가: `MultithreadingTester`, `StructuredTaskScopeTester`, `SuspendedJobTester`

#### utils/idgenerators — Ksuid/Snowflake 어댑터

- **`KsuidGenerator`**: `Ksuid.Generator` 전략을 주입받는 `IdGenerator<String>` 어댑터 추가 ([`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340))
- **`SnowflakeGenerator`**: `Snowflake` 구현체를 주입받는 `IdGenerator<Long>` 어댑터 추가 ([`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340))
- **`Snowflakers.default(machineId)`** / **`Snowflakers.global()`** 팩토리 함수 추가 ([`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340))

### Changed

#### utils/idgenerators — ID 생성기 API 통일 (Uuid 패턴)

UUID, KSUID를 `object Uuid { interface Generator; object V1..V7 }` 패턴으로 통일 ([`945b7444`](https://github.com/bluetape4k/bluetape4k-projects/commit/945b7444), [`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340))

- **`object Uuid`**: `Uuid.Generator` interface + `Uuid.V1`/`V4`/`V5`/`V6`/`V7` nested objects
  - `Uuid.random(random)` — 커스텀 Random V4 생성기
  - `Uuid.epochRandom(random)` — 커스텀 Random V7 생성기
  - `Uuid.namebased(name)` — 결정론적 V5 생성기
  - 인코딩 `Url62.encode()` 로 통일 (`nextBase62()`, `nextBase62s(size)`)
- **`object Ksuid`**: `Ksuid.Generator` interface + `Ksuid.Seconds`(초 기반) / `Ksuid.Millis`(밀리초 기반) nested objects
- **`UuidGenerator(generator: Uuid.Generator = Uuid.V7)`**: `IdGenerator<UUID>` 어댑터
- 기존 `TimebasedUuidGenerator`, `RandomUuidGenerator`, `NamebasedUuidGenerator` → `@Deprecated(WARNING)` + `ReplaceWith` 유지
- 기존 `KsuidMillis` → `@Deprecated(WARNING)` + `Ksuid.Millis` 위임으로 하위 호환 유지

#### 마이그레이션 — deprecated 사용처 신규 API로 교체

`TimebasedUuid.Epoch` → `Uuid.V7`, `KsuidMillis` → `Ksuid.Millis` 교체 완료 ([`7163e35e`](https://github.com/bluetape4k/bluetape4k-projects/commit/7163e35e), [`4d9b712c`](https://github.com/bluetape4k/bluetape4k-projects/commit/4d9b712c))

- `spring-boot3/4/StopWatchSupport`, `utils/jwt/KeyChain`, `examples/coroutines-demo`
- `aws/aws/DynamoDbEntity` (`TimebasedUuidGenerator` → `Uuid.V7.nextBase62()`)
- `data/exposed-core/ColumnExtensions`, `data/hibernate`, `data/exposed-jdbc/r2dbc` 테스트 인프라
- `infra/cache-core` 테스트

---

### Added (Beta1)

#### spring-boot4 — Spring Boot 4.x 전용 모듈 신규 추가

- **`bluetape4k-spring-boot4-core`**: WebFlux/RestClient Coroutines DSL (`suspendGet`, `suspendPost`, `suspendPut`, `suspendPatch`, `suspendDelete`), Jackson 2 ObjectMapper 커스터마이저, Retrofit2 통합, WebClient/WebTestClient 확장
- **`bluetape4k-spring-boot4-data-redis`**: Spring Data Redis 고성능 직렬화 (`RedisBinarySerializer`, `RedisCompressSerializer`, `redisSerializationContext {}` DSL)
- **`bluetape4k-spring-boot4-r2dbc`**: Spring Data R2DBC 코루틴 확장 (`XyzSuspending` 패턴)
- **`bluetape4k-spring-boot4-mongodb`**: Spring Data MongoDB Reactive 코루틴 확장, Criteria/Query/Update infix DSL
- **`bluetape4k-spring-boot4-cassandra`**: Spring Data Cassandra 코루틴 확장
- **`bluetape4k-spring-boot4-cassandra-demo`**: Cassandra + Spring Data Cassandra 종합 예제

> Spring Boot 4 BOM은 `implementation(platform(...))` 방식으로 적용 (`dependencyManagement { imports }` 방식은 KGP 2.3.x와 충돌)

### Changed

#### spring-boot3, spring-boot4 — Deprecated 함수 제거

- `spring-boot3/r2dbc`, `spring-boot4/r2dbc`: `suspend*` 접두사 deprecated 래퍼 함수 제거 (총 18개)
  - 제거: `suspendFindOneById`, `suspendSelectOne`, `suspendInsert`, `suspendUpdate`, `suspendDelete`, `suspendCount`, `suspendExists` 등
- `spring-boot3/cassandra`, `spring-boot4/cassandra`: `suspend*`/`co*` 접두사 deprecated 래퍼 함수 제거 (총 130개 이상)
  - 제거: `suspendQuery`, `coQuery`, `suspendExecute`, `suspendSelectOne`, `suspendInsert`, `suspendUpdate`, `suspendDelete` 등
  - 사용처 모두 `XyzSuspending` 형식으로 교체 완료

### Fixed

- `gradle.properties`에서 deprecated `kotlin.incremental.useClasspathSnapshot=false` 속성 제거 (KGP 2.3.x에서 불필요)



#### infra/cache-core — JCache 기반 NearCache

- **`NearJCache<K, V>`**: Caffeine front + JCache back 2-Tier 동기 NearCache (`JCache<K,V>` 위임) ([
  `0a09c19d`](https://github.com/bluetape4k/bluetape4k-projects/commit/0a09c19d))
- **`SuspendNearJCache<K, V>`**: Caffeine front + SuspendJCache back 코루틴 NearCache ([
  `0a09c19d`](https://github.com/bluetape4k/bluetape4k-projects/commit/0a09c19d))
- **`NearJCacheConfig<K, V>`** + **`NearJCacheConfigBuilder`** + **`nearJCacheConfig {}`** DSL ([
  `b19b48b9`](https://github.com/bluetape4k/bluetape4k-projects/commit/b19b48b9))
- `AbstractNearCacheOperationsTest` / `AbstractSuspendNearCacheOperationsTest` 동시성 테스트 추가 ([
  `a4c0bf14`](https://github.com/bluetape4k/bluetape4k-projects/commit/a4c0bf14))
- `ResilientNearCacheDecorator` 단위 테스트 추가 ([
  `054be42a`](https://github.com/bluetape4k/bluetape4k-projects/commit/054be42a))

#### infra/cache-lettuce — 팩토리 확장

- **`LettuceCaches.suspendJCache()`**: `LettuceSuspendJCache<V>` 팩토리 ([
  `0b0ebbf6`](https://github.com/bluetape4k/bluetape4k-projects/commit/0b0ebbf6))
- **`LettuceCaches.nearJCache()`**: DSL/Config 오버로드로 `NearJCache<K,V>` 생성 ([
  `0b0ebbf6`](https://github.com/bluetape4k/bluetape4k-projects/commit/0b0ebbf6))
- **`LettuceCaches.suspendNearJCache()`**: DSL/Config 오버로드로 `SuspendNearJCache<K,V>` 생성 ([
  `0b0ebbf6`](https://github.com/bluetape4k/bluetape4k-projects/commit/0b0ebbf6))

#### README 최신화

- 통합 모듈별 README.md 신규 작성: `spring/boot3`, `vertx`, `aws`, `aws-kotlin`, `utils/geo`
- `io/jackson2`, `io/jackson3` — 바이너리(CBOR, Ion, Smile, Avro, Protobuf) 및 텍스트(YAML, CSV, TOML, Properties) 포맷 지원 섹션 추가

### Changed

#### 모듈 리네이밍

- **`bluetape4k-jackson` → `bluetape4k-jackson2`** (디렉토리: `io/jackson` → `io/jackson2`)
  — `bluetape4k-jackson3`과의 버전 대칭을 위한 명시적 리네이밍
- **`bluetape4k-exposed-jackson` → `bluetape4k-exposed-jackson2`** (디렉토리: `data/exposed-jackson` → `data/exposed-jackson2`)

#### 모듈 통합 — io

- **`bluetape4k-jackson2`**: 구 `bluetape4k-jackson-binary`(CBOR, Ion, Smile, Avro, Protobuf) + `bluetape4k-jackson-text`(YAML, CSV, TOML, Properties) 통합 ([`9ca9b975`](https://github.com/bluetape4k/bluetape4k-projects/commit/9ca9b975), [`35d32eb0`](https://github.com/bluetape4k/bluetape4k-projects/commit/35d32eb0))
- **`bluetape4k-jackson3`**: 구 `bluetape4k-jackson3-binary` + `bluetape4k-jackson3-text` 통합 ([`b3415a0f`](https://github.com/bluetape4k/bluetape4k-projects/commit/b3415a0f))

#### 모듈 통합 — utils

- **`bluetape4k-geo`**: 구 `bluetape4k-geocode`(Bing/Google) + `bluetape4k-geohash` + `bluetape4k-geoip2`(MaxMind) 통합 ([`84553efe`](https://github.com/bluetape4k/bluetape4k-projects/commit/84553efe))

#### 모듈 통합 — spring

- **`bluetape4k-spring-boot3`** (`spring/boot3`): 구 `spring/core` + `spring/webflux` + `spring/retrofit2` + `spring/tests` + `spring/jpa` 통합 ([`9f0b5fa2`](https://github.com/bluetape4k/bluetape4k-projects/commit/9f0b5fa2))

#### 모듈 통합 — vertx

- **`bluetape4k-vertx`**: 구 `vertx/core` + `vertx/sqlclient` + `vertx/resilience4j` 통합 ([`a0ba94ad`](https://github.com/bluetape4k/bluetape4k-projects/commit/a0ba94ad))

#### 모듈 통합 — aws

- **`bluetape4k-aws`**: 구 `aws/core`, `aws/dynamodb`, `aws/s3`, `aws/ses`, `aws/sns`, `aws/sqs`, `aws/kms`, `aws/cloudwatch`, `aws/kinesis`, `aws/sts` 통합 (22개 → 2개) ([`f2c36d53`](https://github.com/bluetape4k/bluetape4k-projects/commit/f2c36d53))
- **`bluetape4k-aws-kotlin`**: 구 `aws-kotlin/core`, `aws-kotlin/dynamodb`, `aws-kotlin/s3`, `aws-kotlin/ses`, `aws-kotlin/sesv2`, `aws-kotlin/sns`, `aws-kotlin/sqs`, `aws-kotlin/kms`, `aws-kotlin/cloudwatch`, `aws-kotlin/kinesis`, `aws-kotlin/sts` 통합 ([`f2c36d53`](https://github.com/bluetape4k/bluetape4k-projects/commit/f2c36d53))

#### infra/cache — 일관성 리팩토링

- **`LettuceBinaryCodec` 통일**: 팩토리 파라미터의 `BinarySerializer` → `LettuceBinaryCodec<V>` 교체 ([
  `598c88c0`](https://github.com/bluetape4k/bluetape4k-projects/commit/598c88c0))
- **`LettuceSuspendCacheManager`**: 미사용 파라미터 실제 활용으로 개선 ([
  `495f5330`](https://github.com/bluetape4k/bluetape4k-projects/commit/495f5330))
- **`RedissonCaches`**: 팩토리 네이밍 통일 ([`a10df979`](https://github.com/bluetape4k/bluetape4k-projects/commit/a10df979))
- **`HazelcastCaches.nearJCache/suspendNearJCache`**: 파라미터 2개로 축소 + DSL 지원 ([
  `7f7cdb52`](https://github.com/bluetape4k/bluetape4k-projects/commit/7f7cdb52))
- `JCache NearCache`를 `nearcache.jcache` 서브패키지로 이동 ([
  `f405197b`](https://github.com/bluetape4k/bluetape4k-projects/commit/f405197b))

### Deprecated

#### io/crypto

- **`bluetape4k-crypto`**: Jasypt 기반 암호화 모듈 Deprecated — `bluetape4k-tink` (Google Tink AEAD)로 대체 ([
  `38a05c26`](https://github.com/bluetape4k/bluetape4k-projects/commit/38a05c26))

#### io/okio

- **Cipher/Jasypt Sink/Source**: `io/okio` 모듈에서 cipher 및 jasypt 관련 클래스 제거 — `io/tink`의 `TinkEncryptSink`/
  `TinkDecryptSource` 사용 권장 ([`27edccc5`](https://github.com/bluetape4k/bluetape4k-projects/commit/27edccc5))

#### utils-deprecated

- **`ahocorasick`**: `utils/` → `utils-deprecated/` 이동, 빌드 제외 ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))
- **`lingua`**: `utils/` → `utils-deprecated/` 이동, 빌드 제외 ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))
- **`naivebayes`**: `utils/` → `utils-deprecated/` 이동, 빌드 제외 ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))
- **`mutiny-examples`**: 예제성 모듈 → `utils-deprecated/` 이동, 빌드 제외 ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))

### Removed

- 구 서브모듈 소스 파일 정리 (`jackson-binary/text`, `jackson3-binary/text`, `geocode`, `geohash`, `geoip2`, `vertx/core`, `vertx/sqlclient`, `vertx/resilience4j`, aws 개별 서브모듈) ([`c7fb930c`](https://github.com/bluetape4k/bluetape4k-projects/commit/c7fb930c))
- **`TiDBServer`**: 테스트 인프라에서 TiDB Testcontainers 지원 제거 ([
  `bf617426`](https://github.com/bluetape4k/bluetape4k-projects/commit/bf617426))
- **예제성 모듈 제거**: 사용 빈도 낮은 예제 모듈 빌드에서 제외 (`utils-deprecated/`, `x-obsoleted/` 이동) ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))
- **`bloomfilter`**: 사용 빈도 낮아 `x-obsoleted/bloomfilter`로 이동 ([
  `8b30555c`](https://github.com/bluetape4k/bluetape4k-projects/commit/8b30555c))
- **`captcha`**: 사용 빈도 낮아 `x-obsoleted/captcha`로 이동 ([
  `8b30555c`](https://github.com/bluetape4k/bluetape4k-projects/commit/8b30555c))
- **`logback-kafka`**: 사용 빈도 낮아 `x-obsoleted/logback-kafka`로 이동 ([
  `8b30555c`](https://github.com/bluetape4k/bluetape4k-projects/commit/8b30555c))
- **`nats`**: 사용 빈도 낮아 `x-obsoleted/nats`로 이동
- **`javers`**: 사용 빈도 낮아 `x-obsoleted/javers`로 이동
- **`tokenizer`**: 사용 빈도 낮아 `x-obsoleted/tokenizer`로 이동

### Fixed

#### utils/javatimes

- `MinPeriodTime` / `MaxPeriodTime` import 누락 수정 ([`1962525d`](https://github.com/bluetape4k/bluetape4k-projects/commit/1962525d))

#### infra/kafka

- `StringKafkaCodec` deserializer 인코딩 키 버그 수정 ([`ec7d0d99`](https://github.com/bluetape4k/bluetape4k-projects/commit/ec7d0d99))

#### data/cassandra

- `CqlDuration` nano 파트 변환 버그 수정 ([`e13d043e`](https://github.com/bluetape4k/bluetape4k-projects/commit/e13d043e))

#### utils/geo

- `bluetape4k-geo` 소스 디렉토리 경로 수정 ([`873e64e3`](https://github.com/bluetape4k/bluetape4k-projects/commit/873e64e3))

### Chores

- 전 모듈 코드 리뷰: KDoc 보강, `!!` 남용 패턴 개선, `requireNotNull` 중복 제거 (javatimes, geoip2, geohash, logback-kafka, math, naivebayes, vertx, spring-core, aws-kotlin 등)
- CLAUDE.md 모듈 구조 섹션 업데이트 (모듈 통합 반영) ([`4d111851`](https://github.com/bluetape4k/bluetape4k-projects/commit/4d111851))

---

## [1.4.0] - 2026-03-12

> **Full diff**: [`1.3.0...1.4.0`](https://github.com/bluetape4k/bluetape4k-projects/compare/1.3.0...1.4.0)

### Added

#### infra/lettuce

- **`LettuceIntCodec`**: Int 값을 4바이트 big-endian으로 직렬화하는 `RedisCodec<String, Int>` (Redisson `IntegerCodec`과 바이너리 호환) ([`1277dbf`](https://github.com/bluetape4k/bluetape4k-projects/commit/1277dbfc))
- **`LettuceLongCodec`**: Long 값을 8바이트 big-endian으로 직렬화하는 `RedisCodec<String, Long>` (Redisson `LongCodec`과 바이너리 호환) ([`1277dbf`](https://github.com/bluetape4k/bluetape4k-projects/commit/1277dbfc))
- **`LettuceLeaderElection`** / **`LettuceSuspendLeaderElection`**: 분산 리더 선출 ([`17063567`](https://github.com/bluetape4k/bluetape4k-projects/commit/17063567))
- **`LettuceLeaderGroupElection`** / **`LettuceSuspendLeaderGroupElection`**: 분산 그룹 리더 선출 ([`17063567`](https://github.com/bluetape4k/bluetape4k-projects/commit/17063567))
- **`LeaderElectionOptions`** / **`LeaderGroupElectionOptions`**: `bluetape4k-leaders` 모듈 옵션 클래스 ([`5a026a2`](https://github.com/bluetape4k/bluetape4k-projects/commit/5a026a2a))
- Lettuce cache contracts 강화 및 Redis 8 테스트 추가 ([`19339945`](https://github.com/bluetape4k/bluetape4k-projects/commit/19339945))

#### infra/cache-lettuce

- **`LettuceMemoizer<K, V>`**: `LettuceMap<V>` 기반 동기 메모이제이션 (`Memoizer<K,V>` 인터페이스) ([`6b2b1aa`](https://github.com/bluetape4k/bluetape4k-projects/commit/6b2b1aa6))
- **`LettuceAsyncMemoizer<K, V>`**: `LettuceMap<V>` 기반 비동기 메모이제이션 (`AsyncMemoizer<K,V>` 인터페이스) ([`6b2b1aa`](https://github.com/bluetape4k/bluetape4k-projects/commit/6b2b1aa6))
- **`LettuceSuspendMemoizer<K, V>`**: `LettuceMap<V>` 기반 suspend 메모이제이션 (`SuspendMemoizer<K,V>` 인터페이스) ([`6b2b1aa`](https://github.com/bluetape4k/bluetape4k-projects/commit/6b2b1aa6))

#### infra/redisson

- **`RedissonMemoizer`** / **`AsyncRedissonMemoizer`** / **`RedissonSuspendMemoizer`**: `Memoizer<K,V>` 인터페이스 구현체 추가 ([`a6baef8`](https://github.com/bluetape4k/bluetape4k-projects/commit/a6baef84))

#### cache-core (testFixtures)

- **`AbstractMemoizerTest`** / **`AbstractAsyncMemoizerTest`** / **`AbstractSuspendMemoizerTest`**: Memoizer 공통 테스트 기반 클래스 ([`384311a`](https://github.com/bluetape4k/bluetape4k-projects/commit/384311a1))

#### cache-hazelcast / cache-ignite

- `HazelcastMemoizer`, `IgniteMemoizer` 등 `Memoizer<K,V>` 인터페이스 구현 추가 ([`384311a`](https://github.com/bluetape4k/bluetape4k-projects/commit/384311a1))
- cache 모듈별 **Factory object** 추가 및 JCache SPI 정리 ([`5c7ec82`](https://github.com/bluetape4k/bluetape4k-projects/commit/5c7ec829))

#### io/protobuf (신규 모듈)

- `io/grpc`에서 Protobuf 유틸리티 분리 → `io/protobuf` 독립 모듈 ([`63abe48`](https://github.com/bluetape4k/bluetape4k-projects/commit/63abe486))

#### io/okio (신규 모듈)

- `io/io` 모듈의 okio 패키지 → `io/okio` 독립 모듈로 분리 ([`5b92c2d`](https://github.com/bluetape4k/bluetape4k-projects/commit/5b92c2dd))

### Changed

#### infra/lettuce

- **분산 Primitive 클래스명 변경** (`Redis*` → `Lettuce*`): `RedisMap` → `LettuceMap`, `RedisSuspendMap` → `LettuceSuspendMap`, `RedisAtomicLong` → `LettuceAtomicLong`, `RedisSemaphore` → `LettuceSemaphore`, `RedisLock` → `LettuceLock` 등 ([`92625766`](https://github.com/bluetape4k/bluetape4k-projects/commit/92625766))
- **`LettuceMap<V>`**: `syncCommands` 접근자 `protected` 변경, `putTtl` / `putAllTtl` 메서드 추가 ([`5949f7a`](https://github.com/bluetape4k/bluetape4k-projects/commit/5949f7a4))
- Memoizer 기능: `infra/lettuce` → `infra/cache-lettuce` 이동, `LettuceMemoizer<K:Any, V:Any>` Generic화 ([`6b2b1aa`](https://github.com/bluetape4k/bluetape4k-projects/commit/6b2b1aa6))
- 분산 Primitive 리팩토링 및 동시성 테스트 강화 ([`5949f7a`](https://github.com/bluetape4k/bluetape4k-projects/commit/5949f7a4))

#### infra/redis (모듈 분리)

- `bluetape4k-redis` 단일 모듈 → `lettuce` + `redisson` + `spring-data-redis` 3개 모듈로 분리 ([`953321b`](https://github.com/bluetape4k/bluetape4k-projects/commit/953321bf))
- `awaitSuspending()`: 자체 구현 → `kotlinx.coroutines.future.await`로 대체 ([`221c6d5`](https://github.com/bluetape4k/bluetape4k-projects/commit/221c6d51))

#### utils/leader

- leader local election flows 단순화 (불필요한 중간 state 제거) ([`5ad9477`](https://github.com/bluetape4k/bluetape4k-projects/commit/5ad9477c))
- `LeaderElectionOptions` / `LeaderGroupElectionOptions` 옵션 클래스 lettuce/redisson 모듈에 적용 ([`647ed27`](https://github.com/bluetape4k/bluetape4k-projects/commit/647ed272))

#### bluetape4k-core

- 핵심 유틸리티 코드 단순화 리팩토링 ([`0c5fa93`](https://github.com/bluetape4k/bluetape4k-projects/commit/0c5fa939))

#### infra/cache-core

- **`SuspendNearCache.clear()`**: front/back cache 모두 clear 완료 후 info 로그 추가 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### infra/cache-lettuce

- **`ResilientLettuceSuspendNearCacheTest`**: write-behind 동기화 timeout 3초 → 5초로 증가 (테스트 안정성 개선) ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### infra/cache-redisson

- **`RedisSuspendNearCacheTest`**: Redisson `DEFAULT_EXPIRY_CHECK_PERIOD`(30s)를 고려한 clearAll timeout 30→40초 조정, `untilSuspending`에 `atMost` 명시 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### infra/nats

- **`ServerPoolExample`**: `@TestInstance(PER_CLASS)` + `@BeforeAll`/`@AfterAll` 방식으로 NATS 서버 관리 개선, 순차 기동으로 Docker 레이스 컨디션 방지 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

### Removed

#### infra/cache-ignite2

- **`cache-ignite2` 모듈 완전 제거**: cache umbrella에서 ignite2 의존성 삭제, 모듈 소스 및 테스트 전체 삭제 ([`4972b98`](https://github.com/bluetape4k/bluetape4k-projects/commit/4972b985))
  - Apache Ignite 2.x 지원 종료 (Ignite 3.x `cache-ignite`로 대체)

#### examples

- vertx 관련 예제 제거 (미사용) ([`fa64ee3`](https://github.com/bluetape4k/bluetape4k-projects/commit/fa64ee3c))

### Fixed

#### infra/lettuce

- **`LettuceIntCodec` / `LettuceLongCodec`**: `decodeValue`에서 absolute read → `bytes.duplicate()` 방식으로 수정 (caller position 불변) ([`d59db75`](https://github.com/bluetape4k/bluetape4k-projects/commit/d59db750))
- **`LettuceJCaching`**: 기본값 `localhost:6379` 에 자동 연결되는 문제 수정 ([`7074c27`](https://github.com/bluetape4k/bluetape4k-projects/commit/7074c27c))

#### infra/cache-lettuce

- **`LettuceAsyncMemoizer`**: `thenApply` 내부 blocking sync 호출 → `thenCompose` + `getAsync()`로 교체 ([`d59db75`](https://github.com/bluetape4k/bluetape4k-projects/commit/d59db750))
- **`LettuceAsyncMemoizer.clear()`**: dangling future 방지를 위한 `inFlight.clear()` 제거 ([`d59db75`](https://github.com/bluetape4k/bluetape4k-projects/commit/d59db750))
- **`ResilientLettuceSuspendNearCache.close()`**: 채널을 먼저 닫고 `consumerJob.join()`으로 write-behind 커맨드 소진 대기 후 `scope.cancel()`하도록 종료 순서 개선 (커맨드 유실 방지) ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))
- `LettuceNearCache` / `RedissonNearCache` 기본 코덱을 LZ4 + Fory로 지정 ([`ee5de36`](https://github.com/bluetape4k/bluetape4k-projects/commit/ee5de362))
- Write-behind `DEFAULT_BATCH_SIZE` 변경 (100 → 500) ([`52320804`](https://github.com/bluetape4k/bluetape4k-projects/commit/52320804))

#### infra/cache-hazelcast

- **`HazelcastJCaching`**: 기본 포트(5701)에 무한 재시도하는 문제 수정 ([`ee63b31`](https://github.com/bluetape4k/bluetape4k-projects/commit/ee63b317))

#### infra/cache-ignite

- **`IgniteAsyncMemoizer`**: 비동기 API(`*Async()`)로 개선하여 ARM64 타임아웃 문제 해결 ([`3d96439`](https://github.com/bluetape4k/bluetape4k-projects/commit/3d96439e))
- `untilSuspending` 의 hang 방지 및 timeout 전파 수정 (root cause 보존) ([`12766771`](https://github.com/bluetape4k/bluetape4k-projects/commit/12766771))
- redisson 비동기 near cache 테스트를 `runSuspendIO`로 전환 ([`12766771`](https://github.com/bluetape4k/bluetape4k-projects/commit/12766771))
- ignite 동적 cache readiness 회귀 테스트 추가 ([`12766771`](https://github.com/bluetape4k/bluetape4k-projects/commit/12766771))

#### infra/cache-redisson

- **`RedissonSuspendCache`**: 모든 `*Async()` 호출의 `awaitSuspending()` → `kotlinx.coroutines.future.await()`로 교체 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### aws-kotlin (dynamodb / s3 / sqs / sts)

- `existsTable` 전체 페이지 순회 개선, s3 `exists/putAll/getAll` 보강 ([`aa5aecc`](https://github.com/bluetape4k/bluetape4k-projects/commit/aa5aecc4))
- sqs `receive` 기본 대기시간 결함 수정, sts `durationSeconds` 계약 검증 추가 ([`1b693960`](https://github.com/bluetape4k/bluetape4k-projects/commit/1b693960))

#### testing/testcontainers

- Testcontainers 1.21.4 → 2.0.3 업그레이드 안정화 (2.x 좌표 체계 반영) ([`1aaf713`](https://github.com/bluetape4k/bluetape4k-projects/commit/1aaf7130))
- **`NatsServer`**: NATS 이미지 태그 `2.10` → `2.12` 업그레이드 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))
- **`LocalStackServerTest`**: custom network 테스트 `@Disabled` 처리 (Ryuk 레이스 컨디션, Docker Desktop macOS 이슈) ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### bluetape4k-coroutines

- Coroutines 함수 호출 버그 수정 ([`ec0570c`](https://github.com/bluetape4k/bluetape4k-projects/commit/ec0570c8))

#### testing/junit5

- **`untilSuspending`**: `coroutineScope` → `withContext(Dispatchers.IO)` 변경으로 hang 방지 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))
- Awaitility 4.2+ 필드명 변경(`timeoutConstraint` → `waitConstraint`) 호환성 대응 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### 기타

- infra/nats: 관리 API 예외 계약 정리 ([`51ba07c`](https://github.com/bluetape4k/bluetape4k-projects/commit/51ba07c7))
- infra/bucket4j: probe 기반 잔여 토큰 계산 최적화 ([`4af6eba`](https://github.com/bluetape4k/bluetape4k-projects/commit/4af6ebac))
- infra/micrometer: Timer/Retrofit 계측 경로 회귀 보강 ([`740eab5`](https://github.com/bluetape4k/bluetape4k-projects/commit/740eab5e))
- infra/opentelemetry: Span/Coroutine helper 예외 처리 정리 ([`5c033fb`](https://github.com/bluetape4k/bluetape4k-projects/commit/5c033fb4))
- infra/resilience4j: 코드리뷰, KDoc 보강, README 최신화 ([`905554a`](https://github.com/bluetape4k/bluetape4k-projects/commit/905554a0))
- vertx/sqlclient: Transaction CancellationException 전파 보강, TupleMapper 리플렉션 캐시 도입 ([`e6a11a1`](https://github.com/bluetape4k/bluetape4k-projects/commit/e6a11a19))
- utils/tokenizer: Korean/NounTokenizer topN 검증, fallback offset 보정 최적화 ([`56aaa23`](https://github.com/bluetape4k/bluetape4k-projects/commit/56aaa23d))
- utils/javatimes: ISO nullable 포맷 정렬, 캘린더 주 동작 계약 정렬 및 범위 계약 강화 ([`ff943f5`](https://github.com/bluetape4k/bluetape4k-projects/commit/ff943f53))
- testing/junit5: 스트레스 테스터 실행 모델 개선 (대량 rounds 시 메모리 안정화) ([`3c6f1cb`](https://github.com/bluetape4k/bluetape4k-projects/commit/3c6f1cba))

### Chores

- ConsulServer Docker 이미지 `hashicorp/consul 1.20`으로 업데이트 ([`7aa5761`](https://github.com/bluetape4k/bluetape4k-projects/commit/7aa5761f))

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
