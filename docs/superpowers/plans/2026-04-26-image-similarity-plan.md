# Image Similarity 모듈 확장 구현 Plan (utils/images)

- **Spec**: [`docs/superpowers/specs/2026-04-26-image-similarity-design.md`](../specs/2026-04-26-image-similarity-design.md)
- **이슈**: #130 — utils/images 유사도 지표 확장
- **브랜치**: `issue-130-image-similarity` (`.worktrees/issue-130-image-similarity`)
- **모듈**: `bluetape4k-images` (`utils/images`)
- **작성일**: 2026-04-26

---

## 진행 원칙

- **Plan Task는 모두 필수** — 선택 없이 전수 완료 후 PR 생성, 완료 후 Plan 대비 비교 표 보고.
- **TDD**: 각 task는 RED → GREEN → REFACTOR 순서.
- **테스트 작성 + 실행 검증** 필수: 작성·수정 즉시 테스트 실행, pass/skip/fail 결과를 보고에 포함.
- **bluetape4k-assertions 비교 matcher** 사용 (`shouldBeGreaterThan`, `shouldBeLessOrEqualTo`, `shouldBeInRange`). `(x > y).shouldBeTrue()` 금지.
- **단계별 commit 분리** (Korean + prefix).
- **편집 후 `ide_diagnostics`** 확인, 임포트 오류·`@Deprecated`는 즉시 해소.

---

## Task 목록 (의존 그래프)

```
T1 ─┬─ T2 ─┐
    ├─ T3 ─┤
    ├─ T4 ─┼─ T2~T5 테스트 ─ T6 ─ T6-C ─ T6 테스트 ─ T7 ─ T8 ─ T9 ─ T10 ─ T7~T10 테스트 ─ T11 ─ T12 ─ T11~T12 테스트 ─ T13 ─ T14 ─ T15 ─ T16
    └─ T5 ─┘
```

---

### T1. SimilarityInternals.kt 추출
- **complexity**: medium
- **파일**:
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/SimilarityInternals.kt`
  - 수정: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/ImageSimilarity.kt`
- **내용**:
  - `ImageSimilarity.kt`의 `private` 헬퍼를 `SimilarityInternals.kt`에 `internal`로 이동.
    - 이동 대상: `luminance(p: Pixel)`, `requireSameSize`, `dct2d(input, n)`, 상수 `PIXEL_MAX`, `SSIM_C1`, `SSIM_C2`, `PHASH_SIZE`, `PHASH_LOW_SIZE`, `PHASH_BITS` (이름은 spec과 동일).
  - 신규 추가:
    - `internal const val HASH_SCALE_METHOD = ScaleMethod.Bicubic` — 모든 해시 공통 리사이즈 메서드.
    - `internal fun gaussianKernel1d(windowSize: Int, sigma: Double): DoubleArray` — T6에서 사용.
    - `internal fun haarTransform2d(matrix: Array<DoubleArray>, levels: Int = 1)` — T4에서 사용. 정사각 2^n 입력 in-place 단일 또는 다중 레벨 Haar 변환.
    - `internal fun hsvComponents(p: Pixel): Triple<Float, Float, Float>` — `Color.RGBtoHSB(r,g,b,null)` 래핑, T7에서 사용.
  - 기존 `ImageSimilarity.kt`는 import 만 정리(동작 변경 없음). 슬림화 후 ~250라인.
- **검증**:
  - 기존 `ImageSimilarityTest` 전수 통과 (회귀 가드).
  - `./gradlew :bluetape4k-images:test --tests "*ImageSimilarityTest"` PASS.
  - `ide_diagnostics` 오류 0건.
  - 커밋: `refactor: similarity 내부 헬퍼 SimilarityInternals.kt 로 추출`.
- **의존**: 없음.

---

