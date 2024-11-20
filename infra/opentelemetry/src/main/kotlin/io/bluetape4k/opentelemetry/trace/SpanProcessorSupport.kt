package io.bluetape4k.opentelemetry.trace

import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter

fun simpleSpanProcessorOf(exporter: SpanExporter): SpanProcessor {
    return SimpleSpanProcessor.create(exporter)
}

inline fun batchSpanProcess(
    exporter: SpanExporter,
    initializer: BatchSpanProcessorBuilder.() -> Unit,
): BatchSpanProcessor {
    return BatchSpanProcessor.builder(exporter).apply(initializer).build()
}
