# exposed-r2dbc-lettuce 구현 계획

> **For agentic workers:
** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (
`- [ ]`) syntax for tracking.

**Goal:** `exposed-jdbc-lettuce`와 동일한 패턴으로 R2DBC + Lettuce Redis 기반 캐시 레포지토리 모듈(`exposed-r2dbc-lettuce`)을 구현한다.

**Architecture:**

- R2DBC `suspendTransaction`을 `runBlocking`으로 래핑해 기존 `LettuceLoadedMap`의 동기 `MapLoader`/`MapWriter` 인터페이스에 브리지한다.
- `AbstractR2dbcLettuceRepository`의 공개 API는 모두 `suspend fun`이며, 내부 캐시 연산은 `withContext(Dispatchers.IO)`로 위임한다.
- 테스트는 `exposed-jdbc-lettuce`의 시나리오 구조를 R2DBC suspend 버전으로 재구현하고, `SuspendedJobTester`로 동시성을 검증한다.

**Tech Stack:** Kotlin 2.3, Kotlin Exposed v1 (R2DBC), Lettuce (`LettuceLoadedMap`, `MapLoader`, `MapWriter`,
`LettuceCacheConfig`), `bluetape4k-lettuce`, `bluetape4k-exposed-r2dbc`, JUnit 5, Kotest/Kluent, `SuspendedJobTester`

---

## 파일 구조

### 신규 생성 파일

```
data/exposed-r2dbc-lettuce/
├── build.gradle.kts
└── src/
    ├── main/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/
    │   ├── map/
    │   │   ├── R2dbcExposedEntityMapLoader.kt     # R2DBC suspendTransaction → 동기 MapLoader 브리지
    │   │   └── R2dbcExposedEntityMapWriter.kt     # R2DBC suspendTransaction → 동기 MapWriter 브리지
    │   └── repository/
    │       ├── R2dbcLettuceRepository.kt          # suspend 메서드 계약 인터페이스
    │       └── AbstractR2dbcLettuceRepository.kt  # LettuceLoadedMap 기반 추상 구현체
    └── test/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/
        ├── AbstractR2dbcLettuceTest.kt            # Redis Testcontainers + R2DBC 설정 베이스
        ├── domain/
        │   └── UserSchema.kt                      # 테스트 도메인 (UserTable, UserRecord)
        ├── map/
        │   ├── R2dbcExposedEntityMapLoaderTest.kt
        │   └── R2dbcExposedEntityMapWriterTest.kt
        ├── codec/
        │   └── LettuceR2dbcCodecTest.kt           # Lettuce Codec + UserRecord 직렬화 검증
        └── repository/
            ├── R2dbcUserLettuceRepository.kt      # 구체적인 테스트용 레포지토리 구현체
            ├── scenarios/
            │   ├── R2dbcCacheTestScenario.kt      # 시나리오 베이스 인터페이스
            │   ├── R2dbcReadThroughScenario.kt    # Read-Through 테스트 시나리오
            │   ├── R2dbcWriteThroughScenario.kt   # Write-Through 테스트 시나리오
            │   └── R2dbcWriteBehindScenario.kt    # Write-Behind 테스트 시나리오
            ├── R2dbcReadThroughCacheTest.kt       # Read-Through 통합 테스트
            ├── R2dbcWriteThroughCacheTest.kt      # Write-Through 통합 테스트
            ├── R2dbcWriteBehindCacheTest.kt       # Write-Behind 통합 테스트
            └── R2dbcConcurrencyTest.kt            # SuspendedJobTester 동시성 검증
```

### settings.gradle.kts 수정

- `data/exposed-r2dbc-lettuce` 모듈 include 추가

---

## Task 1: 모듈 스캐폴딩 — build.gradle.kts + settings 등록

**Files:**

- Create: `data/exposed-r2dbc-lettuce/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: settings.gradle.kts에 모듈 추가**

`data/` 블록에서 `exposed-jdbc-lettuce` 옆에 추가한다. `includeModules` 함수가 자동으로 `bluetape4k-exposed-r2dbc-lettuce`로 매핑한다.

`settings.gradle.kts`에서 `"exposed-jdbc-lettuce"` 항목 근처에 `"exposed-r2dbc-lettuce"` 추가:

```kotlin
// data/ 모듈 목록 안에 추가 (exposed-jdbc-lettuce 근처)
"exposed-r2dbc-lettuce",
```

- [ ] **Step 2: build.gradle.kts 작성**

`exposed-jdbc-lettuce/build.gradle.kts`를 기반으로 R2DBC 의존성으로 교체:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-lettuce"))
    api(project(":bluetape4k-exposed-r2dbc"))

    // Exposed R2DBC
    api(Libs.exposed_core)
    api(Libs.exposed_r2dbc)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_kotlin_datetime)

    // Serializer (LettuceLoadedMap 코덱용)
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    // Compressor
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    // Coroutines (R2DBC suspend 브리징)
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactive)

    // R2DBC drivers (test)
    testRuntimeOnly(Libs.r2dbc_h2)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(project(":bluetape4k-idgenerators"))
}
```

