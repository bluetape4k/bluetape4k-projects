# Module bluetape4k-exposed-r2dbc-lettuce

Exposed R2DBC와 Lettuce Redis 캐시를 결합한 코루틴 네이티브 Read-through / Write-through / Write-behind 캐시 레포지토리 모듈입니다.
`runBlocking` 없이 `suspendTransaction` 기반으로 완전한 코루틴 네이티브 동작을 보장합니다.

## 개요

`bluetape4k-exposed-r2dbc-lettuce`는 다음을 제공합니다:

- **Read-through 캐시**: `findById` 시 캐시 미스이면 R2DBC `suspendTransaction`으로 DB 자동 로드 후 Redis에 캐싱
- **Write-through / Write-behind**: `save` 시 Redis와 DB를 동시(또는 비동기)로 반영
- **NearCache 지원**: Caffeine 로컬 캐시(front) + Redis(back) 2-tier 캐시 (옵션)
- **코루틴 레포지토리**: `R2dbcLettuceRepository` / `AbstractR2dbcLettuceRepository`
- **MapLoader / MapWriter**: Lettuce `LettuceSuspendedLoadedMap` 연동을 위한 R2DBC 기반 구현체

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc-lettuce:${version}")
}
```

## 기본 사용법

### 코루틴 레포지토리 구현 (AbstractR2dbcLettuceRepository)

```kotlin
import io.bluetape4k.exposed.r2dbc.lettuce.repository.AbstractR2dbcLettuceRepository
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient

data class UserRecord(val id: Long, val name: String, val email: String): java.io.Serializable

