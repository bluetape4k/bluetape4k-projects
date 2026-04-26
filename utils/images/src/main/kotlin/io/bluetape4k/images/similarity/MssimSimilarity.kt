package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage

/**
 * MSSIM(Mean SSIM) 기본 윈도우 한 변 크기.
 *
 * Wang et al. (2004) 논문 권장값(11). 홀수, 3 이상이어야 합니다.
 */
private const val MSSIM_DEFAULT_WINDOW = 11

/**
 * MSSIM 가우시안 가중치의 기본 표준편차(σ).
 *
 * Wang et al. (2004) 논문 권장값(1.5).
 */
private const val MSSIM_DEFAULT_SIGMA = 1.5

/**
 * 11×11 가우시안 가중 슬라이딩 윈도우 기반 MSSIM(Mean SSIM)을 계산합니다.
 *
 * Wang et al. (2004) "Image Quality Assessment: From Error Visibility to Structural Similarity"
 * 의 표준 구현으로, 글로벌 [ssimTo]보다 국소 구조(local structure)를 정확히 반영합니다.
 *
 * ## 알고리즘
 *
 * 휘도(luminance) 채널만 사용. 각 윈도우에서 가우시안 가중 평균/분산/공분산을 계산하고
 * 다음 식으로 SSIM을 산출한 뒤, 이미지 내 모든 유효 윈도우의 SSIM 평균을 반환합니다.
 *
 * ```
 * SSIM(x,y) = (2·μx·μy + C1)(2·σxy + C2) / ((μx² + μy² + C1)(σx² + σy² + C2))
 * ```
 *
 * - `C1 = (0.01·L)²`, `C2 = (0.03·L)²`, `L = 255` (8bit)
 * - 유효 윈도우는 중심이 `[half, w-half) × [half, h-half)` 범위에 있는 픽셀 (`half = windowSize / 2`)
 *
 * ## 성능 주의
 *
 * 픽셀당 `O(windowSize²)` 연산. 1MP 이상 이미지에서 수 초가 소요될 수 있습니다.
 * 대형 이미지는 [scaleToMaxSide] 등으로 먼저 다운스케일 후 호출하세요.
 *
 * ```kotlin
 * val a = immutableImageOf(File("a.jpg"))
 * val b = immutableImageOf(File("b.jpg"))
 *
 * a.mssimTo(b)               // 기본 11×11, σ=1.5
 * a.mssimTo(b, windowSize = 7, sigma = 1.0)
 * ```
 *
 * @receiver 비교 기준 이미지
 * @param other 비교 대상 이미지 (동일 크기 필수)
 * @param windowSize 윈도우 한 변 크기. 홀수, 3 이상. 기본 11 (논문 권장).
 * @param sigma 가우시안 표준편차. 양수. 기본 1.5 (논문 권장).
 * @return MSSIM (-1.0 ~ 1.0, 완전 동일하면 1.0)
 * @throws IllegalArgumentException 이미지 크기 불일치, [windowSize]가 짝수이거나 3 미만,
 *   이미지가 [windowSize]보다 작거나, [sigma]가 0 이하일 때
 */
fun ImmutableImage.mssimTo(
    other: ImmutableImage,
    windowSize: Int = MSSIM_DEFAULT_WINDOW,
    sigma: Double = MSSIM_DEFAULT_SIGMA,
): Double {
    requireSameSize(other)
    require(windowSize % 2 == 1 && windowSize >= 3) {
        "windowSize는 홀수 3 이상이어야 합니다: $windowSize"
    }
    require(minOf(width, height) >= windowSize) {
        "이미지 크기(${width}x${height})가 windowSize($windowSize)보다 작습니다"
    }
    require(sigma > 0.0) { "sigma는 양수여야 합니다: $sigma" }

    val kernel1d = gaussianKernel1d(windowSize, sigma)
    // 2D 가우시안 커널 (외적, separable kernel)
    val kernel2d = Array(windowSize) { i ->
        DoubleArray(windowSize) { j -> kernel1d[i] * kernel1d[j] }
    }

    val pixA = pixels()
    val pixB = other.pixels()
    val w = width
    val h = height

    val lumA = DoubleArray(w * h) { luminance(pixA[it]) }
    val lumB = DoubleArray(w * h) { luminance(pixB[it]) }

    val half = windowSize / 2
    var ssimSum = 0.0
    var count = 0

    for (cy in half until h - half) {
        for (cx in half until w - half) {
            // 가우시안 가중 평균 μx, μy
            var muA = 0.0
            var muB = 0.0
            for (wy in 0 until windowSize) {
                val baseY = (cy + wy - half) * w
                for (wx in 0 until windowSize) {
                    val weight = kernel2d[wy][wx]
                    val idx = baseY + (cx + wx - half)
                    muA += weight * lumA[idx]
                    muB += weight * lumB[idx]
                }
            }
            // 가우시안 가중 분산/공분산 σx², σy², σxy
            var sigA2 = 0.0
            var sigB2 = 0.0
            var sigAB = 0.0
            for (wy in 0 until windowSize) {
                val baseY = (cy + wy - half) * w
                for (wx in 0 until windowSize) {
                    val weight = kernel2d[wy][wx]
                    val idx = baseY + (cx + wx - half)
                    val da = lumA[idx] - muA
                    val db = lumB[idx] - muB
                    sigA2 += weight * da * da
                    sigB2 += weight * db * db
                    sigAB += weight * da * db
                }
            }
            val num = (2.0 * muA * muB + SSIM_C1) * (2.0 * sigAB + SSIM_C2)
            val den = (muA * muA + muB * muB + SSIM_C1) * (sigA2 + sigB2 + SSIM_C2)
            ssimSum += num / den
            count++
        }
    }
    return ssimSum / count
}
