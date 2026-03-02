package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.webp.Gif2WebpWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireInRange

/**
 * [Gif2WebpWriter] 에 Coroutines 를 이용하여 비동기 방식으로 처리할 수 있도록 한다.
 */
class SuspendGif2WebpWriter(
    private val q: Int = -1,
    private val m: Int = -1,
    private val lossy: Boolean = false,
): Gif2WebpWriter(q, m, lossy), SuspendAnimatedImageWriter {

    companion object: KLoggingChannel() {
        val Default = SuspendGif2WebpWriter()
    }

    /**
     * 손실 압축 모드를 활성화한 새 [SuspendGif2WebpWriter]를 반환합니다.
     *
     * @return 손실 압축이 활성화된 [SuspendGif2WebpWriter]
     */
    override fun withLossy(): SuspendGif2WebpWriter {
        return SuspendGif2WebpWriter(q, m, true)
    }

    /**
     * 품질(Quality) 값을 설정한 새 [SuspendGif2WebpWriter]를 반환합니다.
     *
     * @param q 품질 값 (0~100)
     * @return 품질이 설정된 [SuspendGif2WebpWriter]
     */
    override fun withQ(q: Int): SuspendGif2WebpWriter {
        q.requireInRange(0, 100, "q")
        return SuspendGif2WebpWriter(q, m, lossy)
    }

    /**
     * 압축 방법(Method) 값을 설정한 새 [SuspendGif2WebpWriter]를 반환합니다.
     *
     * @param m 압축 방법 값 (0~6)
     * @return 압축 방법이 설정된 [SuspendGif2WebpWriter]
     */
    override fun withM(m: Int): SuspendGif2WebpWriter {
        m.requireInRange(0, 6, "m")
        return SuspendGif2WebpWriter(q, m, lossy)
    }
}
