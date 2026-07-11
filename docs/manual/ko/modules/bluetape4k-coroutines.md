---
manualId: bluetape4k-coroutines
title: Coroutine과 Flow 확장
description: lifecycle을 가진 coroutine scope, Deferred helper, Flow operator, subject, structured concurrency bridge를 제공합니다.
kind: library
group: foundation
---

# Coroutine과 Flow 확장

## 해결하는 문제 {#problem}

Kotlin coroutine이 primitive를 제공해도 backend에서는 scope ownership, `Deferred` 조정, Flow window와 parallel map, multicast subject, Java structured concurrency bridge를 반복해서 작성합니다. 같은 코드를 여러 곳에서 만들면 cancellation과 shutdown 동작이 달라집니다. `bluetape4k-coroutines`는 저장소에서 공통으로 쓰는 계약을 모읍니다.

## 사용 시점 {#when-to-use}

필요한 operator나 lifecycle abstraction이 이 모듈에 있고 cancellation 계약이 caller와 맞을 때 사용합니다. `coroutineScope`, `async`, 표준 Flow operator만으로 충분하면 표준 API를 우선합니다. request가 끝날 때 작업도 끝나야 한다면 caller scope를 사용해야지, request마다 장기 scope를 새로 만들면 안 됩니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-coroutines")
}
```

core와 virtual-thread API 연동을 노출합니다. Java structured concurrency bridge는 runtime에서 호환되는 JDK 구현이 필요합니다.

## 핵심 개념 {#concepts}

`DefaultCoroutineScope`, `IoCoroutineScope`, `ThreadPoolCoroutineScope`, `VirtualThreadCoroutineScope`는 dispatcher와 job을 소유하므로 닫아야 합니다. `DeferredValue`는 생성 시 async 계산 하나를 시작하고 `await()`로 결과를 제공합니다. blocking `value` property는 coroutine 코드에서 deprecated입니다. Flow extension은 batching, range, race, backpressure policy, parallel transform, multicast subject를 다룹니다.

structured task scope는 fail-fast, first-success, supervised policy를 제공합니다. 어떤 failure가 sibling을 취소하고 어떤 result를 반환할 수 있는지는 policy가 결정합니다.

## 빠른 시작 {#quick-start}

```kotlin
import io.bluetape4k.coroutines.deferredValueOf

suspend fun loadAnswer(): Int {
    val value = deferredValueOf { 21 * 2 }
    return try {
        value.await()
    } finally {
        value.close()
    }
}
```

`DeferredValue`는 자체 `DefaultCoroutineScope`를 소유합니다. caller가 계산을 포기할 수 있다면 반드시 닫아야 합니다.

## 작업별 API {#api-by-task}

| 작업 | 시작 지점 |
| --- | --- |
| eager async 값 하나 변환 | `DeferredValue`, `map`, `flatMap` |
| plain deferred 조정 | `support.awaitAny`, `awaitAnyAndCancelOthers`, `zip` |
| Flow batch 또는 window | `chunked`, `windowed`, `bufferUntilChanged` |
| 순서를 유지한 async 처리 | `flow.async` |
| bounded parallel transform | `mapParallel` |
| state/event multicast | `BehaviorSubject`, `PublishSubject` |
| close 가능한 scope 소유 | `DefaultCoroutineScope`, `IoCoroutineScope`, `VirtualThreadCoroutineScope` |
| structured failure policy 선택 | `taskScope`, `firstSuccessTaskScope`, `supervisedTaskScope` |

## 권장 패턴 {#patterns}

structured scope는 가장 좁은 lifecycle boundary에 둡니다. broad exception 처리 전에 `CancellationException`을 다시 던집니다. parallel Flow의 concurrency는 CPU 수만 보지 말고 downstream service capacity에 맞춥니다. blocking bridge 대신 `await()`를 사용합니다. scope를 소유하는 type은 `Closeable`로 만들고 application shutdown에 연결합니다.

## 연동 {#integrations}

`bluetape4k-core`, virtual-thread dispatcher, Reactor context helper, Java `CompletableFuture`와 stream, Kotlin Flow와 연동합니다. Reactor 전용 helper는 `coroutines.reactor` package에 있어 Reactor를 사용하지 않는 코드가 해당 모델에 묶이지 않게 합니다.

## 설정 {#configuration}

중앙 property file은 없습니다. dispatcher, parallelism, buffer size, timeout/deadline, subject capacity를 호출 지점이나 component owner에서 설정합니다. module build의 benchmark 설정은 저장소 측정용이며 application runtime default가 아닙니다.

## 실패 동작 {#failures}

`await()`는 계산의 failure와 cancellation을 그대로 전달합니다. fail-fast task scope는 첫 failure 뒤 남은 작업을 취소하고, first-success scope는 모든 branch가 실패하면 실패합니다. supervised scope는 partial result를 유지합니다. deadline join은 `TimeoutException`을 던집니다. 제한된 dispatcher에서 blocking access를 사용하면 deadlock이나 starvation이 생길 수 있어 `DeferredValue.value`가 deprecated되었습니다.

## 운영 {#operations}

active job, queue와 buffer 증가, downstream latency, cancellation rate, timeout count를 관찰합니다. shutdown에서 소유한 scope와 channel을 닫습니다. parallelism을 크게 잡으면 bottleneck이 database나 remote service로 이동할 수 있으므로 capacity limit는 해당 boundary에 둡니다.

## 테스트 {#testing}

suspend 계약은 `runTest`로 검증합니다. child failure를 의도적으로 관찰하는 test는 `supervisorScope`로 test body cancellation을 분리합니다. `DeferredSupportTest`, `StructuredConcurrencyTest`, `AsyncFlowTest`, subject test와 각 Flow operator test가 대표 근거입니다.

```bash
./gradlew :bluetape4k-coroutines:test --no-configuration-cache
```

## 워크숍 {#workshops}

repository example은 Ktor와 Spring 상위 모듈을 거쳐 이 API를 사용합니다. 한 기능을 집중해서 볼 때는 `StructuredConcurrencyTest`나 Flow operator test 하나에서 failure order, cancellation, timeout, parallelism을 바꿔 실행합니다.

## 제한 사항 {#limitations}

custom Flow operator를 사용해도 cold/hot stream, buffering, cancellation을 이해해야 합니다. subject를 global하게 노출하면 ownership이 흐려집니다. virtual-thread bridge는 선택한 JDK 구현에 의존하며 모든 CPU-bound workload를 개선한다고 가정하면 안 됩니다.

## 근거 {#sources}

- [모듈 README와 예제](../../../../bluetape4k/coroutines/README.ko.md)
- [`DeferredValue` lifecycle 계약](../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DeferredValue.kt)
- [Flow extension source](../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow)
- [Coroutine 테스트](../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines)
- [모듈 build와 benchmark 설정](../../../../bluetape4k/coroutines/build.gradle.kts)
