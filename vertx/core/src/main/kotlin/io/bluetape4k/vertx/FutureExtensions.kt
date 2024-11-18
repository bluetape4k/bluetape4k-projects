package io.bluetape4k.vertx

import java.util.concurrent.CompletableFuture

/**
 * Vert.x [Future]를 [CompletableFuture]로 변환합니다.
 */
fun <T> io.vertx.core.Future<T>.asCompletableFuture(): CompletableFuture<T> {
    return this.toCompletionStage().toCompletableFuture()
}
