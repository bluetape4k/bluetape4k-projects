package io.bluetape4k.opentelemetry.examples.javaagent

import io.bluetape4k.opentelemetry.metrics.loggingMetricExporterOf
import io.bluetape4k.opentelemetry.metrics.periodicMetricReader
import io.bluetape4k.opentelemetry.metrics.sdkMeterProvider
import io.bluetape4k.opentelemetry.openTelemetrySdk
import io.bluetape4k.opentelemetry.trace.loggingSpanExporterOf
import io.bluetape4k.opentelemetry.trace.sdkTracerProvider
import io.bluetape4k.opentelemetry.trace.simpleSpanProcessorOf
import io.opentelemetry.api.OpenTelemetry
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Duration

@SpringBootApplication
class OtelSpringBootApp {

    @Bean
    fun openTelemetry(): OpenTelemetry {
        // Tracer provider configured to export spans with SimpleSpanProcessor using the logging exporter.
        val tracerProvider = sdkTracerProvider {
            addSpanProcessor(simpleSpanProcessorOf(loggingSpanExporterOf()))
        }
        val meterProvider = sdkMeterProvider {
            // Create an instance of PeriodicMetricReader and configure it to export via the logging exporter
            val metricReader = periodicMetricReader(loggingMetricExporterOf()) {
                setInterval(Duration.ofMillis(1000L))
            }
            registerMetricReader(metricReader)
        }

        // javaagent 는 GlobalOpenTelemetry.get() 을 사용하므로, 여기서는 직접 생성해서 사용합니다.
        return openTelemetrySdk {
            setTracerProvider(tracerProvider)
            setMeterProvider(meterProvider)
        }
    }
}

fun main(vararg args: String) {
    runApplication<OtelSpringBootApp>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
