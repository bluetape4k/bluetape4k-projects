---
manualId: bluetape4k-core
title: "핵심 Kotlin 라이브러리"
description: bluetape4k 전반에서 사용하는 validation, codec, collection, range, time DSL, reflection, concurrency primitive를 제공합니다.
kind: library
group: foundation
learningOrder: 110
---

# 핵심 Kotlin 라이브러리

## 해결하는 문제 {#problem}

backend 모듈은 일관된 exception type을 쓰는 parameter validation, byte-safe encoder, bounded collection, 날짜와 시간 helper를 반복해서 필요로 합니다. `bluetape4k-core`는 이런 기반 기능을 모아 상위 모듈이 조금씩 다른 구현을 다시 만들지 않게 합니다.

## 사용 시점 {#when-to-use}

애플리케이션이나 다른 library가 여러 기반 type을 함께 사용할 때 core를 추가합니다. JDK나 Kotlin 한 줄로 계약이 분명하게 끝난다면 그 표현을 우선합니다. core의 가치는 여러 모듈이 같은 failure와 lifecycle 계약을 공유할 때 커집니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-core")
}
```

저장소는 core를 기본 Java/JVM 25 toolchain과 Kotlin 2.4로 compile합니다. 여러 상위 bluetape4k 모듈이 core를 API dependency로 사용합니다.

## 핵심 개념 {#concepts}

이 모듈은 하나의 runtime subsystem이 아니라 toolbox입니다. 주요 영역은 `support` validation과 extension, `codec` encoder, `collections` bounded/paginated container, `range` value type, `concurrent` helper, `functional` adapter, `time` DSL, reflection과 Apache Commons bridge입니다.

validation 이름은 failure 의미를 드러냅니다. 새 `require*` helper는 caller 입력을 `IllegalArgumentException`으로 거부합니다. `BoundedStack`, `RingBuffer` 같은 collection은 capacity가 계약에 포함됩니다.

## 매뉴얼 지도 {#manual-map}

Core를 API 이름순으로 읽기보다 경계가 실행 흐름을 따라 이동하는 순서로 읽습니다.

![Core boundary validation map](../../assets/core/validation-boundary.svg)

| 설계 질문 | 장 | 결정할 계약 |
| --- | --- | --- |
| 잘못된 caller 입력을 어디서 끊을까? | [검증과 불변식](./bluetape4k-core/validation.md) | exception type, parameter name, non-null 내부 모델 |
| bytes를 text로 어떻게 옮길까? | [Encoding과 데이터 경계](./bluetape4k-core/encoding-data.md) | charset, URL-safe Base64/Hex, malformed input |
| 최근 N개를 어떤 순서로 유지할까? | [Bounded collections](./bluetape4k-core/bounded-collections.md) | capacity, eviction, stack/ring read order |
| 시간 조회의 끝을 포함할까? | [시간과 범위](./bluetape4k-core/time-ranges.md) | endpoint inclusion, overlap, timezone |
| active와 waiting work를 어디까지 허용할까? | [Concurrency와 lifecycle](./bluetape4k-core/concurrency-lifecycle.md) | rejection, cancellation, close order |
| 위 계약을 한 component로 어떻게 묶을까? | [Core 실전 레시피](./bluetape4k-core/recipes.md) | end-to-end test와 운영 signal |

## 빠른 시작 {#quick-start}

```kotlin
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.support.requireNotBlank

