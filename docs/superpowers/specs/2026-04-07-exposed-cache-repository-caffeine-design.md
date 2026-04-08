# exposed-cache 리네이밍 + Caffeine 로컬 캐시 모듈 설계

- **작성일**: 2026-04-07 (수정: 2026-04-08)
- **대상 저장소**: bluetape4k-projects
- **관련 모듈**: `exposed-redis-api` → `exposed-cache`, `exposed-jdbc-caffeine` (신규), `exposed-r2dbc-caffeine` (신규)

---

## Section 1: 요구사항 및 배경

### 배경

현재 Exposed 캐시 모듈 구조:

| 모듈                       | 역할                              |
|--------------------------|---------------------------------|
| `exposed-redis-api`      | 공통 캐시 인터페이스 + testFixtures 시나리오 |
| `exposed-jdbc-lettuce`   | JDBC + Lettuce Redis 캐시 구현      |
| `exposed-jdbc-redisson`  | JDBC + Redisson Redis 캐시 구현     |
| `exposed-r2dbc-lettuce`  | R2DBC + Lettuce Redis 캐시 구현     |
| `exposed-r2dbc-redisson` | R2DBC + Redisson Redis 캐시 구현    |

**문제점**: 로컬 캐시(Caffeine 등) 전략 구현체가 없고, `exposed-redis-api`라는 모듈명이 Redis 종속적으로 오해를 유발함.

### 요구사항

1. `exposed-redis-api` → `exposed-cache` 모듈명 변경 (패키지 `io.bluetape4k.exposed.cache` 와 일치, 소스 변경 없음)
2. `exposed-jdbc-caffeine` 신규 모듈 추가 (로컬 캐시 전략, JDBC)
3. `exposed-r2dbc-caffeine` 신규 모듈 추가 (로컬 캐시 + R2DBC/코루틴)

---

## Section 2: 인터페이스 계층 구조

### 현재 구조의 문제

`JdbcCacheRepository`, `SuspendedJdbcCacheRepository`, `R2dbcCacheRepository`에
`invalidateByPattern(patterns: String, count: Int): Long` 메서드가 정의되어 있으나, 이는 Redis SCAN 명령 전용 기능으로 Caffeine에는 직접 대응 개념이 없음.

### 변경된 계층 구조

```
exposed-cache (공통 인터페이스 + 공통 Config)
├── JdbcCacheRepository<ID, E>           ← invalidateByPattern 제거
├── SuspendedJdbcCacheRepository<ID, E>  ← invalidateByPattern 제거
├── R2dbcCacheRepository<ID, E>          ← invalidateByPattern 제거
│
├── JdbcRedisRepository<ID, E>           ← 신규: JdbcCacheRepository 확장 + invalidateByPattern
├── SuspendJdbcRedisRepository<ID, E>    ← 신규
├── R2dbcRedisRepository<ID, E>          ← 신규 (invalidateByPattern은 suspend fun)
│
├── LocalCacheConfig                     ← 신규: 로컬 캐시 공통 설정 (maximumSize, TTL 등)
└── RedisRepositoryResilienceConfig      ← 신규: Redis 전용 Resilience 설정 (optional)

exposed-jdbc-lettuce
└── JdbcLettuceRepository → JdbcRedisRepository 구현 (변경)

exposed-jdbc-redisson
└── JdbcRedissonRepository → JdbcRedisRepository 구현 (변경)

exposed-r2dbc-lettuce
└── R2dbcLettuceRepository → R2dbcRedisRepository 구현 (변경)

exposed-r2dbc-redisson
└── R2dbcRedissonRepository → R2dbcRedisRepository 구현 (변경)

exposed-jdbc-caffeine (신규)
├── JdbcCaffeineRepository          → JdbcCacheRepository 구현 (cacheMode = LOCAL 고정)
├── SuspendedJdbcCaffeineRepository → SuspendedJdbcCacheRepository 구현 (JDBC 기반)
└── CaffeineCacheConfig (선택)      → LocalCacheConfig 상속, recordStats 등 Caffeine 특화 옵션

exposed-r2dbc-caffeine (신규)
└── R2dbcCaffeineRepository → R2dbcCacheRepository 구현 (R2DBC 전용, JDBC 의존 없음)
    ← LocalCacheConfig 사용 (exposed-cache 참조)
```