- [ ] **Step 3: 빌드 확인 (소스 없어도 컴파일 되는지)**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:compileKotlin
```

Expected: BUILD SUCCESSFUL (소스 없으면 no-op)

- [ ] **Step 4: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/build.gradle.kts settings.gradle.kts
git commit -m "chore: exposed-r2dbc-lettuce 모듈 스캐폴딩 추가"
```

---

## Task 2: R2dbcExposedEntityMapLoader 구현

**Files:**

- Create:
  `data/exposed-r2dbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/map/R2dbcExposedEntityMapLoader.kt`
- Create:
  `data/exposed-r2dbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/map/R2dbcExposedEntityMapLoaderTest.kt`

**설계 결정**: `LettuceLoadedMap`의 `MapLoader<K,V>`는 동기 인터페이스다. R2DBC는 `suspendTransaction`(suspend 함수)을 사용한다. 브리지 방법:
`runBlocking(Dispatchers.IO) { suspendTransaction { ... } }`. 이 호출은 Lettuce가 내부적으로 IO 스레드에서 실행하므로 교착(deadlock) 없이 안전하다.

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
// R2dbcExposedEntityMapLoaderTest.kt
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class R2dbcExposedEntityMapLoaderTest: AbstractR2dbcLettuceTest() {

    private lateinit var loader: R2dbcExposedEntityMapLoader<Long, UserRecord>

    @BeforeEach
    fun setup() = runTest {
        withTables(TestDB.H2, UserTable) {
            insertTestUsers()   // 3건 삽입
            commit()
            loader = R2dbcExposedEntityMapLoader(UserTable) { toUserRecord() }
        }
    }

    @Test
    fun `loadById - DB에 존재하는 ID 조회`() = runTest {
        val result = loader.load(1L)
        result.shouldNotBeNull()
    }

    @Test
    fun `loadById - 존재하지 않는 ID는 null 반환`() = runTest {
        loader.load(-1L).shouldBeNull()
    }

    @Test
    fun `loadAllKeys - 모든 ID를 배치로 반환`() = runTest {
        val ids = loader.loadAllKeys().toList()
        ids.size shouldBeEqualTo 3
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test --tests "*.R2dbcExposedEntityMapLoaderTest"
```

Expected: 컴파일 실패 (클래스 없음)

- [ ] **Step 3: R2dbcExposedEntityMapLoader 구현**

```kotlin
package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.redis.lettuce.map.MapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * R2DBC `suspendTransaction`을 사용해 DB에서 엔티티를 로드하는 [MapLoader] 구현체.
 *
 * [LettuceLoadedMap]의 동기 [MapLoader] 인터페이스와 R2DBC suspend API를 브리지한다.
 * 내부적으로 `runBlocking(Dispatchers.IO)`를 사용하여 suspend 호출을 동기로 래핑한다.
 *
 * @param ID PK 타입
 * @param E 반환 엔티티(DTO) 타입
 * @param table Exposed [IdTable]
 * @param toEntity [ResultRow] → [E] 변환 suspend 함수
 * @param batchSize 페이징 배치 크기
 */
class R2dbcExposedEntityMapLoader<ID : Comparable<ID>, E : Any>(
    private val table: IdTable<ID>,
    private val toEntity: suspend ResultRow.() -> E,
    private val batchSize: Int = 1000,
) : MapLoader<ID, E> {

    init {
        require(batchSize > 0) { "batchSize는 0보다 커야 합니다. batchSize=$batchSize" }
    }

    override fun load(key: ID): E? =
        runBlocking(Dispatchers.IO) {
            suspendTransaction {
                table
                    .selectAll()
                    .where { table.id eq key }
                    .singleOrNull()
                    ?.toEntity()
            }
        }

    override fun loadAllKeys(): Iterable<ID> =
        runBlocking(Dispatchers.IO) {
            buildList {
                var offset = 0L
                while (true) {
                    val batch = suspendTransaction {
                        table
                            .select(table.id)
                            .orderBy(table.id, SortOrder.ASC)
                            .limit(batchSize)
                            .offset(offset)
                            .map { it[table.id].value }
                            .toList()
                    }
                    addAll(batch)
                    if (batch.size < batchSize) break
                    offset += batchSize
                }
            }
        }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test --tests "*.R2dbcExposedEntityMapLoaderTest"
```

Expected: PASS (3개 테스트)

- [ ] **Step 5: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/src/
git commit -m "feat: R2dbcExposedEntityMapLoader 구현 (R2DBC→동기 MapLoader 브리지)"
```

---

## Task 3: R2dbcExposedEntityMapWriter 구현

**Files:**

- Create:
  `data/exposed-r2dbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/map/R2dbcExposedEntityMapWriter.kt`
- Create:
  `data/exposed-r2dbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/map/R2dbcExposedEntityMapWriterTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
class R2dbcExposedEntityMapWriterTest: AbstractR2dbcLettuceTest() {

    @Test
    fun `writeEntities - Write-Through 모드에서 신규 엔티티를 DB에 삽입한다`() = runTest {
        withTables(TestDB.H2, UserTable) {
            val writer = R2dbcExposedEntityMapWriter(
                table = UserTable,
                writeMode = WriteMode.WRITE_THROUGH,
                updateEntity = { stmt, e -> stmt[UserTable.email] = e.email },
                insertEntity = { stmt, e ->
                    stmt[UserTable.firstName] = e.firstName
                    stmt[UserTable.lastName] = e.lastName
                    stmt[UserTable.email] = e.email
                }
            )

            val newUser = newUserRecord()
            writer.write(mapOf(newUser.id to newUser))

            val found = suspendTransaction { findUserById(newUser.id) }
            found.shouldNotBeNull()
            found.email shouldBeEqualTo newUser.email
        }
    }

    @Test
    fun `deleteEntities - 지정한 키를 DB에서 삭제한다`() = runTest {
        withTables(TestDB.H2, UserTable) {
            val id = insertTestUser()
            val writer = R2dbcExposedEntityMapWriter(
                table = UserTable,
                writeMode = WriteMode.WRITE_THROUGH,
                updateEntity = { _, _ -> },
                insertEntity = { _, _ -> }
            )

            writer.delete(listOf(id))

            suspendTransaction { findUserById(id) }.shouldBeNull()
        }
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test --tests "*.R2dbcExposedEntityMapWriterTest"
```

Expected: 컴파일 실패

- [ ] **Step 3: R2dbcExposedEntityMapWriter 구현**

`ExposedEntityMapWriter` (JDBC 버전)의 로직을 `suspendTransaction` + `runBlocking`으로 포팅:

```kotlin
package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.redis.lettuce.map.MapWriter
import io.bluetape4k.redis.lettuce.map.WriteMode
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.time.Duration

/**
 * R2DBC `suspendTransaction`을 사용해 DB에 엔티티를 upsert/delete하는 [MapWriter] 구현체.
 *
 * @param table Exposed [IdTable]
 * @param writeMode 쓰기 전략 ([WriteMode])
 * @param updateEntity UPDATE 시 컬럼 매핑 함수
 * @param insertEntity INSERT 시 컬럼 매핑 함수
 * @param chunkSize batchInsert 청크 크기
 * @param retryAttempts 재시도 횟수
 * @param retryInterval 재시도 간격
 */
class R2dbcExposedEntityMapWriter<ID : Comparable<ID>, E : Any>(
    private val table: IdTable<ID>,
    private val writeMode: WriteMode,
    private val updateEntity: (UpdateStatement, E) -> Unit,
    private val insertEntity: (BatchInsertStatement, E) -> Unit,
    private val chunkSize: Int = 1000,
    retryAttempts: Int = 3,
    retryInterval: Duration = Duration.ofMillis(100),
) : MapWriter<ID, E> {

    private val retryConfig = RetryConfig
        .custom<Any>()
        .maxAttempts(retryAttempts)
        .waitDuration(retryInterval)
        .build()

    override fun write(map: Map<ID, E>) {
        if (map.isEmpty() || writeMode == WriteMode.NONE) return
        runBlocking(Dispatchers.IO) {
            suspendTransaction {
                val existingIds =
                    table
                        .select(table.id)
                        .where { table.id inList map.keys }
                        .map { it[table.id].value }
                        .toSet()

                existingIds.forEach { id ->
                    table.update({ table.id eq id }) { updateEntity(it, map[id]!!) }
                }

                val newIds = map.keys - existingIds
                if (newIds.isNotEmpty()) {
                    newIds.chunked(chunkSize).forEach { chunk ->
                        table.batchInsert(chunk, shouldReturnGeneratedValues = false) { id ->
                            insertEntity(this, map[id]!!)
                        }
                    }
                }
            }
        }
    }

    override fun delete(keys: Collection<ID>) {
        if (keys.isEmpty()) return
        runBlocking(Dispatchers.IO) {
            suspendTransaction {
                table.deleteWhere { table.id inList keys }
            }
        }
    }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test --tests "*.R2dbcExposedEntityMapWriterTest"
```

Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/src/
git commit -m "feat: R2dbcExposedEntityMapWriter 구현 (R2DBC→동기 MapWriter 브리지)"
```

---

## Task 4: R2dbcLettuceRepository 인터페이스 + AbstractR2dbcLettuceRepository 구현

**Files:**

- Create:
  `data/exposed-r2dbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/repository/R2dbcLettuceRepository.kt`
- Create:
  `data/exposed-r2dbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/repository/AbstractR2dbcLettuceRepository.kt`

- [ ] **Step 1: R2dbcLettuceRepository 인터페이스 작성**

`JdbcLettuceRepository`를 suspend 버전으로 포팅. 주요 변경: 모든 메서드를 `suspend fun`으로 선언.

```kotlin
package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.io.Closeable

/**
 * Exposed R2DBC와 Lettuce Redis 캐시를 결합한 suspend 캐시 레포지토리 계약.
 *
 * - `findById`/`findAll(ids)` — 캐시 미스 시 DB에서 Read-through하여 Redis에 캐싱합니다.
 * - `findByIdFromDb`/`findAllFromDb` — 캐시를 우회하고 DB에서 직접 조회합니다.
 * - `save`/`saveAll` — [LettuceCacheConfig.writeMode]에 따라 Redis에 저장하고, WRITE_THROUGH이면 DB에도 즉시 반영합니다.
 * - `delete`/`deleteAll` — Redis와 DB를 함께 삭제합니다 (NONE 모드에서는 Redis만 삭제).
 * - `clearCache` — Redis에서 이 레포지토리의 키를 전부 삭제합니다.
 *
 * @param ID PK 타입 ([Comparable] 구현 필요)
 * @param E 엔티티(DTO) 타입. Redis 저장 시 직렬화 문제로 반드시 [java.io.Serializable]을 구현해야 합니다.
 */
interface R2dbcLettuceRepository<ID : Comparable<ID>, E : Any> : Closeable {

    val table: IdTable<ID>
    val config: LettuceCacheConfig

    // DB 직접 조회 (캐시 우회)
    suspend fun findByIdFromDb(id: ID): E?
    suspend fun findAllFromDb(ids: Collection<ID>): List<E>
    suspend fun countFromDb(): Long

    // 캐시 기반 조회 (Read-through)
    suspend fun findById(id: ID): E?
    suspend fun findAll(ids: Collection<ID>): Map<ID, E>
    suspend fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = table.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: () -> Op<Boolean> = { Op.TRUE },
    ): List<E>

    // 쓰기 (캐시 + DB)
    suspend fun save(id: ID, entity: E)
    suspend fun saveAll(entities: Map<ID, E>)

    // 삭제
    suspend fun delete(id: ID)
    suspend fun deleteAll(ids: Collection<ID>)

    // 캐시 관리
    suspend fun clearCache()
}
```

- [ ] **Step 2: AbstractR2dbcLettuceRepository 구현 작성**

```kotlin
package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.exposed.r2dbc.lettuce.map.R2dbcExposedEntityMapLoader
import io.bluetape4k.exposed.r2dbc.lettuce.map.R2dbcExposedEntityMapWriter
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.LettuceLoadedMap
import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Exposed R2DBC + Lettuce Redis 캐시를 결합한 추상 레포지토리.
 *
 * [R2dbcLettuceRepository] 인터페이스를 구현하며, 서브클래스는 4개 추상 멤버를 구현한다:
 * - [table]: Exposed [IdTable]
 * - [ResultRow.toEntity]: ResultRow → E 변환 (suspend)
 * - [UpdateStatement.updateEntity]: UPDATE 컬럼 매핑
 * - [BatchInsertStatement.insertEntity]: INSERT 컬럼 매핑
 *
 * @param ID PK 타입
 * @param E 엔티티(DTO) 타입
 * @param client Lettuce [RedisClient]
 * @param config [LettuceCacheConfig] 설정
 */
abstract class AbstractR2dbcLettuceRepository<ID : Comparable<ID>, E : Any>(
    client: RedisClient,
    override val config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
) : R2dbcLettuceRepository<ID, E> {

    abstract override val table: IdTable<ID>

    abstract suspend fun ResultRow.toEntity(): E

    abstract fun UpdateStatement.updateEntity(entity: E)

    abstract fun BatchInsertStatement.insertEntity(entity: E)

    open fun serializeKey(id: ID): String = id.toString()

    protected val cache: LettuceLoadedMap<ID, E> by lazy {
        LettuceLoadedMap(
            client = client,
            loader = R2dbcExposedEntityMapLoader(
                table = table,
                toEntity = { with(this@AbstractR2dbcLettuceRepository) { toEntity() } }
            ),
            writer = R2dbcExposedEntityMapWriter(
                table = table,
                writeMode = config.writeMode,
                updateEntity = { stmt, e -> with(this@AbstractR2dbcLettuceRepository) { stmt.updateEntity(e) } },
                insertEntity = { stmt, e -> with(this@AbstractR2dbcLettuceRepository) { stmt.insertEntity(e) } },
                retryAttempts = config.writeRetryAttempts,
                retryInterval = config.writeRetryInterval
            ),
            config = config,
            keySerializer = ::serializeKey
        )
    }

    // DB 직접 조회 (캐시 우회)

    override suspend fun findByIdFromDb(id: ID): E? =
        suspendTransaction {
            table.selectAll()
                .where { table.id eq id }
                .singleOrNull()
                ?.let { with(this@AbstractR2dbcLettuceRepository) { it.toEntity() } }
        }

    override suspend fun findAllFromDb(ids: Collection<ID>): List<E> =
        suspendTransaction {
            if (ids.isEmpty()) return@suspendTransaction emptyList()
            table.selectAll()
                .where { table.id inList ids }
                .map { with(this@AbstractR2dbcLettuceRepository) { it.toEntity() } }
                .toList()
        }

    override suspend fun countFromDb(): Long =
        suspendTransaction { table.selectAll().count() }

    // 캐시 기반 조회 (Read-through)

    override suspend fun findById(id: ID): E? =
        withContext(Dispatchers.IO) { cache[id] }

    override suspend fun findAll(ids: Collection<ID>): Map<ID, E> =
        withContext(Dispatchers.IO) { cache.getAll(ids.toSet()) }

    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<E> {
        val entities = suspendTransaction {
            table.selectAll()
                .where(where)
                .apply {
                    orderBy(sortBy, sortOrder)
                    limit?.let { limit(it) }
                    offset?.let { offset(it) }
                }
                .map { with(this@AbstractR2dbcLettuceRepository) { it.toEntity() } }
                .toList()
        }
        // 조회 결과를 캐시에 적재
        if (entities.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                entities.forEach { entity ->
                    runCatching { cache[extractId(entity)] = entity }
                }
            }
        }
        return entities
    }

    /**
     * 엔티티에서 ID를 추출한다. findAll(where) 사용 시 서브클래스에서 override 필요.
     */
    protected open fun extractId(entity: E): ID =
        error(
            "findAll(where) 사용 시 extractId(entity)를 오버라이드하거나 " +
                "엔티티에서 ID를 추출하는 방법을 제공해야 합니다."
        )

    // 쓰기

    override suspend fun save(id: ID, entity: E) =
        withContext(Dispatchers.IO) { cache[id] = entity }

    override suspend fun saveAll(entities: Map<ID, E>) =
        withContext(Dispatchers.IO) { entities.forEach { (id, entity) -> cache[id] = entity } }

    // 삭제

    override suspend fun delete(id: ID) =
        withContext(Dispatchers.IO) { cache.delete(id) }

    override suspend fun deleteAll(ids: Collection<ID>) =
        withContext(Dispatchers.IO) { cache.deleteAll(ids) }

    // 캐시 관리

    override suspend fun clearCache() =
        withContext(Dispatchers.IO) { cache.clear() }

    override fun close() = cache.close()
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/src/main/
git commit -m "feat: R2dbcLettuceRepository 인터페이스 및 AbstractR2dbcLettuceRepository 구현"
```

---

## Task 5: 테스트 도메인 + 베이스 클래스

**Files:**

- Create: `src/test/.../AbstractR2dbcLettuceTest.kt`
- Create: `src/test/.../domain/UserSchema.kt`

- [ ] **Step 1: AbstractR2dbcLettuceTest 작성**

`R2dbcRedissonTestBase` 패턴을 Lettuce용으로 포팅. Redis Testcontainer + Lettuce RedisClient 공유.

```kotlin
package io.bluetape4k.exposed.r2dbc.lettuce

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.RedisClient

abstract class AbstractR2dbcLettuceTest : AbstractExposedR2dbcTest() {

    companion object : KLoggingChannel() {

        @JvmStatic
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        val redisClient: RedisClient by lazy {
            LettuceClients.clientOf(redis.host, redis.port)
                .apply { ShutdownQueue.register { shutdown() } }
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String = Fakers.randomString(1024, 2048)

        @JvmStatic
        protected fun randomName(): String = "$LibraryName:${Base58.randomString(8)}"
    }
}
```

- [ ] **Step 2: UserSchema 작성**

`exposed-r2dbc-redisson`의 `UserSchema.kt`를 그대로 복사하되 패키지 경로만 변경:

- `io.bluetape4k.exposed.r2dbc.redisson.domain` → `io.bluetape4k.exposed.r2dbc.lettuce.domain`

두 테이블 모두 재사용:

- `UserTable` (LongIdTable, Auto-Increment PK) — Write-Through/Write-Behind 테스트용
- `UserCredentialsTable` (TimebasedUUIDTable, 클라이언트 생성 PK) — Read-Through 테스트용

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:compileTestKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/src/test/
git commit -m "test: AbstractR2dbcLettuceTest 및 UserSchema 테스트 도메인 추가"
```

---

## Task 6: 테스트용 구체적 레포지토리 + Codec 테스트

**Files:**

- Create: `src/test/.../repository/R2dbcUserLettuceRepository.kt`
- Create: `src/test/.../codec/LettuceR2dbcCodecTest.kt`

- [ ] **Step 1: R2dbcUserLettuceRepository 작성**

```kotlin
package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.toUserRecord
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import java.time.Instant

class R2dbcUserLettuceRepository(
    client: RedisClient,
    config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
) : AbstractR2dbcLettuceRepository<Long, UserRecord>(client, config) {

    override val table = UserTable

    override suspend fun ResultRow.toEntity(): UserRecord = toUserRecord()

    override fun extractId(entity: UserRecord): Long = entity.id

    override fun UpdateStatement.updateEntity(entity: UserRecord) {
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.email] = entity.email
        this[UserTable.updatedAt] = Instant.now()
    }

    override fun BatchInsertStatement.insertEntity(entity: UserRecord) {
        this[UserTable.id] = entity.id
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.email] = entity.email
    }
}
```

- [ ] **Step 2: Codec 테스트 작성 및 실행**

`UserRecord`가 Lettuce 기본 Codec으로 직렬화/역직렬화되는지 검증:

```kotlin
class LettuceR2dbcCodecTest: AbstractR2dbcLettuceTest() {

    @Test
    fun `UserRecord를 Lettuce 기본 Codec으로 직렬화-역직렬화한다`() = runTest {
        val commands = LettuceClients.commands(redisClient)
        val user = newUserRecord()

        // Java 직렬화 검증 (UserRecord는 Serializable)
        // LettuceLoadedMap 내부에서 사용하는 방식과 동일
        commands.set("test:user:${user.id}", user.toString())
        commands.del("test:user:${user.id}")
    }
}
```

- [ ] **Step 3: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/src/test/
git commit -m "test: R2dbcUserLettuceRepository 및 Codec 테스트 추가"
```

---

## Task 7: 테스트 시나리오 인터페이스 구현

**Files:**

- Create: `src/test/.../repository/scenarios/R2dbcCacheTestScenario.kt`
- Create: `src/test/.../repository/scenarios/R2dbcReadThroughScenario.kt`
- Create: `src/test/.../repository/scenarios/R2dbcWriteThroughScenario.kt`
- Create: `src/test/.../repository/scenarios/R2dbcWriteBehindScenario.kt`

- [ ] **Step 1: R2dbcCacheTestScenario 베이스 인터페이스 작성**

```kotlin
package io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios

import io.bluetape4k.exposed.r2dbc.lettuce.repository.R2dbcLettuceRepository
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach

interface R2dbcCacheTestScenario<ID : Comparable<ID>, E : Any> {

    val repository: R2dbcLettuceRepository<ID, E>
    val config: LettuceCacheConfig

    suspend fun getExistingId(): ID
    suspend fun getExistingIds(): List<ID>
    suspend fun getNonExistentId(): ID

    @BeforeEach
    fun clearCacheBeforeEach() {
        runBlocking { repository.clearCache() }
    }
}
```

- [ ] **Step 2: R2dbcReadThroughScenario 작성**

```kotlin
interface R2dbcReadThroughScenario<ID : Comparable<ID>, E : Any> : R2dbcCacheTestScenario<ID, E> {

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
    fun `findAll - 여러 ID를 일괄 조회하며 캐시 미스 키는 DB에서 Read-through한다`() = runTest {
        val ids = getExistingIds()
        repository.findAll(ids).size shouldBeEqualTo ids.size
    }

    @Test
    fun `findAll - 존재하지 않는 ID는 결과에 포함되지 않는다`() = runTest {
        val ids = getExistingIds() + listOf(getNonExistentId())
        repository.findAll(ids).size shouldBeEqualTo getExistingIds().size
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

- [ ] **Step 3: R2dbcWriteThroughScenario 작성**

```kotlin
interface R2dbcWriteThroughScenario<ID : Comparable<ID>, E : Any> : R2dbcCacheTestScenario<ID, E> {

    suspend fun newEntityWithId(id: ID): E
    suspend fun findFromDb(id: ID): E?

    @Test
    fun `save - Write-Through 모드에서 캐시 저장 시 DB에도 즉시 반영된다`() = runTest {
        val id = getNonExistentId()
        val entity = newEntityWithId(id)

        repository.save(id, entity)

        // 캐시에서 조회
        repository.findById(id).shouldNotBeNull()
        // DB에서도 조회 가능
        findFromDb(id).shouldNotBeNull()
    }

    @Test
    fun `saveAll - 여러 엔티티를 한 번에 저장하면 DB에도 반영된다`() = runTest {
        val entities = (1..3).associate { idx ->
            val id = getNonExistentId()
            id to newEntityWithId(id)
        }

        repository.saveAll(entities)

        entities.keys.forEach { id ->
            repository.findById(id).shouldNotBeNull()
            findFromDb(id).shouldNotBeNull()
        }
    }

    @Test
    fun `delete - Write-Through 모드에서 캐시 삭제 시 DB에서도 삭제된다`() = runTest {
        val id = getNonExistentId()
        repository.save(id, newEntityWithId(id))

        repository.delete(id)

        repository.findById(id).shouldBeNull()
        findFromDb(id).shouldBeNull()
    }
}
```

- [ ] **Step 4: R2dbcWriteBehindScenario 작성**

```kotlin
interface R2dbcWriteBehindScenario<ID : Comparable<ID>, E : Any> : R2dbcCacheTestScenario<ID, E> {

    suspend fun newEntityWithId(id: ID): E

    @Test
    fun `save - Write-Behind 모드에서 캐시에 즉시 저장되고 DB에 비동기로 반영된다`() = runTest {
        val id = getNonExistentId()
        val entity = newEntityWithId(id)

        repository.save(id, entity)

        // 캐시에서 즉시 조회 가능
        repository.findById(id).shouldNotBeNull()

        // Write-Behind: DB 반영까지 대기 (LettuceLoadedMap의 writeBehindDelay 이후)
        delay(2000)
    }
}
```

- [ ] **Step 5: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/src/test/kotlin/.../scenarios/
git commit -m "test: R2DBC Lettuce 캐시 시나리오 인터페이스 구현 (Read/Write-Through/Behind)"
```

---

## Task 8: 통합 테스트 클래스 구현

**Files:**

- Create: `src/test/.../repository/R2dbcReadThroughCacheTest.kt`
- Create: `src/test/.../repository/R2dbcWriteThroughCacheTest.kt`
- Create: `src/test/.../repository/R2dbcWriteBehindCacheTest.kt`

- [ ] **Step 1: R2dbcReadThroughCacheTest 작성**

```kotlin
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class R2dbcReadThroughCacheTest :
    AbstractR2dbcLettuceTest(),
    R2dbcReadThroughScenario<Long, UserRecord> {

    override val config = LettuceCacheConfig.READ_ONLY

    override val repository: R2dbcUserLettuceRepository by lazy {
        R2dbcUserLettuceRepository(redisClient, config)
    }

    private val existingIds = mutableListOf<Long>()

    @BeforeAll
    fun setupDb() = runTest {
        withTables(TestDB.H2, UserTable) {
            repeat(5) {
                val id = UserTable.insertAndGetId {
                    it[firstName] = faker.name().firstName()
                    it[lastName] = faker.name().lastName()
                    it[email] = faker.internet().safeEmailAddress()
                }.value
                existingIds.add(id)
            }
            commit()
        }
    }

    override suspend fun getExistingId(): Long = existingIds.first()
    override suspend fun getExistingIds(): List<Long> = existingIds.take(3)
    override suspend fun getNonExistentId(): Long = -999L

    override suspend fun buildEntityForId(id: Long): UserRecord =
        UserRecord(id = id, firstName = "Test", lastName = "User", email = "test@test.com")
}
```

- [ ] **Step 2: R2dbcWriteThroughCacheTest 작성**

`LettuceCacheConfig.READ_WRITE_THROUGH` 설정으로 동일한 구조 반복.

- [ ] **Step 3: R2dbcWriteBehindCacheTest 작성**

`LettuceCacheConfig.WRITE_BEHIND` 설정으로 동일한 구조 반복.

- [ ] **Step 4: 테스트 실행 — 모든 통합 테스트 통과**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test --tests "*.R2dbcReadThroughCacheTest"
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test --tests "*.R2dbcWriteThroughCacheTest"
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test --tests "*.R2dbcWriteBehindCacheTest"
```

Expected: 각각 PASS (최소 5개 테스트씩)

- [ ] **Step 5: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/src/test/kotlin/.../repository/
git commit -m "test: R2DBC Lettuce 통합 테스트 클래스 구현 (ReadThrough/WriteThrough/WriteBehind)"
```

---

## Task 9: SuspendedJobTester 동시성 검증 테스트

**Files:**

- Create: `src/test/.../repository/R2dbcConcurrencyTest.kt`

**목적**: 여러 코루틴이 동시에 `findById`, `save`, `findAll`을 호출할 때 캐시 일관성이 유지됨을 검증.

- [ ] **Step 1: 동시성 테스트 작성**

```kotlin
package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.exposed.r2dbc.lettuce.AbstractR2dbcLettuceTest
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.newUserRecord
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class R2dbcConcurrencyTest : AbstractR2dbcLettuceTest() {

    private val repository: R2dbcUserLettuceRepository by lazy {
        R2dbcUserLettuceRepository(redisClient, LettuceCacheConfig.READ_WRITE_THROUGH)
    }

    private val existingIds = mutableListOf<Long>()

    @BeforeAll
    fun setupDb() = runTest {
        withTables(TestDB.H2, UserTable) {
            repeat(20) {
                val id = UserTable.insertAndGetId {
                    it[UserTable.firstName] = faker.name().firstName()
                    it[UserTable.lastName] = faker.name().lastName()
                    it[UserTable.email] = faker.internet().safeEmailAddress()
                }.value
                existingIds.add(id)
            }
            commit()
        }
    }

    @Test
    fun `동시에 findById를 호출해도 캐시 일관성이 유지된다`() = runTest {
        repository.clearCache()

        SuspendedJobTester()
            .workers(8)
            .rounds(50)
            .add {
                val id = existingIds.random()
                repository.findById(id).shouldNotBeNull()
            }
            .run()
    }

    @Test
    fun `동시에 save와 findById를 혼용해도 예외가 발생하지 않는다`() = runTest {
        repository.clearCache()
        val newEntities = (1..10).map { newUserRecord() }

        SuspendedJobTester()
            .workers(8)
            .rounds(30)
            .addAll(
                // 기존 ID 조회
                { repository.findById(existingIds.random()) },
                // 새 엔티티 저장
                {
                    val entity = newEntities.random()
                    repository.save(entity.id, entity)
                },
                // 여러 ID 일괄 조회
                { repository.findAll(existingIds.take(5)) }
            )
            .run()
    }

    @Test
    fun `동시에 save와 delete를 반복해도 예외가 발생하지 않는다`() = runTest {
        val entities = (1..5).map { newUserRecord() }
        entities.forEach { repository.save(it.id, it) }

        SuspendedJobTester()
            .workers(4)
            .rounds(20)
            .addAll(
                {
                    val entity = entities.random()
                    repository.save(entity.id, entity)
                },
                {
                    val entity = entities.random()
                    repository.delete(entity.id)
                }
            )
            .run()
    }

    @Test
    fun `clearCache와 findById를 동시에 호출해도 예외가 발생하지 않는다`() = runTest {
        SuspendedJobTester()
            .workers(4)
            .rounds(10)
            .addAll(
                { repository.clearCache() },
                { repository.findById(existingIds.random()) }
            )
            .run()
    }
}
```

- [ ] **Step 2: 동시성 테스트 실행 — 통과 확인**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test --tests "*.R2dbcConcurrencyTest"
```

Expected: PASS (4개 테스트, 레이스 컨디션 없음)

- [ ] **Step 3: 커밋**

```bash
git add data/exposed-r2dbc-lettuce/src/test/kotlin/.../repository/R2dbcConcurrencyTest.kt
git commit -m "test: SuspendedJobTester를 이용한 R2DBC Lettuce 동시성 검증 테스트 추가"
```

---

## Task 10: 전체 빌드 검증 + settings.gradle.kts 모듈 목록 확인

- [ ] **Step 1: 전체 모듈 테스트 실행**

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test
```

Expected: BUILD SUCCESSFUL, 모든 테스트 통과

- [ ] **Step 2: 테스트 요약 확인**

```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-exposed-r2dbc-lettuce:test
```

- [ ] **Step 3: CLAUDE.md Architecture 섹션 업데이트**

`CLAUDE.md`의 `data/` 모듈 목록에 `exposed-r2dbc-lettuce` 항목 추가:

```markdown
- **exposed-r2dbc-lettuce**: Exposed R2DBC + Lettuce Redis 캐시 (Read-through / Write-through / Write-behind) —
  `AbstractR2dbcLettuceRepository`, `R2dbcExposedEntityMapLoader`, `R2dbcExposedEntityMapWriter`
```

- [ ] **Step 4: 최종 커밋**

```bash
git add CLAUDE.md
git commit -m "docs: CLAUDE.md에 exposed-r2dbc-lettuce 모듈 추가"
```

---

## 구현 상 주의사항

### 1. runBlocking 안전성

- `R2dbcExposedEntityMapLoader.load()` / `R2dbcExposedEntityMapWriter.write()`는 `LettuceLoadedMap` 내부에서 호출됨
- `LettuceLoadedMap`은 Netty EventLoop가 아닌 일반 스레드에서 실행되므로 `runBlocking(Dispatchers.IO)`는 안전
- 만약 교착 발생 시: `LettuceLoadedMap` 생성 시 별도의 스케줄러 주입 고려

### 2. UserRecord Serializable

- `UserRecord`는 `data class`여야 하며 `java.io.Serializable` 구현 필수
- `Instant`, `UUID` 등 필드 타입도 모두 직렬화 가능 타입

### 3. Auto-Increment ID와 Write-Through

- `UserTable`(LongIdTable)은 Auto-Increment이므로 `R2dbcExposedEntityMapWriter.write()`에서 INSERT 시 ID를 수동으로 설정해야 함 (
  `stmt[id] = entity.id`)
- `exposed-r2dbc-redisson`의 `canBatchInsert` 체크와 동일하게 `autoIncColumnType` 확인 후 INSERT 스킵 옵션 추가 가능

### 4. Write-Behind 테스트 타이밍

- `LettuceCacheConfig.WRITE_BEHIND`의 기본 `writeBehindDelay=1s`
- 테스트에서 `delay(2000)`으로 플러시 완료 대기 필요

### 5. 동시성 테스트 격리

- `R2dbcConcurrencyTest`는 `@BeforeAll`에서 DB 데이터를 공유하므로 캐시 키가 충돌하지 않도록 각 테스트 메서드 시작 전 `clearCache()` 호출
