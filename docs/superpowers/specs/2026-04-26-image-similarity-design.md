# Image Similarity 모듈 확장 설계 (utils/images)

- **이슈**: #130 — utils/images 유사도 지표 확장
- **브랜치**: `issue-130-image-similarity` (`.worktrees/issue-130-image-similarity`)
- **작성일**: 2026-04-26
- **모듈**: `bluetape4k-images` (`utils/images`)
- **의존성 가정**: scrimage `4.3.10`, Kotlin `2.3`, JDK `21`, BoofCV 미도입(1차)

---

## 1. Problem & Constraints

### 1.1 현 상태 (`ImageSimilarity.kt`, 296라인)

기존 코드는 단일 파일에 다음 6개 지표를 top-level 확장 함수로 제공한다.

| 함수 | 동작 | 한계 |
|---|---|---|
| `pixelAvgDeltaTo` / `pixelMaxDeltaTo` | 채널별 절대 차이 평균/최대 | 픽셀 레벨, 시각적 유사도와 괴리 |
| `mseTo` / `psnrTo` | RGB MSE 및 PSNR | 인지적 거리 측정 부족 |
| `ssimTo` | **글로벌 SSIM** (전체 이미지를 단일 윈도우로 처리) | 사실상 정규화된 cross-correlation. 국소 구조 무시. 표준 SSIM과 결과 다름 |
| `phash()` | 32×32 → DCT-II → 8×8 저주파 → 64bit 해시 | **64bit 고정**, 비트 수 옵션 없음. 작은 변형에는 유용하지만 대규모 코퍼스에서 충돌률↑ |
| `phashDistanceTo` | pHash Hamming distance | 단일 알고리즘 — aHash/dHash/wHash 없음 |
| `hammingDistance(Long, Long)` | 64bit 비트 차이 | LongArray (256bit/1024bit) 미지원 |

내부 헬퍼: `luminance(Pixel)`, `requireSameSize`, `dct2d(input, n)`, 상수 `PIXEL_MAX`, `SSIM_C1/C2`, `PHASH_SIZE/LOW_SIZE/BITS`.

### 1.2 요구 기능 (이슈 #130)

1. **MSSIM** — 11×11 sliding window 기반 표준 SSIM (Wang 2004). 글로벌 SSIM의 국소성 부족을 해소.
2. **aHash / dHash / wHash** — pHash 외 보조 지각 해시 3종.
3. **pHash 비트 폭 옵션** — 64 / 256 / 1024bit.
4. **컬러 히스토그램 유사도** — Chi-Square, Bhattacharyya, Earth Mover's Distance (1D EMD). RGB 및 HSV 색공간.
5. **키포인트 기반 매칭** — 회전/이동에 부분적으로 견고한 descriptor 비교.

### 1.3 실무 시나리오

- **eCommerce 중복 상품 이미지 제거**: 서로 다른 사이즈/JPEG 품질로 업로드된 동일 이미지 식별 → pHash + MSSIM 조합.
- **콘텐츠 추천 (유사 이미지 검색)**: 색감 유사도(히스토그램) + 구조 유사도(MSSIM/pHash) 가중 스코어.
- **저작권/표절 검사**: 잘라내기·회전된 변형 탐지 → 키포인트 + 다중 해시 비교.
- **이미지 회귀 테스트**: 픽셀 레벨(`pixelMaxDelta`) + 인지 레벨(`mssim`) 동시 검증.

### 1.4 제약

- **scrimage 4.3.10 + JDK 표준 라이브러리 only** — 이슈 #130 1차 PR에서는 BoofCV/OpenCV 도입 회피.
- **파일당 최대 ~800라인** — 현 파일에 5개 기능 추가 시 1500라인 초과 → 분할 필수.
- **공개 API 안정성** — 기존 함수 시그니처 변경 금지, deprecated 처리 없이 그대로 유지.
- **bluetape4k 컨벤션** — top-level 확장 함수 + `companion object : KLoggingChannel()` + KDoc(한국어 허용).
- **JUnit 5 + MockK + bluetape4k-assertions** — 테스트 프레임워크 고정. `shouldBeInRange` 등 비교 matcher 사용.
- **재현성** — 동일 입력 동일 결과(난수 사용 금지). 해시 알고리즘에서 `ScaleMethod.Bicubic` 고정 (scrimage 기본값, `HASH_SCALE_METHOD` 상수화).
- **이미지 크기 정책 (통일 기준)**:
  | 측정 종류 | 크기 다른 이미지 처리 |
  |---|---|
  | `pixelAvgDeltaTo`, `mseTo`, `psnrTo`, `ssimTo`, `mssimTo` | `requireSameSize` — IllegalArgumentException |
  | `ahash`, `dhash`, `whash`, `phash`, `phashOf` | 내부 `scaleTo` 리사이즈 (크기 무관) |
  | `HistogramSimilarity.measure` | 허용 — 정규화된 히스토그램(sum=1.0) 비교 |
  | `blockMeanSimilarityTo`, `bestRotationSimilarityTo` | 내부 그리드 정규화 (크기 무관) |

---

## 2. Risks & Failure Modes

### 2.1 Risk: MSSIM sliding window — 작은 이미지

- **상황**: 이미지의 너비 또는 높이가 windowSize(11) 미만일 때 윈도우를 만들 수 없음.
- **영향**: ArrayIndexOutOfBoundsException 또는 NaN.
- **완화**:
  1. `min(width, height) < windowSize` → `IllegalArgumentException` with 명확한 메시지.
  2. (옵션) `windowSize` 자동 축소 모드는 도입하지 않음(예측 가능성 우선).
  3. 테스트에서 7×7, 11×11, 12×12 경계 케이스 명시적으로 검증.

