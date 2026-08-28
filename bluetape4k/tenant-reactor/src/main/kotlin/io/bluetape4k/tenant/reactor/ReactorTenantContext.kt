package io.bluetape4k.tenant.reactor

import io.bluetape4k.tenant.MissingTenantContextException
import io.bluetape4k.tenant.TenantId
import reactor.util.context.Context
import reactor.util.context.ContextView

/** immutable Reactor subscriber [Context]에서 tenant를 binding하고 조회합니다. */
object ReactorTenantContext {

    private object TenantKey

    /** 현재 subscriber tenant를 반환하며 binding이 없으면 `null`을 반환합니다. */
    fun currentOrNull(context: ContextView): TenantId? =
        if (context.hasKey(TenantKey)) context.get(TenantKey) else null

    /** 현재 subscriber tenant를 반환하며 binding이 없으면 공통 missing 예외를 던집니다. */
    fun requireCurrent(context: ContextView): TenantId =
        currentOrNull(context) ?: throw MissingTenantContextException()

    /** 입력 [context]를 변경하지 않고 [tenantId]를 가진 derived [Context]를 반환합니다. */
    fun withTenant(context: Context, tenantId: TenantId): Context =
        context.put(TenantKey, tenantId)
}
