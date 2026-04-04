# Module bluetape4k-exposed-r2dbc-redisson

English | [한국어](./README.ko.md)

Combines Exposed R2DBC with Redisson caching to implement asynchronous Read-Through/Write-Through cache patterns.

## Overview

`bluetape4k-exposed-r2dbc-redisson` integrates Exposed R2DBC (asynchronous) with the [Redisson](https://github.com/redisson/redisson) Redis client, making it easy to cache database query results in Redis within an async environment. All interfaces are based on `suspend` functions and are fully compatible with Kotlin Coroutines.

### Key Features

- **Async MapLoader/MapWriter support**: Integration with Redisson `AsyncMapLoader`/`AsyncMapWriter`
  - `loadAllKeys()` iterates reliably in ascending primary key order
- **Repository abstraction**: Common cache + DB access pattern (`R2dbcRedissonRepository`)
- **Coroutines-native**: All operations are `suspend` functions
- **Near Cache support**: Two-tier Local Cache + Redis caching
- **Read-Through/Write-Through/Write-Behind**: Multiple cache patterns supported

## Adding Dependencies

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc-redisson:${version}")
    implementation("org.redisson:redisson:3.37.0")

    // R2DBC driver
    implementation("org.postgresql:r2dbc-postgresql:1.0.5.RELEASE")
}
```

## Basic Usage

### 1. Implementing R2dbcRedissonRepository

Extend `AbstractR2dbcRedissonRepository` to implement an async cache Repository.

```kotlin
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.redisson.repository.AbstractR2dbcRedissonRepository
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient

// Entity (must implement java.io.Serializable)
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

    // Required for Write-Through mode
    override fun doUpdateEntity(statement: UpdateStatement, entity: UserRecord) {
        statement[UserTable.name]  = entity.name
        statement[UserTable.email] = entity.email
    }
}

// Usage (all methods are suspend)
val repo = UserR2dbcRedissonRepository(redissonClient, RedisCacheConfig.readOnly())

// Retrieve from cache (auto-loads from DB on miss)
val user = repo.get(1L)

// Bypass cache and query DB directly
val freshUser = repo.findByIdFromDb(1L)

// Load from DB and populate cache
val all = repo.findAll(limit = 100)

// Store in cache
repo.put(user!!)
repo.putAll(users)

// Invalidate cache
repo.invalidate(1L)
repo.invalidateAll()
repo.invalidateByPattern("user:*")
```

### 2. Cache pattern configuration

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

// Enable Near Cache (Local + Redis two-tier)
val nearCacheConfig = RedisCacheConfig.readOnly(
    ttl = Duration.ofMinutes(30),
    nearCacheEnabled = true,
)
```

## Architecture Overview

```mermaid
classDiagram
    direction TB
    class RedissonR2dbcRepository~E~ {
        <<abstractSuspend>>
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

```

## Class Diagrams

### R2DBC Redisson Repository Hierarchy

```mermaid
classDiagram
    class R2dbcRedissonRepository~ID_E~ {
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

class AbstractR2dbcRedissonRepository~ID_E~ {
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

class R2dbcEntityMapLoader~ID_E~ {
<<abstract>>
+load(key: ID): CompletableFuture~E~
+loadAllKeys(): AsyncIterator~ID~
}

class R2dbcEntityMapWriter~ID_E~ {
<<abstract>>
+write(map: Map~ID, E~): CompletableFuture~Void~
+delete(keys: Collection~Any~): CompletableFuture~Void~
}

class R2dbcExposedEntityMapLoader~ID_E~ {
-entityTable: IdTable~ID~
-scope: CoroutineScope
-batchSize: Int
-toEntity: suspend ResultRow.() -> E
 }

class R2dbcExposedEntityMapWriter~ID_E~ {
-entityTable: IdTable~ID~
-scope: CoroutineScope
-updateBody: (UpdateStatement, E) -> Unit
-batchInsertBody: BatchInsertStatement.(E) -> Unit
-deleteFromDBOnInvalidate: Boolean
-writeMode: WriteMode
}

R2dbcRedissonRepository~ID_E~ <|.. AbstractR2dbcRedissonRepository~ID_E~
AbstractR2dbcRedissonRepository~ID_E~ --> R2dbcEntityMapLoader~ID_E~: r2dbcEntityMapLoader
AbstractR2dbcRedissonRepository~ID_E~ --> R2dbcEntityMapWriter~ID_E~: r2dbcEntityMapWriter (nullable)
R2dbcEntityMapLoader~ID_E~ <|-- R2dbcExposedEntityMapLoader~ID_E~
R2dbcEntityMapWriter~ID_E~ <|-- R2dbcExposedEntityMapWriter~ID_E~
```

## Cache Patterns

### Read-Through (R2DBC + suspend)

On a cache miss, `R2dbcExposedEntityMapLoader` automatically loads from the DB via R2DBC `suspendTransaction`.

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
        Loader ->> DB: SELECT WHERE id=? (R2DBC async)
        DB -->> Loader: ResultRow
        Loader -->> RMap: entity (stored in cache)
        RMap -->> Repo: entity
        Repo -->> Client: entity
    end
