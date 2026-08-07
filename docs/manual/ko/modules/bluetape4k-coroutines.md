---
manualId: bluetape4k-coroutines
title: "Coroutine과 Flow 확장"
description: lifecycle을 가진 coroutine scope, Deferred helper, Flow operator, subject, structured concurrency bridge를 제공합니다.
kind: library
group: concurrency
learningOrder: 200
---

# Coroutine과 Flow 확장

## 해결하는 문제 {#problem}

Kotlin coroutine이 primitive를 제공해도 backend에서는 scope ownership, `Deferred` 조정, Flow window와 parallel map, multicast subject, Java structured concurrency bridge를 반복해서 작성합니다. 같은 코드를 여러 곳에서 만들면 cancellation과 shutdown 동작이 달라집니다. `bluetape4k-coroutines`는 저장소에서 공통으로 쓰는 계약을 모읍니다.

![bluetape4k-coroutines 매뉴얼 의사결정 지도](../../assets/coroutines/module-foundation.svg)

## 사용 시점 {#when-to-use}

필요한 operator나 lifecycle abstraction이 이 모듈에 있고 cancellation 계약이 caller와 맞을 때 사용합니다. `coroutineScope`, `async`, 표준 Flow operator만으로 충분하면 표준 API를 우선합니다. request가 끝날 때 작업도 끝나야 한다면 caller scope를 사용해야지, request마다 장기 scope를 새로 만들면 안 됩니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
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

## 무엇을 먼저 선택할까 {#decision-guide}

| 요구 사항 | 기본 선택 | bluetape4k helper를 선택할 때 |
| --- | --- | --- |
| 한 request 안에서 2~3개 suspend 호출 조합 | caller의 `coroutineScope`와 `async` | 여러 곳에서 같은 winner/zip 규칙을 반복하면 `DeferredSupport`를 사용합니다. |
| 입력 순서를 유지하는 비동기 Flow 변환 | `map` 또는 `flow.async` | 요소 계산은 겹치되 emit 순서는 유지해야 하면 `flow.async`가 맞습니다. |
| 결과 순서보다 처리량이 중요한 bounded 변환 | `mapParallel` | downstream capacity를 근거로 `parallelism`을 제한할 수 있을 때 사용합니다. |
| component가 dispatcher와 scope를 함께 소유 | framework lifecycle scope | dispatcher까지 직접 만들었다면 `CloseableCoroutineScope` 구현을 lifecycle에서 닫습니다. |
| 가장 먼저 끝난 replica 결과 선택 | `awaitAny` | 나머지 작업도 취소해야 하면 `awaitAnyAndCancelOthers`를 사용합니다. 둘 다 첫 성공이 아니라 첫 완료를 선택합니다. |
| 완료/오류 terminal event가 있는 hot stream | `StateFlow`/`SharedFlow` | 명시적인 `complete()` 또는 `emitError()` 계약이 필요할 때 subject를 선택합니다. |
| JDK structured task policy 연결 | `coroutineScope`/`supervisorScope` | virtual-thread task scope와 first-success/fail-fast 정책을 직접 연결할 때 structured helper를 사용합니다. |

## 실전 레시피 {#recipes}

### 1. request lifecycle은 caller scope에 유지하기

```kotlin
suspend fun loadDashboard(userId: String): Dashboard = coroutineScope {
    val profile = async { profileClient.load(userId) }
    val notices = async { noticeClient.load(userId) }
    Dashboard(profile.await(), notices.await())
}
```

별도 application scope로 child를 탈출시키지 않으면 caller 취소가 두 요청에 함께 전달됩니다. component 자체가 background job을 소유하는 경우에만 close 가능한 scope를 만들고, owner의 `close()`와 묶습니다.

### 2. 순서 보장과 처리량을 구분하기

```kotlin
val ordered = ids.asFlow()
    .async { id -> client.load(id) }
    .toList()                      // 입력 순서 유지

val throughput = ids.asFlow()
    .mapParallel(parallelism = 8) { id -> client.load(id) }
    .toList()                      // 완료 순서가 달라질 수 있음
```

