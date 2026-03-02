# Module bluetape4k-exposed-jdbc-redisson

Exposed JDBC와 Redisson 캐시를 결합해 Read-Through/Write-Through 캐시 패턴을 구성하는 모듈입니다.

## 개요

`bluetape4k-exposed-jdbc-redisson`은 JetBrains Exposed ORM과 [Redisson](https://github.com/redisson/redisson) Redis 클라이언트를 통합하여,
데이터베이스 조회 결과를 Redis에 캐싱하는 패턴을 쉽게 구현할 수 있도록 지원합니다.

### 주요 기능

- **MapLoader/MapWriter 지원**: Redisson Read-Through/Write-Through 캐시 연동
- **Repository 추상화**: 캐시 + DB 접근 공통 패턴 (`JdbcRedissonRepository`, `SuspendedJdbcRedissonRepository`)
- **동기/코루틴 구현 제공**: 운영 환경에 맞는 방식 선택
- **Near Cache 지원**: Local Cache + Redis 2-Tier 캐시
- **Write-Behind 지원**: 비동기 DB 반영 패턴

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc-redisson:${version}")
    implementation("org.redisson:redisson:3.37.0")
}
```

## 기본 사용법

### 1. JdbcRedissonRepository (동기) 구현

`AbstractJdbcRedissonRepository`를 상속하여 동기 방식의 캐시 Repository를 구현합니다.

```kotlin
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.redisson.repository.AbstractJdbcRedissonRepository
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.update
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

    // Write-Through 모드 시 구현 필요
    override fun doUpdateEntity(statement: UpdateStatement, entity: UserRecord) {
        statement[UserTable.name]  = entity.name
        statement[UserTable.email] = entity.email
    }
}

// 사용 (Read-Through)
val repo = UserRedissonRepository(redissonClient, RedisCacheConfig.readOnly())

// 캐시에서 조회 (미스 시 DB에서 자동 로드)
val user = repo[1L]

// DB에서 직접 조회 (캐시 무시)
val freshUser = repo.findByIdFromDb(1L)

// 여러 엔티티 일괄 조회
val users = repo.getAll(listOf(1L, 2L, 3L))

// 캐시 무효화
repo.invalidate(1L)
repo.invalidateAll()
```

### 2. SuspendedJdbcRedissonRepository (코루틴) 구현

`AbstractSuspendedJdbcRedissonRepository`를 상속하여 코루틴 방식의 캐시 Repository를 구현합니다.

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

// 사용 (suspend 함수)
val repo = SuspendedUserRedissonRepository(redissonClient, RedisCacheConfig.readOnly())

val user = repo.get(1L)                          // 캐시 조회 (suspend)
val fresh = repo.findByIdFromDb(1L)              // DB 직접 조회 (suspend)
val all = repo.findAll(limit = 100)              // DB 조회 후 캐시 저장 (suspend)
repo.put(user)                                    // 캐시 저장 (suspend)
repo.putAll(users)                               // 일괄 캐시 저장 (suspend)
repo.invalidate(1L)                              // 캐시 무효화 (suspend)
repo.invalidateAll()                             // 전체 캐시 무효화 (suspend)
repo.invalidateByPattern("user:*")              // 패턴으로 무효화 (suspend)
```

### 3. 캐시 패턴 설정

```kotlin
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig

// Read-Through Only (기본)
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

## 캐시 패턴

### Read-Through

```
요청 → 캐시 조회 → (히트) → 반환
                 → (미스) → DB 조회 → 캐시 저장 → 반환
```

### Write-Through

```
저장 요청 → 캐시 저장 → DB 저장 (동기) → 완료
```

### Write-Behind

```
저장 요청 → 캐시 저장 → 완료
                      → (비동기) DB 저장
```

## JdbcRedissonRepository 주요 메서드

| 메서드                                    | 설명                    |
|----------------------------------------|----------------------|
| `get(id)`                              | 캐시에서 엔티티 조회           |
| `getAll(ids, batchSize)`               | 캐시에서 여러 엔티티 일괄 조회     |
| `findByIdFromDb(id)`                   | DB에서 직접 조회            |
| `findAllFromDb(ids)`                   | DB에서 여러 엔티티 직접 조회     |
| `findAll(limit, offset, sortBy, where)`| DB 조회 후 캐시 저장         |
| `put(entity)`                          | 캐시에 저장               |
| `putAll(entities, batchSize)`          | 캐시에 일괄 저장            |
| `invalidate(ids)`                      | 캐시에서 제거              |
| `invalidateAll()`                      | 캐시 전체 비우기            |
| `invalidateByPattern(pattern, count)`  | 패턴에 맞는 키 캐시 제거       |

## 주요 파일/클래스 목록

### Repository (repository/)

| 파일                                                   | 설명                               |
|------------------------------------------------------|----------------------------------|
| `JdbcRedissonRepository.kt`                          | 동기식 캐시 Repository 인터페이스          |
| `AbstractJdbcRedissonRepository.kt`                  | 동기식 캐시 Repository 추상 클래스         |
| `SuspendedJdbcRedissonRepository.kt`                 | 코루틴 캐시 Repository 인터페이스          |
| `AbstractSuspendedJdbcRedissonRepository.kt`         | 코루틴 캐시 Repository 추상 클래스         |
| `ExposedCacheRepository.kt`                          | (Deprecated) 구 동기식 Repository 인터페이스 |
| `AbstractExposedCacheRepository.kt`                  | (Deprecated) 구 동기식 추상 클래스        |
| `SuspendedExposedCacheRepository.kt`                 | (Deprecated) 구 코루틴 Repository 인터페이스 |
| `AbstractSuspendedExposedCacheRepository.kt`         | (Deprecated) 구 코루틴 추상 클래스        |

### Map (map/)

| 파일                                     | 설명                     |
|----------------------------------------|------------------------|
| `EntityMapLoader.kt`                   | 동기식 MapLoader 인터페이스     |
| `EntityMapWriter.kt`                   | 동기식 MapWriter 인터페이스     |
| `ExposedEntityMapLoader.kt`            | Exposed JDBC 기반 MapLoader |
| `ExposedEntityMapWriter.kt`            | Exposed JDBC 기반 MapWriter |
| `SuspendedEntityMapLoader.kt`          | 코루틴 MapLoader 인터페이스     |
| `SuspendedEntityMapWriter.kt`          | 코루틴 MapWriter 인터페이스     |
| `SuspendedExposedEntityMapLoader.kt`   | 코루틴 MapLoader 구현체       |
| `SuspendedExposedEntityMapWriter.kt`   | 코루틴 MapWriter 구현체       |

## 테스트

```bash
./gradlew :bluetape4k-exposed-jdbc-redisson:test
```

## 참고

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Redisson](https://github.com/redisson/redisson)
- [Redisson RMap](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RMap.html)
- [bluetape4k-exposed-jdbc](../exposed-jdbc)
