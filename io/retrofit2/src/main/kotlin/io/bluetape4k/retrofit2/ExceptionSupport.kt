package io.bluetape4k.retrofit2

import java.io.IOException

/**
 * 임의의 [Throwable]을 [IOException]으로 정규화합니다.
 *
 * ## 동작/계약
 * - 이미 [IOException]이면 같은 인스턴스를 반환합니다.
 * - 그 외 예외는 원인(cause)로 포함한 새 [IOException]을 생성합니다.
 * - 메시지는 `this.message`가 있으면 사용하고, 없으면 `toString()`을 사용합니다.
 *
 * ```kotlin
 * val ioe = IllegalStateException("boom").toIOException()
 * // ioe.cause is IllegalStateException == true
 * ```
 */
fun Throwable.toIOException(): IOException {
    return when (this) {
        is IOException -> this
        else -> IOException(this.message ?: this.toString(), this)
    }
}
