package io.bluetape4k.images.vips.java21.ops

import com.criteo.vips.VipsImage
import io.bluetape4k.support.requirePositiveNumber

/**
 * JVips `VipsImage`를 긴 변 기준으로 썸네일 크기로 축소합니다.
 *
 * **중요**: `VipsImage`를 in-place로 변경합니다. 호출 전 `clone()`하십시오.
 *
 * @param maxDimension 긴 변의 최대 크기 (픽셀)
 */
internal fun thumbnailWithJVips(image: VipsImage, maxDimension: Int) {
    maxDimension.requirePositiveNumber("maxDimension")
    // crop=false: 비율 유지, 긴 변을 maxDimension에 맞춤
    image.thumbnailImage(maxDimension, maxDimension, false)
}
