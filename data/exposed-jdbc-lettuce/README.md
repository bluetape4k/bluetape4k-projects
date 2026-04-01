# Module bluetape4k-exposed-jdbc-lettuce

Exposed JDBC와 Lettuce Redis 캐시를 결합한 Read-through / Write-through / Write-behind 캐시 레포지토리 모듈입니다. 동기(
`JdbcLettuceRepository`) 버전과 코루틴 네이티브(`SuspendedJdbcLettuceRepository`) 버전을 모두 제공합니다.

## 개요

`bluetape4k-exposed-jdbc-lettuce`는 다음을 제공합니다:

- **Read-through 캐시**: `findById` 시 캐시 미스이면 DB에서 자동 로드 후 Redis에 캐싱
- **Write-through / Write-behind**: `save` 시 Redis와 DB를 동시(또는 비동기)로 반영
- **동기 레포지토리**: `JdbcLettuceRepository` / `AbstractJdbcLettuceRepository`
- **코루틴 레포지토리**: `SuspendedJdbcLettuceRepository` / `AbstractSuspendedJdbcLettuceRepository`
- **MapLoader / MapWriter**: Lettuce `LettuceLoadedMap` 연동을 위한 Exposed 기반 구현체
  - `loadAllKeys()`는 PK 오름차순으로 안정적으로 순회
  - writer의 `chunkSize`/loader의 `batchSize`는 0보다 커야 함

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc-lettuce:${version}")
}
```

## 기본 사용법

### 1. 동기 레포지토리 구현 (AbstractJdbcLettuceRepository)

```kotlin
import io.bluetape4k.exposed.lettuce.repository.AbstractJdbcLettuceRepository
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient

data class UserRecord(val id: Long, val name: String, val email: String)

class UserLettuceRepository(redisClient: RedisClient):
    AbstractJdbcLettuceRepository<Long, UserRecord>(
        client = redisClient,
        config = LettuceCacheConfig.READ_WRITE_THROUGH,
    ) {
    override val table = UserTable

    override fun ResultRow.toEntity() = UserRecord(
        id = this[UserTable.id].value,
        name = this[UserTable.name],
        email = this[UserTable.email],
    )

    override fun UpdateStatement.updateEntity(entity: UserRecord) {
        this[UserTable.name] = entity.name
        this[UserTable.email] = entity.email
    }

    override fun BatchInsertStatement.insertEntity(entity: UserRecord) {
        this[UserTable.id] = entity.id
        this[UserTable.name] = entity.name
        this[UserTable.email] = entity.email
    }

    override fun extractId(entity: UserRecord) = entity.id
}

// 사용
val repo = UserLettuceRepository(redisClient)
repo.save(1L, UserRecord(1L, "홍길동", "hong@example.com"))
val user = repo.findById(1L)   // 캐시 미스 시 DB 조회 후 캐싱
repo.delete(1L)                // Redis + DB 동시 삭제
```

### 2. 코루틴 레포지토리 구현 (AbstractSuspendedJdbcLettuceRepository)

```kotlin
import io.bluetape4k.exposed.lettuce.repository.AbstractSuspendedJdbcLettuceRepository

class UserSuspendedRepository(redisClient: RedisClient):
    AbstractSuspendedJdbcLettuceRepository<Long, UserRecord>(
        client = redisClient,
        config = LettuceCacheConfig.READ_WRITE_THROUGH,
    ) {
    override val table = UserTable
    override fun ResultRow.toEntity() = /* ... */
        override
    fun UpdateStatement.updateEntity(entity: UserRecord) = /* ... */
        override
    fun BatchInsertStatement.insertEntity(entity: UserRecord) = /* ... */
        override
    fun extractId(entity: UserRecord) = entity.id
}

// suspend 함수로 사용
suspend fun example(repo: UserSuspendedRepository) {
    repo.save(1L, UserRecord(1L, "홍길동", "hong@example.com"))
    val user = repo.findById(1L)     // NearCache → Redis → DB 순으로 조회
    repo.clearCache()                // Redis 캐시 전체 삭제
}
```

## 아키텍처 개요

```mermaid
classDiagram
    direction TB
    class LettuceJdbcRepository~E~ {
        <<abstract>>
        -nearCache: LettuceNearCache
        +findByIdOrNull(id): E?
        +save(entity): E
        +deleteById(id): Int
    }
    class LettuceNearCache~V~ {
        +get(key): V?
        +put(key, value)
        +invalidate(key)
    }
    class ReadWriteThrough {
        <<strategy>>
        DB읽기 → 캐시 저장
        캐시 먼저 조회 → Miss시 DB
    }

    LettuceJdbcRepository --> LettuceNearCache : L1/L2 cache
    LettuceJdbcRepository --> ReadWriteThrough : pattern

    classDef repoStyle fill:#2196F3,color:#fff,stroke:#1565C0
    classDef cacheStyle fill:#F44336,color:#fff,stroke:#B71C1C
    classDef serviceStyle fill:#4CAF50,color:#fff,stroke:#388E3C
    class LettuceJdbcRepository:::repoStyle
    class LettuceNearCache:::cacheStyle
    class ReadWriteThrough:::serviceStyle
