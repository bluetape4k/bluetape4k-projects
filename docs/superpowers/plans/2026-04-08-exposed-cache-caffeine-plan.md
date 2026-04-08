# exposed-cache 리네이밍 + Caffeine 로컬 캐시 모듈 구현 플랜

- **작성일**: 2026-04-08
- **스펙**: `docs/superpowers/specs/2026-04-07-exposed-cache-repository-caffeine-design.md`
- **예상 복잡도**: HIGH (4개 Phase, 신규 모듈 2개 + 기존 모듈 5개 변경)

---

## Phase 1: 리네이밍 + 인터페이스 정리 (exposed-cache)

> **선행 조건**: 없음
> **Gradle 빌드 검증**: Phase 1 완료 후
`./gradlew :bluetape4k-exposed-cache:build :bluetape4k-exposed-jdbc-lettuce:build :bluetape4k-exposed-jdbc-redisson:build :bluetape4k-exposed-r2dbc-lettuce:build :bluetape4k-exposed-r2dbc-redisson:build`

### Task 1.1: 디렉토리 리네이밍 + Gradle 의존성 업데이트

- **complexity**: low
- **depends_on**: -
- **변경 파일**:
    - `data/exposed-redis-api/` → `data/exposed-cache/` (디렉토리 이름 변경, `mv`)
    - `data/exposed-jdbc-lettuce/build.gradle.kts`: `project(":bluetape4k-exposed-redis-api")` →
      `project(":bluetape4k-exposed-cache")`, `testFixtures(project(":bluetape4k-exposed-redis-api"))` →
      `testFixtures(project(":bluetape4k-exposed-cache"))`
    - `data/exposed-jdbc-redisson/build.gradle.kts`: 동일 패턴
    - `data/exposed-r2dbc-lettuce/build.gradle.kts`: 동일 패턴
    - `data/exposed-r2dbc-redisson/build.gradle.kts`: 동일 패턴
- **검증 기준**: `./gradlew :bluetape4k-exposed-cache:compileKotlin` 성공, 기존 4개 모듈의 의존성 resolve 성공

### Task 1.2: CacheMode에 LOCAL 값 추가

- **complexity**: low
- **depends_on**: 1.1
- **변경 파일**:
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/CacheMode.kt`
        - `LOCAL` enum 값 추가 (KDoc 포함: "인프로세스 캐시만 사용 (Caffeine/Cache2k)")
        - 기존 `REMOTE`, `NEAR_CACHE` 유지
- **검증 기준**: 컴파일 성공, 기존 `when` 표현식에 `LOCAL` 분기 누락 없는지 확인 (`ide_diagnostics` 또는 Grep으로 `when.*cacheMode` 검색)

### Task 1.3: 기본 인터페이스에서 invalidateByPattern 제거 + Redis 전용 인터페이스 생성

- **complexity**: medium
- **depends_on**: 1.1
- **변경 파일** (인터페이스 변경):
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/JdbcCacheRepository.kt`
        - `invalidateByPattern(patterns: String, count: Int): Long` 메서드 제거
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/SuspendedJdbcCacheRepository.kt`
        - `suspend fun invalidateByPattern(...)` 제거
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/R2dbcCacheRepository.kt`
        - `suspend fun invalidateByPattern(...)` 제거
