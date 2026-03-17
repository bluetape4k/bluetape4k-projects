# Redisson/JDBC/R2DBC Repository Generic 리팩토링 구현 계획

> **For agentic workers:
** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (
`- [ ]`) syntax for tracking.

**Goal:
** Redisson Repository의 Generic 파라미터를 Lettuce Repository와 일치시키고, exposed-jdbc/exposed-r2dbc 기본 Repository도 동일한 패턴으로 통일한다.

**Architecture:** `<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>>` → `<ID: Any, E: Any>`로 변경. Table(T) 제네릭 제거,
`HasIdentifier` 의존 제거, `entity.id` 접근을 `extractId(entity)` 추상 메서드로 대체. MapLoader/MapWriter 내부에서는
`Map<ID, E>`의 key로 ID 접근.

**Tech Stack:** Kotlin 2.3, Exposed 1.0+, Redisson, Lettuce, JUnit 5

---

## 변경 범위 요약

### 참조 모델 (Lettuce — 변경 없음)

| 파일                               | Generic                                       |
|----------------------------------|-----------------------------------------------|
| `JdbcLettuceRepository`          | `<ID: Any, E: Any>`                           |
| `SuspendedJdbcLettuceRepository` | `<ID: Any, E: Any>`                           |
| `R2dbcLettuceRepository`         | `<ID: Any, E: Any>`                           |
| `AbstractJdbcLettuceRepository`  | `extractId(entity): ID` (open, error default) |

### 변경 대상

| 모듈                                                                  | 현재 Generic                                        | 목표 Generic                 |
|---------------------------------------------------------------------|---------------------------------------------------|----------------------------|
| **exposed-jdbc-redisson** map (8파일)                                 | `<ID: Any, E: HasIdentifier<ID>>`                 | `<ID: Any, E: Any>`        |
| **exposed-jdbc-redisson** repository (4파일)                          | `<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>>` | `<ID: Any, E: Any>`        |
| **exposed-r2dbc-redisson** map (4파일)                                | `<ID: Any, E: HasIdentifier<ID>>`                 | `<ID: Any, E: Any>`        |
| **exposed-r2dbc-redisson** repository (2파일)                         | `<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>>` | `<ID: Any, E: Any>`        |
| **exposed-jdbc** repository (2파일)                                   | `<ID: Any, T: IdTable<ID>, E: Any>`               | `<ID: Any, E: Any>` (T 제거) |
| **exposed-r2dbc** repository (3파일)                                  | `<ID: Any, T: IdTable<ID>, E: Any>` 등             | `<ID: Any, E: Any>` (T 제거) |
| **테스트** (exposed-jdbc-redisson ~20파일, exposed-r2dbc-redisson ~13파일) | T 포함, HasIdentifier                               | T 제거, extractId 오버라이드      |

### 핵심 패턴 변환

**1. entity.id → extractId(entity)**

```kotlin
// Before (HasIdentifier 의존)
fun put(entity: E) = cache.fastPut(entity.id, entity)
fun putAll(entities: Collection<E>) = cache.putAll(entities.associateBy { it.id })

// After (extractId 패턴)
fun put(entity: E) = cache.fastPut(extractId(entity), entity)
fun putAll(entities: Collection<E>) = cache.putAll(entities.associateBy { extractId(it) })
```

**2. MapWriter 내부 writeThrough/writeBehind — map key 활용**

```kotlin
// Before (HasIdentifier 의존)
private fun <K: Any, V: HasIdentifier<K>> writeThrough(map: Map<K, V>, ...) {
    val entitiesToUpdate = map.values.filter { it.id in existIds }
    entitiesToUpdate.forEach { entity ->
        entityTable.update({ entityTable.id eq entity.id }) { ... }
    }
    val entitiesToInsert = map.values.filterNot { it.id in existIds }
}

// After (map entry 활용)
private fun <K: Comparable<K>, V: Any> writeThrough(map: Map<K, V>, ...) {
    val entriesToUpdate = map.entries.filter { it.key in existIds }
    entriesToUpdate.forEach { (id, entity) ->
        entityTable.update({ entityTable.id eq id }) { updateBody(it, entity) }
    }
    val entriesToInsert = map.entries.filterNot { it.key in existIds }
    entriesToInsert.chunked(batchSize).forEach { chunk ->
        entityTable.batchInsert(chunk, shouldReturnGeneratedValues = false) {
            batchInsertBody(this, it.value)
        }
    }
}
```

**3. Table 제네릭 제거**

```kotlin
// Before
interface JdbcRedissonRepository<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>> {
    val entityTable: T
}

// After
interface JdbcRedissonRepository<ID: Any, E: Any> {
    val table: IdTable<ID>
    fun extractId(entity: E): ID
}
```

**4. SoftDeleted Repository는 T 유지**