```

```mermaid
sequenceDiagram
    participant App
    participant Repo as LettuceJdbcRepository
    participant Cache as LettuceNearCache
    participant DB as PostgreSQL

    App->>Repo: findByIdOrNull(id)
    Repo->>Cache: get(id)
    alt Cache Hit
        Cache-->>Repo: entity
    else Cache Miss
        Repo->>DB: SELECT WHERE id=?
        DB-->>Repo: row
        Repo->>Cache: put(id, entity)
    end
    Repo-->>App: entity?
```

## 클래스 다이어그램

### Repository 계층 구조

```mermaid
classDiagram
    direction TB

    class JdbcLettuceRepository~ID, E~ {
<<interface>>
+table: IdTable~ID~
+config: LettuceCacheConfig
+findById(id: ID) E?
+findAll(ids: Collection~ID~) Map~ ID, E~
+findAll(limit, offset, ...) List~E~
+findByIdFromDb(id: ID) E?
+findAllFromDb(ids) List~E~
+save(id: ID, entity: E)
+saveAll(entities: Map~ID, E~)
+delete(id: ID)
+deleteAll(ids)
+clearCache()
}

class SuspendedJdbcLettuceRepository~ID, E~ {
<<interface>>
+table: IdTable~ID~
+config: LettuceCacheConfig
+suspend findById(id: ID) E?
+suspend findAll(ids) Map~ID, E~
+suspend findByIdFromDb(id: ID) E?
+suspend save(id: ID, entity: E)
+suspend delete(id: ID)
+suspend clearCache()
}

class AbstractJdbcLettuceRepository~ID, E~ {
<<abstract>>
#cache: LettuceLoadedMap~ID, E~
+abstract ResultRow.toEntity() E
+abstract UpdateStatement.updateEntity(E)
+abstract BatchInsertStatement.insertEntity(E)
#extractId(entity: E) ID
 }

class AbstractSuspendedJdbcLettuceRepository~ID, E~ {
<<abstract>>
#cache: LettuceSuspendedLoadedMap~ID, E~
#nearCache: LettuceSuspendNearCache~E~ ?
+abstract ResultRow.toEntity() E
+abstract UpdateStatement.updateEntity(E)
+abstract BatchInsertStatement.insertEntity(E)
#extractId(entity: E) ID
}

class EntityMapLoader~ID, E~ {
<<abstract>>
+load(key: ID) E?
+loadAllKeys() Iterable~ID~
#abstract loadById(id: ID) E?
#abstract loadAllIds() Iterable~ID~
}

class EntityMapWriter~ID, E~ {
<<abstract>>
-retry: Retry
+write(map: Map~ID, E~)
+delete(keys: Collection~ID~)
#abstract writeEntities(map: Map~ID, E~)
#abstract deleteEntities(keys: Collection~ID~)
}

class SuspendedEntityMapLoader~ID, E~ {
<<abstract>>
+suspend load(key: ID) E?
+suspend loadAllKeys() List~ID~
#abstract loadById(id: ID) E?
#abstract loadAllIds() List~ID~
}

class SuspendedEntityMapWriter~ID, E~ {
<<abstract>>
-retry: Retry
+suspend write(map: Map~ID, E~)
+suspend delete(keys: Collection~ID~)
#abstract writeEntities(map: Map~ID, E~)
#abstract deleteEntities(keys: Collection~ID~)
}

class ExposedEntityMapLoader~ID, E~ {
-table: IdTable~ID~
-toEntity: (ResultRow) → E
#loadById(id: ID) E?
#loadAllIds() Iterable~ID~
}

class ExposedEntityMapWriter~ID, E~ {
-table: IdTable~ID~
-writeMode: WriteMode
#writeEntities(map: Map~ID, E~)
#deleteEntities(keys: Collection~ID~)
}

class SuspendedExposedEntityMapLoader~ID, E~ {
-table: IdTable~ID~
-toEntity: (ResultRow) → E
#loadById(id: ID) E?
#loadAllIds() List~ID~
}

