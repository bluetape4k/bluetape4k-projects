package io.bluetape4k.junit5.stopwatch

import io.bluetape4k.junit5.store
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.time.Duration

/**
 * 테스트 메서드 시작/종료 시각을 기록해 실행 시간을 로그로 남기는 JUnit5 확장입니다.
 *
 * ## 동작/계약
 * - 시작 시 `System.nanoTime()` 값을 extension store에 저장합니다.
 * - 종료 시 저장값과 현재 시각 차이를 계산해 ns 또는 ms 단위로 로그를 출력합니다.
 * - store에 시작값이 없으면 `0L`을 사용해 매우 큰 duration이 기록될 수 있습니다.
 *
 * ```kotlin
 * @StopwatchTest
 * class SlowTest
 * // 로그에 "Completed test: [...] took ... msecs." 출력
 * ```
 */
class StopwatchExtension(
    private val logger: org.slf4j.Logger = log,
): BeforeTestExecutionCallback, AfterTestExecutionCallback {

    companion object: KLogging()

    /**
     * 테스트 실행 시작 시각을 기록합니다.
     */
    override fun beforeTestExecution(context: ExtensionContext) {
        val testMethod = context.requiredTestMethod
        logger.info { "Starting test: [${testMethod.name}]" }
        context.store(StopwatchExtension::class).put(testMethod, System.nanoTime())
    }

    /**
     * 테스트 실행 종료 후 경과 시간을 계산해 로그로 출력합니다.
     */
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
