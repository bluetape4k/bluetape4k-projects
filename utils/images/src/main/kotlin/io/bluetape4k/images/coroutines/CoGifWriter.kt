package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.GifWriter
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
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
class CoGifWriter(
    progressive: Boolean = false,
): GifWriter(progressive), CoImageWriter {

    companion object: KLogging() {
        @JvmStatic
        val Default = CoGifWriter(false)

        @JvmStatic
        val Progressive = CoGifWriter(true)
    }

    /**
     * 진행형 Gif 이미지를 생성합니다.
     */
    override fun withProgressive(progressive: Boolean): CoGifWriter {
        return CoGifWriter(progressive)
    }

    /**
     * [AwtImage]를 [OutputStream]에 gif 이미지로 쓰기 위해 사용합니다.
     *
     * @param image 이미지 정보를 담은 [AwtImage]
     * @param metadata 이미지 메타데이터
     * @param out 이미지를 쓸 [OutputStream]
     */
    override suspend fun writeSuspending(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        withContext(currentCoroutineContext()) {
            write(image, metadata, out)
        }
    }
}
