package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.PngWriter
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.io.OutputStream

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
class CoPngWriter(
    compressionLevel: Int = 9,
): PngWriter(compressionLevel), CoImageWriter {

    companion object: KLogging() {
        @JvmStatic
        val MaxCompression = CoPngWriter(9)

        @JvmStatic
        val MinCompression = CoPngWriter(1)

        @JvmStatic
        val NoComppression = CoPngWriter(0)
    }

    override fun withCompression(compression: Int): CoPngWriter {
        return CoPngWriter(compression)
    }

    override fun withMaxCompression(): CoPngWriter {
        return MaxCompression
    }

    override fun withMinCompression(): CoPngWriter {
        return MinCompression
    }

    override suspend fun writeSuspending(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        withContext(currentCoroutineContext()) {
            write(image, metadata, out)
        }
    }
}
