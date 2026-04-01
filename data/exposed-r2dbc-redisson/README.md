# Module bluetape4k-exposed-r2dbc-redisson

Exposed R2DBC와 Redisson 캐시를 결합해 비동기 Read-Through/Write-Through 캐시 패턴을 구성하는 모듈입니다.

## 개요

`bluetape4k-exposed-r2dbc-redisson`은 Exposed R2DBC(비동기)와 [Redisson](https://github.com/redisson/redisson) Redis 클라이언트를 통합하여,
비동기 환경에서 데이터베이스 조회 결과를 Redis에 캐싱하는 패턴을 쉽게 구현할 수 있도록 지원합니다.
모든 인터페이스는 `suspend` 함수 기반이며 Kotlin Coroutines와 완벽하게 호환됩니다.

### 주요 기능

- **MapLoader/MapWriter 비동기 지원**: Redisson `AsyncMapLoader`/`AsyncMapWriter` 연동
  - `loadAllKeys()`는 PK 오름차순으로 안정적으로 순회
- **Repository 추상화**: 캐시 + DB 접근 공통 패턴 (`R2dbcRedissonRepository`)
- **Coroutines 네이티브**: 모든 연산이 `suspend` 함수
- **Near Cache 지원**: Local Cache + Redis 2-Tier 캐시
- **Read-Through/Write-Through/Write-Behind**: 다양한 캐시 패턴 지원

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc-redisson:${version}")
    implementation("org.redisson:redisson:3.37.0")

    // R2DBC 드라이버
    implementation("org.postgresql:r2dbc-postgresql:1.0.5.RELEASE")
}
```

## 기본 사용법

### 1. R2dbcRedissonRepository 구현

`AbstractR2dbcRedissonRepository`를 상속하여 비동기 캐시 Repository를 구현합니다.

```kotlin
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.redisson.repository.AbstractR2dbcRedissonRepository
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient

// 엔티티 (java.io.Serializable 필수)
data class UserRecord(
    override val id: Long,
    val name: String,
    val email: String,
): HasIdentifier<Long>, java.io.Serializable

object UserTable: LongIdTable("users") {
    val name = varchar("name", 100)
    val email = varchar("email", 200)
}

class UserR2dbcRedissonRepository(
    redissonClient: RedissonClient,
    config: RedisCacheConfig,
): AbstractR2dbcRedissonRepository<Long, UserTable, UserRecord>(
    redissonClient = redissonClient,
    cacheName = "users",
    config = config,
) {
    override val entityTable = UserTable

    override suspend fun ResultRow.toEntity() = UserRecord(
        id    = this[UserTable.id].value,
        name  = this[UserTable.name],
        email = this[UserTable.email],
    )

    // Write-Through 모드 시 구현 필요
    override fun doUpdateEntity(statement: UpdateStatement, entity: UserRecord) {
        statement[UserTable.name]  = entity.name
        statement[UserTable.email] = entity.email
    }
}

// 사용 (모든 메서드가 suspend)
val repo = UserR2dbcRedissonRepository(redissonClient, RedisCacheConfig.readOnly())

// 캐시에서 조회 (미스 시 DB에서 자동 로드)
val user = repo.get(1L)

// DB에서 직접 조회
val freshUser = repo.findByIdFromDb(1L)

// DB 조회 후 캐시 저장
val all = repo.findAll(limit = 100)

// 캐시에 저장
repo.put(user!!)
repo.putAll(users)

// 캐시 무효화
repo.invalidate(1L)
repo.invalidateAll()
repo.invalidateByPattern("user:*")
```

### 2. 캐시 패턴 설정

```kotlin
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig

// Read-Through Only
val readOnlyConfig = RedisCacheConfig.readOnly(
    ttl = Duration.ofMinutes(30),
)

// Read-Through + Write-Through
val readWriteConfig = RedisCacheConfig.readWrite(
    ttl = Duration.ofMinutes(30),
    writeMode = WriteMode.WRITE_THROUGH,
)