class UserR2dbcLettuceRepository(redisClient: RedisClient):
    AbstractR2dbcLettuceRepository<Long, UserRecord>(
        client = redisClient,
        config = LettuceCacheConfig.READ_WRITE_THROUGH,
    ) {
    override val table = UserTable

    override suspend fun ResultRow.toEntity() = UserRecord(
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

// suspend 함수로 사용
suspend fun example(repo: UserR2dbcLettuceRepository) {
    repo.save(1L, UserRecord(1L, "홍길동", "hong@example.com"))
    val user = repo.findById(1L)   // NearCache → Redis → DB 순으로 조회
    repo.delete(1L)                // Redis + DB 동시 삭제
    repo.clearCache()              // Redis 캐시 전체 삭제
}
```

## 클래스 다이어그램

### Repository 계층 구조

```mermaid
classDiagram
    direction TB

    class R2dbcLettuceRepository~ID, E~ {
<<interface>>
+table: IdTable~ID~
+config: LettuceCacheConfig
+suspend findById(id: ID) E?
+suspend findAll(ids: Collection~ID~) Map~ ID, E~
+suspend findAll(limit, offset, ...) List~E~
+suspend findByIdFromDb(id: ID) E?
+suspend findAllFromDb(ids) List~E~
+suspend countFromDb() Long
+suspend save(id: ID, entity: E)
+suspend saveAll(entities: Map~ID, E~)
+suspend delete(id: ID)
+suspend deleteAll(ids)
+suspend clearCache()
}

class AbstractR2dbcLettuceRepository~ID, E~ {
<<abstract>>
#cache: LettuceSuspendedLoadedMap~ID, E~
#nearCache: LettuceSuspendNearCache~E~ ?
+abstract suspend ResultRow.toEntity() E
+abstract UpdateStatement.updateEntity(E)
+abstract BatchInsertStatement.insertEntity(E)
#serializeKey(id: ID) String
#extractId(entity: E) ID
}

class R2dbcEntityMapLoader~ID, E~ {
<<abstract>>
+suspend load(key: ID) E?
+suspend loadAllKeys() List~ID~
#abstract suspend loadById(id: ID) E?
#abstract suspend loadAllIds() List~ID~
}

class R2dbcEntityMapWriter~ID, E~ {
<<abstract>>
-retry: Retry
+suspend write(map: Map~ID, E~)
+suspend delete(keys: Collection~ID~)
#abstract suspend writeEntities(map: Map~ID, E~)
#abstract suspend deleteEntities(keys: Collection~ID~)
}

class R2dbcExposedEntityMapLoader~ID, E~ {
-table: IdTable~ID~
-toEntity: suspend ResultRow.() → E
-batchSize: Int
#suspend loadById(id: ID) E?
#suspend loadAllIds() List~ID~
}

class R2dbcExposedEntityMapWriter~ID, E~ {
-table: IdTable~ID~
-writeMode: WriteMode
-chunkSize: Int
#suspend writeEntities(map: Map~ID, E~)
#suspend deleteEntities(keys: Collection~ID~)
}

class LettuceSuspendedLoadedMap~ID, E~ {
+suspend get(key: ID) E?
+suspend getAll(keys: Set~ID~) Map~ ID, E~
+suspend set(key: ID, value: E)
+suspend delete(key: ID)
+suspend deleteAll(keys: Collection~ID~)
+suspend clear()
}

class LettuceSuspendNearCache~E~ {
+suspend get(key: String) E?
+suspend put(key: String, value: E)
+suspend remove(key: String)
+suspend clearAll()
}

R2dbcLettuceRepository <|.. AbstractR2dbcLettuceRepository
R2dbcEntityMapLoader <|-- R2dbcExposedEntityMapLoader
R2dbcEntityMapWriter <|-- R2dbcExposedEntityMapWriter
AbstractR2dbcLettuceRepository *-- LettuceSuspendedLoadedMap
AbstractR2dbcLettuceRepository o-- LettuceSuspendNearCache
LettuceSuspendedLoadedMap ..> R2dbcExposedEntityMapLoader: uses (cache miss)
LettuceSuspendedLoadedMap ..> R2dbcExposedEntityMapWriter: uses (write)
```

## 시퀀스 다이어그램

### Read-through — findById (NearCache 포함)

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractR2dbcLettuceRepository
    participant Near as LettuceSuspendNearCache (Caffeine)
    participant Map as LettuceSuspendedLoadedMap
    participant Redis as Redis (Lettuce)
    participant Loader as R2dbcExposedEntityMapLoader
    participant DB as Database (R2DBC)
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
            Redis -->> Map: entity (직렬화된 값)
            Map -->> Repo: entity
        else Redis MISS
            Redis -->> Map: null
            Map ->> Loader: suspend load(id)
            Loader ->> DB: suspendTransaction — SELECT WHERE id = ?
            DB -->> Loader: ResultRow
            Loader ->> Loader: suspend ResultRow.toEntity()
            Loader -->> Map: entity
            Map ->> Redis: SET key entity (TTL)
            Map -->> Repo: entity
        end
        Repo ->> Near: nearCache?.put(key, entity)
        Repo -->> Client: entity
    end
```

### Read-through — findAll (다건 조회, NearCache 포함)

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractR2dbcLettuceRepository
    participant Near as LettuceSuspendNearCache (Caffeine)
    participant Map as LettuceSuspendedLoadedMap
    participant Redis as Redis (Lettuce)
    participant DB as Database (R2DBC)
    Client ->> Repo: suspend findAll(ids)
    loop ids 순회
        Repo ->> Near: nearCache?.get(key)
        alt NearCache HIT
            Near -->> Repo: entity → result[id] = entity
        else NearCache MISS
            Near -->> Repo: null → missedIds에 추가
        end
    end
    alt missedIds 존재
        Repo ->> Map: cache.getAll(missedIds)
        Map ->> Redis: MGET keys
        Note over Map, DB: Redis MISS 키는 R2dbcExposedEntityMapLoader가<br/>suspendTransaction으로 DB에서 일괄 로드 후 Redis에 SET
        Map -->> Repo: Map(ID → entity)
        loop 로드된 엔티티
            Repo ->> Near: nearCache?.put(key, entity)
        end
    end
    Repo -->> Client: Map(ID → entity)
```

### Write-through — save

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractR2dbcLettuceRepository
    participant Near as LettuceSuspendNearCache (Caffeine)
    participant Map as LettuceSuspendedLoadedMap
    participant Redis as Redis (Lettuce)
    participant Writer as R2dbcExposedEntityMapWriter
    participant DB as Database (R2DBC)
    Client ->> Repo: suspend save(id, entity)
    Repo ->> Map: cache.set(id, entity)
    Map ->> Redis: SET key entity (TTL)
    Redis -->> Map: OK
    Map ->> Writer: suspend write(mapOf(id to entity))
    Writer ->> DB: suspendTransaction — SELECT existing IDs
    DB -->> Writer: existingIds
    alt id 존재 (UPDATE)
        Writer ->> DB: UPDATE table SET ... WHERE id = ?
        DB -->> Writer: OK
    else id 미존재 (INSERT)
        Writer ->> DB: batchInsert INTO table ...
        DB -->> Writer: OK
    end
    Writer -->> Map: OK
    Map -->> Repo: OK
    Repo ->> Near: nearCache?.put(key, entity)
    Repo -->> Client: OK
```

### Write-behind — save (비동기 DB 반영)

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractR2dbcLettuceRepository
    participant Near as LettuceSuspendNearCache (Caffeine)
    participant Map as LettuceSuspendedLoadedMap
    participant Redis as Redis (Lettuce)
    participant Writer as R2dbcExposedEntityMapWriter
    participant DB as Database (R2DBC)
    Client ->> Repo: suspend save(id, entity)
    Repo ->> Map: cache.set(id, entity)
    Map ->> Redis: SET key entity (TTL)
    Redis -->> Map: OK
    Map -->> Repo: OK (즉시 반환)
    Repo ->> Near: nearCache?.put(key, entity)
    Repo -->> Client: OK (즉시 반환)
    Note over Writer, DB: 백그라운드에서 비동기로 DB 반영
    Map --) Writer: suspend write(mapOf(id to entity))
    Writer ->> DB: suspendTransaction — UPDATE / batchInsert
    DB -->> Writer: OK
```

### delete — 캐시 + DB 동시 삭제

```mermaid
sequenceDiagram
    participant Client
    participant Repo as AbstractR2dbcLettuceRepository
    participant Near as LettuceSuspendNearCache (Caffeine)
    participant Map as LettuceSuspendedLoadedMap
    participant Redis as Redis (Lettuce)
    participant Writer as R2dbcExposedEntityMapWriter
    participant DB as Database (R2DBC)
    Client ->> Repo: suspend delete(id)
    Repo ->> Map: cache.delete(id)
    Map ->> Redis: DEL key
    Redis -->> Map: OK
    alt writeMode != NONE
        Map ->> Writer: suspend delete(listOf(id))
        Writer ->> DB: suspendTransaction — DELETE FROM table WHERE id = ?
        DB -->> Writer: OK
        Writer -->> Map: OK
    end
    Map -->> Repo: OK
    Repo ->> Near: nearCache?.remove(key)
    Repo -->> Client: OK
```

## R2dbcLettuceRepository 주요 메서드

| 메서드                                   | 설명                                           |
|---------------------------------------|----------------------------------------------|
| `suspend findById(id)`                | NearCache → Redis → DB 순으로 조회 (Read-through) |
| `suspend findAll(ids)`                | 다건 조회, 미스 키만 Redis → DB Read-through         |
| `suspend findAll(limit, offset, ...)` | R2DBC DB 조회 후 결과를 Redis에 적재                  |
| `suspend findByIdFromDb(id)`          | 캐시 우회, R2DBC `suspendTransaction` 직접 조회      |
| `suspend findAllFromDb(ids)`          | 캐시 우회, R2DBC 다건 직접 조회                        |
| `suspend countFromDb()`               | R2DBC DB 전체 레코드 수                            |
| `suspend save(id, entity)`            | Redis 저장 + WriteMode에 따라 R2DBC DB 반영         |
| `suspend saveAll(entities)`           | 다건 저장                                        |
| `suspend delete(id)`                  | Redis + R2DBC DB 동시 삭제                       |
| `suspend deleteAll(ids)`              | 다건 삭제                                        |
| `suspend clearCache()`                | NearCache + Redis 키 전체 삭제 (DB 영향 없음)         |

## LettuceCacheConfig — 쓰기 모드

| WriteMode            | 동작                                  |
|----------------------|-------------------------------------|
| `READ_WRITE_THROUGH` | save 시 Redis + R2DBC DB 동시 반영 (기본값) |
| `READ_WRITE_BEHIND`  | save 시 Redis 즉시, R2DBC DB는 비동기 반영   |
| `READ_ONLY`          | Redis에만 저장, DB 쓰기 없음                |

## NearCache 설정

`LettuceCacheConfig.nearCacheEnabled = true`로 Caffeine 로컬 캐시(front)를 활성화할 수 있습니다.

```kotlin
val config = LettuceCacheConfig(
    writeMode = WriteMode.WRITE_THROUGH,
    nearCacheEnabled = true,
    nearCacheName = "user-near-cache",
    nearCacheMaxSize = 1000,
    nearCacheTtl = Duration.ofMinutes(5),
)
```

NearCache가 활성화되면 조회 순서: **Caffeine(로컬) → Redis → DB**

## JDBC 버전과의 차이점

| 항목               | exposed-jdbc-lettuce                               | exposed-r2dbc-lettuce            |
|------------------|----------------------------------------------------|----------------------------------|
| DB 드라이버          | JDBC (blocking)                                    | R2DBC (non-blocking)             |
| 트랜잭션             | `transaction {}` / `suspendedTransactionAsync(IO)` | `suspendTransaction {}`          |
| `toEntity`       | 일반 함수 (`fun`)                                      | suspend 함수 (`suspend fun`)       |
| `runBlocking` 사용 | 없음 (`LettuceSuspendedLoadedMap`)                   | 없음 (`LettuceSuspendedLoadedMap`) |
| 동기 레포지토리         | `JdbcLettuceRepository` 제공                         | 미제공 (suspend only)               |

## 주요 파일/클래스 목록

| 파일                                             | 설명                                                                  |
|------------------------------------------------|---------------------------------------------------------------------|
| `repository/R2dbcLettuceRepository.kt`         | suspend 캐시 레포지토리 인터페이스                                              |
| `repository/AbstractR2dbcLettuceRepository.kt` | 추상 구현체 (LettuceSuspendedLoadedMap + NearCache)                      |
| `map/R2dbcEntityMapLoader.kt`                  | R2DBC `suspendTransaction` 기반 MapLoader 추상 클래스                      |
| `map/R2dbcEntityMapWriter.kt`                  | R2DBC `suspendTransaction` + Resilience4j Retry 기반 MapWriter 추상 클래스 |
| `map/R2dbcExposedEntityMapLoader.kt`           | Exposed R2DBC DSL 기반 MapLoader 구현체                                  |
| `map/R2dbcExposedEntityMapWriter.kt`           | Exposed R2DBC DSL 기반 MapWriter 구현체 (upsert 전략)                      |

## 테스트

```bash
./gradlew :bluetape4k-exposed-r2dbc-lettuce:test
```

## 참고

- [bluetape4k-exposed-r2dbc](../exposed-r2dbc)
- [bluetape4k-exposed-jdbc-lettuce](../exposed-jdbc-lettuce)
- [bluetape4k-lettuce](../../infra/lettuce)
- [Lettuce Redis Client](https://lettuce.io)
