package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.vertx.junit5.VertxExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
abstract class AbstractVertxFutureTest {

    companion object: KLoggingChannel()

    protected fun CircuitBreaker.applyEventPublisher() = apply {
        eventPublisher
            .onSuccess { log.debug { "Success to execute." } }
            .onError { log.error(it.throwable) { "Fail to execute in circuit-breaker context" } }
    }

    protected fun Retry.applyEventPublisher() = apply {
        eventPublisher
            .onSuccess { log.debug { "Success to execute. retry count=${it.numberOfRetryAttempts}" } }
            .onRetry { log.debug { "Retry to execute. event=$it" } }
            .onError { log.error(it.lastThrowable) { "Fail to execute in retry context" } }
    }
}
