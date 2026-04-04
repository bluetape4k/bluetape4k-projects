package io.bluetape4k.vertx

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

/**
 * 현재 실행 중인 [Vertx] 인스턴스를 반환합니다.
 * 현재 컨텍스트가 없으면 새 [Vertx] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val vertx = currentVertx()
 * // vertx != null  (기존 컨텍스트 또는 새로 생성된 인스턴스)
 * ```
 */
fun currentVertx(): Vertx = Vertx.currentContext()?.owner() ?: Vertx.vertx()

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
