package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.science.exposed.NetCdfException
import org.junit.jupiter.api.Test

class NetCdfAuxiliarySerializerTest {

    @Test
    fun `empty auxiliary map is represented as SQL null`() {
        serializeAuxiliaryAttributes(emptyMap<String, Double>()) shouldBeEqualTo null
    }

    @Test
    fun `numeric auxiliary values use deterministic JSON`() {
        serializeAuxiliaryAttributes(linkedMapOf("altitude" to 100.0, "pressure" to 850.0))
            .shouldBeEqualTo("{\"altitude\":100.0,\"pressure\":850.0}")
    }

    @Test
    fun `reserved and oversized keys are rejected`() {
        assertFailsWith<NetCdfException.ResourceLimitExceeded> {
            serializeAuxiliaryAttributes(mapOf("__bluetape4k_reserved" to 1.0))
        }
        assertFailsWith<NetCdfException.ResourceLimitExceeded> {
            serializeAuxiliaryAttributes(mapOf("x".repeat(8_190) to 1.0))
        }
    }
}
