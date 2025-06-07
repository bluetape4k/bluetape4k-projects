package io.bluetape4k.opentelemetry.examples.metrics

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.bluetape4k.opentelemetry.common.attributesOf
import io.bluetape4k.opentelemetry.common.toAttributeKey
import io.bluetape4k.opentelemetry.coroutines.useSuspendSpan
import io.bluetape4k.opentelemetry.trace.useSpan
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path

/**
 * Long 수형의 Counter 를 정의하고 사용하는 방법
 */
class LongCounterExamples: AbstractOtelTest() {

    companion object: KLoggingChannel() {
        private val homeDir: File = Path.of(".").toAbsolutePath().toFile()
        private val rootDirectoryKey = "root dir".toAttributeKey()
        private val homeDirectoryAttributes = attributesOf(rootDirectoryKey, homeDir.name)
    }

    private lateinit var directoryCounter: LongCounter

    @ParameterizedTest(name = "measure directory count. name={0}")
    @MethodSource(JUNIT_METHOD_SOURCE_NAME)
    fun `measure directory count`(name: String, otel: OpenTelemetry) {
        log.debug { "use $name OpenTelemetry" }
        val tracer = otel.getTracer("examples.metrics", "1.25.0")
        val sampleMeter = otel.getMeter("examples.metrics")

        directoryCounter = sampleMeter
            .counterBuilder("directory_search_count")
            .setDescription("파일 찾기 위해 접근한 디렉토리 개수")
            .setUnit("ea")
            .build()

        tracer.spanBuilder("workflow")
            .setSpanKind(SpanKind.INTERNAL)
            .useSpan { span ->
                try {
                    directoryCounter.add(1, homeDirectoryAttributes)
                    findFile("logback-test.xml", homeDir, directoryCounter)
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Error while finding file")
                }
            }
    }

    /**
     * 특정 폴더 아래의 모든 파일들 중에 파일명이 [name] 인 놈이 있는지 탐색합니다.
     * 탐색한 Directory의 수를 측정하기 위해 [directoryCounter] 를 사용합니다.
     * 만약 찾았다면 해당 파일의 부모 디렉토리를 출력합니다.
     *
     * @param name
     * @param directory
     */
    private fun findFile(name: String, directory: File, counter: LongCounter) {
        log.debug { "Currently looking at ${directory.absolutePath}" }
        directory.listFiles()
            ?.forEach { file ->
                if (file.isDirectory) {
                    counter.add(1, homeDirectoryAttributes)
                    Thread.sleep(50L)
                    findFile(name, file, counter)
                } else if (name.equals(file.name, ignoreCase = true)) {
                    println(file.parentFile)
                }
            }
    }

    @ParameterizedTest(name = "measure directory count in coroutines. name={0}")
    @MethodSource(JUNIT_METHOD_SOURCE_NAME)
    fun `measure directory count in coroutines`(name: String, otel: OpenTelemetry) = runSuspendIO {
        log.debug { "use $name OpenTelemetry" }
        val tracer = otel.getTracer("examples.metrics", "1.25.0")
        val sampleMeter = otel.getMeter("examples.metrics")

        directoryCounter = sampleMeter
            .counterBuilder("directory_search_count_in_coroutines")
            .setDescription("Coroutine 환경에서 파일 찾기 위해 접근한 디렉토리 개수")
            .setUnit("ea")
            .build()

        // Coroutines 환경에서 Span 을 생성하고 사용하는 방법
        tracer.spanBuilder("coroutine-workflow")
            .setSpanKind(SpanKind.INTERNAL)
            .useSuspendSpan(Dispatchers.IO) { span ->
                try {
                    directoryCounter.add(1, homeDirectoryAttributes)
                    findFileSuspending("logback-test.xml", homeDir, directoryCounter)
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Error while finding file")
                }
            }
    }

    private suspend fun findFileSuspending(name: String, directory: File, counter: LongCounter) {
        log.debug { "Currently looking at ${directory.absolutePath}" }
        directory.listFiles()
            ?.forEach { file ->
                if (file.isDirectory) {
                    counter.add(1, homeDirectoryAttributes)
                    delay(50L)
                    findFileSuspending(name, file, counter)
                } else if (name.equals(file.name, ignoreCase = true)) {
                    println(file.parentFile)
                }
            }
    }
}