class SuspendedExposedEntityMapWriter~ID, E~ {
-table: IdTable~ID~
-writeMode: WriteMode
#writeEntities(map: Map~ID, E~)
#deleteEntities(keys: Collection~ID~)
}

JdbcLettuceRepository <|.. AbstractJdbcLettuceRepository
SuspendedJdbcLettuceRepository <|.. AbstractSuspendedJdbcLettuceRepository
EntityMapLoader <|-- ExposedEntityMapLoader
EntityMapWriter <|-- ExposedEntityMapWriter
SuspendedEntityMapLoader <|-- SuspendedExposedEntityMapLoader
SuspendedEntityMapWriter <|-- SuspendedExposedEntityMapWriter
AbstractJdbcLettuceRepository ..> ExposedEntityMapLoader: uses
AbstractJdbcLettuceRepository ..> ExposedEntityMapWriter: uses
AbstractSuspendedJdbcLettuceRepository ..> SuspendedExposedEntityMapLoader: uses
AbstractSuspendedJdbcLettuceRepository ..> SuspendedExposedEntityMapWriter: uses
```

## 시퀀스 다이어그램

### Read-through — findById (동기)

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractJdbcLettuceRepository
    participant Map as LettuceLoadedMap
    participant Redis as Redis (Lettuce)
    participant Loader as ExposedEntityMapLoader
    participant DB as Database
    Client ->> Repo: findById(id)
    Repo ->> Map: cache[id]
    Map ->> Redis: GET key
    alt 캐시 HIT
        Redis -->> Map: entity (직렬화된 값)
        Map -->> Repo: entity
        Repo -->> Client: entity
    else 캐시 MISS
        Redis -->> Map: null
        Map ->> Loader: load(id)
        Loader ->> DB: SELECT * FROM table WHERE id = ?
        DB -->> Loader: ResultRow
        Loader ->> Loader: ResultRow.toEntity()
        Loader -->> Map: entity
        Map ->> Redis: SET key entity
        Map -->> Repo: entity
        Repo -->> Client: entity
    end
```

### Read-through — findById (코루틴, NearCache 포함)

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractSuspendedJdbcLettuceRepository
    participant Near as LettuceSuspendNearCache<br/>(Caffeine)
    participant Map as LettuceSuspendedLoadedMap
    participant Redis as Redis (Lettuce)
    participant DB as Database
    Client ->> Repo: suspend findById(id)
    Repo ->> Near: nearCache?.get(key)
    alt NearCache HIT (로컬 Caffeine)
        Near -->> Repo: entity
        Repo -->> Client: entity
    else NearCache MISS
        Near -->> Repo: null
        Repo ->> Map: cache.get(id)
        Map ->> Redis: GET key
        alt Redis HIT
            Redis -->> Map: entity
        else Redis MISS
            Redis -->> Map: null
            Map ->> DB: SELECT * FROM table WHERE id = ?
            DB -->> Map: ResultRow → entity
            Map ->> Redis: SET key entity
        end
        Map -->> Repo: entity
        Repo ->> Near: nearCache.put(key, entity)
        Repo -->> Client: entity
    end
```

### Write-through — save

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractJdbcLettuceRepository
    participant Map as LettuceLoadedMap
    participant Redis as Redis (Lettuce)
    participant Writer as ExposedEntityMapWriter
    participant DB as Database
    Client ->> Repo: save(id, entity)
    Repo ->> Map: cache[id] = entity
    Map ->> Redis: SET key entity
    alt writeMode == WRITE_THROUGH
        Map ->> Writer: write(mapOf(id to entity))
        Writer ->> DB: UPDATE / INSERT INTO table ...
        DB -->> Writer: (완료)
        Writer -->> Map: (완료)
    else writeMode == WRITE_BEHIND
        Map ->> Writer: write(mapOf(id to entity)) [비동기]
        Note over Writer, DB: 백그라운드에서 DB 반영
    end
    Map -->> Repo: (완료)
    Repo -->> Client: (완료)
```

### Write-behind — save (코루틴)

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractSuspendedJdbcLettuceRepository
    participant Near as LettuceSuspendNearCache (Caffeine)
    participant Map as LettuceSuspendedLoadedMap
    participant Redis as Redis (Lettuce)
    participant Writer as SuspendedExposedEntityMapWriter
    participant DB as Database
    Client ->> Repo: suspend save(id, entity)
    Repo ->> Map: cache.set(id, entity)
    Map ->> Redis: SET key entity (TTL)
    Redis -->> Map: OK
    alt writeMode == WRITE_THROUGH
        Map ->> Writer: suspend write(mapOf(id to entity))
        Writer ->> DB: suspendedTransaction — UPDATE / batchInsert
        DB -->> Writer: OK
        Writer -->> Map: OK
    else writeMode == WRITE_BEHIND
        Map -->> Repo: OK (즉시 반환)
        Note over Writer, DB: 백그라운드에서 비동기로 DB 반영
        Map --) Writer: suspend write(mapOf(id to entity))
        Writer ->> DB: suspendedTransaction — UPDATE / batchInsert
        DB -->> Writer: OK
    end
    Repo ->> Near: nearCache?.put(key, entity)
    Repo -->> Client: OK