```kotlin
// SoftDeletedJdbcRepository는 table.isDeleted 접근이 필요하므로 T 유지
interface SoftDeletedJdbcRepository<ID: Any, T: SoftDeletedIdTable<ID>, E: Any>
    : JdbcRepository<ID, E> {
    override val table: T  // SoftDeletedIdTable<ID>로 오버라이드
}
```

**5. 편의 타입 별칭 업데이트**

```kotlin
// Before
interface LongJdbcRepository<T: LongIdTable, E: Any>: JdbcRepository<Long, T, E>

// After
interface LongJdbcRepository<E: Any>: JdbcRepository<Long, E>

// 또는 T가 필요한 SoftDeleted 계열은 유지
interface LongSoftDeletedJdbcRepository<T: SoftDeletedIdTable<Long>, E: Any>
    : SoftDeletedJdbcRepository<Long, T, E>
```

---

## Task 1: exposed-jdbc-redisson — MapLoader/MapWriter 기반 클래스 (4파일)

`entity.id` 직접 접근을 하지 않는 기반 클래스부터 변경합니다.

**Files:**

- Modify: `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/map/EntityMapLoader.kt`
- Modify: `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/map/EntityMapWriter.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/map/SuspendedEntityMapLoader.kt` (SuspendedEntityMapLoader.kt)
- Modify:
  `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/map/SuspendedEntityMapWriter.kt` (SuspendedEntityMapWriter.kt)

### 변경 내용

**EntityMapLoader.kt:28** — 제네릭 변경:

```kotlin
// Before
open class EntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
// After
    open class EntityMapLoader<ID: Any, E: Any>(
```

- `import io.bluetape4k.exposed.core.HasIdentifier` 제거

**EntityMapWriter.kt:27** — 제네릭 변경:

```kotlin
// Before
open class EntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
// After
    open class EntityMapWriter<ID: Any, E: Any>(
```

- `import io.bluetape4k.exposed.core.HasIdentifier` 제거

**SuspendedEntityMapLoader.kt:49** — 제네릭 변경:

```kotlin
// Before
open class SuspendedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
// After
    open class SuspendedEntityMapLoader<ID: Any, E: Any>(
```

- `import io.bluetape4k.exposed.core.HasIdentifier` 제거

**SuspendedEntityMapWriter.kt:38** — 제네릭 변경:

```kotlin
// Before
open class SuspendedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
// After
    open class SuspendedEntityMapWriter<ID: Any, E: Any>(
```

- `import io.bluetape4k.exposed.core.HasIdentifier` 제거

- [ ] **Step 1: 4개 기반 클래스 제네릭 변경** — `ID: Any` → `ID: Comparable<ID>`, `E: HasIdentifier<ID>` → `E: Any`, import 제거
- [ ] **Step 2: 빌드 확인** — `./gradlew :bluetape4k-exposed-jdbc-redisson:compileKotlin` (아직 실패 예상 — 하위 클래스 미변경)
- [ ] **Step 3: 커밋** — `git commit -m "refactor: exposed-jdbc-redisson MapLoader/MapWriter 기반 클래스 제네릭 변경"`

---

## Task 2: exposed-jdbc-redisson — ExposedEntityMapLoader/Writer 구현 클래스 (4파일)

`HasIdentifier`를 직접 사용하는 구현 클래스를 변경합니다. **`writeThrough`/`writeBehind` 내부의 `entity.id` → map entry key 활용**이 핵심입니다.

**Files:**

- Modify: `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/map/ExposedEntityMapLoader.kt`
- Modify: `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/map/ExposedEntityMapWriter.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/map/SuspendedExposedEntityMapLoader.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/map/SuspendedExposedEntityMapWriter.kt`

### 변경 내용

**ExposedEntityMapLoader.kt:38** — 제네릭만 변경:

```kotlin
// Before
open class ExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
// After
    open class ExposedEntityMapLoader<ID: Any, E : Any>(
```

**ExposedEntityMapWriter.kt** — 제네릭 + writeThrough/writeBehind 내부 로직 변경:

클래스 선언(44행):

```kotlin
// Before
open class ExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
// After
    open class ExposedEntityMapWriter<ID: Any, E: Any>(
```

`writeThrough` companion 함수(75행) — **map entry 기반으로 변환**:

