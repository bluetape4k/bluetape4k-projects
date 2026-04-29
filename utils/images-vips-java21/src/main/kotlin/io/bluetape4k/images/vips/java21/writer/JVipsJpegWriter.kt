package io.bluetape4k.images.vips.java21.writer

import com.criteo.vips.VipsImage
import io.bluetape4k.images.vips.VipsEncodeException
import io.bluetape4k.images.vips.VipsEncodeOptions
import com.criteo.vips.VipsException

/**
 * JVips JPEG 인코더.
 *
 * 출력 바이트의 첫 3바이트는 반드시 `FF D8 FF` (JPEG 매직 바이트)입니다.
 */
internal object JVipsJpegWriter {

    /**
     * [VipsImage]를 JPEG 바이트 배열로 인코딩합니다.
     *
     * @param image 원본 JVips 이미지
     * @param options 인코딩 옵션 (`quality`, `stripMetadata` 사용)
     * @throws VipsEncodeException 인코딩 실패 시
     */
    fun writeToBytes(image: VipsImage, options: VipsEncodeOptions): ByteArray {
        return try {
            image.writeJPEGToArray(options.quality, options.stripMetadata)
        } catch (e: VipsException) {
            throw VipsEncodeException("JPEG encoding failed", e)
        }
    }
}
