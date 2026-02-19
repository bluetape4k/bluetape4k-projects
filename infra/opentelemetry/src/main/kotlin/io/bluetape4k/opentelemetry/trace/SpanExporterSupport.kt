package io.bluetape4k.opentelemetry.trace

import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

/**
 * [LoggingSpanExporter]를 생성합니다.
 */
fun loggingSpanExporterOf(): LoggingSpanExporter = LoggingSpanExporter.create()

/**
 * 모든 내보내기를 순서대로 [exporters]에 위임하는 [SpanExporter]를 반환합니다.
 *
 * [io.opentelemetry.sdk.trace.export.SimpleSpanProcessor] 또는
 * [io.opentelemetry.sdk.trace.export.BatchSpanProcessor]와 같은 동일한
 * [io.opentelemetry.sdk.trace.SpanProcessor]를 사용하여 여러 백엔드로 내보내는 데 사용할 수 있습니다.
 */
fun spanExportOf(vararg exporters: SpanExporter): SpanExporter =
    SpanExporter.composite(*exporters)

/**
 * 지정된 [SpanData]들을 내보냅니다.
 *
 * @param spanDatas 내보낼 [SpanData] 목록
 * @return 내보내기 결과
 */
fun SpanExporter.export(vararg spanDatas: SpanData): CompletableResultCode {
    return export(spanDatas.asList())
}
