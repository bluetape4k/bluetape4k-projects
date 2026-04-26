package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter
import io.bluetape4k.images.filters.dsl.ColorSpaceConverter
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * 색온도(켈빈)를 기반으로 이미지의 색 균형을 조정하는 [Filter].
 *
 * Tanner Helland 알고리즘으로 [kelvin] 값에 대응하는 RGB 배율 (tr, tg, tb)을 계산하고,
 * 각 픽셀에 `R' = R*tr/255, G' = G*tg/255, B' = B*tb/255` 를 적용합니다.
 *
 * **주의**: 이 필터는 `image.awt()`의 래스터를 직접 변경합니다.
 *
 * @param kelvin 목표 색온도 (켈빈). 1000~40000 범위여야 합니다. 5500K가 중성에 가깝습니다.
 */
class ColorTemperatureFilter(private val kelvin: Int) : Filter {

    init {
        require(kelvin in 1000..40000) { "kelvin must be in 1000..40000, but was $kelvin" }
    }

    companion object : KLoggingChannel()

    override fun apply(image: ImmutableImage) {
        val (tr, tg, tb) = ColorSpaceConverter.kelvinToRgb(kelvin)
        val rf = tr / 255f
        val gf = tg / 255f
        val bf = tb / 255f
        val raster = image.awt().raster
        val width = image.width
        val height = image.height
        val pixel = IntArray(raster.numBands.coerceAtLeast(3))

        for (y in 0 until height) {
            for (x in 0 until width) {
                raster.getPixel(x, y, pixel)
                pixel[0] = (pixel[0] * rf).toInt().coerceIn(0, 255)
                pixel[1] = (pixel[1] * gf).toInt().coerceIn(0, 255)
                pixel[2] = (pixel[2] * bf).toInt().coerceIn(0, 255)
                raster.setPixel(x, y, pixel)
            }
        }
    }
}

/**
 * 색온도 변환 [Filter]를 생성합니다.
 *
 * ```kotlin
 * val warmed = image.filter(colorTemperatureFilterOf(kelvin = 3000))
 * ```
 *
 * @param kelvin 목표 색온도 (켈빈). 1000~40000 범위.
 */
fun colorTemperatureFilterOf(kelvin: Int): Filter = ColorTemperatureFilter(kelvin)