```

### delete — 캐시 + DB 동시 삭제

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractJdbcLettuceRepository
    participant Map as LettuceLoadedMap
    participant Redis as Redis (Lettuce)
    participant Writer as ExposedEntityMapWriter
    participant DB as Database
    Client ->> Repo: delete(id)
    Repo ->> Map: cache.delete(id)
    Map ->> Redis: DEL key
    alt writeMode != NONE
        Map ->> Writer: delete(listOf(id))
        Writer ->> DB: DELETE FROM table WHERE id = ?
        DB -->> Writer: (완료)
    end
    Map -->> Repo: (완료)
    Repo -->> Client: (완료)
```

## JdbcLettuceRepository 주요 메서드

| 메서드                           | 설명                               |
|-------------------------------|----------------------------------|
| `findById(id)`                | 캐시 조회 → 미스 시 DB Read-through     |
| `findAll(ids)`                | 다건 캐시 조회 → 미스 키만 DB Read-through |
| `findAll(limit, offset, ...)` | DB 조회 후 결과를 캐시에 적재               |
| `findByIdFromDb(id)`          | 캐시 우회, DB 직접 조회                  |
| `findAllFromDb(ids)`          | 캐시 우회, DB 직접 다건 조회               |
| `countFromDb()`               | DB 전체 레코드 수                      |
| `save(id, entity)`            | Redis 저장 + WriteMode에 따라 DB 반영   |
| `saveAll(entities)`           | 다건 저장                            |
| `delete(id)`                  | Redis + DB 동시 삭제                 |
| `deleteAll(ids)`              | 다건 삭제                            |
| `clearCache()`                | Redis 키 전체 삭제 (DB 영향 없음)         |

## LettuceCacheConfig — 쓰기 모드

| WriteMode            | 동작                            |
|----------------------|-------------------------------|
| `READ_WRITE_THROUGH` | save 시 Redis + DB 동시 반영 (기본값) |
| `READ_WRITE_BEHIND`  | save 시 Redis 즉시, DB는 비동기 반영   |
| `READ_ONLY`          | Redis에만 저장, DB 쓰기 없음          |

## 주요 파일/클래스 목록

| 파일                                                     | 설명                                                 |
|--------------------------------------------------------|----------------------------------------------------|
| `repository/JdbcLettuceRepository.kt`                  | 동기 캐시 레포지토리 인터페이스                                  |
| `repository/SuspendedJdbcLettuceRepository.kt`         | 코루틴 캐시 레포지토리 인터페이스                                 |
| `repository/AbstractJdbcLettuceRepository.kt`          | 동기 추상 구현체 (LettuceLoadedMap 기반)                    |
| `repository/AbstractSuspendedJdbcLettuceRepository.kt` | 코루틴 추상 구현체 (LettuceSuspendedLoadedMap + NearCache) |
| `map/EntityMapLoader.kt`                               | MapLoader 추상 기반 클래스                                |
| `map/EntityMapWriter.kt`                               | MapWriter 추상 기반 클래스 (Resilience4j Retry 내장)        |
| `map/ExposedEntityMapLoader.kt`                        | Exposed DSL 기반 동기 MapLoader                        |
| `map/ExposedEntityMapWriter.kt`                        | Exposed DSL 기반 동기 MapWriter                        |
| `map/SuspendedEntityMapLoader.kt`                      | suspendedTransactionAsync 기반 MapLoader             |
| `map/SuspendedEntityMapWriter.kt`                      | suspendedTransactionAsync + Retry 기반 MapWriter     |
| `map/SuspendedExposedEntityMapLoader.kt`               | Exposed DSL 기반 코루틴 MapLoader                       |
| `map/SuspendedExposedEntityMapWriter.kt`               | Exposed DSL 기반 코루틴 MapWriter                       |

## 테스트

```bash
./gradlew :bluetape4k-exposed-jdbc-lettuce:test
```

## 참고

- [bluetape4k-exposed-jdbc](../exposed-jdbc)
- [bluetape4k-lettuce](../../infra/lettuce)
- [Lettuce Redis Client](https://lettuce.io)
