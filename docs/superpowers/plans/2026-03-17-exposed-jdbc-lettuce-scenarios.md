# exposed-jdbc-lettuce Scenario Tests Implementation Plan

> **For agentic workers:
** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (
`- [ ]`) syntax for tracking.

**Goal:** `exposed-jdbc-redisson`의 `repository/scenarios` 패턴을
`exposed-jdbc-lettuce`에 이식하여 Read-through / Write-through / Write-behind 캐시 전략별 시나리오 테스트를 작성한다.

**Architecture:**

- Scenario interface 계층: `CacheTestScenario` → `ReadThroughScenario` / `WriteThroughScenario` /
  `WriteBehindScenario` (sync + suspend 각 4개)
- Concrete 테스트 클래스: `UserRepository` / `SuspendedUserRepository` 기반으로 각 시나리오 구현
- `JdbcLettuceRepository` API (save/delete/findById) 기반 — Redisson의 put/invalidate와 다름

**Tech Stack:** Kotlin, JUnit 5, Kluent assertions, H2 in-memory DB, Testcontainers Redis, Lettuce `LettuceLoadedMap`,
`kotlinx-coroutines-test`

---

## 파일 구조

### 신규 생성 파일

```
data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/
└── repository/
    └── scenarios/
        ├── CacheTestScenario.kt               # sync base interface
        ├── ReadThroughScenario.kt             # sync read-through tests
        ├── WriteThroughScenario.kt            # sync write-through tests
        ├── WriteBehindScenario.kt             # sync write-behind tests
        ├── SuspendedCacheTestScenario.kt      # suspend base interface
        ├── SuspendedReadThroughScenario.kt    # suspend read-through tests
        ├── SuspendedWriteThroughScenario.kt   # suspend write-through tests
        └── SuspendedWriteBehindScenario.kt    # suspend write-behind tests
    ├── UserReadThroughCacheTest.kt            # concrete: READ_ONLY + ReadThroughScenario
    ├── UserWriteThroughCacheTest.kt           # concrete: WRITE_THROUGH + WriteThroughScenario
    ├── UserWriteBehindCacheTest.kt            # concrete: WRITE_BEHIND + WriteBehindScenario
    ├── SuspendedUserReadThroughCacheTest.kt   # suspend concrete: READ_ONLY
    ├── SuspendedUserWriteThroughCacheTest.kt  # suspend concrete: WRITE_THROUGH
    └── SuspendedUserWriteBehindCacheTest.kt   # suspend concrete: WRITE_BEHIND
```

### 기존 파일 (수정 불필요)

- `AbstractJdbcLettuceRepositoryTest.kt` — `redisClient` 제공 (ShutdownQueue 등록)
- `domain/UserRepository.kt`, `domain/SuspendedUserRepository.kt` — concrete repo
- `domain/UserSchema.kt` — `UserTable`, `UserRecord`, `newUserRecord()`

---

## Redisson vs Lettuce API 매핑

| Redisson                               | Lettuce (JdbcLettuceRepository)                    |
|----------------------------------------|----------------------------------------------------|
| `repository.get(id)`                   | `repository.findById(id)`                          |
| `repository.getAll(ids, batchSize)`    | `repository.findAll(ids)`                          |
| `repository.exists(id)`                | `repository.findById(id) != null`                  |
| `repository.put(entity)`               | `repository.save(id, entity)`                      |
| `repository.putAll(entities)`          | `repository.saveAll(entities.associateBy { ... })` |
| `repository.invalidate(id)`            | `repository.delete(id)`                            |
| `repository.invalidateAll()`           | `repository.clearCache()`                          |
| `withEntityTable(testDB) { ... }`      | `@BeforeEach` + `@AfterEach` (H2 고정)               |
| `getExistingId()` / `getExistingIds()` | 동일                                                 |
| `cacheConfig.isReadOnly`               | `config.writeMode == WriteMode.NONE`               |
| `cacheConfig.deleteFromDBOnInvalidate` | `config.writeMode != WriteMode.NONE`               |

