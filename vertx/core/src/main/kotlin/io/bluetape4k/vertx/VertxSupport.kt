package io.bluetape4k.vertx

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

/**
 * 현 [Vertx] 인스턴스
 */
fun currentVertx(): Vertx = Vertx.currentContext()?.owner() ?: Vertx.vertx()

/**
 * Run the block with the [Vertx] dispatcher.
 *
 * ```
 * vertx.withVertxDispatcher {
 *    // Do something with Vertx dispatcher
 *    // This block will be executed in the Vertx dispatcher
 *    // and will be suspended if it is a suspend function.
 *    // You can use `this` as CoroutineScope.
 *    // You can use `vertx` as Vertx instance.
 *    // You can use `context` as Context instance.
 * }
 * ```
 *
 * @param block Virtx Dispatcher 내에서 수행할 코드 블럭
 */
suspend inline fun <T> Vertx.withVertxDispatcher(crossinline block: suspend CoroutineScope.() -> T): T {
    return withContext(this.dispatcher()) {
        block(this)
    }
}