`mapParallel(parallelism = 1)`은 일반 `map` 경로를 사용하고 순서를 유지합니다. 2 이상이면 `flatMapMerge` 기반이므로 결과 순서를 API 계약으로 삼지 않습니다.

### 3. 가장 빠른 replica를 선택하고 loser 취소하기

```kotlin
suspend fun <T> fastestReplica(
    requests: List<Deferred<T>>,
): T = requests.awaitAnyAndCancelOthers()
```

winner는 가장 먼저 **완료**한 작업입니다. 첫 작업이 실패하거나 취소되면 그 결과를 그대로 전달하고 나머지를 취소합니다. 첫 **성공**이 필요하면 `firstSuccessTaskScope`를 사용해야 합니다.

### 4. PublishSubject의 첫 event를 잃지 않기

```kotlin
val subject = PublishSubject<Event>()
coroutineScope {
    val collector = launch { subject.collect(::handle) }
    subject.awaitCollector()
    subject.emit(Event.Started)
    subject.complete()
    collector.join()
}
```

`PublishSubject`는 과거 값을 replay하지 않으므로 producer가 먼저 emit하면 첫 event가 사라질 수 있습니다. 시작 순서가 중요한 test와 adapter에서는 `awaitCollector()`로 collector 등록을 확인합니다. `complete()`나 `emitError()` 이후의 terminal 호출은 무시됩니다.

## 권장 패턴 {#patterns}

structured scope는 가장 좁은 lifecycle boundary에 둡니다. broad exception 처리 전에 `CancellationException`을 다시 던집니다. parallel Flow의 concurrency는 CPU 수만 보지 말고 downstream service capacity에 맞춥니다. blocking bridge 대신 `await()`를 사용합니다. scope를 소유하는 type은 `Closeable`로 만들고 application shutdown에 연결합니다.

## 연동 {#integrations}

`bluetape4k-core`, virtual-thread dispatcher, Reactor context helper, Java `CompletableFuture`와 stream, Kotlin Flow와 연동합니다. Reactor 전용 helper는 `coroutines.reactor` package에 있어 Reactor를 사용하지 않는 코드가 해당 모델에 묶이지 않게 합니다.

## 설정 {#configuration}

중앙 property file은 없습니다. dispatcher, parallelism, buffer size, timeout/deadline, subject capacity를 호출 지점이나 component owner에서 설정합니다. module build의 benchmark 설정은 저장소 측정용이며 application runtime default가 아닙니다.

## 실패 동작 {#failures}

`await()`는 계산의 failure와 cancellation을 그대로 전달합니다. fail-fast task scope는 첫 failure 뒤 남은 작업을 취소하고, first-success scope는 모든 branch가 실패하면 실패합니다. supervised scope는 partial result를 유지합니다. deadline join은 `TimeoutException`을 던집니다. 제한된 dispatcher에서 blocking access를 사용하면 deadlock이나 starvation이 생길 수 있어 `DeferredValue.value`가 deprecated되었습니다.

### Cancellation 점검표

1. `catch (e: Exception)`보다 먼저 `CancellationException`을 다시 던지는지 확인합니다.
2. request child가 application scope로 빠져 caller cancellation을 잃지 않는지 확인합니다.
3. 직접 만든 dispatcher/scope/channel의 owner와 close 시점을 문서화합니다.
4. timeout이 remote 작업까지 취소하는지, 단지 local wait만 중단하는지 구분합니다.
5. child failure를 관찰하는 test는 `supervisorScope`로 test body cancellation을 분리합니다.

### 문제 진단표

