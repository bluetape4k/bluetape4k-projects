# LettuceSuspendedLoadedMap Write-Behind Drop 수정

**날짜**: 2026-05-16
**이슈**: #486
**브랜치**: `fix/lettuce-write-behind-drop`

## 근본 원인

`LettuceSuspendedLoadedMap`에는 조용히 실패하는 경로가 두 가지 있었다.

### 1. 재시도 시 `trySend` 결과 무시

`flushBatch()`가 writer 실패를 만나고 retry count가 `MAX_DEAD_LETTER_RETRY`보다 작으면
각 entry를 다음 코드로 다시 queue에 넣으려 했다:

```kotlin
writeBehindChannel?.trySend(Triple(k, v, retryCount))
```

`trySend`가 반환한 `ChannelResult`는 조용히 버려졌다. 그 순간 channel이 가득 차 있거나 닫혀
있으면(예: 다른 write burst가 channel을 채웠거나 `close()`가 호출된 경우), entry는 log도
dead-letter 기록도 없이 사라졌다.

### 2. `close()`가 caller의 `CoroutineScope`를 취소

`close()`는 마지막에 `scope.cancel()`을 호출했다. caller가 공유 scope를 넘기는 경우
(application-wide coroutine supervisor에서 흔한 패턴), write-behind consumer job뿐 아니라
그 scope에서 실행 중인 모든 coroutine이 취소되었다.

## 수정

### `trySend` 결과 확인

`writeToDeadLetter(batch: Map<K, V>)` helper를 추출했다. retry branch는 이제
`ChannelResult`를 확인한다:

```kotlin
val dropped = mutableMapOf<K, V>()
entries.forEach { (k, v, _) ->
    val result = writeBehindChannel?.trySend(Triple(k, v, retryCount))
    if (result == null || result.isFailure) {
        log.warn { "Requeue failed for key=$k (attempt $retryCount): channel full or closed" }
        dropped[k] = v
    }
}
if (dropped.isNotEmpty()) {
    writeToDeadLetter(dropped)
}
```

dead-letter 소진 경로(`retryCount >= MAX_DEAD_LETTER_RETRY`)도 HSET + LPUSH logic 중복 대신
`writeToDeadLetter`를 호출하도록 refactor했다.

### Scope ownership 수정

제공된 scope의 child인 private `ownedJob`(`SupervisorJob`)을 추가하고, 이를 기반으로
`ownedScope`를 만들었다. `writeBehindJob`은 `ownedScope`에서 launch된다.
`close()`는 `ownedJob`만 취소한다:

```kotlin
private val ownedJob = SupervisorJob(parent = scope.coroutineContext[Job])
private val ownedScope = CoroutineScope(scope.coroutineContext + ownedJob)
// ...
override fun close() {
    writeBehindChannel?.close()
    writeBehindJob?.let { job ->
        runBlocking(Dispatchers.IO) {
            withTimeout(...) { job.join() }
        }
    }
    ownedJob.cancel()   // scope.cancel()이 아님
    ...
}
```

## 테스트 범위

`LettuceSuspendedLoadedMapTest`에 새 테스트 2개를 추가했다:

- `close - 공유 scope를 취소하지 않는다` — shared scope를 전달하고 map을 닫은 뒤
  `sharedScope.isActive == true`를 검증한다.
- `write-behind - writer 실패 후 재시도 소진 시 dead-letter에 기록된다` — 항상 실패하는 writer로
  `MAX_DEAD_LETTER_RETRY` 재시도를 소진시키고 Redis dead-letter list에 key가 기록되는지 검증한다.

## 검증

```
:bluetape4k-lettuce:test
322 passing (14.1s) — BUILD SUCCESSFUL
```

## 핵심 교훈

**`trySend`의 `ChannelResult`를 버리지 않는다.** 가득 찼거나 닫힌 channel에서 `trySend`는
조용히 failure를 반환한다. 항상 결과를 확인하고 전달할 수 없는 message를 dead-letter로 보내거나
명시적으로 log에 남긴다.

**`CoroutineScope`의 owner만 그 scope를 취소해야 한다.** class가 scope parameter를 받는다면
내부에서 child job/scope를 만들고 `close()`에서 그 child만 취소해야 한다. 제공받은 scope를
취소하는 것은 공유 scope를 쓰는 모든 caller에 영향을 주는 contract 위반이다.
