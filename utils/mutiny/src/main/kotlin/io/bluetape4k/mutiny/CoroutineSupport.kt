package io.bluetape4k.mutiny

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

/**
 * Produce a [Uni] from given [block] in a non-suspending context.
 */
inline fun <T> CoroutineScope.asUni(
    crossinline block: suspend CoroutineScope.() -> T,
): Uni<T> {
    return async {
        block(this@asUni)
    }.asUni()
}
