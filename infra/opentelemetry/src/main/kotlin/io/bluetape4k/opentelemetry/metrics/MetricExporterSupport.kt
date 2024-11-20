package io.bluetape4k.opentelemetry.metrics

import io.opentelemetry.exporter.logging.LoggingMetricExporter
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter

/**
 * In Memory 에 metrics 측정 값을 내보내는 [InMemoryMetricExporter] 를 생성한다.
 *
 * @param aggregationTemporality Meter 집계 방식 (default: [AggregationTemporality.CUMULATIVE])
 * @return [InMemoryMetricExporter] instance
 */
fun inMemoryMetricExporterOf(
    aggregationTemporality: AggregationTemporality = AggregationTemporality.CUMULATIVE,
): InMemoryMetricExporter {
    return InMemoryMetricExporter.create(aggregationTemporality)
}

/**
 * Logger에 metrics 측정 값을 내보내는 [LoggingMetricExporter] 를 생성한다.
 *
 * @param aggregationTemporality Meter 집계 방식 (default: [AggregationTemporality.CUMULATIVE])
 * @return [LoggingMetricExporter] instance
 */
fun loggingMetricExporterOf(
    aggregationTemporality: AggregationTemporality = AggregationTemporality.CUMULATIVE,
): LoggingMetricExporter {
    return LoggingMetricExporter.create(aggregationTemporality)
}