fun tokenFor(userId: String?): String {
    val id = userId.requireNotBlank("userId")
    return id.encodeBase64String()
}
```

`requireNotBlank`는 검증한 값을 반환합니다. 별도의 `!!` 없이 expression 안에서 validation을 끝낼 수 있습니다.

## 작업별 API {#api-by-task}

| 작업 | 시작 지점 |
| --- | --- |
| nullable string과 caller argument 검증 | `io.bluetape4k.support.RequireSupport` extension |
| Base64, Base58, Base62, Hex, URL62 encode/decode | `io.bluetape4k.codec` |
| bounded LIFO 또는 circular window 유지 | `BoundedStack`, `RingBuffer` |
| page나 lazy permutation 표현 | `PaginatedList`, permutation extension |
| open/closed endpoint를 가진 range 표현 | `io.bluetape4k.range` type |
| duration, period, temporal, quarter 연산 | `io.bluetape4k.time` |
| 명시적인 close 계약을 가진 concurrent reduce | `ConcurrentReducer` |

## 무엇을 먼저 선택할까 {#decision-guide}

| 요구 사항 | 권장 선택 | 선택하지 말아야 할 경우 |
| --- | --- | --- |
| nullable 입력을 검증한 뒤 같은 값을 계속 사용 | `requireNotNull`, `requireNotBlank`, `requirePositiveNumber` 계열 | 내부 불변식 검증에는 사용하지 않습니다. caller 입력 오류를 뜻하는 `IllegalArgumentException` 계약입니다. |
| 최근 값을 LIFO로 보관 | `BoundedStack` | 시간순 재생이 필요하면 `RingBuffer`가 맞습니다. |
| 최근 N개를 입력 순서로 보관 | `RingBuffer` | stack의 top/pop 의미가 필요하면 `BoundedStack`을 사용합니다. |
| 비동기 요청의 동시 실행 수와 대기열을 함께 제한 | `ConcurrentReducer` | suspend 함수의 동시성만 제한한다면 coroutine semaphore나 `mapParallel`이 더 자연스럽습니다. |
| JVM 종료 시 전역 자원을 역순으로 정리 | `ShutdownQueue` | request나 bean lifecycle처럼 더 이른 종료 시점이 있다면 그 lifecycle에서 직접 닫습니다. |

## 실전 레시피 {#recipes}

### 1. 경계에서 검증하고 내부 타입을 단순하게 유지하기

```kotlin
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber

data class PageRequest(val cursor: String, val size: Int)

fun pageRequest(cursor: String?, size: Int): PageRequest =
    PageRequest(
        cursor = cursor.requireNotBlank("cursor"),
        size = size.requirePositiveNumber("size"),
    )
```

`require*` 함수는 검증한 receiver를 반환하므로 validation 이후 코드에서 nullable 분기나 `!!`가 필요 없습니다. 메시지의 parameter name은 API 이름과 맞춰야 운영 로그에서 어느 입력이 잘못됐는지 바로 찾을 수 있습니다.

### 2. bounded collection의 순서를 의도적으로 선택하기

```kotlin
val undo = BoundedStack<String>(maxSize = 3)
undo.pushAll("v1", "v2", "v3", "v4")
undo.toList() // [v4, v3, v2] — top에서 bottom 순서

val recent = RingBuffer<String>(maxSize = 3)
recent.addAll("v1", "v2", "v3", "v4")
recent.toList() // [v2, v3, v4] — 가장 오래된 값부터 순서
```

두 타입 모두 용량을 넘으면 가장 오래된 값을 버리지만 읽는 방향이 다릅니다. `BoundedStack.pop()`은 최신 값을 제거하고, `RingBuffer.drop(n)`은 오래된 값부터 제거합니다. 둘 다 process-local 자료구조이며 durable queue나 분산 backpressure를 대신하지 않습니다.

### 3. 외부 비동기 API 앞에 명시적인 용량 제한 두기

```kotlin
import io.bluetape4k.concurrent.concurrentReducerOf
import java.util.concurrent.CompletionStage

