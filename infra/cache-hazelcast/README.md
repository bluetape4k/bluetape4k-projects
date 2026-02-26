# bluetape4k-cache-hazelcast

`bluetape4k-cache-hazelcast`는 Hazelcast 기반 JCache Provider와 Coroutines 캐시 구현을 제공합니다.

핵심 제공 요소:
- Hazelcast `javax.cache.spi.CachingProvider`
- `HazelcastSuspendCache`

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-hazelcast:${bluetape4kVersion}")
}
```

## 사용 예시

```kotlin
import io.bluetape4k.cache.jcache.coroutines.HazelcastSuspendCache

val suspendCache = HazelcastSuspendCache<String, Any>("hazelcast-cache")
```

## 참고

- Hazelcast Near 조합은 `bluetape4k-cache-hazelcast-near` 모듈을 사용하세요.
- 이 모듈은 provider/suspend cache 중심이며 Near 조합 구현은 분리되어 있습니다.
