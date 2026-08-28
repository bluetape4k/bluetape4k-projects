package io.bluetape4k.tenant

/** tenant를 lexical scope에 binding하고 조회하는 no-default 계약입니다. */
interface TenantContext {
    /** 현재 tenant를 반환하며 binding이 없으면 `null`을 반환합니다. */
    fun currentOrNull(): TenantId?

    /** 현재 tenant를 반환하며 binding이 없으면 [MissingTenantContextException]을 던집니다. */
    fun requireCurrent(): TenantId

    /** [block]을 [tenantId]가 binding된 lexical scope에서 실행합니다. */
    fun <T> withTenant(tenantId: TenantId, block: () -> T): T
}