---

## Task 1: CacheTestScenario (sync base)

**Files:**

- Create:
  `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/CacheTestScenario.kt`

- [ ] **Step 1: CacheTestScenario 인터페이스 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.lettuce.repository.JdbcLettuceRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.junit.jupiter.api.BeforeEach

/**
 * exposed-jdbc-lettuce 통합 테스트 시나리오 베이스 인터페이스.
 *
 * - `@BeforeEach setup()`에서 캐시를 비운다.
 * - 서브 인터페이스(ReadThroughScenario 등)가 테스트 메서드를 추가한다.
 * - 구현 클래스는 @BeforeAll setupDb(), @BeforeEach setupData(), @AfterEach tearDown()을 담당한다.
 */
interface CacheTestScenario<ID : Comparable<ID>, E : Any> {
    companion object : KLogging()

    /** 테스트 대상 레포지토리 */
    val repository: JdbcLettuceRepository<ID, E>

    /** 적용된 캐시 설정 */
    val config: LettuceCacheConfig

    /** DB에 존재하는 샘플 ID를 반환한다 */
    fun getExistingId(): ID

    /** DB에 존재하는 복수 샘플 ID를 반환한다 */
    fun getExistingIds(): List<ID>

    /** DB와 캐시 모두에 존재하지 않는 ID를 반환한다 */
    fun getNonExistentId(): ID

