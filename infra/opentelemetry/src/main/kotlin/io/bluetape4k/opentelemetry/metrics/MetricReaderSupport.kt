package io.bluetape4k.opentelemetry.metrics

import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader


/**
 * [InMemoryMetricReader] 를 생성합니다.
 *
 * [io.opentelemetry.sdk.metrics.data.AggregationTemporality.CUMULATIVE]
 */
fun inMemoryMetricReaderOf(): InMemoryMetricReader = InMemoryMetricReader.create()

/**
 * Metric의 변화량을 읽어오는 [InMemoryMetricReader] 를 생성합니다.
 *
 * @see [InMemoryMetricReader.createDelta]
 * @see [io.opentelemetry.sdk.metrics.data.AggregationTemporality.DELTA]
 *
 */
fun inMemoryMetricReaderDeltaOf(): InMemoryMetricReader = InMemoryMetricReader.createDelta()

/**
 * 주기적으로 Metric 정보를 읽어서 [exporter] 로 내보내는 [io.opentelemetry.sdk.metrics.export.MetricReader] 를 생성한다.
 *
 * @param exporter Metric 정보를 내보내는 [MetricExporter]
 * @param initializer [PeriodicMetricReaderBuilder] 를 설정하는 람다입니다.
 * @return [PeriodicMetricReader] instance
 */
fun periodicMetricReader(
    exporter: MetricExporter,
    initializer: PeriodicMetricReaderBuilder.() -> Unit,
): PeriodicMetricReader {
    return PeriodicMetricReader.builder(exporter).apply(initializer).build()
}
