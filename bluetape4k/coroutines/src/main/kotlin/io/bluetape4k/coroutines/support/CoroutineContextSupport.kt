package io.bluetape4k.coroutines.support

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 비어 있는 컨텍스트면 현재 코루틴 컨텍스트로 대체하고, 아니면 원본을 반환합니다.
 *
 * ## 동작/계약
 * - 수신 객체가 `EmptyCoroutineContext`이면 `currentCoroutineContext()`를 반환합니다.
 * - 그 외 컨텍스트는 새로 복사하지 않고 동일 인스턴스를 그대로 반환합니다.
 * - 컨텍스트 조회만 수행하며 예외를 강제로 발생시키는 자체 검증 로직은 없습니다.
 *
 * ```kotlin
 * val ctx = EmptyCoroutineContext.getOrCurrent()
 * // ctx == currentCoroutineContext()
 * ```
 */
suspend inline fun CoroutineContext.getOrCurrent(): CoroutineContext = when (this) {
    is EmptyCoroutineContext -> currentCoroutineContext()
    else                     -> this
}
