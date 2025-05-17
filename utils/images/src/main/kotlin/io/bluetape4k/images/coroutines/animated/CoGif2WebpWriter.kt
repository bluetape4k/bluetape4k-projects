package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.webp.Gif2WebpWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireInRange

/**
 * [Gif2WebpWriter] 에 Coroutines 를 이용하여 비동기 방식으로 처리할 수 있도록 한다.
 */
class CoGif2WebpWriter(
    private val q: Int = -1,
    private val m: Int = -1,
    private val lossy: Boolean = false,
): Gif2WebpWriter(q, m, lossy), CoAnimatedImageWriter {

    companion object: KLoggingChannel() {
        val Default = CoGif2WebpWriter()
    }

    override fun withLossy(): CoGif2WebpWriter {
        return CoGif2WebpWriter(q, m, true)
    }

    override fun withQ(q: Int): CoGif2WebpWriter {
        q.requireInRange(0, 100, "q")
        return CoGif2WebpWriter(q, m, lossy)
    }

    override fun withM(m: Int): CoGif2WebpWriter {
        m.requireInRange(0, 6, "m")
        return CoGif2WebpWriter(q, m, lossy)
    }
}