### 2.2 Risk: wHash Haar wavelet — 2의 거듭제곱이 아닌 크기

- **상황**: Haar DWT는 입력 크기가 2^n 이어야 한다(level별 1/2 다운샘플).
- **영향**: 임의 입력 이미지를 그대로 변환 시 엣지 픽셀 손실 / 비대칭 변환.
- **완화**:
  1. wHash 입력을 항상 `WHASH_SIZE = 64` (2^6) 또는 `32`로 강제 리사이즈 후 변환.
  2. 8×8 저주파 블록 추출 → 64bit 해시.
  3. 사용자가 크기를 변경할 수 있더라도 `require(size and (size - 1) == 0)` 검증.

### 2.3 Risk: 히스토그램 유사도 — 크기 다른 이미지

- **상황**: pixelDelta/MSE/SSIM은 동일 크기 강제이나, 히스토그램은 크기·종횡비 무관 비교가 가능.
- **영향**: 정책 일관성 결여 시 사용자 혼란.
- **완화 (의도된 정책)**:
  1. 히스토그램 기반 측정은 **크기 다른 이미지 허용** — 정규화된 히스토그램(`sum=1.0`) 비교.
  2. KDoc + 함수명에 명시("크기 무관, 색감 분포만 비교").
  3. EMD 계산 시 1D 누적분포(CDF) 차이로 단순화 → O(N) 비용.

### 2.4 Risk: 1024bit 해시 LongArray 비교 비용

- **상황**: `PHashSize.BITS_1024` → 16개 Long. Hamming distance를 N×M 코퍼스에서 호출.
- **영향**: 충분히 빠르지만 잘못된 구현(예: BitSet 변환) 시 GC 압박.
- **완화**:
  1. `LongArray` 직접 비교 — `Long.countOneBits(a[i] xor b[i])` 누적.
  2. 길이 불일치 시 `IllegalArgumentException`.
  3. 마이크로벤치마크 필수는 아니나, 1024bit 1만쌍 비교 < 50ms 정도면 OK (체크리스트).

### 2.5 Risk: Block-Mean descriptor — 회전/스케일 미대응

- **상황**: keypoint 매칭으로 SIFT/ORB 수준의 robust 매칭을 기대할 수 있음.
- **영향**: 스케일·회전 변형에 약함 → 사용자 기대치와 결과 괴리.
- **완화**:
  1. KDoc에 명시: "이동/JPEG 압축에 견고. 회전·스케일에는 제한적. SIFT/ORB가 필요하면 BoofCV 통합을 별도 이슈에서 추진".
  2. 90° 회전 케이스만 옵션 매칭(`bestRotationSimilarityTo` — 0/90/180/270 회전 4개 비교 후 최댓값) 제공.
  3. Block-Mean descriptor는 8×8 그리드 평균 휘도 → L2 거리 → `1/(1+L2)` 정규화 점수.

### 2.6 Risk: HSV 변환 정확도

- **상황**: `Color.RGBtoHSB(r, g, b, null)` → FloatArray (H, S, V) ∈ [0,1]. H는 wrap-around (0과 1이 같은 빨강).
- **영향**: H bin을 단순 [0..255] 정수화하면 H=0과 H=255가 멀게 계산됨.
- **완화**:
  1. HSV 히스토그램은 H 채널을 circular bin으로 처리 — Bhattacharyya/Chi-Square는 영향 없음(쌍별 bin 비교).
  2. EMD에는 H 채널 wrap 보정 필요 → 1차 PR에서는 V/S 채널 사용 권장 또는 H 채널 EMD 미지원으로 제한.
  3. KDoc로 한계 명시.

---

## 3. Design Approaches

각 접근은 (a) 패키지 구조, (b) API 스타일, (c) 확장성, (d) 사용자 학습 비용 4축으로 비교한다.

### 3.1 접근 A — 파일 분할 + sealed interface 기반 측정자

```kotlin
sealed interface SimilarityMeasure<R> {
    fun measure(a: ImmutableImage, b: ImmutableImage): R
}

object Mssim : SimilarityMeasure<Double> { ... }
data class HistogramChiSquare(val space: ColorSpace) : SimilarityMeasure<Double> { ... }
```

- **장점**:
  - 측정 로직을 객체로 모델링 → 컴포지션·체인 적용 쉬움 (`measures.map { it.measure(a, b) }`).
  - `sealed`로 새 측정자 도입 시 컴파일러가 안내.
  - DI/테스트에서 fake 주입 용이.
- **단점**:
  - bluetape4k 컨벤션과 어긋남 — 기존 모든 지표가 top-level 확장 함수.
  - 매번 `Mssim.measure(a, b)` vs `a.mssimTo(b)` — 호출 사이트가 길어짐.
  - 기존 `ssimTo`/`phash` 와의 일관성 깨짐 (마이그레이션 부담).

### 3.2 접근 B — Top-level 확장 함수 + 파일 분할 (현 스타일 유지)

```kotlin
// MssimSimilarity.kt
fun ImmutableImage.mssimTo(other: ImmutableImage, windowSize: Int = 11): Double

// HashSimilarity.kt
fun ImmutableImage.ahash(): Long
fun ImmutableImage.dhash(): Long
fun ImmutableImage.whash(): Long
fun ImmutableImage.phash(size: PHashSize = PHashSize.BITS_64): LongArray  // ← 변경 필요
```

