# bluetape4k-cache

`bluetape4k-cache`는 캐시 관련 모듈을 한 번에 묶어 쓰기 위한 Umbrella 모듈입니다.

핵심 목적:
- 기존 사용처와의 호환성 유지
- 다수 provider를 한 번에 빠르게 사용하는 환경 지원

권장:
- 신규 프로젝트에서는 필요한 모듈만 선택 의존하는 방식을 권장합니다.

## 모듈 구성

Umbrella는 아래 모듈을 포함합니다.
- `bluetape4k-cache-core`
- `bluetape4k-cache-local`
- `bluetape4k-cache-redisson`
- `bluetape4k-cache-redisson-near`
- `bluetape4k-cache-hazelcast`
- `bluetape4k-cache-hazelcast-near`
- `bluetape4k-cache-ignite`
- `bluetape4k-cache-ignite-near`

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache:${bluetape4kVersion}")
}
```

## 선택 의존 권장 예시

### 1. 로컬 캐시만 필요

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-core:${bluetape4kVersion}")
    implementation("io.bluetape4k:bluetape4k-cache-local:${bluetape4kVersion}")
}
```

### 2. Redisson Near만 필요

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-redisson-near:${bluetape4kVersion}")
}
```

### 3. Hazelcast Suspend + Near 필요

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-hazelcast:${bluetape4kVersion}")
    implementation("io.bluetape4k:bluetape4k-cache-hazelcast-near:${bluetape4kVersion}")
}
```

## CachingProvider 자동 로딩에 대한 주의

여러 모듈이 `META-INF/services/javax.cache.spi.CachingProvider`를 등록합니다.
따라서 클래스패스에 다수 provider가 공존할 때는 아래처럼 명시적으로 provider를 지정하는 것이 안전합니다.

```kotlin
import javax.cache.Caching

val provider = Caching.getCachingProvider("org.redisson.jcache.JCachingProvider")
val manager = provider.cacheManager
```

또는 Spring Boot에서는:

```properties
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.redis.RedisNearCachingProvider
```

## 빠른 시작

### 1. JCache

```kotlin
import io.bluetape4k.cache.jcache.JCaching

val cache = JCaching.Caffeine.getOrCreate<String, Any>("users")
cache.put("u:1", mapOf("name" to "debop"))
```

### 2. NearSuspendCache (Hazelcast 예시)

```kotlin
import io.bluetape4k.cache.nearcache.hazelcast.HazelcastNearSuspendCache

val near = HazelcastNearSuspendCache<String, Any>("hz-users-near")
```

### 3. NearSuspendCache (Ignite 예시)

```kotlin
import io.bluetape4k.cache.nearcache.ignite.IgniteNearSuspendCache

val near = IgniteNearSuspendCache<String, Any>("ignite-users-near")
```

### 4. NearSuspendCache (Redisson 예시)

```kotlin
import io.bluetape4k.cache.nearcache.redis.RedissonNearSuspendCache

val near = RedissonNearSuspendCache<String, Any>("redis-users-near", redissonClient)
```
