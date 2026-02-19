package io.bluetape4k.opentelemetry.trace

import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter

/**
 * [SimpleSpanProcessor]를 생성합니다.
 *
 * @param exporter 내보낼 [SpanExporter]
 * @return [SpanProcessor] 인스턴스
 */
fun simpleSpanProcessorOf(exporter: SpanExporter): SpanProcessor {
    return SimpleSpanProcessor.create(exporter)
}

/**
 * [BatchSpanProcessor]를 생성합니다.
 *
 * @param exporter 내보낼 [SpanExporter]
 * @param builder [BatchSpanProcessorBuilder]를 설정하는 람다
 * @return [BatchSpanProcessor] 인스턴스
 */
inline fun batchSpanProcess(
    exporter: SpanExporter,
    @BuilderInference builder: BatchSpanProcessorBuilder.() -> Unit,
): BatchSpanProcessor {
    return BatchSpanProcessor.builder(exporter).apply(builder).build()
}
