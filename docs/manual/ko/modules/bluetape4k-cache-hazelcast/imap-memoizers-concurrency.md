---
title: IMap memoizer와 동시 계산
description: 동기·CompletableFuture·coroutine memoizer의 실행 위치, JVM 내 same-key 병합과 cluster 경쟁을 설명합니다.
manualId: bluetape4k-cache-hazelcast
chapterId: imap-memoizers-concurrency
---

# IMap memoizer와 동시 계산

## 세 API가 같은 저장소를 쓴다

세 memoizer는 먼저 Hazelcast `IMap`을 조회하고 miss면 evaluator를 실행한 뒤 `putIfAbsent`로 결과를 저장합니다. 다른 JVM이 먼저 값을 넣었다면 그 값을 winner로 반환합니다.

```kotlin
val scores: IMap<String, Score> = hazelcast.getMap("scores-v1")

val sync = scores.memoizer { key -> calculateScore(key) }
val future = scores.asyncMemoizer { key -> calculateScore(key) }
val suspend = scores.suspendMemoizer { key -> calculateScoreSuspend(key) }
```

동기 evaluator는 호출 thread에서 실행됩니다. async evaluator는 `CompletableFuture.supplyAsync`의 기본 executor에서 실행되고, suspend evaluator는 호출 coroutine에서 실행됩니다. CPU·blocking 성격에 맞는 실행 위치를 evaluator 쪽에서 분명히 합니다.

## single-flight 범위는 한 JVM이다

각 인스턴스는 `ConcurrentHashMap<K, ...>` 형태의 `inFlight` map으로 같은 key의 동시 요청을 하나로 합칩니다. release tests는 한 JVM에서 evaluator가 한 번만 실행되는지 검증합니다.

다른 JVM에는 별도의 `inFlight` map이 있으므로 cluster 전체 single-flight는 아닙니다. 두 node가 동시에 miss를 보면 evaluator가 둘 다 실행될 수 있고, `IMap.putIfAbsent`가 최종 결과 하나를 선택합니다. evaluator가 외부 side effect를 만든다면 중복 실행을 허용할 수 있는지 먼저 확인합니다.

## 실패 뒤에는 다음 호출이 다시 계산한다

동기·async·suspend 구현은 실패한 in-flight entry를 제거합니다. async와 suspend tests는 첫 evaluator 실패 뒤 다음 호출이 다시 계산해 성공하는 경로를 확인합니다.

```kotlin
val memoized = values.suspendMemoizer { key ->
    remoteService.load(key) // failure does not become a cached value
}
```

실패 자체는 `IMap`에 저장되지 않습니다. 계속 실패하는 hot key에는 호출마다 원본 작업이 반복될 수 있으므로 retry와 concurrency limit을 memoizer 바깥에서 설계합니다.

## clear는 진행 중 계산의 취소가 아니다

`clear`는 `IMap.clear()`를 호출합니다. async 구현은 로컬 `inFlight` map도 비우지만 이미 실행 중인 future를 취소하지는 않습니다. 동기와 suspend 구현은 진행 중 map을 명시적으로 비우지 않습니다. clear와 evaluator 완료가 겹치면 완료된 계산이 다시 값을 저장할 수 있습니다.

운영 중 schema 전환이나 강제 purge에는 새 map 이름으로 이동하는 편이 경합을 줄입니다.

## Source와 tests

- [`HazelcastMemoizer.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastMemoizer.kt)
- [`HazelcastAsyncMemoizer.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastAsyncMemoizer.kt)
- [`HazelcastSuspendMemoizer.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastSuspendMemoizer.kt)
- [`HazelcastAsyncMemoizerTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/memoizer/HazelcastAsyncMemoizerTest.kt)
- [`HazelcastSuspendMemoizerTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/memoizer/HazelcastSuspendMemoizerTest.kt)
