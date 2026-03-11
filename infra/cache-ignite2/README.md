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
- **`IgniteMemoizer<K,V>`**: `ClientCache` 기반 함수 결과 메모이제이션 (sync, `Memoizer` 인터페이스)
- **`AsyncIgniteMemoizer<K,V>`**: `CompletableFuture` 기반 비동기 메모이제이션 (`AsyncMemoizer` 인터페이스)
- **`SuspendIgniteMemoizer<K,V>`**: `Dispatchers.IO` 기반 코루틴 메모이제이션 (`SuspendMemoizer` 인터페이스)

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

### 5. IgniteMemoizer — 함수 결과 Ignite 캐싱

```kotlin
import io.bluetape4k.cache.memoizer.ignite.memoizer
import io.bluetape4k.cache.memoizer.ignite.asyncMemoizer
import io.bluetape4k.cache.memoizer.ignite.suspendMemoizer

val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")

// 동기 메모이저
val memoizer = cache.memoizer { key -> key * key }
val result1 = memoizer(5)   // 25 — 계산 후 Ignite에 저장
val result2 = memoizer(5)   // 25 — Ignite에서 반환

// 비동기 메모이저 (IO 스레드 풀 사용)
val asyncMemoizer = cache.asyncMemoizer { key -> key * key }
val asyncResult = asyncMemoizer(5).get()

// 코루틴 메모이저 (Dispatchers.IO 사용)
val suspendMemoizer = cache.suspendMemoizer { key -> computeExpensive(key) }
val suspendResult = suspendMemoizer(5)
```

## Factory (IgniteCaches)

`IgniteCaches` object를 사용하면 JCache, SuspendCache, NearCache, SuspendNearCache를 간편하게 생성할 수 있습니다.

```kotlin
// JCache 생성
val jcache = IgniteCaches.jcache<String, String>("my-cache")

// Thin Client 기반 SuspendCache
val clientCache = igniteClient.getOrCreateCache<String, String>("my-cache")
val suspendCache = IgniteCaches.clientSuspendCache(clientCache)

// Caffeine(front) + Ignite(back) NearCache
val backCache = IgniteCaches.jcache<String, String>("my-back-cache")
val near = IgniteCaches.nearCache<String, String>(backCache)

// SuspendNearCache (Back Cache 이름으로 생성)
val suspendNear = IgniteCaches.suspendNearCache<String, String>("my-back-cache", igniteClient)
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
