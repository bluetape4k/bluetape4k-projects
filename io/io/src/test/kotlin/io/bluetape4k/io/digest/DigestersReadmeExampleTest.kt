package io.bluetape4k.io.digest

// DIGESTERS_README_IMPORTS_START
import io.bluetape4k.io.digest.Base64Format
import io.bluetape4k.io.digest.Digesters
// DIGESTERS_README_IMPORTS_END

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test

class DigestersReadmeExampleTest {

    @Test
    fun `README digest examples remain executable and identical`() {
        digestersReadmeExample()

        val sample = readSampleFunctionBody()
        val completeSample = "${readSampleImports()}\n\n$sample"
        readReadmeSample("README.md") shouldBeEqualTo completeSample
        readReadmeSample("README.ko.md") shouldBeEqualTo completeSample
    }

    private fun readSampleFunctionBody(): String {
        val sourcePath = modulePath("src/test/kotlin/io/bluetape4k/io/digest/DigestersReadmeExampleTest.kt")
        val source = Files.readString(sourcePath, StandardCharsets.UTF_8)
        val startMarker = "// DIGESTERS" + "_README_SAMPLE_START"
        val endMarker = "\n// DIGESTERS" + "_README_SAMPLE_END"
        val marker = source.indexOf(startMarker)
        (marker >= 0).shouldBeTrue()
        val bodyStart = source.indexOf('\n', marker) + 1
        val bodyEnd = source.indexOf(endMarker, bodyStart)
        (bodyEnd >= bodyStart).shouldBeTrue()
        return source.substring(bodyStart, bodyEnd)
    }

    private fun readSampleImports(): String {
        val sourcePath = modulePath("src/test/kotlin/io/bluetape4k/io/digest/DigestersReadmeExampleTest.kt")
        val source = Files.readString(sourcePath, StandardCharsets.UTF_8)
        val startMarker = "// DIGESTERS" + "_README_IMPORTS_START"
        val endMarker = "\n// DIGESTERS" + "_README_IMPORTS_END"
        val marker = source.indexOf(startMarker)
        (marker >= 0).shouldBeTrue()
        val bodyStart = source.indexOf('\n', marker) + 1
        val bodyEnd = source.indexOf(endMarker, bodyStart)
        (bodyEnd >= bodyStart).shouldBeTrue()
        return source.substring(bodyStart, bodyEnd)
    }

    private fun readReadmeSample(fileName: String): String {
        val readme = Files.readString(modulePath(fileName), StandardCharsets.UTF_8)
        val startMarker = "<!-- DIGESTERS_README_SAMPLE_START -->"
        val endMarker = "<!-- DIGESTERS_README_SAMPLE_END -->"
        val marker = readme.indexOf(startMarker)
        (marker >= 0).shouldBeTrue()
        val fenceStart = readme.indexOf("```kotlin\n", marker)
        (fenceStart >= 0).shouldBeTrue()
        val bodyStart = fenceStart + "```kotlin\n".length
        val bodyEnd = readme.indexOf("\n```", bodyStart)
        (bodyEnd >= bodyStart).shouldBeTrue()
        val end = readme.indexOf(endMarker, bodyEnd)
        (end >= 0).shouldBeTrue()
        return readme.substring(bodyStart, bodyEnd)
    }

    private fun modulePath(relativePath: String): Path =
        sequenceOf(Path.of(relativePath), Path.of("io/io").resolve(relativePath))
            .firstOrNull(Files::isRegularFile)
            ?: error("Could not find $relativePath from ${Path.of("").toAbsolutePath()}")
}

// DIGESTERS_README_SAMPLE_START
fun digestersReadmeExample() {
    val fingerprint = "commerce:reservation:42"
    val input = fingerprint.toByteArray(Charsets.UTF_8)
    val rawDigest = Digesters.SHA256.digest(input)
    val textDigest = Digesters.SHA256.digestUtf8(fingerprint)
    val wireDigest = Digesters.SHA256.digestBase64Utf8(fingerprint, Base64Format.URL_SAFE_UNPADDED)
    val sessionDigest = Digesters.SHA256.newSession()
        .update("commerce:".toByteArray(Charsets.UTF_8))
        .update("reservation:42".toByteArray(Charsets.UTF_8))
        .digest()

    check(rawDigest.contentEquals(textDigest))
    check(rawDigest.contentEquals(sessionDigest))
    check(Digesters.SHA256.matchesBase64(input, wireDigest, Base64Format.URL_SAFE_UNPADDED))
}
// DIGESTERS_README_SAMPLE_END
