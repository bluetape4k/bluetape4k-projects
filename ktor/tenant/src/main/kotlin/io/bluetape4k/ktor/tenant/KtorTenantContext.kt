package io.bluetape4k.ktor.tenant

import io.bluetape4k.tenant.MissingTenantContextException
import io.bluetape4k.tenant.TenantId
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

/** Ktor [ApplicationCall]에 one-call/one-tenant 계약으로 tenant를 binding하고 조회합니다. */
object KtorTenantContext {

    private const val BINDING_KEY_NAME = "io.bluetape4k.ktor.tenant.binding.v1"
    private val tenantBindingKey = AttributeKey<TenantBinding>(BINDING_KEY_NAME)

    /** 현재 call의 tenant를 반환하며 binding이 없으면 `null`을 반환합니다. */
    fun currentOrNull(call: ApplicationCall): TenantId? =
        call.attributes.getOrNull(tenantBindingKey)?.tenantId

    /** 현재 call의 tenant를 반환하며 binding이 없으면 공통 missing 예외를 던집니다. */
    fun requireCurrent(call: ApplicationCall): TenantId =
        currentOrNull(call) ?: throw MissingTenantContextException()

    /** [call]에 [tenantId]를 최초 한 번만 binding합니다. */
    fun bindTenant(call: ApplicationCall, tenantId: TenantId) {
        synchronized(call.attributes) {
            if (tenantBindingKey in call.attributes) {
                throw TenantAlreadyBoundException()
            }
            call.attributes.put(tenantBindingKey, TenantBinding(tenantId))
        }
    }

    private data class TenantBinding(val tenantId: TenantId)
}
