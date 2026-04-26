package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.transforms.internal.blueComponent
import io.bluetape4k.images.transforms.internal.getArgbPixels
import io.bluetape4k.images.transforms.internal.greenComponent
import io.bluetape4k.images.transforms.internal.redComponent
import io.bluetape4k.images.transforms.internal.toIntArgb
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private val log = KotlinLogging.logger {}

/**
 * 다운샘플링 시 가장 긴 변의 목표 픽셀 수.
 *
 * 256px 보다 큰 이미지를 처리할 때 이 값으로 축소하여 saliency 계산을 수행합니다.
 * 256px 이하 이미지는 원본 해상도에서 직접 계산합니다.
 */
private const val DOWNSAMPLE_TARGET_LONGEST_SIDE = 256

/**
 * 자르기에 사용되는 종횡비를 표현하는 값 객체.
 *
 * `width:height` 비율을 정수 쌍으로 보관하며, 두 값은 모두 양의 정수여야 합니다.
 *
 * ```kotlin
 * val ratio = AspectRatio(16, 9)
 * val square = AspectRatio.SQUARE
 * ```
 *
 * @property width 가로 비율 (양의 정수).
 * @property height 세로 비율 (양의 정수).
 */
data class AspectRatio(val width: Int, val height: Int) {
    init {
        require(width > 0 && height > 0) { "AspectRatio dimensions must be positive: width=$width, height=$height" }
    }

    companion object {
        /** 1:1 정사각형 비율. */
        val SQUARE = AspectRatio(1, 1)

        /** 16:9 와이드스크린 비율. */
        val WIDESCREEN = AspectRatio(16, 9)

        /** 9:16 세로형 (포트레이트) 비율. */
        val PORTRAIT = AspectRatio(9, 16)

        /** 4:3 표준 비율. */
        val STANDARD = AspectRatio(4, 3)
    }
}

/**
 * Smart crop 에서 사용할 saliency (관심 영역) 추정 전략.
 *
 * 이 전략은 **휴리스틱** 이며 머신러닝 기반 얼굴/객체 검출이 아닙니다.
 * 단순한 엣지 에너지(edge energy) 기반으로 "정보가 많아 보이는" 영역을 추정할 뿐입니다.
 */
sealed interface SaliencyStrategy {

    /**
     * Sobel 연산자를 사용해 엣지 강도(L1 norm) 를 합산하는 휴리스틱.
     *
     * - 빠르고 의존성 없음 (순수 JVM 연산).
     * - 얼굴/객체 인식이 아니라 텍스처/엣지가 풍부한 영역을 선호.
     * - 단색 배경 위에 텍스트가 있는 이미지처럼 엣지가 또렷한 콘텐츠에 효과적.
     */
    data object SobelEnergy : SaliencyStrategy
}

/**
 * 이미지에서 [aspectRatio] 비율을 유지하는 가장 "흥미로워 보이는" 영역을 잘라냅니다.
 *
 * > **주의**: 이 함수는 **휴리스틱 saliency** 입니다 — 얼굴/객체 검출이 아닌
 * > **Sobel 엣지 에너지** 기준입니다. 인물 사진 등에서 얼굴이 항상 중앙에 오도록
 * > 보장하지 않으며, 어디까지나 엣지가 풍부한 영역을 우선시할 뿐입니다.
 *
 * ## 동작/계약
 * - 가장 긴 변이 256px 이상인 이미지는 saliency 계산 전 256px 로 다운샘플링하여 성능을 확보합니다.
 * - 다운샘플 좌표계에서 [aspectRatio] 를 유지하는 최대 윈도우를 슬라이딩하며,
 *   integral image 를 사용해 O(1) 윈도우 합 평가로 최적 위치를 찾습니다.
 * - 최적 윈도우를 원본 좌표로 복원하여 [ImmutableImage.subimage] 호출로 잘라냅니다.
 *
 * ```kotlin
 * val cropped = image.smartCrop(AspectRatio.SQUARE)
 * val widescreen = image.smartCrop(AspectRatio.WIDESCREEN)
 * ```
 *
 * @param aspectRatio 결과 영역의 종횡비 (`width:height`).
 * @param strategy saliency 추정 전략. 기본은 [SaliencyStrategy.SobelEnergy].
 * @return saliency 가 가장 높은 영역을 [aspectRatio] 비율로 잘라낸 새 [ImmutableImage].
 */