- **신규 파일** (Redis 전용 인터페이스):
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/redis/JdbcRedisRepository.kt`
      ```kotlin
      interface JdbcRedisRepository<ID: Any, E: Serializable> : JdbcCacheRepository<ID, E> {
          fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long
      }
      ```
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/redis/SuspendJdbcRedisRepository.kt`
      ```kotlin
      interface SuspendJdbcRedisRepository<ID: Any, E: Serializable> : SuspendedJdbcCacheRepository<ID, E> {
          suspend fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long
      }
      ```
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/redis/R2dbcRedisRepository.kt`
      ```kotlin
      interface R2dbcRedisRepository<ID: Any, E: Serializable> : R2dbcCacheRepository<ID, E> {
          suspend fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long
      }
      ```
- **검증 기준**: `exposed-cache` 컴파일 성공

### Task 1.4: Lettuce/Redisson 구현체 부모 인터페이스 변경

- **complexity**: medium
- **depends_on**: 1.3
- **변경 파일**:
    - `data/exposed-jdbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/lettuce/repository/JdbcLettuceRepository.kt`
        - 부모 인터페이스: `JdbcCacheRepository` → `JdbcRedisRepository`
        - import 추가: `io.bluetape4k.exposed.cache.redis.JdbcRedisRepository`
    -
    `data/exposed-jdbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/lettuce/repository/SuspendedJdbcLettuceRepository.kt`
        - 부모 인터페이스: `SuspendedJdbcCacheRepository` → `SuspendJdbcRedisRepository`
    - `data/exposed-jdbc-redisson/` 내 동일 패턴 적용 (JdbcRedissonRepository, SuspendedJdbcRedissonRepository)
    - `data/exposed-r2dbc-lettuce/` 내 R2dbcLettuceRepository → `R2dbcRedisRepository`
    - `data/exposed-r2dbc-redisson/` 내 R2dbcRedissonRepository → `R2dbcRedisRepository`
- **검증 기준**: 4개 모듈 모두 `compileKotlin` 성공, `invalidateByPattern` 메서드 정상 override

### Task 1.5: LocalCacheConfig + RedisRepositoryResilienceConfig 작성

- **complexity**: low
- **depends_on**: 1.1
- **신규 파일**:
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/LocalCacheConfig.kt`
      ```kotlin
      open class LocalCacheConfig(
          val keyPrefix: String = "local",
          val maximumSize: Long = 10_000L,
          val expireAfterWrite: Duration = Duration.ofMinutes(10),
          val expireAfterAccess: Duration? = null,
          val writeMode: CacheWriteMode = CacheWriteMode.READ_ONLY,
          val writeBehindBatchSize: Int = 100,
          val writeBehindQueueCapacity: Int = 10_000,
      ) : Serializable {
          companion object : KLogging() {
              private const val serialVersionUID = 1L
          }
      }
      ```
    - `data/exposed-cache/src/main/kotlin/io/bluetape4k/exposed/cache/redis/RedisRepositoryResilienceConfig.kt`
      ```kotlin
      data class RedisRepositoryResilienceConfig(
          val retryMaxAttempts: Int = 3,
          val retryWaitDuration: Duration = Duration.ofMillis(500),
          val retryExponentialBackoff: Boolean = true,
          val circuitBreakerEnabled: Boolean = false,
          val timeoutDuration: Duration = Duration.ofSeconds(2),
      ) : Serializable { ... }
      ```
- **검증 기준**: 컴파일 성공

### Task 1.6: 테스트 코드 타입 업데이트

- **complexity**: medium
- **depends_on**: 1.4
- **변경 대상**: Lettuce/Redisson 테스트 코드에서 `invalidateByPattern` 호출부 타입 업데이트
    - Grep `invalidateByPattern` → 호출 위치에서 `repository` 변수 타입을 `JdbcRedisRepository`(또는 `R2dbcRedisRepository`,
      `SuspendJdbcRedisRepository`)로 변경
    - ⚠️ **`when(cacheMode)` 수정 불필요**: Lettuce/Redisson 구현체의 `when` 블록은
      `RedissonCacheConfig.CacheMode`(자체 nested enum)를 사용하므로 최상위 `CacheMode.LOCAL` 추가와 무관함. 수정 대상 없음.
- **검증 기준**:
  `./gradlew :bluetape4k-exposed-cache:build :bluetape4k-exposed-jdbc-lettuce:test :bluetape4k-exposed-jdbc-redisson:test :bluetape4k-exposed-r2dbc-lettuce:test :bluetape4k-exposed-r2dbc-redisson:test` 통과

### Task 1.7: exposed-cache README 업데이트

- **complexity**: low
- **depends_on**: 1.6
- **변경 파일**:
    - `data/exposed-cache/README.md` — 모듈명 변경 반영, Redis 전용 인터페이스 설명 추가, LocalCacheConfig 설명
    - `data/exposed-cache/README.ko.md` — 동일 내용 한국어

---

## Phase 2: exposed-jdbc-caffeine 신규 모듈

> **선행 조건**: Phase 1 완료
> **Phase 3과 병렬 진행 가능**
> **Gradle 빌드 검증**: `./gradlew :bluetape4k-exposed-jdbc-caffeine:build`

### Task 2.1: 모듈 디렉토리 + build.gradle.kts 생성