```

### Write-Through (R2DBC + suspend)

On `put()`, `R2dbcExposedEntityMapWriter` immediately persists to DB via R2DBC `suspendTransaction`.

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
    Note over Writer, DB: suspendTransaction inside CoroutineScope(Dispatchers.IO)
    Writer ->> DB: SELECT id (check existence via R2DBC Flow)
    DB -->> Writer: existIds
    alt Existing record
        Writer ->> DB: UPDATE SET ... WHERE id=? (R2DBC)
        DB -->> Writer: OK
    else New record (non-autoInc ID)
        Writer ->> DB: batchInsert(entities) (R2DBC)
        DB -->> Writer: OK
    end
    Writer -->> RMap: done
    RMap -->> Repo: true
    Repo -->> Client: true
```

### Write-Behind (R2DBC + suspend + async DB)

On `put()`, immediately returns and then `R2dbcExposedEntityMapWriter` asynchronously batch-persists to the DB.

```mermaid
sequenceDiagram
    participant Client as Client (Coroutine)
    participant Repo as R2dbcRedissonRepository
    participant RMap as Redisson RMap
    participant Writer as R2dbcExposedEntityMapWriter
    participant DB as Database (R2DBC)
    Client ->> Repo: suspend put(entity)
    Repo ->> RMap: cache.fastPutAsync(id, entity).await()
    RMap -->> Repo: true (returns immediately)
    Repo -->> Client: true
    Note over RMap, DB: Write-Behind: Redisson asynchronously batch-persists to DB
    RMap ->> Writer: writeAsync(map) [async]
    Note over Writer, DB: suspendTransaction inside CoroutineScope(Dispatchers.IO)
    Writer ->> DB: batchInsert(entities).asFlow().collect { ... } (R2DBC)
    DB -->> Writer: OK
```

## R2dbcRedissonRepository Key Methods

| Method                                    | Description                                             |
|-------------------------------------------|---------------------------------------------------------|
| `exists(id)`                              | Check ID existence in cache (suspend)                   |
| `get(id)`                                 | Retrieve entity from cache, load from DB on miss (suspend) |
| `getAll(ids, batchSize)`                  | Batch retrieve from cache (suspend)                     |
| `findByIdFromDb(id)`                      | Bypass cache, query DB directly (suspend)               |
| `findAllFromDb(ids)`                      | Bypass cache, batch query DB (suspend)                  |
| `findAll(limit, offset, sortBy, where)`   | Load from DB and sync cache (suspend)                   |
| `put(entity)`                             | Store in cache (suspend)                                |
| `putAll(entities, batchSize)`             | Batch store in cache (suspend)                          |
| `invalidate(vararg ids)`                  | Remove from cache (suspend)                             |
| `invalidateAll()`                         | Clear all cache entries (suspend)                       |
| `invalidateByPattern(pattern, count)`     | Remove cache entries matching a pattern (suspend)       |

## Cache Configuration Constants (`RedisCacheConfig`)

Commonly used cache mode constants are provided as named constants.

| Constant                                             | Description                              |
|------------------------------------------------------|------------------------------------------|
| `RedisCacheConfig.READ_ONLY`                         | Read-Through only (remote cache)         |
| `RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE`         | Read-Through + Near Cache                |
| `RedisCacheConfig.READ_WRITE_THROUGH`                | Read-Through + Write-Through             |
| `RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE`| Read-Write-Through + Near Cache          |
| `RedisCacheConfig.WRITE_BEHIND`                      | Write-Behind (remote cache)              |
| `RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE`      | Write-Behind + Near Cache                |

## Key Files and Classes

### Repository (repository/)

| File                                      | Description                                         |
|-------------------------------------------|-----------------------------------------------------|
| `R2dbcRedissonRepository.kt`              | R2DBC async cache Repository interface              |
| `AbstractR2dbcRedissonRepository.kt`      | R2DBC async cache Repository abstract class         |
| `R2dbcCacheRepository.kt`                 | (Deprecated) Legacy R2DBC cache Repository          |
| `AbstractR2dbcCacheRepository.kt`         | (Deprecated) Legacy R2DBC cache abstract class      |

### Map (map/)

| File                                    | Description                                                             |
|-----------------------------------------|-------------------------------------------------------------------------|
| `R2dbcEntityMapLoader.kt`               | R2DBC async MapLoader base implementation (`MapLoaderAsync`)            |
| `R2dbcEntityMapWriter.kt`               | R2DBC async MapWriter base implementation (`MapWriterAsync`)            |
| `R2dbcExposedEntityMapLoader.kt`        | Exposed IdTable-based MapLoader implementation                          |
| `R2dbcExposedEntityMapWriter.kt`        | Exposed IdTable-based MapWriter implementation (Write-Through/Behind)   |
| `AsyncIteratorSupport.kt`               | Extension to collect a Redisson `AsyncIterator` into a `List`           |

## Testing

```bash
./gradlew :bluetape4k-exposed-r2dbc-redisson:test
```

## References

- [JetBrains Exposed R2DBC](https://github.com/JetBrains/Exposed)
- [Redisson](https://github.com/redisson/redisson)
- [Redisson AsyncMapLoader](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/map/MapLoaderAsync.html)
- [bluetape4k-exposed-r2dbc](../exposed-r2dbc)
- [bluetape4k-exposed-jdbc-redisson](../exposed-jdbc-redisson)
