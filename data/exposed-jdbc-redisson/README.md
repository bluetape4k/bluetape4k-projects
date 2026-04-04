# Module bluetape4k-exposed-jdbc-redisson

English | [한국어](./README.ko.md)

Combines Exposed JDBC with Redisson caching to implement Read-Through/Write-Through cache patterns.

## Overview

`bluetape4k-exposed-jdbc-redisson` integrates JetBrains Exposed ORM with the [Redisson](https://github.com/redisson/redisson) Redis client, making it easy to cache database query results in Redis.

### Key Features

- **MapLoader/MapWriter support**: Integration with Redisson Read-Through/Write-Through caching
  - `loadAllKeys()` iterates reliably in ascending primary key order
- **Repository abstraction**: Common cache + DB access patterns (`JdbcRedissonRepository`, `SuspendedJdbcRedissonRepository`)
- **Sync and Coroutines implementations**: Choose the right approach for your environment
- **Near Cache support**: Two-tier Local Cache + Redis caching
- **Write-Behind support**: Asynchronous DB persistence pattern

## Adding Dependencies

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc-redisson:${version}")
    implementation("org.redisson:redisson:3.37.0")
}
```

## Basic Usage

### 1. Implementing JdbcRedissonRepository (synchronous)

Extend `AbstractJdbcRedissonRepository` to implement a synchronous cache Repository.

```kotlin
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.redisson.repository.AbstractJdbcRedissonRepository
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.update
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

