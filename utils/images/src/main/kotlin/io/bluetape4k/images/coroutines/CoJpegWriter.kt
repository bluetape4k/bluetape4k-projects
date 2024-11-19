package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.JpegWriter
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
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
class CoJpegWriter(
    val compression: Int = 80,
    val progressive: Boolean = false,
): JpegWriter(compression, progressive), CoImageWriter {

    companion object: KLogging() {
        @JvmStatic
        val Default = CoJpegWriter(80, false)

        @JvmStatic
        val CompressionFromMetaData = CoJpegWriter(-1, false)
    }

    /**
     * 압축 정보를 변경합니다.
     */
    override fun withCompression(compression: Int): CoJpegWriter {
        return CoJpegWriter(compression, progressive)
    }

    /**
     * 프로그레시브 JPEG 여부를 변경합니다.
     */
    override fun withProgressive(progressive: Boolean): CoJpegWriter {
        return CoJpegWriter(compression, progressive)
    }

    override suspend fun writeSuspending(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        withContext(currentCoroutineContext()) {
            write(image, metadata, out)
        }
    }
}