- **complexity**: low
- **depends_on**: 1.6 (Phase 1 완료)
- **신규 파일**: `data/exposed-jdbc-caffeine/build.gradle.kts`
  ```kotlin
  configurations {
      testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
  }
  dependencies {
      api(project(":bluetape4k-exposed-jdbc"))
      api(project(":bluetape4k-exposed-cache"))
      api(project(":bluetape4k-coroutines"))
      api(Libs.caffeine)

      // Exposed
      api(Libs.exposed_core)
      api(Libs.exposed_jdbc)
      compileOnly(Libs.exposed_java_time)

      // Coroutines
      api(Libs.kotlinx_coroutines_core)

      testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
      testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
      testImplementation(project(":bluetape4k-junit5"))
      testImplementation(Libs.h2_v2)
      testImplementation(Libs.hikaricp)
      testImplementation(Libs.kotlinx_coroutines_test)
  }
  ```
- **검증 기준**: `./gradlew :bluetape4k-exposed-jdbc-caffeine:dependencies` 성공

### Task 2.2: CaffeineCacheConfig 작성

- **complexity**: low
- **depends_on**: 2.1
- **신규 파일**: `data/exposed-jdbc-caffeine/src/main/kotlin/io/bluetape4k/exposed/caffeine/CaffeineCacheConfig.kt`
    - `LocalCacheConfig` 상속
    - Caffeine 전용 옵션: `recordStats: Boolean = false`
    - `Serializable` + `KLogging` + `serialVersionUID` 규칙 준수
    - 프리셋 companion 상수: `READ_ONLY`, `WRITE_THROUGH`, `WRITE_BEHIND`
- **검증 기준**: 컴파일 성공

### Task 2.3: JdbcCaffeineRepository 인터페이스 + AbstractJdbcCaffeineRepository 구현

- **complexity**: high
- **depends_on**: 2.2
- **신규 파일**:
    - `data/exposed-jdbc-caffeine/src/main/kotlin/io/bluetape4k/exposed/caffeine/repository/JdbcCaffeineRepository.kt`
        - `JdbcCacheRepository<ID, E>` 확장
        - `val config: LocalCacheConfig`, `val cache: Cache<String, E>` 프로퍼티
    -
    `data/exposed-jdbc-caffeine/src/main/kotlin/io/bluetape4k/exposed/caffeine/repository/AbstractJdbcCaffeineRepository.kt`
        - 추상 멤버: `table`, `ResultRow.toEntity()`, `UpdateStatement.updateEntity()`,
          `BatchInsertStatement.insertEntity()`
        - `cacheMode = CacheMode.LOCAL` 고정
        - **Caffeine Cache 빌더**: `LocalCacheConfig` → `Caffeine.newBuilder()` 변환
          ```kotlin
          override val cache: Cache<String, E> by lazy {
              Caffeine.newBuilder()
                  .maximumSize(config.maximumSize)
                  .expireAfterWrite(config.expireAfterWrite)
                  .apply {
                      config.expireAfterAccess?.let { expireAfterAccess(it) }
                      if (config is CaffeineCacheConfig && config.recordStats) recordStats()
                  }
                  .build()
          }
          ```
        - **Read-Through**: `get(id)` → `cache.getIfPresent(key) ?: findByIdFromDb(id)?.also { cache.put(key, it) }`
        - **`getAll(ids)`**: 캐시 히트 분리 → 미스만 DB 조회 → 결과 캐시 저장 후 병합 반환
        - **Write-Through**: `put(id, entity)` → `cache.put(key, entity)` + `transaction { updateEntity/insertEntity }`
        - **`putAll(entities, batchSize)`**: entries 순회 → 각 put dispatch (write mode 따라)
        - **`containsKey(id)`**: `get(id) != null`
        - **DB 직접 조회**: `findByIdFromDb`, `findAllFromDb`, `countFromDb`, `findAll` —
          `transaction { ... }` 패턴 (Lettuce 구현체 참조)
        - `serializeKey(id: ID): String = id.toString()` (open, 오버라이드 가능)
        - `extractId(entity)` — 기본 error() + 오버라이드 권장 (Lettuce 패턴 동일)
        - `invalidate(id)` → `cache.invalidate(key)`, `invalidateAll(ids)` → ids.forEach, `clear()` →
          `cache.invalidateAll()`
- **검증 기준**: Read-Through + Write-Through 구현 완료, 컴파일 성공

### Task 2.4: Write-Behind 구현 (Channel 기반 배치 큐)

