package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import com.sksamuel.scrimage.nio.AnimatedImageWriter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * [AnimatedImageWriter] 를 Coroutines 를 이용하여 비동기 방식으로 처리할 수 있도록 한다.
 */
interface CoAnimatedImageWriter: AnimatedImageWriter {

    suspend fun writeSuspending(gif: AnimatedGif, out: OutputStream) {
        withContext(currentCoroutineContext()) {
            write(gif, out)
        }
    }
}