### Redis 전용 인터페이스 (신규)

```kotlin
// exposed-cache 패키지: io.bluetape4k.exposed.cache.redis

/** JDBC + Redis 저장소 전용 인터페이스 (Redis SCAN 패턴 무효화 지원) */
interface JdbcRedisRepository<ID: Any, E: Serializable>: JdbcCacheRepository<ID, E> {
    /**
     * 패턴에 맞는 캐시 키를 무효화합니다 (Redis SCAN 전용).
     * @param patterns 캐시 키 패턴 (예: "*user*", "prefix:*")
     * @param count 한 번에 스캔할 키 수
     * @return 무효화된 캐시 항목 수
     */
    fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long
}

interface SuspendJdbcRedisRepository<ID: Any, E: Serializable>: SuspendedJdbcCacheRepository<ID, E> {
    suspend fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long
}

interface R2dbcRedisRepository<ID: Any, E: Serializable>: R2dbcCacheRepository<ID, E> {
    // ⚠️ R2dbcCacheRepository의 suspend fun override → suspend 필수
    suspend fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long
}
```

### CacheMode enum 확장

```kotlin
enum class CacheMode {
    LOCAL,       // 인프로세스 캐시만 사용 (Caffeine/Cache2k) — 신규
    REMOTE,      // 원격 캐시만 사용 (Redis)
    NEAR_CACHE,  // L1 로컬 + L2 원격 혼합 — Lettuce/Redisson에서 계속 사용
}
```

> **Caffeine 구현체**: `cacheMode`를 항상 `LOCAL`로 고정. 생성자 검증 불필요 — 단순히 override 값으로 강제.

---

## Section 3: 모듈 리네이밍 전략

### Gradle 자동 등록 메커니즘

`settings.gradle.kts`의 `includeModules("data", withBaseDir = false)` 함수가 `data/` 하위 디렉토리명을
`bluetape4k-{dirname}`으로 자동 등록함.

따라서 디렉토리명 변경만으로 모듈 ID 변경이 완료됨:

- `data/exposed-redis-api/` → `data/exposed-cache/`
- Gradle 모듈 ID: `bluetape4k-exposed-redis-api` → `bluetape4k-exposed-cache`

### 의존성 업데이트 필요 파일

| 파일                                             | 변경 내용                                                                                                           |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `data/exposed-jdbc-lettuce/build.gradle.kts`   | `project(":bluetape4k-exposed-redis-api")` → `project(":bluetape4k-exposed-cache")`                             |
| `data/exposed-jdbc-redisson/build.gradle.kts`  | 동일                                                                                                              |
| `data/exposed-r2dbc-lettuce/build.gradle.kts`  | 동일                                                                                                              |
| `data/exposed-r2dbc-redisson/build.gradle.kts` | 동일                                                                                                              |
| `testFixtures` 의존성 참조 (lettuce/redisson)       | `testFixtures(project(":bluetape4k-exposed-redis-api"))` → `testFixtures(project(":bluetape4k-exposed-cache"))` |

### 패키지명 유지

소스 파일 내 패키지는 `io.bluetape4k.exposed.cache.*`로 유지 → **소스 파일 변경 불필요**.

### Redis 전용 인터페이스 새 파일 위치

```
data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/redis/
├── JdbcRedisRepository.kt
├── SuspendJdbcRedisRepository.kt
└── R2dbcRedisRepository.kt
```

### LocalCacheConfig + ResilienceConfig 새 파일 위치

```
data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/
├── LocalCacheConfig.kt                 ← 신규: 로컬 캐시 공통 설정 (상속 가능)
└── redis/
    └── RedisRepositoryResilienceConfig.kt  ← 신규: Redis 전용 Resilience 설정 (optional)
```

### LocalCacheConfig 설계

