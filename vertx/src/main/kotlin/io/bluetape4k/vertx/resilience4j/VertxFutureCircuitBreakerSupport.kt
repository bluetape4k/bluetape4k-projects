package io.bluetape4k.vertx.resilience4j

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.vertx.core.Future
import java.util.concurrent.TimeUnit

/**
 * Vert.x [Future]를 [CircuitBreaker]로 decorate 하여 실행합니다.
 *
 * ```
 * val circuitBreaker = CircuitBreaker.ofDefaults("test")
 * val future = circuitBreaker.executeVertxFuture { service.returnHelloWorld() }
 * ```
 *
 * @param supplier Vert.x [Future]를 생성하는 함수
 * @return [Future] 객체
 */
inline fun <T> CircuitBreaker.executeVertxFuture(
    @BuilderInference crossinline supplier: () -> Future<T>,
): Future<T> {
    return decorateVertxFuture(supplier).invoke()
}

/**
 * Vert.x [Future]를 [CircuitBreaker]로 decorate 합니다.
 *
 * ```
 * val circuitBreaker = CircuitBreaker.ofDefaults("test")
 * val decorated = circuitBreaker.decorateVertxFuture { service.returnHelloWorld() }
 * val futureu = decorated.invoke()
 * ```
 *
 * @param supplier Vert.x [Future]를 생성하는 함수
 * @param supplier 를 CircuitBreaker로 decorate 한 함수
 */
inline fun <T> CircuitBreaker.decorateVertxFuture(
    @BuilderInference crossinline supplier: () -> Future<T>,
): () -> Future<T> = {
    if (!tryAcquirePermission()) {
        Future.failedFuture(CallNotPermittedException.createCallNotPermittedException(this))
    } else {
        val start = System.nanoTime()
        try {
            supplier.invoke()
                .onComplete { ar ->
                    val durationInNanos = System.nanoTime() - start
                    if (ar.succeeded()) {
                        onSuccess(durationInNanos, TimeUnit.NANOSECONDS)
                    } else {
                        onError(durationInNanos, TimeUnit.NANOSECONDS, ar.cause())
                    }
                }
        } catch (e: Exception) {
            val durationInNanos = System.nanoTime() - start
            onError(durationInNanos, TimeUnit.NANOSECONDS, e)
            releasePermission() // 예외 발생 시 권한 해제
            Future.failedFuture(e)
        }
    }
}
