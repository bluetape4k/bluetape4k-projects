package io.bluetape4k.io.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SerializationTrustProfileTest {

    @Test
    fun `trust profile display names are stable`() {
        SerializationTrustProfile.entries.map { it.displayName } shouldBeEqualTo listOf(
            "TrustedInternal",
            "AllowListedTypes",
            "NoDynamicTypeLoading",
            "UnsafeLegacyCompatibility",
        )
    }
}
