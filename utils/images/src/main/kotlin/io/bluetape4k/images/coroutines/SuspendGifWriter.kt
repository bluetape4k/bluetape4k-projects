package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.nio.GifWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * Coroutines 방식으로 GIF 이미지를 생성하는 [SuspendImageWriter] 입니다.
 *
 * ```kotlin
 * val writer = SuspendGifWriter()
 * val image = immutableImageOf(File("image.gif"))
 * image.suspendWrite(writer, Paths.get("output.gif"))
 * ```
 *
 * @param progressive 프로그레시브 GIF 이미지를 생성할지 여부
 */
class SuspendGifWriter(
    progressive: Boolean = false,
): GifWriter(progressive), SuspendImageWriter {

    companion object: KLoggingChannel() {
        /** 기본 GIF Writer (비프로그레시브) */
        @JvmStatic
        val Default = SuspendGifWriter(false)

        /** 프로그레시브 GIF Writer */
        @JvmStatic
        val Progressive = SuspendGifWriter(true)
    }

    /**
     * 프로그레시브 여부를 설정한 새 [SuspendGifWriter]를 반환합니다.
     *
     * @param progressive 프로그레시브 여부
     * @return 프로그레시브 설정이 적용된 [SuspendGifWriter]
     */
    override fun withProgressive(progressive: Boolean): SuspendGifWriter {
        return SuspendGifWriter(progressive)
    }
}
