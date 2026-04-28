package io.bluetape4k.images.vips.java25.writer

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.VipsError
import app.photofox.vipsffm.VipsOption
import io.bluetape4k.images.vips.VipsEncodeException
import io.bluetape4k.images.vips.VipsEncodeOptions

/**
 * vips-ffm JPEG 인코더.
 *
 * 출력 바이트의 첫 3바이트는 반드시 `FF D8 FF` (JPEG 매직 바이트)입니다.
 */
internal object FfmVipsJpegWriter {

    fun writeToBytes(image: VImage, options: VipsEncodeOptions): ByteArray {
        return try {
            val blob = image.jpegsaveBuffer(
                VipsOption.Int("Q", options.quality),
                VipsOption.Boolean("strip", options.stripMetadata),
            )
            val buf = blob.asClonedByteBuffer()
            ByteArray(buf.remaining()).also { buf.get(it) }
        } catch (e: VipsError) {
            throw VipsEncodeException("JPEG encoding failed", e)
        }
    }
}
