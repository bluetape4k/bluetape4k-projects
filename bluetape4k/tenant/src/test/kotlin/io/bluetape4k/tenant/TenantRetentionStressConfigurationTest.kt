package io.bluetape4k.tenant

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class TenantRetentionStressConfigurationTest {

    @Test
    fun `tenant retention stress test remains assigned to the dedicated tag`() {
        TenantContextRetentionStressTest::class.java
            .getAnnotation(Tag::class.java)
            ?.value shouldBeEqualTo "tenant-retention-stress"
    }
}
