# Module bluetape4k-exposed-r2dbc-redisson

Exposed R2DBC와 Redisson 캐시를 결합해 읽기/쓰기 캐시 패턴을 구성하는 모듈입니다.

## 개요

`bluetape4k-exposed-r2dbc-redisson`은 Exposed R2DBC(비동기)와 [Redisson](https://github.com/redisson/redisson) Redis 클라이언트를 통합하여, 비동기 환경에서 데이터베이스 조회 결과를 Redis에 캐싱하는 패턴을 쉽게 구현할 수 있도록 지원합니다.

### 주요 기능

- **MapLoader/MapWriter 지원**: Redisson 캐시 적재/저장 연동
- **Repository 추상화**: 캐시 + DB 접근 공통 패턴
- **Async/Coroutine 지원**: R2DBC 흐름과 자연스럽게 결합
- **Read-Through/Write-Through**: 캐시 미스 시 자동 DB 조회

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-r2dbc-redisson:${version}")
    implementation("org.redisson:redisson:3.37.0")
}
```

## 기본 사용법

### 1. R2DBC Cache Repository 구현

```kotlin
import io.bluetape4k.exposed.r2dbc.redisson.repository.R2dbcCacheRepository
import io.bluetape4k.exposed.core.HasIdentifier
import org.jetbrains.exposed.v1.r2dbc.ResultRow

// 엔티티 (Serializable 필수)
data class User(
    override val id: Long,
    val name: String,
    val email: String
): HasIdentifier<Long>, Serializable

// Repository 구현
class UserR2dbcRepository(
    redisson: RedissonClient
): R2dbcCacheRepository<User, Long> {

    override val cacheName = "users"
    override val entityTable = Users
    override val cache: RMap<Long, User?> = redisson.getMap(cacheName)

    override fun ResultRow.toEntity(): User = User(
        id = this[Users.id].value,
        name = this[Users.name],
        email = this[Users.email]
    )

    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        where: () -> Op<Boolean>
    ): List<User> = suspendTransaction {
        entityTable.selectAll()
            .where(where)
            .let { query ->
                limit?.let { query.limit(it, offset ?: 0) } ?: query
            }
            .map { it.toEntity() }
    }

    override suspend fun getAll(ids: Collection<Long>, batchSize: Int): List<User> {
        val cached = cache.getAll(ids.toSet())
        val missing = ids.filter { !cached.containsKey(it) }

        if (missing.isNotEmpty()) {
            val fromDb = findAllFromDb(missing)
            putAll(fromDb)
            return cached.values.filterNotNull() + fromDb
        }
        return cached.values.filterNotNull()
    }
}
```

### 2. 캐시 사용 (Suspend)

```kotlin
val repo = UserR2dbcRepository(redisson)

// 캐시에서 조회
val user = repo.get(1L)

// 캐시에 저장
repo.put(user)

// DB에서 직접 조회
val freshUser = repo.findByIdFromDb(1L)

// 여러 엔티티 조회
val users = repo.getAll(listOf(1L, 2L, 3L))

// 캐시 무효화
repo.invalidate(1L)
repo.invalidateAll()
```

### 3. MapLoader/MapWriter (Redisson 연동)

```kotlin
import io.bluetape4k.exposed.r2dbc.redisson.map.R2dbcEntityMapLoader
import io.bluetape4k.exposed.r2dbc.redisson.map.R2dbcEntityMapWriter

val mapCache: RMapCache<Long, User> = redisson.getMapCache("users")

val config = MapCacheOptions.defaults<Long, User>()
    .loader(R2dbcEntityMapLoader(UserR2dbcRepository(redisson)))
    .writer(R2dbcEntityMapWriter(UserR2dbcRepository(redisson)))

// 캐시 미스 시 자동으로 DB에서 로드 (비동기)
val user = mapCache.get(1L)
```

## 주요 파일/클래스 목록

### Repository (repository/)

| 파일                                | 설명                         |
|-----------------------------------|----------------------------|
| `R2dbcCacheRepository.kt`         | R2DBC 캐시 Repository 인터페이스  |
| `AbstractR2dbcCacheRepository.kt` | R2DBC 캐시 Repository 추상 클래스 |

### Map (map/)

| 파일                        | 설명                 |
|---------------------------|--------------------|
| `R2dbcEntityMapLoader.kt` | R2DBC 기반 MapLoader |
| `R2dbcEntityMapWriter.kt` | R2DBC 기반 MapWriter |

## 테스트

```bash
./gradlew :bluetape4k-exposed-r2dbc-redisson:test
```

## 참고

- [JetBrains Exposed R2DBC](https://github.com/JetBrains/Exposed)
- [Redisson](https://github.com/redisson/redisson)
- [bluetape4k-exposed-r2dbc](../exposed-r2dbc/README.md)
- [bluetape4k-exposed-redisson](../exposed-redisson/README.md)