- **장점**:
  - 기존 `pixelAvgDeltaTo` / `phashDistanceTo` / `ssimTo` 와 일관.
  - import 만 하면 IDE 자동완성으로 발견.
  - Kotlin idiomatic — DSL 빌더 없이 자연스러움.
- **단점**:
  - 히스토그램처럼 "옵션 객체"가 필요한 경우(색공간, bin count) 인자가 늘어나기 쉬움.
  - 다형성 측정자 패턴 사용처(예: 일괄 비교)에서 어색함.

### 3.3 접근 C — 하이브리드 (top-level + 옵션 객체 sealed interface)

- 단순 측정 (`mssim`, `ahash` 등): top-level 확장 함수.
- 다형성 옵션이 있는 측정 (히스토그램의 색공간/거리 함수): sealed interface `HistogramSimilarity` + `companion` 팩토리.
- **장점**:
  - 단순 케이스는 짧게, 옵션 많은 케이스는 명시적으로.
  - 기존 컨벤션 보존 + 신규 기능에 표현력 부여.
  - sealed when 으로 거리 함수 추가 시 컴파일러가 안내.
- **단점**:
  - 두 스타일 혼재 — 학습 비용 약간 증가. KDoc/README에서 선택 기준 명확화 필요.

### 3.4 비교 표

| 축 | A (sealed only) | B (top-level only) | C (hybrid) |
|---|---|---|---|
| 기존 컨벤션 일관성 | 낮음 | 높음 | 높음 |
| 다형성/체인 사용성 | 높음 | 낮음 | 중간(필요한 곳만) |
| 학습 비용 | 중 | 낮음 | 낮음~중 |
| 옵션 많은 측정 표현 | 우수 | 인자 폭발 위험 | 우수 |
| 마이그레이션 비용 | 큼 | 없음 | 거의 없음 |

---

## 4. Selected Approach & Rationale

### 4.1 결정: **접근 C (하이브리드)**

- **단순 측정 (MSSIM, aHash/dHash/wHash, Block-Mean descriptor)** → top-level 확장 함수 (접근 B 스타일 유지).
- **히스토그램 유사도** → `sealed interface HistogramSimilarity` + companion 팩토리 + top-level 편의 함수.
- pHash는 **비트 폭 옵션화**를 위해 반환 타입을 `LongArray`로 변경하면 호환성 깨짐 → 기존 `phash(): Long` 유지하고 신규 `phashOf(size: PHashSize): LongArray` 를 추가하는 비파괴적 변경.

### 4.2 근거

1. **bluetape4k 일관성**: `pixelAvgDeltaTo`, `mseTo`, `ssimTo`, `phashDistanceTo` 등 모든 기존 API가 receiver 확장 함수. 신규 단순 측정도 같은 패턴이 자연스럽다.
2. **히스토그램은 옵션 차원이 많다**: 색공간(RGB/HSV) × 거리함수(ChiSquare/Bhattacharyya/EMD) × bin 수. 인자 5개를 받는 함수보다 `HistogramSimilarity.chiSquare(ColorSpace.HSV, bins = 64).measure(a, b)` 가 가독성·확장성 모두 우수.
3. **하위 호환**: 기존 `phash()`, `phashDistanceTo()`, `hammingDistance(Long, Long)` 모두 그대로 동작. 신규 비트폭 변종은 추가 함수.

### 4.3 채택하지 않은 안

- **접근 A** — 모든 측정을 sealed로 통일하면 기존 호출처(예상 50+ 콜사이트)를 모두 수정해야 함. ROI 낮음.
- **접근 B 만으로** 히스토그램 처리 시 `histogramChiSquareTo(other, colorSpace = HSV, bins = 64, normalize = true)` 식의 long-arg 함수가 됨 → DX 저하.

---

## 5. File Structure

```
utils/images/src/main/kotlin/io/bluetape4k/images/similarity/
├── ImageSimilarity.kt          (기존 유지: pixelDelta, MSE, PSNR, ssim(global), phash(64bit), phashDistance, hammingDistance(Long))
├── SimilarityInternals.kt      (신규 internal: luminance, requireSameSize, dct2d, gaussianKernel, haarTransform2d, hsvComponents)
├── MssimSimilarity.kt          (신규: mssimTo, gaussian-weighted 11x11 sliding window)
├── HashSimilarity.kt           (신규: ahash, dhash, whash, phashOf(PHashSize), HashDistance.hamming(LongArray, LongArray))
├── HistogramSimilarity.kt      (신규: sealed interface + ChiSquare/Bhattacharyya/EarthMover + ColorSpace + 편의 확장함수)
└── KeypointSimilarity.kt       (신규: blockMeanDescriptor, blockMeanSimilarityTo, bestRotationSimilarityTo)
```

테스트 구조 (대칭):

```
utils/images/src/test/kotlin/io/bluetape4k/images/similarity/
├── ImageSimilarityTest.kt           (기존)
├── MssimSimilarityTest.kt           (신규)
├── HashSimilarityTest.kt            (신규: aHash/dHash/wHash/pHash 비트폭)
├── HistogramSimilarityTest.kt       (신규)
└── KeypointSimilarityTest.kt        (신규)
```

`SimilarityInternals.kt` 의 `luminance`, `requireSameSize`, `dct2d` 는 기존 `ImageSimilarity.kt` 의 `private` → `internal` 로 이동. 같은 패키지에서 공유.

### 5.1 라인 수 추정

