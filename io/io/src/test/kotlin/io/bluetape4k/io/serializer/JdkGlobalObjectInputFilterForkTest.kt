package io.bluetape4k.io.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class JdkGlobalObjectInputFilterForkTest {

    @Test
    fun `JVM startup global filter allows ByteArray and bounded direct decode equally`() {
        runFixture(
            mode = "allow",
            filter = "io.bluetape4k.**;java.lang.*;!*",
            expectedMarker = "GLOBAL_FILTER_ALLOW_PASS",
        )
    }

    @Test
    fun `JVM startup global filter rejects ByteArray and bounded direct decode equally`() {
        runFixture(
            mode = "reject",
            filter = "!io.bluetape4k.io.serializer.JdkGlobalObjectInputFilterFixture${'$'}Payload;" +
                    "io.bluetape4k.**;java.lang.*;!*",
            expectedMarker = "GLOBAL_FILTER_REJECT_PASS",
        )
    }

    private fun runFixture(mode: String, filter: String, expectedMarker: String) {
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val process = ProcessBuilder(
            java,
            "-Djdk.serialFilter=$filter",
            "-cp",
            forkClasspath(),
            JdkGlobalObjectInputFilterFixture::class.java.name,
            mode,
        ).redirectErrorStream(true).start()

        val finished = process.waitFor(30, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
        }
        finished.shouldBeTrue()
        val output = process.inputStream.bufferedReader().use { it.readText() }

        process.exitValue() shouldBeEqualTo 0
        output.contains(expectedMarker).shouldBeTrue()
    }

    private fun forkClasspath(): String {
        val loaderEntries = generateSequence(Thread.currentThread().contextClassLoader) { it.parent }
            .filterIsInstance<URLClassLoader>()
            .flatMap { it.urLs.asSequence() }
            .filter { it.protocol == "file" }
            .map { Path.of(it.toURI()).toString() }
        val systemEntries = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .asSequence()

        return (loaderEntries + systemEntries)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(File.pathSeparator)
    }
}
