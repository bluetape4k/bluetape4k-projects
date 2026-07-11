---
manualId: bluetape4k-core
title: Core Kotlin utility
description: bluetape4k 전반에서 사용하는 validation, codec, collection, range, time DSL, reflection, concurrency primitive를 제공합니다.
kind: library
group: foundation
---

# Core Kotlin utility

## 해결하는 문제 {#problem}

backend 모듈은 일관된 exception type을 쓰는 parameter validation, byte-safe encoder, bounded collection, 날짜와 시간 helper를 반복해서 필요로 합니다. `bluetape4k-core`는 이런 기반 기능을 모아 상위 모듈이 조금씩 다른 구현을 다시 만들지 않게 합니다.

## 사용 시점 {#when-to-use}

애플리케이션이나 다른 library가 여러 기반 type을 함께 사용할 때 core를 추가합니다. JDK나 Kotlin 한 줄로 계약이 분명하게 끝난다면 그 표현을 우선합니다. core의 가치는 여러 모듈이 같은 failure와 lifecycle 계약을 공유할 때 커집니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-core")
}
```

저장소 기준은 Java 21과 Kotlin 2.3입니다. 여러 상위 bluetape4k 모듈이 core를 API dependency로 사용합니다.

## 핵심 개념 {#concepts}

이 모듈은 하나의 runtime subsystem이 아니라 toolbox입니다. 주요 영역은 `support` validation과 extension, `codec` encoder, `collections` bounded/paginated container, `range` value type, `concurrent` helper, `functional` adapter, `time` DSL, reflection과 Apache Commons bridge입니다.

validation 이름은 failure 의미를 드러냅니다. 새 `require*` helper는 caller 입력을 `IllegalArgumentException`으로 거부합니다. `BoundedStack`, `RingBuffer` 같은 collection은 capacity가 계약에 포함됩니다.

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

## 권장 패턴 {#patterns}

public boundary에서 입력을 검증하고 내부에는 non-null 값을 넘깁니다. codec은 domain logic 곳곳이 아니라 transport나 storage 경계에서 사용합니다. 작업량이 계속 쌓일 수 있다면 unbounded collection 대신 capacity를 가진 type을 선택합니다. executor나 queue를 소유하는 helper는 `use` 또는 `try-finally`에서 닫습니다.

## 연동 {#integrations}

Kotlin stdlib, Java time/reflection/concurrency, Apache Commons, Eclipse Collections, hashing utility를 감싸거나 보완합니다. 상위 bluetape4k 모듈 public API가 core type을 노출할 수 있습니다. source에서 core API를 직접 import한다면 transitive dependency에 기대지 말고 직접 선언하는 편이 명확합니다.

## 설정 {#configuration}

global 설정 파일은 없습니다. collection capacity, charset, range boundary, timeout 같은 constructor argument와 function parameter로 동작을 선택합니다. 이 값은 관련 없는 global state가 아니라 component를 소유한 설정 가까이에 둡니다.

## 실패 동작 {#failures}

validation helper는 잘못된 caller 입력에 `IllegalArgumentException`을 던집니다. codec decoder는 malformed input 오류를 underlying codec 계약에 따라 전달합니다. bounded collection은 잘못된 capacity를 생성 시점에 거부합니다. `ConcurrentReducer.close()`는 queue의 작업을 취소하고 이후 submission을 거부합니다.

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

## 근거 {#sources}

- [모듈 README와 API catalog](../../../../bluetape4k/core/README.ko.md)
- [Main source package](../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k)
- [모듈 테스트](../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k)
- [모듈 build와 dependency](../../../../bluetape4k/core/build.gradle.kts)
