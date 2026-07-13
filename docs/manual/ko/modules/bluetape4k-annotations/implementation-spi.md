---
title: 구현 전용 SPI 계약
description: 호출은 안정적으로 허용하면서 외부 구현과 상속에만 opt-in을 요구하는 방법을 설명합니다.
manualId: bluetape4k-annotations
chapterId: implementation-spi
---

# 구현 전용 SPI 계약

public interface를 열면 사용자는 메서드를 호출할 수 있을 뿐 아니라 직접 구현할 수도 있습니다. 호출 계약은 안정됐어도 구현자가 지켜야 할 메서드 조합, 수명주기, default method 규칙은 아직 바뀔 수 있습니다. 이때 `BluetapeImplementationApi`와 `@SubclassOptInRequired`를 함께 사용합니다.

## 선언하는 쪽

```kotlin
import io.bluetape4k.annotations.BluetapeImplementationApi
import kotlin.SubclassOptInRequired

@SubclassOptInRequired(BluetapeImplementationApi::class)
interface StorageProvider {
    fun load(key: String): ByteArray?
}
```

이 interface를 인자로 받거나 `load`를 호출하는 코드는 opt-in하지 않아도 됩니다. 외부에서 interface를 구현하거나 open class를 상속할 때만 warning이 발생합니다.

## 구현하는 쪽

```kotlin
@OptIn(BluetapeImplementationApi::class)
class FileStorageProvider : StorageProvider {
    override fun load(key: String): ByteArray? = TODO()
}
```

opt-in은 “현재 구현 계약이 바뀔 수 있으며, 버전을 올릴 때 구현 코드를 다시 확인하겠다”는 뜻입니다. 단순히 warning을 없애는 annotation이 아닙니다.

## 일반 marker와 다른 점

`BluetapeImplementationApi`의 target은 `CLASS`와 `ANNOTATION_CLASS`뿐입니다. 함수나 property에 직접 붙이는 일반적인 사용 지점 marker가 아닙니다. 반드시 SPI type에 `@SubclassOptInRequired(BluetapeImplementationApi::class)` 형태로 적용합니다.

호출 자체도 불안정하다면 `BluetapeExperimentalApi`나 `BluetapeBetaApi`가 더 맞습니다. “호출은 안정적이고 구현만 제한한다”는 조건이 성립할 때 `Implementation`을 선택합니다.

## SPI 작성자가 문서에 남길 내용

- 외부 구현이 아직 불안정한 이유
- 구현자가 지켜야 할 수명주기와 thread-safety 규칙
- 새 abstract member가 추가될 가능성
- 지원하는 구현 방식과 지원하지 않는 방식
- 안정화 여부를 다시 판단할 기준

marker는 compiler에 경계를 보여 줄 뿐, 구현 계약 자체를 설명하지는 않습니다. 위 정보는 type KDoc에 남겨야 합니다.

## 언제 제거하나요?

외부 구현을 장기적으로 호환할 수 있고, 새 abstract member 추가나 수명주기 변경 정책이 정해졌을 때 제거합니다. marker를 떼기 전에 실제 third-party implementation이 다음 minor 버전에서도 유지될 수 있는지 검토합니다.

## 근거

- [`BluetapeImplementationApi` source와 KDoc](../../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations/BluetapeImplementationApi.kt)
- [Local implementation compile test](../../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