| 파일 | 추정 라인 (구현+KDoc) |
|---|---|
| ImageSimilarity.kt (slim 후) | ~250 |
| SimilarityInternals.kt | ~180 |
| MssimSimilarity.kt | ~220 |
| HashSimilarity.kt | ~280 |
| HistogramSimilarity.kt | ~320 |
| KeypointSimilarity.kt | ~200 |

모든 파일 800라인 이하, 모듈 전체 ≤ 1500라인.

---

## 6. API Design

### 6.1 MSSIM (`MssimSimilarity.kt`)

```kotlin
/**
 * 11×11 가우시안 가중 슬라이딩 윈도우 기반 SSIM 평균값(MSSIM)을 계산합니다.
 *
 * - Wang 등(2004)의 표준 SSIM. 글로벌 [ssimTo] 보다 국소 구조 보존성이 높습니다.
 * - 두 이미지는 동일한 크기여야 합니다.
 *
 * @param windowSize 윈도우 한 변 크기. 기본 11(권장). 홀수, 3 이상.
 * @param sigma 가우시안 표준편차. 기본 1.5(논문 권장).
 * @return MSSIM (-1.0 ~ 1.0, 완전 동일 = 1.0)
 * @throws IllegalArgumentException windowSize가 짝수이거나 이미지 변보다 큰 경우
 */
fun ImmutableImage.mssimTo(
    other: ImmutableImage,
    windowSize: Int = MSSIM_DEFAULT_WINDOW,   // 11
    sigma: Double = MSSIM_DEFAULT_SIGMA,      // 1.5
): Double
```

### 6.2 Hash 확장 (`HashSimilarity.kt`)

```kotlin
/**
 * pHash 비트 폭 옵션. 비트 수가 클수록 더 세밀하지만 비교 비용 증가.
 */
enum class PHashSize(val bits: Int, internal val resize: Int, internal val lowSide: Int) {
    BITS_64(64, 32, 8),       // 기존 호환 — 32x32 → DCT 8x8
    BITS_256(256, 64, 16),    // 64x64 → DCT 16x16
    BITS_1024(1024, 128, 32); // 128x128 → DCT 32x32
}

/**
 * aHash / dHash / wHash 비트폭 옵션.
 * [gridSide] = NxN 그리드 한 변 크기. 비트 수 = gridSide².
 */
enum class HashSize(val bits: Int, internal val gridSide: Int) {
    BITS_64(64, 8),       // 8×8 그리드 → 64bit
    BITS_256(256, 16),    // 16×16 그리드 → 256bit
    BITS_1024(1024, 32);  // 32×32 그리드 → 1024bit
}

/**
 * DCT pHash 비트폭 옵션.
 * [resize] = DCT 입력 이미지 크기, [lowSide] = 저주파 블록 한 변 크기.
 * 비트 수 = lowSide².
 */
enum class PHashSize(val bits: Int, internal val resize: Int, internal val lowSide: Int) {
    BITS_64(64, 32, 8),       // 32×32 → DCT 8×8 → 64bit (기존 phash() 호환)
    BITS_256(256, 64, 16),    // 64×64 → DCT 16×16 → 256bit
    BITS_1024(1024, 128, 32); // 128×128 → DCT 32×32 → 1024bit
}

/** 8×8(또는 옵션 크기) 평균 해시. size.gridSide × size.gridSide 리사이즈. */
fun ImmutableImage.ahashOf(size: HashSize = HashSize.BITS_64): LongArray

/** (gridSide+1)×gridSide 그레이스케일 → 인접 픽셀 차이. 그래디언트 기반, JPEG에 견고. */
fun ImmutableImage.dhashOf(size: HashSize = HashSize.BITS_64): LongArray

/**
 * Haar wavelet 해시. [PHashSize] 재사용 (resize/lowSide 구조 동일).
 * BITS_64: scaleTo(32,32) → 3-level DWT → 8×8 LL = 64bit.
 * ⚠️ 성능: 내부적으로 resize 이미지 크기만 처리하므로 매우 빠름 (~0.5ms).
 */
fun ImmutableImage.whashOf(size: PHashSize = PHashSize.BITS_64): LongArray

/**
 * DCT pHash 비트폭 옵션 버전. [PHashSize] 사용 (aHash/dHash/wHash의 [HashSize]와 별도 enum).
 *
 * `phashOf(PHashSize.BITS_64)[0] == phash()` 동일.
 * **비트 순서**: row-major, LSB = first bit, DC 성분(`low[0]`)은 평균에서 제외.
 * **스케일 메서드**: `HASH_SCALE_METHOD = ScaleMethod.Bicubic` 고정 (기존 `phash()` 와 동일).
 */
fun ImmutableImage.phashOf(size: PHashSize = PHashSize.BITS_64): LongArray

/** 편의 단축형 (64bit 하위 호환) */
fun ImmutableImage.ahash(): Long = ahashOf(HashSize.BITS_64)[0]
fun ImmutableImage.dhash(): Long = dhashOf(HashSize.BITS_64)[0]
fun ImmutableImage.whash(): Long = whashOf(PHashSize.BITS_64)[0]

/** 두 가변 길이 해시의 Hamming distance. */
object HashDistance {
    fun hamming(a: Long, b: Long): Int = (a xor b).countOneBits()
    fun hamming(a: LongArray, b: LongArray): Int   // require(a.size == b.size)
}

// ImageSimilarity.kt 의 기존 top-level 함수 → Deprecated, HashDistance.hamming 으로 위임
@Deprecated("HashDistance.hamming(a, b) 사용", ReplaceWith("HashDistance.hamming(a, b)"))
fun hammingDistance(a: Long, b: Long): Int = HashDistance.hamming(a, b)

/** [phashOf] 결과의 거리. */
fun ImmutableImage.phashOfDistanceTo(other: ImmutableImage, size: PHashSize): Int
```