### T1-S. prepareForSimilarity 유틸리티
- **complexity**: low
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/SimilarityInternals.kt` (T1에서 생성)에 `internal fun scaleToMaxSide(maxSide: Int)` 추가 후, 공개 API는 별도 top-level 파일 `SimilarityScaleUtils.kt`.
- **내용**:
  - `internal fun ImmutableImage.scaleToMaxSide(maxSide: Int): ImmutableImage`
    - `val longSide = maxOf(width, height)` → `if (longSide <= maxSide) return this`
    - 비율 유지: `val scale = maxSide.toDouble() / longSide` → `scaleTo((width*scale).roundToInt(), (height*scale).roundToInt(), HASH_SCALE_METHOD)`
  - `fun ImmutableImage.prepareForSimilarity(maxSide: Int = 512): ImmutableImage`
    - 공개 API. `scaleToMaxSide(maxSide)` 위임.
    - KDoc (§6.5 선택 가이드 포함):
      ```
      * 유사도 계산 전 이미지를 최대 [maxSide]px로 비율 유지 축소합니다.
      *
      * ## 크기별 권장 설정
      * | 이미지 크기 | 알고리즘 | 권장 maxSide |
      * |---|---|---|
      * | ≤ 256px | mssimTo 직접 호출 | — |
      * | ≤ 800px | mssimTo | 800 |
      * | ≤ 2MP | histogram / blockMean | 512 |
      * | 4K+ | hash 계열만 (내부 리사이즈) | — |
      *
      * 이미 [maxSide] 이하이면 원본 반환(복사 없음).
      ```
- **검증**:
  - 원본보다 큰 `maxSide` → 원본 반환.
  - 1024×768, maxSide=512 → 512×384.
  - 3840×2160 (4K), maxSide=800 → 800×450.
- **의존**: T1 (HASH_SCALE_METHOD 상수).

---

### T2. aHash 구현
- **complexity**: high
- **파일**:
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HashSimilarity.kt`
- **내용**:
  - `HashSize` enum 정의 (aHash/dHash/wHash 전용, `bits`, `internal val gridSide`):
    - `BITS_64(64, 8)`, `BITS_256(256, 16)`, `BITS_1024(1024, 32)`.
  - `PHashSize` enum 정의 (DCT pHash 전용, `bits`, `internal val resize`, `internal val lowSide`):
    - `BITS_64(64, 32, 8)`, `BITS_256(256, 64, 16)`, `BITS_1024(1024, 128, 32)`.
  - **C1 fix**: `HashSize`와 `PHashSize`는 별도 enum — `typealias` 금지. resize 파라미터가 달라 `phashOf(BITS_64)[0] == phash()` 불변식이 `PHashSize.BITS_64.resize=32` 일 때만 보장됨.
  - `fun ImmutableImage.ahashOf(size: HashSize = HashSize.BITS_64): LongArray`
    - `scaleTo(size.gridSide, size.gridSide, HASH_SCALE_METHOD)` → grayscale (luminance) → 평균 계산 → 픽셀 ≥ 평균이면 비트 1, 아니면 0.
    - 결과: `LongArray(ceil(bits/64))` (BITS_64 → size 1, BITS_256 → 4, BITS_1024 → 16). 비트 순서: row-major, LSB = first bit.
  - `fun ImmutableImage.ahash(): Long = ahashOf(HashSize.BITS_64)[0]` (편의 단축형).
  - 한국어 KDoc 필수: 알고리즘 요약 + 비트폭별 비용/정밀도 트레이드오프.
  - `companion object : KLoggingChannel()` 추가.
- **검증**:
  - 동일 이미지 self → distance=0 (T2~T5 통합 테스트에서).
  - `ahashOf(BITS_64).size == 1`, `ahashOf(BITS_256).size == 4`, `ahashOf(BITS_1024).size == 16`.
  - 커밋은 T5 완료 후 통합.
- **의존**: T1.

---

