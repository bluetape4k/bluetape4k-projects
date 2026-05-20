# Module `bluetape4k-annotations`

[English](./README.md) | 한국어

`bluetape4k-annotations`는 Bluetape 라이브러리용 API 성숙도 마커를 제공하는 경량 모듈입니다.

주요 기능:

- experimental, beta, internal, delicate, obsolete API를 위한 명시적 Kotlin opt-in 계약
- `@SubclassOptInRequired` 기반 구현 전용 SPI 계약
- 낮은 수준의 모듈도 공개 시그니처에 안전하게 노출할 수 있는 최소 의존성 artifact

## 아키텍처

`bluetape4k-annotations`는 독립 모듈입니다. production 코드는 다른 Bluetape 모듈에 의존하지 않으므로, 어떤 모듈에서도 순환 의존 없이 사용할 수 있습니다.

모든 마커 annotation은 Kotlin `@RequiresOptIn`, `AnnotationRetention.BINARY`, `@MustBeDocumented`를 사용합니다. 선언 전용 마커이며 런타임 스캔 동작은 없습니다.

## 주요 기능

- **BluetapeExperimentalApi**: 호환성 보장 없이 변경 또는 제거될 수 있는 API용 error-level 마커
- **BluetapeBetaApi**: 안정화를 목표로 하지만 minor source, binary, behavior 변경 가능성이 남은 API용 warning-level 마커
- **BluetapeInternalApi**: 기술적 이유로 public이지만 외부 사용자 API가 아닌 선언용 error-level 마커
- **BluetapeDelicateApi**: lifecycle, concurrency, resource-management, security 계약 이해가 필요한 API용 warning-level 마커
- **BluetapeObsoleteApi**: migration 또는 compatibility 목적으로만 남은 API용 error-level 마커
- **BluetapeImplementationApi**: 사용은 안정적이지만 구현 또는 상속은 안정화되지 않은 SPI class/interface에 `@SubclassOptInRequired`로 붙이는 warning-level 마커

## 사용 예시

### 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-annotations")
}
```

### 불안정한 사용 지점 API 표시

```kotlin
import io.bluetape4k.annotations.BluetapeExperimentalApi

@BluetapeExperimentalApi
fun experimentalFeature(): String = "value"

@OptIn(BluetapeExperimentalApi::class)
fun caller(): String = experimentalFeature()
```

### 구현 민감 SPI 표시

```kotlin
import io.bluetape4k.annotations.BluetapeImplementationApi
import kotlin.SubclassOptInRequired

@SubclassOptInRequired(BluetapeImplementationApi::class)
interface StorageProvider

@OptIn(BluetapeImplementationApi::class)
class CustomStorageProvider : StorageProvider
```

`BluetapeImplementationApi`는 `@SubclassOptInRequired`와 함께만 사용합니다. 일반 함수나 프로퍼티 opt-in 마커가 아닙니다.

## 마커 선택 기준

| Marker | Level | 사용 기준 |
|---|---:|---|
| `BluetapeExperimentalApi` | Error | 안정화 전 변경 또는 제거될 수 있는 API |
| `BluetapeBetaApi` | Warning | 안정화에 가깝지만 minor 변경 가능성이 남은 API |
| `BluetapeInternalApi` | Error | 기술적 이유로 public이지만 지원되는 사용자 API가 아닌 선언 |
| `BluetapeDelicateApi` | Warning | lifecycle, concurrency, resource, security 지식이 필요한 API |
| `BluetapeObsoleteApi` | Error | migration 또는 compatibility 목적으로만 남아 새 코드에서 쓰지 않아야 하는 API |
| `BluetapeImplementationApi` | Warning | 호출은 가능하지만 구현 또는 상속에는 명시적 opt-in이 필요한 SPI |

## 호환성 메모

마커가 붙은 API가 stable로 승격되면 source/binary compatibility를 확인한 뒤 해당 API 선언에서 마커를 제거합니다. 단 downstream 코드가 `@OptIn`에서 marker class를 참조할 수 있으므로, major version에서 의도적으로 제거하기 전까지 marker annotation class 자체는 artifact에 유지합니다.
