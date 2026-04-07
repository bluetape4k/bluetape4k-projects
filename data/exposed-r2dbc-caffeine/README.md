# bluetape4k-exposed-r2dbc-caffeine

Exposed R2DBC repository with Caffeine local (in-process) cache. No JDBC dependency -- only `exposed-cache` is referenced.

## Architecture

```mermaid
classDiagram
    direction TB

    class R2dbcCacheRepository~ID, E~ {
        <<interface>>
        +get(id: ID): E?
        +getAll(ids: Collection~ID~): Map~ID, E~
        +put(id: ID, entity: E)
        +putAll(entities: Map~ID, E~)
        +invalidate(id: ID)
        +clear()
        +findByIdFromDb(id: ID): E?
        +findAllFromDb(ids: Collection~ID~): List~E~
    }

    class R2dbcCaffeineRepository~ID, E~ {
        <<interface>>
        +config: LocalCacheConfig
        +cache: AsyncCache~String, E~
    }

    class AbstractR2dbcCaffeineRepository~ID, E~ {
        <<abstract>>
        #table: IdTable~ID~
        #toEntity(): E
        #updateEntity(entity: E)
        #insertEntity(entity: E)
        -writeBehindQueue: Channel
        -writeBehindJob: Job
    }

    R2dbcCacheRepository <|-- R2dbcCaffeineRepository
    R2dbcCaffeineRepository <|.. AbstractR2dbcCaffeineRepository
```

```mermaid
sequenceDiagram
    participant Client
    participant Repository
    participant Caffeine as Caffeine AsyncCache
    participant DB as R2DBC Database

    Note over Client,DB: Read-Through
    Client->>Repository: get(id)
    Repository->>Caffeine: getIfPresent(key)
    alt Cache Hit
        Caffeine-->>Repository: entity
    else Cache Miss
        Caffeine-->>Repository: null
        Repository->>DB: suspendTransaction { selectAll }
        DB-->>Repository: entity
        Repository->>Caffeine: put(key, entity)
    end
    Repository-->>Client: entity

    Note over Client,DB: Write-Through
    Client->>Repository: put(id, entity)
    Repository->>Caffeine: put(key, entity)
    Repository->>DB: suspendTransaction { update/insert }
    Repository-->>Client: done

    Note over Client,DB: Write-Behind
    Client->>Repository: put(id, entity)
    Repository->>Caffeine: put(key, entity)
    Repository->>Repository: writeBehindQueue.send(entry)
    Repository-->>Client: done (immediate)
    Repository->>DB: flushBatch (async)
```

## Features

- **Read-Through**: Cache miss triggers DB load via R2DBC `suspendTransaction`, result cached in Caffeine
- **Write-Through**: `put()` updates both Caffeine and DB synchronously
- **Write-Behind**: `put()` updates Caffeine immediately, DB write is batched asynchronously via `Channel`
- **No JDBC dependency**: Pure R2DBC with `exposed-cache` interfaces only
- **Caffeine AsyncCache**: Non-blocking cache backed by `CompletableFuture`
- **Coroutine-native**: All DB operations use `suspendTransaction`

## Usage

```kotlin
class ActorRepository(
    config: LocalCacheConfig = LocalCacheConfig.WRITE_THROUGH,
) : AbstractR2dbcCaffeineRepository<Long, ActorRecord>(config) {

    override val table = ActorTable

    override suspend fun ResultRow.toEntity() = toActorRecord()

    override fun UpdateStatement.updateEntity(entity: ActorRecord) {
        this[ActorTable.firstName] = entity.firstName
        this[ActorTable.lastName] = entity.lastName
        this[ActorTable.email] = entity.email
    }

    override fun BatchInsertStatement.insertEntity(entity: ActorRecord) {
        this[ActorTable.firstName] = entity.firstName
        this[ActorTable.lastName] = entity.lastName
        this[ActorTable.email] = entity.email
    }

    override fun extractId(entity: ActorRecord) = entity.id
}

// Read-Through (cache miss -> DB load)
val actor = repository.get(1L)

// Write-Through (cache + DB)
repository.put(1L, updatedActor)

// Write-Behind (cache immediate, DB async batch)
val behindConfig = LocalCacheConfig(writeMode = CacheWriteMode.WRITE_BEHIND)
val behindRepo = ActorRepository(behindConfig)
behindRepo.put(1L, updatedActor)  // returns immediately
```

## Dependencies

| Dependency | Purpose |
|---|---|
| `bluetape4k-exposed-r2dbc` | Exposed R2DBC transaction support |
| `bluetape4k-exposed-cache` | `R2dbcCacheRepository`, `LocalCacheConfig`, `CacheMode` |
| `bluetape4k-coroutines` | Coroutines utilities |
| `com.github.ben-manes.caffeine:caffeine` | In-process async cache |
