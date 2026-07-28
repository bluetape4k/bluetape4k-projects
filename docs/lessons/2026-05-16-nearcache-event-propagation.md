# Near-Cache Event Propagation: 근본 원인과 backend별 판정

**이슈**: #490
**브랜치**: fix/nearcache-event-propagation
**날짜**: 2026-05-16

## 근본 원인

`AbstractSuspendNearJCacheTest`는 `SuspendNearJCache` **internal constructor**를 직접 호출해
`suspendNearJCache1`/`2`를 생성했다:

```kotlin
// BEFORE (broken) — listener 등록을 우회
protected val suspendNearJCache1 by lazy {
    SuspendNearJCache(frontCoCache1, backSuspendJCache)
}
```

`SuspendNearJCache`에는 `internal constructor(frontCache, backCache)`와 동일한 parameter type을 가진
companion `operator fun invoke(frontCache, backCache)`가 있다. testFixtures는 main source와
**같은 module**에 있기 때문에 `internal` constructor가 보이고, Kotlin은
`SuspendNearJCache(front, back)`를 companion `invoke`가 아니라 **constructor**로 해석한다.
back cache에 `SuspendJCacheEntryEventListener`를 등록하는 유일한 경로는 companion `invoke`였으므로
event가 전파되지 않았고, 모든 cross-cache propagation assertion이 timeout 되었다.

Sync `AbstractNearJCacheTest`는 영향을 받지 않았다. 이 test는 `NearJCache(nearCacheCfg, backCache)`를
호출하고, 이는 constructor와 parameter type이 다른 companion `invoke(nearCacheCfg, backCache)`로
mapping되므로 ambiguity가 없었다.

## 수정

Test fixture에 `open fun createSuspendNearJCache(front, back)` hook을 추가하고, 기본 body는
명시적 companion call인 `SuspendNearJCache.invoke(front, back)`로 두었다:

```kotlin
// AFTER (correct) — 명시적인 companion invoke가 listener를 등록
protected open fun createSuspendNearJCache(
    front: SuspendJCache<String, Any>,
    back: SuspendJCache<String, Any>,
): SuspendNearJCache<String, Any> = SuspendNearJCache.invoke(front, back)

protected val suspendNearJCache1 by lazy { createSuspendNearJCache(frontCoCache1, backSuspendJCache) }
protected val suspendNearJCache2 by lazy { createSuspendNearJCache(frontCoCache2, backSuspendJCache) }
```

이 `open` hook 덕분에 Hazelcast처럼 listener registration이 구조적으로 불가능한 backend는
`SuspendNearJCache.withoutListener(front, back)`로 override할 수 있다.

## Backend별 판정

### Lettuce (cache-lettuce)
**Verdict A — re-enabled.**  
`LettuceJCache.dispatchEvent()`는 in-process 방식이다(listener의 `ConcurrentHashMap`을 mutation 뒤
동기 호출). Fixture bug가 수정되자 test method 19개가 모두 통과했다. `@Disabled`를 제거했다.

### Redisson (cache-redisson)
**Verdict A — re-enabled.**  
Fixture 수정 후 모든 test가 통과했다. `SuspendNearJCache` 구현은 이미 explicit per-key remove로
Redisson의 `removeAll`/`replace` event gap을 우회하고 있었다.
기존 `@Disabled("버그가 많아 일단 테스트에서 제외한다.")`는 stale이었다. `@Disabled`를 제거했다.

### Hazelcast sync (cache-hazelcast — HazelcastNearJCacheTest)
**Verdict B — 정확한 이유를 남기고 disabled 유지.**
Hazelcast는 `MutableCacheEntryListenerConfiguration`을 Java serialization으로 cluster-distribute한다.
`JCacheEntryEventListener`는 serializable이 아닌 front `JCache`(Caffeine) reference를 가진다.
Registration은 `HazelcastSerializationException: NotSerializableException`을 던진다.
`Tracked: #490`와 함께 `@Disabled` message를 갱신했다.

### Hazelcast suspend (cache-hazelcast — HazelcastSuspendNearJCacheTest)
**Verdict B — 정확한 이유를 남기고 disabled 유지.**
Sync와 같은 구조적 제약이다. `SuspendJCacheEntryEventListener`가 serializable이 아닌
`CaffeineSuspendJCache`를 capture한다. Registration은
`HazelcastSerializationException: NotSerializableException(CaffeineSuspendJCache)`을 던진다.
`Tracked: #490`와 함께 `@Disabled` message를 갱신했다.

## 교훈: Kotlin Companion Invoke와 Internal Constructor Ambiguity

class가 `internal constructor(A, B)`와 동일한 parameter type의 companion
`operator fun invoke(A, B)`를 동시에 가지면, 같은 module의 `ClassName(a, b)` 호출은 companion
`invoke`가 아니라 **constructor**로 해석된다. module 밖 visibility만 보고 예상하면 반대로
판단하기 쉽다.

Companion invoke가 의도한 entry point이고 constructor를 우회해야 한다면
**항상 `ClassName.invoke(...)` 또는 `ClassName.Companion.invoke(...)`를 명시적으로 사용한다.**
또는 constructor를 `private`으로 만들어 ambiguity를 제거한다.

## 변경 파일

| 파일 | 변경 |
|---|---|
| `cache/cache-core/src/testFixtures/.../AbstractSuspendNearJCacheTest.kt` | `createSuspendNearJCache` hook 추가, 기본 구현은 `SuspendNearJCache.invoke(...)` 호출 |
| `cache/cache-lettuce/src/test/.../LettuceSuspendNearJCacheTest.kt` | `@Disabled` 제거 |
| `cache/cache-redisson/src/test/.../RedissonSuspendNearJCacheTest.kt` | `@Disabled` 제거 |
| `cache/cache-hazelcast/src/test/.../HazelcastNearJCacheTest.kt` | 정확한 이유와 `Tracked: #490`로 `@Disabled` 갱신 |
| `cache/cache-hazelcast/src/test/.../HazelcastSuspendNearJCacheTest.kt` | 정확한 이유와 `Tracked: #490`로 `@Disabled` 갱신 |

## 테스트 결과

- `bluetape4k-cache-core:test --tests "*NearJCache*"` → BUILD SUCCESSFUL
- `bluetape4k-cache-lettuce:test --tests "*NearJCache*"` → BUILD SUCCESSFUL (was failing)
- `bluetape4k-cache-redisson:test --tests "*NearJCache*"` → BUILD SUCCESSFUL (was disabled)
- `bluetape4k-cache-hazelcast:test --tests "*NearJCache*"` → BUILD SUCCESSFUL (Hazelcast tests skipped per @Disabled)
