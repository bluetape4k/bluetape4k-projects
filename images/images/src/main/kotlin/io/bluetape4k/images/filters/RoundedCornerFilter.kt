package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.awt.image.BufferedImage
import kotlin.math.sqrt

/**
 * 이미지의 네 모서리를 둥글게 깎는 [Filter].
 *
 * 코너 영역 픽셀에 대해 코너 중심으로부터의 거리를 계산하여 알파 채널을 조정합니다.
 * 이미지가 알파 채널을 갖도록 `TYPE_INT_ARGB` 타입으로 변환됩니다.
 *
 * @param radius 모서리 반경 (픽셀). 0 이상이어야 합니다.
 */
class RoundedCornerFilter(private val radius: Int) : Filter {

    init {
        require(radius >= 0) { "radius must be >= 0, but was $radius" }
    }

    companion object : KLoggingChannel()

    override fun apply(image: ImmutableImage) {
        if (radius == 0) return

        val awt = image.awt()
        val width = awt.width
        val height = awt.height

        for (y in 0 until radius) {
            for (x in 0 until radius) {
                val dx = (radius - 1 - x).toDouble()
                val dy = (radius - 1 - y).toDouble()
                val dist = sqrt(dx * dx + dy * dy)
                val alpha = when {
                    dist <= radius - 1 -> 255
                    dist <= radius -> ((radius - dist) * 255).toInt().coerceIn(0, 255)
                    else -> 0
                }
                setAlpha(awt, x, y, alpha)
                setAlpha(awt, width - 1 - x, y, alpha)
                setAlpha(awt, x, height - 1 - y, alpha)
                setAlpha(awt, width - 1 - x, height - 1 - y, alpha)
            }
        }
    }

    private fun setAlpha(image: BufferedImage, x: Int, y: Int, alpha: Int) {
        val argb = image.getRGB(x, y)
        val newArgb = (alpha shl 24) or (argb and 0x00FFFFFF)
        image.setRGB(x, y, newArgb)
    }
}

/**
 * 사각형 모서리를 둥글게 깎는 [Filter]를 생성합니다.
 *
 * ```kotlin
 * val rounded = image.filter(roundedCornerFilterOf(radius = 32))
 * ```
 *
 * @param radius 모서리 반경 (픽셀). 0 이상이어야 합니다.
 */
fun roundedCornerFilterOf(radius: Int): Filter = RoundedCornerFilter(radius)
