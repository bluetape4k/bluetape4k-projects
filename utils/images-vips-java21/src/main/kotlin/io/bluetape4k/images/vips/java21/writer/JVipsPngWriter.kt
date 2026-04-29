package io.bluetape4k.images.vips.java21.writer

import com.criteo.vips.VipsImage
import io.bluetape4k.images.vips.VipsEncodeException
import io.bluetape4k.images.vips.VipsEncodeOptions
import com.criteo.vips.VipsException

/**
 * JVips PNG 인코더.
 *
 * PNG는 무손실 압축입니다. `quality` 옵션은 무시되며 `effort`는 압축 레벨(0–9)로 매핑됩니다.
 * 출력 바이트의 첫 4바이트는 반드시 `89 50 4E 47` (PNG 매직 바이트)입니다.
 */
internal object JVipsPngWriter {

    /**
     * [VipsImage]를 PNG 바이트 배열로 인코딩합니다.
     *
     * @param image 원본 JVips 이미지
     * @param options 인코딩 옵션 (`effort`를 압축 레벨로 사용, `stripMetadata` 사용)
     * @throws VipsEncodeException 인코딩 실패 시
     */
    fun writeToBytes(image: VipsImage, options: VipsEncodeOptions): ByteArray {
        return try {
            // writePNGToArray(compression, interlace, palette, dither, stripMetadata)
            // effort(1-9)는 libvips PNG compression level(0-9)로 매핑
            image.writePNGToArray(options.effort, false, 0, options.stripMetadata)
        } catch (e: VipsException) {
            throw VipsEncodeException("PNG encoding failed", e)
        }
    }
}
