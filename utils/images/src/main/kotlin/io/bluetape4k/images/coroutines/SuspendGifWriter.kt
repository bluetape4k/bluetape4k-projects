package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.nio.GifWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel

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
}