- **complexity**: high
- **depends_on**: 2.3
- **변경 파일**: `AbstractJdbcCaffeineRepository.kt` 내부에 Write-Behind 로직 추가
- **설계 세부사항**:
  ```kotlin
  // CoroutineScope (Dispatchers.IO, SupervisorJob)
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  // Channel: 용량 제한 (config.writeBehindQueueCapacity)
  private val writeBehindQueue = Channel<Pair<ID, E>>(capacity = config.writeBehindQueueCapacity)

  // Consumer Job: chunked 배치 처리
  private val writeBehindJob = scope.launch {
      writeBehindQueue.consumeAsFlow()
          .chunked(config.writeBehindBatchSize)  // bluetape4k-coroutines 의존
          .collect { batch ->
              runCatching {
                  transaction {
                      batch.forEach { (id, entity) ->
                          table.upsert(id, entity)  // 또는 batchInsert/update
                      }
                  }
              }.onFailure { log.error(it) { "Write-Behind DB 쓰기 실패: ${batch.size}건" } }
          }
  }

  // writeBehind 호출
  private fun writeBehind(id: ID, entity: E) {
      writeBehindQueue.trySend(id to entity)
          .onFailure { log.warn { "Write-Behind 큐 포화: 항목 드랍" } }
  }
  ```
- **close() drain 로직**:
  ```kotlin
  override fun close() {
      if (config.writeMode == CacheWriteMode.WRITE_BEHIND) {
          writeBehindQueue.close()           // 새 항목 추가 차단
          runBlocking { writeBehindJob.join() }  // 남은 배치 DB 반영 대기
      }
      cache.invalidateAll()
      scope.cancel()
  }
  ```
- **에러 처리**: `runCatching` + 로그 기록, 재시도 없음 (설계 원칙: 로컬 캐시는 최종 일관성 허용)
- **검증 기준**: Write-Behind 시나리오 테스트에서 close() 후 데이터 유실 없음 확인

### Task 2.5: SuspendedJdbcCaffeineRepository 인터페이스 + AbstractSuspendedJdbcCaffeineRepository 구현

- **complexity**: high
- **depends_on**: 2.4
- **신규 파일**:
    -
    `data/exposed-jdbc-caffeine/src/main/kotlin/io/bluetape4k/exposed/caffeine/repository/SuspendedJdbcCaffeineRepository.kt`
        - `SuspendedJdbcCacheRepository<ID, E>` 확장
    -
    `data/exposed-jdbc-caffeine/src/main/kotlin/io/bluetape4k/exposed/caffeine/repository/AbstractSuspendedJdbcCaffeineRepository.kt`
        - DB I/O: `suspendedTransactionAsync(Dispatchers.IO) { ... }.await()` 패턴 (Lettuce suspended 구현체 참조)
        - Caffeine `Cache<String, E>` 사용 (동기 Cache — JDBC 기반이므로 `AsyncCache` 불필요)
        - Write-Behind: 동일 Channel 패턴이지만 `send()` (suspend) 사용
        - `close()`: `writeBehindQueue.close()` + `writeBehindJob.join()` (suspend 컨텍스트 아니므로 `runBlocking`)
- **검증 기준**: 컴파일 성공, suspend 시그니처 일치

### Task 2.6: 테스트 도메인 + H2 설정

- **complexity**: medium
- **depends_on**: 2.5
- **신규 파일**:
    - `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/domain/ActorSchema.kt`
        - Lettuce 테스트의 `ActorSchema` 구조 재사용 (UserTable + ActorRecord + withUserTable 등)
        - Redis 관련 설정 제거, H2 전용
    - `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/domain/ActorCaffeineRepository.kt`
        - `AbstractJdbcCaffeineRepository<Long, ActorRecord>` 구현
        - `table`, `toEntity()`, `updateEntity()`, `insertEntity()`, `extractId()` 구현
    -
    `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/domain/SuspendedActorCaffeineRepository.kt`
        - `AbstractSuspendedJdbcCaffeineRepository<Long, ActorRecord>` 구현
    - `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/AbstractJdbcCaffeineTest.kt`
        - `getEnabledDialects()` → `listOf(TestDB.H2_V2)`
        - Caffeine 기반이므로 Redis Testcontainers 불필요
- **검증 기준**: 테스트 도메인 컴파일 성공