| 증상 | 먼저 확인할 것 | 대응 |
| --- | --- | --- |
| caller가 끝난 뒤에도 계산이 계속됨 | `DeferredValue` 또는 직접 만든 scope가 닫히지 않았는지 | `await()`를 우선 사용하고 owner lifecycle에서 `close()`합니다. |
| `mapParallel` 결과 순서가 바뀜 | `parallelism`이 2 이상인지 | 순서가 계약이면 `flow.async` 또는 일반 `map`을 사용합니다. |
| timeout 뒤에도 remote 호출이 남음 | client가 coroutine cancellation을 실제 I/O 취소로 연결하는지 | client cancellation 계약을 확인하고 별도 deadline/idempotency 정책을 둡니다. |
| subject의 첫 event가 사라짐 | collector 등록 전에 `emit`했는지 | `awaitCollector()`를 사용하거나 replay가 필요한 subject/`StateFlow`를 선택합니다. |
| 한 child failure가 sibling 전체를 취소함 | fail-fast가 의도한 정책인지 | partial result가 필요하면 `supervisorScope` 또는 `supervisedTaskScope`를 사용합니다. |
| shutdown 후 thread가 남음 | `ThreadPoolCoroutineScope`처럼 dispatcher를 소유한 scope인지 | component `close()`를 application shutdown에 연결합니다. |

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

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### coroutines 모듈 구성 개요 다이어그램

[![coroutines 모듈 구성 개요 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-01.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### Coroutines 클래스 구조 다이어그램

[![Coroutines 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-02.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 1. Flow 확장 함수 카테고리 개요 다이어그램

[![1. Flow 확장 함수 카테고리 개요 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-03.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### DeferredValue eager async 사용 흐름 다이어그램

[![DeferredValue eager async 사용 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-01.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 2. chunked(n) 고정 크기 청크 생성 흐름 다이어그램

[![2. chunked(n) 고정 크기 청크 생성 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-02.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 3. windowed(size, step) 슬라이딩 윈도우 흐름 다이어그램

[![3. windowed(size, step) 슬라이딩 윈도우 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-03.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 4. sliding(n) 및 bufferedSliding(n) 1칸 이동 윈도우 다이어그램

[![4. sliding(n) 및 bufferedSliding(n) 1칸 이동 윈도우 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-04.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 5. mapParallel(parallelism) 병렬 변환 흐름 다이어그램

[![5. mapParallel(parallelism) 병렬 변환 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-05.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 6. concatMapEager eager 병렬 수집과 순서 보장 방출 다이어그램

[![6. concatMapEager eager 병렬 수집과 순서 보장 방출 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-06.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-06.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 7. bufferingDebounce(timeout) 디바운스 배치 흐름 다이어그램

[![7. bufferingDebounce(timeout) 디바운스 배치 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-07.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-07.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 8. throttleLeading, throttleTrailing, throttleBoth 윈도우 방출 정책 다이어그램

[![8. throttleLeading, throttleTrailing, throttleBoth 윈도우 방출 정책 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-08.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-08.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 9. takeUntil(notifier) 및 skipUntil(notifier) 게이트 제어 다이어그램

[![9. takeUntil(notifier) 및 skipUntil(notifier) 게이트 제어 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-09.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-09.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 10. merge(flows) 동시 수집과 도착 순서 병합 다이어그램

[![10. merge(flows) 동시 수집과 도착 순서 병합 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-10.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-10.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 11. pairwise() 및 zipWithNext() 인접 쌍 생성 다이어그램

[![11. pairwise() 및 zipWithNext() 인접 쌍 생성 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-11.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-11.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 12. scanWith(initial) collect 시점 seed 생성과 누적 방출 다이어그램

[![12. scanWith(initial) collect 시점 seed 생성과 누적 방출 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-12.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-12.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

### 13. AsyncFlow LazyDeferred 시작과 입력 순서 방출 다이어그램

[![13. AsyncFlow LazyDeferred 시작과 입력 순서 방출 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-13.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-13.svg)

_배포본 README: [`bluetape4k/coroutines/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README와 예제](../../../../bluetape4k/coroutines/README.ko.md)
- [`DeferredValue` lifecycle 계약](../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DeferredValue.kt)
- [Flow extension source](../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow)
- [Coroutine 테스트](../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines)
- [모듈 build와 benchmark 설정](../../../../bluetape4k/coroutines/build.gradle.kts)
