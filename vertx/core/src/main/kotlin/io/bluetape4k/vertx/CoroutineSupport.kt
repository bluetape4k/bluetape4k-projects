package io.bluetape4k.vertx

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Get [CoroutineDispatcher] of current [Vertx] instance.
 */
fun currentVertxDispatcher(): CoroutineDispatcher = currentVertx().dispatcher()

/**
 * 현 [Vertx]의 `dispatcher()`를 사용하는 [CoroutineScope]를 빌드합니다.
 */
fun Vertx.asCoroutineScope(): CoroutineScope = CoroutineScope(this.dispatcher())

/**
 * Current [Vertx]의 Thread를 사용하는 [CoroutineScope]를 빌드합니다.
 */
fun currentVertxCoroutineScope(): CoroutineScope = currentVertx().asCoroutineScope()
