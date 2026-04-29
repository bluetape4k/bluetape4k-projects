package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter
import io.bluetape4k.images.filters.dsl.ColorSpaceConverter
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * HSV 색공간에서 색조(Hue)를 회전시키는 [Filter].
 * 각 픽셀의 H 채널에 [deltaDegrees]를 더하고 360도 모듈러 연산을 적용합니다.
 *
 * **주의**: 이 필터는 `image.awt()`의 래스터를 직접 변경합니다.
 *
 * @param deltaDegrees 색조 이동량 (도). 임의값 허용, 360도 정규화됩니다.
 */
class HueAdjustFilter(private val deltaDegrees: Float) : Filter {

    companion object : KLoggingChannel()

    override fun apply(image: ImmutableImage) {
        val raster = image.awt().raster
        val width = image.width
        val height = image.height
        val hsvOut = FloatArray(3)
        val rgbOut = IntArray(3)
        val pixel = IntArray(raster.numBands.coerceAtLeast(3))

        for (y in 0 until height) {
            for (x in 0 until width) {
                raster.getPixel(x, y, pixel)
                ColorSpaceConverter.rgbToHsvInto(pixel[0], pixel[1], pixel[2], hsvOut)
                hsvOut[0] = ((hsvOut[0] + deltaDegrees) % 360f + 360f) % 360f
                ColorSpaceConverter.hsvToRgbInto(hsvOut[0], hsvOut[1], hsvOut[2], rgbOut)
                pixel[0] = rgbOut[0]
                pixel[1] = rgbOut[1]
                pixel[2] = rgbOut[2]
                raster.setPixel(x, y, pixel)
            }
        }
    }
}

/**
 * HSV 공간에서 색조를 회전시키는 [Filter]를 생성합니다.
 *
 * ```kotlin
 * val rotated = image.filter(hueFilterOf(deltaDegrees = 60f))
 * ```
 *
 * @param deltaDegrees 색조 이동량 (도). 임의값 허용.
 */
fun hueFilterOf(deltaDegrees: Float): Filter = HueAdjustFilter(deltaDegrees)
