---
manualId: bluetape4k-annotations
title: API 성숙도 annotation
description: 실험적 API, 내부 API, 주의가 필요한 API, 폐기 예정 API, beta API, 구현 제한 SPI에 명시적인 opt-in 계약을 적용합니다.
kind: library
group: foundation
---

# API 성숙도 annotation

## 해결하는 문제 {#problem}

Kotlin visibility만으로 모든 호환성 경계를 표현할 수는 없습니다. inline이나 framework 통합 때문에 public이어야 하지만 직접 호출하면 안 되는 선언이 있고, 사용은 안정적이지만 외부 구현은 아직 허용하기 어려운 SPI도 있습니다. `bluetape4k-annotations`는 이런 경계를 `@RequiresOptIn` marker로 compiler에 알립니다.

## 사용 시점 {#when-to-use}

불안정한 호출 지점에는 `BluetapeExperimentalApi`, 기술적인 이유로 public인 내부 선언에는 `BluetapeInternalApi`, lifecycle·동시성·resource·보안 계약을 주의해야 하는 API에는 `BluetapeDelicateApi`를 사용합니다. migration 목적으로만 남긴 API는 `BluetapeObsoleteApi`, 안정화를 예상하지만 아직 변경 가능성이 있는 API는 `BluetapeBetaApi`가 맞습니다. 호출은 허용하되 상속이나 구현을 제한하려면 `BluetapeImplementationApi`를 선택합니다.

호환성 작업을 피하려고 안정된 API에 marker를 붙이면 안 됩니다. Kotlin visibility로 제한할 수 있다면 visibility를 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-annotations")
}
```

annotation은 binary retention을 사용합니다. downstream compile에서는 계약을 확인할 수 있지만 runtime 동작은 추가하지 않습니다.

## 핵심 개념 {#concepts}

오류 수준 marker인 `Experimental`, `Internal`, `Obsolete`는 명시적인 `@OptIn`이 없으면 compile을 막습니다. 경고 수준인 `Beta`, `Delicate`는 위험을 보여 주되 compile은 허용합니다. `BluetapeImplementationApi`는 `@SubclassOptInRequired`를 사용해 “interface 호출”과 “interface 구현”을 구분합니다.

## 빠른 시작 {#quick-start}

```kotlin
import io.bluetape4k.annotations.BluetapeExperimentalApi

@BluetapeExperimentalApi
fun unstablePlan(): String = "draft"

@OptIn(BluetapeExperimentalApi::class)
fun evaluateDraft(): String = unstablePlan()
```

opt-in 범위는 가능한 한 좁게 둡니다. file 수준 opt-in은 같은 파일의 다른 불안정 호출까지 가릴 수 있습니다.

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

불안정한 계약을 소유한 선언에 marker를 붙입니다. experimental API를 감싼 public wrapper는 내부에서 opt-in하고 안정된 계약을 제공하거나, wrapper에도 marker를 전파해야 합니다. SPI는 base class나 interface에 표시해 caller가 아니라 구현자가 경고를 받게 합니다.

## 연동 {#integrations}

이 모듈은 Kotlin annotation 의미만 사용하므로 모든 bluetape4k 모듈에서 함께 쓸 수 있습니다. Java caller는 annotation metadata를 볼 수 있지만 Kotlin opt-in 진단을 받지 않습니다. Java에 공개하는 API라면 문서에도 제한을 적어야 합니다.

## 설정 {#configuration}

runtime 설정은 없습니다. build에서는 의존성을 추가하고, 제한된 source set 전체가 같은 계약을 의도적으로 받아들일 때만 compiler opt-in flag를 고려합니다. 어떤 호출에서 위험을 받아들였는지 남기는 source 수준 `@OptIn`이 기본 선택입니다.

## 실패 동작 {#failures}

오류 수준 marker는 opt-in이 없을 때 Kotlin compile을 실패시킵니다. 경고 수준 marker는 diagnostic을 출력합니다. bluetape4k 선언에서 marker를 모두 제거했더라도 marker annotation class 자체를 삭제하면 downstream의 `@OptIn` source가 깨질 수 있습니다.

## 운영 {#operations}

thread, I/O, resource lifecycle을 추가하지 않습니다. 운영상 주의점은 호환성 관리입니다. 전체 opt-in으로 경고를 숨겨 두기보다는 upgrade할 때 opt-in 지점을 다시 검토해야 합니다.

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