```kotlin
// Before
private fun <K: Any, V: HasIdentifier<K>> writeThrough(
    map: Map<K, V>,
    entityTable: IdTable<K>,
    updateBody: IdTable<K>.(UpdateStatement, V) -> Unit,
    batchInsertBody: BatchInsertStatement.(V) -> Unit,
) {
    val existIds = entityTable.select(entityTable.id)
        .where { entityTable.id inList map.keys }
        .map { it[entityTable.id].value }
    val entitiesToUpdate = map.values.filter { it.id in existIds }
    entitiesToUpdate.forEach { entity ->
        entityTable.update({ entityTable.id eq entity.id }) {
            updateBody(it, entity)
        }
    }
    val canBatchInsert = entityTable.id.autoIncColumnType == null && !entityTable.id.isDatabaseGenerated()
    if (canBatchInsert) {
        val entitiesToInsert = map.values.filterNot { it.id in existIds }
        entitiesToInsert.chunked(DEFAULT_BATCH_SIZE).forEach { chunk ->
            entityTable.batchInsert(chunk, shouldReturnGeneratedValues = false) {
                batchInsertBody(this, it)
            }
        }
    }
}

// After
private fun <K: Comparable<K>, V: Any> writeThrough(
    map: Map<K, V>,
    entityTable: IdTable<K>,
    updateBody: IdTable<K>.(UpdateStatement, V) -> Unit,
    batchInsertBody: BatchInsertStatement.(V) -> Unit,
) {
    log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=${map.keys}" }
    val existIds = entityTable.select(entityTable.id)
        .where { entityTable.id inList map.keys }
        .map { it[entityTable.id].value }

    val entriesToUpdate = map.entries.filter { it.key in existIds }
    entriesToUpdate.forEach { (id, entity) ->
        entityTable.update({ entityTable.id eq id }) {
            updateBody(it, entity)
        }
    }

    val canBatchInsert = entityTable.id.autoIncColumnType == null && !entityTable.id.isDatabaseGenerated()
    if (canBatchInsert) {
        val entriesToInsert = map.entries.filterNot { it.key in existIds }
        log.debug { "ID가 자동증가 타입이 아니므로, batchInsert 를 수행합니다...entities size=${entriesToInsert.size}" }
        entriesToInsert.map { it.value }.chunked(DEFAULT_BATCH_SIZE).forEach { chunk ->
            entityTable.batchInsert(chunk, shouldReturnGeneratedValues = false) {
                batchInsertBody(this, it)
            }
        }
    }
}
```

`writeBehind` companion 함수(107행):

```kotlin
// Before
private fun <K: Any, V: HasIdentifier<K>> writeBehind(
    map: Map<K, V>,
    ...
) {
    val entitiesToInsert = map.values
    entitiesToInsert.chunked(batchSize).forEach { chunk ->
        log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=${chunk.map { it.id }}" }
        ...
    }
}

// After
private fun <K: Comparable<K>, V: Any> writeBehind(
    map: Map<K, V>,
    ...
) {
    map.values.chunked(batchSize).forEach { chunk ->
        log.debug { "캐시 변경 사항을 DB에 반영합니다... chunk size=${chunk.size}" }
        entityTable.batchInsert(chunk, shouldReturnGeneratedValues = false) {
            batchInsertBody(this, it)
        }
    }
}
```

**SuspendedExposedEntityMapLoader.kt:42** — ExposedEntityMapLoader와 동일 패턴:

```kotlin
// Before
open class SuspendedExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
// After
    open class SuspendedExposedEntityMapLoader<ID: Any, E: Any>(
```

**SuspendedExposedEntityMapWriter.kt:46** — ExposedEntityMapWriter와 동일 패턴으로 writeThrough/writeBehind 수정:

```kotlin
// Before
open class SuspendedExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
// After
    open class SuspendedExposedEntityMapWriter<ID: Any, E : Any>(
```

- `writeThrough`/`writeBehind` companion 함수도 ExposedEntityMapWriter와 동일하게 map entry 기반으로 변경

- [ ] **Step 1: ExposedEntityMapLoader 제네릭 변경**
- [ ] **Step 2: ExposedEntityMapWriter 제네릭 + writeThrough/writeBehind 로직 변경**
- [ ] **Step 3: SuspendedExposedEntityMapLoader 제네릭 변경**
- [ ] **Step 4: SuspendedExposedEntityMapWriter 제네릭 + writeThrough/writeBehind 로직 변경**
- [ ] **Step 5: 모든 HasIdentifier import 제거 확인**
- [ ] **Step 6: 커밋** — `git commit -m "refactor: exposed-jdbc-redisson Exposed MapLoader/Writer 구현 클래스 제네릭 변경"`

---

## Task 3: exposed-jdbc-redisson — Repository 인터페이스 (2파일)

**Files:**

- Modify:
  `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/repository/JdbcRedissonRepository.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/repository/SuspendedJdbcRedissonRepository.kt`

### JdbcRedissonRepository.kt 변경

```kotlin
// Before (36행)
interface JdbcRedissonRepository<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>> {
    val cacheName: String
    val entityTable: T
    fun ResultRow.toEntity(): E
    val cache: RMap<ID, E?>

    fun put(entity: E) = cache.fastPut(entity.id, entity)
    fun putAll(entities: Collection<E>, batchSize: Int = DEFAULT_BATCH_SIZE) {
        cache.putAll(entities.associateBy { it.id }, batchSize)
    }
}

// After
interface JdbcRedissonRepository<ID: Any, E: Any> {
    val cacheName: String
    val table: IdTable<ID>
    fun ResultRow.toEntity(): E
    val cache: RMap<ID, E?>

    /** 엔티티에서 ID를 추출합니다. 구현 클래스에서 반드시 오버라이드해야 합니다. */
    fun extractId(entity: E): ID

    fun put(entity: E) = cache.fastPut(extractId(entity), entity)
    fun putAll(entities: Collection<E>, batchSize: Int = DEFAULT_BATCH_SIZE) {
        require(batchSize > 0) { "batchSize must be greater than 0. batchSize=$batchSize" }
        cache.putAll(entities.associateBy { extractId(it) }, batchSize)
    }
}
```

