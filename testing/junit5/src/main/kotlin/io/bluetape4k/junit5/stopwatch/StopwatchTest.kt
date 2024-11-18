package io.bluetape4k.junit5.stopwatch

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트 실행 시간을 측정하도록 하는 Annotation입니다.
 *
 * ```
 * @StopwatchTest
 * fun `test name`() {
 *     // ...
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION
)
@MustBeDocumented
@Repeatable
@Test
@ExtendWith(StopwatchExtension::class)
annotation class StopwatchTest
