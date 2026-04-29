package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * [MedianBlurFilter]가 윈도우 경계 밖의 픽셀을 처리하는 방식.
 */
enum class MedianBoundaryMode {
    /** 경계 밖은 가장 가까운 경계 픽셀 값을 복제합니다. */
    REPLICATE,

    /** 경계 밖은 경계를 축으로 반사된 픽셀 값을 사용합니다. */
    REFLECT,
}

/**
 * 픽셀 주변 (2r+1)² 윈도우에서 R/G/B 채널별 중앙값으로 노이즈를 제거하는 [Filter].
 *
 * jhlabs MedianFilter는 scrimage internal 이므로 직접 구현합니다.
 * Median 특성상 in-place 불가 — 소스 전체를 읽은 뒤 결과 배열에 쓰고 마지막에 일괄 저장합니다.
 *
 * **주의**: 이 필터는 `image.awt()`의 래스터를 직접 변경합니다.
 *
 * @param radius 윈도우 반경. 0 이상이어야 합니다. 0이면 identity. 윈도우 크기 = (2r+1)².
 * @param boundary 경계 픽셀 처리 방식. 기본값 [MedianBoundaryMode.REPLICATE].
 */
class MedianBlurFilter(
    private val radius: Int,
    private val boundary: MedianBoundaryMode = MedianBoundaryMode.REPLICATE,
): Filter {

    init {
        require(radius >= 0) { "radius must be >= 0, but was $radius" }
    }

    companion object: KLoggingChannel()

    override fun apply(image: ImmutableImage) {
        if (radius == 0) return

        val awt = image.awt()
        val width = awt.width
        val height = awt.height
        val numBands = awt.raster.numBands.coerceAtLeast(3)

        // 소스 래스터 전체를 먼저 읽기 (median은 in-place 불가)
        val srcPixels = IntArray(width * height * numBands)
        awt.raster.getPixels(0, 0, width, height, srcPixels)

        val windowSize = (2 * radius + 1) * (2 * radius + 1)
        val window = IntArray(windowSize)

        val dstPixels = IntArray(srcPixels.size)

        for (y in 0 until height) {
            for (x in 0 until width) {
                for (band in 0 until 3) {
                    var count = 0
                    for (dy in -radius..radius) {
                        for (dx in -radius..radius) {
                            val sx = clampOrReflect(x + dx, width)
                            val sy = clampOrReflect(y + dy, height)
                            window[count++] = srcPixels[(sy * width + sx) * numBands + band]
                        }
                    }
                    window.sort(0, count)
                    dstPixels[(y * width + x) * numBands + band] = window[count / 2]
                }
                // alpha 채널이 있으면 그대로 복사
                if (numBands > 3) {
                    dstPixels[(y * width + x) * numBands + 3] = srcPixels[(y * width + x) * numBands + 3]
                }
            }
        }

        awt.raster.setPixels(0, 0, width, height, dstPixels)
    }

    private fun clampOrReflect(pos: Int, size: Int): Int = when (boundary) {
        MedianBoundaryMode.REPLICATE -> pos.coerceIn(0, size - 1)
        MedianBoundaryMode.REFLECT -> {
            // 주기 2*size 기반 iterative reflection — radius >= size 인 경우에도 올바른 결과
            val period = 2 * size
            val p = ((pos % period) + period) % period
            if (p >= size) period - 1 - p else p
        }
    }
}

/**
 * 픽셀 주변 윈도우의 채널별 중앙값으로 노이즈를 제거하는 [Filter]를 생성합니다.
 *
 * ```kotlin
 * val denoised = image.filter(medianBlurFilterOf(radius = 2))
 * ```
 *
 * @param radius 윈도우 반경. 0 이상이어야 합니다. 윈도우 크기 = (2r+1)².
 * @param boundary 경계 픽셀 처리 방식. 기본값 [MedianBoundaryMode.REPLICATE].
 */
fun medianBlurFilterOf(
    radius: Int,
    boundary: MedianBoundaryMode = MedianBoundaryMode.REPLICATE,
): Filter = MedianBlurFilter(radius, boundary)
