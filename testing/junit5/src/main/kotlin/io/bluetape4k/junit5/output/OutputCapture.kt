package io.bluetape4k.junit5.output

import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트 중 `System.out`/`System.err` 출력을 캡처하는 확장을 활성화합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `@ExtendWith(OutputCaptureExtension::class)`를 적용합니다.
 * - 테스트 메서드 파라미터로 [OutputCapturer]를 주입받아 출력 내용을 검증할 수 있습니다.
 * - 클래스/파일/함수 수준 적용을 지원합니다.
 *
 * ```kotlin
 * @OutputCapture
 * fun `표준 출력 검증`(capturer: OutputCapturer) {
 *   println("hello")
 *   // capturer.capture().contains("hello") == true
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
@ExtendWith(OutputCaptureExtension::class)
annotation class OutputCapture
