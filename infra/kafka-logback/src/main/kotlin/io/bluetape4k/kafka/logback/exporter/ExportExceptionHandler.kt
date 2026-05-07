package io.bluetape4k.logback.kafka.exporter

/**
 * Export 시에 예외가 발생했을 경우, Fallback 처리를 위한 인터페이스
 *
 * ## 동작/계약
 * - exporter가 전송 실패를 감지하면 [handle]을 호출합니다.
 * - 구현체는 fallback 기록/재시도/무시 정책을 자유롭게 정의할 수 있습니다.
 *
 * ```kotlin
 * val handler = ExportExceptionHandler<Any> { _, ex -> if (ex != null) println(ex.message) }
 * // 전송 실패 시 handler가 호출됨
 * ```
 *
 * @param E Event Type
 */
fun interface ExportExceptionHandler<E: Any> {

    /** 전송 실패 이벤트를 처리합니다. */
    fun handle(event: E, throwable: Throwable?)

}
