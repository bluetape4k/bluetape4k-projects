package io.bluetape4k.images.vips.java25.ops

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.VipsOption
import io.bluetape4k.support.requirePositiveNumber
import java.lang.foreign.Arena

/**
 * vips-ffm [VImage]를 지정 크기(W×H)로 리사이즈합니다.
 *
 * vips-ffm의 `resize(scale)` 메서드는 비율(scale factor)을 받으므로,
 * 목표 크기에서 수평/수직 스케일을 각각 계산하여 전달합니다.
 */
internal fun resizeWithFfm(
    arena: Arena,
    image: VImage,
    targetWidth: Int,
    targetHeight: Int,
    currentWidth: Int,
    currentHeight: Int,
): VImage {
    targetWidth.requirePositiveNumber("targetWidth")
    targetHeight.requirePositiveNumber("targetHeight")
    val hscale = targetWidth.toDouble() / currentWidth
    val vscale = targetHeight.toDouble() / currentHeight
    return image.resize(hscale, VipsOption.Double("vscale", vscale))
}
