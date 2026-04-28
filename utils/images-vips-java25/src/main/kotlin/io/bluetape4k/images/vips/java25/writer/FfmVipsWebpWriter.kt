package io.bluetape4k.images.vips.java25.writer

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.VipsError
import app.photofox.vipsffm.VipsOption
import io.bluetape4k.images.vips.VipsEncodeException
import io.bluetape4k.images.vips.VipsEncodeOptions

/**
 * vips-ffm WebP 인코더.
 *
 * 출력 바이트의 첫 4바이트는 반드시 `52 49 46 46` (`RIFF`)이며,
 * 오프셋 8부터 4바이트는 `57 45 42 50` (`WEBP`)입니다.
 */
internal object FfmVipsWebpWriter {

    fun writeToBytes(image: VImage, options: VipsEncodeOptions): ByteArray {
        return try {
            val blob = image.webpsaveBuffer(
                VipsOption.Int("Q", options.quality),
                VipsOption.Boolean("lossless", options.lossless),
                VipsOption.Boolean("strip", options.stripMetadata),
            )
            val buf = blob.asClonedByteBuffer()
            ByteArray(buf.remaining()).also { buf.get(it) }
        } catch (e: VipsError) {
            throw VipsEncodeException("WebP encoding failed", e)
        }
    }
}
