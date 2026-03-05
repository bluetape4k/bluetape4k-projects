# Module bluetape4k-cache-hazelcast

`bluetape4k-cache-hazelcast`는 Hazelcast 기반 JCache Provider, Coroutines 캐시 구현, 그리고 **Caffeine + Hazelcast 2-Tier Near Cache**를 제공합니다.

> 기존 `bluetape4k-cache-hazelcast-near` 모듈이 이 모듈에 통합되었습니다.

## 제공 기능

- **Hazelcast JCache Provider** (`com.hazelcast.cache.HazelcastCachingProvider`)
- **Hazelcast Near Cache Provider** (`HazelcastNearCachingProvider`)
- **`HazelcastSuspendCache`**: JCache 기반 코루틴 캐시
- **`HazelcastNearCache`**: Caffeine(로컬) + Hazelcast(분산) 2-Tier Near Cache
- **`HazelcastNearSuspendCache`**: Near Cache 코루틴 래퍼

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-hazelcast:${bluetape4kVersion}")
}
```

## 사용 예시

### 1. HazelcastSuspendCache

```kotlin
import io.bluetape4k.cache.jcache.coroutines.HazelcastSuspendCache

val suspendCache = HazelcastSuspendCache<String, Any>("hazelcast-cache")
suspendCache.put("key", "value")
val value = suspendCache.get("key")
```

### 2. Hazelcast Near Cache (2-Tier)

```kotlin
import io.bluetape4k.cache.nearcache.hazelcast.HazelcastNearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig

val nearConfig = NearCacheConfig<String, Any>()
val nearCache = HazelcastNearCache<String, Any>("hz-near", hazelcastInstance, nearConfig)

nearCache.put("key", "value")
val value = nearCache.get("key")  // 로컬 Caffeine에서 우선 조회
```

### 3. HazelcastNearSuspendCache (코루틴)

```kotlin
import io.bluetape4k.cache.nearcache.hazelcast.coroutines.HazelcastNearSuspendCache

val nearSuspend = HazelcastNearSuspendCache<String, Any>("hz-near-suspend", hazelcastInstance)
nearSuspend.put("key", "value")
val value = nearSuspend.get("key")
```

### 4. Spring Boot 설정 (Near Cache Provider 사용)

```properties
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.hazelcast.HazelcastNearCachingProvider
```

## CachingProvider 등록 목록

`META-INF/services/javax.cache.spi.CachingProvider`에 등록된 Provider:

```
com.hazelcast.cache.HazelcastCachingProvider
io.bluetape4k.cache.nearcache.hazelcast.HazelcastNearCachingProvider
```

클래스패스에 여러 Provider가 공존할 때는 명시적으로 지정하세요:

```kotlin
val provider = Caching.getCachingProvider("io.bluetape4k.cache.nearcache.hazelcast.HazelcastNearCachingProvider")
```
