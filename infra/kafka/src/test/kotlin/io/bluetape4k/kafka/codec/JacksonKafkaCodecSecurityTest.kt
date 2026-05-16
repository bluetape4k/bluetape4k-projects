package io.bluetape4k.kafka.codec

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Test

/**
 * Security tests for [JacksonKafkaCodec] type allowlist enforcement.
 *
 * Verifies that:
 * - Untrusted class names in the `VALUE_TYPE_KEY` header are rejected by default (empty allowedTypePackages)
 * - Explicitly allowed packages are deserialized correctly
 * - [AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE] opt-in restores legacy allow-all behavior
 */
class JacksonKafkaCodecSecurityTest {

    companion object: KLogging()

    private data class TrustedDto(val value: String)

    @Test
    fun `untrusted header class is rejected when allowedTypePackages is empty`() {
        val codec = JacksonKafkaCodec() // default: allowedTypePackages = emptySet()

        val writingCodec = JacksonKafkaCodec(
            allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE
        )
        val headers = RecordHeaders()
        val dto = TrustedDto("hello")
        val bytes = writingCodec.serialize("topic", headers, dto)
        bytes.shouldNotBeNull()

        // empty allowedTypePackages → rejected → null (poison-pill)
        val result = codec.deserialize("topic", headers, bytes)
        result.shouldBeNull()
    }

    @Test
    fun `allowed package is deserialized correctly`() {
        val codec = JacksonKafkaCodec(
            allowedTypePackages = setOf("io.bluetape4k.kafka.codec")
        )

        val headers = RecordHeaders()
        val dto = TrustedDto("world")
        val bytes = codec.serialize("topic", headers, dto)
        bytes.shouldNotBeNull()

        val result = codec.deserialize("topic", headers, bytes) as TrustedDto
        result.shouldNotBeNull()
        result.value shouldBeEqualTo "world"
    }

    @Test
    fun `ALLOW_ALL_TYPES_UNSAFE opt-in restores legacy behavior`() {
        val codec = JacksonKafkaCodec(
            allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE
        )

        val headers = RecordHeaders()
        val dto = TrustedDto("unsafe-but-intentional")
        val bytes = codec.serialize("topic", headers, dto)
        bytes.shouldNotBeNull()

        val result = codec.deserialize("topic", headers, bytes) as TrustedDto
        result.shouldNotBeNull()
        result.value shouldBeEqualTo "unsafe-but-intentional"
    }

    @Test
    fun `class outside allowedTypePackages is rejected`() {
        val writingCodec = JacksonKafkaCodec(
            allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE
        )
        val readingCodec = JacksonKafkaCodec(
            allowedTypePackages = setOf("com.example.trusted")
        )

        val headers = RecordHeaders()
        val dto = TrustedDto("blocked")
        val bytes = writingCodec.serialize("topic", headers, dto)
        bytes.shouldNotBeNull()

        // io.bluetape4k.kafka.codec not in com.example.trusted → rejected → null
        val result = readingCodec.deserialize("topic", headers, bytes)
        result.shouldBeNull()
    }
}
