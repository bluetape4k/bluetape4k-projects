package io.bluetape4k.images.vips.java21.ops

import com.criteo.vips.VipsImage
import io.bluetape4k.support.requirePositiveNumber

/**
 * JVips `VipsImage`를 지정 크기로 리사이즈합니다.
 *
 * **중요**: `VipsImage`를 in-place로 변경합니다. 호출 전 `clone()`하십시오.
 */
internal fun resizeWithJVips(image: VipsImage, width: Int, height: Int) {
    width.requirePositiveNumber("width")
    height.requirePositiveNumber("height")
    // crop=false: 비율 무시하고 목표 크기로 리사이즈
    image.resize(width, height, false)
}
