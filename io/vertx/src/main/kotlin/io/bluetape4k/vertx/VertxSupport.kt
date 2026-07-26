package io.bluetape4k.vertx

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val defaultVertxLock = ReentrantLock()

@Volatile
private var defaultVertxRef: Vertx? = null

/**
 * Returns the Vert.x instance that owns the current context, or the managed default Vert.x instance.
 *
 * ## Behaviour / Contract
 * - Inside a Vert.x context, returns `Vertx.currentContext().owner()`.
 * - Outside a Vert.x context, returns [defaultVertx] instead of creating an unowned Vert.x instance.
 * - Close the managed fallback with [closeDefaultVertx] during application shutdown or test cleanup.
 *
 * ```kotlin
 * val vertx = currentVertx()
 * // Uses the current context owner when present; otherwise uses the managed default Vert.x.
 * ```
 */
fun currentVertx(): Vertx = Vertx.currentContext()?.owner() ?: defaultVertx()

/**
 * Returns the process-local managed default [Vertx] instance.
 *
 * ## Behaviour / Contract
 * - The instance is created lazily and reused across calls.
 * - The lifecycle is owned by this module only when callers use this function or [currentVertx]
 *   outside a Vert.x context.
 * - Call [closeDefaultVertx] to close and clear the managed instance.
 */
fun defaultVertx(): Vertx {
    defaultVertxRef?.let { return it }

    return defaultVertxLock.withLock {
        defaultVertxRef ?: Vertx.vertx().also { defaultVertxRef = it }
    }
}

/**
 * Closes the managed default [Vertx] instance, if one has been created.
 *
 * Returns a succeeded future when no managed default instance exists.
 */
fun closeDefaultVertx(): Future<Void> {
    val vertx = defaultVertxLock.withLock {
        defaultVertxRef.also { defaultVertxRef = null }
    }

    return vertx?.close() ?: Future.succeededFuture()
}

/**
 * 현재 [Vertx] 인스턴스의 dispatcher 내에서 [block]을 실행합니다.
 *
 * ```kotlin
 * val result = vertx.withVertxDispatcher {
 *    // Vertx dispatcher 내에서 실행됩니다.
 *    // suspend 함수 호출이 가능합니다.
 *    "done"
 * }
 * // result == "done"
 * ```
 *
 * @param block Vertx Dispatcher 내에서 수행할 코드 블럭
 * @return 코드 블럭의 실행 결과
 */
suspend inline fun <T> Vertx.withVertxDispatcher(crossinline block: suspend CoroutineScope.() -> T): T =
    withContext(dispatcher()) {
        block()
    }