### 6.3 Histogram (`HistogramSimilarity.kt`)

```kotlin
enum class ColorSpace { RGB, HSV }

/**
 * 컬러 히스토그램 기반 유사도 측정 전략.
 *
 * 크기·종횡비가 다른 이미지 비교 가능 — 정규화된 히스토그램(sum=1.0)으로 비교합니다.
 *
 * 측정값은 모두 `[0.0, 1.0]` 범위로 정규화되며, **클수록 유사**합니다(완전 동일 = 1.0).
 */
sealed interface HistogramSimilarity {

    /** 0.0(다름) ~ 1.0(동일). */
    fun measure(a: ImmutableImage, b: ImmutableImage): Double

    /**
     * Chi-Square 거리. d = sum((p-q)^2 / (p+q+ε))
     * 분포 차이에 민감, 작은 차이도 잘 포착.
     *
     * **정규화**: `similarity = exp(-d / 2)` (d=0 → 1.0, d→∞ → 0.0 연속 감소).
     * **zero-histogram**: 두 히스토그램 모두 0이면 `1.0` 반환.
     */
    data class ChiSquare(
        val colorSpace: ColorSpace = ColorSpace.RGB,
        val binsPerChannel: Int = 32,
    ) : HistogramSimilarity

    /**
     * Bhattacharyya 계수. coefficient = sum(sqrt(p*q)) ∈ [0,1]
     * 분포 중첩도 직접 측정 — coefficient = 1.0 이면 완전 동일.
     *
     * **정규화**: `similarity = sum(sqrt(p*q))` — 직접 사용 (추가 변환 없음).
     * **zero-histogram**: 두 이미지 모두 단색/빈 히스토그램이면 `1.0` 반환 (동일 이미지 간주).
     */
    data class Bhattacharyya(
        val colorSpace: ColorSpace = ColorSpace.RGB,
        val binsPerChannel: Int = 32,
    ) : HistogramSimilarity

    /**
     * 1D Earth Mover's Distance (CDF 차이의 합, 채널별 누산 후 합산).
     * H 채널에는 wrap-around 보정 미적용 — V/S 또는 RGB 권장.
     *
     * **정규화**: `dMax = channels * (binsPerChannel - 1)` 기준.
     * `similarity = 1 - emd / dMax` → [0,1].
     * **zero-histogram**: 두 히스토그램 모두 0이면 `1.0` 반환.
     */
    data class EarthMover(
        val colorSpace: ColorSpace = ColorSpace.RGB,
        val binsPerChannel: Int = 32,
    ) : HistogramSimilarity

    companion object {
        fun chiSquare(colorSpace: ColorSpace = ColorSpace.RGB, bins: Int = 32): HistogramSimilarity =
            ChiSquare(colorSpace, bins)

        fun bhattacharyya(colorSpace: ColorSpace = ColorSpace.RGB, bins: Int = 32): HistogramSimilarity =
            Bhattacharyya(colorSpace, bins)

        fun earthMover(colorSpace: ColorSpace = ColorSpace.RGB, bins: Int = 32): HistogramSimilarity =
            EarthMover(colorSpace, bins)
    }
}

/** 편의 함수 — `HistogramSimilarity.bhattacharyya().measure(a, b)` 단축형. */
fun ImmutableImage.histogramSimilarityTo(
    other: ImmutableImage,
    measure: HistogramSimilarity = HistogramSimilarity.chiSquare(),
): Double
```

### 6.4 Keypoint (Block-Mean) (`KeypointSimilarity.kt`)

```kotlin
/**
 * 이미지를 [gridRows]×[gridCols] 그리드로 나눠 각 셀의 평균 휘도를 정규화한 descriptor.
 *
 * 회전·스케일에는 제한적, 이동·JPEG 압축에는 견고. SIFT/ORB가 필요하면 BoofCV 통합 별도 이슈.
 *
 * @return 길이 `gridRows*gridCols` 의 평균 휘도 배열 (각 원소 0.0 ~ 1.0)
 */
fun ImmutableImage.blockMeanDescriptor(
    gridRows: Int = BLOCK_MEAN_DEFAULT_GRID,   // 8
    gridCols: Int = BLOCK_MEAN_DEFAULT_GRID,   // 8
): DoubleArray

/**
 * Block-Mean descriptor 의 L2 거리를 [0,1] 유사도로 정규화: `1 / (1 + L2)`.
 * 동일 이미지 = 1.0.
 */
fun ImmutableImage.blockMeanSimilarityTo(
    other: ImmutableImage,
    gridRows: Int = BLOCK_MEAN_DEFAULT_GRID,
    gridCols: Int = BLOCK_MEAN_DEFAULT_GRID,
): Double

/**
 * 0/90/180/270 회전 4가지 중 최대 [blockMeanSimilarityTo] 를 반환.
 * 회전 변형 가능성이 있을 때 사용.
 */
fun ImmutableImage.bestRotationSimilarityTo(
    other: ImmutableImage,
    gridRows: Int = BLOCK_MEAN_DEFAULT_GRID,
    gridCols: Int = BLOCK_MEAN_DEFAULT_GRID,
): Double
```

### 6.5 크기별 전처리 유틸리티 (`SimilarityInternals.kt` 또는 별도 top-level)

#### 성능 특성

