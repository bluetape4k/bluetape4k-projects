# utils/images 필터/색보정 DSL 구현 플랜 — Issue #131

- **Spec**: [`/docs/superpowers/specs/2026-04-26-image-filter-dsl-design.md`](../specs/2026-04-26-image-filter-dsl-design.md)
- **Issue**: #131
- **모듈**: `utils/images` (`bluetape4k-images`)
- **브랜치**: `issue-131-image-filter-dsl`
- **작업 위치**: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/issue-131-image-filter-dsl/`
- **작성일**: 2026-04-26

---

## 개요

본 플랜은 spec v3/v4를 기반으로 구현 작업을 Task 단위로 분해한다. 의존 관계에 따라 순차 진행을 권장하지만, T5~T8b(신규 필터 5종)와 T9~T13(DSL 멤버 그룹)은 인프라(T1~T3)와 색공간(T4) 완료 후 병렬 작업이 가능하다.

**핵심 원칙**:
- DSL 진입점 `compactAndApply` 에서 `source.copy()` 1회로 mutation 격리
- 사용자 선언 순서 보존 (비가환성)
- 인접 Native 필터는 `PipelineFilter` 로 묶어 단일 패스 적용
- 신규 필터 5종은 모두 scrimage `Filter` 인터페이스 구현 + `xxxFilterOf(...)` 팩토리
- Float/Double 타입은 scrimage 생성자 시그니처를 그대로 따름 (§4.12)
- 모든 입력은 `require` 가드로 즉시 검증

**테스트 컨벤션**:
- JUnit 5 + Kluent + MockK
- `runSuspendIO {}` (또는 `runTest`) 로 suspend 테스트
- 신규 필터는 골든 이미지 1장씩 + 라운드트립 fuzz
- scrimage 내장 래퍼는 직접 호출과의 동등성 검증으로 갈음 (골든 이미지 불필요)

**모듈 패키지**:
- 신규 패키지: `io.bluetape4k.images.filters.dsl`
- 신규 필터: `io.bluetape4k.images.filters` (기존 위치)

---

## Task 목록

### T-V0: 사전 검증 - 모듈 구조 / 기존 코드 파악
- **complexity**: low
- **파일**: 읽기 전용 — `utils/images/src/main/kotlin/io/bluetape4k/images/**`, `utils/images/src/test/kotlin/io/bluetape4k/images/**`, `utils/images/build.gradle.kts`, `utils/images/README.md`
- **내용**:
  - `WatermarkFilterSupport`, `CaptionFilterSupport`, `ImmutableImageSupport` 시그니처 확인
  - 기존 `AbstractFilterTest` 위치 및 helper(`loadResourceImage`) 확인 — **`assertSimilarToImage` 는 존재하지 않음; T0a 에서 신규 구현**
  - `runSuspendIO` / `runTest` 사용 패턴 확인 (기존 SuspendImageWriter 테스트 참고)
  - test 리소스 경로 (`src/test/resources/`) 구조 확인
  - 기존 Mermaid UML 구조 확인 (README.md 다이어그램)
- **완료 기준**:
  - 기존 헬퍼/팩토리 시그니처 메모 확보
  - 신규 코드가 호출/위임할 수 있는 기존 API 목록 명확화
- **의존**: 없음

---

### T0a: 테스트 헬퍼 - AbstractFilterTest 확장 (assertSimilarToImage)
- **complexity**: medium
- **파일** (수정):
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/AbstractFilterTest.kt`
- **내용**:
  - `assertSimilarToImage(actual: ImmutableImage, expected: ImmutableImage, tolerance: Int = 2)` — 동일 width/height 검증 + 픽셀별 채널 절대 차 ≤ tolerance
  - `assertNotSimilarToImage(a: ImmutableImage, b: ImmutableImage, threshold: Int = 5)` — 어느 픽셀이라도 채널 차 > threshold
  - `assertSimilarToResource(actual: ImmutableImage, resourceName: String, tolerance: Int = 3)` — `WatermarkFilterTest` 의 private 헬퍼를 `AbstractFilterTest` 로 승격 (protected 또는 companion)
  - tolerance 정의: 픽셀 채널(R/G/B) 절대값 차이, 채널 당 독립 측정 (RMS 아님)
  - 헬퍼 자체 단위 테스트 추가 (동일 이미지 → assertSimilar 통과, 다른 이미지 → assertNotSimilar 통과)
- **완료 기준**:
  - `./gradlew :bluetape4k-images:test --tests "*AbstractFilterTest*"` 통과
  - 이후 T14/T15/T16/T17 에서 이 헬퍼를 실제 사용
- **의존**: T-V0

---

### T1: 인프라 - DSL 패키지 + DslMarker + ImageFilterChain 골격
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterDsl.kt` (`@DslMarker annotation class ImageFilterDsl`)
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChain.kt`
- **내용**:
  - `@DslMarker annotation class ImageFilterDsl`
  - `@ImageFilterDsl class ImageFilterChain internal constructor()`
  - private sealed interface `Op` { class `Native(val filter: Filter)`, class `Pixel(val transform: (ImmutableImage) -> ImmutableImage)` }
  - `private val ops = mutableListOf<Op>()`
  - public 멤버: `raw(filter: Filter)`, `pixel(transform: (ImmutableImage) -> ImmutableImage)`
  - `internal fun apply(source: ImmutableImage): ImmutableImage` 선언만 (T2에서 구현)
  - 한국어 KDoc + 사용 예제
- **완료 기준**:
  - `./gradlew :bluetape4k-images:compileKotlin` 성공
  - `ImageFilterChain` public API 가 spec §4.1 / §6.3 시그니처와 일치
  - `Op` sealed interface 가 `private`
- **의존**: T-V0

---

### T2: 인프라 - compactAndApply (mutation 격리 + PipelineFilter 묶음)
- **complexity**: high
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterCompaction.kt` (private/internal helpers: `compactAndApply`, `compact`)
- **내용**:
  - `internal fun compactAndApply(source: ImmutableImage, ops: List<Op>): ImmutableImage`
    - `if (ops.isEmpty()) return source` (방어 복사 생략)
    - `var current = source.copy()` (1회 방어 복사)
    - 인접 Native 묶기: `compact(ops)` → `List<Group>` (`NativeGroup(filters: List<Filter>)` 또는 `PixelOp(transform)`)
    - 각 그룹에 대해:
      - `NativeGroup` → `current = current.filter(PipelineFilter(group.filters))` (단일도 PipelineFilter로 감싼다 — 일관성)
      - `PixelOp` → `current = group.transform(current)`
    - return current
  - `ImageFilterChain.apply(source)` 가 `compactAndApply(source, ops.toList())` 호출
- **완료 기준**:
  - 빈 ops → source 그대로 반환 (mutation 없음)
  - 단일/복수 Native 모두 `PipelineFilter` 로 감싸짐
  - Pixel 변환은 그 자리에서 적용
  - 사용자 선언 순서 보존 (비가환성 유지)
- **의존**: T1

---

### T3: 인프라 - applyFilters / suspendApplyFilters 확장 함수
- **complexity**: low
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterDslApply.kt`
- **내용**:
  - `fun ImmutableImage.applyFilters(block: ImageFilterChain.() -> Unit): ImmutableImage`
    - `= ImageFilterChain().apply(block).apply(this)`
  - `suspend fun ImmutableImage.suspendApplyFilters(block: ImageFilterChain.() -> Unit): ImmutableImage`
    - `= withContext(Dispatchers.Default) { applyFilters(block) }`
  - 한국어 KDoc + 사용 예제 (spec §6.1 KDoc 그대로)
- **완료 기준**:
  - 컴파일 성공
  - `suspendApplyFilters` 가 `withContext(Dispatchers.Default)` 사용
  - KDoc 에 "원본 이미지는 변경되지 않음" 명시
- **의존**: T2

---

### T4: 색공간 - ColorSpaceConverter + ImmutableImage 확장 + 라운드트립 테스트
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ColorSpaceConverter.kt` (public object)
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageColorSpaceExtensions.kt` (`toHsvArray()`, `toYCbCrArray()`)
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/ColorSpaceConverterTest.kt`
- **내용**:
  - `public object ColorSpaceConverter`:
    - `fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float>` (java.awt.Color.RGBtoHSB 활용)
    - `fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int>` (java.awt.Color.HSBtoRGB → ARGB 분해)
    - `fun rgbToYCbCr(r: Int, g: Int, b: Int): Triple<Float, Float, Float>` (BT.601)
    - `fun yCbCrToRgb(y: Float, cb: Float, cr: Float): Triple<Int, Int, Int>` (역변환 + clamp 0..255)
    - `fun kelvinToRgb(kelvin: Int): Triple<Int, Int, Int>` (Tanner Helland 알고리즘)
      - 입력이 `KELVIN_MIN..KELVIN_MAX` 범위를 벗어나면 **silently clamp** (예외 아님)
      - `companion object` 에 `const val KELVIN_MIN = 1000`, `const val KELVIN_MAX = 40000` 정의 (향후 범위 변경 시 단일 지점 수정)
    - `@JvmSynthetic internal fun rgbToHsvInto(r, g, b, out: FloatArray)` (out.size >= 3)
    - `@JvmSynthetic internal fun hsvToRgbInto(h, s, v, out: IntArray)` (out.size >= 3)
    - `@JvmSynthetic internal fun kelvinToRgbInto(kelvin, out: IntArray)` (clamp 동일 적용)
  - `fun ImmutableImage.toHsvArray(): FloatArray` — width × height × 3, row-major
  - `fun ImmutableImage.toYCbCrArray(): FloatArray` — 동일
  - 테스트:
    - 라운드트립 fuzz: 랜덤 RGB 1000회 → HSV → RGB, 채널당 ±2 허용
    - 라운드트립 fuzz: YCbCr 동일
    - kelvinToRgb: `KELVIN_MIN`(붉은색), 5500K(중성 근사), 6500K, `KELVIN_MAX`(푸른색)
    - clamp 동작: `kelvinToRgb(500)` → `kelvinToRgb(KELVIN_MIN)` 과 동일 결과 (예외 없음)
    - clamp 동작: `kelvinToRgb(50000)` → `kelvinToRgb(KELVIN_MAX)` 과 동일 결과
- **완료 기준**:
  - public API 시그니처가 spec §4.9 / §5.3 과 일치
  - 라운드트립 fuzz 테스트 통과 (1000회, 채널당 ±2)
  - `toHsvArray()` / `toYCbCrArray()` 결과 길이 = `width * height * 3`
  - 내부 `*Into` 함수들은 `@JvmSynthetic internal` 가시성
- **의존**: T-V0 (T1~T3와 병렬 가능)

---

### T5: 신규 필터 - SaturationAdjustFilter + saturationFilterOf + 골든 이미지 테스트
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/SaturationAdjustFilter.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/SaturationFilterTest.kt`
  - `utils/images/src/test/resources/filters/dsl/source.png` (256×256, 다양한 색상; T5에서 최초 생성, 이후 모든 테스트가 공유)
  - `utils/images/src/test/resources/filters/dsl/expected_saturation_1_5.png`
- **내용**:
  - `class SaturationAdjustFilter(private val factor: Float) : Filter`
    - `init { require(factor >= 0f) { "factor must be >= 0, but was $factor" } }`
    - `override fun apply(image: ImmutableImage)`: `image.awt().raster` 에서 픽셀별 RGB→HSV→S*=factor (0..1 clamp)→RGB→setPixels (in-place)
    - 내부 고성능 경로: `ColorSpaceConverter.rgbToHsvInto` / `hsvToRgbInto` 활용 (FloatArray/IntArray 재사용)
    - companion object : KLoggingChannel()
  - `fun saturationFilterOf(factor: Float): Filter = SaturationAdjustFilter(factor)`
    - **KDoc 필수**: `"이 필터는 image.awt()를 직접 변경합니다. 원본 보존이 필요하면 applyFilters DSL 또는 image.copy().filter(...) 사용"` 경고 포함
  - **source.png 생성**: `SourceImageGenerator.kt` (test 전용 helper) — 256×256 BufferedImage를 `ColorRainbow` 그라디언트 + 4×4 컬러 패치 구성으로 결정론적 생성 (시드 고정, 재현 가능). 1회 실행 후 리소스로 동결.
  - 테스트:
    - `factor = 1.0f` → identity (tolerance = 1)
    - `factor = 0f` → 흑백 (R=G=B 검증)
    - `factor = 1.5f` → 골든 이미지 비교 (tolerance = 3); **초기 실행 시 결과를 사람이 시각 확인 후 동결**
    - `factor = -0.1f` → IllegalArgumentException
- **완료 기준**:
  - `source.png` (256×256) RGB 전 영역 커버, 결정론적 재생성 가능
  - 골든 이미지 1장 저장 + 테스트 통과
  - require 가드 단위 테스트 통과
  - `saturationFilterOf` KDoc에 mutation 경고 포함
  - `saturationFilterOf` 팩토리는 `Filter` 반환
- **의존**: T4 (ColorSpaceConverter 사용)

---

### T6: 신규 필터 - HueAdjustFilter + hueFilterOf + 골든 이미지 테스트
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/HueAdjustFilter.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/HueFilterTest.kt`
  - `utils/images/src/test/resources/filters/dsl/expected_hue_60deg.png`
- **내용**:
  - `class HueAdjustFilter(private val deltaDegrees: Float) : Filter`
    - 가드 없음 (degrees 는 임의값 허용, mod 360 으로 정규화)
    - `apply`: 픽셀별 RGB→HSV → H = ((H * 360 + delta) mod 360) / 360 → HSV→RGB
  - `fun hueFilterOf(deltaDegrees: Float): Filter`
  - 테스트:
    - `delta = 0f` → identity
    - `delta = 360f` → identity (modulo 검증, tolerance = 2)
    - `delta = 60f` → 골든 이미지 비교
    - `delta = -180f` → 보색 변환 검증
- **완료 기준**:
  - 360도 회전 시 원본과 거의 동일 (tolerance ≤ 2)
  - 골든 이미지 1장 저장 + 테스트 통과
- **의존**: T4, T5 (source.png 공유)

---

### T7: 신규 필터 - ColorTemperatureFilter + colorTemperatureFilterOf + 골든 이미지 테스트
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/ColorTemperatureFilter.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/ColorTemperatureFilterTest.kt`
  - `utils/images/src/test/resources/filters/dsl/expected_temperature_3000k.png`
- **내용**:
  - `class ColorTemperatureFilter(private val kelvin: Int) : Filter`
    - `init { require(kelvin in 1000..40000) { "kelvin must be in 1000..40000, but was $kelvin" } }`
    - `apply`: 한 번 `ColorSpaceConverter.kelvinToRgb(kelvin)` 으로 (tr, tg, tb) 계산 → 픽셀별 R'=R*tr/255, G'=G*tg/255, B'=B*tb/255 (clamp 0..255)
  - `fun colorTemperatureFilterOf(kelvin: Int): Filter`
  - 테스트:
    - `kelvin = 999` → IllegalArgumentException
    - `kelvin = 40001` → IllegalArgumentException
    - `kelvin = 6500` → 거의 identity (중성 근처, tolerance = 5)
    - `kelvin = 3000` → 골든 이미지 비교 (따뜻한 톤)
    - `kelvin = 10000` → 푸른 톤 (B 채널 평균이 원본보다 높음)
- **완료 기준**:
  - require 가드 동작
  - 골든 이미지 1장 저장 + 테스트 통과
  - 6500K 가 원본에 가까움 (중성 근사 검증)
- **의존**: T4, T5 (source.png 공유)

---

### T8: 신규 필터 - RoundedCornerFilter + roundedCornerFilterOf + 골든 이미지 테스트
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/RoundedCornerFilter.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/RoundedCornerFilterTest.kt`
  - `utils/images/src/test/resources/filters/dsl/expected_rounded_32.png`
- **내용**:
  - `class RoundedCornerFilter(private val radius: Int) : Filter`
    - `init { require(radius >= 0) { "radius must be >= 0, but was $radius" } }`
    - `apply`: 4개 코너 영역 (radius × radius) 각 픽셀에 대해 코너 중심 거리 d 계산
      - d ≤ radius : 알파 그대로
      - radius < d ≤ radius + 1 : 알파 페이드 (안티앨리어싱 1px)
      - d > radius : 알파 = 0
    - `BufferedImage` 가 알파 채널을 갖도록 `TYPE_INT_ARGB` 보장 (필요 시 raster 직접 조작 또는 Graphics2D.setComposite + RoundRectangle2D 마스크 합성 — 단, in-place 보장)
  - `fun roundedCornerFilterOf(radius: Int): Filter`
  - 테스트:
    - `radius = 0` → identity
    - `radius = -1` → IllegalArgumentException
    - `radius = 32` → 골든 이미지 비교 (네 코너 픽셀의 알파 = 0 검증)
    - `radius = min(width, height) / 2` → 원형 비슷
- **완료 기준**:
  - require 가드 동작
  - 코너 픽셀 알파 = 0 검증
  - 골든 이미지 1장 저장 + 테스트 통과
- **의존**: T5 (source.png 공유) — ColorSpaceConverter 미사용

---

### T8b: 신규 필터 - MedianBlurFilter + medianBlurFilterOf + 골든 이미지 테스트
- **complexity**: high
- **파일** (생성):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/MedianBlurFilter.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/MedianBlurFilterTest.kt`
  - `utils/images/src/test/resources/filters/dsl/expected_median_2.png`
- **내용**:
  - `enum class MedianBoundaryMode { REPLICATE, REFLECT }` (파일: `MedianBlurFilter.kt` 내 또는 별도 파일)
    - `REPLICATE`: 경계 밖은 가장 가까운 경계 픽셀 값을 복제
    - `REFLECT`: 경계 밖은 경계를 축으로 반사된 픽셀 값 사용
  - `class MedianBlurFilter(private val radius: Int, private val boundary: MedianBoundaryMode = MedianBoundaryMode.REPLICATE) : Filter`
    - `init { require(radius >= 0) { "radius must be >= 0, but was $radius" } }`
    - `apply`: 픽셀별 (2r+1)² 윈도우의 R/G/B 채널별 중앙값 계산
      - radius=0 → identity (윈도우 = 단일 픽셀)
      - 경계 처리: `boundary` 파라미터에 따라 좌표 clamp(REPLICATE) 또는 반사(REFLECT)
      - in-place 가 아닌 (median 은 이웃 의존) — 임시 픽셀 배열 사용
      - IntArray 윈도우 + Arrays.sort → 중앙값
  - `fun medianBlurFilterOf(radius: Int, boundary: MedianBoundaryMode = MedianBoundaryMode.REPLICATE): Filter`
  - DSL 멤버 (T12 에서 연동): `fun medianBlur(radius: Int = 1, boundary: MedianBoundaryMode = MedianBoundaryMode.REPLICATE)`
  - 테스트:
    - `radius = 0` → identity (tolerance = 0)
    - `radius = -1` → IllegalArgumentException
    - `radius = 2, boundary = REPLICATE` → 골든 이미지 비교
    - `radius = 2, boundary = REFLECT` → REPLICATE 결과와 중심 영역은 동일, 경계 1px 행/열 비교
    - 임펄스 노이즈 제거 검증: salt-and-pepper 노이즈 추가한 이미지에 적용 → 노이즈 픽셀 수 감소
- **완료 기준**:
  - `MedianBoundaryMode` enum 정의 + KDoc
  - require 가드 동작
  - radius=0 identity 검증
  - 골든 이미지 1장 저장 + 테스트 통과
  - boundary 모드 전환 테스트 통과
  - 노이즈 제거 효과 검증 (정성적, 노이즈 픽셀 비율 감소)
- **의존**: T5 (source.png 공유)

---

### T9: DSL 멤버 - 색/톤 보정 + 단위 테스트
- **complexity**: medium
- **파일** (수정):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChain.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChainTest.kt` (신규)
- **내용**:
  - DSL 멤버 (Native 또는 Pixel 등록):
    - `brightness(amount: Float = 1.2f)` → Native(BrightnessFilter)
    - `contrast(amount: Double = 1.2)` → Native(ContrastFilter)
    - `gamma(gamma: Double = 1.0)` → Native(GammaFilter)
    - `hsb(hue: Float = 0f, saturation: Float = 0f, brightness: Float = 0f)` → Native(HSBFilter)
    - `rgb(r: Float = 1f, g: Float = 1f, b: Float = 1f)` → Native(RGBFilter)
    - `opacity(alpha: Float)` → require(alpha in 0f..1f) + Native(OpacityFilter)
    - `threshold(value: Int = 127)` → Native(ThresholdFilter)
    - `posterize(levels: Int = 6)` → require(levels >= 2) + Native(PosterizeFilter)
    - `gainBias(gain: Float, bias: Float)` → Native(GainBiasFilter)
    - `saturation(factor: Float)` → require(factor >= 0f) + Native(SaturationAdjustFilter(factor))
    - `hue(deltaDegrees: Float)` → Native(HueAdjustFilter)
    - `colorTemperature(kelvin: Int)` → require(kelvin in 1000..40000) + Native(ColorTemperatureFilter)
  - 모든 멤버에 한국어 KDoc + 예제
  - 테스트 (`ImageFilterChainTest`):
    - 각 멤버 호출 → ops 리스트 길이 +1 (reflection 또는 internal `opsForTest` 노출 — internal getter)
    - require 가드 단위 테스트
    - 결과 검증은 T14 에서 통합 (여기는 빌더 단위)
- **완료 기준**:
  - 12개 멤버 함수 전수 구현
  - require 가드 단위 테스트 통과 (음수 alpha, levels<2, factor<0, kelvin 범위 등)
  - KDoc + 예제 부착
- **의존**: T2, T5, T6, T7

---

### T10: DSL 멤버 - 톤/스타일 preset + 단위 테스트
- **complexity**: low
- **파일** (수정):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChain.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChainTest.kt`
- **내용**:
  - DSL 멤버 (모두 Native, 매개변수 없음):
    - `sepia()`, `grayscale()`, `invert()`, `vintage()`, `chrome()`, `nashville()`, `gotham()`, `summer()`, `oldPhoto()`
  - 각 멤버에 KDoc + 예제
  - 테스트:
    - 각 멤버 호출 → ops += 1
    - 빈 이미지에 적용 → 예외 없음 (smoke test)
- **완료 기준**:
  - 9개 멤버 구현
  - 빌더 ops 누적 검증
- **의존**: T2

---

### T11: DSL 멤버 - 블러/선명도 + 단위 테스트
- **complexity**: low
- **파일** (수정):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChain.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChainTest.kt`
- **내용**:
  - DSL 멤버:
    - `blur()` → Native(BlurFilter)
    - `gaussianBlur(radius: Int = 2)` → require(radius >= 0) + Native(GaussianBlurFilter)
    - `motionBlur(distance: Float, angle: Float)` → Native(MotionBlurFilter)
    - `sharpen()` → Native(SharpenFilter) — 매개변수 없음 (scrimage 실제 시그니처)
    - `unsharp()` → Native(UnsharpFilter)
    - `noiseReduction(threshold: Int = 8)` → Native(NoiseReductionFilter)
  - 각 멤버에 KDoc + 예제
  - 테스트:
    - require 가드 (gaussianBlur radius < 0)
    - 빌더 ops 누적
- **완료 기준**:
  - 6개 멤버 구현
  - require 가드 테스트
- **의존**: T2

---

### T12: DSL 멤버 - 효과 + 단위 테스트
- **complexity**: medium
- **파일** (수정):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChain.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChainTest.kt`
- **내용**:
  - DSL 멤버:
    - `oil(range: Int = 3, levels: Int = 256)` → Native(OilFilter)
    - `crystallize()` → Native(CrystallizeFilter)
    - `pixelate(blockSize: Int = 8)` → require(blockSize >= 1) + Native(PixelateFilter)
    - `medianBlur(radius: Int = 1)` → require(radius >= 0) + Native(MedianBlurFilter(radius))
    - `border(thickness: Int = 1, color: java.awt.Color = java.awt.Color.BLACK)` → require(thickness >= 0) + Native(BorderFilter)
    - `vignette(start: Float = 0.85f, end: Float = 0.95f, blur: Float = 0.3f, color: java.awt.Color = java.awt.Color.BLACK)` → Native(VignetteFilter)
    - `glow(amount: Float = 0.5f)` → Native(GlowFilter)
    - `lensFlare()` → Native(LensFlareFilter)
    - `roundedCorners(radius: Int)` → require(radius >= 0) + Native(RoundedCornerFilter)
  - 각 멤버에 KDoc + 예제
  - 테스트:
    - require 가드 (pixelate blockSize<1, border thickness<0, roundedCorners radius<0, medianBlur radius<0)
    - 빌더 ops 누적
- **완료 기준**:
  - 9개 멤버 구현
  - require 가드 테스트 통과
- **의존**: T2, T8, T8b

---

### T13: DSL 통합 - watermark / caption 멤버 (기존 팩토리 위임)
- **complexity**: medium
- **파일** (수정):
  - `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChain.kt`
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChainTest.kt`
- **내용**:
  - DSL 멤버 (기존 `watermarkFilterOf` / `captionFilterOf` 위임):
    - `watermark(text, font, type=COVER, antiAlias=true, alpha=0.1, color=WHITE)` → Native(watermarkFilterOf(...))
    - `watermarkAt(text, x, y, font, antiAlias, alpha, color)` → Native(watermarkFilterOf(... x/y ...))
    - `caption(text, x=0, y=0, font, color)` → Native(captionFilterOf(...))
  - 기본값은 기존 팩토리의 기본값 그대로 사용 (T-V0에서 확인한 시그니처)
  - 테스트:
    - 워터마크 적용 → 결과 픽셀이 source 와 다름 (smoke)
    - 캡션 적용 동일
- **완료 기준**:
  - 두 watermark 오버로드 + 1개 caption 멤버 구현
  - 기존 팩토리 함수에 정확히 위임 (시그니처 호환)
  - 빌더 + 적용 smoke 테스트 통과
- **의존**: T2, T-V0 (기존 팩토리 시그니처 확인)

---

### T14: 테스트 - applyFilters 통합 동작 검증
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterDslApplyTest.kt`
- **내용**:
  - `applyFilters preserves source immutability` — source 픽셀 해시가 적용 전후 동일
  - `applyFilters chain matches sequential filter calls` — DSL vs 수동 sequential filter 동등 (tolerance = 2)
  - `applyFilters preserves user declared order` — `brightness→contrast` 와 `contrast→brightness` 결과 다름
  - `applyFilters with empty block returns source` — ops 비어있을 때 source 와 동일
  - `applyFilters with single Pixel op` — Pixel 만 있는 경우
  - `saturation factor of 1_0 is identity` — saturation(1.0f) 결과가 source 와 거의 동일
- **완료 기준**:
  - 6개 테스트 케이스 통과
  - mutation 격리 검증 (픽셀 해시)
- **의존**: T3, T9

---

### T15: 테스트 - suspendApplyFilters 디스패처 + 취소
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/SuspendImageFilterDslTest.kt`
- **내용**:
  - `suspendApplyFilters runs on Default dispatcher` — `pixel { Thread.currentThread() }` 캡처 → 이름에 "DefaultDispatcher" 포함
  - `suspendApplyFilters returns equivalent to applyFilters` — 동기/비동기 결과 동등
  - `suspendApplyFilters propagates cancellation` — 외부 취소 시 CancellationException 전파 (긴 픽셀 변환 후 취소)
  - 사용 패턴: `runTest { ... }` 또는 `runSuspendIO { ... }` (T-V0 컨벤션 확인)
- **완료 기준**:
  - dispatcher 검증 통과
  - 동기/비동기 결과 동등 검증
  - 취소 전파 검증
- **의존**: T3, T14

---

### T16: 테스트 - PipelineCompactionTest (결과 기반)
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/PipelineCompactionTest.kt`
- **내용**:
  - `compactAndApply groups consecutive native ops` — `[N, N, Pixel(identity), N]` 가 수동 `PipelineFilter([N,N]) → identity → PipelineFilter([N])` 와 픽셀 동등 (tolerance = 1)
  - `compactAndApply preserves source when ops empty` — 빈 ops → source 동등
  - `compactAndApply handles only Pixel ops` — Native 없이 Pixel 만으로도 동작
  - `compactAndApply wraps single native in PipelineFilter` — 단일 Native 도 `PipelineFilter` 로 감싸짐 (결과 기반 검증; 직접 호출과 동등)
- **완료 기준**:
  - 4개 테스트 케이스 통과
  - spy 없이 결과 기반 검증
- **의존**: T2, T3

---

### T17: 테스트 - 체인 골든 이미지 3+
- **complexity**: medium
- **파일** (생성):
  - `utils/images/src/test/kotlin/io/bluetape4k/images/filters/dsl/ChainGoldenImageTest.kt`
  - `utils/images/src/test/resources/filters/dsl/expected_pipeline_brightness_contrast_sepia.png`
  - `utils/images/src/test/resources/filters/dsl/expected_pipeline_vintage_vignette.png`
  - `utils/images/src/test/resources/filters/dsl/expected_pipeline_saturation_temperature_watermark.png`
- **내용**:
  - 시나리오 1 — 색보정: `brightness(1.2f); contrast(1.1); sepia()` → 골든
  - 시나리오 2 — 빈티지 효과: `vintage(); vignette()` → 골든
  - 시나리오 3 — 신규 필터 합성: `saturation(1.3f); colorTemperature(4000); watermark("test")` → 골든
  - tolerance = 3
- **완료 기준**:
  - 3개 시나리오 골든 이미지 저장 + 테스트 통과
- **의존**: T9, T10, T12, T13

---

### T18: 문서 - README.md (영어)
- **complexity**: low
- **파일** (수정):
  - `utils/images/README.md`
- **내용**:
  - 새 섹션: "Filter DSL"
    - 사용 예제 (applyFilters, suspendApplyFilters)
    - 필터 카탈로그 표 (DSL 멤버 → scrimage 클래스 / 신규 구현)
    - mutation 격리 보장 설명
  - 새 섹션: "Color Space Conversion"
    - ColorSpaceConverter 사용 예
    - toHsvArray / toYCbCrArray
  - Mermaid UML 갱신 (ImageFilterChain + Op + ColorSpaceConverter 관계도)
- **완료 기준**:
  - DSL 섹션 추가
  - 필터 카탈로그 표 (대표 30개)
  - Mermaid 다이어그램 갱신
  - 영어 작성
- **의존**: T1~T17 (구현 완료 후)

---

### T19: 문서 - README.ko.md (한국어, 동기화)
- **complexity**: low
- **파일** (수정):
  - `utils/images/README.ko.md`
- **내용**:
  - T18 의 영어 README 와 동일한 구조/예제 → 한국어 번역
  - Mermaid 다이어그램은 영어 README 와 동일 (다이어그램 텍스트 한글화 선택적, 일관성 유지)
- **완료 기준**:
  - README.md 와 섹션 구조 동일
  - 한국어 번역 완료
  - Mermaid 다이어그램 동기
- **의존**: T18

---

### T20: 검증 - 테스트 + detekt 전수 통과
- **complexity**: low
- **파일**: 없음 (실행만)
- **내용**:
  - `./bin/repo-test-summary -- ./gradlew :bluetape4k-images:test` 전수 통과
  - `./gradlew :bluetape4k-images:detekt` 통과 (신규 코드 0 issue; 기존 위반은 baseline 처리)
  - 실패 시 fix → 재실행 (필요 모듈 의존성 한정)
- **완료 기준**:
  - 테스트 0 fail (skip 허용, 골든 이미지 누락 0)
  - detekt 신규 코드 0 issue
  - 결과(passing count + duration) 확보 → T20a에 기록
- **의존**: T1~T19 전체

---

### T20a: testlog 기록
- **complexity**: low
- **파일** (수정):
  - `docs/testlogs/2026-04.md` — 표 맨 위에 새 행 추가
- **내용**:
  - 형식: `| {날짜} | {작업 설명} | :bluetape4k-images | {N} passing, {M} skipped | ✅ | {duration} | {비고} |`
  - T20에서 얻은 passing count + duration 기록
- **완료 기준**:
  - `docs/testlogs/2026-04.md` 파일 최상단에 오늘 날짜 행 추가
- **의존**: T20

---

### T21: 리뷰 - code-reviewer 에이전트 실행
- **complexity**: low
- **파일**: 없음 (에이전트 실행)
- **내용**:
  - `oh-my-claudecode:code-reviewer` 또는 `pr-review-toolkit:code-reviewer` 스킬 실행
  - 변경된 신규 파일 + 수정 파일 대상
  - HIGH/CRITICAL 이슈 발견 시 즉시 수정 → 재리뷰
- **완료 기준**:
  - HIGH/CRITICAL 0 (또는 모두 fix 후 재리뷰 클린)
  - MEDIUM 가능한 한 fix
- **의존**: T20a

---

### T21.5: PR 사전 체크리스트 (CLAUDE.md MANDATORY 항목)
- **complexity**: low
- **파일**: 없음 (검증만)
- **내용**: CLAUDE.md "Before Creating a PR" 전 항목 개별 확인
  - [ ] `./gradlew :bluetape4k-images:test` 결과 (passing/failed/skipped + duration) 확보
  - [ ] code-reviewer 실행 → HIGH/CRITICAL 0 확인
  - [ ] README.md + README.ko.md 모두 DSL 섹션 업데이트
  - [ ] 모든 신규 public API 한국어 KDoc 포함
  - [ ] 작업이 `.worktrees/issue-131-image-filter-dsl` worktree 내부에서 완료됨
  - [ ] `docs/superpowers/index/2026-04.md` 업데이트
  - [ ] `docs/testlogs/2026-04.md` 기록
  - [ ] mock-web-server / mock-webflux-server 변경 없음 확인 (jib 재빌드 불필요)
  - [ ] `virtualthread/api` 변경 없음 확인
  - [ ] LAB 변환 미포함 사유 PR 본문에 명시 (Issue #131 요구 vs 1차 제외 결정)
- **완료 기준**: 모든 체크항목 ✅ 또는 해당 없음 N/A
- **의존**: T21

---

### T22: 마무리 - superpowers 인덱스 + PR 생성
- **complexity**: low
- **파일** (수정):
  - `docs/superpowers/index/2026-04.md` — 항목 추가 (이달 파일 맨 위)
  - `docs/superpowers/INDEX.md` — ✅/⏳ 카운트 갱신
- **내용**:
  - superpowers 인덱스에 본 작업 항목 추가 (스펙 + 플랜 링크)
  - `gh pr create` (non-interactive, --json) 으로 PR 생성
    - 본문에 spec 링크, 테스트 결과(passing count + duration), 검증 명령어, LAB 미포함 사유 포함
    - base = `develop`, head = `issue-131-image-filter-dsl`
  - `/wiki-update` 스킬 실행 (새 spec/plan 등록)
- **완료 기준**:
  - PR URL 확보 + base/head 정확
  - superpowers 인덱스 두 파일 모두 커밋
  - wiki-update 완료
- **의존**: T21.5

---

## Task 의존 그래프 (요약)

```
T-V0
 ├─ T1 ─ T2 ─ T3
 │              ├─ T9 (T2, T5, T6, T7 후)
 │              ├─ T10 (T2 후)
 │              ├─ T11 (T2 후)
 │              ├─ T12 (T2, T8, T8b 후)
 │              └─ T13 (T2, T-V0 후)
 │                  ├─ T14 (T3, T9)
 │                  ├─ T15 (T3, T14)
 │                  ├─ T16 (T2, T3)
 │                  └─ T17 (T9, T10, T12, T13)
 ├─ T4 ────────┤
 │             ├─ T5 ──┤
 │             ├─ T6 ──┤
 │             ├─ T7 ──┤
 │             ├─ T8 ──┤  (T5의 source.png 공유)
 │             └─ T8b ─┘
 ↓
T18 → T19 → T20 → T21 → T22
```

**병렬 가능 구간**:
- T4 / T5~T8b: T-V0 후 T2 와 무관하게 진행 가능 (단, T5 가 source.png 최초 생성 → T6/T7/T8/T8b 는 T5 후)
- T10 / T11: T2 만 의존 → 서로 병렬
- T9 / T12: 신규 필터(T5/T6/T7 / T8/T8b) 가 각각 끝나는 대로 시작

---

## Definition of Done 체크리스트 (spec §9 매핑)

| Spec 항목 | 대응 Task |
|---|---|
| ImageFilterChain DSL + applyFilters/suspendApplyFilters | T1, T2, T3 |
| §5.1 scrimage 내장 래퍼 전수 노출 | T9, T10, T11, T12 |
| §5.2 신규 필터 5종 직접 구현 | T5, T6, T7, T8, T8b |
| 신규 필터별 xxxFilterOf 팩토리 | T5, T6, T7, T8, T8b |
| ColorSpaceConverter + toHsvArray/toYCbCrArray | T4 |
| compactAndApply mutation 격리 + Pipeline 묶음 | T2 |
| §5b 에러 처리 정책 (require) | T5, T7, T8, T8b, T9, T11, T12 |
| 모든 신규 public API 한국어 KDoc + 예제 | T1, T3, T4, T5~T8b, T9~T13 |
| DSL 빌더 단위 테스트 | T9~T13 (각 멤버), T16 |
| applyFilters/suspendApplyFilters 동작 테스트 | T14, T15 |
| 신규 필터 골든 이미지 (각 1장+) | T5, T6, T7, T8, T8b |
| 체인 케이스 골든 3+ | T17 |
| 색공간 라운드트립 fuzz | T4 |
| PipelineCompaction 검증 | T16 |
| `:bluetape4k-images:test` 전수 통과 | T20 |
| `:bluetape4k-images:detekt` 통과 | T20 |
| README.md / README.ko.md 갱신 (Mermaid 포함) | T18, T19 |
| code-reviewer 리뷰 HIGH/CRITICAL 0 | T21 |
| superpowers/index/2026-04.md 항목 추가 | T22 |
| PR 생성 (spec 링크 포함) | T22 |

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v1 | 2026-04-26 | Spec v3/v4 기반 초안 — 22개 Task (T-V0 + T1~T22) |

---

(끝 — v1)
