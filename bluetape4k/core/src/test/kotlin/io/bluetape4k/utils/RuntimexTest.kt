package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class RuntimexTest {

    companion object: KLogging()

    @Test
    fun `컴퓨터 CPU Core 수를 얻습니다`() {
        log.trace { "CPU Core = ${Runtimex.availableProcessors}" }
        Runtimex.availableProcessors shouldBeGreaterThan 1
    }

    @Test
    fun `가용 메모리 얻기`() {
        log.trace { "Available Memory = ${Runtimex.availableMemory} bytes" }
        Runtimex.availableMemory shouldBeGreaterThan 0
    }

    @Test
    fun `가용 메모리 Percentage 계산`() {
        log.trace { "Available Memory Percentage = ${Runtimex.availableMemoryPercent} %" }
        Runtimex.availableMemoryPercent shouldBeGreaterThan 0.0
    }

    @Test
    fun `Free 메모리 얻기`() {
        log.trace { "Free Memory = ${Runtimex.freeMemory} bytes" }
        Runtimex.freeMemory shouldBeGreaterOrEqualTo 0
    }

    @Test
    fun `Free 메모리 Percentage 계산`() {
        log.trace { "Free Memory Percentage = ${Runtimex.freeMemoryPercent} %" }
        Runtimex.freeMemoryPercent shouldBeGreaterOrEqualTo 0.0
    }


    @Test
    fun `add shutdown hook`() {
        Runtimex.addShutdownHook {
            log.info { "JVM Shutdown gracefully." }
        }
    }

    @Test
    fun `run process and capture output`() {
        val process = ProcessBuilder("bash", "-c", "ls").start()
        val result = Runtimex.run(process)

        log.trace { "process result=$result" }
        result.out shouldContain "build.gradle.kts"
    }

    @Test
    fun `run process captures stderr and non-zero exit code`() {
        val process = ProcessBuilder("bash", "-c", "echo 'failure message' 1>&2; exit 7").start()
        val result = Runtimex.run(process)

        result.exitCode shouldBeEqualTo 7
        result.out shouldContain "failure message"
        result.out shouldContain "err>failure message"
    }

    @Test
    fun `class location returns code source`() {
        val location = Runtimex.classLocation(Runtimex::class.java)
        location.shouldNotBeNull()
    }
}