| 알고리즘 | 1024×768 기준 | 병목 원인 |
|---|---|---|
| aHash / dHash / whash | < 1ms | 내부 리사이즈 후 소규모 연산 |
| phash BITS_64 | ~1ms | O(N³) DCT, N=32 |
| phash BITS_1024 | ~80ms | O(N³) DCT, N=128 — **배치 전용** |
| mssimTo | ~2–5초 | O(W×H×K²) K=11 — **⚠️ 실시간 불가** |
| Histogram | ~5ms | O(W×H) 1회 스캔 |
| blockMeanSimilarityTo | ~5ms | O(W×H) 1회 스캔 |
| bestRotationSimilarityTo | ~20ms | 4× blockMean + 3× 픽셀 복사 |

`mssimTo`는 4K 이미지에서 30–60초 소요. 대형 이미지는 **호출 전 다운스케일 필수**.

#### 전처리 래퍼

```kotlin
/**
 * 유사도 계산 전 이미지를 최대 [maxSide]px로 비율 유지 축소합니다.
 *
 * 사용 시나리오:
 * - MSSIM: 800px 이하 권장 (1024×768 → ~2-5초, 4K → 30-60초)
 * - Histogram / BlockMean: 512px 이하면 충분
 * - Hash 계열: 내부 리사이즈가 있으므로 전처리 불필요
 *
 * ```kotlin
 * // MSSIM 전 다운스케일
 * val score = img.prepareForSimilarity(800).mssimTo(other.prepareForSimilarity(800))
 *
 * // 히스토그램은 크기 무관이지만 속도 향상을 원할 때
 * val sim = img.prepareForSimilarity(512).histogramSimilarityTo(other.prepareForSimilarity(512))
 * ```
 *
 * 이미 [maxSide] 이하이면 원본 이미지를 그대로 반환합니다(복사 없음).
 *
 * @param maxSide 긴 변 최대 픽셀 수. 기본 512.
 * @return 축소된 이미지 또는 원본(크기가 이미 충분히 작은 경우)
 */
fun ImmutableImage.prepareForSimilarity(maxSide: Int = 512): ImmutableImage
```

#### 선택 가이드 (KDoc + README에 포함)

```
이미지 크기 → 알고리즘 선택
────────────────────────────
작은 이미지 (≤ 256px):  mssimTo 직접 호출 가능
중간 이미지 (≤ 800px):  prepareForSimilarity(800) 후 mssimTo
대형 이미지 (> 800px):  prepareForSimilarity(512) 후 hash 또는 histogram 권장
4K+ 이미지:             hash 계열만 (내부 32px/128px 리사이즈로 빠름)
```

### 6.6 Internals (`SimilarityInternals.kt`)

```kotlin
internal const val PIXEL_MAX = 255.0

internal fun ImmutableImage.requireSameSize(other: ImmutableImage)

/** ITU-R BT.601 luminance. 기존과 동일. */
internal fun luminance(p: Pixel): Double

/** O(N^3) 2D DCT-II — N ≤ 32 권장. 32 초과는 별도 FFT 기반 필요. */
internal fun dct2d(input: Array<DoubleArray>, n: Int): Array<DoubleArray>

/** 11×11 가우시안 가중치 (sigma=1.5) → DoubleArray (길이 windowSize). */
internal fun gaussianKernel1d(windowSize: Int, sigma: Double): DoubleArray

/** 정사각 2의 거듭제곱 입력에 대한 in-place Haar wavelet 단일 레벨 변환. */
internal fun haarTransform2d(matrix: Array<DoubleArray>, levels: Int = 1)

/** AWT Color.RGBtoHSB 호출 → Triple(H, S, V) ∈ [0,1]. */
internal fun hsvComponents(p: Pixel): Triple<Float, Float, Float>

/** 긴 변이 [maxSide] 초과 시 비율 유지 축소. 이미 작으면 원본 반환(복사 없음). */
internal fun ImmutableImage.scaleToMaxSide(maxSide: Int): ImmutableImage
```

---

## 7. Testing Strategy

### 7.1 공통 자산

- `AbstractImageTest.kt` 베이스 (기존).
- `Resourcex.getInputStream("images/<file>.jpg")` 로 골든 이미지 로드.
- 기존 자산: `homer.jpg`, `labor.jpg`, `cafe.jpg`, `landscape.jpg`.
- 신규 가공 — JPEG 90% 재저장본, 좌우 5px 시프트, 90° 회전, 밝기 ±10 — 은 테스트 setup 단계에서 scrimage 변환으로 즉석 생성(테스트 리소스 추가 없음).

### 7.2 시나리오 매트릭스

| 시나리오 | MSSIM | aHash | dHash | wHash | pHash(64) | pHash(256/1024) | Histogram | BlockMean |
|---|---|---|---|---|---|---|---|---|
| 동일 이미지 | ≈ 1.0 | dist=0 | dist=0 | dist=0 | dist=0 | dist=0 | ≈ 1.0 | ≈ 1.0 |
| JPEG 90% 재저장 | > 0.7* | dist≤4 | dist≤4 | dist≤6 | dist≤4 | dist≤scaled | > 0.95 | > 0.9 |
| 좌우 5px 시프트 | > 0.7 | dist≤10 | dist≤10 | — | dist≤10 | — | ≈ 1.0 | > 0.7 |
| 90° 회전 | < 0.5 | — | — | — | dist 다양 | — | ≈ 1.0 | bestRotation > 0.95 |
| 밝기 +10 | > 0.7* | dist 다양 | dist≤6 | dist≤6 | dist≤6 | — | ChiSquare 변화 | > 0.8 |
| 다른 이미지 (homer vs landscape) | < 0.5 | dist > 20 | dist > 20 | dist > 15 | dist > 20 | dist 비례 | < 0.5 | < 0.5 |
| 크기 다른 이미지 | throws | OK(내부 리사이즈) | OK | OK | OK | OK | OK (정책: 허용) | OK |