fun ImmutableImage.smartCrop(
    aspectRatio: AspectRatio,
    strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
): ImmutableImage {
    require(aspectRatio.width > 0 && aspectRatio.height > 0) {
        "aspectRatio dimensions must be positive: width=${aspectRatio.width}, height=${aspectRatio.height}"
    }

    val origW = width
    val origH = height
    val longest = if (origW >= origH) origW else origH

    // 1) 다운샘플링: 가장 긴 변이 256 초과인 경우만
    val dsScale: Double
    val dsImage: ImmutableImage
    if (longest > DOWNSAMPLE_TARGET_LONGEST_SIDE) {
        dsScale = DOWNSAMPLE_TARGET_LONGEST_SIDE.toDouble() / longest
        val newW = (origW * dsScale).roundToInt().coerceAtLeast(1)
        val newH = (origH * dsScale).roundToInt().coerceAtLeast(1)
        dsImage = scaleTo(newW, newH)
    } else {
        dsScale = 1.0
        dsImage = this
    }

    val dsW = dsImage.width
    val dsH = dsImage.height

    // 2) ARGB 픽셀 추출 후 luma(grayscale) 변환
    val argb = dsImage.toIntArgb().getArgbPixels()
    val luma = IntArray(dsW * dsH)
    for (i in argb.indices) {
        val p = argb[i]
        val r = p.redComponent()
        val g = p.greenComponent()
        val b = p.blueComponent()
        luma[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    // 3) Sobel 엣지 magnitude (L1 norm). border 픽셀은 0.
    val mag = when (strategy) {
        SaliencyStrategy.SobelEnergy -> computeSobelMagnitude(luma, dsW, dsH)
    }

    // 4) Integral image (1-indexed, (dsW+1) x (dsH+1))
    val iw = dsW + 1
    val ih = dsH + 1
    val integ = LongArray(iw * ih)
    for (y in 0 until dsH) {
        var rowSum = 0L
        for (x in 0 until dsW) {
            rowSum += mag[y * dsW + x]
            integ[(y + 1) * iw + (x + 1)] = integ[y * iw + (x + 1)] + rowSum
        }
    }

    // 5) 종횡비 유지 최대 윈도우 크기 결정 (다운샘플 좌표계)
    val ratio = aspectRatio.width.toDouble() / aspectRatio.height
    val winW: Int
    val winH: Int
    if (dsW / ratio <= dsH) {
        winW = dsW
        winH = (dsW / ratio).toInt().coerceAtLeast(1)
    } else {
        winH = dsH
        winW = (dsH * ratio).toInt().coerceAtLeast(1)
    }

    // 6) 슬라이딩 윈도우로 최대 saliency 위치 탐색
    var bestSum = Long.MIN_VALUE
    var bestX = 0
    var bestY = 0
    val maxX = dsW - winW
    val maxY = dsH - winH
    for (y in 0..maxY) {
        for (x in 0..maxX) {
            val sum = integ[(y + winH) * iw + (x + winW)] -
                integ[y * iw + (x + winW)] -
                integ[(y + winH) * iw + x] +
                integ[y * iw + x]
            if (sum > bestSum) {
                bestSum = sum
                bestX = x
                bestY = y
            }
        }
    }

    // 7) 원본 좌표로 복원
    val restoredX: Int
    val restoredY: Int
    val restoredW: Int
    val restoredH: Int
    if (dsScale == 1.0) {
        restoredX = bestX.coerceIn(0, origW - 1)
        restoredY = bestY.coerceIn(0, origH - 1)
        restoredW = winW.coerceIn(1, origW - restoredX)
        restoredH = winH.coerceIn(1, origH - restoredY)
    } else {
        restoredX = (bestX / dsScale).toInt().coerceIn(0, origW - 1)
        restoredY = (bestY / dsScale).toInt().coerceIn(0, origH - 1)
        restoredW = (winW / dsScale).toInt().coerceIn(1, origW - restoredX)
        restoredH = (winH / dsScale).toInt().coerceIn(1, origH - restoredY)
    }

    log.debug {
        "smartCrop: orig=${origW}x${origH}, ds=${dsW}x${dsH} (scale=$dsScale), " +
            "win=${winW}x${winH} at ds($bestX,$bestY) -> orig(${restoredX},${restoredY}) ${restoredW}x${restoredH}"
    }

    return subimage(restoredX, restoredY, restoredW, restoredH)
}

/**
 * 이미지를 [width] x [height] 크기로 smart crop + resize 합니다.
 *
 * > **주의**: 내부적으로 [smartCrop] 의 휴리스틱 saliency 를 사용합니다 —
 * > 얼굴/객체 검출이 아닌 Sobel 엣지 에너지 기준입니다.
 *
 * ## 동작/계약
 * - 1) [width] / [height] 비율로 [smartCrop] 호출하여 핵심 영역을 자르고,
 * - 2) `scaleTo(width, height)` 로 정확한 출력 크기로 리사이즈합니다.
 *
 * ```kotlin
 * val thumb = image.smartCropTo(400, 300)
 * ```
 *
 * @param width 출력 이미지 너비 (양의 정수).
 * @param height 출력 이미지 높이 (양의 정수).
 * @param strategy saliency 추정 전략. 기본은 [SaliencyStrategy.SobelEnergy].
 * @return smart-crop 후 정확히 [width] x [height] 로 리사이즈된 [ImmutableImage].
 */
fun ImmutableImage.smartCropTo(
    width: Int,
    height: Int,
    strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
): ImmutableImage {
    require(width > 0 && height > 0) { "output dimensions must be positive: width=$width, height=$height" }
    return smartCrop(AspectRatio(width, height), strategy).scaleTo(width, height)
}

/**
 * 코루틴 환경에서 [smartCrop] 을 실행합니다.
 *
 * > **주의**: [smartCrop] 과 동일하게 휴리스틱 saliency 입니다 — 얼굴/객체 검출이 아닌
 * > Sobel 엣지 에너지 기준입니다.
 *
 * ## 동작/계약
 * - `Dispatchers.Default` 컨텍스트에서 [smartCrop] 을 호출합니다.
 *
 * ```kotlin
 * val cropped = image.suspendSmartCrop(AspectRatio.SQUARE)
 * ```
 *
 * @param aspectRatio 결과 영역의 종횡비.
 * @param strategy saliency 추정 전략. 기본은 [SaliencyStrategy.SobelEnergy].
 * @return saliency 가 가장 높은 영역을 [aspectRatio] 비율로 잘라낸 새 [ImmutableImage].
 */
suspend fun ImmutableImage.suspendSmartCrop(
    aspectRatio: AspectRatio,
    strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
): ImmutableImage = withContext(Dispatchers.Default) { smartCrop(aspectRatio, strategy) }

/**
 * Sobel 엣지 magnitude (L1 norm) 맵을 계산합니다.
 *
 * - Gx kernel: `[[-1,0,1],[-2,0,2],[-1,0,1]]`
 * - Gy kernel: `[[-1,-2,-1],[0,0,0],[1,2,1]]`
 * - 결과: `|Gx| + |Gy|` (border 픽셀은 0)
 */
private fun computeSobelMagnitude(luma: IntArray, w: Int, h: Int): IntArray {
    val mag = IntArray(w * h)
    if (w < 3 || h < 3) {
        return mag
    }
    for (y in 1 until h - 1) {
        val ym1 = (y - 1) * w
        val y0 = y * w
        val yp1 = (y + 1) * w
        for (x in 1 until w - 1) {
            val tl = luma[ym1 + (x - 1)]
            val t = luma[ym1 + x]
            val tr = luma[ym1 + (x + 1)]
            val l = luma[y0 + (x - 1)]
            val r = luma[y0 + (x + 1)]
            val bl = luma[yp1 + (x - 1)]
            val bot = luma[yp1 + x]
            val br = luma[yp1 + (x + 1)]

            val gx = -tl + tr - 2 * l + 2 * r - bl + br
            val gy = -tl - 2 * t - tr + bl + 2 * bot + br
            mag[y0 + x] = abs(gx) + abs(gy)
        }
    }
    return mag
}

