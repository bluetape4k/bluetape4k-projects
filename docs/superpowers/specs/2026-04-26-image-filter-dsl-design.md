# utils/images 필터/색보정 DSL 설계 — Issue #131

- **Issue**: #131
- **모듈**: `utils/images` (`bluetape4k-images`)
- **scrimage 버전**: 4.3.10
- **작성일**: 2026-04-26
- **브랜치**: `issue-131-image-filter-dsl`
- **상태**: Draft (각 섹션별 사용자 승인 대기)

---

## 1. 배경 및 목적

### 1.1 현재 상황

`utils/images` 모듈은 scrimage 4.3.10을 기반으로 다음 기능을 제공한다.

- `ImmutableImageSupport` — 로딩/저장/스케일 확장
- `WatermarkFilterSupport`, `CaptionFilterSupport` — 단일 필터 적용 헬퍼
- `coroutines/SuspendImageWriter` — 코루틴 기반 비동기 쓰기

그러나 **다중 필터 체인을 선언적으로 구성하는 DSL이 없다**. 사용자가 70+ 내장 필터를 조합하려면 다음 패턴 중 하나를 선택해야 한다.

```kotlin
// (a) varargs — 가독성 낮음, 매개변수 명명 불가
val out = image.filter(
    BrightnessFilter(1.2f),
    ContrastFilter(1.1f),
    SepiaFilter()
)

// (b) 수동 체이닝 — 중간 객체 생성 비용
val out = image
    .filter(BrightnessFilter(1.2f))
    .filter(ContrastFilter(1.1f))
    .filter(SepiaFilter())

// (c) PipelineFilter 직접 — Java 컬렉션 타입, 가독성 낮음
val out = image.filter(PipelineFilter(listOf(...)))
```

### 1.2 도입 효과

1. **선언적 색보정 파이프라인**: `applyFilters { brightness(1.2f); contrast(1.1); saturation(1.15f); sepia() }` — 의도가 자명한 코드
2. **체인 최적화**: 인접한 scrimage Native 필터들을 `PipelineFilter`로 묶어 단일 패스로 적용 → varargs 대비 메모리/시간 절약
3. **확장성**: bluetape4k 컨벤션의 `xxxFilterOf(...)` 팩토리들을 DSL 컨텍스트의 멤버 함수로 노출하여 일관된 API 표면
4. **신규 필터 통합**: scrimage에 없는 `Saturation`, `ColorTemperature`, `RoundedCorner`, `MedianBlur` 등을 동일 DSL에서 사용
5. **suspend 친화**: `suspendApplyFilters { ... }`로 `Dispatchers.Default`에서 무거운 픽셀 연산을 비동기 처리

### 1.3 비목표(Non-goals)

- 70+ 내장 필터 100% 래핑 — Issue #131에 명시된 필터 + 일반적으로 자주 쓰이는 보정 필터에 한정
- ImageMagick / OpenCV 수준의 알고리즘 자체 구현
- GPU 가속 (scrimage가 CPU-bound, 외부 의존성 없이 작업)
- 색공간 변환의 완전한 캘리브레이션 (sRGB ↔ linear ↔ LAB의 화이트포인트 정밀 매핑)

---

## 2. 설계 위험 및 실패 모드

### Risk-1: scrimage `Filter.apply()`의 mutation semantics

scrimage `Filter.apply(ImmutableImage)` 시그니처는 **반환값이 없으며 인자를 직접 변경**한다. `ImmutableImage.filter(f)`는 `BufferedImage` 타입이 다를 때만 새 객체를 만들고, 같은 타입이면 원본을 그대로 변경한다 (4.3.10 소스 검증 완료: `ImmutableImage.filter(Filter)` — 타입 일치 시 `target = this` alias).

또한 `PipelineFilter` 자체도 mutation 격리를 보장하지 않는다 — 검증 결과:
- `PipelineFilter.apply(image)` — `TYPE_INT_ARGB`/`TYPE_INT_RGB` 입력은 **그대로 alias** (복사 안 함), 각 sub-filter의 `apply(copy)` 가 in-place로 변경
- `BufferedOpFilter.apply()` — `op().filter(image.awt(), image.awt())` 인플레이스 변환

- **실패 모드**: DSL이 어떤 스타일로 호출하든 — 단일 `image.filter(builtFilter)` 든, `PipelineFilter` 묶음이든 — 호출자의 원본 `image`가 mutate될 수 있다. "DSL은 새 이미지를 반환한다"는 외부 계약을 위반.
- **완화**: DSL 진입점(`compactAndApply`)에서 항상 `source.copy()` 로 1회 방어 복사하고, 그 사본에 대해서만 필터 체인을 적용한다 → 원본 보존이 보장된다. 자세한 메커니즘은 §4.7 참조.

### Risk-2: 색공간 변환의 정밀도/성능 손실

RGB ↔ HSV / HSB / LAB 변환은 부동소수점 연산이며, 라운드트립 시 양자화 오차로 1~2 픽셀 단위 색상 변화가 발생할 수 있다.

- **실패 모드**: 사용자가 `colorSpace { hsv { saturation += 0.1 } }`처럼 DSL 안에서 색공간 변환을 반복하면 누적 오차로 의도한 색과 결과가 달라진다.
- **완화**:
  - 단일 변환만 노출 (RGB → HSV → 조작 → RGB), 중첩 금지
  - HSBFilter 기반 구현은 scrimage가 이미 검증한 변환 함수(`Color.RGBtoHSB`)를 재사용
  - LAB 변환은 1차 범위에서 제외 (Risk-3에서 본격 처리)

### Risk-3: 모든 70+ 필터 노출의 API surface 폭증

scrimage 70개 이상의 필터를 DSL 멤버 함수로 노출하면 단일 인터페이스에 70+ 메서드가 매달린다 → IDE 자동완성 노이즈, 학습 곡선 증가, 거의 안 쓰이는 필터 유지보수 부담.

- **실패 모드**: PR 리뷰 단계에서 "이 50개 중 어떤 게 검증된 거냐?"는 질문이 나오고, 일부는 골든 이미지 테스트 없이 머지된다.
- **완화**: Issue #131 명시 + 색보정 핵심 + 효과/스타일 대표만 1차 노출. 나머지는 `raw(filter: Filter)` 이스케이프 해치로 사용 가능.

### Risk-4: SaturationFilter / RoundedCornerFilter 부재

검증 결과 (jar 인덱스 확인):
- `com.sksamuel.scrimage.filter.SaturationFilter` — **없음**. `thirdparty.jhlabs.image.SaturationFilter`만 존재 (scrimage 공개 API 아님)
- `com.sksamuel.scrimage.filter.MedianFilter` — **없음** (jhlabs 내부에만)
- `RoundedCornerFilter` — **없음**
- `VignetteFilter` — **있음** (scrimage 공개)
- `PipelineFilter` — **있음** (scrimage-core)
- `Filter` 인터페이스 — `com.sksamuel.scrimage.filter.Filter` (scrimage-core)

- **실패 모드**: HSBFilter로 saturation을 흉내 낸다고 해서 jhlabs `SaturationFilter`와 픽셀 단위로 동일하지 않다. 골든 이미지가 안 맞을 수 있다.
- **완화**: 신규 필터(`SaturationAdjustFilter` 등)를 직접 구현하되 알고리즘은 명세화하고 (HSV 공간에서 S 채널 곱셈), scrimage 의존하지 않는 자체 골든 이미지를 생성/저장.

