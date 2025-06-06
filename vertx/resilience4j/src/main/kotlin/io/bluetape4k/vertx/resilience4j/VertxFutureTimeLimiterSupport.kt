package io.bluetape4k.vertx.resilience4j

import io.github.resilience4j.timelimiter.TimeLimiter
import io.vertx.core.Future
import io.vertx.core.Promise
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Vert.x [Future]를 [TimeLimiter]로 decorate 하여 실행합니다.
 */
inline fun <T, F: Future<T>> TimeLimiter.executeVertxFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory()),
    @BuilderInference crossinline supplier: () -> F,
): Future<T> {
    return decorateVertxFuture(scheduler, supplier).invoke()
}

/**
 * Vert.x [Future]를 [TimeLimiter]로 decorate 합니다.
 */
inline fun <T, F: Future<T>> TimeLimiter.decorateVertxFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    @BuilderInference crossinline supplier: () -> F,
): () -> Future<T> = {
    val promise = Promise.promise<T>()

    decorateCompletionStage(scheduler) { supplier().toCompletionStage() }
        .get()
        .whenComplete { result, cause ->
            if (cause != null) promise.fail(cause)
            else promise.complete(result)
        }

    promise.future()
}
