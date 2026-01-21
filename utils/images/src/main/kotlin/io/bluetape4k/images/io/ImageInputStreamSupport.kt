package io.bluetape4k.images.io

import kotlinx.coroutines.coroutineScope
import javax.imageio.stream.ImageOutputStream

// TODO: ImageOutputStream 관련 Extension Function 구현

suspend inline fun ImageOutputStream.usingSuspend(
    @BuilderInference crossinline block: suspend (ImageOutputStream) -> Unit,
) = coroutineScope {
    this@usingSuspend.use {
        block(this@usingSuspend)
    }
}
