# bluetape4k-cache

다양한 캐시 라이브러리를 Kotlin DSL 스타일로 쉽게 사용할 수 있도록 지원하는 라이브러리입니다.

## 개요

`bluetape4k-cache`는 JCache(JSR-107) 표준을 기반으로 여러 캐시 제공자(Caffeine, Ehcache, Cache2k, Redis)를 통일된 인터페이스로 사용할 수 있게 해주며, 2-Tier 캐시(Near Cache) 패턴과 함수 메모이제이션(Memorizer) 기능을 제공합니다.

### 주요 특징

- **JCache(JSR-107) 지원**: Caffeine, Ehcache, Cache2k, Redis(Redisson) 지원
- **Near Cache(2-Tier)**: 로컬 캐시 + 원격 캐시 조합으로 고성능 분산 캐싱
- **Memorizer 패턴**: 함수 실행 결과 자동 캐싱
- **Coroutine 지원**: 모든 기능의 suspend 함수 버전 제공
- **Spring Cache 연동**: Spring Boot 캐시 추상화와 통합

## 설치

### Gradle

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache:${bluetape4kVersion}")
}
```

### Maven

```xml

<dependency>
    <groupId>io.bluetape4k</groupId>
    <artifactId>bluetape4k-cache</artifactId>
    <version>${bluetape4kVersion}</version>
</dependency>
```

## 사용법

### 1. JCache 기본 사용

#### 캐시 생성 및 조회

```kotlin
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.getOrCreate

// Caffeine 캐시 생성
val caffeineCache = JCaching.Caffeine.getOrCreate<String, User>("users")

// Ehcache 캐시 생성
val ehcache = JCaching.EhCache.getOrCreate<String, User>("users")

// Redis 캐시 생성
val redisCache = JCaching.Redisson.getOrCreate<String, User>("users", redissonClient)
```

#### 캐시 설정

```kotlin
import io.bluetape4k.cache.jcache.jcacheConfiguration
import javax.cache.expiry.TouchedExpiryPolicy
import javax.cache.expiry.Duration

val config = jcacheConfiguration<String, User> {
    setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(Duration.ONE_HOUR))
    isStatisticsEnabled = true
    isManagementEnabled = true
}

val cache = cacheManager.getOrCreate("users", config)
```

#### 캐시 사용

```kotlin
import io.bluetape4k.cache.jcache.getOrPut

// 값 저장
cache.put("user-123", user)

// 값 조회
val user = cache.get("user-123")

// 없으면 생성 (getOrPut)
val user = cache.getOrPut("user-123") {
    userRepository.findById("user-123")
}
```

### 2. Near Cache (2-Tier 캐시)

로컬 캐시(Front)와 원격 캐시(Back)를 조합하여 빠른 읽기 성능과 데이터 일관성을 동시에 제공합니다.

```kotlin
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig

// Redis를 Back Cache로 사용하는 NearCache 생성
val nearCache = NearCache(
    nearCacheCfg = NearCacheConfig(
        frontCacheName = "local-users",
        isSynchronous = false,  // 비동기 모드로 백 캐시 갱신
        checkExpiryPeriod = 30_000L  // 30초마다 만료 검사
    ),
    backCache = redisCache
)

// 사용 - Front Cache에서 먼저 조회, 없으면 Back Cache에서 로드
val user = nearCache.get("user-123")

// 저장 - Front와 Back Cache에 동시 저장
nearCache.put("user-123", user)
```

#### NearCache 설정 상세

```kotlin
val config = NearCacheConfig<String, User>(
    // Front Cache 관리자 (기본: Caffeine)
    cacheManagerFactory = CaffeineCacheManagerFactory,

    // Front Cache 이름
    frontCacheName = "my-local-cache",

    // Front Cache 설정 (만료 시간 등)
    frontCacheConfiguration = jcacheConfiguration {
        setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES))
    },

    // 동기화 모드 (true: 동기, false: 비동기)
    isSynchronous = false,

    // Back Cache 만료 검사 주기 (ms)
    checkExpiryPeriod = 30_000L,

    // 원격 동기화 타임아웃 (ms)
    syncRemoteTimeout = 500L
)
```

### 3. Memorizer (함수 결과 캐싱)

#### 기본 Memorizer

```kotlin
import io.bluetape4k.cache.memorizer.inmemory.memorizer