### Task 2.7: 테스트 시나리오 (ReadThrough / WriteThrough / WriteBehind)

- **complexity**: medium
- **depends_on**: 2.6
- **신규 파일**:
    - `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/repository/ReadThroughCacheTest.kt`
        - testFixtures의 `JdbcReadThroughScenario<Long, ActorRecord>` 구현
        - `cacheWriteMode = CacheWriteMode.READ_ONLY`
    - `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/repository/WriteThroughCacheTest.kt`
        - testFixtures의 `JdbcWriteThroughScenario<Long, ActorRecord>` 구현
    - `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/repository/WriteBehindCacheTest.kt`
        - testFixtures의 `JdbcWriteBehindScenario<Long, ActorRecord>` 구현
        - **추가 검증**: `close()` 호출 후 DB 반영 확인, 큐 drain 테스트
    -
    `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/repository/SuspendedReadThroughCacheTest.kt`
        - testFixtures의 `SuspendedJdbcReadThroughScenario` 구현
    -
    `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/repository/SuspendedWriteThroughCacheTest.kt`
    -
    `data/exposed-jdbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/caffeine/repository/SuspendedWriteBehindCacheTest.kt`
- **검증 기준**: `./gradlew :bluetape4k-exposed-jdbc-caffeine:test` 전체 통과 (H2만)

### Task 2.8: README 작성

- **complexity**: low
- **depends_on**: 2.7
- **신규 파일**:
    - `data/exposed-jdbc-caffeine/README.md` (영어)
    - `data/exposed-jdbc-caffeine/README.ko.md` (한국어)
    - Architecture → UML → Features → Examples 순서

---

## Phase 3: exposed-r2dbc-caffeine 신규 모듈

> **선행 조건**: Phase 1 완료
> **Phase 2와 병렬 진행 가능**
> **Gradle 빌드 검증**: `./gradlew :bluetape4k-exposed-r2dbc-caffeine:build`

### Task 3.1: 모듈 디렉토리 + build.gradle.kts 생성

- **complexity**: low
- **depends_on**: 1.6 (Phase 1 완료)
- **신규 파일**: `data/exposed-r2dbc-caffeine/build.gradle.kts`
  ```kotlin
  configurations {
      testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
  }
  dependencies {
      api(project(":bluetape4k-exposed-r2dbc"))
      api(project(":bluetape4k-exposed-cache"))
      api(project(":bluetape4k-coroutines"))
      api(Libs.caffeine)

      // Exposed R2DBC
      api(Libs.exposed_core)
      api(Libs.exposed_r2dbc)
      compileOnly(Libs.exposed_java_time)
      compileOnly(Libs.exposed_kotlin_datetime)

      // Coroutines
      api(Libs.kotlinx_coroutines_core)
      api(Libs.kotlinx_coroutines_reactive)

      // R2DBC drivers (test)
      testRuntimeOnly(Libs.r2dbc_h2)

      testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
      testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
      testImplementation(project(":bluetape4k-junit5"))
      testImplementation(Libs.h2_v2)
      testImplementation(Libs.kotlinx_coroutines_test)
  }
  ```
    - **JDBC 의존 없음** 확인
- **검증 기준**: `./gradlew :bluetape4k-exposed-r2dbc-caffeine:dependencies` 성공

### Task 3.2: R2dbcCaffeineRepository 인터페이스 + AbstractR2dbcCaffeineRepository 구현

- **complexity**: high
- **depends_on**: 3.1
- **신규 파일**:
    -
    `data/exposed-r2dbc-caffeine/src/main/kotlin/io/bluetape4k/exposed/r2dbc/caffeine/repository/R2dbcCaffeineRepository.kt`
        - `R2dbcCacheRepository<ID, E>` 확장
        - `val config: LocalCacheConfig`, `val cache: AsyncCache<String, E>` 프로퍼티
    -
    `data/exposed-r2dbc-caffeine/src/main/kotlin/io/bluetape4k/exposed/r2dbc/caffeine/repository/AbstractR2dbcCaffeineRepository.kt`
        - **AsyncCache 사용** (non-blocking):
          ```kotlin
          override val cache: AsyncCache<String, E> by lazy {
              Caffeine.newBuilder()
                  .maximumSize(config.maximumSize)
                  .expireAfterWrite(config.expireAfterWrite)
                  .apply { config.expireAfterAccess?.let { expireAfterAccess(it) } }
                  .buildAsync()
          }
          ```
        - `cacheMode = CacheMode.LOCAL` 고정
        - `get(id)`:
          `cache.getIfPresent(key)?.await() ?: findByIdFromDb(id)?.also { cache.put(key, completedFuture(it)) }`
        - DB I/O: R2DBC 네이티브 suspend API 사용 (`newSuspendedTransaction` 등)
        - **`getAll(ids)`**, **`putAll(entities)`**, **`containsKey(id)`** 구현 포함 (JDBC Caffeine 구현과 동일 로직, suspend 버전)
        - Write-Behind: Channel 패턴 (suspend `send()`)
        - `close()`:
          ```kotlin
          writeBehindQueue.close()
          runBlocking { writeBehindJob.join() }
          cache.synchronous().invalidateAll()
          scope.cancel()  // CoroutineScope 정리 (JDBC 구현체와 동일)
          ```
