package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.webp.WebpWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.io.OutputStream

/**
 * Coroutines 방식으로 WebP 형식의 이미지를 생성하는 [CoImageWriter] 입니다.
 *
 * 사진보다는 일러스트같은 이미지 형태에 대해서 압축률이 상당히 좋다.
 * JPG와 비교하여 처리하는 데는 시간이 더 걸리지만, 압축률은 2배 이상 좋다
 *
 * **NOTE: 동적인 처리 시에는 사용하지 않는 것을 추천합니다.**
 *
 * ```
 * val writer = CoWebpWriter()
 * val image = immutableImageOf(File("image.webp"))
 * writer.write(image, File("output.webp"))
 * ```
 */
class SuspendWebpWriter(
    private val z: Int = -1,
    private val q: Int = -1,
    private val m: Int = -1,
    private val lossless: Boolean = false,
    private val noAlpha: Boolean = false,
): WebpWriter(z, q, m, lossless, noAlpha), SuspendImageWriter {

    companion object: KLoggingChannel() {
        @JvmStatic
        val Default = SuspendWebpWriter()

        /**
         * Max lossless compression - 압축에 많은 시간이 걸린다. 배치 작업 시 사용하기 좋다.
         */
        @JvmStatic
        val MaxLosslessCompression = Default.withZ(9)
    }

    override fun withLossless(): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, true, noAlpha)
    }

    override fun withoutAlpha(): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, lossless, true)
    }

    override fun withQ(q: Int): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, lossless, noAlpha)
    }

    override fun withM(m: Int): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, lossless, noAlpha)
    }

    override fun withZ(z: Int): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, lossless, noAlpha)
    }

    override suspend fun suspendWrite(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        write(image, metadata, out)
    }
}