### T3. dHash 구현
- **complexity**: high
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HashSimilarity.kt` (수정/추가).
- **내용**:
  - `fun ImmutableImage.dhashOf(size: HashSize = HashSize.BITS_64): LongArray`
    - `scaleTo(size.gridSide + 1, size.gridSide, HASH_SCALE_METHOD)` → grayscale.
    - 인접 픽셀(같은 row, x vs x+1) 휘도 비교 → 좌측 < 우측이면 비트 1.
    - 비트 수 = `gridSide * gridSide`. 결과 LongArray 길이 = `ceil(bits/64)`.
  - `fun ImmutableImage.dhash(): Long = dhashOf(HashSize.BITS_64)[0]`.
  - 한국어 KDoc: 그래디언트 기반, JPEG 압축에 견고.
- **검증**:
  - 동일 이미지 → distance=0.
  - JPEG 90% 재저장 → distance ≤ 4 (T2~T5 통합 테스트).
- **의존**: T1, T2 (같은 파일).

---

### T4. wHash 구현
- **complexity**: high
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HashSimilarity.kt` (수정/추가).
- **내용**:
  - `fun ImmutableImage.whashOf(size: PHashSize = PHashSize.BITS_64): LongArray`
    - **OQ-1 확정**: wHash는 `PHashSize` 재사용 (DCT pHash와 동일 구조).
    - `scaleTo(size.resize, size.resize, HASH_SCALE_METHOD)` → BITS_64 = 32×32.
    - grayscale 매트릭스 → `haarTransform2d(matrix, levels = 3)` (3-level → BITS_64에서 LL = 8×8 = 64 coeff).
    - `size.lowSide × size.lowSide` LL subband 추출 → 평균 기준 비트화.
    - 결과: `LongArray(ceil(bits/64))`. BITS_64 → LongArray(1).
  - `fun ImmutableImage.whash(): Long = whashOf(PHashSize.BITS_64)[0]`.
  - `PHashSize.resize`가 항상 2^n(32/64/128)임은 enum 정의로 보장.
- **검증**:
  - 비-2^n 입력(임의 사진) → `scaleTo(32, 32)` 강제 → 정상 동작.
  - JPEG 90% → distance ≤ 6.
- **의존**: T1 (haarTransform2d), T2 (PHashSize enum은 T5에서 정의 → T4는 T5 이후 또는 동일 파일).

---

### T5. phashOf + HashDistance
- **complexity**: medium
- **파일**:
  - 수정: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HashSimilarity.kt` — `phashOf`, `HashDistance`, `phashOfDistanceTo` 추가.
  - 수정: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/ImageSimilarity.kt` — `hammingDistance(Long, Long)` `@Deprecated` 처리.
- **내용**:
  - `fun ImmutableImage.phashOf(size: PHashSize = PHashSize.BITS_64): LongArray`
    - 기존 `phash()` 알고리즘 일반화: `scaleTo(size.resize, size.resize, HASH_SCALE_METHOD)` → grayscale → DCT-II → 좌상단 `size.lowSide × size.lowSide` 저주파 추출 → 평균(DC 성분 `low[0]` 제외) 기준 비트화.
    - 비트 순서: row-major, LSB = first bit.
    - **불변식**: `phashOf(PHashSize.BITS_64)[0] == phash()` (resize=32, lowSide=8 동일).
  - `object HashDistance`:
    - `fun hamming(a: Long, b: Long): Int = (a xor b).countOneBits()`
    - `fun hamming(a: LongArray, b: LongArray): Int` — `require(a.size == b.size) { "..." }`, 누산.
  - `fun ImmutableImage.phashOfDistanceTo(other: ImmutableImage, size: PHashSize = PHashSize.BITS_64): Int`
    - `HashDistance.hamming(this.phashOf(size), other.phashOf(size))`.
  - 기존 `hammingDistance(Long, Long)`:
    - `@Deprecated("HashDistance.hamming(a, b) 사용", ReplaceWith("HashDistance.hamming(a, b)"))` 어노테이션 + 위임 구현.
- **검증**:
  - `phashOf(BITS_64)[0] == phash()` 단위 테스트.
  - `HashDistance.hamming(LongArray, LongArray)` 길이 불일치 시 `IllegalArgumentException`.
  - 기존 `phashDistanceTo` 동작 무변경 (deprecation warning만 추가).
- **의존**: T1.

---