fun <T> fetchBounded(
    ids: List<String>,
    fetchAsync: (String) -> CompletionStage<T>,
): List<T> = concurrentReducerOf<T>(
    maxConcurrency = 8,
    maxQueueSize = 64,
).use { reducer ->
    ids.map { id -> reducer.add { fetchAsync(id) } }
        .map { promise -> promise.join() }
}
```

`add`는 queue가 가득 차거나 reducer가 닫힌 경우 호출 시점에 직접 throw하지 않습니다. 각각 `CapacityReachedException` 또는 `RejectedExecutionException`으로 완료된 `CompletableFuture`를 반환합니다. 따라서 반환 promise를 반드시 관찰해야 합니다. `close()`는 대기 중 작업을 취소하지만 이미 실행 중인 외부 `CompletionStage`까지 강제로 중단하지는 않습니다.

## 권장 패턴 {#patterns}

public boundary에서 입력을 검증하고 내부에는 non-null 값을 넘깁니다. codec은 domain logic 곳곳이 아니라 transport나 storage 경계에서 사용합니다. 작업량이 계속 쌓일 수 있다면 unbounded collection 대신 capacity를 가진 type을 선택합니다. executor나 queue를 소유하는 helper는 `use` 또는 `try-finally`에서 닫습니다.

## 연동 {#integrations}

Kotlin stdlib, Java time/reflection/concurrency, Apache Commons, Eclipse Collections, hashing utility를 감싸거나 보완합니다. 상위 bluetape4k 모듈 public API가 core type을 노출할 수 있습니다. source에서 core API를 직접 import한다면 transitive dependency에 기대지 말고 직접 선언하는 편이 명확합니다.

## 설정 {#configuration}

global 설정 파일은 없습니다. collection capacity, charset, range boundary, timeout 같은 constructor argument와 function parameter로 동작을 선택합니다. 이 값은 관련 없는 global state가 아니라 component를 소유한 설정 가까이에 둡니다.

## 실패 동작 {#failures}

validation helper는 잘못된 caller 입력에 `IllegalArgumentException`을 던집니다. codec decoder는 malformed input 오류를 underlying codec 계약에 따라 전달합니다. bounded collection은 잘못된 capacity를 생성 시점에 거부합니다. `ConcurrentReducer.close()`는 queue의 작업을 취소하고 이후 submission을 거부합니다.

### 문제 진단표

| 증상 | 먼저 확인할 것 | 대응 |
| --- | --- | --- |
| validation 예외의 값이 예상과 다름 | 체이닝 앞 단계에서 receiver가 변환됐는지, parameter name이 맞는지 | 경계에서 원본을 한 번 검증하고 변환을 뒤로 옮깁니다. |
| 최근 항목의 순서가 반대로 보임 | `BoundedStack.toList()`는 최신순, `RingBuffer.toList()`는 오래된 순인지 | undo/history 요구를 구분해 타입을 바꿉니다. |
| `ConcurrentReducer` 결과가 `CompletionException`으로 실패 | cause가 `CapacityReachedException`, task 예외, null stage인지 | queue 포화는 retry/429 같은 overload 정책으로 처리하고 task failure와 분리합니다. |
| shutdown 후 새 작업이 계속 들어옴 | reducer promise의 `RejectedExecutionException`을 무시하는지 | 모든 반환 future를 관찰하고 producer를 먼저 중단한 뒤 reducer를 닫습니다. |
| `ShutdownQueue` 자원 순서가 중요함 | 등록 순서의 역순(LIFO)으로 close되는지 | 의존 자원을 먼저 등록하고 그 자원을 사용하는 wrapper를 나중에 등록합니다. |

## 운영 {#operations}

대부분의 helper는 background service를 소유하지 않습니다. executor, queue, 큰 buffer를 감싸는 utility는 별도로 봐야 합니다. workload 근거로 capacity를 정하고 service lifecycle에 close/shutdown을 연결합니다. reflection helper를 hot path에 넣기 전에는 실제로 측정합니다.

## 테스트 {#testing}

테스트는 package와 계약 단위로 나뉩니다. `BoundedStackTest`, `PaginatedListTest`, codec/range/time test, `ConcurrentReducer` test를 시작점으로 삼을 수 있습니다.

```bash
./gradlew :bluetape4k-core:test --no-configuration-cache
```

특정 helper를 도입할 때는 전체 모듈을 하나의 integration으로 취급하지 말고 대응하는 가장 작은 test pattern을 참고합니다.

## 워크숍 {#workshops}

toolbox 전체를 다루는 단일 workshop은 없습니다. 상위 repository example이 core를 transitive하게 사용합니다. 한 기능을 익히려면 대응하는 unit test의 assertion 하나를 작은 실행 예제로 바꾸는 방법이 가장 빠릅니다.

## 제한 사항 {#limitations}

core의 API는 범위가 넓어 lifecycle과 성능 특성이 모두 같지 않습니다. 선택한 family의 source와 test를 확인해야 합니다. encoding은 암호화가 아니고, reflection helper가 비공개 API를 안정된 계약으로 바꾸지도 않으며, bounded container는 distributed backpressure를 제공하지 않습니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### bluetape4k-core 모듈 구성 개요 다이어그램

[![bluetape4k-core 모듈 구성 개요 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-diagram-01.svg)

_배포본 README: [`bluetape4k/core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/core/README.ko.md)_

### bluetape4k-core 핵심 클래스 구조 다이어그램

[![bluetape4k-core 핵심 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-diagram-02.svg)

_배포본 README: [`bluetape4k/core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/core/README.ko.md)_

### RequireSupport Validation 체이닝 흐름 다이어그램

[![RequireSupport Validation 체이닝 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-sequence-01.svg)

_배포본 README: [`bluetape4k/core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/core/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README와 API catalog](../../../../bluetape4k/core/README.ko.md)
- [Main source package](../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k)
- [모듈 테스트](../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k)
- [모듈 build와 dependency](../../../../bluetape4k/core/build.gradle.kts)