- **검증 기준**: 컴파일 성공, JDBC 의존 없음 (`ide_diagnostics` 확인)

### Task 3.3: 테스트 도메인 + H2 R2DBC 설정

- **complexity**: medium
- **depends_on**: 3.2
- **신규 파일**:
    - `data/exposed-r2dbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/r2dbc/caffeine/domain/ActorSchema.kt`
        - R2DBC 용 UserTable + ActorRecord + withUserTable(suspend)
    -
    `data/exposed-r2dbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/r2dbc/caffeine/domain/ActorR2dbcCaffeineRepository.kt`
        - `AbstractR2dbcCaffeineRepository<Long, ActorRecord>` 구현
    - `data/exposed-r2dbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/r2dbc/caffeine/AbstractR2dbcCaffeineTest.kt`
- **검증 기준**: 테스트 도메인 컴파일 성공

### Task 3.4: 테스트 시나리오 (R2DBC ReadThrough / WriteThrough / WriteBehind)

- **complexity**: medium
- **depends_on**: 3.3
- **신규 파일**:
    -
    `data/exposed-r2dbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/r2dbc/caffeine/repository/ReadThroughCacheTest.kt`
        - testFixtures의 `R2dbcReadThroughScenario<Long, ActorRecord>` 구현
    -
    `data/exposed-r2dbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/r2dbc/caffeine/repository/WriteThroughCacheTest.kt`
    -
    `data/exposed-r2dbc-caffeine/src/test/kotlin/io/bluetape4k/exposed/r2dbc/caffeine/repository/WriteBehindCacheTest.kt`
- **검증 기준**: `./gradlew :bluetape4k-exposed-r2dbc-caffeine:test` 전체 통과 (H2만)

### Task 3.5: README 작성

- **complexity**: low
- **depends_on**: 3.4
- **신규 파일**:
    - `data/exposed-r2dbc-caffeine/README.md` (영어)
    - `data/exposed-r2dbc-caffeine/README.ko.md` (한국어)

---

## Phase 4: 후처리

> **선행 조건**: Phase 2 + Phase 3 완료
> **Gradle 빌드 검증**: `./gradlew build -x test` (전체 빌드)

### Task 4.1: CLAUDE.md 업데이트

- **complexity**: low
- **depends_on**: 2.7, 3.4
- **변경 파일**: `CLAUDE.md`
    - Exposed 모듈 테이블에 추가:
        - `exposed-cache` (구 `exposed-redis-api`)
        - `exposed-jdbc-caffeine`
        - `exposed-r2dbc-caffeine`
    - `exposed-redis-api` 참조 제거

### Task 4.2: bluetape4k-patterns 체크리스트 검증

- **complexity**: low
- **depends_on**: 4.1
- **확인 항목**:
    - `LocalCacheConfig`, `CaffeineCacheConfig`, `RedisRepositoryResilienceConfig`: `Serializable` +
      `companion object : KLogging()` + `serialVersionUID = 1L`
    - 모든 public 클래스·인터페이스·확장 함수: **한국어 KDoc** 존재 여부
    - `AbstractJdbcCaffeineRepository`, `AbstractSuspendedJdbcCaffeineRepository`, `AbstractR2dbcCaffeineRepository`:
      `companion object : KLogging()` 포함
    - Caffeine Abstract 클래스 생성자에서 `config.cacheMode` 강제 검증 없이 `LOCAL` 고정 확인
    - `JdbcRedisRepository`, `SuspendJdbcRedisRepository`, `R2dbcRedisRepository` 인터페이스 KDoc 완비