// Near Cache 활성화 (Local + Redis 2-Tier)
val nearCacheConfig = RedisCacheConfig.readOnly(
    ttl = Duration.ofMinutes(30),
    nearCacheEnabled = true,
)
```

## 아키텍처 개요

```mermaid
classDiagram
    direction TB
    class RedissonR2dbcRepository~E~ {
        <<abstract suspend>>
        -nearCache: RedissonNearCache
        +findByIdOrNull(id): E?
        +findAll(): Flow~E~
        +save(entity): E
    }
    class RedissonNearCache~V~ {
        +get(key): V?
        +put(key, value)
        +invalidate(key)
    }
    RedissonR2dbcRepository --> RedissonNearCache : RLocalCachedMap

    classDef repoStyle fill:#2196F3,color:#fff,stroke:#1565C0
    classDef cacheStyle fill:#F44336,color:#fff,stroke:#B71C1C
    class RedissonR2dbcRepository:::repoStyle
    class RedissonNearCache:::cacheStyle
```

## 클래스 다이어그램

### R2DBC Redisson Repository 계층 구조

```mermaid
classDiagram
    class R2dbcRedissonRepository~ID, E~ {
<<interface>>
+cacheName: String
+table: IdTable~ID~
+cache: RMap~ID, E~
+extractId(entity: E): ID
+toEntity(ResultRow): E [suspend]
+exists(id: ID): Boolean [suspend]
+get(id: ID): E? [suspend]
+getAll(ids, batchSize): List~E~ [suspend]
+findByIdFromDb(id: ID): E? [suspend]
+findAllFromDb(ids): List~E~ [suspend]
+findAll(...): List~E~ [suspend]
+put(entity: E): Boolean? [suspend]
+putAll(entities, batchSize) [suspend]
+invalidate(vararg ids): Long [suspend]
+invalidateAll(): Boolean [suspend]
+invalidateByPattern(patterns, count): Long [suspend]
}

class AbstractR2dbcRedissonRepository~ID, E~ {
<<abstract>>
+redissonClient: RedissonClient
+cacheName: String
#config: RedissonCacheConfig
#scope: CoroutineScope
#r2dbcEntityMapLoader: R2dbcEntityMapLoader~ID, E~
#r2dbcEntityMapWriter: R2dbcEntityMapWriter~ID, E~ ?
#createLocalCacheMap(): RLocalCachedMap
#createMapCache(): RMapCache
#doUpdateEntity(stmt, entity)
#doInsertEntity(stmt, entity)
+findAll(...): List~E~ [suspend]
+getAll(ids, batchSize): List~E~ [suspend]
}

class R2dbcEntityMapLoader~ID, E~ {
<<abstract>>
+load(key: ID): CompletableFuture~E~
+loadAllKeys(): AsyncIterator~ID~
}

class R2dbcEntityMapWriter~ID, E~ {
<<abstract>>
+write(map: Map~ID, E~): CompletableFuture~Void~
+delete(keys: Collection~Any~): CompletableFuture~Void~
}

class R2dbcExposedEntityMapLoader~ID, E~ {
-entityTable: IdTable~ID~
-scope: CoroutineScope
-batchSize: Int
-toEntity: suspend ResultRow.() -> E
 }

class R2dbcExposedEntityMapWriter~ID, E~ {
-entityTable: IdTable~ID~
-scope: CoroutineScope
-updateBody: (UpdateStatement, E) -> Unit
-batchInsertBody: BatchInsertStatement.(E) -> Unit
-deleteFromDBOnInvalidate: Boolean
-writeMode: WriteMode
}