### T2~T5 통합 테스트: HashSimilarityTest
- **complexity**: medium
- **파일**: `utils/images/src/test/kotlin/io/bluetape4k/images/similarity/HashSimilarityTest.kt`
- **내용**:
  - 테스트 클래스: `class HashSimilarityTest : AbstractImageTest()`, `companion object : KLoggingChannel()`.
  - 사용 자산: `homer.jpg`, `landscape.jpg`. JPEG 90% 재저장본/밝기 +10/좌우 5px 시프트는 setup에서 scrimage로 즉석 생성.
  - 케이스 (bluetape4k-assertions 비교 matcher 사용):
    - `동일 이미지의 모든 해시는 distance가 0`: aHash/dHash/wHash/pHash(64) 4종.
    - `JPEG 90% 재저장 후 해시 거리는 임계값 이하`: aHash ≤ 4, dHash ≤ 4, wHash ≤ 6, pHash(64) ≤ 4.
    - `다른 이미지의 해시 거리는 20 초과`.
    - `phashOf(BITS_64)[0]은 phash()와 동일하다`.
    - `HashDistance.hamming은 길이 불일치 시 IllegalArgumentException`.
    - `wHash는 비-2^n 입력도 정상 동작한다`.
    - `BITS_256/BITS_1024 LongArray 길이 검증`: `hash.size shouldBeEqualTo 4`, `hash.size shouldBeEqualTo 16`.
  - `./gradlew :bluetape4k-images:test --tests "*HashSimilarityTest"` PASS.
  - 커밋: `feat: aHash/dHash/wHash 및 pHash 비트폭 옵션 추가`.
- **의존**: T2, T3, T4, T5.

---

### T6. MssimSimilarity.kt
- **complexity**: high
- **파일**: 신규 `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/MssimSimilarity.kt`.
- **내용**:
  - 상수: `MSSIM_DEFAULT_WINDOW = 11`, `MSSIM_DEFAULT_SIGMA = 1.5`.
  - `fun ImmutableImage.mssimTo(other: ImmutableImage, windowSize: Int = MSSIM_DEFAULT_WINDOW, sigma: Double = MSSIM_DEFAULT_SIGMA): Double`
    - `requireSameSize(other)` 호출.
    - `require(windowSize >= 3 && windowSize % 2 == 1) { "windowSize는 3 이상의 홀수: $windowSize" }`.
    - `require(min(width, height) >= windowSize) { "이미지 변($width x $height)이 windowSize($windowSize)보다 작습니다" }`.
    - `gaussianKernel1d(windowSize, sigma)` 사용 → 2D separable 가우시안 가중치.
    - 슬라이딩 윈도우 (stride=1) 별로 가중 평균/분산/공분산 계산 → 표준 SSIM 공식 적용.
    - 모든 윈도우 SSIM 평균 반환.
  - `companion object : KLoggingChannel()`.
  - KDoc: Wang 2004 논문 명시, 글로벌 `ssimTo`와 차이 설명.
- **검증**:
  - 동일 이미지 → MSSIM ≥ 0.999.
  - 짝수 windowSize → IllegalArgumentException.
  - 5×5 + windowSize=11 → IllegalArgumentException.
- **의존**: T1.

---

### T6-C. MSSIM threshold calibration
- **complexity**: low
- **파일**: 측정 후 spec §7.2의 임계값을 확정 → 테스트 코드(T6 테스트)에 반영.
- **내용**:
  - 임시 측정 스크립트 또는 일회성 테스트로 다음 값을 측정:
    - `homer.jpg` → JPEG 90% 재저장 후 `mssimTo`.
    - `homer.jpg` → 밝기 +10 후 `mssimTo`.
  - 측정값 기록 — `docs/testlogs/2026-04.md` 상단 행에 추가. spec §7.2의 `> 0.7*` 임계값을 (실측값 - 0.05 마진) 기준으로 갱신.
  - T6 테스트의 임계값을 확정값으로 업데이트.
  - **spec §7.2 갱신 commit**: 임계값 확정 후 spec 파일도 수정하여 `*` placeholder를 실제 값으로 교체, `git commit -m "docs: MSSIM 임계값 calibration 결과 반영"` 으로 별도 commit.
- **검증**:
  - 측정값이 `MssimSimilarityTest`의 기대 임계값과 일치.
  - 측정값을 plan 본문 또는 testlog에 기록.
- **의존**: T6.

---

