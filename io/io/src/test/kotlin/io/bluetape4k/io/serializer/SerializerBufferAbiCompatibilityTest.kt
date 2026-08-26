package io.bluetape4k.io.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.assertions.expectThat
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.security.MessageDigest

class SerializerBufferAbiCompatibilityTest {

    @Test
    fun `frozen pre-change authority matches its manifest`() {
        val root = "compat/issue-754/pre-change"
        val manifest = javaClass.classLoader.getResourceAsStream("$root/manifest.json").use { input ->
            ObjectMapper().readTree(requireNotNull(input))
        }

        manifest.path("producer").path("commit").asText() shouldBeEqualTo PRE_CHANGE_COMMIT
        manifest.path("producer").path("tree").asText() shouldBeEqualTo PRE_CHANGE_TREE
        manifest.path("jars").size() shouldBeEqualTo 3
        manifest.path("fixtures").size() shouldBeEqualTo 5

        manifest.path("fixtures").forEach { fixture ->
            val path = fixture.path("path").asText()
            val bytes = javaClass.classLoader.getResourceAsStream("$root/$path").use { input ->
                requireNotNull(input) { "Missing frozen fixture: $path" }.readBytes()
            }
            expectThat(fixture.path("size").asInt(), path) { bytes.size }
            expectThat(fixture.path("sha256").asText(), path) { bytes.sha256() }
        }
    }

    @Test
    fun `binary interface exposes executable buffer defaults`() {
        val serializeTo = BinarySerializer::class.java.getMethod(
            "serializeTo",
            Any::class.java,
            ByteBuffer::class.java,
        )
        val deserializeFrom = BinarySerializer::class.java.getMethod(
            "deserializeFrom",
            ByteBuffer::class.java,
        )

        serializeTo.isDefault.shouldBeTrue()
        deserializeFrom.isDefault.shouldBeTrue()
    }

    @Test
    fun `legacy binary ByteBuffer extension keeps its static JVM symbol`() {
        val extensionHolder = Class.forName("io.bluetape4k.io.serializer.BinarySerializerSupportKt")
        extensionHolder.getMethod(
            "deserialize",
            BinarySerializer::class.java,
            ByteBuffer::class.java,
        )
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val PRE_CHANGE_COMMIT = "90b267871e9154f242e6de7ee9fd0539f83e509e"
        const val PRE_CHANGE_TREE = "f40ccbda16ddf56d4b7770c01e9b0b2cb07cedba"
    }
}