R2dbcRedissonRepository~ID, E~ <|.. AbstractR2dbcRedissonRepository~ID, E~
AbstractR2dbcRedissonRepository~ID, E~ --> R2dbcEntityMapLoader~ID, E~: r2dbcEntityMapLoader
AbstractR2dbcRedissonRepository~ID, E~ --> R2dbcEntityMapWriter~ID, E~: r2dbcEntityMapWriter (nullable)
R2dbcEntityMapLoader~ID, E~ <|-- R2dbcExposedEntityMapLoader~ID, E~
R2dbcEntityMapWriter~ID, E~ <|-- R2dbcExposedEntityMapWriter~ID, E~
```

## 캐시 패턴

### Read-Through (R2DBC + suspend)

캐시 미스 시 `R2dbcExposedEntityMapLoader`가 R2DBC `suspendTransaction`으로 DB에서 자동 로드합니다.

```mermaid
sequenceDiagram
    participant Client as Client (Coroutine)
    participant Repo as R2dbcRedissonRepository
    participant RMap as Redisson RMap
    participant Loader as R2dbcExposedEntityMapLoader
    participant DB as Database (R2DBC)
    Client ->> Repo: suspend get(id)
    Repo ->> RMap: cache.getAsync(id).await()
    alt RMap HIT
        RMap -->> Repo: entity
        Repo -->> Client: entity
    else RMap MISS
        RMap ->> Loader: loadAsync(id) [Read-Through]
        Note over Loader, DB: suspendTransaction { selectAll().where { id eq ... } }
        Loader ->> DB: SELECT WHERE id=? (R2DBC 비동기)
        DB -->> Loader: ResultRow
        Loader -->> RMap: entity (캐시에 저장)
        RMap -->> Repo: entity
        Repo -->> Client: entity
    end
```

### Write-Through (R2DBC + suspend)

`put()` 호출 시 `R2dbcExposedEntityMapWriter`가 R2DBC `suspendTransaction`으로 DB에 즉시 반영합니다.

```mermaid
sequenceDiagram
    participant Client as Client (Coroutine)
    participant Repo as R2dbcRedissonRepository
    participant RMap as Redisson RMap
    participant Writer as R2dbcExposedEntityMapWriter
    participant DB as Database (R2DBC)
    Client ->> Repo: suspend put(entity)
    Repo ->> RMap: cache.fastPutAsync(id, entity).await()
    RMap ->> Writer: writeAsync(map) [Write-Through]
    Note over Writer, DB: CoroutineScope(Dispatchers.IO) 내 suspendTransaction
    Writer ->> DB: SELECT id (존재 여부 확인, R2DBC Flow)
    DB -->> Writer: existIds
    alt 기존 레코드
        Writer ->> DB: UPDATE SET ... WHERE id=? (R2DBC)
        DB -->> Writer: OK
    else 신규 레코드 (non-autoInc ID)
        Writer ->> DB: batchInsert(entities) (R2DBC)
        DB -->> Writer: OK
    end
    Writer -->> RMap: 완료
    RMap -->> Repo: true
    Repo -->> Client: true
```

### Write-Behind (R2DBC + suspend + 비동기 DB)

`put()` 호출 즉시 응답하고, 이후 `R2dbcExposedEntityMapWriter`가 비동기 배치로 DB에 반영합니다.

```mermaid
sequenceDiagram
    participant Client as Client (Coroutine)
    participant Repo as R2dbcRedissonRepository
    participant RMap as Redisson RMap
    participant Writer as R2dbcExposedEntityMapWriter
    participant DB as Database (R2DBC)
    Client ->> Repo: suspend put(entity)
    Repo ->> RMap: cache.fastPutAsync(id, entity).await()
    RMap -->> Repo: true (즉시 반환)
    Repo -->> Client: true
    Note over RMap, DB: Write-Behind: Redisson이 비동기 배치로 DB 반영
    RMap ->> Writer: writeAsync(map) [비동기]
    Note over Writer, DB: CoroutineScope(Dispatchers.IO) 내 suspendTransaction
    Writer ->> DB: batchInsert(entities).asFlow().collect { ... } (R2DBC)
    DB -->> Writer: OK