### 7.3 bluetape4k-assertions matcher 사용 규칙

- 비교는 비교 matcher 사용 (`shouldBeGreaterThan`, `shouldBeLessOrEqualTo`, `shouldBeInRange`).
- `(score > 0.9).shouldBeTrue()` 형태 금지 — 실패 시 실제 값이 메시지에 노출되지 않음.

```kotlin
// GOOD
score shouldBeGreaterThan 0.9
distance shouldBeLessOrEqualTo 4
hash.size shouldBeEqualTo 16  // BITS_1024 / 64
```

### 7.4 경계/예외 케이스

- MSSIM: 5×5 이미지 + windowSize=11 → `IllegalArgumentException` 메시지 검증.
- pHash: `BITS_64` 결과 LongArray.size == 1 이고 `phashOf(BITS_64)[0] == phash()` 일치.
- HashDistance.hamming(LongArray, LongArray): 길이 다르면 throw.
- wHash: 입력이 16×16, 32×32, 64×64 모두 동작 + 비-2^n 입력은 강제 리사이즈 후 정상.
- Histogram: 종횡비/크기 다른 두 이미지 동일 색감일 때 측정값 ≈ 1.0.

### 7.5 테스트 클래스 컨벤션

```kotlin
class MssimSimilarityTest : AbstractImageTest() {
    companion object : KLoggingChannel()

    @Test
    fun `동일 이미지의 MSSIM은 1에 매우 가까워야 한다`() { ... }

    @Test
    fun `windowSize가 이미지 변보다 크면 IllegalArgumentException`() { ... }
}
```

---

## 8. Implementation Order (Step 3 plan 힌트)

각 단계는 **RED → GREEN → REFACTOR** 사이클을 따른다. 단계 간 commit 분리.

### Step 3-1. Internals 추출 (리팩토링, 동작 변경 없음)

- `ImageSimilarity.kt` 의 `private` 헬퍼 → `SimilarityInternals.kt` `internal` 로 이동.
- 기존 테스트 그대로 통과해야 함 → 회귀 가드.
- 커밋: `refactor: similarity 내부 헬퍼 SimilarityInternals.kt 로 추출`.

### Step 3-2. HashSimilarity.kt — aHash, dHash, wHash, phashOf

- `PHashSize` enum + `phashOf(size): LongArray` 신규.
- `ahash()`, `dhash()`, `whash()` 신규.
- `HashDistance` object — `hamming(LongArray, LongArray)` 추가.
- `phashDistanceTo(other, size: PHashSize)` 오버로드.
- 테스트: `HashSimilarityTest`.
- 커밋: `feat: aHash/dHash/wHash 및 pHash 비트폭 옵션 추가`.

### Step 3-3. MssimSimilarity.kt — sliding window MSSIM

- `gaussianKernel1d` 헬퍼 internals 에 추가.
- `mssimTo(other, windowSize, sigma)` 구현.
- 작은 이미지 검증, 윈도우 짝수 검증.
- 테스트: `MssimSimilarityTest` (동일 이미지 ≈ 1.0, JPEG 90% > 0.9, 글로벌 `ssimTo` 와 별개의 결과 검증).
- 커밋: `feat: MSSIM (11x11 sliding window) 구현`.

### Step 3-4. HistogramSimilarity.kt — sealed interface + 3가지 측정

- `ColorSpace` enum + `hsvComponents` internals 추가.
- `sealed interface HistogramSimilarity` + `ChiSquare/Bhattacharyya/EarthMover` data class.
- 정규화 히스토그램 빌드 → 거리 계산 → `[0,1]` 유사도로 변환.
- `histogramSimilarityTo(other, measure)` top-level 편의 함수.
- 테스트: `HistogramSimilarityTest` (크기 다른 이미지 허용 검증 포함).
- 커밋: `feat: 컬러 히스토그램 유사도 (ChiSquare/Bhattacharyya/EMD) 추가`.

### Step 3-5. KeypointSimilarity.kt — Block-Mean descriptor

- `blockMeanDescriptor`, `blockMeanSimilarityTo`, `bestRotationSimilarityTo`.
- BoofCV 미사용 — scrimage `rotateLeft`/`rotateRight` 사용.
- 테스트: `KeypointSimilarityTest` (이동 견고성, 90° 회전은 `bestRotationSimilarityTo` 로만 매칭).
- 커밋: `feat: Block-Mean descriptor 기반 키포인트 유사도 추가`.

### Step 3-6. README 및 KDoc 업데이트

- `utils/images/README.md` 와 `README.ko.md` 양쪽 모두 신규 API 섹션 추가.
- `ImageSimilarity` KDoc 의 "제공 지표" 표를 업데이트 (MSSIM·aHash·dHash·wHash·Histogram·BlockMean 행 추가).
- 사용 예시 코드블록 추가.
- `docs/superpowers/index/2026-04.md` 항목 추가 → `docs/superpowers/INDEX.md` 카운트 갱신.
- `/wiki-update` 실행.
- 커밋: `docs: image similarity 확장 README/KDoc 업데이트`.

### Step 3-7. PR 전 체크리스트

1. `./gradlew :bluetape4k-images:test` 전수 통과 (passing count + duration 보고).
2. `oh-my-claudecode:code-reviewer` 또는 `pr-review-toolkit:code-reviewer` 실행 → HIGH/CRITICAL 0건.
3. `./bin/repo-status` 로 변경 파일 확인.
4. 모든 신규 public API에 KDoc.
5. PR 본문에 테스트 결과 + 변경 의도 + 검증 명령 명시.

