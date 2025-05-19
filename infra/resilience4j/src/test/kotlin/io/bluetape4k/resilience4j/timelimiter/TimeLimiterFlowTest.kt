package io.bluetape4k.resilience4j.timelimiter

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.resilience4j.CoHelloWorldService
import io.github.resilience4j.kotlin.timelimiter.timeLimiter
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeoutException
import kotlin.test.assertFailsWith

class TimeLimiterFlowTest {

    companion object: KLoggingChannel()

    @Test
    fun `실행이 성공하는 메소드를 flow로 수행한다`() = runSuspendTest {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = CoHelloWorldService()
        val results = mutableListOf<String>()

        flow {
            repeat(3) {
                emit(helloWorldService.returnHelloWorld() + it)
            }
        }
            .timeLimiter(timelimiter)
            .toList(results)

        repeat(3) {
            results[it] shouldBeEqualTo "Hello world$it"
        }

        results.size shouldBeEqualTo 3
        helloWorldService.invocationCount shouldBeEqualTo 3
    }

    @Test
    fun `예외를 일으키는 메소드도 flow로 실행됩니다`() = runSuspendTest {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = CoHelloWorldService()
        val results = mutableListOf<String>()

        assertFailsWith<IllegalStateException> {
            flow<String> {
                helloWorldService.throwException()
            }
                .timeLimiter(timelimiter)
                .toList(results)
        }
        results.shouldBeEmpty()
        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `timeout 시에는 flow가 취소됩니다`() = runSuspendTest {
        val config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(10))
            .build()
        val timelimiter = TimeLimiter.of(config)
        val helloWorldService = CoHelloWorldService()
        val results = mutableListOf<String>()

        assertFailsWith<TimeoutException> {
            flow<String> {
                helloWorldService.await()
            }
                .timeLimiter(timelimiter)
                .toList()
        }

        results.size shouldBeEqualTo 0
        helloWorldService.invocationCount shouldBeEqualTo 1
    }
}
