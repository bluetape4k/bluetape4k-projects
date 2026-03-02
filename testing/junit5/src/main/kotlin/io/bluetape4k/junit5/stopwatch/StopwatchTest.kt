package io.bluetape4k.junit5.stopwatch

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트 실행 시간을 측정하고 로그로 출력하는 확장을 활성화합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `@Test`와 `@ExtendWith(StopwatchExtension::class)`를 함께 적용합니다.
 * - 클래스/파일/함수 수준에 적용할 수 있습니다.
 * - 실제 측정/출력 포맷은 [StopwatchExtension] 구현을 따릅니다.
 *
 * ```kotlin
 * @StopwatchTest
 * fun `성능 측정 대상 테스트`() {
 *   // 실행 시간 로그가 남는다.
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