- `entityTable` → `table` 이름 변경 (Lettuce 패턴과 일치)
- `findByIdFromDb`, `findAllFromDb` default 구현 내 `entityTable` → `table`
- `findAll` 시그니처의 `sortBy: Expression<*> = entityTable.id` → `sortBy: Expression<*> = table.id`
- `import io.bluetape4k.exposed.core.HasIdentifier` 제거
- `import org.jetbrains.exposed.v1.core.dao.id.IdTable` 유지

### SuspendedJdbcRedissonRepository.kt 변경

동일 패턴. 추가로 async 메서드들:

```kotlin
// Before
suspend fun put(entity: E): Boolean = cache.fastPutAsync(entity.id, entity).await()
suspend fun putAll(entities: Collection<E>, batchSize: Int = DEFAULT_BATCH_SIZE) {
    cache.putAllAsync(entities.associateBy { it.id }, batchSize).await()
}

// After
suspend fun put(entity: E): Boolean = cache.fastPutAsync(extractId(entity), entity).await()
suspend fun putAll(entities: Collection<E>, batchSize: Int = DEFAULT_BATCH_SIZE) {
    batchSize.requirePositiveNumber("batchSize")
    cache.putAllAsync(entities.associateBy { extractId(it) }, batchSize).await()
}
```

- [ ] **Step 1: JdbcRedissonRepository 제네릭/멤버/메서드 변경**
- [ ] **Step 2: SuspendedJdbcRedissonRepository 제네릭/멤버/메서드 변경**
- [ ] **Step 3: 커밋** — `git commit -m "refactor: exposed-jdbc-redisson Repository 인터페이스 제네릭 변경"`

---

## Task 4: exposed-jdbc-redisson — Repository 추상 클래스 (2파일)

**Files:**

- Modify:
  `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/repository/AbstractJdbcRedissonRepository.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/repository/AbstractSuspendedJdbcRedissonRepository.kt`

### AbstractJdbcRedissonRepository.kt 변경

```kotlin
// Before (62행)
abstract class AbstractJdbcRedissonRepository<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    protected val config: RedissonCacheConfig,
): JdbcRedissonRepository<ID, T, E> {

    protected open val mapLoader: EntityMapLoader<ID, E> by lazy {
        ExposedEntityMapLoader(entityTable) { toEntity() }
    }

    override fun findAll(...): List<E> {
        ...
        cache.putAll(entities.associateBy { it.id })
        ...
    }
}

// After
abstract class AbstractJdbcRedissonRepository<ID: Any, E: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    protected val config: RedissonCacheConfig,
): JdbcRedissonRepository<ID, E> {

    protected open val mapLoader: EntityMapLoader<ID, E> by lazy {
        ExposedEntityMapLoader(table) { toEntity() }
    }

    override fun findAll(...): List<E> {
        ...
        cache.putAll(entities.associateBy { extractId(it) })
        ...
    }
}
```

- `entityTable` 참조 → `table` 참조 (mapLoader, mapWriter, createLocalCacheMap, createMapCache 등)
- `entities.associateBy { it.id }` → `entities.associateBy { extractId(it) }`
- `import io.bluetape4k.exposed.core.HasIdentifier` 제거

### AbstractSuspendedJdbcRedissonRepository.kt 변경

동일 패턴:

```kotlin
// Before (72행)
abstract class AbstractSuspendedJdbcRedissonRepository<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>>(
// After
abstract class AbstractSuspendedJdbcRedissonRepository<ID: Any, E: Any>(
```

- `entityTable` → `table`
- `entities.associateBy { it.id }` → `entities.associateBy { extractId(it) }`
- `SuspendedExposedEntityMapLoader(entityTable, scope) { toEntity() }` →
  `SuspendedExposedEntityMapLoader(table, scope) { toEntity() }`

- [ ] **Step 1: AbstractJdbcRedissonRepository 변경**
- [ ] **Step 2: AbstractSuspendedJdbcRedissonRepository 변경**
- [ ] **Step 3: 빌드 확인** — `./gradlew :bluetape4k-exposed-jdbc-redisson:compileKotlin`
- [ ] **Step 4: 커밋** — `git commit -m "refactor: exposed-jdbc-redisson Repository 추상 클래스 제네릭 변경"`

---

## Task 5: exposed-r2dbc-redisson — MapLoader/MapWriter (4파일)

**Files:**

