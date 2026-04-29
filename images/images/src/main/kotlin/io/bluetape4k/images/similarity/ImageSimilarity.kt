package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 이미지 유사도 계산 유틸리티.
 *
 * ## 제공 지표
 *
 * | 함수 | 의미 | 값 범위 | 완전 동일 |
 * |---|---|---|---|
 * | [pixelAvgDelta] | 채널별 RGB 차이 평균 | 0.0 ~ 255.0 | 0.0 |
 * | [pixelMaxDelta] | 채널별 RGB 차이 최댓값 | 0 ~ 255 | 0 |
 * | [mse]           | Mean Squared Error | 0.0 ~ 65025.0 | 0.0 |
 * | [psnr]          | Peak Signal-to-Noise Ratio (dB) | 0 ~ ∞ | ∞ |
 * | [ssim]          | Structural Similarity Index | -1.0 ~ 1.0 | 1.0 |
 * | [phash]         | 64bit Perceptual Hash | Long | 동일 |
 * | [phashDistance] | pHash Hamming distance | 0 ~ 64 | 0 |
 *
 * ## 선택 가이드
 *
 * - 바이트 단위 회귀 테스트: [pixelAvgDelta] / [pixelMaxDelta]
 * - JPEG 압축 품질 평가: [psnr] (≥ 30dB 양호, ≥ 40dB 거의 동일)
 * - 인지적 유사도: [ssim] (≥ 0.95 시각적으로 거의 구분 불가)
 * - 중복/리사이즈 탐지: [phashDistance] (≤ 10 유사, ≤ 5 거의 동일)
 *
 * ```kotlin
 * val a = immutableImageOf(File("a.jpg"))
 * val b = immutableImageOf(File("b.jpg"))
 *
 * a.pixelAvgDeltaTo(b)    // 2.1
 * a.psnrTo(b)             // 42.3 dB
 * a.ssimTo(b)             // 0.987
 * a.phashDistanceTo(b)    // 0
 * ```
 */

/**
 * 두 이미지의 RGB 채널별 절대 차이의 평균을 계산합니다.
 *
 * 값이 작을수록 유사합니다. 완전 동일하면 0.0.
 *
 * @receiver 비교 기준 이미지
 * @param other 비교 대상 이미지
 * @return 채널당 평균 RGB 차이 (0.0 ~ 255.0)
 * @throws IllegalArgumentException 두 이미지의 크기가 다를 때
 */
fun ImmutableImage.pixelAvgDeltaTo(other: ImmutableImage): Double {
    requireSameSize(other)
    val a = pixels()
    val b = other.pixels()
    var total = 0L
    for (i in a.indices) {
        total += abs(a[i].red() - b[i].red()) +
            abs(a[i].green() - b[i].green()) +
            abs(a[i].blue() - b[i].blue())
    }
    return total.toDouble() / (a.size * 3)
}

/**
 * 두 이미지의 RGB 채널별 절대 차이의 최댓값을 계산합니다.
 *
 * 단일 픽셀이라도 큰 변화가 있으면 반영되는 엄격한 지표.
 *
 * @receiver 비교 기준 이미지
 * @param other 비교 대상 이미지
 * @return 채널당 최대 RGB 차이 (0 ~ 255)
 * @throws IllegalArgumentException 두 이미지의 크기가 다를 때
 */
fun ImmutableImage.pixelMaxDeltaTo(other: ImmutableImage): Int {
    requireSameSize(other)
    val a = pixels()
    val b = other.pixels()
    var max = 0
    for (i in a.indices) {
        val dr = abs(a[i].red() - b[i].red())
        val dg = abs(a[i].green() - b[i].green())
        val db = abs(a[i].blue() - b[i].blue())
        val localMax = maxOf(dr, dg, db)
        if (localMax > max) max = localMax
    }
    return max
}

/**
 * 두 이미지의 Mean Squared Error(MSE)를 계산합니다.
 *
 * RGB 3채널 평균. 완전 동일하면 0.0.
 *
 * @receiver 비교 기준 이미지
 * @param other 비교 대상 이미지
 * @return MSE (0.0 ~ 65025.0)
 * @throws IllegalArgumentException 두 이미지의 크기가 다를 때
 */
fun ImmutableImage.mseTo(other: ImmutableImage): Double {
    requireSameSize(other)
    val a = pixels()
    val b = other.pixels()
    var sum = 0.0
    for (i in a.indices) {
        val dr = (a[i].red() - b[i].red()).toDouble()
        val dg = (a[i].green() - b[i].green()).toDouble()
        val db = (a[i].blue() - b[i].blue()).toDouble()
        sum += dr * dr + dg * dg + db * db
    }
    return sum / (a.size * 3)
}

/**
 * 두 이미지의 Peak Signal-to-Noise Ratio(PSNR)를 계산합니다.
 *
 * dB 단위로 클수록 유사합니다.
 * - ≥ 30 dB: 일반적으로 양호
 * - ≥ 40 dB: 거의 동일
 * - 완전 동일하면 [Double.POSITIVE_INFINITY]
 *
 * @receiver 비교 기준 이미지
 * @param other 비교 대상 이미지
 * @return PSNR in dB (0 ~ ∞)
 * @throws IllegalArgumentException 두 이미지의 크기가 다를 때
 */
