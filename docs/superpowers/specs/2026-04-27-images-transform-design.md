# utils/images 변환/조작 API 설계 — Issue #132

- **Issue**: #132
- **모듈**: `utils/images` (`bluetape4k-images`)
- **scrimage 버전**: 4.3.10
- **작성일**: 2026-04-27
- **브랜치**: `feat/issue-132-images-transform`
- **상태**: Approved (Step 2-R 통과, Step 3 대기)

---

## 1. 배경 및 목적

### 1.1 현재 상황

`utils/images` 모듈은 Issue #131 까지 다음 기능을 갖춘다.

- 로딩/저장/스케일/스플릿 (`ImmutableImageSupport`, `scaler/`, `splitter/`)
- 70+ 필터 + 색보정 DSL (`filters/`, `filters/dsl/ImageFilterChain`)
- 이미지 유사도 (`similarity/`)
- 코루틴 비동기 I/O (`coroutines/`)

scrimage 자체에는 `rotate(radians: Double)`, `rotateLeft()`, `rotateRight()`, `flipX()`, `flipY()`, `subimage(x, y, w, h)` 등이 이미 존재한다. 그러나 다음 5종의
**고수준 변환/조작**은 모듈 외부 사용자가 직접 구현해야 했다.

1. **AutoCrop**: 사진 가장자리에 균일한 배경 (흰색/투명/단색) 여백을 자동으로 잘라내기
2. **Smart Crop**: 이미지의 시각적으로 중요한 영역을 보존하면서 지정 종횡비로 크롭
3. **임의 각도 회전 + 좌우/상하 반전**: 사용자 친화적인 도 (degree) 단위 회전 + 경계 자동 확장
4. **Perspective Transform**: 4점 변환 (예: 책 표지를 정면으로 펴기) — 3×3 호모그래피 + 역매핑
5. **Histogram Equalization (CLAHE)**: 휘도 채널 대비 보정으로 어두운 사진 가시성 향상

### 1.2 도입 효과

1. **선언적 변환 파이프라인**: 필터 DSL과 동일한 컨벤션으로 `applyFilters { autoCrop(); rotate(15.0); clahe() }` 형태 호출
2. **확장 함수 + DSL 양 계층
   노출**: 단발 사용은 `image.autoCrop()`, 체인 사용은 DSL — 기존 `ImmutableImageSupport`/`WatermarkFilterSupport`/`filters/dsl` 와 동일한 이중 표면 (dual surface) 패턴 유지
3. **Pure
   JVM**: BoofCV / Jama / OpenCV 등 외부 의존성 없이 Java2D `AffineTransform` + `BufferedImage.raster` + `DoubleArray` 기반 구현 → `build.gradle.kts` 의존성 변동 없음
4. **suspend 친화**: 무거운 픽셀 연산은 `Dispatchers.Default`에서 실행 (`suspend*` 변형 + `suspendApplyFilters` 호환)
5. **종횡비/색상/품질 보존 권장
   기본값**: AutoCrop tolerance, SmartCrop 후보 윈도우, 회전 보간 (BICUBIC), CLAHE clipLimit 등 OpenCV/scrimage 관례 기반의 안전한 기본값

### 1.3 비목표 (Non-goals)