- Modify: `data/exposed-r2dbc-redisson/src/main/kotlin/io/bluetape4k/exposed/r2dbc/redisson/map/R2dbcEntityMapLoader.kt`
- Modify: `data/exposed-r2dbc-redisson/src/main/kotlin/io/bluetape4k/exposed/r2dbc/redisson/map/R2dbcEntityMapWriter.kt`
- Modify:
  `data/exposed-r2dbc-redisson/src/main/kotlin/io/bluetape4k/exposed/r2dbc/redisson/map/R2dbcExposedEntityMapLoader.kt`
- Modify:
  `data/exposed-r2dbc-redisson/src/main/kotlin/io/bluetape4k/exposed/r2dbc/redisson/map/R2dbcExposedEntityMapWriter.kt`

### 변경 패턴 (Task 1~2와 동일)

**R2dbcEntityMapLoader.kt:43**:

```kotlin
// Before: open class R2dbcEntityMapLoader<ID: Any, E: HasIdentifier<ID>>
// After:  open class R2dbcEntityMapLoader<ID: Any, E: Any>
```

**R2dbcEntityMapWriter.kt:36**:

```kotlin
// Before: open class R2dbcEntityMapWriter<ID: Any, E: HasIdentifier<ID>>
// After:  open class R2dbcEntityMapWriter<ID: Any, E: Any>
```

**R2dbcExposedEntityMapLoader.kt:42**:

```kotlin
// Before: open class R2dbcExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>
// After:  open class R2dbcExposedEntityMapLoader<ID: Any, E: Any>
```

**R2dbcExposedEntityMapWriter.kt:50** — writeThrough/writeBehind도 Task 2와 동일한 map entry 패턴:

```kotlin
// Before: open class R2dbcExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>
// After:  open class R2dbcExposedEntityMapWriter<ID: Any, E: Any>
```

- `writeThrough`(83행), `writeBehind`(118행) companion 함수의 `<K: Any, V: HasIdentifier<K>>` →
  `<K: Comparable<K>, V: Any>`, entity.id → map entry key

- [ ] **Step 1: 4개 파일 제네릭 변경 + writeThrough/writeBehind 로직 변경**
- [ ] **Step 2: 모든 HasIdentifier import 제거 확인**
- [ ] **Step 3: 커밋** — `git commit -m "refactor: exposed-r2dbc-redisson MapLoader/MapWriter 제네릭 변경"`

---

## Task 6: exposed-r2dbc-redisson — Repository (2파일)

**Files:**

- Modify:
  `data/exposed-r2dbc-redisson/src/main/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/R2dbcRedissonRepository.kt`
- Modify:
  `data/exposed-r2dbc-redisson/src/main/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/AbstractR2dbcRedissonRepository.kt`

### R2dbcRedissonRepository.kt 변경

```kotlin
// Before (39행)
interface R2dbcRedissonRepository<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>> {
    val entityTable: T
    suspend fun ResultRow.toEntity(): E
    suspend fun put(entity: E): Boolean? = cache.fastPutAsync(entity.id, entity).await()
    suspend fun putAll(entities: Collection<E>, batchSize: Int = DEFAULT_BATCH_SIZE) {
        cache.putAllAsync(entities.associateBy { it.id }, batchSize).await()
    }
}

// After
interface R2dbcRedissonRepository<ID: Any, E: Any> {
    val table: IdTable<ID>
    suspend fun ResultRow.toEntity(): E
    fun extractId(entity: E): ID
    suspend fun put(entity: E): Boolean? = cache.fastPutAsync(extractId(entity), entity).await()
    suspend fun putAll(entities: Collection<E>, batchSize: Int = DEFAULT_BATCH_SIZE) {
        require(batchSize > 0) { "batchSize must be greater than 0. batchSize=$batchSize" }
        cache.putAllAsync(entities.associateBy { extractId(it) }, batchSize).await()
    }
}
```

### AbstractR2dbcRedissonRepository.kt 변경

```kotlin
// Before (56행)
abstract class AbstractR2dbcRedissonRepository<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>>(
// After
abstract class AbstractR2dbcRedissonRepository<ID: Any, E: Any>(
```

- `entityTable` → `table`, `entities.associateBy { it.id }` → `entities.associateBy { extractId(it) }`
- R2dbc loader/writer 생성 시 `entityTable` → `table`

- [ ] **Step 1: R2dbcRedissonRepository 인터페이스 변경**
- [ ] **Step 2: AbstractR2dbcRedissonRepository 추상 클래스 변경**
- [ ] **Step 3: 빌드 확인** — `./gradlew :bluetape4k-exposed-r2dbc-redisson:compileKotlin`
- [ ] **Step 4: 커밋** — `git commit -m "refactor: exposed-r2dbc-redisson Repository 제네릭 변경"`

---

## Task 7: exposed-jdbc — JdbcRepository + SoftDeletedJdbcRepository (2파일)

**Files:**

