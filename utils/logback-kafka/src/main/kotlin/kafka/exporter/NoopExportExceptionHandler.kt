package io.bluetape4k.logback.kafka.exporter

class NoopExportExceptionHandler: ExportExceptionHandler<Any> {

    override fun handle(event: Any, throwable: Throwable?) {
        // Nothing to do
    }
}