// 팩토리얼 함수 메모이제이션
val factorial = { n: Int ->
    if (n <= 1) 1 else n * factorial(n - 1)
}.memorizer()

// 첫 실행 시 계산
println(factorial(5))  // 120 (계산)

// 캐시된 값 반환
println(factorial(5))  // 120 (캐시 조회)
```

#### Caffeine 기반 Memorizer

```kotlin
import io.bluetape4k.cache.memorizer.caffeine.caffeineMemorizer

val userCache = { userId: String ->
    userRepository.findById(userId)  // DB 조회
}.caffeineMemorizer {
    maximumSize(1000)
    expireAfterWrite(Duration.ofMinutes(10))
}

val user = userCache("user-123")  // DB 조회 또는 캐시 반환
```

#### Coroutine 지원 (SuspendMemorizer)

```kotlin
import io.bluetape4k.cache.memorizer.caffeine.suspendCaffeineMemorizer

val fetchUser = suspendCaffeineMemorizer { userId: String ->
    userRepository.findById(userId)  // suspend 함수
}

// 코루틴에서 사용
lifecycleScope.launch {
    val user = fetchUser("user-123")
}
```

### 4. Coroutine 지원 캐시 (SuspendCache)

```kotlin
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache

val suspendCache = CaffeineSuspendCache<String, User>()

// 코루틴에서 사용
suspendCache.put("user-123", user)
val user = suspendCache.get("user-123")
```

### 5. Spring Cache 연동

```kotlin
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        // NearCache를 Spring CacheManager로 노출
        return NearCachingProvider(cacheManager, nearCacheConfig)
            .createCacheManager()
    }
}

@Service
class UserService(private val userRepository: UserRepository) {

    @Cacheable("users")
    fun getUser(id: String): User {
        return userRepository.findById(id)
    }
}
```

## 지원 캐시 제공자

| 제공자      | 로컬/원격 | 특징                  | Maven Coordinate                       |
|----------|-------|---------------------|----------------------------------------|
| Caffeine | 로컬    | 고성능, W-TinyLFU 알고리즘 | `com.github.ben-manes.caffeine:jcache` |
| Ehcache  | 로컬    | 오프힙 저장, 대용량 데이터     | `org.ehcache:ehcache`                  |
| Cache2k  | 로컬    | 초고성능, 낮은 지연시간       | `org.cache2k:cache2k-jcache`           |
| Redisson | 원격    | Redis 기반, 분산 락 지원   | `org.redisson:redisson`                |

## 캐시 전략 비교

### 단일 로컬 캐시 (Caffeine/Ehcache)

- **적합한场景**: 단일 인스턴스 애플리케이션
- **장점**: 최고의 성능, 간단한 설정
- **단점**: 분산 환경에서 데이터 불일치

### 단일 원격 캐시 (Redis)

- **적합한场景**: 분산 환경, 데이터 공유 필요
- **장점**: 데이터 일관성, 영속성
- **단점**: 네트워크 지연

### Near Cache (2-Tier)

- **적합한场景**: 고성능이 필요한 분산 환경
- **장점**: 로컬 캐시 성능 + 원격 캐시 일관성
- **단점**: 복잡한 설정, 메모리 사용 증가

## 성능 팁

1. **Near Cache 사용 시**: Front Cache의 만료 시간을 Back Cache보다 짧게 설정
2. **Memorizer 사용 시**: 적절한 maximumSize 설정으로 메모리 누수 방지
3. **비동기 모드**: 데이터 일관성이 덜 중요한 경우 비동기 모드로 성능 향상
4. **통계 활성화**: 운영 환경에서 캐시 적중률 모니터링

## 테스트

```bash
# 모든 테스트 실행
./gradlew :bluetape4k-cache:test

# 특정 테스트 실행
./gradlew :bluetape4k-cache:test --tests "NearCacheTest"
./gradlew :bluetape4k-cache:test --tests "CaffeineMemorizerTest"
```

## 참고

- [JCache (JSR-107) Specification](https://www.jcp.org/en/jsr/detail?id=107)
- [Caffeine Documentation](https://github.com/ben-manes/caffeine/wiki)
- [Ehcache Documentation](https://www.ehcache.org/documentation/)
- [Redisson Documentation](https://redisson.org/documentation.html)

## 라이선스

Apache License 2.0
