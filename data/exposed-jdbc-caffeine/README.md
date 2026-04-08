# bluetape4k-exposed-jdbc-caffeine

English | [한국어](./README.ko.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.bluetape4k/bluetape4k-exposed-jdbc-caffeine)](https://central.sonatype.com/artifact/io.github.bluetape4k/bluetape4k-exposed-jdbc-caffeine)

Exposed JDBC repository with Caffeine local (in-process) cache. No Redis dependency — only `exposed-cache` interfaces are required.

> **See also**: [exposed-cache — Full Module Ecosystem & Interface Hierarchy](../exposed-cache/README.md)

## Architecture

```mermaid
classDiagram
    direction TB

    class JdbcCacheRepository~ID, E~ {
        <<interface>>
        +get(id: ID): E?
        +getAll(ids: Collection~ID~): Map~ID, E~
        +put(id: ID, entity: E)
        +putAll(entities: Map~ID, E~)
        +invalidate(id: ID)
        +invalidateAll(ids: Collection~ID~)
        +clear()
        +containsKey(id: ID): Boolean
        +findByIdFromDb(id: ID): E?
        +findAllFromDb(ids: Collection~ID~): List~E~
        +findAll(...): List~E~
    }

    class JdbcCaffeineRepository~ID, E~ {
        <<interface>>
        +config: LocalCacheConfig
        +cache: Cache~String, E~
    }

    class AbstractJdbcCaffeineRepository~ID, E~ {
        <<abstract>>
        #table: IdTable~ID~
        #toEntity(): E
        #updateEntity(entity: E)
        #insertEntity(entity: E)
        #serializeKey(id: ID): String
        -writeBehindQueue: Channel
        -writeBehindJob: Job
    }

    class SuspendedJdbcCacheRepository~ID, E~ {
        <<interface>>
        +get(id: ID): E?
        +put(id: ID, entity: E)
        +invalidate(id: ID)
        +clear()
        +findByIdFromDb(id: ID): E?
        +findAll(...): List~E~
    }

    class SuspendedJdbcCaffeineRepository~ID, E~ {
        <<interface>>
        +config: LocalCacheConfig
        +cache: Cache~String, E~
    }

    class AbstractSuspendedJdbcCaffeineRepository~ID, E~ {
        <<abstract>>
        #table: IdTable~ID~
        #toEntity(): E
        #updateEntity(entity: E)
        #insertEntity(entity: E)
        #serializeKey(id: ID): String
        -writeBehindQueue: Channel
        -writeBehindJob: Job
    }

    JdbcCacheRepository <|-- JdbcCaffeineRepository
    JdbcCaffeineRepository <|.. AbstractJdbcCaffeineRepository
    SuspendedJdbcCacheRepository <|-- SuspendedJdbcCaffeineRepository
    SuspendedJdbcCaffeineRepository <|.. AbstractSuspendedJdbcCaffeineRepository
```

## Write Strategy Flows

```mermaid
sequenceDiagram
    participant Client
    participant Repo as Repository
    participant Caffeine as Caffeine Cache
    participant DB as JDBC Database

    Note over Client,DB: Read-Through (cache miss)
    Client->>Repo: get(id)
    Repo->>Caffeine: getIfPresent(key)
    Caffeine-->>Repo: null
    Repo->>DB: transaction { selectAll where id = ? }
    DB-->>Repo: row
    Repo->>Caffeine: put(key, entity)
    Repo-->>Client: entity

    Note over Client,DB: Write-Through
    Client->>Repo: put(id, entity)
    Repo->>Caffeine: put(key, entity)
    Repo->>DB: transaction { update / batchInsert }
    Repo-->>Client: done

    Note over Client,DB: Write-Behind
    Client->>Repo: put(id, entity)
    Repo->>Caffeine: put(key, entity)
    Repo->>Repo: writeBehindQueue.send(id to entity)
    Repo-->>Client: done (immediate)
    Repo->>DB: flushBatch (async, batched)
```

## Features

- **Read-Through**: Cache miss triggers DB load via `transaction { selectAll }`, result stored in Caffeine
- **Write-Through**: `put()` updates both Caffeine and DB synchronously in a single JDBC transaction
- **Write-Behind**: `put()` updates Caffeine immediately; DB writes are batched asynchronously via a Kotlin `Channel`
- **Sync repository**: `AbstractJdbcCaffeineRepository` — all methods use blocking `transaction {}`
- **Suspend repository**: `AbstractSuspendedJdbcCaffeineRepository` — all DB calls use `suspendedTransactionAsync`
- **No Redis dependency**: Pure in-process Caffeine; suitable for single-instance deployments
- **AutoIncrement safety**: Write-Through and Write-Behind skip INSERT for AutoInc tables (DB assigns the ID)
- **Graceful shutdown**: `close()` drains the Write-Behind queue before cancelling the coroutine scope

## Usage

### Sync repository (AbstractJdbcCaffeineRepository)

```kotlin
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.jdbc.caffeine.repository.AbstractJdbcCaffeineRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement

data class ActorRecord(val id: Long, val firstName: String, val lastName: String) : java.io.Serializable {
    companion object { private const val serialVersionUID = 1L }
}

class ActorCaffeineRepository(
    config: LocalCacheConfig = LocalCacheConfig.WRITE_THROUGH,
) : AbstractJdbcCaffeineRepository<Long, ActorRecord>(config) {

    override val table = ActorTable

    override fun ResultRow.toEntity() = ActorRecord(
        id = this[ActorTable.id].value,
        firstName = this[ActorTable.firstName],
        lastName = this[ActorTable.lastName],
    )

    override fun UpdateStatement.updateEntity(entity: ActorRecord) {
        this[ActorTable.firstName] = entity.firstName
        this[ActorTable.lastName] = entity.lastName
    }

    override fun BatchInsertStatement.insertEntity(entity: ActorRecord) {
        this[ActorTable.firstName] = entity.firstName
        this[ActorTable.lastName] = entity.lastName
    }

    override fun extractId(entity: ActorRecord) = entity.id
}

// Read-Through (cache miss -> DB load)
val actor = repo.get(1L)

// Write-Through (cache + DB synchronously)
repo.put(1L, ActorRecord(1L, "Hong", "Gildong"))

// Batch write
repo.putAll(mapOf(1L to actor1, 2L to actor2))

// Invalidate cache entry (no DB effect)
repo.invalidate(1L)
```

### Suspend repository (AbstractSuspendedJdbcCaffeineRepository)

```kotlin
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.jdbc.caffeine.repository.AbstractSuspendedJdbcCaffeineRepository

class ActorSuspendedRepository(
    config: LocalCacheConfig = LocalCacheConfig.WRITE_THROUGH,
) : AbstractSuspendedJdbcCaffeineRepository<Long, ActorRecord>(config) {

    override val table = ActorTable

    override fun ResultRow.toEntity() = ActorRecord(
        id = this[ActorTable.id].value,
        firstName = this[ActorTable.firstName],
        lastName = this[ActorTable.lastName],
    )

    override fun UpdateStatement.updateEntity(entity: ActorRecord) {
        this[ActorTable.firstName] = entity.firstName
        this[ActorTable.lastName] = entity.lastName
    }

    override fun BatchInsertStatement.insertEntity(entity: ActorRecord) {
        this[ActorTable.firstName] = entity.firstName
        this[ActorTable.lastName] = entity.lastName
    }

    override fun extractId(entity: ActorRecord) = entity.id
}

// All operations are suspend functions
suspend fun example(repo: ActorSuspendedRepository) {
    val actor = repo.get(1L)                        // Read-Through
    repo.put(1L, ActorRecord(1L, "Hong", "Gil"))    // Write-Through
    repo.invalidate(1L)                             // Cache eviction only
    repo.clear()                                    // Evict all cache entries
}
```

### Write-Behind configuration

```kotlin
val behindConfig = LocalCacheConfig(
    keyPrefix = "actor",
    maximumSize = 5_000L,
    writeMode = CacheWriteMode.WRITE_BEHIND,
    writeBehindBatchSize = 200,
    writeBehindQueueCapacity = 5_000,
)
val repo = ActorCaffeineRepository(behindConfig)

// put() returns immediately; DB flush happens asynchronously in batches
repo.put(1L, actor)
```

## LocalCacheConfig Reference

```kotlin
val config = LocalCacheConfig(
    keyPrefix = "actor",                          // cache key prefix
    maximumSize = 10_000L,                        // max entries in Caffeine
    expireAfterWrite = Duration.ofMinutes(30),    // TTL from last write
    expireAfterAccess = null,                     // TTL from last access (optional)
    writeMode = CacheWriteMode.WRITE_THROUGH,     // READ_ONLY | WRITE_THROUGH | WRITE_BEHIND
    writeBehindBatchSize = 100,                   // flush batch size
    writeBehindQueueCapacity = 10_000,            // queue size (must not be unlimited)
)
```

## Test Databases

Tests run against:

- **H2 (MySQL mode)** — in-memory, default for fast local runs
- **PostgreSQL** — via Testcontainers
- **MySQL 8** — via Testcontainers

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc-caffeine:$version")
}
```

## References

- [exposed-cache — Hub module](../exposed-cache/README.md)
- [exposed-jdbc](../exposed-jdbc)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