    @BeforeEach
    fun clearCacheBeforeEach() {
        repository.clearCache()
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew :bluetape4k-exposed-jdbc-lettuce:compileTestKotlin -x detekt
```

Expected: `BUILD SUCCESSFUL`

---

## Task 2: ReadThroughScenario (sync)

**Files:**

- Create:
  `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/ReadThroughScenario.kt`

- [ ] **Step 1: ReadThroughScenario 인터페이스 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Read-through 캐시 전략 시나리오.
 *
 * - 캐시 미스 시 DB에서 로드 후 캐시에 적재
 * - 캐시 삭제(delete) 시 DB에는 영향 없음 (ReadOnly 모드)
 */
interface ReadThroughScenario<ID : Comparable<ID>, E : Any> : CacheTestScenario<ID, E> {
    companion object : KLogging()

    @Test
    fun `findById - 캐시 미스 시 DB에서 Read-through로 값을 로드한다`() {
        val id = getExistingId()

        val fromDb = repository.findByIdFromDb(id)
        fromDb.shouldNotBeNull()

        repository.clearCache()
        val fromCache = repository.findById(id)
        fromCache.shouldNotBeNull()
        fromCache shouldBeEqualTo fromDb
    }

    @Test
    fun `findById - DB에 없는 ID는 null을 반환한다`() {
        repository.findById(getNonExistentId()).shouldBeNull()
    }

    @Test
    fun `findAll - 여러 ID를 일괄 조회하며 캐시 미스 키는 DB에서 Read-through한다`() {
        val ids = getExistingIds()
        val result = repository.findAll(ids)
        result.size shouldBeEqualTo ids.size
    }

    @Test
    fun `findAll - 존재하지 않는 ID는 결과에 포함되지 않는다`() {
        val ids = getExistingIds() + listOf(getNonExistentId())
        val result = repository.findAll(ids)
        result.size shouldBeEqualTo getExistingIds().size
    }

    @Test
    fun `clearCache - 캐시를 비운 후 재조회하면 DB에서 다시 Read-through한다`() {
        val id = getExistingId()
        repository.findById(id) // 캐시에 적재
        repository.clearCache()

        val found = repository.findById(id)
        found.shouldNotBeNull()
    }

    @Test
    fun `delete - 캐시에만 저장된 엔티티를 삭제하면 findById는 null을 반환한다`() {
        // READ_ONLY 모드에서 save()는 Redis에만 저장, DB에 쓰지 않음
        // → delete() 후 DB에도 없으므로 findById()는 null
        val id = getNonExistentId()
        val entity = buildEntityForId(id)
        repository.save(id, entity) // Redis에만 저장

        repository.delete(id)

        repository.findById(id).shouldBeNull()
    }

    /**
     * getNonExistentId()에 해당하는 엔티티를 생성해 반환한다 (DB에는 저장하지 않음).
     * 구현 클래스에서 override한다.
     */
    fun buildEntityForId(id: ID): E
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew :bluetape4k-exposed-jdbc-lettuce:compileTestKotlin -x detekt
```

---

## Task 3: WriteThroughScenario (sync)

**Files:**

- Create:
  `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/WriteThroughScenario.kt`

- [ ] **Step 1: WriteThroughScenario 인터페이스 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Write-through 캐시 전략 시나리오.
 *
 * - save() 시 캐시와 DB를 동시에 갱신
 * - delete() 시 캐시와 DB를 모두 삭제
 */
interface WriteThroughScenario<ID : Comparable<ID>, E : Any> : CacheTestScenario<ID, E> {
    companion object : KLogging()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    fun updateEmail(entity: E): E

    @Test
    fun `save - 캐시와 DB 모두에 반영된다`() {
        val id = getExistingId()
        val fromDb = repository.findByIdFromDb(id).shouldNotBeNull()

        val updated = updateEmail(fromDb)
        repository.save(id, updated)

        repository.findById(id) shouldBeEqualTo updated
        repository.findByIdFromDb(id) shouldBeEqualTo updated
    }

    @Test
    fun `saveAll - Map으로 일괄 저장 후 캐시와 DB 모두 반영된다`() {
        val ids = getExistingIds()
        val entities = repository.findAll(ids)

        val updated = entities.mapValues { (_, v) -> updateEmail(v) }
        repository.saveAll(updated)

        updated.forEach { (id, entity) ->
            repository.findById(id) shouldBeEqualTo entity
            repository.findByIdFromDb(id) shouldBeEqualTo entity
        }
    }

    @Test
    fun `delete - 캐시와 DB 모두에서 삭제된다`() {
        val id = getExistingId()
        repository.save(id, repository.findByIdFromDb(id)!!)

        repository.delete(id)

        repository.findById(id).shouldBeNull()
        repository.findByIdFromDb(id).shouldBeNull()
    }

    @Test
    fun `deleteAll - 복수 ID를 한번에 삭제한다`() {
        val ids = getExistingIds()
        ids.forEach { id -> repository.save(id, repository.findByIdFromDb(id)!!) }

        repository.deleteAll(ids)

        ids.forEach { id ->
            repository.findById(id).shouldBeNull()
            repository.findByIdFromDb(id).shouldBeNull()
        }
    }

    @Test
    fun `findByIdFromDb - 캐시를 우회하고 DB에서 직접 조회한다`() {
        val id = getExistingId()
        repository.findByIdFromDb(id).shouldNotBeNull()
    }

    @Test
    fun `findAllFromDb - 여러 ID로 DB에서 직접 조회한다`() {
        val ids = getExistingIds()
        val result = repository.findAllFromDb(ids)
        result.size shouldBeEqualTo ids.size
    }

    @Test
    fun `countFromDb - DB 전체 레코드 수를 반환한다`() {
        val count = repository.countFromDb()
        count shouldBeEqualTo getExistingIds().size.toLong()
    }
}
```

---

## Task 4: WriteBehindScenario (sync)

**Files:**

- Create:
  `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/WriteBehindScenario.kt`

- [ ] **Step 1: WriteBehindScenario 인터페이스 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Write-behind 캐시 전략 시나리오.
 *
 * - save() 시 캐시에는 즉시 반영, DB는 비동기로 적재
 */
interface WriteBehindScenario<ID : Comparable<ID>, E : Any> : CacheTestScenario<ID, E> {
    companion object : KLogging()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    fun updateEmail(entity: E): E

    /** DB 반영 완료까지 폴링 대기 */
    fun awaitDbReflection(timeout: Long = 5_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeout
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100L)
        }
    }

    @Test
    fun `save - WRITE_BEHIND 저장 후 캐시에는 즉시 반영된다`() {
        val id = getExistingId()
        val entity = repository.findByIdFromDb(id).shouldNotBeNull()

        val updated = updateEmail(entity)
        repository.save(id, updated)

        repository.findById(id) shouldBeEqualTo updated
    }

    @Test
    fun `save - WRITE_BEHIND flush 주기 후 DB에 반영된다`() {
        val id = getExistingId()
        val entity = repository.findByIdFromDb(id).shouldNotBeNull()

        val updated = updateEmail(entity)
        repository.save(id, updated)

        awaitDbReflection { repository.findByIdFromDb(id) == updated }

        repository.findByIdFromDb(id) shouldBeEqualTo updated
    }

    @Test
    fun `saveAll - 여러 레코드를 배치로 비동기 적재한다`() {
        val ids = getExistingIds()
        val entities = ids.associateWith { id ->
            updateEmail(repository.findByIdFromDb(id)!!)
        }
        repository.saveAll(entities)

        awaitDbReflection {
            entities.all { (id, expected) -> repository.findByIdFromDb(id) == expected }
        }

        entities.forEach { (id, expected) ->
            repository.findByIdFromDb(id) shouldBeEqualTo expected
        }
    }
}
```

---

## Task 5: Suspended base + scenario interfaces

**Files:**

- Create: `scenarios/SuspendedCacheTestScenario.kt`
- Create: `scenarios/SuspendedReadThroughScenario.kt`
- Create: `scenarios/SuspendedWriteThroughScenario.kt`
- Create: `scenarios/SuspendedWriteBehindScenario.kt`

- [ ] **Step 1: SuspendedCacheTestScenario 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.lettuce.repository.SuspendedJdbcLettuceRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach

interface SuspendedCacheTestScenario<ID : Comparable<ID>, E : Any> {
    companion object : KLoggingChannel()

    val repository: SuspendedJdbcLettuceRepository<ID, E>
    val config: LettuceCacheConfig

    suspend fun getExistingId(): ID
    suspend fun getExistingIds(): List<ID>
    suspend fun getNonExistentId(): ID

    @BeforeEach
    fun clearCacheBeforeEach() {
        runTest { repository.clearCache() }
    }
}
```

- [ ] **Step 2: SuspendedReadThroughScenario 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

interface SuspendedReadThroughScenario<ID : Comparable<ID>, E : Any> : SuspendedCacheTestScenario<ID, E> {
    companion object : KLoggingChannel()

    suspend fun buildEntityForId(id: ID): E

    @Test
    fun `findById - 캐시 미스 시 DB에서 Read-through로 값을 로드한다`() = runTest {
        val id = getExistingId()
        val fromDb = repository.findByIdFromDb(id).shouldNotBeNull()

        repository.clearCache()
        val fromCache = repository.findById(id).shouldNotBeNull()
        fromCache shouldBeEqualTo fromDb
    }

    @Test
    fun `findById - DB에 없는 ID는 null을 반환한다`() = runTest {
        repository.findById(getNonExistentId()).shouldBeNull()
    }

    @Test
    fun `findAll - 여러 ID 일괄 조회 시 캐시 미스 키를 DB에서 Read-through한다`() = runTest {
        val ids = getExistingIds()
        val result = repository.findAll(ids)
        result.size shouldBeEqualTo ids.size
    }

    @Test
    fun `findAll - 존재하지 않는 ID는 결과에 포함되지 않는다`() = runTest {
        val ids = getExistingIds() + listOf(getNonExistentId())
        val result = repository.findAll(ids)
        result.size shouldBeEqualTo getExistingIds().size
    }

    @Test
    fun `clearCache - 캐시를 비운 후 재조회하면 DB에서 다시 Read-through한다`() = runTest {
        val id = getExistingId()
        repository.findById(id)
        repository.clearCache()

        repository.findById(id).shouldNotBeNull()
    }

    @Test
    fun `delete - 캐시에만 저장된 엔티티를 삭제하면 findById는 null을 반환한다`() = runTest {
        val id = getNonExistentId()
        repository.save(id, buildEntityForId(id))
        repository.delete(id)

        repository.findById(id).shouldBeNull()
    }
}
```

- [ ] **Step 3: SuspendedWriteThroughScenario 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

interface SuspendedWriteThroughScenario<ID : Comparable<ID>, E : Any> : SuspendedCacheTestScenario<ID, E> {
    companion object : KLoggingChannel()

    suspend fun updateEmail(entity: E): E

    @Test
    fun `save - 캐시와 DB 모두에 반영된다`() = runTest {
        val id = getExistingId()
        val entity = repository.findByIdFromDb(id).shouldNotBeNull()
        val updated = updateEmail(entity)
        repository.save(id, updated)

        repository.findById(id) shouldBeEqualTo updated
        repository.findByIdFromDb(id) shouldBeEqualTo updated
    }

    @Test
    fun `saveAll - Map 일괄 저장 후 캐시와 DB 모두 반영된다`() = runTest {
        val ids = getExistingIds()
        val entities = repository.findAll(ids)
        val updated = entities.mapValues { (_, v) -> updateEmail(v) }
        repository.saveAll(updated)

        updated.forEach { (id, entity) ->
            repository.findById(id) shouldBeEqualTo entity
            repository.findByIdFromDb(id) shouldBeEqualTo entity
        }
    }

    @Test
    fun `delete - 캐시와 DB 모두에서 삭제된다`() = runTest {
        val id = getExistingId()
        repository.save(id, repository.findByIdFromDb(id)!!)
        repository.delete(id)

        repository.findById(id).shouldBeNull()
        repository.findByIdFromDb(id).shouldBeNull()
    }

    @Test
    fun `deleteAll - 복수 ID를 한번에 삭제한다`() = runTest {
        val ids = getExistingIds()
        ids.forEach { id -> repository.save(id, repository.findByIdFromDb(id)!!) }
        repository.deleteAll(ids)

        ids.forEach { id ->
            repository.findById(id).shouldBeNull()
            repository.findByIdFromDb(id).shouldBeNull()
        }
    }

    @Test
    fun `countFromDb - DB 전체 레코드 수를 반환한다`() = runTest {
        repository.countFromDb() shouldBeEqualTo getExistingIds().size.toLong()
    }
}
```

- [ ] **Step 4: SuspendedWriteBehindScenario 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface SuspendedWriteBehindScenario<ID : Comparable<ID>, E : Any> : SuspendedCacheTestScenario<ID, E> {
    companion object : KLoggingChannel()

    suspend fun updateEmail(entity: E): E

    @Test
    fun `save - WRITE_BEHIND 저장 후 캐시에는 즉시 반영된다`() = runTest {
        val id = getExistingId()
        val entity = repository.findByIdFromDb(id).shouldNotBeNull()
        val updated = updateEmail(entity)
        repository.save(id, updated)

        repository.findById(id) shouldBeEqualTo updated
    }

    @Test
    fun `save - WRITE_BEHIND flush 주기 후 DB에 반영된다`() = runTest(timeout = 10.seconds) {
        val id = getExistingId()
        val entity = repository.findByIdFromDb(id).shouldNotBeNull()
        val updated = updateEmail(entity)
        repository.save(id, updated)

        val deadline = System.currentTimeMillis() + 5_000L
        while (repository.findByIdFromDb(id) != updated && System.currentTimeMillis() < deadline) {
            delay(100.milliseconds)
        }

        repository.findByIdFromDb(id) shouldBeEqualTo updated
    }

    @Test
    fun `saveAll - 여러 레코드를 배치로 비동기 적재한다`() = runTest(timeout = 10.seconds) {
        val ids = getExistingIds()
        val entities = ids.associateWith { id ->
            updateEmail(repository.findByIdFromDb(id)!!)
        }
        repository.saveAll(entities)

        val deadline = System.currentTimeMillis() + 5_000L
        while (entities.any { (id, e) -> repository.findByIdFromDb(id) != e }
            && System.currentTimeMillis() < deadline
        ) {
            delay(100.milliseconds)
        }

        entities.forEach { (id, expected) ->
            repository.findByIdFromDb(id) shouldBeEqualTo expected
        }
    }
}
```

- [ ] **Step 5: 컴파일 확인**

```bash
./gradlew :bluetape4k-exposed-jdbc-lettuce:compileTestKotlin -x detekt
```

---

## Task 6: Concrete Test Classes (sync)

**Files:**

- Create: `repository/UserReadThroughCacheTest.kt`
- Create: `repository/UserWriteThroughCacheTest.kt`
- Create: `repository/UserWriteBehindCacheTest.kt`

- [ ] **Step 1: UserReadThroughCacheTest 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.lettuce.domain.UserRepository
import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.repository.scenarios.ReadThroughScenario
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

class UserReadThroughCacheTest :
    AbstractJdbcLettuceRepositoryTest(),
    ReadThroughScenario<Long, UserRecord> {
    companion object : KLogging()

    override val config = LettuceCacheConfig.READ_ONLY
    override val repository by lazy { UserRepository(redisClient, config) }

    private val testUsers = mutableListOf<UserRecord>()

    @BeforeAll
    fun setupDb() {
        Database.connect(
            url = "jdbc:h2:mem:test-lettuce-rt-scenario;DB_CLOSE_DELAY=-1;MODE=MySQL",
            driver = "org.h2.Driver"
        )
        transaction { SchemaUtils.create(UserTable) }
    }

    @BeforeEach
    fun setupData() {
        transaction { UserTable.deleteAll() }
        testUsers.clear()
        testUsers.addAll((1..3).map { repository.createInDb(UserSchema.newUserRecord()) })
    }

    @AfterEach
    fun tearDown() {
        repository.clearCache()
    }

    override fun getExistingId() = testUsers.first().id
    override fun getExistingIds() = testUsers.map { it.id }
    override fun getNonExistentId() = 999_999L

    override fun buildEntityForId(id: Long) =
        UserSchema.newUserRecord().copy(id = id)
}
```

- [ ] **Step 2: UserWriteThroughCacheTest 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.lettuce.domain.UserRepository
import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.repository.scenarios.WriteThroughScenario
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

class UserWriteThroughCacheTest :
    AbstractJdbcLettuceRepositoryTest(),
    WriteThroughScenario<Long, UserRecord> {
    companion object : KLogging()

    override val config = LettuceCacheConfig.READ_WRITE_THROUGH
    override val repository by lazy { UserRepository(redisClient, config) }

    private val testUsers = mutableListOf<UserRecord>()

    @BeforeAll
    fun setupDb() {
        Database.connect(
            url = "jdbc:h2:mem:test-lettuce-wt-scenario;DB_CLOSE_DELAY=-1;MODE=MySQL",
            driver = "org.h2.Driver"
        )
        transaction { SchemaUtils.create(UserTable) }
    }

    @BeforeEach
    fun setupData() {
        transaction { UserTable.deleteAll() }
        testUsers.clear()
        testUsers.addAll((1..3).map { repository.createInDb(UserSchema.newUserRecord()) })
    }

    @AfterEach
    fun tearDown() {
        repository.clearCache()
    }

    override fun getExistingId() = testUsers.first().id
    override fun getExistingIds() = testUsers.map { it.id }
    override fun getNonExistentId() = 999_999L

    override fun updateEmail(entity: UserRecord) =
        entity.copy(email = "wt-updated-${entity.id}@example.com")
}
```

- [ ] **Step 3: UserWriteBehindCacheTest 작성**

```kotlin
package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.lettuce.domain.UserRepository
import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.repository.scenarios.WriteBehindScenario
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.time.Duration

class UserWriteBehindCacheTest :
    AbstractJdbcLettuceRepositoryTest(),
    WriteBehindScenario<Long, UserRecord> {
    companion object : KLogging()

    override val config = LettuceCacheConfig.WRITE_BEHIND.copy(
        writeBehindDelay = Duration.ofMillis(300),
        writeBehindBatchSize = 50
    )
    override val repository by lazy { UserRepository(redisClient, config) }

    private val testUsers = mutableListOf<UserRecord>()

    @BeforeAll
    fun setupDb() {
        Database.connect(
            url = "jdbc:h2:mem:test-lettuce-wb-scenario;DB_CLOSE_DELAY=-1;MODE=MySQL",
            driver = "org.h2.Driver"
        )
        transaction { SchemaUtils.create(UserTable) }
    }

    @BeforeEach
    fun setupData() {
        transaction { UserTable.deleteAll() }
        testUsers.clear()
        testUsers.addAll((1..3).map { repository.createInDb(UserSchema.newUserRecord()) })
    }

    @AfterEach
    fun tearDown() {
        repository.clearCache()
    }

    override fun getExistingId() = testUsers.first().id
    override fun getExistingIds() = testUsers.map { it.id }
    override fun getNonExistentId() = 999_999L

    override fun updateEmail(entity: UserRecord) =
        entity.copy(email = "wb-updated-${entity.id}@example.com")
}
```

- [ ] **Step 4: 컴파일 및 테스트 실행**

```bash
./gradlew :bluetape4k-exposed-jdbc-lettuce:test \
  --tests "io.bluetape4k.exposed.lettuce.repository.UserReadThroughCacheTest" \
  --tests "io.bluetape4k.exposed.lettuce.repository.UserWriteThroughCacheTest" \
  --tests "io.bluetape4k.exposed.lettuce.repository.UserWriteBehindCacheTest" \
  -x detekt
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add data/exposed-jdbc-lettuce/src/test/kotlin/
git commit -m "test: exposed-jdbc-lettuce sync 캐시 전략 시나리오 테스트 추가"
```

---

## Task 7: Concrete Suspended Test Classes

**Files:**

- Create: `repository/SuspendedUserReadThroughCacheTest.kt`
- Create: `repository/SuspendedUserWriteThroughCacheTest.kt`
- Create: `repository/SuspendedUserWriteBehindCacheTest.kt`

패턴은 Task 6와 동일하되:

- `UserRepository` → `SuspendedUserRepository`
- `WriteThroughScenario` → `SuspendedWriteThroughScenario`
- `clearCache()` 호출 시 `runTest { ... }` 래핑

- [ ] **Step 1: 3개 suspend concrete 테스트 클래스 작성** (Task 6와 동일 패턴)

- [ ] **Step 2: 전체 테스트 실행**

```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-exposed-jdbc-lettuce:test -x detekt
```

Expected: `BUILD SUCCESSFUL`, 모든 시나리오 테스트 통과

- [ ] **Step 3: 커밋**

```bash
git add data/exposed-jdbc-lettuce/src/test/kotlin/
git commit -m "test: exposed-jdbc-lettuce suspend 캐시 전략 시나리오 테스트 추가"
```

---

## 검증 체크리스트

- [ ] `compileTestKotlin` 오류 없음
- [ ] `UserReadThroughCacheTest` — 6개 테스트 통과
- [ ] `UserWriteThroughCacheTest` — 7개 테스트 통과
- [ ] `UserWriteBehindCacheTest` — 3개 테스트 통과
- [ ] `SuspendedUserReadThroughCacheTest` — 6개 테스트 통과
- [ ] `SuspendedUserWriteThroughCacheTest` — 5개 테스트 통과
- [ ] `SuspendedUserWriteBehindCacheTest` — 3개 테스트 통과
- [ ] 기존 `ItemRepositoryTest`, `LettuceLoadedMapConsistencyTest` 통과 유지
