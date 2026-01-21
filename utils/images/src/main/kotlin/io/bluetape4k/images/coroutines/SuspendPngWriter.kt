package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.nio.PngWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * Coroutines 방식으로 PNG 파일을 생성하는 [CoImageWriter] 입니다.
 *
 * ```
 * val writer = CoPngWriter()
 * val image = immutableImageOf(File("image.png"))
 * writer.write(image, File("output.png"))
 * ```
 *
 * @param compressionLevel
 */
class SuspendPngWriter(
    compressionLevel: Int = 9,
): PngWriter(compressionLevel), SuspendImageWriter {

    companion object: KLoggingChannel() {
        @JvmStatic
        val MaxCompression = SuspendPngWriter(9)

        @JvmStatic
        val MinCompression = SuspendPngWriter(1)

        @JvmStatic
        val NoComppression = SuspendPngWriter(0)
    }

    override fun withCompression(compression: Int): SuspendPngWriter {
        return SuspendPngWriter(compression)
    }

    override fun withMaxCompression(): SuspendPngWriter {
        return MaxCompression
    }

    override fun withMinCompression(): SuspendPngWriter {
        return MinCompression
    }
}