### Risk-5: 필터 적용 순서에 따른 결과 차이

`brightness(1.2f); contrast(1.1)` 와 `contrast(1.1); brightness(1.2f)`는 결과가 다르다 — 비가환(non-commutative).

- **실패 모드**: DSL이 내부적으로 정렬하거나 최적화하면서 사용자 선언 순서를 바꾸면 출력이 변한다.
- **완화**: DSL은 **사용자 선언 순서를 보존**한다. 내부 최적화 금지. 문서에 명시.

---

## 3. 설계 접근법 비교

### 접근법 A: scrimage `Filter` SAM 직접 사용 + Kotlin DSL builder

`com.sksamuel.scrimage.filter.Filter`는 Java 인터페이스이며 단일 메서드 `void apply(ImmutableImage)`를 가진다. Kotlin에서는 SAM 변환으로 `Filter { image -> ... }`처럼 사용 가능.

```kotlin
class ImageFilterChain {
    private val filters = mutableListOf<Filter>()
    fun add(filter: Filter) = filters.add(filter)
    fun brightness(amount: Float) = add(BrightnessFilter(amount))
    fun contrast(amount: Double) = add(ContrastFilter(amount))
    // ...
    fun build(): Filter = PipelineFilter(filters)
}

fun ImmutableImage.applyFilters(block: ImageFilterChain.() -> Unit): ImmutableImage =
    filter(ImageFilterChain().apply(block).build())
```

- **장점**:
  - 추가 추상화 0층 — scrimage 타입 그대로 노출되어 학습 곡선 최소
  - 사용자가 `add(MyCustomFilter())`로 임의 scrimage 필터 즉시 사용 가능
  - 기존 `xxxFilterOf(...)` 팩토리들과 자연스럽게 결합 (`add(watermarkFilterOf(...))`)
- **단점**:
  - DSL 멤버 함수 안에 색공간 / 신규 필터를 추가할 때 `Filter` 외 타입을 노출해야 할 수 있음 (예: 색공간 컨버터는 `Filter`가 아닌 픽셀 함수)
  - `Filter`는 mutation 의미를 갖는 Java 인터페이스 — Kotlin 타입 안전성 측면에서 살짝 어색

### 접근법 B: 자체 `ImageFilter` fun interface로 한 단계 래핑

```kotlin
fun interface ImageFilter {
    fun apply(image: ImmutableImage): ImmutableImage
}

// scrimage Filter → ImageFilter 어댑터
fun Filter.asImageFilter(): ImageFilter = ImageFilter { it.filter(this) }

class ImageFilterChain {
    private val filters = mutableListOf<ImageFilter>()
    // ...
    fun build(): ImageFilter = ImageFilter { img ->
        filters.fold(img) { acc, f -> f.apply(acc) }
    }
}
```

- **장점**:
  - `apply`가 **입력→출력** 함수 시그니처 — Kotlin/FP 친화, 불변성 명확
  - 색공간 변환 같은 non-Filter 변환을 1급 시민으로 등록 가능
  - 향후 다른 백엔드(예: Java2D, OpenCV 어댑터) 전환 시 추상화 경계
- **단점**:
  - `PipelineFilter`의 단일 패스 최적화를 잃는다 — `fold` 기반은 매 단계 `ImmutableImage` 사본 생성
  - 사용자가 scrimage 필터를 직접 쓰려면 항상 `.asImageFilter()` 어댑터가 필요 → 보일러플레이트 증가
  - 추상화의 가치가 명확하지 않음 — scrimage가 `Filter`로 충분히 안정적

### 접근법 C: 하이브리드 — scrimage `Filter`를 1차 시민, 색공간/신규 변환은 DSL 멤버 함수로 별도 등록

```kotlin
class ImageFilterChain {
    private val ops = mutableListOf<Op>()

    private sealed interface Op {
        class Native(val filter: Filter) : Op
        class Pixel(val transform: (ImmutableImage) -> ImmutableImage) : Op
    }

    // scrimage 내장 래퍼
    fun brightness(amount: Float) = ops.add(Op.Native(BrightnessFilter(amount)))
    // 색공간 변환
    fun saturation(factor: Float) = ops.add(Op.Pixel { saturationAdjust(it, factor) })
    // 이스케이프 해치
    fun raw(filter: Filter) = ops.add(Op.Native(filter))
    fun pixel(transform: (ImmutableImage) -> ImmutableImage) = ops.add(Op.Pixel(transform))

    fun apply(source: ImmutableImage): ImmutableImage {
        // 진입점에서 source.copy() 1회로 mutation 격리
        // 이후 인접 Native는 PipelineFilter로 묶어 단일 패스, Pixel은 그 자리에서 변환
        return compactAndApply(source, ops.toList())
    }
}
```

- **장점**:
  - 색공간 변환과 scrimage 필터를 동시에 자연스럽게 표현
  - 인접 Native 묶음으로 PipelineFilter 단일 패스 최적화 유지
  - 사용자 측 API는 깔끔 (`brightness`, `saturation` 모두 평범한 멤버 함수)
- **단점**:
  - 내부 sealed class 추가 — 모듈 코드량 증가
  - `source.copy()` 진입점 비용 (1회) — 매우 작은 N에서는 직접 호출 대비 약간의 오버헤드

### 거부된 접근법 분석

**접근법 B 거부 이유**:
1. bluetape4k 컨벤션(`xxxFilterOf(...) -> Filter`)와 충돌 — 기존 팩토리들의 반환형이 모두 scrimage `Filter`인데, B를 채택하면 `xxxFilterOf` → `xxxImageFilterOf`로 전부 마이그레이션하거나 어댑터를 강제. 비파괴적이지 않음.
2. `PipelineFilter`의 단일 패스 최적화는 N=5+ 필터 체인에서 측정 가능한 차이를 낸다 (TYPE_INT_ARGB 변환 1회 vs N회). 이 가치를 포기할 명분이 약함.
3. `WatermarkFilterSupport`, `CaptionFilterSupport`가 이미 `Filter`를 반환하며 사용자 코드가 그 타입에 노출되어 있다. 새 추상화 도입은 호환성 비용 대비 이득 불확실.

**접근법 A 거부 이유**:
1. 색공간 변환(`saturation`, `colorTemperature`)은 scrimage `Filter`로 자연스럽게 표현되지 않는다. `Filter`로 강제하면 매번 `BufferedImage` round-trip이 필요하거나, mutation을 통해 상태를 관리해야 함 → Risk-1 재발.
2. 신규 직접 구현 필터(`SaturationAdjustFilter`)도 `Filter`로 만들면 mutation API가 되어 테스트가 어려움.

→ **접근법 C 채택**.

---

## 4. 선택된 설계

### 4.1 핵심 타입

`Op` sealed interface는 내부 구현 상세 (private). 외부 API는 `applyFilters { ... }` DSL과 그 안의 멤버 함수.

