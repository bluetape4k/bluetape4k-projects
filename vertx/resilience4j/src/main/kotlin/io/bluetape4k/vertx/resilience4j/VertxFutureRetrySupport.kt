package io.bluetape4k.vertx.resilience4j

import io.github.resilience4j.retry.Retry
import io.vertx.core.Future
import io.vertx.core.Promise
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Vert.x [Future]를 [Retry]로 decorate 하여 실행합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("test")
 * val future = retry.executeVertxFuture { service.returnHelloWorld() }
 * ```
 *
 * @param scheduler [ScheduledExecutorService] 객체
 * @param supplier Vert.x [Future]를 생성하는 함수
 */
inline fun <T> Retry.executeVertxFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    @BuilderInference crossinline supplier: () -> Future<T>,
): Future<T> {
    return decorateVertxFuture(scheduler, supplier).invoke()
}

/**
 * Vert.x [Future]를 [Retry]로 decorate 합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("test")
 * val decorated = retry.decorateVertxFuture { service.returnHelloWorld() }
 * val future = decorated.invoke()
 * ```
 *
 * @param scheduler [ScheduledExecutorService] 객체
 * @param supplier Vert.x [Future]를 생성하는 함수
 * @return [supplier] 를 [Retry]로 decorate 한 함수
 */
inline fun <T> Retry.decorateVertxFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    @BuilderInference crossinline supplier: () -> Future<T>,
): () -> Future<T> = {
    val promise = Promise.promise<T>()

    Retry.decorateCompletionStage(this, scheduler) { supplier().toCompletionStage() }
        .get()
        .whenComplete { result, cause ->
            if (cause != null) promise.fail(cause)
            else promise.complete(result)
        }

    promise.future()
}
