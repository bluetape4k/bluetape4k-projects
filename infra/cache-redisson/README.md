# bluetape4k-cache-redisson

`bluetape4k-cache-redisson`은 Redisson 기반 JCache Provider와 Coroutines 캐시 구현을 제공합니다.

핵심 제공 요소:
- Redisson `javax.cache.spi.CachingProvider`
- `RedissonSuspendCache`

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-redisson:${bluetape4kVersion}")
}
```

## 사용 예시

```kotlin
import io.bluetape4k.cache.jcache.coroutines.RedissonSuspendCache
import io.bluetape4k.cache.jcache.jcacheConfiguration

val config = jcacheConfiguration<String, Any> { }
val suspendCache = RedissonSuspendCache("redis-cache", redissonClient, config)
```

## 참고

- Redisson Near 전용 구성은 `bluetape4k-cache-redisson-near` 모듈을 사용하세요.
- Spring에서 Redisson Near CachingProvider를 직접 쓰려면 `cache-redisson-near`의 Provider를 지정하면 됩니다.