- Modify: `data/exposed-jdbc/src/main/kotlin/io/bluetape4k/exposed/jdbc/repository/JdbcRepository.kt`
- Modify: `data/exposed-jdbc/src/main/kotlin/io/bluetape4k/exposed/jdbc/repository/SoftDeletedJdbcRepository.kt`

### JdbcRepository.kt 변경

```kotlin
// Before (92행)
interface JdbcRepository<ID : Any, T : IdTable<ID>, E : Any> {
    val table: T
    ...
}

// After
interface JdbcRepository<ID: Any, E : Any> {
    val table: IdTable<ID>
    ...
}
```

- `table: T` → `table: IdTable<ID>` (T 제거)
- `deleteAll`, `deleteAllIgnore` 등의 `op: (IdTable<ID>).() -> Op<Boolean>` — 변경 없음
- `updateById`, `updateAll`의 `updateStatement: IdTable<ID>.(UpdateStatement) -> Unit` — 변경 없음

**편의 타입 별칭 변경**:

```kotlin
// Before
interface IntJdbcRepository<T : IntIdTable, E : Any> : JdbcRepository<Int, T, E>
interface LongJdbcRepository<T : LongIdTable, E : Any> : JdbcRepository<Long, T, E>
interface UuidJdbcRepository<T : UuidTable, E : Any> : JdbcRepository<Uuid, T, E>
interface UUIDJdbcRepository<T : UUIDTable, E : Any> : JdbcRepository<UUID, T, E>
interface StringJdbcRepository<T : IdTable<String>, E : Any> : JdbcRepository<String, T, E>

// After
interface IntJdbcRepository<E : Any> : JdbcRepository<Int, E>
interface LongJdbcRepository<E : Any> : JdbcRepository<Long, E>
@OptIn(ExperimentalUuidApi::class)
interface UuidJdbcRepository<E : Any> : JdbcRepository<Uuid, E>
interface UUIDJdbcRepository<E : Any> : JdbcRepository<UUID, E>
interface StringJdbcRepository<E : Any> : JdbcRepository<String, E>
```

### SoftDeletedJdbcRepository.kt 변경

`SoftDeletedJdbcRepository`는 `table.isDeleted` 접근이 필요하므로 T 유지:

```kotlin
// Before (58행)
interface SoftDeletedJdbcRepository<ID: Any, T: SoftDeletedIdTable<ID>, E: Any>: JdbcRepository<ID, T, E> {
    override val table: T
}

// After
interface SoftDeletedJdbcRepository<ID: Any, T: SoftDeletedIdTable<ID>, E: Any>: JdbcRepository<ID, E> {
    override val table: T  // SoftDeletedIdTable<ID>로 좁혀서 isDeleted 접근 가능
}
```

**편의 타입 별칭**:

```kotlin
// Before
interface LongSoftDeletedJdbcRepository<T: SoftDeletedIdTable<Long>, E: Any>: SoftDeletedJdbcRepository<Long, T, E>

// After (동일 — T 유지)
interface LongSoftDeletedJdbcRepository<T: SoftDeletedIdTable<Long>, E: Any>: SoftDeletedJdbcRepository<Long, T, E>
```

- [ ] **Step 1: JdbcRepository 제네릭 변경 (T 제거, ID: Comparable)**
- [ ] **Step 2: 편의 타입 별칭 업데이트**
- [ ] **Step 3: SoftDeletedJdbcRepository 변경 (T 유지, ID: Comparable)**
- [ ] **Step 4: 빌드 확인** — `./gradlew :bluetape4k-exposed-jdbc:compileKotlin`
- [ ] **Step 5: 커밋** — `git commit -m "refactor: exposed-jdbc JdbcRepository T 제네릭 제거, ID: Comparable 변경"`

---

## Task 8: exposed-r2dbc — R2dbcRepository + ExposedR2dbcRepository + SoftDeleted (3파일)

**Files:**

- Modify: `data/exposed-r2dbc/src/main/kotlin/io/bluetape4k/exposed/r2dbc/repository/R2dbcRepository.kt`
- Modify: `data/exposed-r2dbc/src/main/kotlin/io/bluetape4k/exposed/r2dbc/repository/ExposedR2dbcRepository.kt`
- Modify: `data/exposed-r2dbc/src/main/kotlin/io/bluetape4k/exposed/r2dbc/repository/SoftDeletedR2dbcRepository.kt`

### R2dbcRepository.kt 변경

```kotlin
// Before (95행)
interface R2dbcRepository<ID : Any, T : IdTable<ID>, E : Any> {
    val table: T
}

// After
interface R2dbcRepository<ID: Any, E : Any> {
    val table: IdTable<ID>
}
```

**편의 타입 별칭**:

```kotlin
// After
interface IntR2dbcRepository<E : Any> : R2dbcRepository<Int, E>
interface LongR2dbcRepository<E : Any> : R2dbcRepository<Long, E>
interface UuidR2dbcRepository<E : Any> : R2dbcRepository<Uuid, E>
interface UUIDR2dbcRepository<E : Any> : R2dbcRepository<UUID, E>
interface StringR2dbcRepository<E : Any> : R2dbcRepository<String, E>
```

