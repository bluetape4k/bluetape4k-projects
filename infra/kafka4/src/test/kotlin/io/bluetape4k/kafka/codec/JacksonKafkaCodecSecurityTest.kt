package io.bluetape4k.kafka.codec

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
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

    @Test
    fun `rejected malicious type header is absent from every WARN event`() {
        val maliciousType = "evil.Type\r\n\t\u0000\u0001\u2028\u2029" + "X".repeat(512) + "TYPE-TAIL"
        val maliciousAllowlist = "trusted\r\n\t\u0002\u2028\u2029" + "Y".repeat(128) + "ALLOWLIST-TAIL"
        val codec = JacksonKafkaCodec(allowedTypePackages = setOf(maliciousAllowlist))
        val headers = RecordHeaders().add(
            AbstractKafkaCodec.VALUE_TYPE_KEY,
            maliciousType.toByteArray(Charsets.UTF_8),
        )
        val logger = AbstractKafkaCodec.log as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            codec.deserialize("test-topic", headers, byteArrayOf(1)).shouldBeNull()

            val events = appender.list
            events.size shouldBeEqualTo 2
            events.all { it.level == Level.WARN } shouldBeEqualTo true
            events.all { it.throwableProxy == null } shouldBeEqualTo true
            events.all { event ->
                val message = event.formattedMessage
                message.length <= 1600 &&
                    message.none(Char::isISOControl) &&
                    !message.contains('\u2028') &&
                    !message.contains('\u2029') &&
                    !message.contains(maliciousType) &&
                    !message.contains("TYPE-TAIL") &&
                    !message.contains(maliciousAllowlist) &&
                    !message.contains("ALLOWLIST-TAIL")
            } shouldBeEqualTo true
            val securityMessage = events.single { it.formattedMessage.contains("[SECURITY]") }.formattedMessage
            securityMessage.contains("rejectedTypeLength=${maliciousType.length}") shouldBeEqualTo true
            securityMessage.contains("allowedPackageCount=1") shouldBeEqualTo true
            events.any { it.formattedMessage.contains("poison pill skipped") } shouldBeEqualTo true
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