```kotlin
// io.bluetape4k.exposed.cache.LocalCacheConfig
open class LocalCacheConfig(
    val keyPrefix: String = "local",
    val maximumSize: Long = 10_000L,
    val expireAfterWrite: Duration = Duration.ofMinutes(10),
    val expireAfterAccess: Duration? = null,
    val writeMode: CacheWriteMode = CacheWriteMode.READ_ONLY,
    val writeBehindBatchSize: Int = 100,
    val writeBehindQueueCapacity: Int = 10_000,
): Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}
```

**확장 예시**: Caffeine 특화 옵션이 필요한 경우에만 서브클래스 작성.

```kotlin
// exposed-jdbc-caffeine: io.bluetape4k.exposed.caffeine.CaffeineCacheConfig
class CaffeineCacheConfig(
    keyPrefix: String = "caffeine",
    maximumSize: Long = 10_000L,
    expireAfterWrite: Duration = Duration.ofMinutes(10),
    expireAfterAccess: Duration? = null,
    writeMode: CacheWriteMode = CacheWriteMode.READ_ONLY,
    writeBehindBatchSize: Int = 100,
    writeBehindQueueCapacity: Int = 10_000,
    val recordStats: Boolean = false,   // Caffeine 전용
): LocalCacheConfig(
    keyPrefix, maximumSize, expireAfterWrite, expireAfterAccess,
    writeMode, writeBehindBatchSize, writeBehindQueueCapacity
) {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}
// 특화 옵션 없으면 LocalCacheConfig 직접 사용 가능
```

### RedisRepositoryResilienceConfig 설계

```kotlin
// io.bluetape4k.exposed.cache.redis.RedisRepositoryResilienceConfig
data class RedisRepositoryResilienceConfig(
    val retryMaxAttempts: Int = 3,
    val retryWaitDuration: Duration = Duration.ofMillis(500),
    val retryExponentialBackoff: Boolean = true,
    val circuitBreakerEnabled: Boolean = false,
    val timeoutDuration: Duration = Duration.ofSeconds(2),
): Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}

// Redis Repository Config에 optional 임베드
// (예: JdbcLettuceRepositoryConfig 별도 래퍼 or LettuceCacheConfig에 추가)
val resilience: RedisRepositoryResilienceConfig? = null  // null = 비활성화
```

---

## Section 4: exposed-jdbc-caffeine 설계

### 패키지

`io.bluetape4k.exposed.caffeine` (Lettuce는 `io.bluetape4k.exposed.lettuce` 동일 패턴)

### 의존성 (`build.gradle.kts`)

```kotlin
dependencies {
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-cache"))     // LocalCacheConfig, CacheMode, CacheWriteMode
    api(project(":bluetape4k-coroutines"))        // Write-Behind Channel chunked() 사용
    api(Libs.caffeine)
    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
}
```

### Config

`LocalCacheConfig` (from `exposed-cache`)를 직접 사용하거나, Caffeine 특화 옵션 필요 시 `CaffeineCacheConfig`로 확장.
(Section 3 참조)

### JdbcCaffeineRepository 인터페이스

```kotlin
interface JdbcCaffeineRepository<ID: Any, E: Serializable>: JdbcCacheRepository<ID, E> {
    val config: LocalCacheConfig         // CaffeineCacheConfig or LocalCacheConfig 직접
    val cache: Cache<String, E>          // Caffeine Cache (키: String 직렬화)
}
```

### AbstractJdbcCaffeineRepository

