package io.bluetape4k.io.serializer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.io.Serializable
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Duration

/**
 * Fory `COMPATIBLE` 모드의 Kotlin metadata round-trip과 1.6.0 fixture 읽기를 고정한다.
 *
 * 이 테스트는 `SCHEMA_CONSISTENT` FastFory의 wire 호환성을 약속하지 않는다.
 */
class ForyWireCompatibilityTest {

    @Test
    fun `Fory 1_7_1은 Kotlin metadata payload를 roundtrip한다`() {
        val original = ForyWireCompatibilityFixtures.sample()

        val bytes = BinarySerializers.Fory.serialize(original)
        bytes.shouldNotBeEmpty()

        BinarySerializers.Fory.deserialize<ForyWireCompatibilityPayload>(bytes) shouldBeEqualTo original
    }

    @Test
    fun `Fory 1_6_0 fixture를 1_7_1이 읽는다`() {
        val manifest = readManifest()
        val bytes = readFixture(manifest)

        manifest.path("serializer").path("foryVersion").asText() shouldBeEqualTo FORY_OLD_VERSION
        manifest.path("serializer").path("foryKotlinVersion").asText() shouldBeEqualTo FORY_OLD_VERSION
        manifest.path("fixtureObject").path("class").asText() shouldBeEqualTo
            ForyWireCompatibilityPayload::class.java.name
        bytes.size shouldBeEqualTo manifest.path("fixture").path("size").asInt()
        bytes.sha256() shouldBeEqualTo manifest.path("fixture").path("sha256").asText()

        val restored = BinarySerializers.Fory.deserialize<ForyWireCompatibilityPayload>(bytes)
        restored shouldBeEqualTo ForyWireCompatibilityFixtures.sample()
    }

    @Test
    fun `malformed 입력은 BinarySerializationException으로 bounded failure한다`() {
        assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            assertFailsWith<BinarySerializationException> {
                BinarySerializers.Fory.deserialize<ForyWireCompatibilityPayload>(byteArrayOf(0x01, 0x02, 0x03))
            }
        }
    }

    @Test
    fun `truncated fixture는 ByteBuffer 경계 안에서 실패한다`() {
        val fixture = readFixture(readManifest())
        val truncated = fixture.copyOf(fixture.size / 2)

        assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            assertFailsWith<BinarySerializationException> {
                BinarySerializers.Fory.deserializeFrom<ForyWireCompatibilityPayload>(ByteBuffer.wrap(truncated))
            }
        }
    }

    private fun readManifest(): JsonNode =
        javaClass.classLoader.getResourceAsStream("$FIXTURE_ROOT/manifest.json").use { input ->
            ObjectMapper().readTree(requireNotNull(input) { "Missing Fory compatibility manifest" })
        }

    private fun readFixture(manifest: JsonNode): ByteArray {
        val path = manifest.path("fixture").path("path").asText()
        return javaClass.classLoader.getResourceAsStream("$FIXTURE_ROOT/$path").use { input ->
            requireNotNull(input) { "Missing Fory compatibility fixture: $path" }.readBytes()
        }
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val FIXTURE_ROOT = "compat/issue-1639/fory-1.6.0"
        const val FORY_OLD_VERSION = "1.6.0"

        @JvmStatic
        @BeforeAll
        fun warmUpForySerializer() {
            val sample = ForyWireCompatibilityFixtures.sample()
            val bytes = BinarySerializers.Fory.serialize(sample)
            BinarySerializers.Fory.deserialize<ForyWireCompatibilityPayload>(bytes) shouldBeEqualTo sample
        }
    }
}

@JvmInline
value class ForyWireOwnerId(val value: String)

data class ForyWireCompatibilityMetadata(
    val source: String = "issue-1639",
    val note: String? = null,
) : Serializable {
    private companion object {
        const val serialVersionUID: Long = 1L
    }
}

data class ForyWireCompatibilityPayload(
    val id: Long,
    val title: String = "fory-wire",
    val description: String? = null,
    val tags: List<String> = listOf("kotlin", "metadata", "redis"),
    val owner: ForyWireOwnerId = ForyWireOwnerId("owner-1639"),
    val metadata: ForyWireCompatibilityMetadata = ForyWireCompatibilityMetadata(),
) : Serializable {
    private companion object {
        const val serialVersionUID: Long = 1L
    }
}

object ForyWireCompatibilityFixtures {

    @JvmStatic
    fun sample(): ForyWireCompatibilityPayload = ForyWireCompatibilityPayload(id = 1639L)
}
