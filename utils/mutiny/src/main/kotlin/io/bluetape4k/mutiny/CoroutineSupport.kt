package io.bluetape4k.mutiny

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext

/**
 * Produce a [Uni] from given [block] in a non-suspending context.
 *
 * The [suspendSupplier] block isn't attached to the structured concurrency of the current `coroutineContext` by default
 * but executed in the [GlobalScope], that means that failures raised from [suspendSupplier] will not be
 * thrown immediately but propagated to the resulting [Uni], similar to the behavior of `Uni.createFrom().item<T>(() -> T)`.
 * The behaviour can be changed by passing an own [context] that's used for `coroutines` execution of the given [suspendSupplier].
 */
inline fun <T> CoroutineScope.asUni(
    context: CoroutineContext = Dispatchers.Default,
    crossinline block: suspend CoroutineScope.() -> T,
): Uni<T> {
    return async(context) {
        block(this@asUni)
    }.asUni()
}