```kotlin
package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter

@DslMarker
annotation class ImageFilterDsl

@ImageFilterDsl
class ImageFilterChain internal constructor() {

    private sealed interface Op {
        class Native(val filter: Filter) : Op
        class Pixel(val transform: (ImmutableImage) -> ImmutableImage) : Op
    }

    private val ops = mutableListOf<Op>()

    /** scrimage 필터 인스턴스를 직접 추가 (이스케이프 해치) */
    fun raw(filter: Filter) { ops += Op.Native(filter) }

    /** 픽셀 변환 함수를 직접 추가 (이스케이프 해치) */
    fun pixel(transform: (ImmutableImage) -> ImmutableImage) { ops += Op.Pixel(transform) }

    // --- 4.2 색/톤 보정 ---
    fun brightness(amount: Float = 1.2f) { ... }
    fun contrast(amount: Double = 1.2) { ... }
    fun gamma(gamma: Double = 1.0) { ... }
    fun hsb(hue: Float = 0f, saturation: Float = 0f, brightness: Float = 0f) { ... }
    fun saturation(factor: Float) { ... }   // 1.0=원본, >1 채도 증가, <1 감소, 0=흑백
    fun rgb(r: Float = 1f, g: Float = 1f, b: Float = 1f) { ... }
    fun opacity(alpha: Float) { ... }
    fun threshold(value: Int = 127) { ... }
    fun posterize(levels: Int = 6) { ... }
    fun gainBias(gain: Float, bias: Float) { ... }
    fun colorTemperature(kelvin: Int) { ... }   // 신규 직접 구현

    // --- 4.3 톤/스타일 (preset) ---
    fun sepia() { ... }
    fun grayscale() { ... }
    fun invert() { ... }
    fun vintage() { ... }
    fun chrome() { ... }
    fun nashville() { ... }
    fun gotham() { ... }
    fun summer() { ... }
    fun oldPhoto() { ... }

    // --- 4.4 블러/선명도 ---
    fun blur() { ... }
    fun gaussianBlur(radius: Int = 2) { ... }
    fun motionBlur(distance: Float, angle: Float) { ... }
    fun sharpen() { ... }
    fun unsharp() { ... }
    fun noiseReduction(threshold: Int = 8) { ... }

    // --- 4.5 효과/스타일 ---
    fun oil(range: Int = 3, levels: Int = 256) { ... }
    fun crystallize() { ... }
    fun pixelate(blockSize: Int = 8) { ... }
    fun border(thickness: Int = 1, color: java.awt.Color = java.awt.Color.BLACK) { ... }
    fun vignette() { ... }
    fun glow(amount: Float = 0.5f) { ... }
    fun lensFlare() { ... }

    // --- 4.6 기존 팩토리 통합 ---
    fun watermark(text: String, /* ... */) { ... }   // watermarkFilterOf 위임
    fun caption(text: String, /* ... */) { ... }     // captionFilterOf 위임

    internal fun apply(source: ImmutableImage): ImmutableImage {
        // 진입점에서 source.copy() 로 mutation 격리, 이후 인접 Native 구간은 PipelineFilter로 묶음
        return compactAndApply(source, ops.toList())
    }
}

/**
 * DSL 진입점 — 동기 버전.
 *
 * ```kotlin
 * val out = image.applyFilters {
 *     brightness(1.2f)
 *     contrast(1.1)
 *     saturation(1.15f)
 *     vignette()
 * }
 * ```
 */
fun ImmutableImage.applyFilters(block: ImageFilterChain.() -> Unit): ImmutableImage =
    ImageFilterChain().apply(block).apply(this)

/** suspend 버전 — Dispatchers.Default 에서 적용 */
suspend fun ImmutableImage.suspendApplyFilters(
    block: ImageFilterChain.() -> Unit,
): ImmutableImage = withContext(Dispatchers.Default) {
    applyFilters(block)
}
```

### 4.7 mutation 격리 메커니즘 + PipelineFilter 묶음 최적화

scrimage의 `Filter.apply()` 와 `PipelineFilter` 모두 입력 이미지를 in-place로 변경할 수 있다 (Risk-1 참조). 따라서 mutation 격리는 **DSL 진입점에서 명시적인 방어 복사로 보장**한다.

**핵심 규칙**:
- mutation 격리는 `compactAndApply` 진입점에서 항상 `source.copy()` 를 1회 수행하여 보장한다.
- 그 사본에 대해 PipelineFilter(인접 Native 묶음) 또는 Pixel 변환을 적용한다.
- 이렇게 하면 어떤 타입의 이미지도 원본이 mutate되지 않는다.

**의사 코드**:
```
compactAndApply(source, ops):
    if ops is empty:
        return source                    // 변경할 게 없으면 원본 그대로 (방어 복사 생략)
    var current = source.copy()          // 방어 복사 1회 (원본 보존)
    for each group in compact(ops):
        current = when group:
            NativeGroup → current.filter(PipelineFilter(group.filters))
            Pixel       → group.transform(current)
    return current
```

**예시 — 인접 Native 묶음 동작**:
```
입력 ops: [Native(b), Native(c), Pixel(s), Native(v)]

current = source.copy()
current = current.filter(PipelineFilter([b, c]))   // 묶음 1
current = s(current)                                // Pixel
current = current.filter(PipelineFilter([v]))      // 묶음 2 (단일도 PipelineFilter로 감싸 일관성)
```

이렇게 하면 진입점 1회의 `copy()` 로 원본 보호가 보장되고, 인접 Native 필터들은 PipelineFilter 단일 패스로 적용되어 sub-filter들의 in-place mutation 이 사본 안에 격리된다.

### 4.8 ImageFilter fun interface 결정

**결정: 별도 `ImageFilter` fun interface를 노출하지 않는다**.

근거:
- 외부 API는 `applyFilters { ... }` DSL과 scrimage `Filter`로 충분
- bluetape4k의 `xxxFilterOf(...) -> Filter` 컨벤션과 호환
- 향후 자체 추상화가 필요하면 internal `ImageFilter`로 도입 가능 (호환성 위험 없음)

### 4.9 색공간 변환 모듈

색공간 변환 API는 **단일 `public object ColorSpaceConverter` 로 통일**한다. 별도 internal `ColorSpaces` 유틸리티는 두지 않는다 — 단일화로 유지보수 부담을 줄이고, 성능에 민감한 내부 메서드는 `ColorSpaceConverter` 안에 `@JvmSynthetic internal fun` 으로 둔다.

근거:
- 사용자는 DSL 멤버 (`saturation`, `colorTemperature`) 로 고수준 의도를 표현하지만, 직접 색공간을 다뤄야 하는 경우(픽셀 변환을 작성하는 고급 사용자)도 있다 — Issue #131 도 `ColorSpaceConverter` 를 공개 API로 명시
- internal/public 분리는 동일한 변환 로직이 중복되거나 정합성이 깨질 위험이 있다 → 단일화

```kotlin
/**
 * RGB ↔ HSV / YCbCr / Kelvin 변환 유틸리티.
 *
 * 공개 API — DSL 외부에서도 사용 가능.
 * 성능에 민감한 내부 호출은 `@JvmSynthetic internal` 함수를 사용하여 박싱을 회피한다.
 */
object ColorSpaceConverter {
    fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float>
    fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int>
    fun rgbToYCbCr(r: Int, g: Int, b: Int): Triple<Float, Float, Float>
    fun yCbCrToRgb(y: Float, cb: Float, cr: Float): Triple<Int, Int, Int>
    fun kelvinToRgb(kelvin: Int): Triple<Int, Int, Int>

    // --- 내부 고성능 경로 (FloatArray/IntArray 기반, 박싱 회피) ---
    @JvmSynthetic internal fun rgbToHsvInto(r: Int, g: Int, b: Int, out: FloatArray)
    @JvmSynthetic internal fun hsvToRgbInto(h: Float, s: Float, v: Float, out: IntArray)
    @JvmSynthetic internal fun kelvinToRgbInto(kelvin: Int, out: IntArray)
}
```

