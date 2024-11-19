package io.bluetape4k.images.io

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import javax.imageio.stream.ImageOutputStream

// TODO: ImageOutputStream 관련 Extension Function 구현

suspend fun ImageOutputStream.usingSuspend(block: suspend (ImageOutputStream) -> Unit) {
    try {
        block(this)
    } finally {
        withContext(currentCoroutineContext()) {
            this@usingSuspend.close()
        }
    }
}
