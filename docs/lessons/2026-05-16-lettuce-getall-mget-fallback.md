# LettuceLoadedMap getAll MGET Fallback 수정

**날짜**: 2026-05-16
**이슈**: #485
**브랜치**: `fix/lettuce-getall-fallback`

## 근본 원인

`LettuceLoadedMap.getAll()`과 `LettuceSuspendedLoadedMap.getAll()`은 모두 다음 코드를 사용했다:

```kotlin
val values = runCatching { commands.mget(*redisKeys) }
    .onFailure { log.warn(it) { "Redis MGET 실패, loader fallback: ..." } }
    .getOrNull() ?: emptyList()
```

MGET이 실패하면 `values`는 `emptyList()`가 되었다. 이어지는 `forEachIndexed` loop가 전혀 돌지 않아
`missedKeys`가 비어 있었다. Loader는 호출되지 않았고 caller는 fetched value 대신 빈 결과를 받았다.

Log message는 "loader fallback"이라고 말했지만, code path는 그것을 불가능하게 만들었다.

## 수정

### Sync (`LettuceLoadedMap.kt`)

```kotlin
val mgetResult = runCatching { commands.mget(*redisKeys) }
    .onFailure { log.warn(it) { "Redis MGET 실패, loader fallback: ..." } }
    .getOrNull()

val missedKeys: MutableList<K>
if (mgetResult == null) {
    // MGET failed entirely — treat all requested keys as cache misses
    missedKeys = keyList.toMutableList()
} else {
    missedKeys = mutableListOf()
    mgetResult.forEachIndexed { i, kv ->
        if (kv != null && kv.hasValue()) result[keyList[i]] = kv.value
        else missedKeys.add(keyList[i])
    }
}
```

### Suspend (`LettuceSuspendedLoadedMap.kt`)

동일한 logic을 적용하고, `CancellationException`을 삼키는
`runCatching { asyncCommands.mget(...).await() }`를 명시적인 try/catch로 교체해
`CancellationException`을 rethrow한다:

```kotlin
val mgetResult = try {
    asyncCommands.mget(*redisKeys).await()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    log.warn(e) { "Redis MGET 실패, loader fallback: ..." }
    null
}
```

SETEX `runCatching` block도 같은 명시적 try/catch로 교체했다.

## 테스트 범위

`LettuceLoadedMapTest`와 `LettuceSuspendedLoadedMapTest`에 새 test를 추가했다:

- `getAll - 모든 키가 캐시 미스인 경우 loader로 모두 처리한다` — Redis에 모든 key가 없을 때
  모든 key가 loader를 거치고 결과가 반환되는지 검증한다.
- `getAll - 일부 캐시 미스 키는 loader로 Read-through한다`(suspend) — suspend test class에
  빠져 있던 partial miss scenario를 추가했다.

참고: `mgetResult == null` 경로(MGET throw)는 static analysis로 검증했다. MGET failure의
integration-level simulation은 mock 기반 unit test 또는 broken-connection fixture가 필요하며,
follow-up으로 추가할 수 있다.

## 검증

```
:bluetape4k-lettuce:test
67 passing (6.2s) — BUILD SUCCESSFUL
```

## 핵심 교훈

**`getOrNull() ?: emptyList()`는 error를 빈 결과로 조용히 바꾼다.**
"failure를 all-miss로 취급"하려는 의도라면 `keyList`에서 `missedKeys`를 명시적으로 채운다.
Fallback empty collection을 순회해 missed key를 추론하지 않는다.

**`runCatching {}`은 `suspend` 호출을 감싸면 안 된다.** `CancellationException`을 삼켜
coroutine structured concurrency를 깨뜨린다. 모든 suspend path에서는 `CancellationException`을
rethrow하는 명시적인 try/catch를 사용한다.