LAB 변환은 1차 범위 제외 (Risk-2).

### 4.10 필터 노출 정책

**결정: Issue #131 명시 + 일반적 색보정 핵심 + 대표 효과/스타일만 노출**한다 (~30개). 나머지는 `raw(...)` 이스케이프 해치.

거부한 대안 — 70+ 전체 노출:
- API surface가 너무 큼 (IDE 자동완성에 30+ 메서드만 떠도 이미 많음)
- 골든 이미지 테스트 부담이 70+ 케이스로 늘어남 → 1차 PR 범위 폭증
- bluetape4k는 다른 모듈에서도 "필요한 만큼만 래핑하고 raw escape hatch 제공" 패턴을 쓴다 (예: lettuce, exposed)

### 4.11 SaturationFilter 처리

**결정: HSV 공간에서 직접 구현한다** (`saturation(factor: Float)` DSL 멤버).

거부한 대안 — HSBFilter 위임:
- HSBFilter는 `(hue, saturation, brightness)`를 동시에 받아 HSB 공간에서 곱셈/덧셈을 수행 — saturation 전용이 아님
- HSB와 HSV는 다르다 (밝기 정의가 다름) → 사용자가 "saturation"이라고 부른 의도와 다른 결과
- 단순 직접 구현이 더 명확함 (RGB → HSV → S *= factor → RGB, 50줄 미만)

**결정: hue 만 단독 조정하는 `hue(deltaDegrees: Float)`도 동일하게 직접 구현한다**.

### 4.12 Float / Double 컨벤션

scrimage 필터 생성자가 받는 타입을 그대로 따른다 — 파라미터 변환 비용 0, 사용자가 IDE에서 보는 타입과 라이브러리 타입이 일치.

**규칙**:
- scrimage 생성자가 `Double`을 받는 필터(Contrast, Gamma 등)는 DSL 멤버도 `Double` 사용
- scrimage 생성자가 `Float`을 받는 필터(Brightness, HSB 등)는 DSL 멤버도 `Float` 사용
- 신규 직접 구현 필터는 픽셀 연산 수치 정밀도가 충분한 `Float`을 기본으로 하되, 명백히 `Double` 정밀도가 필요하면 `Double` 채택

대표 적용:
| DSL 멤버 | 타입 | 근거 |
|---|---|---|
| `brightness(amount: Float)` | Float | scrimage `BrightnessFilter(float)` |
| `contrast(amount: Double)` | Double | scrimage `ContrastFilter(double)` |
| `gamma(gamma: Double)` | Double | scrimage `GammaFilter(double)` |
| `hsb(hue, saturation, brightness: Float)` | Float | scrimage `HSBFilter(float, float, float)` |
| `saturation(factor: Float)` | Float | 신규, Float 충분 |
| `hue(deltaDegrees: Float)` | Float | 신규, Float 충분 |

---

## 5. 구현 대상 필터 목록

### 5.1 scrimage 내장 래퍼 (확인됨)

`Filter` 반환 팩토리 또는 DSL 멤버 함수로 노출.

| 카테고리 | DSL 멤버 | scrimage 클래스 | 비고 |
|---|---|---|---|
| 색/톤 | `brightness(Float)` | `BrightnessFilter` | |
| | `contrast(Double)` | `ContrastFilter` | scrimage 타입 준수 |
| | `gamma(Double)` | `GammaFilter` | |
| | `hsb(Float, Float, Float)` | `HSBFilter` | (h, s, b) 동시 |
| | `rgb(Float, Float, Float)` | `RGBFilter` | (r, g, b) 채널 곱셈 |
| | `opacity(Float)` | `OpacityFilter` | |
| | `threshold(Int)` | `ThresholdFilter` | |
| | `posterize(Int)` | `PosterizeFilter` | |
| | `gainBias(Float, Float)` | `GainBiasFilter` | |
| 톤/스타일 | `sepia` | `SepiaFilter` | |
| | `grayscale` | `GrayscaleFilter` | |
| | `invert` | `InvertFilter` | |
| | `vintage` | `VintageFilter` | |
| | `chrome` | `ChromeFilter` | |
| | `nashville` | `NashvilleFilter` | |
| | `gotham` | `GothamFilter` | |
| | `summer` | `SummerFilter` | |
| | `oldPhoto` | `OldPhotoFilter` | |
| 블러/선명도 | `blur` | `BlurFilter` | |
| | `gaussianBlur(Int)` | `GaussianBlurFilter` | |
| | `motionBlur(Float, Float)` | `MotionBlurFilter` | |
| | `sharpen` | `SharpenFilter` | scrimage 파라미터 없음 |
| | `unsharp` | `UnsharpFilter` | |
| | `noiseReduction(Int)` | `NoiseReductionFilter` | |
| 효과 | `oil(Int, Int)` | `OilFilter` | |
| | `crystallize` | `CrystallizeFilter` | |
| | `pixelate(Int)` | `PixelateFilter` | |
| | `border(Int, Color)` | `BorderFilter` | |
| | `vignette(start, end, blur, color)` | `VignetteFilter` | `(0.85f, 0.95f, 0.3f, BLACK)` |
| | `glow(Float)` | `GlowFilter` | |
| | `lensFlare` | `LensFlareFilter` | |
| 텍스트 | `watermark(...)` | `WatermarkFilter`/Cover/Stamp | 기존 팩토리 위임 |
| | `caption(...)` | `CaptionFilter` | 기존 팩토리 위임 |

### 5.2 신규 직접 구현 필터

| 이름 | DSL 멤버 | 알고리즘 |
|---|---|---|
| `SaturationAdjustFilter` | `saturation(factor: Float)` | 픽셀별 RGB→HSV, S *= factor, HSV→RGB. factor=1.0이 원본, >1 채도 증가, <1 감소 (0=흑백). |
| `HueAdjustFilter` | `hue(deltaDegrees: Float)` | 픽셀별 RGB→HSV, H = (H + delta) mod 360, HSV→RGB. |
| `ColorTemperatureFilter` | `colorTemperature(kelvin: Int)` | Kelvin→RGB 변환 (Tanner Helland 알고리즘) 후 채널 곱. kelvin ∈ [1000, 40000]. |
| `RoundedCornerFilter` | `roundedCorners(radius: Int)` | 알파 마스크 합성. 코너 반경 픽셀 영역에서 거리 기반 알파 페이드. |
| `MedianBlurFilter` | `medianBlur(radius: Int)` | 픽셀 주변 (2r+1)² 윈도우에서 R/G/B 채널별 중앙값 계산. jhlabs는 internal이므로 직접 구현. ★ 1차 포함. |

**표준 구현 골격 — scrimage `Filter` 인터페이스 구현**:

신규 필터 5종은 모두 scrimage `Filter` 인터페이스를 구현하며 다음 패턴을 따른다.

```kotlin
class SaturationAdjustFilter(private val factor: Float) : Filter {

    init {
        require(factor >= 0f) { "factor must be >= 0, but was $factor" }
    }

    override fun apply(image: ImmutableImage) {
        // apply()는 이미 compactAndApply 진입점에서 source.copy() 된 사본을 받으므로
        // image.awt() 의 raster 에 in-place 로 setPixels 가능 (원본 보호는 DSL 진입점이 책임)
        val raster = image.awt().raster
        // ... RGB→HSV, S *= factor, HSV→RGB, raster.setPixels
    }
}
```

