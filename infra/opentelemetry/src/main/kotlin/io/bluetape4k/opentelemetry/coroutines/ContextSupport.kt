package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.coroutines.support.getOrCurrent
import io.bluetape4k.opentelemetry.currentOtelContext
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Current Coroutine Context와 Opentelemetry [Context] 하에서 [block]을 실행합니다.
 *
 * @param T [block]의 실행 결과 타입입니다.
 * @param block 실행할 코드 블록입니다.
 * @return [block]의 실행 결과입니다.
 */
suspend inline fun <T> withOtelContext(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    otelContext: Context = currentOtelContext(),
    crossinline block: suspend CoroutineScope.() -> T,
): T {
    val coContext = coroutineContext.getOrCurrent()
    return withContext(coContext + otelContext.asContextElement()) {
        block()
    }
}

/**
 * Current Coroutine Context와 Current Opentelemetry [Context] 하에서 [block]을 실행합니다.
 *
 * @param T [block]의 실행 결과 타입입니다.
 * @param block 실행할 코드 블록입니다.
 * @return [block]의 실행 결과입니다.
 */
suspend inline fun <T> Context.withOtelContext(
    crossinline block: suspend CoroutineScope.() -> T,
): T {
    return withContext(currentCoroutineContext() + this.asContextElement()) {
        block()
    }
}
