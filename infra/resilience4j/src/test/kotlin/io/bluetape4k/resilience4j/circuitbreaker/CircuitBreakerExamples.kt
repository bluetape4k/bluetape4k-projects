package io.bluetape4k.resilience4j.circuitbreaker

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.resilience4j.SuspendHelloWorldService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class CircuitBreakerExamples {

    companion object: KLogging()

    // Process 범위에서 CircuitBreaker를 관리하기 위한 Registry 입니다.
    private val breakerRegistry = CircuitBreakerRegistry.ofDefaults().apply {
        eventPublisher
            .onEntryAdded { addedEvent ->
                val addedBreaker = addedEvent.addedEntry
                log.info { "CircuitBreaker [${addedBreaker.name}] added." }

                addedBreaker.eventPublisher.onEvent { evt ->
                    log.info { evt.toString() }
                }
            }
            .onEntryRemoved { removeEvent ->
                val removedBreaker = removeEvent.removedEntry
                log.info { "CircuitBreaker [${removedBreaker.name}] removed." }
            }
    }

    @Test
    fun `execute successful function`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("test")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val helloWorldService = SuspendHelloWorldService()

        val result = circuitBreaker.executeSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        result shouldBeEqualTo "Hello world"
        metrics.numberOfBufferedCalls shouldBeEqualTo 1
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 1

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `circuit breaker with supplier`() {
        val breaker = breakerRegistry.circuitBreaker("supplier")

        val supplier = breaker.checkedSupplier {
            "This can be any method which returns: `Hello"
        }
        val result = runCatching { supplier() }.map { "$it world`" }

        result.isSuccess.shouldBeTrue()
        result.getOrNull() shouldBeEqualTo "This can be any method which returns: `Hello world`"
    }
}