**규약**:
- DSL 진입점(`compactAndApply`) 에서 방어 복사 책임을 진다 → 신규 `Filter` 구현체의 `apply()` 는 in-place 쓰기 허용
- 사용자가 `image.filter(saturationFilterOf(1.5f))` 처럼 직접 호출하는 경우, scrimage `ImmutableImage.filter(Filter)` 가 동일 타입에서 mutate 하는 동작과 일관됨 (사용자가 원본 보호를 원하면 `image.copy().filter(...)` 또는 DSL 사용)

### 5.3 색공간 변환 API

Issue #131에서 `ColorSpaceConverter`를 **공개 유틸리티**로 요구하므로 단일 `public object ColorSpaceConverter` 로 노출한다 (§4.9 단일화 정책).

```kotlin
/**
 * RGB ↔ HSV / YCbCr / Kelvin 변환 유틸리티.
 *
 * ```kotlin
 * val (h, s, v) = ColorSpaceConverter.rgbToHsv(255, 128, 0)
 * val (r, g, b) = ColorSpaceConverter.hsvToRgb(30f, 1f, 1f)
 * ```
 */
object ColorSpaceConverter {
    fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float>
    fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int>
    fun rgbToYCbCr(r: Int, g: Int, b: Int): Triple<Float, Float, Float>
    fun yCbCrToRgb(y: Float, cb: Float, cr: Float): Triple<Int, Int, Int>
    fun kelvinToRgb(kelvin: Int): Triple<Int, Int, Int>
    // LAB 변환은 1차 범위 제외 (Risk-2)

    // 성능에 민감한 내부 경로 (박싱 회피)
    @JvmSynthetic internal fun rgbToHsvInto(r: Int, g: Int, b: Int, out: FloatArray)
    @JvmSynthetic internal fun hsvToRgbInto(h: Float, s: Float, v: Float, out: IntArray)
}
```

