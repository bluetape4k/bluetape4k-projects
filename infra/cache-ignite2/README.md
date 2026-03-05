# Module bluetape4k-cache-ignite2

`bluetape4k-cache-ignite2`는 Apache Ignite 2.x 기반 JCache Provider, Coroutines 캐시 구현, 그리고 **Caffeine + Ignite 2-Tier Near Cache**를 제공합니다.

> 기존 `bluetape4k-cache-ignite-near` 모듈이 이 모듈에 통합되었습니다.

## 제공 기능

- **Ignite JCache Provider** (`IgniteJCaching`)
- **Ignite Near Cache Provider** (`IgniteNearCachingProvider`)
- **`IgniteSuspendCache`**: Embedded Ignite 기반 코루틴 캐시
- **`IgniteClientSuspendCache`**: Thin Client 기반 코루틴 캐시
- **`IgniteNearCache`**: Caffeine(로컬) + Ignite(분산) 2-Tier Near Cache
- **`IgniteSuspendNearCache`**: Near Cache 코루틴 팩토리

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-ignite2:${bluetape4kVersion}")
}
```

> Apache Ignite 2.x는 Java 11+ 모듈 시스템 접근이 필요합니다. 다음 JVM 옵션을 추가하세요:
>
> ```
> --add-opens=java.base/java.nio=ALL-UNNAMED
> --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
> --add-opens=java.base/java.lang=ALL-UNNAMED
> ```

## 사용 예시

### 1. IgniteSuspendCache (Embedded Ignite 기반)

```kotlin
import io.bluetape4k.cache.jcache.IgniteSuspendCache

val suspendCache = IgniteSuspendCache<String, Any>("ignite-cache")
suspendCache.put("key", "value")
val value = suspendCache.get("key")
```

### 2. IgniteClientSuspendCache (Thin Client 기반)

```kotlin
import io.bluetape4k.cache.jcache.IgniteClientSuspendCache

val suspendCache = IgniteClientSuspendCache(
    igniteClient.getOrCreateCache("my-cache")
)
suspendCache.put("key", "value")
val value = suspendCache.get("key")
```

### 3. Ignite Near Cache (2-Tier, Thin Client 권장)

```kotlin
import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.IgniteClientSuspendCache
import io.bluetape4k.cache.nearcache.IgniteSuspendNearCache

val nearCache = IgniteSuspendNearCache<String, Any>(
    frontSuspendCache = CaffeineSuspendCache { maximumSize(10_000) },
    backSuspendCache = IgniteClientSuspendCache(igniteClient.getOrCreateCache("my-near-cache")),
)

nearCache.put("key", "value")
val value = nearCache.get("key")  // 로컬 Caffeine에서 우선 조회
```

### 4. Spring Boot 설정 (Near Cache Provider 사용)

```properties
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.IgniteNearCachingProvider
```

## CachingProvider 등록 목록

`META-INF/services/javax.cache.spi.CachingProvider`에 등록된 Provider:

```
org.apache.ignite.cache.CachingProvider
io.bluetape4k.cache.nearcache.IgniteNearCachingProvider
```

클래스패스에 여러 Provider가 공존할 때는 명시적으로 지정하세요:

```kotlin
val provider = Caching.getCachingProvider("io.bluetape4k.cache.nearcache.IgniteNearCachingProvider")
```
