package io.bluetape4k.retrofit2

import java.io.IOException

/**
 * [Throwable]을 [IOException]으로 변환합니다.
 */
fun Throwable.toIOException(): IOException {
    return when (this) {
        is IOException -> this
        else           -> {
            val message = this.message ?: this.toString()
            IOException(message, this)
        }
    }
}
