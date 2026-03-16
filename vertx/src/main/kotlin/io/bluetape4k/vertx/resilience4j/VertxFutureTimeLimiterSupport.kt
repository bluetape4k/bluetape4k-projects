package io.bluetape4k.vertx.resilience4j

import io.github.resilience4j.timelimiter.TimeLimiter
import io.vertx.core.Future
import io.vertx.core.Promise
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Vert.x [Future]를 [TimeLimiter]로 decorate 하여 실행합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("test")
 * val future = timeLimiter.executeVertxFuture { service.returnHelloWorld() }
 * ```
 *
 * @param scheduler [ScheduledExecutorService] 객체
 * @param supplier Vert.x [Future]를 생성하는 함수
 * @return [Future] 객체
 */
inline fun <T, F : Future<T>> TimeLimiter.executeVertxFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline supplier: () -> F,
): Future<T> = decorateVertxFuture(scheduler, supplier).invoke()

/**
 * Vert.x [Future]를 [TimeLimiter]로 decorate 합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("test")
 * val decorated = timeLimiter.decorateVertxFuture { service.returnHelloWorld() }
 * val future = decorated.invoke()
 * ```
 *
 * @param scheduler [ScheduledExecutorService] 객체
 * @param supplier Vert.x [Future]를 생성하는 함수
 * @return [supplier] 를 [TimeLimiter]로 decorate 한 함수
 */
inline fun <T, F : Future<T>> TimeLimiter.decorateVertxFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline supplier: () -> F,
): () -> Future<T> =
    {
        val promise = Promise.promise<T>()

        decorateCompletionStage(scheduler) { supplier().toCompletionStage() }
            .get()
            .whenComplete { result, cause ->
                if (cause != null) {
                    promise.fail(cause)
                } else {
                    promise.complete(result)
                }
            }

        promise.future()
    }