추가로 `ImmutableImage` 확장 함수도 제공 (Issue #131 명시):

```kotlin
/**
 * 이미지의 각 픽셀을 HSV 색공간 값 배열로 변환합니다.
 * 반환: `FloatArray` of [h, s, v] per pixel (row-major order)
 */
fun ImmutableImage.toHsvArray(): FloatArray

/**
 * 이미지의 각 픽셀을 YCbCr 색공간 값 배열로 변환합니다.
 * 반환: `FloatArray` of [y, cb, cr] per pixel (row-major order)
 */
fun ImmutableImage.toYCbCrArray(): FloatArray
```

LAB 변환은 1차 범위 제외 (Risk-2).

### 5.4 이스케이프 해치

```kotlin
fun raw(filter: Filter)                                      // scrimage 임의 필터
fun pixel(transform: (ImmutableImage) -> ImmutableImage)     // 임의 픽셀 변환
```

---

## 5b. 에러 처리 정책

DSL 멤버 함수와 신규 `Filter` 구현체는 시스템 경계에서 입력값을 즉시 검증한다 (`require` 기반). 잘못된 값은 `IllegalArgumentException` 으로 즉시 실패하며, 메시지에 잘못된 값을 포함한다.

| 함수 / 필터 | 검증 | 사유 |
|---|---|---|
| `saturation(factor)` | `require(factor >= 0f) { "factor must be >= 0, but was $factor" }` | 음수 채도는 의미 없음 |
| `medianBlur(radius)` / `MedianBlurFilter` | `require(radius >= 0)` | 음수 반경 → 윈도우 크기 음수 |
| `gaussianBlur(radius)` | `require(radius >= 0)` | 동일 |
| `colorTemperature(kelvin)` / `ColorTemperatureFilter` | `require(kelvin in 1000..40000)` | Tanner Helland 알고리즘 근사 범위 |
| `roundedCorners(radius)` / `RoundedCornerFilter` | `require(radius >= 0)` | 음수 반경 → 마스크 좌표 깨짐 |
| `border(thickness)` | `require(thickness >= 0)` | 동일 |
| `pixelate(blockSize)` | `require(blockSize >= 1)` | 블록 크기 0/음수 무의미 |
| `posterize(levels)` | `require(levels >= 2)` | level=1 은 단색 변환, 일반적이지 않음 |
| `opacity(alpha)` | `require(alpha in 0f..1f)` | scrimage 가 0~1 범위 가정 |

**DSL 진입점 빈 ops 처리**:
- `applyFilters {}` — block 안에 ops가 추가되지 않으면 `compactAndApply` 가 `source` 를 그대로 반환 (`source.copy()` 생략, 원본 불변 보장).
- 단 한 개의 ops만 있어도 진입점 `source.copy()` 는 항상 수행한다 (일관성 + mutation 격리).

---

## 6. API 계약 (KDoc 포함)

### 6.1 진입점

```kotlin
/**
 * 이미지에 여러 필터를 DSL 형태로 일괄 적용합니다.
 *
 * 내부적으로 인접한 scrimage 네이티브 필터들은 [PipelineFilter]로 묶여 단일 패스로
 * 적용되어, 매 단계 `BufferedImage` 사본을 만들지 않습니다. 사용자 선언 순서는 그대로
 * 보존됩니다 (필터는 비가환).
 *
 * 원본 이미지는 변경되지 않습니다 — DSL 진입점에서 `source.copy()` 1회로 mutation을 격리합니다.
 *
 * ```kotlin
 * val edited: ImmutableImage = image.applyFilters {
 *     brightness(1.2f)
 *     contrast(1.1)
 *     saturation(1.15f)
 *     vignette()
 *     watermark("© bluetape4k")
 * }
 * ```
 *
 * @param block DSL 빌더 블록. 호출 순서대로 필터가 적용됩니다.
 * @return 필터가 적용된 새 [ImmutableImage] (원본은 변경되지 않음)
 */
fun ImmutableImage.applyFilters(
    block: ImageFilterChain.() -> Unit,
): ImmutableImage

/**
 * [applyFilters] 의 suspend 버전. [Dispatchers.Default] 에서 픽셀 연산을 수행합니다.
 *
 * `withContext(Dispatchers.Default)` 가 기본적으로 취소 포인트를 제공하므로 별도 `ensureActive()`
 * 호출은 필요 없습니다. 외부 취소는 context switch 시 전파됩니다.
 *
 * ```kotlin
 * val edited = image.suspendApplyFilters {
 *     brightness(1.2f)
 *     vintage()
 * }
 * ```
 */
suspend fun ImmutableImage.suspendApplyFilters(
    block: ImageFilterChain.() -> Unit,
): ImmutableImage
```

### 6.2 신규 필터 인스턴스 팩토리

bluetape4k 컨벤션에 따라 DSL 외에 직접 사용할 수 있는 `xxxFilterOf(...)` 팩토리도 함께 제공한다.

```kotlin
/**
 * HSV 공간에서 채도를 조정하는 [Filter] 를 생성합니다.
 *
 * ```kotlin
 * val saturated = image.filter(saturationFilterOf(factor = 1.2f))
 * ```
 *
 * @param factor 채도 배수. 1.0=원본, >1 증가, <1 감소, 0=흑백. 0 이상이어야 함.
 */
fun saturationFilterOf(factor: Float): Filter

fun hueFilterOf(deltaDegrees: Float): Filter

/**
 * 색온도 변환 [Filter] 를 생성합니다.
 *
 * @param kelvin 목표 색온도 (켈빈). 1000~40000 권장. 5500K가 중성.
 */
fun colorTemperatureFilterOf(kelvin: Int): Filter

/**
 * 사각형 모서리를 둥글게 깎는 [Filter] 를 생성합니다.
 *
 * @param radius 모서리 반경 (픽셀). 0 이상이어야 함.
 */
fun roundedCornerFilterOf(radius: Int): Filter

/**
 * 픽셀 주변 윈도우의 채널별 중앙값으로 노이즈를 제거하는 [Filter] 를 생성합니다.
 *
 * @param radius 윈도우 반경. 0 이상이어야 함. 윈도우 크기 = (2r+1)².
 */
fun medianBlurFilterOf(radius: Int): Filter
```

### 6.3 ImageFilterChain 멤버 함수 시그니처 (대표)

타입 컨벤션은 §4.12 참조 — scrimage 생성자 타입을 그대로 따름.

```kotlin
@ImageFilterDsl
class ImageFilterChain {
    fun raw(filter: Filter)
    fun pixel(transform: (ImmutableImage) -> ImmutableImage)

    // 색/톤
    fun brightness(amount: Float = 1.2f)        // scrimage BrightnessFilter(float)
    fun contrast(amount: Double = 1.2)          // scrimage ContrastFilter(double)
    fun gamma(gamma: Double = 1.0)              // scrimage GammaFilter(double)
    fun hsb(hue: Float = 0f, saturation: Float = 0f, brightness: Float = 0f)
    fun saturation(factor: Float)               // 1.0=원본, >1 증가, <1 감소, 0=흑백 (S × factor)
    fun hue(deltaDegrees: Float)
    fun rgb(r: Float = 1f, g: Float = 1f, b: Float = 1f)
    fun opacity(alpha: Float)
    fun threshold(value: Int = 127)
    fun posterize(levels: Int = 6)
    fun gainBias(gain: Float, bias: Float)
    fun colorTemperature(kelvin: Int)

    // 톤/스타일 — 매개변수 없음
    fun sepia()
    fun grayscale()
    fun invert()
    fun vintage()
    fun chrome()
    fun nashville()
    fun gotham()
    fun summer()
    fun oldPhoto()

    // 블러/선명도
    fun blur()
    fun gaussianBlur(radius: Int = 2)
    fun motionBlur(distance: Float, angle: Float)
    fun sharpen()
    fun unsharp()
    fun noiseReduction(threshold: Int = 8)

    // 효과
    fun oil(range: Int = 3, levels: Int = 256)
    fun crystallize()
    fun pixelate(blockSize: Int = 8)
    fun medianBlur(radius: Int = 1)             // 신규 직접 구현
    fun border(thickness: Int = 1, color: java.awt.Color = java.awt.Color.BLACK)
    fun vignette(start: Float = 0.85f, end: Float = 0.95f, blur: Float = 0.3f, color: java.awt.Color = java.awt.Color.BLACK)
    fun glow(amount: Float = 0.5f)
    fun lensFlare()
    fun roundedCorners(radius: Int)

    // 텍스트 — 두 오버로드 모두 노출 (watermarkFilterOf 위임)
    // 오버로드 1: COVER/STAMP 방식
    fun watermark(
        text: String,
        font: java.awt.Font = DEFAULT_FONT,
        type: WatermarkFilterType = WatermarkFilterType.COVER,
        antiAlias: Boolean = true,
        alpha: Double = 0.1,
        color: java.awt.Color = java.awt.Color.WHITE,
    )
    // 오버로드 2: x/y 좌표 방식
    fun watermarkAt(
        text: String,
        x: Int,
        y: Int,
        font: java.awt.Font = DEFAULT_FONT,
        antiAlias: Boolean = true,
        alpha: Double = 0.1,
        color: java.awt.Color = java.awt.Color.WHITE,
    )
    fun caption(
        text: String,
        x: Int = 0,
        y: Int = 0,
        font: java.awt.Font = DEFAULT_FONT,
        color: java.awt.Color = java.awt.Color.WHITE,
    )
}
```

### 6.4 반환 타입과 mutation 보장

- `applyFilters`/`suspendApplyFilters`는 항상 새 `ImmutableImage`를 반환하며 호출자 원본을 변경하지 않는다.
- **보장 메커니즘**: `compactAndApply` 진입점에서 `source.copy()` 1회 수행 — 그 사본에 대해 PipelineFilter(인접 Native 묶음) 또는 Pixel 변환을 적용한다 (§4.7 참조). 이 방어 복사가 scrimage `Filter.apply()` 와 `PipelineFilter` 의 in-place mutation 으로부터 원본을 보호하는 유일한 수단이다.
- ops가 비어 있으면 `source` 를 그대로 반환 (`copy()` 생략 — 원본 불변은 자동 보장).
- 단일 필터만 있어도 동일하게 `source.copy()` 후 `PipelineFilter([f])`로 감싸 적용 (일관성).

---

## 7. 테스트 전략

### 7.1 테스트 구조

```
utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/
  ImageFilterChainTest.kt              # DSL 빌더 단위 테스트 (op 누적 순서)
  ImageFilterDslApplyTest.kt           # applyFilters / suspendApplyFilters 동작
  SaturationFilterTest.kt              # 신규 필터 픽셀 단위 검증
  HueFilterTest.kt
  ColorTemperatureFilterTest.kt
  RoundedCornerFilterTest.kt
  MedianBlurFilterTest.kt
  ColorSpaceConverterTest.kt           # RGB↔HSV / YCbCr 라운드트립
  PipelineCompactionTest.kt            # 인접 Native 묶음 최적화 검증

utils/images/src/test/resources/filters/dsl/
  source.png                           # 입력 이미지 (256x256, 다양한 색)
  expected_brightness_1_2.png          # 골든 이미지
  expected_pipeline_b_c_sepia.png
  ...
```

### 7.2 테스트 패턴

기존 `AbstractFilterTest` 활용:

```kotlin
class ImageFilterDslApplyTest : AbstractFilterTest() {

    companion object : KLoggingChannel()

    @Test
    fun `applyFilters preserves source immutability`() {
        // Arrange
        val source = loadResourceImage("source.png")
        val sourcePixelHash = source.pixels().contentHashCode()

        // Act
        source.applyFilters {
            brightness(1.5f)
            contrast(1.2)
        }

        // Assert
        source.pixels().contentHashCode() shouldBeEqualTo sourcePixelHash
    }

    @Test
    fun `applyFilters chain matches sequential filter calls`() {
        // Arrange
        val source = loadResourceImage("source.png")

        // Act
        val viaDsl = source.applyFilters {
            brightness(1.2f)
            contrast(1.1)
        }
        val viaSequential = source.copy()
            .filter(BrightnessFilter(1.2f))
            .filter(ContrastFilter(1.1))

        // Assert — 픽셀 단위 허용 오차
        assertSimilarToImage(viaDsl, viaSequential, tolerance = 2)
    }

    @Test
    fun `applyFilters preserves user declared order`() {
        // brightness→contrast 와 contrast→brightness 는 결과가 다름을 보장
        val source = loadResourceImage("source.png")
        val a = source.applyFilters { brightness(1.5f); contrast(1.5) }
        val b = source.applyFilters { contrast(1.5); brightness(1.5f) }

        assertNotSimilarToImage(a, b)
    }

    @Test
    fun `saturation factor of 1_0 is identity`() {
        val source = loadResourceImage("source.png")
        val out = source.applyFilters { saturation(1.0f) }
        assertSimilarToImage(out, source.copy(), tolerance = 1)
    }

    @Test
    fun `suspendApplyFilters runs on Default dispatcher`() = runTest {
        val source = loadResourceImage("source.png")
        var capturedThread: Thread? = null

        val result = source.suspendApplyFilters {
            pixel { img ->
                capturedThread = Thread.currentThread()
                img
            }
            sepia()
        }

        capturedThread!!.name shouldContain "DefaultDispatcher"
        result.width shouldBeEqualTo source.width
        result.height shouldBeEqualTo source.height
    }
}
```

### 7.3 골든 이미지 정책

- **scrimage 내장 필터 래퍼**: `assertSimilarToImage(viaDsl, viaSequential, tolerance = 2)` — DSL 출력이 직접 호출 출력과 거의 같음을 검증. 이 경우 **별도 골든 이미지 저장 불필요**.
- **신규 직접 구현 필터** (`saturation`, `hue`, `colorTemperature`, `roundedCorners`, `medianBlur`): 골든 이미지 1장씩 (`expected_<filter>_<param>.png`)를 저장하고 `assertSimilarToResource(result, "expected_saturation_1_2.png", tolerance = 3)`로 검증.
- **체인 케이스**: 대표 체인 3~5개에 골든 이미지 (`expected_pipeline_brightness_contrast_sepia.png` 등).
- **허용 오차**: scrimage 내부 부동소수점 연산 차이 흡수 위해 픽셀 채널당 ±3 허용 (`WatermarkFilterTest`의 기존 패턴 준수).
- **테스트 이미지 크기**: 256×256 — 골든 이미지 디스크 비용 최소화.

### 7.4 PipelineCompactionTest

내부 최적화 (인접 Native 묶음) 를 spy 없이 **결과 기반**으로 검증한다. `[N, N, Pixel, N]` 와 수동으로 `PipelineFilter([N, N]) → Pixel → PipelineFilter([N])` 체인이 픽셀 동등하면 묶음 최적화가 의도대로 동작한 것이다 (사용자 선언 순서 보존 + 인접 Native 묶음).

```kotlin
@Test
fun `compactAndApply groups consecutive native ops`() {
    val source = loadResourceImage("source.png")

    // 기대값: 수동으로 [Native(b, c)] → Pixel(identity) → [Native(sepia)] 체인
    val expected = source.copy()
        .filter(PipelineFilter(listOf(BrightnessFilter(1.2f), ContrastFilter(1.1))))
        .let { img -> img }   // Pixel identity — 분기점
        .filter(PipelineFilter(listOf(SepiaFilter())))

    // 실제: DSL 이 [N, N, Pixel, N] 을 [N+N], [Pixel], [N] 으로 묶어 동일 결과를 내야 함
    val actual = source.applyFilters {
        brightness(1.2f)
        contrast(1.1)
        pixel { it }        // identity 분기점
        sepia()
    }

    assertSimilarToImage(actual, expected, tolerance = 1)
}

@Test
fun `compactAndApply preserves source when ops empty`() {
    val source = loadResourceImage("source.png")
    val out = source.applyFilters { /* no ops */ }
    // ops 비어 있으면 copy() 생략 — 원본과 동일 인스턴스 또는 동등
    assertSimilarToImage(out, source, tolerance = 0)
}

@Test
fun `compactAndApply handles only Pixel ops`() {
    val source = loadResourceImage("source.png")
    val out = source.applyFilters {
        pixel { it.copy() }
        pixel { it.copy() }
    }
    // Native 가 없는 경로도 정상 동작
    assertSimilarToImage(out, source, tolerance = 0)
}
```

### 7.5 Coverage 목표

- `ImageFilterChain`: 신규 멤버 함수 100% 라인 커버
- `ColorSpaceConverter`: 100% 라인 + 라운드트립 fuzz (랜덤 RGB → HSV → RGB 1000회)
- 신규 필터: 골든 이미지 + 경계 케이스 (factor=1.0 identity, kelvin=5500K 중성 등)
- 에러 처리: `require` 가드 단위 테스트 (음수 factor, 범위 초과 kelvin 등 → `IllegalArgumentException`)

---

## 8. 영향 범위 / 호환성

- **신규 파일** — 기존 코드 수정 없음
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChain.kt`
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterDslApply.kt` (extension functions)
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ColorSpaceConverter.kt` (public)
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/SaturationAdjustFilter.kt`
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/HueAdjustFilter.kt`
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/ColorTemperatureFilter.kt`
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/RoundedCornerFilter.kt`
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/MedianBlurFilter.kt`
- **README.md / README.ko.md** 업데이트 (DSL 사용 예제 + 신규 필터 표)
- **build.gradle.kts** — 변경 없음 (scrimage 의존성은 이미 존재)

기존 `WatermarkFilterSupport`, `CaptionFilterSupport` 시그니처 변경 없음 → 완전 비파괴적.

---

## 9. Definition of Done

- [ ] `ImageFilterChain` DSL 클래스 + `applyFilters` / `suspendApplyFilters` 확장 구현 완료
- [ ] §5.1 표의 모든 scrimage 내장 래퍼 DSL 멤버 함수 노출
- [ ] §5.2 표의 신규 필터 5종 (`SaturationAdjustFilter`, `HueAdjustFilter`, `ColorTemperatureFilter`, `RoundedCornerFilter`, `MedianBlurFilter`) 직접 구현
- [ ] 신규 필터별 `xxxFilterOf(...)` 팩토리 함수 (DSL 외부에서도 사용 가능)
- [ ] `ColorSpaceConverter` public object (rgbToHsv, hsvToRgb, rgbToYCbCr, yCbCrToRgb, kelvinToRgb) + `ImmutableImage.toHsvArray()` / `toYCbCrArray()` 확장 함수
- [ ] `compactAndApply` — 진입점 `source.copy()` mutation 격리 + 인접 Native 묶음 PipelineFilter 최적화
- [ ] §5b 에러 처리 정책 — 모든 신규 멤버/필터에 `require` 가드 적용
- [ ] 모든 신규 public API에 한국어 KDoc + 코드 예제
- [ ] DSL 빌더 단위 테스트 (op 순서 보존)
- [ ] applyFilters / suspendApplyFilters 동작 테스트 (mutation 격리, 순서 보존, suspend 디스패처 검증)
- [ ] 신규 필터 픽셀 단위 골든 이미지 테스트 (각 1장 이상)
- [ ] 체인 케이스 골든 이미지 테스트 3+
- [ ] 색공간 라운드트립 fuzz 테스트
- [ ] PipelineCompaction 최적화 검증 테스트 (결과 기반, spy 없음)
- [ ] `./gradlew :bluetape4k-images:test` 전수 통과
- [ ] `./gradlew :bluetape4k-images:detekt` 통과
- [ ] `README.md` / `README.ko.md` 업데이트 (DSL 섹션 추가, Mermaid UML 갱신)
- [ ] `code-reviewer` 에이전트 리뷰 → HIGH/CRITICAL 0
- [ ] `docs/superpowers/index/2026-04.md` 항목 추가
- [ ] PR 생성 시 본 spec 링크 포함

---

## 9b. Issue #131 요구사항 대조표

| Issue #131 요구 | Spec 처리 | 섹션 |
|---|---|---|
| `ImageFilter` fun interface (public) | 미노출 (설계 결정: 기존 scrimage `Filter` SAM으로 충분) | §4.8 |
| scrimage `Filter` → `ImageFilter` 어댑터 | DSL `raw(filter)` 이스케이프 해치로 동등 기능 제공 | §5.4 |
| `BrightnessFilter(factor: Float)` | DSL `brightness(amount: Float)` | §5.1 |
| `ContrastFilter(factor: Float)` | DSL `contrast(amount: Double)` (scrimage 타입 준수) | §5.1, §4.12 |
| `SaturationFilter(factor: Float)` | 신규 `SaturationAdjustFilter(factor: Float)` | §5.2 |
| `GammaFilter(gamma: Double)` | DSL `gamma(gamma: Double)` | §5.1 |
| `SepiaFilter` | DSL `sepia()` | §5.1 |
| `GrayscaleFilter` | DSL `grayscale()` | §5.1 |
| `InvertFilter` | DSL `invert()` | §5.1 |
| `VignetteFilter(strength, radius)` | `vignette(start, end, blur, color)` (scrimage 실제 API) | §5.1 |
| `RoundedCornerFilter(radius)` | 신규 `RoundedCornerFilter` | §5.2 |
| `GaussianBlurFilter(radius)` | DSL `gaussianBlur(radius)` | §5.1 |
| `SharpenFilter(strength)` | DSL `sharpen()` — scrimage `SharpenFilter` 파라미터 없음 | §5.1 |
| `PixelateFilter(blockSize)` | DSL `pixelate(blockSize)` | §5.1 |
| `MedianFilter(radius)` | 신규 `MedianBlurFilter(radius)` | §5.2 |
| `ColorSpaceConverter.rgbToHsv` | `ColorSpaceConverter.rgbToHsv(...)` | §5.3 |
| `ColorSpaceConverter.rgbToLab` | 1차 범위 제외 (Risk-2) | §5.3 |
| `ImmutableImage.toHsv()` | `ImmutableImage.toHsvArray()` | §5.3 |
| `ImmutableImage.toYCbCr()` | `ImmutableImage.toYCbCrArray()` | §5.3 |
| DSL `applyFilters { }` | `ImmutableImage.applyFilters { }` | §4.1 |

---

## 10. 초안 Task 목록

| ID | 분류 | 작업 |
|---|---|---|
| **T-V1** | 검증 | scrimage 4.3.10 `PipelineFilter` 의 mutation semantics 검증 (alias 동작 확인 — 완료, source 검증 근거 §2 Risk-1 참조) |
| **T-V2** | 검증 | `ImmutableImage.filter(Filter)` 의 same-type / different-type 분기 동작 확인 (완료) |
| **T1** | 인프라 | `filters/dsl/` 패키지 + `@ImageFilterDsl` DslMarker + `ImageFilterChain` 골격 (sealed `Op` 포함) |
| **T2** | 인프라 | `compactAndApply` — 진입점 `source.copy()` + 인접 Native 묶음 → PipelineFilter, Pixel은 그 자리 변환 |
| **T3** | 인프라 | `applyFilters` / `suspendApplyFilters` 확장 함수 |
| **T4** | 색공간 | `ColorSpaceConverter` public object (rgbToHsv, hsvToRgb, rgbToYCbCr, yCbCrToRgb, kelvinToRgb, internal *Into 변종) + `toHsvArray()`/`toYCbCrArray()` ImmutableImage 확장 + 라운드트립 fuzz 테스트 |
| **T5** | 신규 필터 | `SaturationAdjustFilter` (HSV, S×factor) + `saturationFilterOf` 팩토리 + `require(factor >= 0)` + 골든 이미지 테스트 |
| **T6** | 신규 필터 | `HueAdjustFilter` + `hueFilterOf` 팩토리 + 골든 이미지 테스트 |
| **T7** | 신규 필터 | `ColorTemperatureFilter` + 팩토리 + `require(kelvin in 1000..40000)` + 골든 이미지 테스트 |
| **T8** | 신규 필터 | `RoundedCornerFilter` + 팩토리 + `require(radius >= 0)` + 골든 이미지 테스트 |
| **T8b** | 신규 필터 | `MedianBlurFilter` (직접 구현) + `medianBlurFilterOf` 팩토리 + `require(radius >= 0)` + 골든 이미지 테스트 |
| **T9** | DSL 멤버 | 색/톤 보정 DSL 멤버 함수 (brightness/contrast/gamma/hsb/rgb/opacity/threshold/posterize/gainBias/saturation/hue/colorTemperature) + 단위 테스트 |
| **T10** | DSL 멤버 | 톤/스타일 preset DSL 멤버 (sepia/grayscale/invert/vintage/chrome/nashville/gotham/summer/oldPhoto) + 단위 테스트 |
| **T11** | DSL 멤버 | 블러/선명도 DSL 멤버 (blur/gaussianBlur/motionBlur/sharpen/unsharp/noiseReduction) + 단위 테스트 |
| **T12** | DSL 멤버 | 효과 DSL 멤버 (oil/crystallize/pixelate/border/vignette/glow/lensFlare/roundedCorners/medianBlur) + 단위 테스트 |
| **T13** | DSL 통합 | `watermark` / `caption` DSL 멤버 (기존 팩토리 위임) |
| **T14** | 테스트 | applyFilters mutation 격리 검증, 사용자 선언 순서 보존, sequential 비교 동등성 |
| **T15** | 테스트 | suspendApplyFilters dispatcher 검증 (`Thread.currentThread().name shouldContain "DefaultDispatcher"`) + 취소 안전성 |
| **T16** | 테스트 | PipelineCompactionTest — 결과 기반 동등성 검증, 빈 ops/Pixel-only 경로 |
| **T17** | 테스트 | 체인 골든 이미지 3+ (대표 시나리오: 색보정 / 빈티지 효과 / 워터마크 합성) |
| **T18** | 문서 | `README.md` 업데이트 — DSL 섹션, 사용 예제, 필터 카탈로그 표, Mermaid UML |
| **T19** | 문서 | `README.ko.md` 동기 업데이트 |
| **T20** | 검증 | `./gradlew :bluetape4k-images:test :bluetape4k-images:detekt` 전수 통과 |
| **T21** | 리뷰 | `oh-my-claudecode:code-reviewer` 에이전트 실행 → HIGH/CRITICAL 해소 |
| **T22** | 마무리 | `docs/superpowers/index/2026-04.md` 항목 추가, PR 생성 |

---

## 11. 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v1 | 2026-04-26 | Draft 초안 |
| v2 | 2026-04-26 | Issue #131 대조 반영: MedianBlurFilter 1차 포함, saturation API를 factor 곱셈 방식으로 통일, gamma Double 타입 수정, VignetteFilter 파라미터 명확화, ColorSpaceConverter public 공개, toHsvArray/toYCbCrArray 추가, watermark/caption DSL 통합, 신규 필터 5종으로 업데이트 |
| v3 | 2026-04-26 | Spec Review 반영: mutation 격리 재설계(`source.copy()` 진입점 1회), saturation factor 통일(전 섹션), ColorSpaces 단일화(public object ColorSpaceConverter), Float/Double 컨벤션 명시(§4.12), PipelineCompactionTest 결과 기반 재설계, suspendApplyFilters dispatcher 검증 강화, 에러 처리 정책(§5b) 추가, Issue #131 대조표(§9b) 추가 |
| v4 | 2026-04-26 | watermark/caption DSL 시그니처 확정: 오버로드 2개 (COVER/STAMP, x/y 좌표). medianBlur 기본값 radius=1 유지 |

---

(끝 — v3, Spec Review 반영 완료)
