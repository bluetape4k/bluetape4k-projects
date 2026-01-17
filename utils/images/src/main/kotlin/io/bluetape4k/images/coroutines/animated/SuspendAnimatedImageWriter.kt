package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import com.sksamuel.scrimage.nio.AnimatedImageWriter
import kotlinx.coroutines.coroutineScope
import java.io.OutputStream

/**
 * [AnimatedImageWriter] 를 Coroutines 를 이용하여 비동기 방식으로 처리할 수 있도록 한다.
 */
interface SuspendAnimatedImageWriter: AnimatedImageWriter {

    suspend fun suspendWrite(gif: AnimatedGif, out: OutputStream) = coroutineScope {
        write(gif, out)
    }
}
