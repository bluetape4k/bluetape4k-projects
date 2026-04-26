package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.filter.BrightnessFilter
import com.sksamuel.scrimage.filter.ContrastFilter
import com.sksamuel.scrimage.filter.GainBiasFilter
import com.sksamuel.scrimage.filter.GammaFilter
import com.sksamuel.scrimage.filter.HSBFilter
import com.sksamuel.scrimage.filter.OpacityFilter
import com.sksamuel.scrimage.filter.PosterizeFilter
import com.sksamuel.scrimage.filter.ThresholdFilter
import io.bluetape4k.images.filters.ColorTemperatureFilter
import io.bluetape4k.images.filters.HueAdjustFilter
import io.bluetape4k.images.filters.SaturationAdjustFilter

/**
 * 이미지 밝기를 조정합니다.
 *
 * @param amount 밝기 배수. scrimage [BrightnessFilter] 사용.
 */
fun ImageFilterChain.brightness(amount: Float = 1.2f) {
    addNative(BrightnessFilter(amount))
}

/**
 * 이미지 대비를 조정합니다.
 *
 * @param amount 대비 배수. scrimage [ContrastFilter] 사용.
 */
fun ImageFilterChain.contrast(amount: Double = 1.2) {
    addNative(ContrastFilter(amount))
}

/**
 * 이미지 감마를 조정합니다.
 *
 * @param gamma 감마 값. scrimage [GammaFilter] 사용.
 */
fun ImageFilterChain.gamma(gamma: Double = 1.0) {
    addNative(GammaFilter(gamma))
}

/**
 * HSB(Hue/Saturation/Brightness) 값을 조정합니다.
 *
 * @param hue 색조 조정값 (0.0 기준 편차)
 * @param saturation 채도 조정값
 * @param brightness 밝기 조정값
 */
fun ImageFilterChain.hsb(hue: Float = 0f, saturation: Float = 0f, brightness: Float = 0f) {
    addNative(HSBFilter(hue, saturation, brightness))
}

/**
 * HSV 공간에서 채도를 조정합니다.
 *
 * @param factor 채도 배수. 1.0=원본, >1 증가, <1 감소, 0=흑백. 0 이상이어야 합니다.
 */
fun ImageFilterChain.saturation(factor: Float) {
    require(factor >= 0f) { "saturation factor must be >= 0, but was $factor" }
    addNative(SaturationAdjustFilter(factor))
}

/**
 * HSV 공간에서 색조를 회전합니다.
 *
 * @param deltaDegrees 색조 이동량 (도). 임의값 허용, 360도 정규화됩니다.
 */
fun ImageFilterChain.hue(deltaDegrees: Float) {
    addNative(HueAdjustFilter(deltaDegrees))
}

/**
 * 각 RGB 채널에 개별 배율을 적용합니다.
 *
 * @param r Red 채널 배율 (기본값 1.0). 0 이상이어야 합니다.
 * @param g Green 채널 배율 (기본값 1.0). 0 이상이어야 합니다.
 * @param b Blue 채널 배율 (기본값 1.0). 0 이상이어야 합니다.
 */
fun ImageFilterChain.rgb(r: Float = 1f, g: Float = 1f, b: Float = 1f) {
    require(r >= 0f && g >= 0f && b >= 0f) { "rgb factors must be >= 0, but were ($r, $g, $b)" }
    addPixel { image ->
        val target = image.copy()
        val raster = target.awt().raster
        val width = target.width
        val height = target.height
        val pixel = IntArray(raster.numBands.coerceAtLeast(3))
        for (y in 0 until height) {
            for (x in 0 until width) {
                raster.getPixel(x, y, pixel)
                pixel[0] = (pixel[0] * r).toInt().coerceIn(0, 255)
                pixel[1] = (pixel[1] * g).toInt().coerceIn(0, 255)
                pixel[2] = (pixel[2] * b).toInt().coerceIn(0, 255)
                raster.setPixel(x, y, pixel)
            }
        }
        target
    }
}

/**
 * 이미지 불투명도를 조정합니다.
 *
 * @param alpha 불투명도 (0.0~1.0). scrimage [OpacityFilter] 사용.
 */
fun ImageFilterChain.opacity(alpha: Float) {
    require(alpha in 0f..1f) { "opacity alpha must be in 0..1, but was $alpha" }
    addNative(OpacityFilter(alpha))
}

/**
 * 임계값 기준으로 픽셀을 흑/백으로 변환합니다.
 *
 * @param value 임계값 (0~255). scrimage [ThresholdFilter] 사용.
 */
fun ImageFilterChain.threshold(value: Int = 127) {
    addNative(ThresholdFilter(value))
}

/**
 * 색상을 포스터화합니다 (색상 단계를 줄임).
 *
 * @param levels 색상 단계 수 (2 이상). scrimage [PosterizeFilter] 사용.
 */
fun ImageFilterChain.posterize(levels: Int = 6) {
    require(levels >= 2) { "posterize levels must be >= 2, but was $levels" }
    addNative(PosterizeFilter(levels))
}

/**
 * Gain/Bias 조정으로 밝기와 대비를 제어합니다.
 *
 * @param gain 게인 값
 * @param bias 바이어스 값
 */
fun ImageFilterChain.gainBias(gain: Float, bias: Float) {
    addNative(GainBiasFilter(gain, bias))
}

/**
 * 색온도를 켈빈 단위로 조정합니다.
 *
 * @param kelvin 색온도 (켈빈). 1000~40000 범위. 5500K가 중성에 가깝습니다.
 */
fun ImageFilterChain.colorTemperature(kelvin: Int) {
    addNative(ColorTemperatureFilter(kelvin))
}
