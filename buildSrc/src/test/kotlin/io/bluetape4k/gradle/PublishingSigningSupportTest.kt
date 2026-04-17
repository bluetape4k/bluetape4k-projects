package io.bluetape4k.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PublishingSigningSupportTest {

    @Test
    fun `resolveSigningKeyId keeps short key ID`() {
        assertEquals("5C6DF399", resolveSigningKeyId("5C6DF399"))
    }

    @Test
    fun `resolveSigningKeyId converts long key ID to short key ID`() {
        assertEquals("5C6DF399", resolveSigningKeyId("7CF28E155C6DF399"))
    }

    @Test
    fun `resolveSigningKeyId converts prefixed long key ID to short key ID`() {
        assertEquals("0x5C6DF399", resolveSigningKeyId("0x7CF28E155C6DF399"))
    }

    @Test
    fun `normalizeSigningKeyId returns warning for long key ID`() {
        val normalized = normalizeSigningKeyId("7CF28E155C6DF399")

        assertEquals("5C6DF399", normalized.value)
        assertEquals(
            "signingKeyId should use the trailing 8 hex digits. Received 7CF28E155C6DF399, normalized to 5C6DF399.",
            normalized.warning,
        )
    }

    @Test
    fun `normalizeSigningKeyId keeps short key ID without warning`() {
        val normalized = normalizeSigningKeyId("5C6DF399")

        assertEquals("5C6DF399", normalized.value)
        assertNull(normalized.warning)
    }
}
