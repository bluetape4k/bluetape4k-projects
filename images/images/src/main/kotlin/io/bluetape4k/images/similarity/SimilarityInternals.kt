package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.ScaleMethod
import com.sksamuel.scrimage.pixels.Pixel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow

// 기존 ImageSimilarity.kt에서 이동된 internal 헬퍼들

internal const val PIXEL_MAX = 255.0

// SSIM 상수
internal val SSIM_C1 = (0.01 * PIXEL_MAX).pow(2)
internal val SSIM_C2 = (0.03 * PIXEL_MAX).pow(2)

// pHash 상수 (기존 phash() 하위 호환)
internal const val PHASH_SIZE = 32
internal const val PHASH_LOW_SIZE = 8
internal const val PHASH_BITS = PHASH_LOW_SIZE * PHASH_LOW_SIZE // 64

// 해시 계열 공통 스케일 메서드 — 기존 phash()와 동일하게 Bicubic 고정
internal val HASH_SCALE_METHOD = ScaleMethod.Bicubic

internal fun ImmutableImage.requireSameSize(other: ImmutableImage) {
    require(width == other.width && height == other.height) {
        "이미지 크기가 동일해야 합니다: (${width}x${height}) vs (${other.width}x${other.height})"
    }
}

/** ITU-R BT.601 휘도 변환. 기존과 동일. */
internal fun luminance(p: Pixel): Double =
    0.299 * p.red() + 0.587 * p.green() + 0.114 * p.blue()

/**
 * 2D DCT-II를 O(N^3)로 직접 계산합니다. N ≤ 32 전용.
 */
internal fun dct2d(input: Array<DoubleArray>, n: Int): Array<DoubleArray> {
    val cosTable = Array(n) { k -> DoubleArray(n) { i -> cos(PI * (i + 0.5) * k / n) } }
    val temp = Array(n) { DoubleArray(n) }
    val out = Array(n) { DoubleArray(n) }
    for (i in 0 until n) {
        for (k in 0 until n) {
            var s = 0.0
            for (j in 0 until n) s += input[i][j] * cosTable[k][j]
            temp[i][k] = s
        }
    }
    for (j in 0 until n) {
        for (k in 0 until n) {
            var s = 0.0
            for (i in 0 until n) s += temp[i][j] * cosTable[k][i]
            out[k][j] = s
        }
    }
    return out
}

/**
 * 1D 가우시안 가중치 커널 생성.
 * @param windowSize 홀수, 3 이상
 * @param sigma 가우시안 표준편차
 */
internal fun gaussianKernel1d(windowSize: Int, sigma: Double): DoubleArray {
    val half = windowSize / 2
    val kernel = DoubleArray(windowSize) { i ->
        val x = (i - half).toDouble()
        exp(-x * x / (2.0 * sigma * sigma))
    }
    val sum = kernel.sum()
    return DoubleArray(windowSize) { kernel[it] / sum }
}

/**
 * 정사각 2D Haar wavelet 변환 (in-place, [levels] 레벨).
 * 입력 크기는 2^n 이어야 합니다.
 */
internal fun haarTransform2d(matrix: Array<DoubleArray>, levels: Int = 1) {
    val n = matrix.size
    var size = n
    repeat(levels) {
        // 행 방향
        for (row in 0 until size) {
            val r = matrix[row]
            val tmp = DoubleArray(size)
            val h = size / 2
            for (i in 0 until h) {
                tmp[i] = (r[2 * i] + r[2 * i + 1]) / 2.0
                tmp[h + i] = (r[2 * i] - r[2 * i + 1]) / 2.0
            }
            for (i in 0 until size) r[i] = tmp[i]
        }
        // 열 방향
        for (col in 0 until size) {
            val tmp = DoubleArray(size)
            val h = size / 2
            for (i in 0 until h) {
                tmp[i] = (matrix[2 * i][col] + matrix[2 * i + 1][col]) / 2.0
                tmp[h + i] = (matrix[2 * i][col] - matrix[2 * i + 1][col]) / 2.0
            }
            for (i in 0 until size) matrix[i][col] = tmp[i]
        }
        size /= 2
    }
}

/**
 * AWT Color.RGBtoHSB 래핑 — (H, S, V) ∈ [0,1].
 */
internal fun hsvComponents(p: Pixel): Triple<Float, Float, Float> {
    val hsb = java.awt.Color.RGBtoHSB(p.red(), p.green(), p.blue(), null)
    return Triple(hsb[0], hsb[1], hsb[2])
}

/**
 * 긴 변이 [maxSide] 초과 시 비율 유지 다운스케일. 이미 작으면 원본 반환(복사 없음).
 */
internal fun ImmutableImage.scaleToMaxSide(maxSide: Int): ImmutableImage {
    val longSide = maxOf(width, height)
    if (longSide <= maxSide) return this
    val scale = maxSide.toDouble() / longSide
    return scaleTo((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), HASH_SCALE_METHOD)
}
