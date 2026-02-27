# bluetape4k-cache-ignite-near

`bluetape4k-cache-ignite-near`는 Apache Ignite 2 Back Cache와 Near Cache 조합을 위한 모듈입니다.

핵심 제공 요소:
- `IgniteNearCache`
- `IgniteNearSuspendCache`

기본 Front는 Caffeine 기반이며, 사용자 지정 Front suspendCache를 사용할 수 있습니다.

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-ignite-near:${bluetape4kVersion}")
}
```

## CachingProvider

`META-INF/services/javax.cache.spi.CachingProvider`에 Ignite Provider가 등록됩니다.
- `org.apache.ignite.cache.CachingProvider`

## 사용 예시

### 1. NearCache 생성 (동기 API)

```kotlin
import io.bluetape4k.cache.nearcache.ignite.IgniteNearCache

val near = IgniteNearCache<String, Any>("ignite-users-near")
```

### 2. NearSuspendCache 생성 (기본 Front=Caffeine)

```kotlin
import io.bluetape4k.cache.nearcache.ignite.IgniteNearSuspendCache

val nearSuspend = IgniteNearSuspendCache<String, Any>("ignite-users-near-suspend")
```

### 3. NearSuspendCache 생성 (사용자 Front 지정)

```kotlin
val nearSuspend = IgniteNearSuspendCache(
    backCacheName = "ignite-users-near-suspend",
    frontSuspendCache = myFrontSuspendCache,
)
```

## 동작 시퀀스

### 1. Read Path (`get`, Front only)

```mermaid
sequenceDiagram
    participant A as App
    participant F as Front Cache
    A ->> F: get(key)
    F -->> A: value or null
```

### 2. Read-through (`getDeeply`, Front miss -> Back hit)

```mermaid
sequenceDiagram
    participant A as App
    participant F as Front Cache
    participant B as Back Cache (Ignite)
    A ->> F: getDeeply(key)
    F-->>A: miss
    A->>B: get(key)
    B-->>A: value
    A->>F: put(key, value)
    F-->>A: return value
```

### 3. Write-through + 전파

```mermaid
sequenceDiagram
    participant A as NearCache-A
    participant F as NearCache-A Front
    participant B as Back Cache (Ignite)
    participant C as NearCache-B Front
    A ->> F: put(key, value)
    A ->> B: sync backCache.put(key, value)
    B-->>C: cache entry event (created/updated)
    C->>C: front cache sync
```

### 4. Back 만료 -> Front 무효화

```mermaid
sequenceDiagram
    participant T as Expiry Checker / Listener
    participant B as Back Cache (Ignite)
    participant F as Front Cache
    T->>B: check key state
    B-->>T: expired or missing
    T->>F: remove(key)
```
