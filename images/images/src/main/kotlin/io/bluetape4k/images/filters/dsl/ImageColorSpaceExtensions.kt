package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage

/**
 * 이미지의 각 픽셀을 HSV 색공간 값 배열로 변환합니다.
 * 반환: FloatArray of [h0, s0, v0, h1, s1, v1, ...] (row-major)
 */
fun ImmutableImage.toHsvArray(): FloatArray {
    val pixels = this.pixels()
    val result = FloatArray(pixels.size * 3)
    val out = FloatArray(3)
    for (i in pixels.indices) {
        val p = pixels[i]
        ColorSpaceConverter.rgbToHsvInto(p.red(), p.green(), p.blue(), out)
        result[i * 3] = out[0]
        result[i * 3 + 1] = out[1]
        result[i * 3 + 2] = out[2]
    }
    return result
}

/**
 * 이미지의 각 픽셀을 YCbCr 색공간 값 배열로 변환합니다.
 * 반환: FloatArray of [y0, cb0, cr0, y1, cb1, cr1, ...] (row-major)
 */
fun ImmutableImage.toYCbCrArray(): FloatArray {
    val pixels = this.pixels()
    val result = FloatArray(pixels.size * 3)
    val out = FloatArray(3)
    for (i in pixels.indices) {
        val p = pixels[i]
        ColorSpaceConverter.rgbToYCbCrInto(p.red(), p.green(), p.blue(), out)
        result[i * 3] = out[0]
        result[i * 3 + 1] = out[1]
        result[i * 3 + 2] = out[2]
    }
    return result
}
