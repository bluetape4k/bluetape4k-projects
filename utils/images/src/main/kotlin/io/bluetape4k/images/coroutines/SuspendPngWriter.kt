package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.nio.PngWriter
import io.bluetape4k.images.coroutines.SuspendPngWriter.Companion.MaxCompression
import io.bluetape4k.images.coroutines.SuspendPngWriter.Companion.MinCompression
import io.bluetape4k.images.coroutines.SuspendPngWriter.Companion.NoCompression
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * Coroutines 방식으로 PNG 파일을 생성하는 [SuspendImageWriter] 입니다.
 *
 * ```
 * val writer = SuspendPngWriter()
 * val image = immutableImageOf(File("image.png"))
 * writer.write(image, File("output.png"))
 * ```
 *
 * @param compressionLevel 압축 레벨 (0: 무압축, 9: 최대 압축, 기본값 9)
 */
class SuspendPngWriter(
    compressionLevel: Int = 9,
): PngWriter(compressionLevel), SuspendImageWriter {

    companion object: KLoggingChannel() {
        /** 최대 압축(level=9) [SuspendPngWriter] 인스턴스 */
        @JvmStatic
        val MaxCompression = SuspendPngWriter(9)

        /** 최소 압축(level=1) [SuspendPngWriter] 인스턴스 */
        @JvmStatic
        val MinCompression = SuspendPngWriter(1)

        /** 무압축(level=0) [SuspendPngWriter] 인스턴스 */
        @JvmStatic
        val NoCompression = SuspendPngWriter(0)

        /** @suppress 오타 호환용. [NoCompression]을 사용하세요. */
        @Deprecated("오타 수정: NoCompression을 사용하세요", replaceWith = ReplaceWith("NoCompression"))
        @JvmStatic
        val NoComppression = NoCompression
    }

    /**
     * 압축 레벨을 설정한 새 [SuspendPngWriter]를 반환합니다.
     *
     * @param compression 압축 레벨 (0: 무압축, 9: 최대 압축)
     * @return 압축 레벨이 설정된 [SuspendPngWriter]
     */
    override fun withCompression(compression: Int): SuspendPngWriter {
        return SuspendPngWriter(compression)
    }

    /**
     * 최대 압축(level=9)으로 설정된 [SuspendPngWriter]를 반환합니다.
     *
     * @return [MaxCompression] 인스턴스
     */
    override fun withMaxCompression(): SuspendPngWriter {
        return MaxCompression
    }

    /**
     * 최소 압축(level=1)으로 설정된 [SuspendPngWriter]를 반환합니다.
     *
     * @return [MinCompression] 인스턴스
     */
    override fun withMinCompression(): SuspendPngWriter {
        return MinCompression
    }
}