### ExposedR2dbcRepository.kt (Deprecated) 변경

```kotlin
// Before (49행)
interface ExposedR2dbcRepository<T : HasIdentifier<ID>, ID : Any> {
    suspend fun delete(entity: T): Int = table.deleteWhere { table.id eq entity.id }
    suspend fun deleteIgnore(entity: T): Int = table.deleteIgnoreWhere { table.id eq entity.id }
}

// After
interface ExposedR2dbcRepository<ID: Any, E : Any> {
    fun extractId(entity: E): ID
    suspend fun delete(entity: E): Int = table.deleteWhere { table.id eq extractId(entity) }
    suspend fun deleteIgnore(entity: E): Int = table.deleteIgnoreWhere { table.id eq extractId(entity) }
}
```

- `T : HasIdentifier<ID>` → `E : Any` (제네릭 파라미터명도 T→E로 변경)
- `entity.id` → `extractId(entity)`
- `import io.bluetape4k.exposed.core.HasIdentifier` 제거

### SoftDeletedR2dbcRepository.kt 변경

```kotlin
// Before (59행)
interface SoftDeletedR2dbcRepository<ID: Any, T: SoftDeletedIdTable<ID>, E: Any>: R2dbcRepository<ID, T, E> {

// After
interface SoftDeletedR2dbcRepository<ID: Any, T: SoftDeletedIdTable<ID>, E: Any>: R2dbcRepository<ID, E> {
    override val table: T  // SoftDeletedIdTable<ID>로 좁혀서 isDeleted 접근 가능
}
```

**편의 타입 별칭**도 동일 패턴.

- [ ] **Step 1: R2dbcRepository 제네릭 변경 (T 제거, ID: Comparable)**
- [ ] **Step 2: ExposedR2dbcRepository 변경 (HasIdentifier 제거, extractId 도입)**
- [ ] **Step 3: SoftDeletedR2dbcRepository 변경 (T 유지, ID: Comparable)**
- [ ] **Step 4: 빌드 확인** — `./gradlew :bluetape4k-exposed-r2dbc:compileKotlin`
- [ ] **Step 5: 커밋** — `git commit -m "refactor: exposed-r2dbc R2dbcRepository T 제네릭 제거, ID: Comparable 변경"`

---

## Task 9: exposed-jdbc-redisson 테스트 업데이트

**Files:**

- Modify: `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/domain/UserSchema.kt` —
  `HasIdentifier` 유지 가능 (data class 자체는 변경 불필요, 다만 Repository가 더 이상 요구하지 않음)
- Modify: `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/domain/UserCacheRepository.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/domain/UserCredentialCacheRepository.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/domain/SuspendedUserCacheRepository.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/domain/SuspendedUserCredentialCacheRepository.kt`
- Modify:
  `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/repository/scenarios/*.kt` (7 시나리오 파일)
- Modify: `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/repository/*.kt` (6 테스트 파일)
- Modify: `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/map/ExposedEntityMapLoaderTest.kt`

### 주요 변경 패턴

**테스트 Repository 클래스**:

```kotlin
// Before
class UserCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String,
    config: RedissonCacheConfig,
): AbstractJdbcRedissonRepository<Long, UserTable, UserRecord>(redissonClient, cacheName, config) {
    override val entityTable = UserTable
    override fun ResultRow.toEntity() = toUserRecord()
}

// After
class UserCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String,
    config: RedissonCacheConfig,
): AbstractJdbcRedissonRepository<Long, UserRecord>(redissonClient, cacheName, config) {
    override val table = UserTable
    override fun ResultRow.toEntity() = toUserRecord()
    override fun extractId(entity: UserRecord): Long = entity.id!!
}
```

**시나리오 인터페이스**:

```kotlin
// Before
interface CacheTestScenario<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>> {
// After
interface CacheTestScenario<ID: Any, E: Any> {
```

**테스트 클래스의 Nested 클래스**:

```kotlin
// Before
abstract inner class AutoIncIdReadThrough:
    AbstractJdbcRedissonRepository<Long, UserTable, UserRecord>(...),
    ReadThroughScenario<Long, UserTable, UserRecord>

// After
abstract inner class AutoIncIdReadThrough:
    AbstractJdbcRedissonRepository<Long, UserRecord>(...),
    ReadThroughScenario<Long, UserRecord>
```

- [ ] **Step 1: 테스트 도메인 Repository 4개 클래스 변경 (extractId 오버라이드 추가)**
- [ ] **Step 2: 시나리오 인터페이스 7개 변경 (T 제거, HasIdentifier 제거)**
- [ ] **Step 3: 테스트 클래스 6개 + MapLoaderTest 1개 변경**
- [ ] **Step 4: 테스트 실행** — `./gradlew :bluetape4k-exposed-jdbc-redisson:test`
- [ ] **Step 5: 커밋** — `git commit -m "test: exposed-jdbc-redisson 테스트 제네릭 리팩토링"`