fun ImmutableImage.psnrTo(other: ImmutableImage): Double {
    val m = mseTo(other)
    return if (m == 0.0) Double.POSITIVE_INFINITY else 20.0 * log10(PIXEL_MAX / sqrt(m))
}

/**
 * 두 이미지의 Structural Similarity Index(SSIM)를 휘도(luminance) 기준으로 계산합니다.
 *
 * 전역 SSIM(global SSIM) 방식 — 이미지 전체를 하나의 윈도우로 간주합니다.
 * 정교한 11x11 sliding window 기반 MSSIM이 필요하면 별도 구현을 고려하세요.
 *
 * - 1.0: 완전 동일
 * - 0.95 이상: 시각적으로 거의 구분 불가
 * - 0.8 이하: 명확한 차이
 *
 * @receiver 비교 기준 이미지
 * @param other 비교 대상 이미지
 * @return SSIM (-1.0 ~ 1.0)
 * @throws IllegalArgumentException 두 이미지의 크기가 다를 때
 */
fun ImmutableImage.ssimTo(other: ImmutableImage): Double {
    requireSameSize(other)
    val a = pixels()
    val b = other.pixels()
    val n = a.size
    val la = DoubleArray(n) { luminance(a[it]) }
    val lb = DoubleArray(n) { luminance(b[it]) }

    var sumX = 0.0
    var sumY = 0.0
    for (i in 0 until n) {
        sumX += la[i]
        sumY += lb[i]
    }
    val mx = sumX / n
    val my = sumY / n

    var sx2 = 0.0
    var sy2 = 0.0
    var sxy = 0.0
    for (i in 0 until n) {
        val dx = la[i] - mx
        val dy = lb[i] - my
        sx2 += dx * dx
        sy2 += dy * dy
        sxy += dx * dy
    }
    sx2 /= n
    sy2 /= n
    sxy /= n

    val num = (2.0 * mx * my + SSIM_C1) * (2.0 * sxy + SSIM_C2)
    val den = (mx * mx + my * my + SSIM_C1) * (sx2 + sy2 + SSIM_C2)
    return num / den
}

/**
 * 이미지의 64bit Perceptual Hash(pHash)를 계산합니다.
 *
 * 알고리즘: 32×32 리사이즈 → 휘도 변환 → 2D DCT-II → 저주파 8×8 블록 추출 →
 * DC 성분 제외 평균 기준 비트 판정.
 *
 * 두 이미지의 pHash가 유사하면 시각적으로도 유사합니다.
 * 리사이즈·약한 JPEG 압축·밝기 변화에 견고합니다.
 *
 * @receiver 이미지
 * @return 64bit perceptual hash
 */
fun ImmutableImage.phash(): Long {
    val scaled = scaleTo(PHASH_SIZE, PHASH_SIZE, HASH_SCALE_METHOD)
    val gray = Array(PHASH_SIZE) { y -> DoubleArray(PHASH_SIZE) { x -> luminance(scaled.pixel(x, y)) } }
    val dct = dct2d(gray, PHASH_SIZE)

    val low = DoubleArray(PHASH_BITS)
    for (y in 0 until PHASH_LOW_SIZE) {
        for (x in 0 until PHASH_LOW_SIZE) {
            low[y * PHASH_LOW_SIZE + x] = dct[y][x]
        }
    }
    var avg = 0.0
    for (i in 1 until PHASH_BITS) avg += low[i]
    avg /= (PHASH_BITS - 1)

    var hash = 0L
    for (i in 0 until PHASH_BITS) {
        if (low[i] > avg) hash = hash or (1L shl i)
    }
    return hash
}

/**
 * 두 이미지의 pHash [hammingDistance]를 계산합니다.
 *
 * 값 가이드:
 * - 0 ~ 5: 거의 동일
 * - 6 ~ 10: 시각적으로 유사
 * - 11 ~ 20: 약간의 변형
 * - 20 이상: 다른 이미지
 *
 * @receiver 비교 기준 이미지
 * @param other 비교 대상 이미지
 * @return Hamming distance (0 ~ 64)
 */
fun ImmutableImage.phashDistanceTo(other: ImmutableImage): Int =
    hammingDistance(phash(), other.phash())

/**
 * 두 64bit 해시 사이의 Hamming distance를 계산합니다.
 *
 * @param a 첫 번째 해시
 * @param b 두 번째 해시
 * @return 서로 다른 비트 수 (0 ~ 64)
 */
@Deprecated(
    message = "HashDistance.hamming(a, b) 사용",
    replaceWith = ReplaceWith("HashDistance.hamming(a, b)", "io.bluetape4k.images.similarity.HashDistance")
)
fun hammingDistance(a: Long, b: Long): Int = (a xor b).countOneBits()
