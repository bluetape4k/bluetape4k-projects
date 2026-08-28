package io.bluetape4k.tenant

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TenantIdTest {

    @Test
    fun `빈 tenant id는 거부한다`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            TenantId("")
        }

        failure.message shouldBeEqualTo "TenantId must not be blank"
    }

    @Test
    fun `공백뿐인 tenant id는 거부한다`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            TenantId("   ")
        }

        failure.message shouldBeEqualTo "TenantId must not be blank"
    }

    @Test
    fun `tenant id 원문을 정규화하지 않는다`() {
        TenantId(" clinic-a ").value shouldBeEqualTo " clinic-a "
    }
}
