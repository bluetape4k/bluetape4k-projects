package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import com.sksamuel.scrimage.nio.AnimatedImageWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * [AnimatedImageWriter] 를 Coroutines 를 이용하여 비동기 방식으로 처리할 수 있도록 한다.
 */
interface SuspendAnimatedImageWriter: AnimatedImageWriter {

    /**
     * Coroutines 방식으로 [gif]를 [out]에 씁니다.
     *
     * - 내부적으로 [Dispatchers.IO] 컨텍스트에서 실행합니다.
     *
     * @param gif 출력할 [AnimatedGif]
     * @param out 쓰기 대상 [OutputStream]
     */
    suspend fun suspendWrite(gif: AnimatedGif, out: OutputStream) {
        withContext(Dispatchers.IO) {
            write(gif, out)
        }
    }
}
