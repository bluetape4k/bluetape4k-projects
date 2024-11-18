package io.bluetape4k.vertx.resilience4j

import io.github.resilience4j.ratelimiter.RateLimiter
import io.vertx.core.Future
import io.vertx.core.Promise

/**
 * Vert.x [Future]를 [RateLimiter]로 decorate 하여 실행합니다.
 *
 * ```
 * val rateLimiter = RateLimiter.ofDefaults("test")
 * val future = rateLimiter.executeVertxFuture { service.returnHelloWorld() }
 * ```
 *
 * @param supplier Vert.x [Future]를 생성하는 함수
 * @return [Future] 객체
 */
inline fun <T> RateLimiter.executeVertxFuture(
    crossinline supplier: () -> Future<T>,
): Future<T> {
    return decorateVertxFuture(supplier).invoke()
}

/**
 * Vert.x [Future]를 [RateLimiter]로 decorate 합니다.
 *
 * ```
 * val rateLimiter = RateLimiter.ofDefaults("test")
 * val decorated = rateLimiter.decorateVertxFuture { service.returnHelloWorld() }
 * val future = decorated.invoke()
 * ```
 *
 * @param supplier Vert.x [Future]를 생성하는 함수
 * @return [supplier] 를 [RateLimiter]로 decorate 한 함수
 */
inline fun <T> RateLimiter.decorateVertxFuture(
    crossinline supplier: () -> Future<T>,
): () -> Future<T> = {
    val promise = Promise.promise<T>()

    try {
        RateLimiter.waitForPermission(this, 1)
        supplier.invoke()
            .onSuccess { promise.complete(it) }
            .onFailure { promise.fail(it) }
    } catch (e: Throwable) {
        promise.fail(e)
    }

    promise.future()
}
