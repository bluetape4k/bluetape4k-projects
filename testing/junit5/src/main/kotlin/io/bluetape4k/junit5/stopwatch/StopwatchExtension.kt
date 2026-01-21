package io.bluetape4k.junit5.stopwatch

import io.bluetape4k.junit5.store
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.time.Duration

/**
 * Stopwatch 를 이용하여 테스트 실행 시간을 측정하는 JUnit5 Extension 입니다.
 *
 * ```
 * @StopWatcherTest
 * class TestClass {
 *    ....
 * }
 * ```
 */
class StopwatchExtension(
    private val logger: org.slf4j.Logger = log,
): BeforeTestExecutionCallback, AfterTestExecutionCallback {

    companion object: KLogging()

    override fun beforeTestExecution(context: ExtensionContext) {
        val testMethod = context.requiredTestMethod
        logger.info { "Starting test: [${testMethod.name}]" }
        context.store(StopwatchExtension::class).put(testMethod, System.nanoTime())
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val testMethod = context.requiredTestMethod
        val startNano = context.store(StopwatchExtension::class).get(testMethod, Long::class.java) ?: 0L
        val nanos = Duration.ofNanos(System.nanoTime() - startNano)

        val millis = nanos.toMillis()
        if (millis <= 0L) {
            logger.info { "Completed test: [${testMethod.name}] took $nanos nanos" }
        } else {
            logger.info { "Completed test: [${testMethod.name}] took $millis msecs." }
        }
    }
}
