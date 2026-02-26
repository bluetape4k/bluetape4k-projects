# bluetape4k-cache-ignite

`bluetape4k-cache-ignite`는 Apache Ignite 2.x 기반 JCache Provider와 Coroutines 캐시 구현을 제공합니다.

핵심 제공 요소:
- Ignite 2 `javax.cache.spi.CachingProvider`
- `Ignite2SuspendCache` (JCache 기반)
- `Ignite2ClientSuspendCache` (Thin Client 기반)

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-ignite:${bluetape4kVersion}")
}
```

## 사용 예시

### 1. JCache 기반 SuspendCache

```kotlin
import io.bluetape4k.cache.jcache.coroutines.Ignite2SuspendCache

val suspendCache = Ignite2SuspendCache<String, Any>("ignite-cache")
```

### 2. Thin Client 기반 SuspendCache

```kotlin
import io.bluetape4k.cache.jcache.coroutines.Ignite2ClientSuspendCache

val suspendCache = Ignite2ClientSuspendCache(clientCache)
```

## 참고

- Ignite Near 조합은 `bluetape4k-cache-ignite-near` 모듈을 사용하세요.