```kotlin
abstract class AbstractJdbcCaffeineRepository<ID: Any, E: Serializable>(
    override val config: LocalCacheConfig = LocalCacheConfig(),
): JdbcCaffeineRepository<ID, E> {

    abstract override val table: IdTable<ID>
    abstract override fun ResultRow.toEntity(): E
    abstract fun UpdateStatement.updateEntity(entity: E)
    abstract fun BatchInsertStatement.insertEntity(entity: E)

    open fun serializeKey(id: ID): String = id.toString()

    override val cacheName: String get() = config.keyPrefix
    override val cacheMode: CacheMode get() = CacheMode.LOCAL
    override val cacheWriteMode: CacheWriteMode get() = config.writeMode

    override val cache: Cache<String, E> by lazy {
        Caffeine.newBuilder()
            .maximumSize(config.maximumSize)
            .expireAfterWrite(config.expireAfterWrite)
            .apply { config.expireAfterAccess?.let { expireAfterAccess(it) } }
            .build()
    }

    // get() → 캐시 히트 시 반환, 미스 시 DB 조회 후 캐시 저장
    override fun get(id: ID): E? {
        val key = serializeKey(id)
        return cache.getIfPresent(key) ?: findByIdFromDb(id)?.also { cache.put(key, it) }
    }

    // put() → cacheWriteMode에 따라 DB 동기/비동기 쓰기
    override fun put(id: ID, entity: E) {
        cache.put(serializeKey(id), entity)
        when (config.writeMode) {
            CacheWriteMode.WRITE_THROUGH -> writeThrough(id, entity)
            CacheWriteMode.WRITE_BEHIND  -> writeBehind(id, entity)
            CacheWriteMode.READ_ONLY     -> { /* 캐시만 */
            }
        }
    }

    // invalidate → 캐시에서만 제거 (DB 미영향)
    override fun invalidate(id: ID) {
        cache.invalidate(serializeKey(id))
    }
    override fun clear() {
        cache.invalidateAll()
    }

    /**
     * Write-Behind 큐 drain 후 캐시 무효화. 데이터 유실 방지 필수.
     * READ_ONLY / WRITE_THROUGH 모드에서는 drain 불필요.
     */
    override fun close() {
        if (config.writeMode == CacheWriteMode.WRITE_BEHIND) {
            writeBehindQueue.close()          // 새 항목 추가 차단
            runBlocking { writeBehindJob.join() }  // 남은 배치 DB 반영 대기
        }
        cache.invalidateAll()
    }
}
```

### SuspendedJdbcCaffeineRepository (코루틴 지원)

JDBC 기반이므로 `exposed-jdbc-caffeine`에 위치. DB I/O는 `suspendedTransactionAsync { ... }` 패턴 사용 (Lettuce 구현체와 동일).

```kotlin
abstract class AbstractSuspendedJdbcCaffeineRepository<ID: Any, E: Serializable>(
    override val config: LocalCacheConfig = LocalCacheConfig(),
): SuspendedJdbcCaffeineRepository<ID, E> {
    // suspend get/put/invalidate 등 코루틴 버전
    // DB I/O는 suspendedTransactionAsync { ... } 사용 (withContext(IO) 아님)
}
```

### Write-Behind 구현

Caffeine의 `RemovalListener`를 활용하거나, 내부 `CoroutineScope` + `Channel`로 배치 큐 처리:

```kotlin
// WriteBehind 큐: 용량 제한으로 메모리 급증 방지 (UNLIMITED 금지)
private val writeBehindQueue = Channel<Pair<ID, E>>(capacity = config.writeBehindQueueCapacity)
private val writeBehindJob = scope.launch {
    writeBehindQueue.consumeAsFlow()
        .chunked(config.writeBehindBatchSize)   // bluetape4k-coroutines 의존 필요
        .collect { batch ->
            runCatching { writeToDb(batch) }
                .onFailure { log.error(it) { "Write-Behind DB 쓰기 실패: ${batch.size}건 — 재시도 없이 로그만 남김" } }
        }
}
// close()에서 writeBehindQueue.close() + writeBehindJob.join() 호출 필수 (위 참조)
```

---

## Section 5: exposed-r2dbc-caffeine 설계

### 패키지

`io.bluetape4k.exposed.r2dbc.caffeine`

### 의존성

```kotlin
dependencies {
    api(project(":bluetape4k-exposed-r2dbc"))
    api(project(":bluetape4k-exposed-cache"))     // LocalCacheConfig — JDBC 의존 없음
    api(project(":bluetape4k-coroutines"))
    api(Libs.caffeine)
    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
}
```