### T6 테스트: MssimSimilarityTest
- **complexity**: medium
- **파일**: 신규 `utils/images/src/test/kotlin/io/bluetape4k/images/similarity/MssimSimilarityTest.kt`.
- **내용**:
  - 케이스:
    - `동일 이미지의 MSSIM은 1에 매우 가깝다`: `score shouldBeGreaterThan 0.999`.
    - `짝수 windowSize는 IllegalArgumentException`: `assertFailsWith<IllegalArgumentException> { img.mssimTo(img, windowSize = 10) }`.
    - `windowSize가 이미지 변보다 크면 IllegalArgumentException`: 5×5 이미지 + windowSize=11.
    - `JPEG 90% 재저장 후 MSSIM은 calibrated 임계값 이상` (T6-C에서 확정).
    - `밝기 +10 후 MSSIM은 calibrated 임계값 이상` (T6-C).
    - `글로벌 ssimTo와 mssimTo 결과는 다르다 (국소성 차이)`: 충분히 차이가 큰 케이스로 비교.
    - `다른 이미지(homer vs landscape)의 MSSIM은 0.5 미만`: `score shouldBeLessThan 0.5`.
  - 커밋: `feat: MSSIM (11x11 sliding window) 구현`.
- **의존**: T6, T6-C.

---

### T7. HistogramSimilarity — ChiSquare
- **complexity**: high
- **파일**: 신규 `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HistogramSimilarity.kt`.
- **내용**:
  - `enum class ColorSpace { RGB, HSV }`.
  - `internal fun buildHistogram(img: ImmutableImage, colorSpace: ColorSpace, binsPerChannel: Int): Array<DoubleArray>`
    - `require(binsPerChannel in 2..256) { "binsPerChannel은 2..256 범위: $binsPerChannel" }`.
    - 채널별 분리: RGB → R/G/B 3채널, HSV → H/S/V 3채널 (`hsvComponents` 사용).
    - bin 인덱스: `min(floor(value * binsPerChannel).toInt(), binsPerChannel - 1)` — 경계값 clamp 필수 (HSV는 [0,1] float, RGB는 [0,255] → /256.0).
    - 정규화: 채널별 합 = 1.0 (zero-histogram이면 그대로 0 유지).
    - 반환: `Array<DoubleArray>` — `[channel][bin]`.
  - `sealed interface HistogramSimilarity { fun measure(a: ImmutableImage, b: ImmutableImage): Double }`.
  - `data class ChiSquare(colorSpace = RGB, binsPerChannel = 32) : HistogramSimilarity`
    - 동작: `pHist = buildHistogram(a)`, `qHist = buildHistogram(b)`.
    - **zero-histogram**: 두 이미지 모두 모든 채널 합 = 0 → return 1.0.
    - 채널별 `d_c = sum((p-q)^2 / (p+q+ε))`, ε = 1e-10. 채널 평균.
    - `similarity = exp(-d / 2)`.
  - 한국어 KDoc 필수.
- **검증**:
  - 동일 이미지 → 1.0.
  - zero-histogram → 1.0.
  - bin 범위 밖 입력 → IllegalArgumentException.
- **의존**: T1.

---

### T8. Bhattacharyya
- **complexity**: medium
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HistogramSimilarity.kt` (수정).
- **내용**:
  - `data class Bhattacharyya(colorSpace = RGB, binsPerChannel = 32) : HistogramSimilarity`
    - `coefficient = sum(sqrt(p*q))` 채널 평균.
    - **zero-histogram** → 1.0.
    - `similarity = coefficient` (직접 사용, 0..1 범위).
- **검증**:
  - 동일 이미지 → 1.0.
- **의존**: T7.

---

### T9. EarthMover
- **complexity**: high
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HistogramSimilarity.kt` (수정).
- **내용**:
  - `data class EarthMover(colorSpace = RGB, binsPerChannel = 32) : HistogramSimilarity`
    - 채널별 1D CDF 차이 합산: `emd_c = sum(|cdfP_i - cdfQ_i|)` for i in 0..bins-1.
    - 전체 `emd = sum(emd_c)`.
    - `dMax = channels * (binsPerChannel - 1)`.
    - **zero-histogram** → 1.0.
    - `similarity = 1 - emd / dMax` (clamp [0,1]).
  - KDoc: H 채널 wrap 미적용 명시 (V/S 또는 RGB 권장).
