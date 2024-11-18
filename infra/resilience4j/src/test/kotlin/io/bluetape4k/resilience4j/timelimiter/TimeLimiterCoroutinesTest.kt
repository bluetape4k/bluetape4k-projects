package io.bluetape4k.resilience4j.timelimiter

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.resilience4j.CoHelloWorldService
import io.github.resilience4j.kotlin.timelimiter.decorateSuspendFunction
import io.github.resilience4j.kotlin.timelimiter.executeSuspendFunction
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeoutException
import kotlin.test.assertFailsWith

class TimeLimiterCoroutinesTest {

    companion object: KLogging()

    @Test
    fun `suspend 함수를 실행합니다`() = runSuspendTest {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = CoHelloWorldService()

        val result = timelimiter.executeSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        result shouldBeEqualTo "Hello world"
        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `예외가 발생하는 메소드도 실행합니다`() = runSuspendTest {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = CoHelloWorldService()

        assertFailsWith<IllegalStateException> {
            timelimiter.executeSuspendFunction {
                helloWorldService.throwException()
            }
        }
        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `time out 된 작업은 cancel 합니다`() = runSuspendTest {
        val config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(10))
            .build()
        val timelimiter = TimeLimiter.of(config)

        val helloWorldService = CoHelloWorldService()

        assertFailsWith<TimeoutException> {
            timelimiter.executeSuspendFunction {
                helloWorldService.await()
            }
        }

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `timelimiter로 decorate 하기`() = runSuspendTest {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = CoHelloWorldService()

        val function = timelimiter.decorateSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        function() shouldBeEqualTo "Hello world"
        helloWorldService.invocationCount shouldBeEqualTo 1
    }
}
