package io.bluetape4k.opentelemetry.examples.metrics

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.bluetape4k.opentelemetry.common.attributesOf
import io.bluetape4k.utils.Runtimex
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class DoubleGaugeExamples: AbstractOtelTest() {

    companion object: KLogging()

    @ParameterizedTest(name = "use gauge with opentelemetry. name={0}")
    @MethodSource(JUNIT_METHOD_SOURCE_NAME)
    fun `use gauge with opentelemetry`(name: String, otel: OpenTelemetry) {
        log.debug { "Use $name opentelemetry" }

        val sampleMeter = otel.getMeter("example.metrics.$name")

        val gauge = sampleMeter
            .gaugeBuilder("jvm.memory.free")
            .setDescription("Reports JVM free memory.")
            .setUnit("byte")
            .buildWithCallback { result: ObservableDoubleMeasurement ->
                result.record(Runtimex.freeMemory.toDouble(), attributesOf("jvm.memory.free", "sample"))
            }

        Thread.sleep(2000L)
        gauge.close()
    }
}
