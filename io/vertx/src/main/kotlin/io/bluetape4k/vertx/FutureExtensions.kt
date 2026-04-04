package io.bluetape4k.vertx

import java.util.concurrent.CompletableFuture

/**
 * Vert.x [Future]를 Java [CompletableFuture]로 변환합니다.
 *
 * ```kotlin
 * val vertxFuture: Future<String> = Future.succeededFuture("hello")
 * val cf: CompletableFuture<String> = vertxFuture.asCompletableFuture()
 * val result = cf.get()
 * // result == "hello"
 * ```
 *
 * @return [CompletableFuture] 인스턴스
 */
fun <T> io.vertx.core.Future<T>.asCompletableFuture(): CompletableFuture<T> =
    this.toCompletionStage().toCompletableFuture()