> `exposed-jdbc-caffeine` 의존 없음 — `LocalCacheConfig`는 `exposed-cache`에서 직접 참조.

### AbstractR2dbcCaffeineRepository

R2DBC suspend 환경에서는 Caffeine의 `AsyncCache<String, E>` 사용 (non-blocking):

```kotlin
abstract class AbstractR2dbcCaffeineRepository<ID: Any, E: Serializable>(
    override val config: LocalCacheConfig = LocalCacheConfig(),
): R2dbcCaffeineRepository<ID, E> {

    override val cacheMode: CacheMode get() = CacheMode.LOCAL  // 항상 LOCAL 고정

    override val cache: AsyncCache<String, E> by lazy {
        Caffeine.newBuilder()
            .maximumSize(config.maximumSize)
            .expireAfterWrite(config.expireAfterWrite)
            .apply { config.expireAfterAccess?.let { expireAfterAccess(it) } }
            .buildAsync()
    }

    override suspend fun get(id: ID): E? {
        val key = serializeKey(id)
        return cache.getIfPresent(key)?.await()
            ?: findByIdFromDb(id)?.also { cache.put(key, CompletableFuture.completedFuture(it)) }
    }
}
```

---

## Section 6: testFixtures 전략

### 재사용 가능성

기존 `exposed-cache`의 testFixtures 시나리오는 base 인터페이스(`JdbcCacheRepository`, `SuspendedJdbcCacheRepository`,
`R2dbcCacheRepository`)만 참조하며, `invalidateByPattern`을 호출하지 않음 → **Caffeine 구현에 그대로 재사용 가능**.

### DB 테스트 범위

| 모듈                       | 테스트 DB                           |
|--------------------------|----------------------------------|
| `exposed-jdbc-caffeine`  | **H2만** (Redis 불필요, 로컬 캐시)       |
| `exposed-r2dbc-caffeine` | **H2만** (Redis 불필요, 로컬 캐시)       |
| `exposed-jdbc-lettuce`   | H2 + Postgres + MySQL_V8 (기존 유지) |
| `exposed-jdbc-redisson`  | H2 + Postgres + MySQL_V8 (기존 유지) |
| `exposed-r2dbc-lettuce`  | H2 + Postgres + MySQL_V8 (기존 유지) |
| `exposed-r2dbc-redisson` | H2 + Postgres + MySQL_V8 (기존 유지) |

### Caffeine 테스트 모듈 구성

```kotlin
// exposed-jdbc-caffeine 테스트 (H2만, Redis Testcontainers 불필요)
class ActorReadThroughCaffeineTest: JdbcReadThroughScenario<Long, ActorRecord> {
    // ActorCaffeineRepository extends AbstractJdbcCaffeineRepository
}
class ActorWriteThroughCaffeineTest: JdbcWriteThroughScenario<Long, ActorRecord> { ... }
class ActorWriteBehindCaffeineTest: JdbcWriteBehindScenario<Long, ActorRecord> { ... }
```

---

## Section 7: 구현 태스크 목록

### Phase 1: 리네이밍 + 인터페이스 정리 (exposed-cache)