---

## 9. Out of Scope (별도 이슈로 분리)

- **BoofCV 통합** — SIFT/ORB/SURF descriptor, FLANN 매칭. 의존성 평가 + 라이선스 검토 필요.
- **GPU 가속** — 대규모 코퍼스 비교 시 병렬화. JOML/JCuda 의존 검토 필요.
- **딥러닝 임베딩 기반 유사도** — CLIP, DINOv2 등. 별도 모듈(`utils/images-embedding`)로 분리 권장.
- **2D EMD with Hungarian solver** — H 채널 wrap 보정 포함. 1차 PR은 1D CDF 차이로 한정.
- **MSSIM 다중 스케일 (MS-SSIM)** — 5단계 다운샘플 평균. 1차에서는 단일 스케일.

---

## 10. BoofCV Decision Summary

| 항목 | 결정 |
|---|---|
| 1차 PR 도입 | **NO** |
| 사유 | (1) `Libs.kt` 미등록 — 사용자 승인 필요 (2) 라이선스 Apache-2.0 OK 이지만 의존 그래프 영향 평가 미실시 (3) Block-Mean 으로 일반 사용 케이스 충분 |
| 추후 도입 시 | 별도 이슈에서 SIFT/ORB feature + RANSAC 매칭 모듈로 분리. 모듈명 후보: `utils/images-cv` |
| 임시 대안 | `bestRotationSimilarityTo` 로 90° 회전만 부분 대응 |

---

## 11. Acceptance Criteria

- [ ] 신규 5개 파일 모두 ≤ 800라인.
- [ ] 모든 신규 public API에 한국어 KDoc + 예시.
- [ ] 동일 이미지 self-similarity 테스트가 모든 측정에서 통과 (MSSIM≈1.0, hash dist=0, histogram≈1.0).
- [ ] JPEG 90% 재저장 → MSSIM > **0.7** (경험적 하한), pHash dist ≤ 4. _(임계값은 구현 후 실제 측정값을 기준으로 T6 calibration task에서 확정)_
- [ ] 90° 회전 → `bestRotationSimilarityTo` > 0.9.
- [ ] 크기 다른 이미지 → 히스토그램 측정 정상, pixel/MSSIM 은 IllegalArgumentException.
- [ ] `ssimTo`/`phash`/`hammingDistance(Long, Long)` 등 기존 API 시그니처·동작 무변경.
- [ ] `./gradlew :bluetape4k-images:test` 전수 통과.
- [ ] README.md / README.ko.md 동기화.
- [ ] `docs/superpowers/index/2026-04.md` 업데이트.

---

## 12. Open Questions (구현 전 확인)

1. wHash 내부 리사이즈 크기 — 32×32(64bit hash)가 적절한가, 64×64로 갈 것인가? → 1차안: **32×32**, 8×8 저주파 = 64bit (다른 해시들과 폭 통일).
2. Histogram bins 기본값 — 32 vs 64? → 1차안: **32** (히스토그램 X 채널 수: RGB=3, HSV=3 → 96bin total). 옵션으로 변경 가능.
3. Block-Mean grid 기본 — 8×8 vs 16×16? → 1차안: **8×8** (descriptor=64), pHash와 정합.
4. `bestRotationSimilarityTo` 회전 단계 — 0/90/180/270 만 vs 임의 각도? → 1차안: **0/90/180/270만** (scrimage 직각 회전 무손실).

이 4개는 구현 시점에 final lock — 변경 시 spec 재서명 필요.

---

## 13. Draft Task List (Step 3 plan 입력용)

```
T1. SimilarityInternals.kt 추출 — private → internal 이동, 회귀 테스트 통과
T2. HashSimilarity.kt — aHash 구현 + 테스트
T3. HashSimilarity.kt — dHash 구현 + 테스트
T4. HashSimilarity.kt — wHash 구현 (Haar 헬퍼 포함) + 테스트
T5. HashSimilarity.kt — PHashSize enum + phashOf(LongArray) + HashDistance.hamming(LongArray) + 테스트
T6. MssimSimilarity.kt — gaussianKernel1d + mssimTo + 경계 검증 + 테스트
T6-C. MSSIM threshold calibration — 실제 homer.jpg 90% JPEG 재저장 시 측정값 기록 후 임계값 확정
T7. HistogramSimilarity.kt — ColorSpace + hsvComponents + sealed interface + ChiSquare + 테스트
T8. HistogramSimilarity.kt — Bhattacharyya + 테스트
T9. HistogramSimilarity.kt — EarthMover (1D CDF) + 테스트
T10. HistogramSimilarity.kt — companion 팩토리 + histogramSimilarityTo 편의 함수
T11. KeypointSimilarity.kt — blockMeanDescriptor + blockMeanSimilarityTo + 테스트
T12. KeypointSimilarity.kt — bestRotationSimilarityTo + 90° 회전 테스트
T13. README.md / README.ko.md 동기화 + KDoc 표 업데이트
T14. /oh-my-claudecode:code-reviewer 실행 + HIGH/CRITICAL 해소
T15. ./gradlew :bluetape4k-images:test 전수 통과 확인 + PR 본문 작성
T16. docs/superpowers/index/2026-04.md 업데이트 + /wiki-update
```

각 task는 **선택 없이 전수 완료** 후 PR 생성. 완료 후 Plan 대비 비교 표 보고.
