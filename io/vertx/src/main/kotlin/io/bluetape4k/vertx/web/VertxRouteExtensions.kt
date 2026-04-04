package io.bluetape4k.vertx.web

import io.bluetape4k.vertx.currentVertxCoroutineScope
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.launch

/**
 * route 핸들러 목록에 요청 핸들러를 추가합니다.
 *
 * ```kotlin
 * router.get("/some/path").suspendHandler { ctx ->
 *    ctx.response().end("Hello, World!")
 *    // ctx.next() // 다음 핸들러로 이동
 *    // ctx.fail(500) // 실패 핸들러로 이동
 * }
 * // GET /some/path → "Hello, World!"
 * ```
 *
 * @param requestHandler request handler
 * @return a reference to this, so the API can be used fluently
 */
inline fun Route.suspendHandler(
    crossinline requestHandler: suspend (RoutingContext) -> Unit,
): Route {
    return handler { ctx ->
        currentVertxCoroutineScope().launch {
            try {
                requestHandler(ctx)
            } catch (e: Throwable) {
                ctx.fail(e)
            }
        }
    }
}

/**
 * 실패 route 핸들러 목록에 실패 핸들러(`requestHandler`) 를 추가합니다.
 *
 * ```kotlin
 * router.get("/some/path").suspendFailureHandler { ctx ->
 *     ctx.response().setStatusCode(500).end("Internal Error")
 *     // 실패 처리 후 응답 전송
 * }
 * // GET /some/path 실패 시 → 500 응답
 * ```
 *
 * @param requestHandler  실패를 처리하는 요청 핸들러
 * @return a reference to this, so the API can be used fluently
 */
inline fun Route.suspendFailureHandler(
    crossinline requestHandler: suspend (RoutingContext) -> Unit,
): Route {
    return failureHandler { ctx ->
        currentVertxCoroutineScope().launch {
            try {
                requestHandler(ctx)
            } catch (e: Throwable) {
                ctx.fail(e)
            }
        }
    }
}