- **검증**:
  - 동일 이미지 → 1.0.
  - 완전 다른 분포 → 0에 근접.
- **의존**: T7.

---

### T10. companion 팩토리 + 편의 함수
- **complexity**: low
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HistogramSimilarity.kt` (수정).
- **내용**:
  - `HistogramSimilarity.Companion`:
    - `fun chiSquare(colorSpace = RGB, bins = 32): HistogramSimilarity = ChiSquare(...)`.
    - `fun bhattacharyya(...) = Bhattacharyya(...)`.
    - `fun earthMover(...) = EarthMover(...)`.
  - Top-level:
    - `fun ImmutableImage.histogramSimilarityTo(other: ImmutableImage, measure: HistogramSimilarity = HistogramSimilarity.chiSquare()): Double = measure.measure(this, other)`.
- **검증**:
  - 팩토리 호출 + `measure()` 호출 결과 일치.
- **의존**: T7, T8, T9.

---

### T7~T10 통합 테스트: HistogramSimilarityTest
- **complexity**: medium
- **파일**: 신규 `utils/images/src/test/kotlin/io/bluetape4k/images/similarity/HistogramSimilarityTest.kt`.
- **내용**:
  - 케이스:
    - `동일 이미지의 모든 히스토그램 유사도는 1에 매우 가깝다` (ChiSquare/Bhattacharyya/EMD).
    - `다른 이미지의 ChiSquare/Bhattacharyya/EMD는 0.5 미만` (homer vs landscape).
    - `RGB와 HSV 색공간 결과는 다르다`.
    - `크기/종횡비가 다른 동일 색감 이미지의 유사도는 1에 가깝다` (homer 원본 vs scaleTo(50%)).
    - `zero-histogram 입력 → 1.0`.
    - `binsPerChannel 1 또는 257 → IllegalArgumentException`.
  - 커밋: `feat: 컬러 히스토그램 유사도 (ChiSquare/Bhattacharyya/EMD) 추가`.
- **의존**: T10.

---

### T11. blockMeanDescriptor + blockMeanSimilarityTo
- **complexity**: medium
- **파일**: 신규 `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/KeypointSimilarity.kt`.
- **내용**:
  - 상수: `BLOCK_MEAN_DEFAULT_GRID = 8`.
  - `fun ImmutableImage.blockMeanDescriptor(gridRows: Int = 8, gridCols: Int = 8): DoubleArray`
    - `require(gridRows >= 1 && gridCols >= 1)`.
    - 이미지를 (높이/gridRows) × (너비/gridCols) 그리드로 분할 → 셀별 평균 휘도.
    - 휘도 0~1 정규화 (`luminance(p) / PIXEL_MAX`).
    - 길이 `gridRows * gridCols` DoubleArray.
  - `fun ImmutableImage.blockMeanSimilarityTo(other: ImmutableImage, gridRows: Int = 8, gridCols: Int = 8): Double`
    - `descA = blockMeanDescriptor`, `descB = other.blockMeanDescriptor`.
    - L2 거리 = `sqrt(sum((a-b)^2))`.
    - 반환: `1.0 / (1.0 + L2)` ∈ (0, 1]. 동일 이미지 = 1.0.
  - `companion object : KLoggingChannel()`.
- **검증**:
  - 동일 이미지 → 1.0.
- **의존**: T1.

---

### T12. bestRotationSimilarityTo
- **complexity**: medium
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/KeypointSimilarity.kt` (수정).
- **내용**:
  - `fun ImmutableImage.bestRotationSimilarityTo(other: ImmutableImage, gridRows: Int = 8, gridCols: Int = 8): Double`
    - 4가지 회전:
      - 0°: `other`
      - 90° CCW: `other.rotateLeft()`
      - 180°: `other.rotateLeft().rotateLeft()`
      - 270° CCW (= 90° CW): `other.rotateLeft().rotateLeft().rotateLeft()` 또는 `other.rotateRight()`
    - 각각 `blockMeanSimilarityTo` 계산 → 최댓값 반환.
  - KDoc: 90° 단위만 견고, 임의 각도는 SIFT/ORB 별도 이슈 명시.
