package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.JpegWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.io.OutputStream

/**
 * Coroutines 방식으로 JPEG 이미지를 생성한다.
 *
 * ```
 * val writer = CoJpegWriter()
 * val image = immutableImageOf(File("image.jpg"))
 * writer.write(image, File("output.jpg"))
 * ```
 *
 * @property compression 압축 정보 (기본 80, 0 이면 최대 압축, 100이면 최소 압축)
 * @property progressive 프로그레시브 JPEG 여부
 */
class SuspendJpegWriter(
    val compression: Int = 80,
    val progressive: Boolean = false,
): JpegWriter(compression, progressive), SuspendImageWriter {

    companion object: KLoggingChannel() {
        @JvmStatic
        val Default = SuspendJpegWriter(80, false)

        @JvmStatic
        val CompressionFromMetaData = SuspendJpegWriter(-1, false)
    }

    /**
     * 압축 정보를 변경합니다.
     */
    override fun withCompression(compression: Int): SuspendJpegWriter {
        return SuspendJpegWriter(compression, progressive)
    }

    /**
     * 프로그레시브 JPEG 여부를 변경합니다.
     */
    override fun withProgressive(progressive: Boolean): SuspendJpegWriter {
        return SuspendJpegWriter(compression, progressive)
    }

    override suspend fun suspendWrite(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        write(image, metadata, out)
    }
}
