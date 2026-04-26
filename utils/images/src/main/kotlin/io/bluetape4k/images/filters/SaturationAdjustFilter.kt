package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter
import io.bluetape4k.images.filters.dsl.ColorSpaceConverter
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * HSV 색공간에서 채도(Saturation)를 조정하는 [Filter].
 * 각 픽셀을 RGB→HSV로 변환 후 S 채널에 [factor]를 곱하고 다시 HSV→RGB로 변환합니다.
 *
 * **주의**: 이 필터는 `image.awt()`의 래스터를 직접 변경합니다.
 * 원본 보존이 필요하면 `applyFilters { saturation(factor) }` DSL 또는
 * `image.copy().filter(saturationFilterOf(factor))` 를 사용하세요.
 *
 * @param factor 채도 배수. 1.0=원본, >1 증가, <1 감소, 0=흑백. 0 이상이어야 합니다.
 */
class SaturationAdjustFilter(private val factor: Float) : Filter {

    init {
        require(factor >= 0f) { "factor must be >= 0, but was $factor" }
    }

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
                val r = pixel[0]
                val g = pixel[1]
                val b = pixel[2]

                ColorSpaceConverter.rgbToHsvInto(r, g, b, hsvOut)
                hsvOut[1] = (hsvOut[1] * factor).coerceIn(0f, 1f)
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
 * HSV 공간에서 채도를 조정하는 [Filter]를 생성합니다.
 *
 * **주의**: 이 필터는 `image.awt()`를 직접 변경합니다.
 * 원본 보존이 필요하면 `applyFilters { saturation(factor) }` DSL 또는
 * `image.copy().filter(saturationFilterOf(factor))` 를 사용하세요.
 *
 * ```kotlin
 * val saturated = image.filter(saturationFilterOf(factor = 1.2f))
 * ```
 *
 * @param factor 채도 배수. 1.0=원본, >1 증가, <1 감소, 0=흑백. 0 이상이어야 합니다.
 */
fun saturationFilterOf(factor: Float): Filter = SaturationAdjustFilter(factor)
