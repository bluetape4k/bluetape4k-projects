---
title: Opt-in 범위
description: 선언에 marker를 붙이는 위치와 사용자가 위험을 승인하는 범위를 함수, class, file, compiler 수준에서 비교합니다.
manualId: bluetape4k-annotations
chapterId: opt-in-scope
---

# Opt-in 범위

marker는 API 제공자가 선언에 붙이고, `@OptIn`은 사용자가 계약을 받아들인 위치에 붙입니다. 이 둘을 구분해야 upgrade 때 어떤 위험을 누가 승인했는지 추적할 수 있습니다.

## 가장 좁은 범위부터 고릅니다

```kotlin
@OptIn(BluetapeExperimentalApi::class)
fun evaluateDraft(): Result = experimentalPlan()
```

함수 하나에서만 불안정한 API를 쓴다면 함수에 붙이는 것이 기본입니다. 호출과 승인 이유가 가까이 있어서 code review와 upgrade 점검이 쉽습니다.

class 전체가 같은 계약을 공유할 때는 class 수준이 자연스럽습니다.

```kotlin
@OptIn(BluetapeDelicateApi::class)
class LegacyCodecAdapter {
    fun encode(value: Any): ByteArray = KafkaCodecs.Jdk.encode(value)
    fun decode(bytes: ByteArray): Any = KafkaCodecs.Jdk.decode(bytes)
}
```

file 수준 opt-in은 해당 파일의 모든 선언에 영향을 줍니다. 같은 adapter나 compatibility bridge만 모아 둔 파일처럼 경계가 분명할 때 사용합니다.

```kotlin
@file:OptIn(BluetapeObsoleteApi::class)
```

## Compiler option은 예외입니다

Gradle compiler option으로 source set 전체를 opt-in할 수도 있습니다.

```kotlin
kotlin {
    compilerOptions {
        optIn.add("io.bluetape4k.annotations.BluetapeBetaApi")
    }
}
```

이 설정은 개별 사용 지점을 source에서 지웁니다. 제한된 실험 모듈 전체가 같은 정책을 의도적으로 받아들이는 경우가 아니라면 사용하지 않는 편이 낫습니다. 특히 `Internal`이나 `Obsolete`를 전역으로 허용하면 새 호출이 추가돼도 review에서 잘 드러나지 않습니다.

## Wrapper는 계약을 숨기거나 전파합니다

public wrapper가 marked API를 호출할 때는 두 선택지 중 하나를 분명히 해야 합니다.

안정된 계약으로 감쌀 수 있다면 wrapper 내부에서 opt-in하고 입력 검증, 실패 동작, 호환성 정책을 wrapper가 책임집니다.

```kotlin
fun decodeTrustedPayload(bytes: ByteArray): Order {
    require(bytes.isNotEmpty())
    return decodeDelicate(bytes)
}
```

wrapper도 같은 위험을 그대로 노출한다면 marker를 전파합니다.

```kotlin
@BluetapeExperimentalApi
fun experimentalFacade(): Result = experimentalPlan()
```

내부에서 `@OptIn`만 붙였다고 wrapper가 저절로 안정되는 것은 아닙니다. 안정된 계약을 실제로 제공하지 못한다면 marker를 숨기면 안 됩니다.

## Java 호출자는 별도로 문서화합니다

annotation은 binary에 남지만 Kotlin compiler의 opt-in 진단은 Java caller에게 강제되지 않습니다. Java에 공개하는 API라면 KDoc와 JavaDoc, migration 문서에도 제한을 적습니다. marker만 붙여 놓고 Java 사용자가 같은 경고를 받을 것으로 기대하면 안 됩니다.

## Upgrade 점검

버전을 올릴 때 `@OptIn` 검색 결과를 검토합니다. API가 안정화됐는지, 대체 API가 생겼는지, 허용 범위가 불필요하게 넓어졌는지를 확인합니다. file이나 compiler 수준 opt-in은 개별 함수 수준보다 먼저 살펴보는 편이 좋습니다.

## 근거

- [Experimental marker의 좁은 opt-in 안내](../../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations/BluetapeExperimentalApi.kt)
- [Kafka codec test의 함수 수준 opt-in](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/codec/KafkaCodecTest.kt)
- [Marker compile 계약 테스트](../../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