- **검증**:
  - 90° 회전 변형 → `bestRotationSimilarityTo > 0.9`, `blockMeanSimilarityTo < 0.9`.
- **의존**: T11.

---

### T11~T12 통합 테스트: KeypointSimilarityTest
- **complexity**: medium
- **파일**: 신규 `utils/images/src/test/kotlin/io/bluetape4k/images/similarity/KeypointSimilarityTest.kt`.
- **내용**:
  - 케이스:
    - `동일 이미지의 blockMeanSimilarityTo는 1에 매우 가깝다`.
    - `JPEG 90% 재저장 후 blockMeanSimilarityTo > 0.9`.
    - `90° 회전 후 blockMeanSimilarityTo < 0.9`.
    - `90° 회전 후 bestRotationSimilarityTo > 0.9`.
    - `다른 이미지(homer vs landscape)의 blockMeanSimilarityTo < 0.5`.
    - `gridRows 0 → IllegalArgumentException`.
  - 커밋: `feat: Block-Mean descriptor 기반 키포인트 유사도 추가`.
- **의존**: T12.

---

### T13. README + KDoc 동기화
- **complexity**: low
- **파일**:
  - 수정: `utils/images/README.md`, `utils/images/README.ko.md`.
  - 수정: `ImageSimilarity.kt` 헤더 KDoc.
- **내용**:
  - README "Similarity" 섹션 신설/확장:
    - 6개 신규 API 표 (`mssimTo`, `ahash/dhash/whash/phashOf`, `histogramSimilarityTo`, `blockMeanSimilarityTo`, `bestRotationSimilarityTo`).
    - Mermaid UML 다이어그램 — `similarity` 패키지의 파일 구조와 의존:
      ```
      ImageSimilarity ─→ SimilarityInternals
      MssimSimilarity ─→ SimilarityInternals
      HashSimilarity  ─→ SimilarityInternals
      HistogramSimilarity ─→ SimilarityInternals
      KeypointSimilarity ─→ SimilarityInternals
      ```
    - 사용 예시 코드 블록 (한국어 README는 한국어 설명, 영문 README는 영문).
  - `ImageSimilarity.kt` 헤더 KDoc의 "제공 지표" 표에 신규 행 추가.
- **검증**:
  - README.md / README.ko.md 모두 갱신.
  - Mermaid 코드블록 렌더링 검증 (GitHub 미리보기).
  - 커밋: `docs: image similarity 확장 README/KDoc 업데이트`.
- **의존**: T1~T12.

---

### T14. Code Review
- **complexity**: low
- **파일**: 코드 리뷰 결과에 따라 수정.
- **내용**:
  - `oh-my-claudecode:code-reviewer` (또는 `pr-review-toolkit:code-reviewer`) 에이전트 실행.
  - HIGH/CRITICAL 이슈 0건이 될 때까지 수정.
  - MEDIUM은 가능한 범위에서 수정.
- **검증**:
  - 리뷰 결과(파일·심각도·수정 커밋) 보고에 포함.
- **의존**: T13.

---

