---
manualId: bluetape4k-annotations
title: "API 성숙도 애너테이션"
description: 실험적 API, 내부 API, 주의가 필요한 API, 폐기 예정 API, beta API, 구현 제한 SPI에 명시적인 opt-in 계약을 적용합니다.
kind: library
group: foundation
learningOrder: 120
---

# API 성숙도 애너테이션

## 해결하는 문제 {#problem}

Kotlin visibility만으로는 모든 호환성 경계를 표현할 수 없습니다. inline 함수나 framework 연동 때문에 선언을 public으로 열어야 하지만 외부에서 직접 쓰면 안 되는 경우가 있습니다. 호출은 안정적이어도 외부 구현까지 허용하기에는 이른 SPI도 있습니다. `bluetape4k-annotations`는 이런 경계를 `@RequiresOptIn` marker로 compiler에 알립니다.

![Bluetape API marker 선택 지도](../../assets/annotations/annotation-decision-map.svg)

### 이 매뉴얼을 읽는 순서

| 궁금한 점 | 읽을 장 |
| --- | --- |
| 어떤 marker를 붙여야 하나요? | [Marker 선택](./bluetape4k-annotations/marker-selection.md) |
| `@OptIn`을 함수, class, file 중 어디에 둬야 하나요? | [Opt-in 범위](./bluetape4k-annotations/opt-in-scope.md) |
| 호출은 허용하고 외부 구현만 막으려면 어떻게 하나요? | [구현 전용 SPI](./bluetape4k-annotations/implementation-spi.md) |
| API가 안정화되거나 폐기될 때 marker는 어떻게 다루나요? | [호환성 수명주기](./bluetape4k-annotations/compatibility-lifecycle.md) |
| 실제 선언과 호출 코드를 함께 보고 싶어요. | [실전 레시피](./bluetape4k-annotations/recipes.md) |

## 사용 시점 {#when-to-use}

호환성을 보장하기 이른 API에는 `BluetapeExperimentalApi`, 기술적인 이유로만 public인 선언에는 `BluetapeInternalApi`를 붙입니다. 수명주기, 동시성, 자원 관리, 보안 계약을 먼저 이해해야 하는 API라면 `BluetapeDelicateApi`가 맞습니다. 마이그레이션을 위해서만 남긴 API는 `BluetapeObsoleteApi`, 안정화가 가까워졌지만 작은 변경 가능성이 남은 API는 `BluetapeBetaApi`로 표시합니다. 호출은 허용하되 상속이나 구현을 제한하려면 `BluetapeImplementationApi`를 선택합니다.

호환성 작업을 피하려고 안정된 API에 marker를 붙이면 안 됩니다. Kotlin visibility로 제한할 수 있다면 visibility를 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-annotations")
}
```

모든 annotation은 binary retention을 사용합니다. 사용하는 쪽에서는 compile할 때 계약을 확인할 수 있지만, runtime 동작은 추가되지 않습니다.

## 핵심 개념 {#concepts}

오류 수준 marker인 `Experimental`, `Internal`, `Obsolete`는 명시적인 `@OptIn`이 없으면 compile을 막습니다. 경고 수준인 `Beta`, `Delicate`는 위험을 알리되 compile은 허용합니다. `BluetapeImplementationApi`는 `@SubclassOptInRequired`와 함께 써서 “interface 호출”과 “interface 구현”을 구분합니다.

## 빠른 시작 {#quick-start}

```kotlin
import io.bluetape4k.annotations.BluetapeExperimentalApi

@BluetapeExperimentalApi
fun unstablePlan(): String = "draft"

@OptIn(BluetapeExperimentalApi::class)
fun evaluateDraft(): String = unstablePlan()
```

opt-in 범위는 가능한 한 좁게 둡니다. file 수준에 붙이면 같은 파일의 다른 불안정한 호출까지 함께 허용되므로, 의도하지 않은 사용을 놓치기 쉽습니다.

## 작업별 API {#api-by-task}

| 작업 | Marker |
| --- | --- |
| 호환성을 보장하지 않는 API 공개 | `BluetapeExperimentalApi` |
| 기술적인 이유로만 public인 선언 표시 | `BluetapeInternalApi` |
| lifecycle, 동시성, resource, 보안 주의 계약 표시 | `BluetapeDelicateApi` |
| migration용 API 표시 | `BluetapeObsoleteApi` |
| 안정화 전 beta API 표시 | `BluetapeBetaApi` |
| 호출은 허용하고 외부 구현은 제한 | `BluetapeImplementationApi` |

## 권장 패턴 {#patterns}

불안정한 계약을 가진 선언에 marker를 붙입니다. experimental API를 감싼 public wrapper라면 내부에서 opt-in한 뒤 안정된 계약을 제공하거나, wrapper에도 같은 marker를 붙여야 합니다. SPI는 base class나 interface에 표시해서 호출자가 아니라 구현자가 경고를 받게 합니다.

## 연동 {#integrations}

이 모듈은 Kotlin annotation 의미만 사용하므로 모든 bluetape4k 모듈에서 함께 쓸 수 있습니다. Java caller는 annotation metadata를 볼 수 있지만 Kotlin opt-in 진단을 받지 않습니다. Java에 공개하는 API라면 문서에도 제한을 적어야 합니다.

## 설정 {#configuration}

runtime 설정은 없습니다. build에서는 의존성을 추가하고, 제한된 source set 전체가 같은 계약을 의도적으로 받아들일 때만 compiler opt-in flag를 고려합니다. 어떤 호출에서 위험을 받아들였는지 남기는 source 수준 `@OptIn`이 기본 선택입니다.

## 실패 동작 {#failures}

오류 수준 marker는 opt-in이 없을 때 Kotlin compile을 실패시킵니다. 경고 수준 marker는 diagnostic을 출력합니다. bluetape4k 선언에서 marker를 모두 제거했더라도 marker annotation class 자체를 삭제하면 downstream의 `@OptIn` source가 깨질 수 있습니다.

## 운영 {#operations}

이 모듈은 thread, I/O, resource lifecycle을 추가하지 않습니다. 운영에서 살펴볼 것은 호환성 관리입니다. compiler option으로 marker를 한꺼번에 허용해 두기보다, 버전을 올릴 때 source의 opt-in 지점을 다시 검토합니다.

## 테스트 {#testing}

`BluetapeApiMarkersTest`가 retention, target, diagnostic level, subclass opt-in 계약을 검증합니다.

```bash
./gradlew :bluetape4k-annotations:test --no-configuration-cache
```

## 워크숍 {#workshops}

등록된 전용 workshop은 없습니다. 가장 작은 실습은 marked declaration을 공개하는 모듈과 이를 사용하는 모듈을 나눠 compiler diagnostic과 좁은 `@OptIn` 수정 결과를 확인하는 것입니다.

## 제한 사항 {#limitations}

marker는 정책을 전달하지만 binary compatibility를 분석하지 않고 Java caller에게 opt-in을 강제하지도 않습니다. 대체 API와 migration 안내가 필요한 상황에서는 `@Deprecated`를 대신할 수 없습니다.

## 근거 {#sources}

- [모듈 README](../../../../bluetape4k/annotations/README.ko.md)
- [Annotation source 디렉터리](../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations)
- [Marker 계약 테스트](../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
- [모듈 build](../../../../bluetape4k/annotations/build.gradle.kts)
