package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.GifWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.io.OutputStream

/**
 * Coroutines 방식으로 gif 이미지를 생성합니다.
 *
 * ```
 * val writer = CoGifWriter()
 * val image = immutableImageOf(File("image.gif"))
 * writer.write(image, File("output.gif"))
 * ```
 *
 * @param progressive 진행형 Gif 이미지를 생성할지 여부
 */
class SuspendGifWriter(
    progressive: Boolean = false,
): GifWriter(progressive), SuspendImageWriter {

    companion object: KLoggingChannel() {
        @JvmStatic
        val Default = SuspendGifWriter(false)

        @JvmStatic
        val Progressive = SuspendGifWriter(true)
    }

    /**
     * 진행형 Gif 이미지를 생성합니다.
     */
    override fun withProgressive(progressive: Boolean): SuspendGifWriter {
        return SuspendGifWriter(progressive)
    }

    /**
     * [AwtImage]를 [OutputStream]에 gif 이미지로 쓰기 위해 사용합니다.
     *
     * @param image 이미지 정보를 담은 [AwtImage]
     * @param metadata 이미지 메타데이터
     * @param out 이미지를 쓸 [OutputStream]
     */
    override suspend fun suspendWrite(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        write(image, metadata, out)
    }
}