---

## Task 10: exposed-r2dbc-redisson 테스트 업데이트

**Files:**

- Modify: `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/domain/UserSchema.kt`
- Modify:
  `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/domain/R2dbcUserRedissonRepository.kt`
- Modify:
  `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/domain/R2dbcUserCredentialRedissonRepository.kt`
- Modify:
  `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/scenario/*.kt` (4 시나리오 파일)
- Modify: `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/*.kt` (3 테스트 파일)
- Modify:
  `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/map/R2dbcExposedEntityMapLoaderTest.kt`

Task 9와 동일한 패턴 적용.

- [ ] **Step 1: 테스트 도메인 Repository 2개 클래스 변경 (extractId 오버라이드 추가)**
- [ ] **Step 2: 시나리오 인터페이스 4개 변경**
- [ ] **Step 3: 테스트 클래스 3개 + MapLoaderTest 1개 변경**
- [ ] **Step 4: 테스트 실행** — `./gradlew :bluetape4k-exposed-r2dbc-redisson:test`
- [ ] **Step 5: 커밋** — `git commit -m "test: exposed-r2dbc-redisson 테스트 제네릭 리팩토링"`

---

## Task 11: exposed-jdbc / exposed-r2dbc 하위 의존 모듈 컴파일 수정

`JdbcRepository`와 `R2dbcRepository`의 T 제거로 인해 이를 상속하는 다른 모듈에서 컴파일 오류가 발생합니다.

**영향 받는 모듈 (Grep으로 확인 필요)**:

- `data/exposed-jdbc-redisson/` — `JdbcRepository`를 직접 사용하지는 않음 (별도 인터페이스)
- `data/exposed-r2dbc-redisson/` — 동일
- `data/exposed-jdbc-lettuce/` — `JdbcRepository`를 직접 사용하지 않음
- `data/exposed-r2dbc-lettuce/` — 동일
- `data/exposed-jdbc-tests/` — 테스트 인프라
- `data/exposed-r2dbc-tests/` — 테스트 인프라
- `timefold/solver-persistence-exposed/` — `JdbcRepository` 사용 가능
- `examples/` — 예제 코드

각 모듈에서 `JdbcRepository<ID, T, E>` → `JdbcRepository<ID, E>`, `R2dbcRepository<ID, T, E>` → `R2dbcRepository<ID, E>` 변경.

- [ ] **Step 1: `rg "JdbcRepository<|R2dbcRepository<|ExposedR2dbcRepository<" data/ --type kotlin` 로 영향 범위 파악**
- [ ] **Step 2: 각 모듈의 구현/테스트 코드에서 T 제거 및 ID: Comparable 변경**
- [ ] **Step 3: 전체 빌드** — `./gradlew build -x test`
- [ ] **Step 4: 커밋** — `git commit -m "refactor: exposed-jdbc/r2dbc 하위 의존 모듈 제네릭 업데이트"`

---

## Task 12: 전체 테스트 검증

- [ ] **Step 1: exposed-jdbc 모듈 테스트** — `./gradlew :bluetape4k-exposed-jdbc:test`
- [ ] **Step 2: exposed-r2dbc 모듈 테스트** — `./gradlew :bluetape4k-exposed-r2dbc:test`
- [ ] **Step 3: exposed-jdbc-redisson 모듈 테스트** — `./gradlew :bluetape4k-exposed-jdbc-redisson:test`
- [ ] **Step 4: exposed-r2dbc-redisson 모듈 테스트** — `./gradlew :bluetape4k-exposed-r2dbc-redisson:test`
- [ ] **Step 5: exposed-jdbc-lettuce 모듈 테스트 (regression)** — `./gradlew :bluetape4k-exposed-jdbc-lettuce:test`
- [ ] **Step 6: exposed-r2dbc-lettuce 모듈 테스트 (regression)** — `./gradlew :bluetape4k-exposed-r2dbc-lettuce:test`
- [ ] **Step 7: 실패 시 수정 후 재테스트**
- [ ] **Step 8: 최종 커밋** — `git commit -m "test: 전체 Exposed Repository 제네릭 리팩토링 검증 완료"`

---

## Task 13: CLAUDE.md 업데이트

루트 `CLAUDE.md`의 Architecture > Module Structure > Data Modules 섹션에 변경사항 반영:

- Redisson Repository 제네릭 패턴 설명 업데이트
- `HasIdentifier` 의존 제거, `extractId` 패턴 설명 추가
- JdbcRepository/R2dbcRepository의 T 제네릭 제거 설명

- [ ] **Step 1: CLAUDE.md 업데이트**
- [ ] **Step 2: 커밋** — `git commit -m "docs: CLAUDE.md Repository 제네릭 리팩토링 반영"`
