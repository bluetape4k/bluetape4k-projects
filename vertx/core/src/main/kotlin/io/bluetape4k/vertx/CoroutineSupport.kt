package io.bluetape4k.vertx

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * 현재 [Vertx] 인스턴스의 [CoroutineDispatcher]를 반환합니다.
 */
fun currentVertxDispatcher(): CoroutineDispatcher = currentVertx().dispatcher()

/**
 * 현재 [Vertx]의 `dispatcher()`를 사용하는 [CoroutineScope]를 빌드합니다.
 */
fun Vertx.asCoroutineScope(): CoroutineScope = CoroutineScope(this.dispatcher())

/**
 * 현재 [Vertx]의 스레드를 사용하는 [CoroutineScope]를 빌드합니다.
 */
fun currentVertxCoroutineScope(): CoroutineScope = currentVertx().asCoroutineScope()