- ML/딥러닝 기반 saliency / 얼굴 검출 — Smart Crop은 **휴리스틱 saliency**(Sobel 에너지) 사용
- ImageMagick / OpenCV 수준의 모든 변환 옵션
- 렌즈 왜곡 (distortion) 보정 — 4점 호모그래피만 지원
- LAB / sRGB linear 변환의 정밀 캘리브레이션 — CLAHE는 ITU-R BT.601 휘도 (Y' = 0.299R + 0.587G + 0.114B) 사용
- GPU 가속

---

## 2. 설계 위험 및 실패 모드

### Risk-1: 필터 체인 안의 dimension 변경

`ImageFilterChain.Op.Pixel` 은 `(ImmutableImage) -> ImmutableImage` 임의 변환을 허용하므로 **도중에 width/height 가
바뀌어도** 체인은 정상 진행된다. 그러나 후속 `Op.Native` 필터가 변경된 bounds 에서 동작한다는 점을 사용자가 인지하지 못할 수 있다.

- **실패 모드**: `applyFilters { rotate(45.0); vignette() }` — 비네트가 회전 후의 확장된 바운딩 박스에 적용되어 의도한 원본 프레임이 아니다.
- **완화**:
    - DSL 함수 KDoc에 **"체인 순서가 결과에 영향을 준다"**, **"다음 필터는 변환 후 bounds 에서 동작"** 명시
    - README 예제에서 변환을 마지막에 두는 권장 패턴 제시
    - Op 자체는 그대로 유지 (방어 차단은 과도한 abstraction)

### Risk-2: 임의 각도 회전 시 bounds 처리

scrimage 의 `ImmutableImage.rotate(radians)` 는 **이미 bounds 를 자동으로 확장**한다 (scrimage 4.3.10 검증 — `Image.scala`).

새 API `rotateDegrees` 가 추가하는 가치:

1. **도 (degree) 단위 각도** — scrimage `rotate(radians)` 와 달리 사용자 친화적
2. **투명 기본 배경** — scrimage 는 검정 배경, 새 API 는 `Color(0, 0, 0, 0)` (알파 0) 기본값
3. **편의 alias** — `flipHorizontal()` / `flipVertical()`

- **완화**:
    - `rotateLeft()`/`rotateRight()`/`flipHorizontal()`/`flipVertical()` 은 차원 변경이 자명하므로 단순 위임

### Risk-3: AutoCrop 의 "배경 없음" 케이스

이미지 전체가 배경이거나 (완전 단색), 가장자리가 균일하지 않은 복잡한 사진 (자연 풍경)에서는 잘라낼 영역이 없거나 과도하게 잘릴 수 있다.

- **실패 모드 A (전체 배경)**: 너비 0 또는 높이 0 의 잘못된 이미지 반환
- **실패 모드 B (배경 없음)**: 기대했던 여백 제거가 일어나지 않거나, 작은 노이즈 픽셀로 인해 거의 자르지 않음
- **완화**:
    - 코너 4픽셀의 평균색을 배경 후보로 사용 (KDoc에 명시) — `backgroundColor` 파라미터로 명시적 지정도 허용
    - `tolerance: Int = 10` (RGB 채널 절대 차이) 파라미터 — OpenCV `cv2.threshold` 관행과 유사
    - 결과 영역이 너무 작으면 (width < 1 || height < 1) 원본을 그대로 반환 — `Result<T>`/예외 대신 silent fallback (기존 모듈 컨벤션 일치)
    - silent fallback 경로에서는 `logger.debug` 로 fallback 이유 + 이미지 크기를 출력한다
    - 패딩 보존 옵션: `padding: Int = 0` — 잘라낸 영역의 사방에 픽셀 추가

### Risk-4: Smart Crop 의 "saliency 정확도" 오해

휴리스틱 (Sobel 엣지 에너지) 기반 SmartCrop 은 **얼굴/사물 인식이 아닌 고주파 영역
우선**이다. 텍스처가 강한 배경, 균일한 색의 주제 (예: 단색 옷의 인물)에서는 의도와 다른 영역이 선택될 수 있다.

- **실패 모드**: 인물 사진에서 얼굴 대신 텍스처가 강한 옷이나 배경이 선택됨
- **완화**:
    - KDoc에 **"휴리스틱 saliency"** 임을 명시. "deep learning 기반 객체 감지가 아님" 경고
    - 함수 이름: `smartCrop` — 단어 자체가 휴리스틱적임을 시사. 추후 다른 알고리즘 추가 여지를 위해 `strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy` sealed interface 노출
    - 디폴트 다운샘플 (긴 변 256px) 후 saliency 계산 → 원본 좌표로 복원. 큰 이미지에서도 100ms 이내 보장

### Risk-5: Perspective Transform 의 8×8 선형 시스템 풀이 정확도

8×8 시스템 (픽셀 좌표 4쌍 → 호모그래피 8개 미지수)은 floats 으로 풀면 누적 오차로 결과 좌표가 ±1픽셀 흔들릴 수 있다. 또한 forward mapping 으로 구현하면 출력에 hole 이 생긴다.

- **실패 모드 A (precision)**: float 으로 풀어 입력 4점이 거의 일직선일 때 (degenerate) 발산
- **실패 모드 B (forward mapping)**: 출력 픽셀 사이에 빈 공간 발생
- **완화**:
    - **`DoubleArray(64)` Gauss-Jordan 부분 피벗팅** 으로 직접 구현 (~30라인) — 정밀도 + 0 의존성
    - degenerate 시 `IllegalArgumentException("source points are nearly collinear")` 명시적 throw
    - **inverse mapping + bilinear 샘플링**: 출력 픽셀 → 호모그래피 역행렬 → 입력 좌표 → 4-tap bilinear → 색상
    - 입력 영역 밖이면 `outsideColor: Color = TRANSPARENT` 채움

### Risk-6: CLAHE 의 색공간 처리

각 RGB 채널을 독립 평활화하면 채도/색상이 뒤틀린다. 휘도 (Y) 채널만 평활화 후 RGB 복원이 표준.

- **실패 모드**: RGB 독립 처리 → 보색 변경, 회색 빨갛게 변함
- **완화**:
    - **YCbCr 공간 변환** → Y 채널에만 CLAHE → YCbCr→RGB 복원
    - 타일 크기 8×8, clipLimit 2.0 (OpenCV 기본값) 디폴트
    - tileSize 가 이미지보다 크면 단일 타일 (= global histogram equalization) 로 폴백
    - silent fallback 경로에서는 `logger.debug` 로 fallback 이유 + 이미지 크기를 출력한다

### Risk-7: 원본 이미지 mutation

기존 DSL 의 `compactAndApply` 는 진입 시 `source.copy()` 1회 방어 복사한다. 새 변환 ops 는 `Op.Pixel` 람다로 등록되며, 람다 내부에서 새 `BufferedImage` 를 생성해 `ImmutableImage.wrapAwt(...)` 로 감싸 반환하므로 mutation 위험 없음.

- 단, 신규 코드에서도 **항상 새 `BufferedImage` 인스턴스에 그리고 `ImmutableImage.wrapAwt(...)` 로 반환** 하는 컨벤션 KDoc/README 에 명시

### Risk-8: 대용량 출력 메모리 할당

`perspectiveTransform` 에 매우 큰 `outputWidth` × `outputHeight` 를 지정하면 수 GB 의 BufferedImage 가 할당될 수 있다.

- **실패 모드**: `OutOfMemoryError` 또는 GC pause 로 인한 응답 지연
- **완화**:
    - `require(outputWidth.toLong() * outputHeight.toLong() <= MAX_OUTPUT_PIXELS)` 사전 검증
    - `MAX_OUTPUT_PIXELS = 67_108_864L` (64M pixels = ~256MB ARGB)
    - 초과 시 `IllegalArgumentException` throw — KDoc 에 명시

---

## 3. 접근 방법 비교

세 가지 가능한 통합 방식을 검토한다. **요점은 알고리즘 선택이 아니라 API 표면 구조**다.

### Approach A: 확장 함수만 (Extension-only)

```kotlin
fun ImmutableImage.autoCrop(...): ImmutableImage
fun ImmutableImage.smartCrop(...): ImmutableImage
fun ImmutableImage.rotateDegrees(...): ImmutableImage
fun ImmutableImage.perspective(...): ImmutableImage
fun ImmutableImage.clahe(...): ImmutableImage
```

- **장점**: 단순. DSL 결합도 0.
- **단점**: 기존 필터 DSL 과 체인 불가. 사용자는 `image.applyFilters { ... }.rotateDegrees(...).applyFilters { ... }` 같은 수동 체인 필요.

### Approach B: DSL 전용 (DSL-only)

```kotlin
// 오직 ImageFilterChain 안에서만 호출
image.applyFilters {
    autoCrop()
    smartCrop(AspectRatio.WIDESCREEN)
    rotateDegrees(15.0)
    clahe()
}
```

- **장점**: 구현 1회. 모든 변환이 체인의 일급 시민.
-

**단점**: 단발 사용이 어색 (`image.applyFilters { autoCrop() }` 의 noise). 기존 모듈 컨벤션과 불일치 — `ImmutableImageSupport`/`WatermarkFilterSupport` 는 모두 top-level extension 도 노출.

### Approach C: 양 계층 (Both layers) — **선택**

```kotlin
// 1) Top-level extension function (단발 사용)
fun ImmutableImage.autoCrop(...): ImmutableImage = ...

// 2) DSL op (체인 사용) — extension 으로 위임
fun ImageFilterChain.autoCrop(...) {
    addPixel { it.autoCrop(...) }
}

// 3) suspend variant (코루틴)
suspend fun ImmutableImage.suspendAutoCrop(...): ImmutableImage =
    withContext(Dispatchers.Default) { autoCrop(...) }
```

- **장점**:
    - 기존 모듈 패턴 (`oil()`, `pixelate()`, `vignette()` 모두 DSL + 직접 `applyFilters { raw(OilFilter()) }` 양 방향) 일관성
    - 단발/체인/코루틴 모두 자연스러움
    - DSL op 가 extension 으로 위임 → 알고리즘 코드 1회만 작성
- **단점**: 파일 수가 약간 더 많음 (feature 당 1 main + 1 DSL ops 합본). 큰 비용 아님

**선택: Approach C**. 기존 패턴 (`ImageFilterChainEffectOps.kt` ↔ `RoundedCornerFilter.kt`/`MedianBlurFilter.kt`) 과 동일.

---

## 4. 패키지 레이아웃

```
src/main/kotlin/io/bluetape4k/images/transforms/
├── AutoCrop.kt                  # autoCrop / suspendAutoCrop + 내부 헬퍼
├── SmartCrop.kt                 # smartCrop / suspendSmartCrop + SaliencyStrategy + AspectRatio
├── Rotation.kt                  # rotateDegrees / flipHorizontal / flipVertical / rotateLeft / rotateRight 위임
├── PerspectiveTransform.kt      # perspectiveTransform + 호모그래피 풀이 헬퍼
├── HistogramEqualization.kt     # clahe / globalEqualize + YCbCr 변환 헬퍼
└── dsl/
    └── ImageFilterChainTransformOps.kt  # ImageFilterChain.{autoCrop,smartCrop,rotateDegrees,...}
```

테스트:

```
src/test/kotlin/io/bluetape4k/images/transforms/
├── AutoCropTest.kt
├── SmartCropTest.kt
├── RotationTest.kt
├── PerspectiveTransformTest.kt
├── HistogramEqualizationTest.kt
└── dsl/
    └── ImageFilterChainTransformOpsTest.kt
```

기존 `AbstractImageTest` / `AbstractFilterTest` 활용.

---

## 5. API 설계

> 모든 public API는 KDoc + `@param` + 예제 포함. 한국어 KDoc.

### 5.1 AutoCrop

```kotlin
package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color

/**
 * 이미지 가장자리의 단색 배경 여백을 자동으로 잘라냅니다.
 *
 * ## 동작/계약
 * - [backgroundColor] 가 null 이면 좌상/우상/좌하/우하 4픽셀 평균색을 배경으로 사용합니다.
 * - 배경과 모든 RGB 채널의 절대 차이가 [tolerance] 이하인 픽셀을 배경으로 간주합니다.
 * - 잘라낸 영역의 4면에 [padding] 픽셀의 여백을 추가합니다.
 * - 결과 width 또는 height 가 1 미만이면 원본을 반환합니다 (silent fallback).
 * - silent fallback 시 `logger.debug` 로 fallback 이유와 이미지 크기를 출력합니다.
 *
 * ```kotlin
 * val cropped = image.autoCrop(tolerance = 10, padding = 4)
 * // cropped.width <= image.width
 * ```

*
* @param tolerance 배경으로 간주할 RGB 채널 최대 차이 (0..255, 기본값 10)
* @param padding 잘라낸 영역에 추가할 사방 여백 픽셀 (기본값 0)
* @param backgroundColor 명시적 배경색. null 이면 코너 평균색 자동 검출
* @return 여백이 제거된 새 [ImmutableImage]
  */ fun ImmutableImage.autoCrop (tolerance: Int = 10, padding: Int = 0, backgroundColor: Color? = null,
  ): ImmutableImage

suspend fun ImmutableImage.suspendAutoCrop (tolerance: Int = 10, padding: Int = 0, backgroundColor: Color? = null,
): ImmutableImage = withContext (Dispatchers.Default) { autoCrop (tolerance, padding, backgroundColor)
}

```

### 5.2 SmartCrop

```kotlin
/** SmartCrop 의 saliency 계산 전략. 추후 확장을 위해 sealed interface 로 정의합니다. */
sealed interface SaliencyStrategy {
    /** Sobel 엣지 magnitude 합. 휴리스틱이며 빠름. */
    object SobelEnergy : SaliencyStrategy
}

/**
 * 종횡비를 나타내는 값 클래스.
 *
 * x는 열(column), y는 행(row) 방향입니다.
 *
 * ```kotlin
 * val ratio = AspectRatio(16, 9)
 * val square = AspectRatio.SQUARE
 * ```

*
* @param width 종횡비 너비 (양수)
* @param height 종횡비 높이 (양수)
  */ data class AspectRatio (val width: Int, val height: Int) { companion object { val SQUARE = AspectRatio (1, 1)
  val WIDESCREEN = AspectRatio (16, 9)
  val PORTRAIT = AspectRatio (9, 16)
  val STANDARD = AspectRatio (4, 3)
  } }

/**

* 시각적으로 중요한 영역을 보존하면서 지정 종횡비로 크롭합니다.
*
* ## 동작/계약
*
    - [strategy] 알고리즘으로 saliency map 을 계산하고, [aspectRatio] 비율의 후보 윈도우 중
* 에너지 합이 최대인 영역을 반환합니다.
*
    - 큰 이미지는 긴 변 256px 로 다운샘플링 후 계산하여 100ms 이내를 목표로 합니다.
*
    - 결과 영역의 좌표는 원본 해상도로 복원됩니다.
*
    - **휴리스틱**입니다: 얼굴/객체 검출이 아닌 고주파 영역 기준입니다.
*
* ```kotlin
* // 16:9 종횡비로 크롭
* val banner = image.smartCrop (AspectRatio.WIDESCREEN)
* // 1:1 정사각형 썸네일
* val square = image.smartCrop (AspectRatio.SQUARE)
* ```
*
* @param aspectRatio 결과 종횡비 ([AspectRatio])
* @param strategy saliency 계산 전략 (기본값 [SaliencyStrategy.SobelEnergy])
* @return 휴리스틱 saliency 기준 최적 영역의 [ImmutableImage]
  */ fun ImmutableImage.smartCrop (aspectRatio: AspectRatio, strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
  ): ImmutableImage

/**

* 시각적으로 중요한 영역을 보존하면서 정확한 픽셀 크기로 크롭합니다.
*
* ```kotlin
* val thumb = image.smartCropTo (320, 240)
* ```
*
* @param width 결과 너비 (양수)
* @param height 결과 높이 (양수)
* @param strategy saliency 계산 전략 (기본값 [SaliencyStrategy.SobelEnergy])
* @return 휴리스틱 saliency 기준 최적 영역의 [ImmutableImage]
  */ fun ImmutableImage.smartCropTo (width: Int, height: Int, strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
  ): ImmutableImage

suspend fun ImmutableImage.suspendSmartCrop (aspectRatio: AspectRatio, strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
): ImmutableImage = withContext (Dispatchers.Default) { smartCrop (aspectRatio, strategy)
}

```

### 5.3 Rotation / Flip

```kotlin
/**
 * 도(degree) 단위로 이미지를 회전합니다.
 *
 * ## 동작/계약
 * - 양의 [angle] 은 시계 방향 회전입니다.
 * - scrimage 의 `rotate(radians)` 는 이미 bounds 를 자동 확장합니다. 이 함수는 추가로:
 *   (1) 도(degree) 단위 각도를 지원합니다.
 *   (2) 투명(`Color(0, 0, 0, 0)`) 배경을 기본값으로 사용합니다.
 *   (3) `flipHorizontal()`/`flipVertical()` 편의 alias 를 제공합니다.
 * - 90도 배수는 [ImmutableImage.rotateLeft]/[ImmutableImage.rotateRight] 로 위임하여 무손실 처리합니다.
 *
 * ```kotlin
 * val tilted = image.rotateDegrees(15.0)
 * val withBg = image.rotateDegrees(45.0, background = Color.WHITE)
 * ```

*
* @param angle 회전 각도 (도)
* @param background 빈 영역 채우기 색 (기본값 알파 0 = 투명)
* @return 회전된 새 [ImmutableImage]
  */ fun ImmutableImage.rotateDegrees (angle: Double, background: Color = Color (0, 0, 0, 0),
  ): ImmutableImage

/** 좌우 반전 (alias for [ImmutableImage.flipX]). */ fun ImmutableImage.flipHorizontal (): ImmutableImage = flipX ()

/** 상하 반전 (alias for [ImmutableImage.flipY]). */ fun ImmutableImage.flipVertical (): ImmutableImage = flipY ()

suspend fun ImmutableImage.suspendRotateDegrees (angle: Double, background: Color = Color (0, 0, 0, 0),
): ImmutableImage = withContext (Dispatchers.Default) { rotateDegrees (angle, background)
}

```

### 5.4 Perspective Transform

```kotlin
/**
 * 이미지의 한 점. 좌상이 (0, 0).
 *
 * x는 열(column), y는 행(row) 방향입니다.
 */
data class ImagePoint(val x: Double, val y: Double)

/**
 * 4점 원근 변환(homography)을 적용합니다.
 *
 * ## 동작/계약
 * - [sourceCorners] 의 4점이 [destinationCorners] 의 4점으로 매핑되도록 3×3 호모그래피를 계산합니다.
 * - 점 순서는 시계방향: 좌상 → 우상 → 우하 → 좌하.
 * - 출력 캔버스 크기는 [outputWidth] × [outputHeight] 입니다.
 * - 출력 최대 크기: `outputWidth * outputHeight <= 67_108_864` (64M pixels, ~256MB ARGB).
 * - inverse mapping + bilinear 샘플링으로 hole 없는 결과를 보장합니다.
 * - 입력 영역 밖은 [outsideColor] 로 채워집니다.
 * - 4점이 거의 일직선이면 [IllegalArgumentException] 을 던집니다.
 *
 * ```kotlin
 * // 기울어진 책 표지를 정면 사각형으로 변환
 * val flat = image.perspectiveTransform(
 *     sourceCorners = listOf(
 *         ImagePoint(120.0, 80.0),  ImagePoint(540.0, 100.0),
 *         ImagePoint(560.0, 420.0), ImagePoint(100.0, 400.0),
 *     ),
 *     destinationCorners = listOf(
 *         ImagePoint(0.0,   0.0),   ImagePoint(400.0, 0.0),
 *         ImagePoint(400.0, 600.0), ImagePoint(0.0,   600.0),
 *     ),
 *     outputWidth = 400,
 *     outputHeight = 600,
 * )
 * ```

*
* @param sourceCorners 원본 이미지의 4점 (시계방향)
* @param destinationCorners 출력 이미지의 4점 (시계방향)
* @param outputWidth 출력 너비
* @param outputHeight 출력 높이
* @param outsideColor 입력 밖 영역 채우기 색 (기본값 알파 0)
* @return 변환된 새 [ImmutableImage]
* @throws IllegalArgumentException 점 개수가 4 가 아니거나, 출력 크기가 0 이하이거나,
* 출력 크기가 64M pixels 초과이거나, sourceCorners 또는 destinationCorners 중 하나가 거의 일직선인 경우
  */ fun ImmutableImage.perspectiveTransform (sourceCorners: List<ImagePoint>, destinationCorners: List<ImagePoint>, outputWidth: Int, outputHeight: Int, outsideColor: Color = Color (0, 0, 0, 0),
  ): ImmutableImage

suspend fun ImmutableImage.suspendPerspectiveTransform (sourceCorners: List<ImagePoint>, destinationCorners: List<ImagePoint>, outputWidth: Int, outputHeight: Int, outsideColor: Color = Color (0, 0, 0, 0),
): ImmutableImage = withContext (Dispatchers.Default) { perspectiveTransform (sourceCorners, destinationCorners, outputWidth, outputHeight, outsideColor)
}

```

### 5.5 Histogram Equalization (CLAHE)

```kotlin
/**
 * CLAHE (Contrast Limited Adaptive Histogram Equalization) 로 휘도 채널을 평활화합니다.
 *
 * ## 동작/계약
 * - RGB → YCbCr 변환 후 Y(휘도) 채널에만 CLAHE 를 적용하고 RGB 로 복원합니다 (채도 보존).
 * - 이미지를 [tileSize] × [tileSize] 타일로 나누어 각 타일의 히스토그램을 [clipLimit] 로 제한 후 평활화합니다.
 * - 타일 경계는 bilinear interpolation 으로 부드럽게 처리합니다.
 * - 이미지가 [tileSize] 보다 작으면 단일 타일(전역 평활화)로 폴백합니다.
 *
 * ```kotlin
 * // 어두운 사진의 가시성 향상
 * val clearer = image.clahe()
 * // 강한 대비 (clipLimit 가 높을수록 강한 평활화)
 * val strong = image.clahe(tileSize = 4, clipLimit = 4.0)
 * ```

*
* @param tileSize 타일 한 변의 픽셀 수 (1 이상, 기본값 8)
* @param clipLimit 히스토그램 클리핑 한계 (양수, 기본값 2.0)
* @return 평활화된 새 [ImmutableImage]
  */ fun ImmutableImage.clahe (tileSize: Int = 8, clipLimit: Double = 2.0,
  ): ImmutableImage

/**

* 전역 히스토그램 평활화. CLAHE 의 단순 변형으로 [clahe] 를 [tileSize] = max (width,height) 로 호출한 것과 동일.
  */ fun ImmutableImage.globalEqualize (): ImmutableImage

suspend fun ImmutableImage.suspendClahe (tileSize: Int = 8, clipLimit: Double = 2.0,
): ImmutableImage = withContext (Dispatchers.Default) { clahe (tileSize, clipLimit) }

```

### 5.6 DSL 통합

```kotlin
package io.bluetape4k.images.transforms.dsl

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.filters.dsl.ImageFilterChain
import io.bluetape4k.images.transforms.*
import java.awt.Color

/** 가장자리 단색 여백 자동 제거. 자세한 동작은 [ImmutableImage.autoCrop] 참조. */
fun ImageFilterChain.autoCrop(
    tolerance: Int = 10,
    padding: Int = 0,
    backgroundColor: Color? = null,
) {
    addPixel { it.autoCrop(tolerance, padding, backgroundColor) }
}

/**
 * 휴리스틱 saliency 기반 종횡비 크롭. 자세한 동작은 [ImmutableImage.smartCrop] 참조.
 *
 * 변환 ops 는 예외 발생 시 op 이름과 입력 요약을 warn 로그로 출력합니다.
 */
fun ImageFilterChain.smartCrop(
    aspectRatio: AspectRatio,
    strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
) {
    addPixel { it.smartCrop(aspectRatio, strategy) }
}

/**
 * 도 단위 회전. 자세한 동작은 [ImmutableImage.rotateDegrees] 참조.
 *
 * 변환 ops 는 예외 발생 시 op 이름과 입력 요약을 warn 로그로 출력합니다.
 */
fun ImageFilterChain.rotateDegrees(
    angle: Double,
    background: Color = Color(0, 0, 0, 0),
) {
    addPixel { it.rotateDegrees(angle, background) }
}

/** 좌로 90도 회전. */
fun ImageFilterChain.rotateLeft() {
    addPixel { it.rotateLeft() }
}

/** 우로 90도 회전. */
fun ImageFilterChain.rotateRight() {
    addPixel { it.rotateRight() }
}

/** 좌우 반전. */
fun ImageFilterChain.flipHorizontal() {
    addPixel { it.flipHorizontal() }
}

/** 상하 반전. */
fun ImageFilterChain.flipVertical() {
    addPixel { it.flipVertical() }
}

/** 4점 원근 변환. 자세한 동작은 [ImmutableImage.perspectiveTransform] 참조. */
fun ImageFilterChain.perspectiveTransform(
    sourceCorners: List<ImagePoint>,
    destinationCorners: List<ImagePoint>,
    outputWidth: Int,
    outputHeight: Int,
    outsideColor: Color = Color(0, 0, 0, 0),
) {
    addPixel {
        it.perspectiveTransform(sourceCorners, destinationCorners, outputWidth, outputHeight, outsideColor)
    }
}

/** CLAHE. 자세한 동작은 [ImmutableImage.clahe] 참조. */
fun ImageFilterChain.clahe(tileSize: Int = 8, clipLimit: Double = 2.0) {
    addPixel { it.clahe(tileSize, clipLimit) }
}
```

> **주의**: 변환 ops 는 모두 `Op.Pixel` 로 등록된다. 인접한 scrimage Native 필터와 자연스럽게 섞이며,
> 변환 op 는 `PipelineFilter` 그룹화의 경계가 된다 (`compactAndApply` 의 기존 동작과 일치).
>
> **예외 로깅**: 각 변환 op 는 예외 발생 시 op 이름과 입력 이미지 크기 (width×height)를 warn 로그로 출력한다.

### 5.7 사용 예시

```kotlin
// 단발 사용
val cropped = image.autoCrop(tolerance = 8)
val rotated = image.rotateDegrees(15.0, background = Color.WHITE)

// 체인 사용 — 필터 + 변환 혼합
val processed = image.applyFilters {
    clahe()                             // 어두움 보정
    autoCrop()                          // 여백 제거
    smartCrop(AspectRatio.WIDESCREEN)   // 16:9 크롭
    rotateDegrees(2.0)                  // 살짝 기울이기
    vignette()                          // 비네트 (변환 후 bounds 기준)
    border(thickness = 4, color = Color.BLACK)
}

// 1:1 정사각형 썸네일
val thumb = image.applyFilters {
    smartCrop(AspectRatio.SQUARE)
}

// 코루틴
val async = image.suspendApplyFilters {
    clahe()
    smartCrop(AspectRatio.SQUARE)
}
```

---

## 6. 구현 접근

### 6.1 AutoCrop

1. `image.awt()` → `BufferedImage` 직접 raster 접근 (`getRGB(int[], ...)`)
2. 코너 4픽셀 평균색을 RGB 정수로 계산 (또는 `backgroundColor` 사용)
3. 위쪽 행부터 스캔 — 모든 픽셀이 배경 ± tolerance 인 행은 잘라낼 후보
4. 같은 방식으로 아래/왼쪽/오른쪽 경계 결정 → `(top, bottom, left, right)`
5. `padding` 만큼 양보 (단, 원본 경계 안으로 클램프)
6. `image.subimage(left, top, w, h)` 반환. silent fallback: `w < 1 || h < 1` 시 `logger.debug("autoCrop silent fallback: ...")` 후 원본 반환
7. 채널 비교는 절대값 차이로 단순 비교 (lightweight)

성능 목표: 1920×1080 RGB 이미지 < 50ms (단일 스레드, 4행 동시 비교 가능 시 < 30ms)

### 6.2 SmartCrop

1. 다운샘플: 긴 변 256px 로 `image.scaleTo(...)` (scrimage 내장)
2. Sobel 3×3 (Gx, Gy) → 그레이스케일에 적용 → magnitude = |Gx| + |Gy| (L1, fast)
3. **integral image** (적분 이미지) 계산 — `IntArray((w+1) * (h+1))`
4. 후보 윈도우 크기: aspect 비율 유지하며 다운샘플 영역에 들어가는 최대 크기
5. 적분 이미지로 모든 윈도우 위치 (슬라이드 stride 1)의 에너지 합을 O (1) 계산 → 최댓값 위치
6. 다운샘플 좌표 → 원본 좌표 스케일 복원
7. `image.subimage(...)` 반환

성능 목표: 4000×3000 사진 < 100ms (다운샘플 + Sobel + 슬라이드)

### 6.3 Rotation / Flip

`rotateDegrees`:

1. 90도 배수면 `rotateLeft()` / `rotateRight()` / `rotate(PI)` 위임 (무손실)
2. 임의 각도 (scrimage 가 bounds 자동 확장하므로 `radians` 변환 후 위임하되, 배경색을 지원하기 위해 직접 구현):
    - `radians = Math.toRadians(angle)`
    - 새 bounds 계산:
        - `newW = |w*cos(θ)| + |h*sin(θ)|`
        - `newH = |w*sin(θ)| + |h*cos(θ)|`
    - `BufferedImage(newW, newH, TYPE_INT_ARGB)` 생성
    - `Graphics2D.setBackground(background)` + `clearRect(0, 0, newW, newH)` 로 배경 채우기
    - `AffineTransform`: `translate(newW/2, newH/2) → rotate(radians) → translate(-w/2, -h/2)`
    - `setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)`
    - `g.drawImage(src, transform, null)`
    - `g.dispose()` (try/finally)

`flipHorizontal` / `flipVertical`: scrimage `flipX` / `flipY` 직접 위임.

### 6.4 Perspective Transform

1. 입력 검증:
    - `require(sourceCorners.size == 4)`, destinationCorners size 4, output > 0
    - `require(outputWidth.toLong() * outputHeight.toLong() <= MAX_OUTPUT_PIXELS)` — `MAX_OUTPUT_PIXELS = 67_108_864L`
2. **호모그래피 행렬 H (3×3) 풀이**:
    - 8개 미지수: `h11, h12, h13, h21, h22, h23, h31, h32` (h33 = 1 정규화)
    - 각 점 쌍 (sx, sy) → (dx, dy) 마다 2개 식:
        - `h11*sx + h12*sy + h13 - h31*sx*dx - h32*sy*dx = dx`
        - `h21*sx + h22*sy + h23 - h31*sx*dy - h32*sy*dy = dy`
    - 4점 → 8×8 시스템. `DoubleArray(64)` + `DoubleArray(8)` 우변
    - **Gauss-Jordan with partial
      pivoting**: 각 열에서 최대 절대값 행을 피벗 → row swap → 정규화 → 다른 행 제거. 작은 피벗 (< 1e-12) 만나면 `IllegalArgumentException("nearly collinear source points")` 던짐
    - 결과 8개 → 3×3 H 구성
3. **역행렬 Hinv** 계산 (다시 동일 Gauss-Jordan, augmented [H | I]) — inverse mapping 용
4. 출력 `BufferedImage(outputWidth, outputHeight, TYPE_INT_ARGB)` 생성, `outsideColor` 채움
5. 출력 픽셀 (x, y) 마다:
    - `[x', y', w'] = Hinv * [x, y, 1]`
    - `srcX = x'/w', srcY = y'/w'`
    - `(srcX, srcY)` 가 [0, w-1] × [0, h-1] 안이면 **bilinear 4-tap 샘플링** (네 코너 가중평균)
    - 밖이면 `outsideColor` 유지
6. `ImmutableImage.wrapAwt(...)` 반환

> **이유**: pure JVM, double 정밀도, ~30~50라인의 헬퍼 함수. 외부 의존성 0.

### 6.5 CLAHE

1. RGB → YCbCr 변환 (각 픽셀 — `Y = 0.299R + 0.587G + 0.114B`, `Cb = -0.169R - 0.331G + 0.5B + 128`, `Cr = 0.5R - 0.419G - 0.081B + 128`)
2. Y 평면을 `IntArray(w*h)` 로 추출
3. 타일별 히스토그램 (각 256 bin) 계산
4. **클리핑**: 빈도가 `clipLimit * (tilePixelCount / 256)` 초과인 빈은 잘라내고, 잘린 양을 모든 빈에 균등 재분배
5. 누적분포 (CDF) → 매핑 함수 `lut[256]` 생성
6. 각 픽셀에 대해 4개 인접 타일의 LUT 를 **bilinear interpolation** 으로 매핑 (타일 경계 부드럽게)
7. Y' → YCbCr → RGB 복원
8. `ImmutableImage.wrapAwt(...)` 반환
9. 타일 크기 > 이미지 시 단일 타일로 폴백 (`globalEqualize()` 와 동일 경로) — `logger.debug` 로 fallback 이유 + 이미지 크기 출력

타일 크기 > 이미지 → 단일 타일 (`globalEqualize()` 와 동일 경로).

성능 목표: 1920×1080 RGB < 200ms (타일 8×8, JIT 후).

### 6.6 공통 헬퍼

`transforms/internal/RasterUtils.kt` (internal):

```kotlin
internal fun ImmutableImage.toIntArgb(): BufferedImage     // BufferedImage.TYPE_INT_ARGB 보장
internal fun BufferedImage.copyArgb(): BufferedImage       // 새 ARGB 복사본
internal fun BufferedImage.fill(color: Color): BufferedImage
```

각 transform 파일 내부에서만 사용. `companion object : KLoggingChannel()` 로깅.

---

## 7. 테스트 전략

### 7.1 테스트 케이스 (각 feature)

#### AutoCrop

- 흰색 여백 + 단색 사각형 합성 이미지 → 크롭 결과 정확히 사각형 영역
- 코너 자동 검출 vs 명시적 `backgroundColor = Color.WHITE` 결과 동일
- `padding = 5` 시 결과가 정확히 사방 +5 픽셀 큰지
- 완전 단색 이미지 → 원본 반환 (silent fallback)
- 자연 사진 (`landscape.jpg`) → tolerance 가 충분히 작으면 변경 없음, 충분히 크면 일부 잘림

#### SmartCrop

- 좌측 절반에 큰 텍스처 (체커보드), 우측 단색인 합성 이미지 → 좌측 영역 선택
- `AspectRatio.SQUARE` / `AspectRatio.WIDESCREEN` / `AspectRatio.PORTRAIT` 종횡비 모두 정상 동작
- 가로 긴 사진 → 결과 width/height 비율이 요청과 정확히 일치 (±1 픽셀)
- 빈 이미지 (흰색만) → 임의 위치 (정확성 검증 X, 크기만 검증)

#### Rotation

- 0도 회전 → 원본과 동일 (assertSimilarToImage)
- 90도 회전 = `rotateLeft()` 결과와 동일 (assertSimilarToImage)
- 180도 회전 두 번 → 원본과 동일 (tolerance ≤ 3, 보간 영향)
- 임의 각도 15도 → bounds 계산 결과가 수학식과 일치
- `flipX().flipX()` → 원본 동일

#### Perspective Transform

- 항등 매핑 (source = destination 사각형) → 원본 픽셀 동일
- 단순 회전 90도 효과를 호모그래피로 → `rotateRight()` 결과와 유사
- 잘못된 입력 (3점, 거의 일직선) → `IllegalArgumentException`
- 출력 크기 초과 (> 64M pixels) → `IllegalArgumentException`
- 출력 영역 밖 영역 → `outsideColor` 정확히 채워짐 (corner 픽셀 검증)

#### CLAHE

- 어두운 합성 이미지 (균일하게 RGB 50) → CLAHE 후 평균 휘도 증가
- 회색 그라데이션 이미지 → CLAHE 후 그라데이션이 더 가팔라짐 (히스토그램이 더 평탄)
- `tileSize` > 이미지 → `globalEqualize()` 와 결과 동일
- 채도 보존: 강한 색조의 이미지 (빨강 단색)에서 CLAHE 전후 평균 채도 변화 < 5% (Cb/Cr 채널 보존 검증)

### 7.2 DSL 테스트

`ImageFilterChainTransformOpsTest.kt`:

- 각 DSL op 가 단발 extension 호출과 동일 결과 (`assertSimilarToImage`)
- 변환 op + 네이티브 필터 혼합 체인 동작 (예: `applyFilters { autoCrop(); vignette() }`)
- 빈 체인 (`applyFilters { }`) — 원본 그대로 반환
- `rotateLeft()` / `rotateRight()` DSL op 정상 동작

### 7.3 코루틴 테스트

각 `suspend*` 변형:

- `runTest { ... }` 사용
- 동기 버전 결과와 동일한지 (`assertSimilarToImage`)
- `suspendApplyFilters { autoCrop(); rotateDegrees(...) }` 통합 시나리오 1건

### 7.4 성능 테스트 (선택)

JMH 없이 간이 측정 — 1920×1080 이미지 100회 반복 → 평균 < 목표 ms. 디버그 로그로 측정값 출력만 하고 `assertTrue(time < threshold)` 강제 검증은 하지 않음 (CI 환경 변동성).

### 7.5 픽셀 비교 컨벤션

기존 `AbstractFilterTest.assertSimilarToImage(actual, expected, tolerance)` 사용.

- 보간 결과 비교 시 `tolerance = 3`
- 무손실 변환 (flip, 90도 rotate) 비교 시 `tolerance = 0`

### 7.6 bluetape4k-assertions matcher 사용 (사용자 메모리 규칙)

```kotlin
result.width shouldBeGreaterThan 0
result.height shouldBeInRange (0..image.height)
// (x >= y).shouldBeTrue() 금지
```

### 7.7 테스트 리소스

기존 `src/test/resources/images/` 활용 (`homer.jpg`, `cafe.jpg`, `landscape.jpg`, `aqua.jpg`). 새 합성 이미지가 필요한 케이스는 테스트 안에서 `BufferedImage` 직접 생성 (외부 리소스 추가 없음).

---

## 8. 성능 / 스레드 안전성

### 8.1 성능 가이드

| Feature              | 입력                | 목표                             |
|----------------------|---------------------|----------------------------------|
| AutoCrop             | 1920×1080           | < 50ms                           |
| SmartCrop            | 4000×3000           | < 100ms (256px 다운샘플 후 계산) |
| rotateDegrees        | 1920×1080           | < 80ms (Java2D BICUBIC)          |
| perspectiveTransform | 1920×1080 → 800×600 | < 200ms (역매핑 + bilinear)      |
| CLAHE                | 1920×1080, 8×8 타일 | < 200ms                          |

핫패스 가이드라인:

- `getRGB(int[])` / `setRGB(int[])` 로 raster 직접 접근 (픽셀 단위 `getPixel` 호출 회피)
- 내부 루프에서 `Color` 객체 생성 금지 — int 비트 연산
- integral image / LUT 등 사전 계산 활용

### 8.2 스레드 안전성

- 모든 변환은 새 `BufferedImage`/`ImmutableImage` 를 반환 — **immutable, 동시 호출 안전**
- `Graphics2D` 는 `try { ... } finally { g.dispose() }` 패턴 적용
- 정적 상태 없음 (모든 함수가 pure)

### 8.3 메모리

- 일시적으로 입력 + 출력 + (CLAHE 의 경우) Y 평면 + 타일 히스토그램 = 약 입력 크기 × 3
- 4000×3000 ARGB 입력 → 약 144MB 추가. CLAHE 는 약 +60MB
- 메모리 예산 초과 가능성 있는 호출자는 입력 다운스케일 후 변환 권장 — KDoc 에 명시

### 8.4 코루틴 컨벤션

- 모든 `suspend*` 는 `withContext(Dispatchers.Default)` 래핑 (CPU bound)
- I/O 는 호출자가 관리 (`suspendImmutableImageOf` 와 결합)
- `CancellationException` 은 자동 전파 — 직접 catch 하지 않음

---

## 9. 영향 분석

### 9.1 기존 코드 변경

- `ImageFilterChain` 코드 수정 없음 — 기존 `addPixel` 진입점만 사용
- 기존 DSL ops 파일 수정 없음
- `build.gradle.kts` 변경 없음

**결정 사항 (Step 2-R 통과)**:

- `rotateDegrees` 에서 `expand` 파라미터 제거 — scrimage 가 이미 bounds 를 자동 확장함
- `AspectRatio` data class 도입 — `smartCrop(aspectWidth, aspectHeight)` 대신 타입 안전 API
- `Point2D` → `ImagePoint` 리네이밍 — 이미지 도메인 명확화
- `SaliencyStrategy` sealed interface 전환 — 미래 확장성

### 9.2 새 의존성

**없음**. 모든 알고리즘은 JDK + scrimage-core 만 사용.

### 9.3 호환성

- 신규 API 만 추가. 기존 `ImmutableImage.rotate` / `flipX` / `flipY` / `subimage` 동작은 그대로
- `rotateDegrees(angle)` 는 scrimage `rotate(radians)` 와 의미 동등 (도 단위 + 투명 배경 기본값 추가)

### 9.4 README 갱신 (`utils/images/README.md` + `README.ko.md`)

- "Transforms / 변환 / 조작" 섹션 추가
- Mermaid 다이어그램: ImageFilterChain 안에 변환 ops 가 새 카테고리로 추가됨을 시각화
- 주요 함수별 예제 1건씩

---

## 10. Definition of Done

### 10.1 구현

- [ ] `io.bluetape4k.images.transforms` 패키지 신규 생성
- [ ] `AutoCrop.kt` 구현 + KDoc + 한국어 주석
- [ ] `SmartCrop.kt` 구현 (Sobel + integral image) — `AspectRatio` data class 포함
- [ ] `Rotation.kt` 구현 (`rotateDegrees`, `flipHorizontal`, `flipVertical`)
- [ ] `PerspectiveTransform.kt` 구현 (Gauss-Jordan + inverse mapping + bilinear)
- [ ] `HistogramEqualization.kt` 구현 (CLAHE + globalEqualize)
- [ ] 모든 함수의 `suspend*` 변형
- [ ] `transforms/dsl/ImageFilterChainTransformOps.kt` — 9개 DSL op (autoCrop, smartCrop, rotateDegrees, rotateLeft, rotateRight, flipHorizontal, flipVertical, perspectiveTransform, clahe)
- [ ] `transforms/internal/RasterUtils.kt` (internal 헬퍼)
- [ ] perspectiveTransform 출력 크기 상한 검증 (64M pixels)
- [ ] 변환 ops 예외 시 op 이름 + 입력 요약 warn 로그
- [ ] silent fallback 경로 debug 로그 emission
- [ ] 모든 `Graphics2D` 사용은 `try { ... } finally { g.dispose() }` 패턴 적용

### 10.2 KDoc

- [ ] 모든 public 함수: KDoc + `@param` + `@return` + 사용 예제
- [ ] Risk 섹션의 주의사항 (휴리스틱, 차원 변경, expand 동작 등) KDoc 에 명시

### 10.3 테스트

- [ ] `AutoCropTest`, `SmartCropTest`, `RotationTest`, `PerspectiveTransformTest`, `HistogramEqualizationTest`
- [ ] `dsl/ImageFilterChainTransformOpsTest`
- [ ] 각 feature 동기 + suspend 양쪽 검증
- [ ] 80% 라인 커버리지
- [ ] `./gradlew :bluetape4k-images:test --tests "io.bluetape4k.images.transforms.*"` 전수 통과

### 10.4 빌드/검증

- [ ] `./gradlew :bluetape4k-images:build` 통과
- [ ] `./gradlew :bluetape4k-images:detekt` 통과
- [ ] `build.gradle.kts` 의존성 변경 없음 (검증)

### 10.5 문서

- [ ] `utils/images/README.md` "Transforms" 섹션 추가
- [ ] `utils/images/README.ko.md` 동기 갱신
- [ ] Mermaid 다이어그램 (변환 카테고리 포함) 1건 갱신/추가

### 10.6 워크플로우

- [ ] worktree `.worktrees/feat/issue-132-images-transform/` 안에서 작업
- [ ] Korean + prefix commit (`feat: utils/images 변환 API 추가 (#132)` 등)
- [ ] PR 본문에 테스트 결과 (passed count + 시간), 검증 명령, 변경 영향 명시
- [ ] OMC code-reviewer 통과 (CRITICAL/HIGH 0건)
- [ ] `/wiki-update` 실행

---

## 11. 향후 확장 (out of scope)

- Lanczos / Mitchell 보간 옵션 (현재 Java2D BICUBIC 고정)
- Saliency 전략 추가: 색상 분산, 코너 검출 (Harris), face-detection (외부 의존성 추가 시)
- `transforms` op 와 `Op.Native` 사이 자동 캐싱 (현재 `Op.Pixel` 사이마다 새 ImmutableImage)
- 4점 자동 검출 (책 표지 자동 변환의 사전 단계)
- LAB 색공간 기반 CLAHE (sRGB linear 캘리브레이션 포함)

---

## 12. 가정 (Assumptions)

본 spec 작성 시 결정된 사항 (Step 2-R 에서 사용자 승인 완료).

1. **AutoCrop 배경 검출**: 코너 4픽셀 평균 사용 (4-corner sampling). 가장자리 1행/열 평균 (edge sampling) 대안은 채택하지 않음 — 단순성 우선.
2. **SmartCrop saliency**: Sobel 엣지 magnitude 만 1차 구현. 색 분산/코너/얼굴 검출은 추후 sealed interface 확장.
3. **rotateDegrees**: `expand` 파라미터 없음 — scrimage 가 이미 bounds 를 자동 확장함. 도 (degree) 단위 + 투명 기본 배경 추가가 핵심 가치.
4. **회전 보간**: `BICUBIC` 고정. 미래에 enum 으로 노출 가능.
5. **CLAHE 색공간**: YCbCr (BT.601). HSL/LAB 대안은 1차 비대상.
6. **CLAHE 기본값**: tileSize 8, clipLimit 2.0 (OpenCV `cv2.createCLAHE` 기본값).
7. **Perspective 점 개수**: 정확히 4. N-point homography (least squares) 는 제외.
8. **Smart Crop 다운샘플 임계값**: 긴 변 256px. 80~512px 범위에서 결과 차이는 미미 (실험 검증으로 조정 가능).
9. **Op.Native vs Op.Pixel
   선택**: 5개 변환 모두 `Op.Pixel` (scrimage Filter 인터페이스로 표현하기 어려움 — bounds 변경, 4점 입력 등). PipelineFilter 그룹화의 자연 경계가 됨.
10. **에러 정책**: silent fallback (AutoCrop) vs IllegalArgumentException (Perspective degenerate). 기존 모듈 컨벤션과 일치.
11.

**AspectRatio**: `data class AspectRatio(val width: Int, val height: Int)` — `smartCrop(aspectWidth, aspectHeight)` 대신 사용. 공통 프리셋 `SQUARE`, `WIDESCREEN`, `PORTRAIT`, `STANDARD` 제공.

12. **ImagePoint**: `Point2D` 대신 `ImagePoint` 사용 — 이미지 도메인 명확화. x는 열 (column), y는 행 (row) 방향.
