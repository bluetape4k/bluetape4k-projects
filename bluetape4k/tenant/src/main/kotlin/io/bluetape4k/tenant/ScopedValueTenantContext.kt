package io.bluetape4k.tenant

/**
 * JDK 25 `ScopedValue` 기반 [TenantContext]입니다.
 *
 * `StructuredTaskScope.fork`의 lexical 상속 범위를 지원하며 일반 coroutine dispatcher hop의
 * 자동 전파는 보장하지 않습니다.
 */
class ScopedValueTenantContext: TenantContext {

    private val currentTenant = ScopedValue.newInstance<TenantId>()

    override fun currentOrNull(): TenantId? =
        if (currentTenant.isBound) currentTenant.get() else null

    override fun requireCurrent(): TenantId =
        currentOrNull() ?: throw MissingTenantContextException()

    override fun <T> withTenant(tenantId: TenantId, block: () -> T): T {
        var captured: Result<T>? = null
        ScopedValue.where(currentTenant, tenantId).run {
            captured = runCatching { block() }
        }
        return checkNotNull(captured) { "ScopedValue.Carrier.run did not execute block" }.getOrThrow()
    }
}
