package io.bluetape4k.io.compressor

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.security.MessageDigest

class CompressorBufferAbiCompatibilityTest {

    @Test
    fun `frozen pre-change authority matches its manifest`() {
        val root = "abi/issue-755/pre-change"
        val manifest = javaClass.classLoader.getResourceAsStream("$root/manifest.json").use { input ->
            ObjectMapper().readTree(requireNotNull(input))
        }
        val fixture = javaClass.classLoader.getResourceAsStream("$root/legacy-compressor-fixtures.jar").use { input ->
            requireNotNull(input).readBytes()
        }

        assertEquals(PRE_CHANGE_COMMIT, manifest.path("producer").path("commit").asText())
        assertEquals(PRE_CHANGE_TREE, manifest.path("producer").path("tree").asText())
        assertEquals(BASELINE_JAR_SHA, manifest.path("baselineJar").path("sha256").asText())
        assertFalse(manifest.path("fixtureJar").path("containsCompressorClass").asBoolean())
        assertEquals(manifest.path("fixtureJar").path("sha256").asText(), fixture.sha256())
    }

    @Test
    fun `compressor exposes executable caller-owned buffer defaults`() {
        val compress = Compressor::class.java.getMethod("compress", ByteBuffer::class.java, ByteBuffer::class.java)
        val decompress = Compressor::class.java.getMethod("decompress", ByteBuffer::class.java, ByteBuffer::class.java)

        assertTrue(compress.isDefault, "compress(ByteBuffer, ByteBuffer) must be a JVM default")
        assertTrue(decompress.isDefault, "decompress(ByteBuffer, ByteBuffer) must be a JVM default")
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
