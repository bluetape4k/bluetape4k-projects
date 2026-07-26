package io.bluetape4k.redis.lettuce.coordination.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import org.junit.jupiter.api.Test

class CoordinationProtocolTest {

    private val schema = mapOf(
        "acquired" to 3,
        "busy" to 2,
        "closed" to 1,
    )

    @Test
    fun `bounded tagged response decodes known tags and non-negative numbers`() {
        val frame = CoordinationProtocol.decode(
            raw = listOf("acquired", 42L, "owner-fingerprint"),
            expectedArities = schema,
        )

        frame.tag shouldBeEqualTo "acquired"
        frame.nonNegativeLong(0) shouldBeEqualTo 42L
        frame.field(1) shouldBeEqualTo "owner-fingerprint"
    }

    @Test
    fun `unknown tag and wrong arity are integrity failures`() {
        assertIntegrityFailure { CoordinationProtocol.decode(listOf("unknown"), schema) }
        assertIntegrityFailure { CoordinationProtocol.decode(listOf("busy"), schema) }
        assertIntegrityFailure { CoordinationProtocol.decode(listOf("closed", "extra"), schema) }
    }

    @Test
    fun `negative and overflowing numeric fields are integrity failures`() {
        val negative = CoordinationProtocol.decode(listOf("acquired", "-1", "owner"), schema)
        val overflow = CoordinationProtocol.decode(
            listOf("acquired", "9223372036854775808", "owner"),
            schema,
        )

        assertIntegrityFailure { negative.nonNegativeLong(0) }
        assertIntegrityFailure { overflow.nonNegativeLong(0) }
    }

    @Test
    fun `response item and byte bounds are enforced`() {
        val tooManyItems = listOf("acquired") + List(16) { "x" }
        val tooManyBytes = listOf("busy", "x".repeat(256))

        assertIntegrityFailure { CoordinationProtocol.decode(tooManyItems, mapOf("acquired" to 17)) }
        assertIntegrityFailure { CoordinationProtocol.decode(tooManyBytes, schema) }
    }

    @Test
    fun `integrity failures redact raw backend values`() {
        val rawSecret = "customer-1234-secret"
        val failure = assertFailsWith<CoordinationProtocolException> {
            CoordinationProtocol.decode(listOf(rawSecret), schema)
        }

        failure.classification shouldBeEqualTo CoordinationFailureClassification.INTEGRITY
        failure.message.orEmpty().contains(rawSecret).shouldBeFalse()
    }

    private fun assertIntegrityFailure(block: () -> Unit) {
        val failure = assertFailsWith<CoordinationProtocolException>(block = block)
        failure.classification shouldBeEqualTo CoordinationFailureClassification.INTEGRITY
    }
}