```

## R2dbcRedissonRepository 주요 메서드

| 메서드                                    | 설명                    |
|----------------------------------------|----------------------|
| `exists(id)`                           | 캐시에 해당 ID 존재 여부 확인 (suspend) |
| `get(id)`                              | 캐시에서 엔티티 조회, 미스 시 DB 로드 (suspend) |
| `getAll(ids, batchSize)`               | 캐시에서 여러 엔티티 배치 조회 (suspend) |
| `findByIdFromDb(id)`                   | DB에서 직접 조회, 캐시 우회 (suspend) |
| `findAllFromDb(ids)`                   | DB에서 여러 엔티티 직접 조회 (suspend) |
| `findAll(limit, offset, sortBy, where)`| DB 조회 후 캐시 동기화 (suspend) |
| `put(entity)`                          | 캐시에 저장 (suspend)      |
| `putAll(entities, batchSize)`          | 캐시에 일괄 저장 (suspend)   |
| `invalidate(vararg ids)`               | 캐시에서 제거 (suspend)     |
| `invalidateAll()`                      | 캐시 전체 비우기 (suspend)   |
| `invalidateByPattern(pattern, count)`  | 패턴에 맞는 키 캐시 제거 (suspend) |

## 캐시 설정 상수 (`RedisCacheConfig`)

자주 사용하는 캐시 모드 설정값이 상수로 제공됩니다.

| 상수                                             | 설명                          |
|------------------------------------------------|------------------------------|
| `RedisCacheConfig.READ_ONLY`                   | Read-Through 전용 (원격 캐시)     |
| `RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE`   | Read-Through + Near Cache    |
| `RedisCacheConfig.READ_WRITE_THROUGH`          | Read-Through + Write-Through  |
| `RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE` | Read-Write-Through + Near Cache |
| `RedisCacheConfig.WRITE_BEHIND`                | Write-Behind (원격 캐시)         |
| `RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE`| Write-Behind + Near Cache    |

## 주요 파일/클래스 목록

### Repository (repository/)

| 파일                                      | 설명                                |
|-----------------------------------------|-----------------------------------|
| `R2dbcRedissonRepository.kt`            | R2DBC 비동기 캐시 Repository 인터페이스     |
| `AbstractR2dbcRedissonRepository.kt`    | R2DBC 비동기 캐시 Repository 추상 클래스    |
| `R2dbcCacheRepository.kt`              | (Deprecated) 구 R2DBC 캐시 Repository |
| `AbstractR2dbcCacheRepository.kt`      | (Deprecated) 구 R2DBC 캐시 추상 클래스    |

### Map (map/)

| 파일                                    | 설명                          |
|---------------------------------------|------------------------------|
| `R2dbcEntityMapLoader.kt`             | R2DBC 비동기 MapLoader 기본 구현 (`MapLoaderAsync`) |
| `R2dbcEntityMapWriter.kt`             | R2DBC 비동기 MapWriter 기본 구현 (`MapWriterAsync`) |
| `R2dbcExposedEntityMapLoader.kt`      | Exposed IdTable 기반 MapLoader 구현체 |
| `R2dbcExposedEntityMapWriter.kt`      | Exposed IdTable 기반 MapWriter 구현체 (Write-Through/Write-Behind) |
| `AsyncIteratorSupport.kt`             | Redisson `AsyncIterator`를 `List`로 수집하는 확장 함수 |

## 테스트

```bash
./gradlew :bluetape4k-exposed-r2dbc-redisson:test
```

## 참고

- [JetBrains Exposed R2DBC](https://github.com/JetBrains/Exposed)
- [Redisson](https://github.com/redisson/redisson)
- [Redisson AsyncMapLoader](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/map/MapLoaderAsync.html)
- [bluetape4k-exposed-r2dbc](../exposed-r2dbc)
- [bluetape4k-exposed-jdbc-redisson](../exposed-jdbc-redisson)
