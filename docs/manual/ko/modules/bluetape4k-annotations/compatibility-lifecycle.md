---
title: 호환성 수명주기
description: Experimental에서 stable로 승격하거나 obsolete API를 제거할 때 marker와 downstream opt-in을 관리하는 방법을 설명합니다.
manualId: bluetape4k-annotations
chapterId: compatibility-lifecycle
---

# 호환성 수명주기

marker는 현재 상태만 표시하는 이름표가 아닙니다. API가 안정화되거나 폐기될 때 어떤 호환성 검사를 거칠지 정하는 정책입니다. marker를 붙이는 순간부터 제거할 때까지 downstream source가 그 annotation class를 참조할 수 있다는 점도 함께 관리해야 합니다.

## Experimental에서 stable로

안정화 전에 다음 항목을 확인합니다.

- public signature와 generic type이 더 바뀌지 않는가
- 예외와 실패 동작을 문서로 고정했는가
- thread-safety와 resource ownership이 분명한가
- 대표 사용 예와 regression test가 있는가
- 다음 minor 버전에서도 source·binary 호환성을 유지할 수 있는가

검토가 끝나면 API 선언에서 marker를 제거합니다. 사용하는 쪽에 남은 `@OptIn`은 더 이상 필요하지 않으므로 정리할 수 있습니다.

## Beta는 약한 Experimental이 아닙니다

`Beta`는 warning 수준이지만 “아직 결정하지 않았다”는 뜻은 아닙니다. 안정화 방향이 정해졌고 작은 수정 가능성만 남았을 때 사용합니다. 큰 signature 변경이나 제거 가능성이 남아 있다면 `Experimental`을 유지합니다.

## Obsolete API 제거

`BluetapeObsoleteApi`는 새 사용을 compile error로 막고, 기존 compatibility bridge만 명시적으로 허용할 때 유용합니다. 그러나 대체 API와 migration 경로를 알려 주지는 않습니다.

```kotlin
@Deprecated(
    message = "Use decodeV2 instead.",
    replaceWith = ReplaceWith("decodeV2(bytes)"),
)
@BluetapeObsoleteApi
fun decodeLegacy(bytes: ByteArray): Value = TODO()
```

이처럼 제거 안내가 필요하면 `@Deprecated`와 함께 사용합니다. 실제 삭제는 major version 정책과 downstream migration 상태를 확인한 뒤 진행합니다.

## Marker class 자체는 더 오래 남습니다

모든 marked API가 stable이 돼도 `BluetapeExperimentalApi` 같은 annotation class를 즉시 artifact에서 지우면 안 됩니다. downstream code가 `@OptIn(BluetapeExperimentalApi::class)`를 여전히 참조할 수 있기 때문입니다. API 선언에서 marker를 제거하는 일과 marker class를 artifact에서 삭제하는 일은 별도 호환성 변경입니다.

marker class 삭제는 major version에서 의도적으로 처리하고 migration note에 남깁니다.

## 버전을 올릴 때 확인할 것

```bash
rg -n "@OptIn\(|Bluetape(Experimental|Beta|Internal|Delicate|Obsolete|Implementation)Api" src
```

검색 결과마다 다음을 확인합니다.

1. marked API가 여전히 같은 상태인가?
2. 더 안정된 대체 API가 생겼는가?
3. opt-in 범위를 함수 수준으로 줄일 수 있는가?
4. Java caller를 위한 제한 설명이 남아 있는가?
5. compiler option이 새 사용을 무심코 허용하고 있지 않은가?

## 근거

- [README 호환성 메모](../../../../../bluetape4k/annotations/README.ko.md)
- [Marker type name 안정성 test](../../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
- [Obsolete marker 계약](../../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations/BluetapeObsoleteApi.kt)
