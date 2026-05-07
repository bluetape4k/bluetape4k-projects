package io.bluetape4k.kafka.logback.exporter

/**
 * 전송 예외를 무시하는 no-op 핸들러입니다.
 *
 * ## 동작/계약
 * - [handle] 호출 시 아무 동작도 수행하지 않습니다.
 */
class NoopExportExceptionHandler: io.bluetape4k.kafka.logback.exporter.ExportExceptionHandler<Any> {

    override fun handle(event: Any, throwable: Throwable?) {
        // Nothing to do
    }
}
