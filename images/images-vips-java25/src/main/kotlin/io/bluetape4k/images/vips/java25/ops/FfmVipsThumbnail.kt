package io.bluetape4k.images.vips.java25.ops

import app.photofox.vipsffm.VImage
import io.bluetape4k.support.requirePositiveNumber
import java.lang.foreign.Arena

/**
 * vips-ffm [VImage]를 긴 변 기준으로 썸네일 크기로 축소합니다.
 *
 * @param maxDimension 긴 변의 최대 크기 (픽셀)
 */
internal fun thumbnailWithFfm(arena: Arena, image: VImage, maxDimension: Int): VImage {
    maxDimension.requirePositiveNumber("maxDimension")
    return image.thumbnailImage(maxDimension)
}
