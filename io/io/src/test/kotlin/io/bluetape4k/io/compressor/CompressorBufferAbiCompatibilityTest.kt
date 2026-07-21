package io.bluetape4k.io.compressor

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.security.MessageDigest

class CompressorBufferAbiCompatibilityTest {

    @Test
    fun `frozen pre-change authority matches its manifest`() {
        val root = "abi/issue-755/pre-change"
        val manifest = javaClass.classLoader.getResourceAsStream("$root/manifest.json").use { input ->
            ObjectMapper().readTree(input.shouldNotBeNull())
        }
        val fixture = javaClass.classLoader.getResourceAsStream("$root/legacy-compressor-fixtures.jar").use { input ->
            input.shouldNotBeNull().readBytes()
        }

        manifest.path("producer").path("commit").asText() shouldBeEqualTo PRE_CHANGE_COMMIT
        manifest.path("producer").path("tree").asText() shouldBeEqualTo PRE_CHANGE_TREE
        manifest.path("baselineJar").path("sha256").asText() shouldBeEqualTo BASELINE_JAR_SHA
        manifest.path("fixtureJar").path("containsCompressorClass").asBoolean().shouldBeFalse()
        fixture.sha256() shouldBeEqualTo manifest.path("fixtureJar").path("sha256").asText()
    }

    @Test
    fun `compressor exposes executable caller-owned buffer defaults`() {
        val compress = Compressor::class.java.getMethod("compress", ByteBuffer::class.java, ByteBuffer::class.java)
        val decompress = Compressor::class.java.getMethod("decompress", ByteBuffer::class.java, ByteBuffer::class.java)

        compress.isDefault.shouldBeTrue()
        decompress.isDefault.shouldBeTrue()
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val PRE_CHANGE_COMMIT = "a065a8e88cf246975660c68df2dd78dfb5b6dc4d"
        const val PRE_CHANGE_TREE = "50cf7789648c0091b6c16de6cf5eb495c26510f8"
        const val BASELINE_JAR_SHA = "34d280b0cb465ffca2a23a2aa57895cc3ba9c08ea18f57c706443b91a0eae6f1"
    }
}
