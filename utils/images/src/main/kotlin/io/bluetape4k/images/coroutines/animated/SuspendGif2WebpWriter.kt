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

    override fun withLossy(): SuspendGif2WebpWriter {
        return SuspendGif2WebpWriter(q, m, true)
    }

    override fun withQ(q: Int): SuspendGif2WebpWriter {
        q.requireInRange(0, 100, "q")
        return SuspendGif2WebpWriter(q, m, lossy)
    }

    override fun withM(m: Int): SuspendGif2WebpWriter {
        m.requireInRange(0, 6, "m")
        return SuspendGif2WebpWriter(q, m, lossy)
    }
}
