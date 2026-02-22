# Module bluetape4k-exposed-redisson

Exposed(JDBC)와 Redisson 캐시를 결합해 캐시 연동 패턴을 구성하는 모듈입니다.

## 개요

`bluetape4k-exposed-redisson`은 JetBrains Exposed ORM과 [Redisson](https://github.com/redisson/redisson) Redis 클라이언트를 통합하여, 데이터베이스 조회 결과를 Redis에 캐싱하는 패턴을 쉽게 구현할 수 있도록 지원합니다.

### 주요 기능

- **MapLoader/MapWriter 지원**: Redisson 캐시 적재/저장 연동
- **Repository 추상화**: 캐시 + DB 접근 공통 패턴
- **동기/코루틴 구현 제공**: 운영 환경에 맞는 방식 선택
- **Read-Through/Write-Through**: 캐시 미스 시 자동 DB 조회

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-redisson:${version}")
    implementation("org.redisson:redisson:3.37.0")
}
```

## 기본 사용법

### 1. Cache Repository 구현

```kotlin
import io.bluetape4k.exposed.redisson.repository.ExposedCacheRepository
import io.bluetape4k.exposed.core.HasIdentifier
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.redisson.api.RMap
import org.redisson.Redisson

// 엔티티 (Serializable 필수)
data class User(
    override val id: Long,
    val name: String,
    val email: String
): HasIdentifier<Long>, Serializable

// Repository 구현
class UserRepository(
    redisson: RedissonClient
): ExposedCacheRepository<User, Long> {
    
    override val cacheName = "users"
    override val entityTable = Users
    override val cache: RMap<Long, User?> = redisson.getMap(cacheName)
    
    override fun ResultRow.toEntity(): User = User(
        id = this[Users.id].value,
        name = this[Users.name],
        email = this[Users.email]
    )
    
    override fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>
    ): List<User> = transaction {
        entityTable.selectAll()
            .where(where)
            .orderBy(sortBy, sortOrder)
            .let { query ->
                limit?.let { query.limit(it, offset ?: 0) } ?: query
            }
            .map { it.toEntity() }
    }
    
    override fun getAll(ids: Collection<Long>, batchSize: Int): List<User> {
        // 캐시에서 먼저 조회, 없으면 DB에서 조회 후 캐시에 저장
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

### 2. 캐시 사용 패턴

```kotlin
val repo = UserRepository(redisson)

// 캐시에서 조회
val user = repo.get(1L)

// 캐시에 저장
repo.put(user)

// DB에서 직접 조회 (캐시 무시)
val freshUser = repo.findByIdFromDb(1L)

// 여러 엔티티 조회
val users = repo.getAll(listOf(1L, 2L, 3L))

// 캐시 무효화
repo.invalidate(1L)
repo.invalidateAll()

// 패턴으로 무효화
repo.invalidateByPattern("user:*")
```

### 3. Coroutines 지원 (Suspend)

```kotlin
import io.bluetape4k.exposed.redisson.repository.SuspendedExposedCacheRepository

class SuspendUserRepository(
    redisson: RedissonClient
): SuspendedExposedCacheRepository<User, Long> {
    // 구현...
}

// 사용
val user = repo.get(1L)          // suspend 함수
repo.put(user)                    // suspend 함수
val users = repo.getAll(ids)      // suspend 함수
```

### 4. MapLoader/MapWriter (Redisson 연동)

```kotlin
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapWriter

// Redisson RMapCache에 Loader/Writer 설정
val mapCache: RMapCache<Long, User> = redisson.getMapCache("users")

val config = MapCacheOptions.defaults<Long, User>()
    .loader(ExposedEntityMapLoader(UserRepository(redisson)))
    .writer(ExposedEntityMapWriter(UserRepository(redisson)))

// 캐시 미스 시 자동으로 DB에서 로드
val user = mapCache.get(1L)  // DB에서 자동 조회
```

## 주요 파일/클래스 목록

### Repository (repository/)

| 파일                                           | 설명                       |
|----------------------------------------------|--------------------------|
| `ExposedCacheRepository.kt`                  | 동기식 캐시 Repository 인터페이스  |
| `AbstractExposedCacheRepository.kt`          | 동기식 캐시 Repository 추상 클래스 |
| `SuspendedExposedCacheRepository.kt`         | 코루틴 캐시 Repository 인터페이스  |
| `AbstractSuspendedExposedCacheRepository.kt` | 코루틴 캐시 Repository 추상 클래스 |

### Map (map/)

| 파일                                   | 설명                   |
|--------------------------------------|----------------------|
| `ExposedEntityMapLoader.kt`          | Exposed 기반 MapLoader |
| `ExposedEntityMapWriter.kt`          | Exposed 기반 MapWriter |
| `SuspendedExposedEntityMapLoader.kt` | 코루틴 MapLoader        |
| `SuspendedExposedEntityMapWriter.kt` | 코루틴 MapWriter        |
| `EntityMapLoader.kt`                 | 범용 MapLoader 인터페이스   |
| `EntityMapWriter.kt`                 | 범용 MapWriter 인터페이스   |

## 캐시 패턴

### Read-Through

```
요청 → 캐시 조회 → (미스) → DB 조회 → 캐시 저장 → 반환
                    (히트) → 반환
```

### Write-Through

```
저장 요청 → 캐시 저장 → DB 저장 → 완료
```

## 테스트

```bash
./gradlew :bluetape4k-exposed-redisson:test
```

## 참고

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Redisson](https://github.com/redisson/redisson)
- [Redisson RMap](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RMap.html)
