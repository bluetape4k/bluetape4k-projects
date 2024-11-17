package io.bluetape4k.coroutines.support

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 현 Coroutine Context 가 EmptyCoroutineContext 인 경우 `currentCoroutineContext()` 를 반환합니다.
 *
 * ```
 * val context = EmptyCoroutineContext.getOrCurrent()
 * ```
 */
suspend inline fun CoroutineContext.getOrCurrent(): CoroutineContext = when (this) {
    is EmptyCoroutineContext -> currentCoroutineContext()
    else                     -> this
}