class UserRedissonRepository(
    redissonClient: RedissonClient,
    config: RedisCacheConfig,
): AbstractJdbcRedissonRepository<Long, UserTable, UserRecord>(
    redissonClient = redissonClient,
    cacheName = "users",
    config = config,
) {
    override val entityTable = UserTable

    override fun ResultRow.toEntity() = UserRecord(
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

// Usage (Read-Through)
val repo = UserRedissonRepository(redissonClient, RedisCacheConfig.READ_ONLY)

// Retrieve from cache (auto-loads from DB on miss)
val user = repo[1L]

// Check existence by ID (DB Read-Through on cache miss)
val exists = repo.exists(1L)

// Bypass cache and query DB directly
val freshUser = repo.findByIdFromDb(1L)

// Batch retrieval of multiple entities
val users = repo.getAll(listOf(1L, 2L, 3L))

// Load from DB and store in cache
val allUsers = repo.findAll(limit = 100)

// Invalidate cache
repo.invalidate(1L)
repo.invalidateAll()
repo.invalidateByPattern("*John*")  // Invalidate by pattern
```

### 2. Implementing SuspendedJdbcRedissonRepository (Coroutines)

Extend `AbstractSuspendedJdbcRedissonRepository` to implement a coroutine-based cache Repository.

```kotlin
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedJdbcRedissonRepository
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.redisson.api.RedissonClient

class SuspendedUserRedissonRepository(
    redissonClient: RedissonClient,
    config: RedisCacheConfig,
): AbstractSuspendedJdbcRedissonRepository<Long, UserTable, UserRecord>(
    redissonClient = redissonClient,
    cacheName = "users",
    config = config,
) {
    override val entityTable = UserTable

    override fun ResultRow.toEntity() = UserRecord(
        id    = this[UserTable.id].value,
        name  = this[UserTable.name],
        email = this[UserTable.email],
    )
}

// Usage (suspend functions)
val repo = SuspendedUserRedissonRepository(redissonClient, RedisCacheConfig.READ_ONLY)

val user = repo.get(1L)                          // Cache lookup (DB Read-Through on miss)
val exists = repo.exists(1L)                     // Check existence
val fresh = repo.findByIdFromDb(1L)              // Bypass cache, query DB directly
val all = repo.findAll(limit = 100)              // Load from DB, populate cache
val batch = repo.getAll(listOf(1L, 2L, 3L))     // Batch retrieval
repo.put(user!!)                                 // Store in cache
repo.putAll(batch)                               // Batch store in cache
repo.invalidate(1L)                              // Invalidate single entry
repo.invalidateAll()                             // Invalidate all (returns Boolean)
repo.invalidateByPattern("user:*")               // Invalidate by pattern
```

### 3. Cache pattern configuration

```kotlin
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.redisson.api.map.WriteMode

// Read-Through Only (default) — auto-loads from DB on cache miss
val readOnlyConfig = RedisCacheConfig.READ_ONLY

// Read-Through + Near Cache — two-tier Local Cache + Redis
val readOnlyNearCacheConfig = RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE

// Read-Through + Write-Through — synchronously persists to DB on cache write
val writeThroughConfig = RedisCacheConfig.READ_WRITE_THROUGH

// Read-Through + Write-Through + Near Cache
val writeThroughNearCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE

// Read-Through + Write-Behind — asynchronously persists to DB after cache write
val writeBehindConfig = RedisCacheConfig.WRITE_BEHIND

// Read-Through + Write-Behind + Near Cache
val writeBehindNearCacheConfig = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

// Also delete from DB on invalidate (deleteFromDBOnInvalidate=true)
// ⚠️ Use with caution in production.
val deleteFromDbConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(
    deleteFromDBOnInvalidate = true,
)
```

### 4. Write-Through / Write-Behind Repository implementation

In Write-Through/Write-Behind mode, also implement `doUpdateEntity` and `doInsertEntity`.

```kotlin
class UserWriteThroughRepository(
    redissonClient: RedissonClient,
): AbstractJdbcRedissonRepository<Long, UserTable, UserRecord>(
    redissonClient = redissonClient,
    cacheName = "users:write-through",
    config = RedisCacheConfig.READ_WRITE_THROUGH,
) {
    override val entityTable = UserTable

    override fun ResultRow.toEntity() = UserRecord(
        id    = this[UserTable.id].value,
        name  = this[UserTable.name],
        email = this[UserTable.email],
    )

    // Called on UPDATE of an existing record
    override fun doUpdateEntity(statement: UpdateStatement, entity: UserRecord) {
        statement[UserTable.name]  = entity.name
        statement[UserTable.email] = entity.email
    }

    // Called on INSERT of a new record (for client-side IDs)
    override fun doInsertEntity(statement: BatchInsertStatement, entity: UserRecord) {
        statement[UserTable.id]    = EntityID(entity.id, UserTable)
        statement[UserTable.name]  = entity.name
        statement[UserTable.email] = entity.email
    }
}

// Write-Through usage
val repo = UserWriteThroughRepository(redissonClient)
transaction {
    val user = UserRecord(id = 0, name = "Hong Gildong", email = "hong@example.com")
    repo.put(user)                   // Write to cache + synchronously persist to DB
    repo.putAll(listOf(user))        // Batch write to cache + DB
    repo.invalidate(user.id)         // Remove from cache (also deletes from DB if deleteFromDBOnInvalidate=true)
}
```

## Architecture Overview

```mermaid
classDiagram
    direction TB
    class RedissonJdbcRepository~E~ {
        <<abstract>>
        -nearCache: RedissonNearCache
        +findByIdOrNull(id): E?
        +save(entity): E
    }
    class RedissonNearCache~V~ {
        +get(key): V?
        +put(key, value)
        +invalidate(key)
    }
    RedissonJdbcRepository --> RedissonNearCache : RLocalCachedMap

```

## Class Diagrams

### Synchronous Repository Hierarchy

```mermaid
classDiagram
    class JdbcRedissonRepository~ID_E~ {
        <<interface>>
        +cacheName: String
        +table: IdTable~ID~
        +cache: RMap~ID, E~
        +extractId(entity: E): ID
        +toEntity(ResultRow): E
        +exists(id: ID): Boolean
        +get(id: ID): E?
        +getAll(ids, batchSize): List~E~
        +findByIdFromDb(id: ID): E?
        +findAllFromDb(ids): List~E~
        +findAll(limit, offset, sortBy, where): List~E~
        +put(entity: E)
        +putAll(entities, batchSize)
        +invalidate(vararg ids): Long
        +invalidateAll()
        +invalidateByPattern(patterns, count): Long
    }

    class AbstractJdbcRedissonRepository~ID_E~ {
        <<abstract>>
        +redissonClient: RedissonClient
        +cacheName: String
        #config: RedissonCacheConfig
        #mapLoader: EntityMapLoader~ID, E~
        #mapWriter: EntityMapWriter~ID, E~?
        #createLocalCacheMap(): RLocalCachedMap
        #createMapCache(): RMapCache
        #doUpdateEntity(stmt, entity)
        #doInsertEntity(stmt, entity)
        +findAll(...): List~E~
        +getAll(ids, batchSize): List~E~
    }

    class EntityMapLoader~ID_E~ {
        <<abstract>>
        +load(key: ID): E?
        +loadAllKeys(): Iterable~ID~?
    }

    class EntityMapWriter~ID_E~ {
        <<abstract>>
        +write(map: Map~ID, E~)
        +delete(keys: Collection~Any~)
    }

    class ExposedEntityMapLoader~ID_E~ {
        -entityTable: IdTable~ID~
        -batchSize: Int
        -toEntity: ResultRow.() -> E
    }

    class ExposedEntityMapWriter~ID_E~ {
        -entityTable: IdTable~ID~
        -updateBody: (UpdateStatement, E) -> Unit
        -batchInsertBody: BatchInsertStatement.(E) -> Unit
        -deleteFromDBOnInvalidate: Boolean
        -writeMode: WriteMode
    }

    JdbcRedissonRepository~ID_E~ <|.. AbstractJdbcRedissonRepository~ID_E~
    AbstractJdbcRedissonRepository~ID_E~ --> EntityMapLoader~ID_E~ : mapLoader
    AbstractJdbcRedissonRepository~ID_E~ --> EntityMapWriter~ID_E~ : mapWriter (nullable)
    EntityMapLoader~ID_E~ <|-- ExposedEntityMapLoader~ID_E~
    EntityMapWriter~ID_E~ <|-- ExposedEntityMapWriter~ID_E~
```

### Coroutines (Suspend) Repository Hierarchy

```mermaid
classDiagram
    class SuspendedJdbcRedissonRepository~ID_E~ {
        <<interface>>
        +cacheName: String
        +table: IdTable~ID~
        +cache: RMap~ID, E~
        +extractId(entity: E): ID
        +toEntity(ResultRow): E
        +exists(id: ID): Boolean [suspend]
        +get(id: ID): E? [suspend]
        +getAll(ids, batchSize): List~E~ [suspend]
        +findByIdFromDb(id: ID): E? [suspend]
        +findAllFromDb(ids): List~E~ [suspend]
        +findAll(...): List~E~ [suspend]
        +put(entity: E): Boolean [suspend]
        +putAll(entities, batchSize) [suspend]
        +invalidate(vararg ids): Long [suspend]
        +invalidateAll(): Boolean [suspend]
        +invalidateByPattern(patterns, count): Long [suspend]
    }

    class AbstractSuspendedJdbcRedissonRepository~ID_E~ {
        <<abstract>>
        +redissonClient: RedissonClient
        +cacheName: String
        #config: RedissonCacheConfig
        #scope: CoroutineScope
        #suspendedMapLoader: SuspendedEntityMapLoader~ID, E~
        #suspendedMapWriter: SuspendedEntityMapWriter~ID, E~?
        #createLocalCacheMap(): RLocalCachedMap
        #createMapCache(): RMapCache
        #doUpdateEntity(stmt, entity)
        #doInsertEntity(stmt, entity)
        +findAll(...): List~E~ [suspend]
        +getAll(ids, batchSize): List~E~ [suspend]
    }

    class SuspendedEntityMapLoader~ID_E~ {
        <<abstract>>
        +load(key: ID): CompletableFuture~E~
        +loadAllKeys(): AsyncIterator~ID~
    }

    class SuspendedEntityMapWriter~ID_E~ {
        <<abstract>>
        +write(map: Map~ID, E~): CompletableFuture~Void~
        +delete(keys: Collection~Any~): CompletableFuture~Void~
    }

    class SuspendedExposedEntityMapLoader~ID_E~ {
        -entityTable: IdTable~ID~
        -scope: CoroutineScope
        -batchSize: Int
        -toEntity: ResultRow.() -> E
    }

    class SuspendedExposedEntityMapWriter~ID_E~ {
        -entityTable: IdTable~ID~
        -scope: CoroutineScope
        -updateBody: (UpdateStatement, E) -> Unit
        -batchInsertBody: BatchInsertStatement.(E) -> Unit
        -deleteFromDBOnInvalidate: Boolean
        -writeMode: WriteMode
    }

    SuspendedJdbcRedissonRepository~ID_E~ <|.. AbstractSuspendedJdbcRedissonRepository~ID_E~
    AbstractSuspendedJdbcRedissonRepository~ID_E~ --> SuspendedEntityMapLoader~ID_E~ : suspendedMapLoader
    AbstractSuspendedJdbcRedissonRepository~ID_E~ --> SuspendedEntityMapWriter~ID_E~ : suspendedMapWriter (nullable)
    SuspendedEntityMapLoader~ID_E~ <|-- SuspendedExposedEntityMapLoader~ID_E~
    SuspendedEntityMapWriter~ID_E~ <|-- SuspendedExposedEntityMapWriter~ID_E~
```

## Cache Patterns

### Read-Through (synchronous)

On a cache miss, `ExposedEntityMapLoader` automatically loads from the DB.

```mermaid
sequenceDiagram
    participant Client as Client
    participant Repo as JdbcRedissonRepository
    participant RMap as Redisson RMap
    participant Loader as ExposedEntityMapLoader
    participant DB as Database (JDBC)

    Client->>Repo: get(id) / exists(id)
    Repo->>RMap: RMap.get(id)
    alt RMap HIT
        RMap-->>Repo: entity
        Repo-->>Client: entity
    else RMap MISS
        RMap->>Loader: load(id) [Read-Through]
        Loader->>DB: SELECT WHERE id=?
        DB-->>Loader: ResultRow
        Loader-->>RMap: entity (stored in cache)
        RMap-->>Repo: entity
        Repo-->>Client: entity
    end
```

### Write-Through (synchronous)

On `put()`, `ExposedEntityMapWriter` immediately and synchronously persists to the DB.

```mermaid
sequenceDiagram
    participant Client as Client
    participant Repo as JdbcRedissonRepository
    participant RMap as Redisson RMap
    participant Writer as ExposedEntityMapWriter
    participant DB as Database (JDBC)
    Client ->> Repo: put(entity)
    Repo ->> RMap: RMap.fastPut(id, entity)
    RMap ->> Writer: write(map) [Write-Through]
    Writer ->> DB: SELECT id (check existence)
    DB -->> Writer: existIds
    alt Existing record
        Writer ->> DB: UPDATE SET ... WHERE id=?
        DB -->> Writer: OK
    else New record (non-autoInc ID)
        Writer ->> DB: batchInsert(entities)
        DB -->> Writer: OK
    end
    Writer -->> RMap: done
    RMap -->> Repo: OK
    Repo -->> Client: done
```

### Write-Behind (synchronous)

On `put()`, immediately returns and then `ExposedEntityMapWriter` asynchronously batch-persists to the DB.

```mermaid
sequenceDiagram
    participant Client as Client
    participant Repo as JdbcRedissonRepository
    participant RMap as Redisson RMap
    participant Writer as ExposedEntityMapWriter
    participant DB as Database (JDBC)

    Client->>Repo: put(entity)
    Repo->>RMap: RMap.fastPut(id, entity)
    RMap-->>Repo: OK (returns immediately)
    Repo-->>Client: done

    Note over RMap,DB: Write-Behind: Redisson asynchronously batch-persists to DB
    RMap->>Writer: write(map) [async]
    Writer->>DB: batchInsert(entities)
    DB-->>Writer: OK
```

### Read-Through (Suspend Coroutines)

`SuspendedJdbcRedissonRepository` exposes all operations as `suspend` functions.

```mermaid
sequenceDiagram
    participant Client as Client (Coroutine)
    participant Repo as SuspendedJdbcRedissonRepository
    participant RMap as Redisson RMap
    participant Loader as SuspendedExposedEntityMapLoader
    participant DB as Database (JDBC/IO)

    Client->>Repo: suspend get(id)
    Repo->>RMap: cache.getAsync(id).await()
    alt RMap HIT
        RMap-->>Repo: entity
        Repo-->>Client: entity
    else RMap MISS
        RMap->>Loader: loadAsync(id) [Read-Through]
        Note over Loader,DB: suspendedTransactionAsync(Dispatchers.IO)
        Loader->>DB: SELECT WHERE id=?
        DB-->>Loader: ResultRow
        Loader-->>RMap: entity (stored in cache)
        RMap-->>Repo: entity
        Repo-->>Client: entity
    end
```

### Write-Through (Suspend Coroutines)

```mermaid
sequenceDiagram
    participant Client as Client (Coroutine)
    participant Repo as SuspendedJdbcRedissonRepository
    participant RMap as Redisson RMap
    participant Writer as SuspendedExposedEntityMapWriter
    participant DB as Database (JDBC/IO)

    Client->>Repo: suspend put(entity)
    Repo->>RMap: cache.fastPutAsync(id, entity).await()
    RMap->>Writer: writeAsync(map) [Write-Through]
    Note over Writer,DB: Runs inside CoroutineScope(Dispatchers.IO)
    Writer->>DB: SELECT id (check existence)
    DB-->>Writer: existIds
    alt Existing record
        Writer->>DB: UPDATE SET ... WHERE id=?
        DB-->>Writer: OK
    else New record (non-autoInc ID)
        Writer->>DB: batchInsert(entities)
        DB-->>Writer: OK
    end
    Writer-->>RMap: done
    RMap-->>Repo: true
    Repo-->>Client: true
```

### Write-Behind (Suspend Coroutines)

```mermaid
sequenceDiagram
    participant Client as Client (Coroutine)
    participant Repo as SuspendedJdbcRedissonRepository
    participant RMap as Redisson RMap
    participant Writer as SuspendedExposedEntityMapWriter
    participant DB as Database (JDBC/IO)

    Client->>Repo: suspend put(entity)
    Repo->>RMap: cache.fastPutAsync(id, entity).await()
    RMap-->>Repo: true (returns immediately)
    Repo-->>Client: true

    Note over RMap,DB: Write-Behind: Redisson asynchronously batch-persists to DB
    RMap->>Writer: writeAsync(map) [async]
    Note over Writer,DB: Runs inside CoroutineScope(Dispatchers.IO)
    Writer->>DB: batchInsert(entities)
    DB-->>Writer: OK
```

## JdbcRedissonRepository / SuspendedJdbcRedissonRepository Key Methods

`JdbcRedissonRepository` uses synchronous calls; `SuspendedJdbcRedissonRepository` exposes the same API as `suspend` functions.

| Method                                    | Description                                                                  |
|-------------------------------------------|------------------------------------------------------------------------------|
| `exists(id)`                              | Check whether the ID exists in cache (DB Read-Through on miss)               |
| `get(id)` / `cache[id]`                  | Retrieve entity from cache (Read-Through)                                    |
| `getAll(ids, batchSize)`                  | Batch retrieve multiple entities from cache                                  |
| `findByIdFromDb(id)`                      | Bypass cache and query DB directly                                           |
| `findAllFromDb(ids)`                      | Bypass cache and batch query DB directly                                     |
| `findAll(limit, offset, sortBy, where)`   | Load from DB and store results in cache                                      |
| `put(entity)`                             | Store in cache (also persists to DB in Write-Through/Behind mode)            |
| `putAll(entities, batchSize)`             | Batch store in cache                                                         |
| `invalidate(ids)`                         | Remove from cache (also deletes from DB if `deleteFromDBOnInvalidate=true`)  |
| `invalidateAll()`                         | Clear all cache entries                                                      |
| `invalidateByPattern(pattern, count)`     | Remove cache entries matching a pattern                                      |

> **Note**: `SuspendedJdbcRedissonRepository.invalidateAll()` returns `Boolean`.

## Key Files and Classes

### Repository (repository/)

| File                                                   | Description                                        |
|--------------------------------------------------------|----------------------------------------------------|
| `JdbcRedissonRepository.kt`                            | Synchronous cache Repository interface             |
| `AbstractJdbcRedissonRepository.kt`                    | Synchronous cache Repository abstract class        |
| `SuspendedJdbcRedissonRepository.kt`                   | Coroutines cache Repository interface              |
| `AbstractSuspendedJdbcRedissonRepository.kt`           | Coroutines cache Repository abstract class         |
| `ExposedCacheRepository.kt`                            | (Deprecated) Legacy synchronous Repository         |
| `AbstractExposedCacheRepository.kt`                    | (Deprecated) Legacy synchronous abstract class     |
| `SuspendedExposedCacheRepository.kt`                   | (Deprecated) Legacy Coroutines Repository          |
| `AbstractSuspendedExposedCacheRepository.kt`           | (Deprecated) Legacy Coroutines abstract class      |

### Map (map/)

| File                                     | Description                              |
|------------------------------------------|------------------------------------------|
| `EntityMapLoader.kt`                     | Synchronous MapLoader interface          |
| `EntityMapWriter.kt`                     | Synchronous MapWriter interface          |
| `ExposedEntityMapLoader.kt`              | Exposed JDBC-based MapLoader             |
| `ExposedEntityMapWriter.kt`              | Exposed JDBC-based MapWriter             |
| `SuspendedEntityMapLoader.kt`            | Coroutines MapLoader interface           |
| `SuspendedEntityMapWriter.kt`            | Coroutines MapWriter interface           |
| `SuspendedExposedEntityMapLoader.kt`     | Coroutines MapLoader implementation      |
| `SuspendedExposedEntityMapWriter.kt`     | Coroutines MapWriter implementation      |

## Testing

```bash
./gradlew :bluetape4k-exposed-jdbc-redisson:test
```

## References

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Redisson](https://github.com/redisson/redisson)
- [Redisson RMap](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RMap.html)
- [bluetape4k-exposed-jdbc](../exposed-jdbc)