| #    | 태스크                                                                                                                                                      | complexity |
|------|----------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| 1.1  | `data/exposed-redis-api/` → `data/exposed-cache/` 디렉토리 이름 변경                                                                                             | low        |
| 1.2  | `CacheMode`에 `LOCAL` 값 추가 + KDoc (NEAR_CACHE 유지)                                                                                                         | low        |
| 1.3  | `JdbcCacheRepository`에서 `invalidateByPattern` 제거                                                                                                         | medium     |
| 1.4  | `SuspendedJdbcCacheRepository`에서 `invalidateByPattern` 제거                                                                                                | medium     |
| 1.5  | `R2dbcCacheRepository`에서 `invalidateByPattern` 제거                                                                                                        | medium     |
| 1.6  | `JdbcRedisRepository`, `SuspendJdbcRedisRepository`, `R2dbcRedisRepository` 신규 인터페이스 작성                                                                  | medium     |
| 1.7  | `LocalCacheConfig` 작성 (open class, Serializable, KLogging)                                                                                               | low        |
| 1.8  | `RedisRepositoryResilienceConfig` 작성 (data class, optional)                                                                                              | low        |
| 1.9  | `exposed-jdbc-lettuce/redisson`, `exposed-r2dbc-lettuce/redisson` build.gradle.kts 의존성 업데이트 (`exposed-redis-api` → `exposed-cache`)                      | low        |
| 1.10 | `exposed-jdbc-lettuce/redisson`, `exposed-r2dbc-lettuce/redisson` 구현체 부모 인터페이스 변경 (`Jdbc/R2dbc/SuspendJdbc*` → `JdbcRedis/R2dbcRedis/SuspendJdbcRedis*`) | medium     |
| 1.11 | 모듈 로컬 테스트에서 `invalidateByPattern` 호출부 타입 업데이트 (`Redis*Repository`로 변경)                                                                                   | medium     |
| 1.12 | `CacheMode.LOCAL` 추가로 인한 기존 `when` 표현식 검토 및 수정                                                                                                           | low        |
| 1.13 | `exposed-cache` `README.md` + `README.ko.md` 업데이트                                                                                                        | low        |

### Phase 2: exposed-jdbc-caffeine 신규 모듈

| #    | 태스크                                                                                  | complexity |
|------|--------------------------------------------------------------------------------------|------------|
| 2.1  | 모듈 디렉토리 + `build.gradle.kts` 생성                                                      | low        |
| 2.2  | `CaffeineCacheConfig` 작성 (`LocalCacheConfig` 상속, `recordStats` 등 추가)                 | low        |
| 2.3  | `JdbcCaffeineRepository` 인터페이스 작성                                                    | medium     |
| 2.4  | `AbstractJdbcCaffeineRepository` Read-Through / Write-Through 구현                     | high       |
| 2.5  | `AbstractJdbcCaffeineRepository` Write-Behind 구현 (Channel 기반 배치 큐 + `close()` drain) | high       |
| 2.6  | `SuspendedJdbcCaffeineRepository` 인터페이스 작성                                           | medium     |
| 2.7  | `AbstractSuspendedJdbcCaffeineRepository` 구현 (`suspendedTransactionAsync` 패턴)        | high       |
| 2.8  | 테스트: 도메인 (`ActorTable`, `ActorRecord`, `ActorRepository`) + H2 설정                    | medium     |
| 2.9  | 테스트: ReadThrough / WriteThrough / WriteBehind 시나리오 (H2만)                             | medium     |
| 2.10 | 테스트: Suspended ReadThrough / WriteThrough / WriteBehind 시나리오                         | medium     |
| 2.11 | `README.md` + `README.ko.md` 작성                                                      | low        |

### Phase 3: exposed-r2dbc-caffeine 신규 모듈

| #   | 태스크                                                                           | complexity |
|-----|-------------------------------------------------------------------------------|------------|
| 3.1 | 모듈 디렉토리 + `build.gradle.kts` 생성                                               | low        |
| 3.2 | `R2dbcCaffeineRepository` 인터페이스 작성                                            | medium     |
| 3.3 | `AbstractR2dbcCaffeineRepository` `AsyncCache` 기반 구현 (`cacheMode = LOCAL` 고정) | high       |
| 3.4 | 테스트: 도메인 + H2 설정 (R2DBC)                                                      | medium     |
| 3.5 | 테스트: R2DBC ReadThrough / WriteThrough / WriteBehind 시나리오                      | medium     |
| 3.6 | `README.md` + `README.ko.md` 작성                                               | low        |

### Phase 4: 후처리

| #   | 태스크                                       | complexity |
|-----|-------------------------------------------|------------|
| 4.1 | `CLAUDE.md` Exposed 모듈 테이블 업데이트           | low        |
| 4.2 | `docs/testlogs/2026-04.md` 테스트 결과 기록      | low        |
| 4.3 | `docs/superpowers/index/2026-04.md` 항목 추가 | low        |
| 4.4 | `docs/superpowers/INDEX.md` 건수 갱신         | low        |
