# bluetape4k-cache-local

`bluetape4k-cache-local`은 JVM 로컬 캐시 Provider를 묶어 제공하는 모듈입니다.

포함 Provider:
- Caffeine
- Cache2k
- Ehcache

또한 Local 환경에서 자주 쓰는 구현을 포함합니다.
- `CaffeineSuspendCache`
- local provider 기반 memorizer 확장

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-local:${bluetape4kVersion}")
}
```

## 제공 기능

- `JCaching.Caffeine`, `JCaching.Cache2k`, `JCaching.EhCache` 사용 가능
- 로컬 캐시 전용 테스트/샘플과 동일한 방식으로 구성 가능
- `cache-core`의 공통 API(`NearCache`, `SuspendCache`, `Memorizer`)와 결합 가능

## 사용 예시

### 1. Caffeine JCache

```kotlin
import io.bluetape4k.cache.jcache.JCaching

val cache = JCaching.Caffeine.getOrCreate<String, String>("local-users")
cache.put("u:1", "debop")
```

### 2. Cache2k JCache

```kotlin
val cache2k = JCaching.Cache2k.getOrCreate<String, String>("local-cache2k")
```

### 3. Ehcache JCache

```kotlin
val ehcache = JCaching.EhCache.getOrCreate<String, String>("local-ehcache")
```

### 4. CaffeineSuspendCache

```kotlin
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache

val suspendCache = CaffeineSuspendCache<String, String>()
```

## 언제 이 모듈을 선택하면 좋은가

- 단일 JVM 인스턴스에서 빠른 로컬 캐시가 필요한 경우
- 분산 Back Cache 없이도 캐시가 충분한 경우
- NearCache의 Front cache를 Caffeine/로컬 provider로 구성하고 싶은 경우