- **검증 방법**: `ide_diagnostics` + KDoc grep

### Task 4.3: docs 업데이트

- **complexity**: low
- **depends_on**: 4.2
- **변경 파일**:
    - `docs/testlogs/2026-04.md` — 테스트 결과 기록 (맨 위 행 추가)
    - `docs/superpowers/index/2026-04.md` — 항목 추가
    - `docs/superpowers/INDEX.md` — 건수 갱신

---

## Phase별 병렬 실행 가능 태스크 그룹

| 실행 순서   | 태스크              | 비고                                       |
|---------|------------------|------------------------------------------|
| Step 1  | 1.1              | 디렉토리 리네이밍 (단독)                           |
| Step 2  | 1.2, 1.3, 1.5    | CacheMode LOCAL + 인터페이스 분리 + Config (병렬) |
| Step 3  | 1.4              | 구현체 부모 변경 (1.3 의존)                       |
| Step 4  | 1.6              | 테스트 코드 타입 업데이트                           |
| Step 5  | 1.7              | exposed-cache README (1.6 완료 후)          |
| Step 6  | 2.1, 3.1         | 두 신규 모듈 디렉토리 생성 (병렬)                     |
| Step 7  | 2.2~2.5, 3.2     | JDBC 구현 + R2DBC 구현 (병렬)                  |
| Step 8  | 2.6~2.7, 3.3~3.4 | 테스트 작성 (병렬)                              |
| Step 9  | 2.8, 3.5         | README (병렬)                              |
| Step 10 | 4.1              | CLAUDE.md 업데이트                           |
| Step 11 | 4.2              | docs 업데이트 (4.1 완료 후)                     |

---

## 태스크 요약

| #   | 태스크                                                          | Complexity | Depends On |
|-----|--------------------------------------------------------------|------------|------------|
| 1.1 | 디렉토리 리네이밍 + Gradle 의존성 업데이트                                  | low        | -          |
| 1.2 | CacheMode에 LOCAL 값 추가                                        | low        | 1.1        |
| 1.3 | invalidateByPattern 분리 + Redis 전용 인터페이스 생성                   | medium     | 1.1        |
| 1.4 | Lettuce/Redisson 구현체 부모 인터페이스 변경                             | medium     | 1.3        |
| 1.5 | LocalCacheConfig + RedisRepositoryResilienceConfig 작성        | low        | 1.1        |
| 1.6 | 테스트 코드 타입 업데이트 + when 표현식 수정                                 | medium     | 1.4        |
| 1.7 | exposed-cache README 업데이트                                    | low        | 1.6        |
| 2.1 | exposed-jdbc-caffeine 모듈 생성                                  | low        | 1.6        |
| 2.2 | CaffeineCacheConfig 작성                                       | low        | 2.1        |
| 2.3 | AbstractJdbcCaffeineRepository Read-Through/Write-Through 구현 | high       | 2.2        |
| 2.4 | Write-Behind 구현 (Channel + close drain)                      | high       | 2.3        |
| 2.5 | AbstractSuspendedJdbcCaffeineRepository 구현                   | high       | 2.4        |
| 2.6 | 테스트 도메인 + H2 설정                                              | medium     | 2.5        |
| 2.7 | 테스트 시나리오 6종 (JDBC sync + suspend)                            | medium     | 2.6        |
| 2.8 | exposed-jdbc-caffeine README 작성                              | low        | 2.7        |
| 3.1 | exposed-r2dbc-caffeine 모듈 생성                                 | low        | 1.6        |
| 3.2 | AbstractR2dbcCaffeineRepository AsyncCache 구현                | high       | 3.1        |
| 3.3 | 테스트 도메인 + H2 R2DBC 설정                                        | medium     | 3.2        |
| 3.4 | 테스트 시나리오 3종 (R2DBC)                                          | medium     | 3.3        |
| 3.5 | exposed-r2dbc-caffeine README 작성                             | low        | 3.4        |
| 4.1 | CLAUDE.md 업데이트                                               | low        | 2.7, 3.4   |
| 4.2 | bluetape4k-patterns 체크리스트 검증                                 | low        | 4.1        |
| 4.3 | docs 업데이트 (testlogs, superpowers)                            | low        | 4.2        |

**총 23개 태스크**: high 4 / medium 8 / low 11
