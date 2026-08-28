package io.bluetape4k.tenant

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import org.junit.jupiter.api.Test

class TenantContextApiTest {

    @Test
    fun `공개 API signature를 제공한다`() {
        val context = CompileOnlyTenantContext()
        val tenantId = TenantId("clinic-a")

        context.currentOrNull() shouldBeEqualTo null
        context.withTenant(tenantId) { tenantId } shouldBeEqualTo tenantId
    }

    @Test
    fun `missing context 예외의 type과 message를 고정한다`() {
        val failure = assertFailsWith<MissingTenantContextException> {
            CompileOnlyTenantContext().requireCurrent()
        }

        failure.message shouldBeEqualTo "Tenant context is not bound"
    }

    @Test
    fun `withTenant에 default 인자를 제공하지 않는다`() {
        TenantContext::class.java.declaredMethods
            .any { it.name == "withTenant\$default" }
            .shouldBeFalse()
    }

    private class CompileOnlyTenantContext: TenantContext {
        override fun currentOrNull(): TenantId? = null

        override fun requireCurrent(): TenantId = throw MissingTenantContextException()

        override fun <T> withTenant(tenantId: TenantId, block: () -> T): T = block()
    }
}
