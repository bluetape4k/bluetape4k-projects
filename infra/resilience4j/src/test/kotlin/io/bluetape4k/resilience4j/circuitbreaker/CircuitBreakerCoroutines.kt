package io.bluetape4k.resilience4j.circuitbreaker

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.resilience4j.CoHelloWorldService
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.decorateSuspendFunction
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CircuitBreakerCoroutines {

    companion object: KLogging()

    // Process 범위에서 CircuitBreaker를 관리하기 위한 Registry 입니다.
    val breakerRegistry = CircuitBreakerRegistry.ofDefaults().apply {
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

        val helloWorldService = CoHelloWorldService()

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
        val result = runCatching { supplier() }.map { "$it world`" }.recover { _ -> "Failed" }

        result.isSuccess.shouldBeTrue()
        result.getOrNull() shouldBeEqualTo "This can be any method which returns: `Hello world`"
    }

    @Test
    fun `circuit 이 열렸을 경우는 실행되지 않는다`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("test")
        circuitBreaker.transitionToOpenState()
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val helloWorldService = CoHelloWorldService()

        assertFailsWith<CallNotPermittedException> {
            circuitBreaker.executeSuspendFunction {
                helloWorldService.returnHelloWorld()
            }
        }

        helloWorldService.invocationCount shouldBeEqualTo 0

        metrics.numberOfBufferedCalls shouldBeEqualTo 0
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 0
        metrics.numberOfNotPermittedCalls shouldBeEqualTo 1
    }

    @Test
    fun `decorate suspend function and return with success`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("test")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val helloWorldService = CoHelloWorldService()

        val function = circuitBreaker.decorateSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        function() shouldBeEqualTo "Hello world"
        helloWorldService.invocationCount shouldBeEqualTo 1

        metrics.numberOfBufferedCalls shouldBeEqualTo 1
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `decorate suspend function and return an exception`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("test")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val helloWorldService = CoHelloWorldService()

        val function = circuitBreaker.decorateSuspendFunction {
            helloWorldService.throwException()
        }

        assertFailsWith<IllegalStateException> {
            function()
        }
        helloWorldService.invocationCount shouldBeEqualTo 1

        metrics.numberOfBufferedCalls shouldBeEqualTo 1
        metrics.numberOfFailedCalls shouldBeEqualTo 1
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 0
    }

    @Test
    fun `decorate suspend function with parameter and return with success`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("test")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val helloWorldService = CoHelloWorldService()

        val function = circuitBreaker.decorateSuspendFunction {
            helloWorldService.returnMessage("Hello world")
        }

        function() shouldBeEqualTo "Hello world"

        helloWorldService.invocationCount shouldBeEqualTo 1

        metrics.numberOfBufferedCalls shouldBeEqualTo 1
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `decorate suspend function with parameter and return an exception`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("test")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val helloWorldService = CoHelloWorldService()

        val function = circuitBreaker.decorateSuspendFunction {
            helloWorldService.throwExceptionWithMessage("error")
        }

        assertFailsWith<IllegalStateException> {
            function()
        }
        helloWorldService.invocationCount shouldBeEqualTo 1

        metrics.numberOfBufferedCalls shouldBeEqualTo 1
        metrics.numberOfFailedCalls shouldBeEqualTo 1
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 0
    }
}