### T15. 최종 테스트 + PR
- **complexity**: low
- **파일**: PR 본문, `docs/testlogs/2026-04.md`.
- **내용**:
  - `./gradlew :bluetape4k-images:test` 전수 실행.
  - 결과 (passing count + duration)을 `docs/testlogs/2026-04.md` 이달 파일 표 맨 위에 행으로 추가.
  - PR 본문 작성:
    - 변경 의도 (이슈 #130 요약).
    - 신규 API 목록.
    - 테스트 결과 (통과 카운트 + duration).
    - 검증 명령 (`./gradlew :bluetape4k-images:test`).
    - Plan 대비 완료 비교 표 (T1~T16).
  - `gh pr create --base develop --head issue-130-image-similarity --title "feat: image similarity 지표 확장 (MSSIM/aHash/dHash/wHash/Histogram/BlockMean) — issue #130"` 비대화형.
- **검증**:
  - 모든 테스트 PASS.
  - PR URL 보고에 포함.
- **의존**: T14.

---

### T16. Superpowers index + wiki-update
- **complexity**: low
- **파일**:
  - 수정: `docs/superpowers/index/2026-04.md` (이달 파일 맨 위에 새 항목).
  - 수정: `docs/superpowers/INDEX.md` (카운트 갱신).
- **내용**:
  - 항목: `image-similarity-design` (spec → plan → 구현 → PR 링크).
  - `/wiki-update` 스킬 실행 → Obsidian wiki 페이지에 spec/plan 반영 + qmd 재인덱싱.
- **검증**:
  - INDEX 카운트 일치.
  - Obsidian wiki에서 새 페이지 검색 가능.
  - 커밋: `docs: superpowers index 및 wiki 업데이트`.
- **의존**: T15.

---

## 완료 기준 (Acceptance Criteria 매핑)

| Spec §11 항목 | 충족 task |
|---|---|
| 신규 5개 파일 ≤ 800라인 | T1, T6, T7, T11 (각 파일 라인 추정 §5.1) |
| 모든 신규 public API에 한국어 KDoc | T2~T12 |
| self-similarity 1.0 / dist=0 | T2~T5 / T6 / T7~T10 / T11~T12 통합 테스트 |
| JPEG 90% MSSIM > calibrated 임계값 | T6, T6-C |
| 90° 회전 → bestRotationSimilarityTo > 0.9 | T11~T12 통합 테스트 |
| 크기 다른 이미지: 히스토그램 OK, pixel/MSSIM throws | T7~T10 통합 테스트, T6 테스트 |
| 기존 API 시그니처 무변경 | T1, T5 (회귀 테스트) |
| `./gradlew :bluetape4k-images:test` 전수 통과 | T15 |
| README.md / README.ko.md 동기화 | T13 |
| `docs/superpowers/index/2026-04.md` 업데이트 | T16 |

---

## 위험 / 백업 플랜

- **MSSIM 임계값 측정 결과가 0.7 미만** → spec §11의 `> 0.7` 가이드는 spec §7.2에서 calibration 후 수정한다고 명시됨. T6-C에서 측정값 - 0.05 마진을 적용하여 테스트 임계값 갱신.
- **wHash 신호 손실 의심** → spec §2.2 완화책에 따라 32×32 강제. 테스트 실패 시 `levels`를 1 → 다단계로 조정.
- **HSV EMD H 채널 wrap 영향** → spec §2.6에 따라 1차 PR은 RGB 또는 V/S 권장. H 채널 wrap 보정은 별도 이슈.
- **1024bit 비교 비용** → spec §2.4. `Long.countOneBits` 사용 + LongArray 직접 비교로 GC 압박 회피.

---

## Task 요약 표

| ID | 제목 | complexity | 의존 |
|---|---|---|---|
| T1 | SimilarityInternals.kt 추출 | medium | — |
| T2 | aHash 구현 | high | T1 |
| T3 | dHash 구현 | high | T1, T2 |
| T4 | wHash 구현 | high | T1, T2 |
| T5 | phashOf + HashDistance | medium | T1 |
| T2~T5 테스트 | HashSimilarityTest | medium | T2, T3, T4, T5 |
| T6 | MssimSimilarity.kt | high | T1 |
| T6-C | MSSIM threshold calibration | low | T6 |
| T6 테스트 | MssimSimilarityTest | medium | T6, T6-C |
| T7 | HistogramSimilarity ChiSquare | high | T1 |
| T8 | Bhattacharyya | medium | T7 |
| T9 | EarthMover | high | T7 |
| T10 | companion + 편의 함수 | low | T7, T8, T9 |
| T7~T10 테스트 | HistogramSimilarityTest | medium | T10 |
| T11 | blockMeanDescriptor + blockMeanSimilarityTo | medium | T1 |
| T12 | bestRotationSimilarityTo | medium | T11 |
| T11~T12 테스트 | KeypointSimilarityTest | medium | T12 |
| T13 | README + KDoc | low | T1~T12 |
| T14 | Code Review | low | T13 |
| T15 | 최종 테스트 + PR | low | T14 |
| T16 | Superpowers index + wiki-update | low | T15 |

총 21개 task (4개 통합 테스트 task 포함). 전수 완료 후 PR 생성.
